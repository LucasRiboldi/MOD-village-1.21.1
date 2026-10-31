package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A entrada da mina na superfície, e o caminho por ela até o nível de agora —
 * pedido do autor, 2026-10-08: <i>"o mineiro deve salvar na memória o local de
 * entrada da mina, ir até ela e só depois começar sua tarefa"</i>.
 *
 * <p><b>O defeito.</b> {@link Mine#entry()} é a entrada do <b>nível atual</b>: a
 * cada "went one level deeper" ela desce dez blocos para dentro da rocha. No save
 * do autor a mina foi aberta em -775, 68, -922 e a "boca" virou -760, 38, -937; o
 * mineiro na superfície andava direto para esse ponto enterrado, em cima da mina,
 * e não chegava ("found no detour within 16 blocks").
 *
 * <p><b>A memória.</b> A cadeia de entradas é guardada quando a mina abre e cresce
 * um nível a cada descida. Sem ela (servidor reiniciado), é deduzida do mundo: cada
 * nível começa no piso da escada do anterior ({@link MineShaft#deepened}), e o giro
 * da escada (ADR-037 M1) muda o rumo sem mudar a entrada, então o rumo de cada
 * nível é o da escada que está cavada no mundo — RC2 do roteiro de 10-10.
 *
 * <p><b>O caminho.</b> As entradas de cada nível, da superfície até a de agora, são
 * ligadas pela escada em caracol já cavada; cada trecho é curto o bastante para a
 * navegação Vanilla. Quem está fora da mina vai primeiro à boca de superfície e
 * desce nível por nível; só no nível de agora volta a valer a perna de sempre.
 */
public final class MineEntrance {

    static {
        ServerMemory.register(MineEntrance.class, MineEntrance::clearAll);
    }

    /** Até onde ele conta como "na entrada": a folga de chegada. */
    static final int ARRIVE = 3;

    /** Quantos níveis a dedução sobe, no máximo. */
    static final int MAX_LEVELS = 40;

    /** Quanto acima do terreno a entrada da boca pode ficar (o arco e a escada de entrada). */
    static final int SURFACE_SLACK = 6;

    /** Dentro desta distância horizontal de uma entrada, ele está na escada dela. */
    static final int IN_LEVEL = 12;

    /**
     * Até onde o nível de agora vai, na horizontal, a partir da entrada dele: o salão e,
     * nos quatro cantos, o salão de cada ramal (Regra 119), com folga. A pedra e o
     * mineiro dentro disso estão no nível; uma régua curta deixava o fim do ramal "fora da
     * mina", e o mineiro na superfície ia direto à entrada enterrada embaixo dele
     * (playtest 0.3.31).
     */
    static final int LEVEL_REACH = 2 * MineShaft.HALL_SIDE + 4;

    /** Quanto da escada de um rumo precisa estar aberto para ela contar como cavada. */
    static final double DUG_SHARE = 0.5;

    /** Depois de uma dedução cortada por chunk fora de memória, quando deduzir de novo. */
    static final int RETRY_TICKS = 200;

    /** A cadeia de entradas de cada colônia, da boca de superfície até a de agora. */
    private static final Map<UUID, Chain> CHAINS = new HashMap<>();

    /** A cadeia, e até quando ela vale sem deduzir de novo. */
    private record Chain(List<ColonyPos> entries, long until) {
    }

    private MineEntrance() {
    }

    /** Guarda a boca de superfície da mina que acabou de abrir. */
    static void opened(UUID colonyId, ColonyPos mouth) {
        CHAINS.put(colonyId, new Chain(List.of(mouth), Long.MAX_VALUE));
    }

    /**
     * A primeira boca que a mina lembra — a da cadeia guardada, antes de qualquer dedução. É a ela que a escada volta
     * ao recomeçar (autor, 2026-10-29: "usar a mesma primeira descida").
     */
    static Optional<ColonyPos> rememberedMouth(UUID colonyId) {
        Chain known = CHAINS.get(colonyId);

        return known == null || known.entries().isEmpty() ? Optional.empty() : Optional.of(known.entries().getFirst());
    }

    /** Quantos níveis a mina já desceu desde a boca de superfície: 0 no primeiro. */
    public static int levelsDown(ServerWorld world, UUID colonyId, Mine mine) {
        return chainOf(world, colonyId, mine.shaft()).size() - 1;
    }

    /** A boca de superfície desta mina: a da cadeia guardada, ou a deduzida subindo os níveis. */
    public static ColonyPos surfaceOf(ServerWorld world, UUID colonyId, Mine mine) {
        return chainOf(world, colonyId, mine.shaft()).getFirst();
    }

    /** As entradas dos níveis, da superfície até a de agora. Uma só quando a mina não desceu. */
    public static List<BlockPos> route(ServerWorld world, UUID colonyId, Mine mine) {
        MineShaft now = mine.shaft();
        List<BlockPos> route = new ArrayList<>();

        if (now.isRamp()) {
            route.add(MinecraftTypeAdapter.toBlockPos(now.entry()));
            return route;
        }

        List<ColonyPos> chain = chainOf(world, colonyId, now);

        route.add(MinecraftTypeAdapter.toBlockPos(chain.getFirst()));

        // Entre uma entrada e a seguinte, o caminho a pé do nível de cima (Regra 119): o caracol, o
        // salão, o canto e o caracol do ramal — trechos curtos para a navegação Vanilla.
        for (int level = 1; level < chain.size(); level++) {
            ColonyPos above = chain.get(level - 1);
            ColonyPos below = chain.get(level);
            Optional<Side> descent = descentBetween(above, below);

            if (descent.isPresent()) {
                MineShaft.from(above, descent.get()).waypoints().stream()
                        .map(MinecraftTypeAdapter::toBlockPos)
                        .forEach(route::add);
            } else {
                route.add(MinecraftTypeAdapter.toBlockPos(below));
            }
        }

        return route;
    }

    /**
     * A cadeia guardada, crescida de um nível quando a mina desceu um; a deduzida do
     * mundo quando não há cadeia, ou quando ela não leva ao nível de agora.
     */
    private static List<ColonyPos> chainOf(ServerWorld world, UUID colonyId, MineShaft now) {
        Chain known = CHAINS.get(colonyId);

        if (known != null && known.until() >= world.getTime()) {
            ColonyPos last = known.entries().getLast();

            if (last.equals(now.entry())) {
                return known.entries();
            }

            if (oneLevelBelow(last, now.entry())) {
                List<ColonyPos> longer = new ArrayList<>(known.entries());
                longer.add(now.entry());
                CHAINS.put(colonyId, new Chain(List.copyOf(longer), known.until()));

                return longer;
            }
        }

        boolean[] cut = { false };
        List<ColonyPos> deduced = deduce(world, now, cut);

        CHAINS.put(colonyId, new Chain(List.copyOf(deduced),
                cut[0] ? world.getTime() + RETRY_TICKS : Long.MAX_VALUE));

        if (deduced.size() > 1) {
            VillageColonyMod.LOGGER.info(
                    "Mine {} — the surface entrance is remembered at {}, the level now starts at {}, {} levels down",
                    colonyId.toString().substring(0, 8), deduced.getFirst(), now.entry(), deduced.size() - 1);
        }

        return deduced;
    }

    /** Se {@code below} é a entrada do nível seguinte ao de {@code above} por algum rumo. */
    private static boolean oneLevelBelow(ColonyPos above, ColonyPos below) {
        return descentBetween(above, below).isPresent();
    }

    /** O rumo de descida que leva da entrada {@code above} à {@code below}, um nível abaixo. */
    private static Optional<Side> descentBetween(ColonyPos above, ColonyPos below) {
        for (Side descent : Side.values()) {
            ColonyPos step = stepOf(descent);

            if (below.equals(new ColonyPos(above.x() + step.x(), above.y() + step.y(), above.z() + step.z()))) {
                return Optional.of(descent);
            }
        }

        return Optional.empty();
    }

    /**
     * Sobe nível por nível pela escada cavada: mantém o rumo enquanto a escada dele
     * está aberta e troca pelo rumo mais aberto quando não está; para quando nenhum
     * rumo tem escada, ou quando o nível de cima ficaria acima do terreno.
     *
     * @param cut marcado quando um chunk fora de memória interrompeu a subida
     */
    private static List<ColonyPos> deduce(ServerWorld world, MineShaft now, boolean[] cut) {
        List<ColonyPos> chain = new ArrayList<>(List.of(now.entry()));
        ColonyPos at = now.entry();
        Side keep = now.descent();

        for (int level = 0; level < MAX_LEVELS; level++) {
            Optional<Side> descent = dugDescentAbove(world, at, keep, cut);

            if (descent.isEmpty()) {
                break;
            }

            ColonyPos up = above(at, descent.get());

            if (aboveTheTerrain(world, up, cut)) {
                break;
            }

            chain.addFirst(up);
            at = up;
            keep = descent.get();
        }

        return chain;
    }

    /** O rumo do nível de cima cuja escada até {@code at} está cavada: o de agora primeiro. */
    private static Optional<Side> dugDescentAbove(ServerWorld world, ColonyPos at, Side keep, boolean[] cut) {
        if (openShare(world, at, keep, cut) >= DUG_SHARE) {
            return Optional.of(keep);
        }

        @Nullable Side best = null;
        double bestShare = DUG_SHARE;

        for (Side descent : Side.values()) {
            if (descent == keep) {
                continue;
            }

            double share = openShare(world, at, descent, cut);

            if (share >= bestShare) {
                best = descent;
                bestShare = share;
            }
        }

        return Optional.ofNullable(best);
    }

    /** Quanto da escada que desce até {@code at} pelo rumo {@code descent} está aberto. */
    private static double openShare(ServerWorld world, ColonyPos at, Side descent, boolean[] cut) {
        MineShaft stair = MineShaft.from(above(at, descent), descent);
        int open = 0;

        for (int index = 0; index < MineShaft.CARVED; index++) {
            BlockPos cell = MinecraftTypeAdapter.toBlockPos(stair.positionAt(index));

            if (world.getChunkManager().getWorldChunk(cell.getX() >> 4, cell.getZ() >> 4) == null) {
                // Chunk fora de memória: não se força carregamento (ADR-002).
                cut[0] = true;
                continue;
            }

            if (MineRock.isOpenSpace(world, cell, world.getBlockState(cell))) {
                open++;
            }
        }

        return (double) open / MineShaft.CARVED;
    }

    /** A entrada do nível de cima, se a escada dele desce por {@code descent} até {@code at}. */
    private static ColonyPos above(ColonyPos at, Side descent) {
        ColonyPos step = stepOf(descent);

        return new ColonyPos(at.x() - step.x(), at.y() - step.y(), at.z() - step.z());
    }

    /**
     * Para onde andar primeiro, quando a pedra está no nível de agora e ele ainda
     * não entrou nele: a boca de superfície, e depois a entrada de cada nível.
     * Vazio quando ele já está no nível — ou a pedra não é da mina —, e a perna de
     * sempre decide.
     */
    public static Optional<BlockPos> legDown(ServerWorld world, UUID colonyId, BlockPos miner, BlockPos target) {
        Optional<Mine> mine = VillageColonyMod.MINES.of(colonyId);

        if (mine.isEmpty()) {
            return Optional.empty();
        }

        List<BlockPos> route = route(world, colonyId, mine.get());
        BlockPos now = route.getLast();

        if (route.size() < 2
                || !insideTheLevel(target, now)
                || insideTheLevel(miner, now)) {
            return Optional.empty();
        }

        BlockPos surface = route.getFirst();

        // Fora da escada (na superfície, ou longe da mina): primeiro a boca, e da
        // boca o primeiro lance.
        if (!nearTheShaft(world, miner, route)) {
            return Optional.of(footing(world, arrived(miner, surface) ? route.get(1) : surface));
        }

        // Na escada, o progresso é a altura: a entrada seguinte é a primeira abaixo
        // dele. A régua anterior ("a entrada mais perto") puxava de volta para cima
        // quem já tinha passado de uma entrada — três abaixo e quatro ao lado dela, e
        // aldeão sobe um (playtest de 08-10, 15:31–15:46).
        for (int index = 1; index < route.size(); index++) {
            if (route.get(index).getY() < miner.getY() - 1) {
                return Optional.of(footing(world, route.get(index)));
            }
        }

        return Optional.of(footing(world, route.getLast()));
    }

    /** Se o mineiro está fora da escada desta mina — na superfície ou longe dela (MineAccess, 2026-10-22). */
    static boolean outsideTheShaft(ServerWorld world, UUID colonyId, BlockPos miner) {
        Optional<Mine> mine = VillageColonyMod.MINES.of(colonyId);

        return mine.isPresent() && !nearTheShaft(world, miner, route(world, colonyId, mine.get()));
    }

    /**
     * Na escada da mina: logo abaixo da boca (o primeiro lance, que pode ser a céu
     * aberto), ou debaixo da terra perto de alguma entrada. Quem está na superfície
     * em cima da mina não conta — mandá-lo à entrada enterrada embaixo dele foi o
     * defeito de 08-10 de manhã.
     */
    private static boolean nearTheShaft(ServerWorld world, BlockPos miner, List<BlockPos> route) {
        BlockPos mouth = route.getFirst();

        if (miner.getY() <= mouth.getY() - 2
                && Math.abs(miner.getX() - mouth.getX()) <= IN_LEVEL / 2
                && Math.abs(miner.getZ() - mouth.getZ()) <= IN_LEVEL / 2) {
            return true;
        }

        if (world.getChunkManager().getWorldChunk(miner.getX() >> 4, miner.getZ() >> 4) == null
                || miner.getY() >= world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                        miner.getX(), miner.getZ()) - 2) {
            return false;
        }

        for (BlockPos entry : route) {
            if (Math.abs(miner.getX() - entry.getX()) <= IN_LEVEL / 2
                    && Math.abs(miner.getZ() - entry.getZ()) <= IN_LEVEL / 2) {
                return true;
            }
        }

        return false;
    }

    /** Chegou à entrada: a até {@link #ARRIVE} blocos em cada eixo. */
    static boolean arrived(BlockPos miner, BlockPos entry) {
        return Math.abs(miner.getX() - entry.getX()) <= ARRIVE
                && Math.abs(miner.getY() - entry.getY()) <= ARRIVE
                && Math.abs(miner.getZ() - entry.getZ()) <= ARRIVE;
    }

    /**
     * O lugar de pé da entrada: ela própria, se cabe um aldeão, ou o mais perto dela
     * a até dois blocos de lado e três abaixo — a escada desce, e a entrada do nível é
     * o alto do primeiro degrau. Sem nenhum, a própria entrada.
     */
    private static BlockPos footing(ServerWorld world, BlockPos entry) {
        for (int dy = 0; dy >= -3; dy--) {
            for (int radius = 0; radius <= 2; radius++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                            continue;
                        }

                        BlockPos at = entry.add(dx, dy, dz);

                        if (world.getChunkManager().getWorldChunk(at.getX() >> 4, at.getZ() >> 4) != null
                                && BuilderApproach.standable(world, at)) {
                            return at;
                        }
                    }
                }
            }
        }

        return entry;
    }

    /** No nível de agora: no alcance dos ramais dele na horizontal e não acima da entrada. */
    static boolean insideTheLevel(BlockPos at, BlockPos levelEntry) {
        return Math.abs(at.getX() - levelEntry.getX()) <= LEVEL_REACH
                && Math.abs(at.getZ() - levelEntry.getZ()) <= LEVEL_REACH
                && at.getY() <= levelEntry.getY() + 2;
    }

    /** O deslocamento de um nível para o seguinte; só depende do sentido de descida. */
    private static ColonyPos stepOf(Side descent) {
        return MineShaft.from(new ColonyPos(0, 0, 0), descent).deepened().entry();
    }

    /**
     * O "nível de cima" ficaria solto acima do terreno: então ele não existe, e a
     * entrada de agora já é a boca. Perguntar "a entrada está a céu aberto?" era
     * frágil — árvore, casa ou o arco em cima da boca a escondiam do céu, e a dedução
     * subia para um nível no ar (bateria de 2026-10-08).
     */
    private static boolean aboveTheTerrain(ServerWorld world, ColonyPos up, boolean[] cut) {
        if (world.getChunkManager().getWorldChunk(up.x() >> 4, up.z() >> 4) == null) {
            // Chunk fora de memória: não se força carregamento (ADR-002); para aqui.
            cut[0] = true;
            return true;
        }

        return up.y() > world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, up.x(), up.z()) + SURFACE_SLACK;
    }

    /** Para o teste: esquece a cadeia guardada desta colônia. */
    static void forget(@Nullable UUID colonyId) {
        CHAINS.remove(colonyId);
    }

    public static void clearAll() {
        CHAINS.clear();
    }
}
