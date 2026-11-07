package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.RoadGraph;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.WorldPathTerrain;
import com.villagecolony.fabric.world.GeneratedVillages;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * As entradas das estruturas da vila, lidas do mundo — Regras 207, 210 e 223 (autor, 2026-11-07): branco só onde se entra.
 * Porta, portão e o portão da mina; sem eles, o lado aberto de quem precisa ser alcançado. Poste, poço e chafariz não têm
 * entrada, nem curral fechado por dentro, e a quina da plantação só vale sem rua a até {@value #FARM_ROAD_REACH} blocos.
 */
final class StructureFronts {

    /** Com linha a até esta distância da quina, a plantação já está na rua. */
    static final int FARM_ROAD_REACH = 2;

    /**
     * O que a releitura leu.
     *
     * @param fronts as entradas
     * @param ways as colunas até a porta que precisam ser calçadas por dentro da caixa, porque nenhum caminho sai dela
     * @param retired as colunas que deixaram de ser entrada: o ramal delas sai (Regra 223)
     */
    record Reading(Set<ColonyPos> fronts, Set<ColonyPos> ways, Set<ColonyPos> retired) {
    }

    private StructureFronts() {
    }

    static Reading read(ServerWorld world, Colony colony, WorldPathTerrain terrain, Set<ColonyPos> paths) {
        Set<ColonyPos> fronts = new HashSet<>();
        Set<ColonyPos> ways = new HashSet<>();
        Set<ColonyPos> retired = new HashSet<>();
        Set<Long> columns = new HashSet<>();
        BlockPos hub = MinecraftTypeAdapter.toBlockPos(colony.center());

        paths.forEach(at -> columns.add(ColonyRoads.column(at.x(), at.z())));

        for (Building building : VillageColonyMod.BUILDINGS.ofColony(colony.id())) {
            if (building.finished()) {
                BlockPos min = MinecraftTypeAdapter.toBlockPos(building.min());
                BlockPos max = MinecraftTypeAdapter.toBlockPos(building.max());

                if (GeneratedVillages.isOrnament(building.blueprint().path())) {
                    EntranceSides.of(world, min, max, hub, terrain).ifPresent(retired::add);
                } else if (!doors(world, min, max, columns, fronts, ways, retired) && !RoadShape.isFarm(building)) {
                    front(world, min, max, hub, terrain).ifPresent(fronts::add);
                }
            }
        }

        for (BlockBox piece : GeneratedVillages.structuresAt(world, hub.getX(), hub.getZ())) {
            BlockPos min = new BlockPos(piece.getMinX(), piece.getMinY(), piece.getMinZ());
            BlockPos max = new BlockPos(piece.getMaxX(), piece.getMaxY(), piece.getMaxZ());

            if (!doors(world, min, max, columns, fronts, ways, retired)) {
                front(world, min, max, hub, terrain).ifPresent(fronts::add);
            }
        }

        for (BlockBox piece : GeneratedVillages.ornamentsAt(world, hub.getX(), hub.getZ())) {
            EntranceSides.of(world, new BlockPos(piece.getMinX(), piece.getMinY(), piece.getMinZ()),
                    new BlockPos(piece.getMaxX(), piece.getMaxY(), piece.getMaxZ()), hub, terrain).ifPresent(retired::add);
        }

        fronts.addAll(MineGates.fronts(world, colony, terrain));

        List<ShepherdPens.Pen> pens = ShepherdPens.pensOf(world, colony);

        withoutTheInsideOfAPen(fronts, pens);

        // Todo portão de cerca é entrada (Regra 178), de obra ou da vila gerada.
        for (ShepherdPens.Pen pen : pens) {
            for (ShepherdPens.Access access : pen.accesses()) {
                BlockPos out = access.outside();
                OptionalInt ground = terrain.groundY(out.getX(), out.getZ(), out.getY() - 1);

                if (ground.isPresent()) {
                    fronts.add(new ColonyPos(out.getX(), ground.getAsInt(), out.getZ()));
                }
            }
        }

        retired.removeAll(fronts);

        return new Reading(fronts, ways, retired);
    }

    /** A quina da plantação que ainda não tem rua ao lado (Regras 184 e 223); a outra entra em {@code retired}. */
    static void farmCorners(Set<ColonyPos> corners, Optional<RoadGraph> graph, Set<ColonyPos> fronts, Set<ColonyPos> retired) {
        for (ColonyPos corner : corners) {
            if (graph.filter(lines -> lines.hasLineNear(corner, FARM_ROAD_REACH)).isPresent()) {
                retired.add(corner);
            } else {
                fronts.add(corner);
            }
        }
    }

    /** Lê as portas desta caixa; verdadeiro se há alguma. */
    private static boolean doors(ServerWorld world, BlockPos min, BlockPos max, Set<Long> paths, Set<ColonyPos> fronts,
                                 Set<ColonyPos> ways, Set<ColonyPos> retired) {
        List<DoorFronts.Entrance> entrances = DoorFronts.entrances(world, min, max);

        for (DoorFronts.Entrance entrance : entrances) {
            fronts.add(MinecraftTypeAdapter.toColonyPos(entrance.front()));

            if (!entrance.outside().equals(entrance.front())) {
                retired.add(MinecraftTypeAdapter.toColonyPos(entrance.outside()));

                if (!leavesByPath(entrance.front(), paths, min, max)) {
                    entrance.way().forEach(at -> ways.add(MinecraftTypeAdapter.toColonyPos(at)));
                }
            }
        }

        return !entrances.isEmpty();
    }

    /** Se um caminho já leva da porta à borda da caixa: a linha segue por ele, e não em reta (Regra 223). */
    static boolean leavesByPath(BlockPos front, Set<Long> paths, BlockPos min, BlockPos max) {
        Set<Long> seen = new HashSet<>();
        Deque<Long> open = new ArrayDeque<>();

        seen.add(ColonyRoads.column(front.getX(), front.getZ()));
        open.add(ColonyRoads.column(front.getX(), front.getZ()));

        while (!open.isEmpty()) {
            long at = open.poll();
            int x = ColonyRoads.xOf(at);
            int z = ColonyRoads.zOf(at);

            if (paths.contains(at) && (x <= min.getX() || x >= max.getX() || z <= min.getZ() || z >= max.getZ())) {
                return true;
            }

            for (long next : List.of(ColonyRoads.column(x + 1, z), ColonyRoads.column(x - 1, z),
                    ColonyRoads.column(x, z + 1), ColonyRoads.column(x, z - 1))) {
                if (paths.contains(next) && seen.add(next)) {
                    open.add(next);
                }
            }
        }

        return false;
    }

    /** Dentro de curral fechado não há entrada: o caminho até lá abriria a cerca, e quem entra fica preso (Regra 223). */
    private static void withoutTheInsideOfAPen(Set<ColonyPos> fronts, List<ShepherdPens.Pen> pens) {
        for (ShepherdPens.Pen pen : pens) {
            Set<Long> inside = new HashSet<>();

            pen.reach().forEach(at -> inside.add(ColonyRoads.column(at.getX(), at.getZ())));
            fronts.removeIf(at -> inside.contains(ColonyRoads.column(at.x(), at.z())));
        }
    }

    /** Quem não tem porta: o portão da borda, ou o lado que parece aberto (Regra 210). */
    private static Optional<ColonyPos> front(ServerWorld world, BlockPos min, BlockPos max, BlockPos hub,
                                             WorldPathTerrain terrain) {
        Optional<ColonyPos> gate = gateFront(world, min, max, terrain);

        return gate.isPresent() ? gate : EntranceSides.of(world, min, max, hub, terrain);
    }

    /** Curral: o lado de fora do primeiro portão da borda. */
    private static Optional<ColonyPos> gateFront(ServerWorld world, BlockPos min, BlockPos max, WorldPathTerrain terrain) {
        for (BlockPos at : BlockPos.iterate(min.getX(), min.getY(), min.getZ(), max.getX(), min.getY() + 2, max.getZ())) {
            boolean border = at.getX() == min.getX() || at.getX() == max.getX()
                    || at.getZ() == min.getZ() || at.getZ() == max.getZ();

            if (border && world.getBlockState(at).getBlock() instanceof FenceGateBlock) {
                int x = at.getX() == min.getX() ? at.getX() - 1 : at.getX() == max.getX() ? at.getX() + 1 : at.getX();
                int z = at.getZ() == min.getZ() ? at.getZ() - 1 : at.getZ() == max.getZ() ? at.getZ() + 1 : at.getZ();
                OptionalInt ground = terrain.groundY(x, z, at.getY() - 1);

                if (ground.isPresent()) {
                    return Optional.of(new ColonyPos(x, ground.getAsInt(), z));
                }
            }
        }

        return Optional.empty();
    }
}
