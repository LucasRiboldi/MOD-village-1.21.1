package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
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
 * <p><b>A memória.</b> A boca de superfície é guardada quando a mina abre. Para a
 * mina que já desceu antes desta regra existir, ela é deduzida: cada nível começa
 * num deslocamento fixo do anterior ({@link MineShaft#deepened}, que só depende do
 * sentido de descida), e sobe-se por ele enquanto o nível de cima fica dentro do
 * terreno.
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

    /** Dentro desta distância horizontal da entrada de agora, ele já está no nível. */
    static final int IN_LEVEL = 12;

    private static final Map<UUID, ColonyPos> SURFACE = new HashMap<>();

    private MineEntrance() {
    }

    /** Guarda a boca de superfície da mina que acabou de abrir. */
    static void opened(UUID colonyId, ColonyPos mouth) {
        SURFACE.put(colonyId, mouth);
    }

    /** A boca de superfície desta mina: a guardada, ou a deduzida subindo os níveis. */
    public static ColonyPos surfaceOf(ServerWorld world, UUID colonyId, Mine mine) {
        ColonyPos remembered = SURFACE.get(colonyId);

        if (remembered != null && onTheChain(remembered, mine.shaft())) {
            return remembered;
        }

        MineShaft shaft = mine.shaft();
        ColonyPos step = stepOf(shaft);
        ColonyPos at = shaft.entry();

        for (int level = 0; level < MAX_LEVELS; level++) {
            ColonyPos up = new ColonyPos(at.x() - step.x(), at.y() - step.y(), at.z() - step.z());

            if (aboveTheTerrain(world, up)) {
                break;
            }

            at = up;
        }

        SURFACE.put(colonyId, at);

        if (!at.equals(shaft.entry())) {
            VillageColonyMod.LOGGER.info(
                    "Mine {} — the surface entrance is remembered at {}, the level now starts at {}",
                    colonyId.toString().substring(0, 8), at, shaft.entry());
        }

        return at;
    }

    /** As entradas dos níveis, da superfície até a de agora. Uma só quando a mina não desceu. */
    public static List<BlockPos> route(ServerWorld world, UUID colonyId, Mine mine) {
        MineShaft now = mine.shaft();
        List<BlockPos> route = new ArrayList<>();

        if (now.isRamp()) {
            route.add(MinecraftTypeAdapter.toBlockPos(now.entry()));
            return route;
        }

        MineShaft level = new MineShaft(surfaceOf(world, colonyId, mine), now.descent(), now.gallery());

        for (int count = 0; count <= MAX_LEVELS; count++) {
            route.add(MinecraftTypeAdapter.toBlockPos(level.entry()));

            if (level.entry().equals(now.entry())) {
                return route;
            }

            level = level.deepened();
        }

        // A cadeia não chegou ao nível de agora (mina realocada): só a entrada atual.
        return new ArrayList<>(List.of(MinecraftTypeAdapter.toBlockPos(now.entry())));
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

    /** No nível de agora: perto da entrada dele na horizontal e não acima dela. */
    private static boolean insideTheLevel(BlockPos at, BlockPos levelEntry) {
        return Math.abs(at.getX() - levelEntry.getX()) <= IN_LEVEL
                && Math.abs(at.getZ() - levelEntry.getZ()) <= IN_LEVEL
                && at.getY() <= levelEntry.getY() + 2;
    }

    /** O deslocamento de um nível para o seguinte; só depende do sentido de descida. */
    private static ColonyPos stepOf(MineShaft shaft) {
        ColonyPos origin = new ColonyPos(0, 0, 0);
        ColonyPos next = new MineShaft(origin, shaft.descent(), shaft.gallery()).deepened().entry();

        return next;
    }

    /** Se a boca guardada ainda leva ao nível de agora pela cadeia de níveis. */
    private static boolean onTheChain(ColonyPos surface, MineShaft now) {
        if (now.isRamp()) {
            return surface.equals(now.entry());
        }

        ColonyPos step = stepOf(now);
        ColonyPos at = surface;

        for (int level = 0; level <= MAX_LEVELS; level++) {
            if (at.equals(now.entry())) {
                return true;
            }

            at = new ColonyPos(at.x() + step.x(), at.y() + step.y(), at.z() + step.z());
        }

        return false;
    }

    /**
     * O "nível de cima" ficaria solto acima do terreno: então ele não existe, e a
     * entrada de agora já é a boca. Perguntar "a entrada está a céu aberto?" era
     * frágil — árvore, casa ou o arco em cima da boca a escondiam do céu, e a dedução
     * subia para um nível no ar (bateria de 2026-10-08).
     */
    private static boolean aboveTheTerrain(ServerWorld world, ColonyPos up) {
        if (world.getChunkManager().getWorldChunk(up.x() >> 4, up.z() >> 4) == null) {
            // Chunk fora de memória: não se força carregamento (ADR-002); para aqui.
            return true;
        }

        return up.y() > world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, up.x(), up.z()) + SURFACE_SLACK;
    }

    /** Para o teste: esquece a boca guardada desta colônia. */
    static void forget(@Nullable UUID colonyId) {
        SURFACE.remove(colonyId);
    }

    public static void clearAll() {
        SURFACE.clear();
    }
}
