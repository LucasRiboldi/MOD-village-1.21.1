package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.coordination.PlanningBudget;
import com.villagecolony.core.coordination.VillageExpansion;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.integration.BuildSiteScanner.Footprint;
import com.villagecolony.fabric.integration.BuildSiteScanner.Site;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O lote dentro da faixa anexada, com o caminho até a rua — decisão do autor de 2026-10-22 (A5), que
 * emenda a Regra 56 D ("o lote só nasce encostado em rua").
 *
 * <p>No deserto (0.3.86) a vila anexou uma faixa ao sul com 423 de 440 colunas planas, e a rua que
 * devia entrar nela não andou um bloco: a duna subia dois na entrada. Sem rua, a faixa não dava lote,
 * e a vila ficou sem obra. Agora, com a faixa pendente, a vila procura o lote dentro dela e um caminho
 * a pé até a rua que já existe:
 *
 * <ul>
 *   <li>o caminho é um <b>corredor</b> ({@link Corridor}): sai da porta em linha reta, vira só em ângulo
 *       reto e para no primeiro caminho que encostar; segue o terreno com degrau de no máximo um, sem
 *       cortar nem aterrar, e contorna a duna quando há volta;</li>
 *   <li>tem até {@link #MAX_PATH} blocos, e é calçado na abertura da obra (o {@code path} do
 *       {@link Site}, como o recuo da Regra 62);</li>
 *   <li>o lote nasce <b>o mais fundo na faixa</b> que couber nela.</li>
 * </ul>
 *
 * <p>A busca é fatiada como a varredura de lote: {@link PlanningBudget#DEADLINE_MS} por chamada, com
 * cursor por colônia.
 */
public final class StripLot {

    static {
        ServerMemory.register(StripLot.class, StripLot::clearAll);
    }

    /** O caminho mais longo até a rua: o alcance da rede de caminhos além da caixa (Regra 64). */
    public static final int MAX_PATH = 32;

    /**
     * O lote da faixa fica a até tantos blocos do centro da vila — Regra 141 (autor 2026-10-28; na taiga
     * simulada o curral nasceu a 98 blocos e as obras seguintes foram atrás dele).
     */
    public static final int REACH = 48;

    /** Quanto o alcance cresce a cada volta inteira pela faixa sem lote. */
    public static final int REACH_STEP = 10;

    /** As voltas sem lote de cada colônia: o alcance de agora é {@code REACH + REACH_STEP × voltas}. */
    private static final Map<UUID, Integer> MISSES = new HashMap<>();

    /** De quantos em quantos blocos a porta é testada, na frente e ao longo da faixa. */
    static final int STEP = 2;

    /** Onde a busca parou, por colônia: a faixa medida, a próxima porta e o que a volta já viu. */
    private record Cursor(VillageBounds strip, Side side, int next, Tally tally) {
    }

    /** O que uma volta pela faixa viu — a linha do fim da volta sem lote (deserto, 2026-10-22). */
    static final class Tally {
        int doors;
        int lotsFit;
        int tooFar;

        /** O fim do orçamento desta passada; entre um tamanho de casa e outro a porta para se passou dele. */
        long deadline = Long.MAX_VALUE;

        /** De que tamanho de casa a porta interrompida recomeça. */
        int resumeFootprint;

        /** Se a porta desta passada parou no meio dos tamanhos — a próxima passada volta a ela. */
        boolean interrupted;
        final Map<String, Integer> pathStoppedBy = new HashMap<>();

        void stopped(String why) {
            pathStoppedBy.merge(why, 1, Integer::sum);
        }

        String summary() {
            return pathStoppedBy.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(6)
                    .map(entry -> entry.getValue() + " " + entry.getKey())
                    .collect(java.util.stream.Collectors.joining("; "));
        }
    }

    private static final Map<UUID, Cursor> CURSORS = new HashMap<>();

    private StripLot() {
    }

    /**
     * O lote mais fundo da faixa anexada que tem caminho até a rua.
     *
     * @param box a caixa da vila de agora
     * @return o lote, com o caminho a calçar; vazio quando nenhum serviu nesta passada (a seguinte continua)
     */
    public static Optional<Site> find(
            ServerWorld world, UUID colonyId, VillageBounds box, Side side, List<Footprint> footprints) {

        VillageBounds strip = VillageExpansion.stripOf(box, side);
        Direction out = Direction.fromVector(side.offsetX(), 0, side.offsetZ());

        if (out == null) {
            return Optional.empty();
        }

        List<BlockPos> doors = doorColumns(strip, side);
        Cursor cursor = CURSORS.get(colonyId);
        boolean resume = cursor != null && cursor.strip().equals(strip) && cursor.side() == side;
        int next = resume ? cursor.next() : 0;
        Tally tally = resume ? cursor.tally() : new Tally();
        long deadline = System.nanoTime() + PlanningBudget.DEADLINE_MS * 1_000_000L;

        tally.deadline = deadline;

        for (; next < doors.size(); next++) {
            if (System.nanoTime() > deadline) {
                CURSORS.put(colonyId, new Cursor(strip, side, next, tally));

                return Optional.empty();
            }

            if (tally.resumeFootprint == 0) {
                tally.doors++;
            }

            Optional<Site> site = siteAt(world, colonyId, doors.get(next), out, strip, footprints, tally);

            if (site.isPresent()) {
                CURSORS.remove(colonyId);
                MISSES.remove(colonyId);

                return site;
            }

            // A busca com corte é cara: a porta que parou no meio volta na próxima passada, do tamanho
            // em que parou, e cada passada anda ao menos um tamanho (0.3.90: 3 s num tique sem isso).
            if (tally.interrupted) {
                tally.interrupted = false;
                CURSORS.put(colonyId, new Cursor(strip, side, next, tally));

                return Optional.empty();
            }

            tally.resumeFootprint = 0;
        }

        // Volta inteira sem lote: diz o que viu, a próxima passada recomeça do fundo e, se a faixa tinha
        // porta além do alcance, o alcance cresce um passo.
        CURSORS.remove(colonyId);

        if (tally.tooFar > 0) {
            MISSES.merge(colonyId, 1, Integer::sum);
        }

        VillageColonyMod.LOGGER.info(
                "Colony {} found no lot inside the annexed strip {} — {} doors ({} beyond {} blocks of the"
                        + " center), {} lots fit, none with a path of at most {} blocks to the road — {}",
                colonyId.toString().substring(0, 8), side, tally.doors, tally.tooFar, reachOf(colonyId),
                tally.lotsFit, MAX_PATH,
                tally.pathStoppedBy.isEmpty() ? "no path search" : "path stopped by " + tally.summary());

        return Optional.empty();
    }

    /** As colunas da porta, do fundo da faixa para a frente, e ao longo dela. */
    static List<BlockPos> doorColumns(VillageBounds strip, Side side) {
        boolean alongX = side.offsetX() == 0;
        int depth = alongX ? strip.maxZ() - strip.minZ() + 1 : strip.maxX() - strip.minX() + 1;
        List<BlockPos> doors = new ArrayList<>();

        for (int k = depth - 1; k >= 0; k -= STEP) {
            int lateralFrom = alongX ? strip.minX() : strip.minZ();
            int lateralTo = alongX ? strip.maxX() : strip.maxZ();

            for (int lateral = lateralFrom; lateral <= lateralTo; lateral += STEP) {
                // k blocos para dentro da faixa, a partir da borda da caixa.
                int edge = switch (side) {
                    case EAST -> strip.minX() + k;
                    case WEST -> strip.maxX() - k;
                    case SOUTH -> strip.minZ() + k;
                    case NORTH -> strip.maxZ() - k;
                };

                doors.add(alongX ? new BlockPos(lateral, 0, edge) : new BlockPos(edge, 0, lateral));
            }
        }

        return doors;
    }

    /** O lote com a porta nesta coluna, voltada para a vila, dentro da faixa e com caminho até a rua. */
    private static Optional<Site> siteAt(
            ServerWorld world, UUID colonyId, BlockPos column, Direction out, VillageBounds strip,
            List<Footprint> footprints, Tally tally) {

        if (beyondReach(colonyId, column)) {
            tally.tooFar++;

            return Optional.empty();
        }

        Optional<BlockPos> door = groundAt(world, column.getX(), column.getZ());

        if (door.isEmpty() || RoadPaving.refusalAt(world, door.get(), world.getBlockState(door.get())).isPresent()) {
            return Optional.empty();
        }

        int floorY = door.get().getY();
        int from = tally.resumeFootprint;

        for (int index = from; index < footprints.size(); index++) {
            Footprint footprint = footprints.get(index);

            if (footprint.doorSide() != out.getOpposite()) {
                continue;
            }

            if (index > from && System.nanoTime() > tally.deadline) {
                tally.resumeFootprint = index;
                tally.interrupted = true;

                return Optional.empty();
            }

            ColonyPos size = footprint.size();
            int lotX = column.getX() + out.getOffsetX();
            int lotZ = column.getZ() + out.getOffsetZ();
            int originX = out.getOffsetX() < 0 ? lotX - size.x() + 1 : lotX;
            int originZ = out.getOffsetZ() < 0 ? lotZ - size.z() + 1 : lotZ;
            boolean alongX = out.getOffsetX() == 0;
            // A porta no meio da frente da casa.
            originX -= alongX ? size.x() / 2 : 0;
            originZ -= alongX ? 0 : size.z() / 2;

            if (!inside(strip, originX, originZ, size)) {
                continue;
            }

            Optional<Integer> floor = LotLevel.flatGroundAt(world, colonyId, originX, originZ, floorY, floorY, size);

            if (floor.isEmpty()) {
                continue;
            }

            tally.lotsFit++;
            Optional<List<ColonyPos>> path = pathToTheRoad(
                    world, colonyId, door.get(), out.getOpposite(), originX, originZ, size, tally);

            if (path.isPresent()) {
                VillageColonyMod.LOGGER.info(
                        "Colony {} finds a lot inside the annexed strip at {}, {} — {} blocks of path to the road",
                        colonyId.toString().substring(0, 8), originX, originZ, path.get().size());

                return Optional.of(new Site(
                        new ColonyPos(originX, floor.get(), originZ), out.getOpposite(), size, path.get()));
            }
        }

        return Optional.empty();
    }

    /**
     * O caminho da porta até a rua: o corredor ({@link Corridor}) — retas de um bloco com curvas em ângulo
     * reto, saindo da porta na direção dela e parando no primeiro caminho que encostar, até
     * {@link #MAX_PATH} blocos, em serpentina. Sem caminho pelo terreno, corta a encosta até {@link Corridor#MAX_CUT} de
     * fundo. Regras do autor de 2026-10-22: o corredor substituiu a busca livre pelo terreno, e o degrau
     * cortado vence a duna que não tem volta.
     *
     * @return as colunas a calçar, da porta até a última antes da rua; vazio sem caminho
     */
    static Optional<List<ColonyPos>> pathToTheRoad(
            ServerWorld world, UUID colonyId, BlockPos door, Direction out,
            int originX, int originZ, ColonyPos size) {

        return pathToTheRoad(world, colonyId, door, out, originX, originZ, size, new Tally());
    }

    /** O mesmo, anotando em {@code tally} o que parou cada ramo da busca. */
    static Optional<List<ColonyPos>> pathToTheRoad(
            ServerWorld world, UUID colonyId, BlockPos door, Direction out,
            int originX, int originZ, ColonyPos size, Tally tally) {

        java.util.function.Predicate<BlockPos> lot = at -> onTheLot(at, originX, originZ, size);
        Optional<List<BlockPos>> natural = Corridor.planWinding(world, colonyId, door, out, MAX_PATH, lot, tally::stopped);

        // O terreno natural primeiro, em serpentina pela encosta; só sem volta por ela o corredor corta, raso
        // e com talude (opção 3 do autor, 2026-10-28).
        return natural.or(() -> Corridor.plan(world, colonyId, door, out, MAX_PATH, Corridor.MAX_CUT, lot, tally::stopped))
                .map(Corridor::asColonyPositions);
    }

    /** O chão desta coluna: o topo do terreno, pela mesma leitura do lote. */
    private static Optional<BlockPos> groundAt(ServerWorld world, int x, int z) {
        if (world.getChunkManager().getWorldChunk(x >> 4, z >> 4) == null) {
            return Optional.empty();
        }

        int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;

        return LotGround.groundInColumn(world, x, z, top);
    }

    /** O alcance de agora desta colônia: {@link #REACH}, mais {@link #REACH_STEP} por volta sem lote. */
    static int reachOf(UUID colonyId) {
        return REACH + REACH_STEP * MISSES.getOrDefault(colonyId, 0);
    }

    /** Se a porta nesta coluna fica além do alcance, medido do centro fixo da vila. */
    private static boolean beyondReach(UUID colonyId, BlockPos column) {
        long reach = reachOf(colonyId);

        return VillageColonyMod.COLONIES.find(colonyId)
                .map(colony -> {
                    long dx = (long) column.getX() - colony.center().x();
                    long dz = (long) column.getZ() - colony.center().z();

                    return dx * dx + dz * dz > reach * reach;
                })
                .orElse(false);
    }

    private static boolean inside(VillageBounds strip, int originX, int originZ, ColonyPos size) {
        return strip.containsColumn(originX, originZ)
                && strip.containsColumn(originX + size.x() - 1, originZ + size.z() - 1);
    }

    private static boolean onTheLot(BlockPos column, int originX, int originZ, ColonyPos size) {
        return column.getX() >= originX && column.getX() < originX + size.x()
                && column.getZ() >= originZ && column.getZ() < originZ + size.z();
    }

    public static void clearAll() {
        CURSORS.clear();
        MISSES.clear();
    }
}
