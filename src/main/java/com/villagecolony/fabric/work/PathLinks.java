package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.NetworkDetour;
import com.villagecolony.core.construction.model.PathStep;
import com.villagecolony.core.construction.model.VillagePathPlanner;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.RoadIndex;
import com.villagecolony.fabric.integration.WorldPathTerrain;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

/**
 * As ligações da rede de caminhos — Regra 123: cada construção pronta da colônia se liga pela porta ao
 * ponto mais barato da rede (decisão D5), e a primeira se liga ao centro da vila, o chafariz. Quando o
 * percurso pela rede até o chafariz passa de duas vezes o direto ({@link NetworkDetour}), a ligação é um
 * atalho direto, que ainda reaproveita a rede onde ela serve. O traçado é
 * o {@link VillagePathPlanner}; quem assenta é {@link PathPaving}.
 *
 * <p>Um traçado por colônia por vez. Construção sem porta legível, ou sem traçado possível, fica de lado
 * por {@link #RETRY_AFTER} e volta depois — o terreno muda, a rede cresce.
 */
public final class PathLinks {

    static {
        ServerMemory.register(PathLinks.class, PathLinks::clearAll);
    }

    /** De quanto em quanto tempo a colônia procura a próxima construção sem ligação. */
    static final long PLAN_EVERY = 600;

    /** Construção sem ligação possível: quanto tempo fica de lado. */
    static final long RETRY_AFTER = 12_000;

    /** A porta mais perto que isto de um caminho já está ligada. */
    static final int LINKED = 1;

    /** Um traçado em curso: os passos e o próximo a assentar. */
    static final class Plan {
        final UUID building;
        final List<PathStep> steps;
        int next;

        Plan(UUID building, List<PathStep> steps) {
            this.building = building;
            this.steps = steps;
        }

        boolean done() {
            return next >= steps.size();
        }
    }

    /** A porta de uma construção: o chão em frente a ela e o rumo para fora. */
    record Entrance(BlockPos front, Side out) {
    }

    private static final Map<UUID, Plan> PLANS = new HashMap<>();
    private static final Map<UUID, Long> PLANNED_AT = new HashMap<>();
    private static final Map<UUID, Long> SET_ASIDE = new HashMap<>();

    private PathLinks() {
    }

    /** O traçado em curso desta colônia, se ainda tem passo por assentar. */
    static Optional<Plan> planOf(UUID colonyId) {
        return Optional.ofNullable(PLANS.get(colonyId)).filter(plan -> !plan.done());
    }

    /** O mundo mudou debaixo do traçado: ele sai, e o ciclo seguinte traça de novo. */
    static void drop(UUID colonyId) {
        PLANS.remove(colonyId);
    }

    /** Uma passagem desta colônia: sem traçado em curso, procura a próxima construção sem ligação. */
    static void tend(ServerWorld world, Colony colony) {
        long now = world.getTime();

        if (planOf(colony.id()).isPresent() || now - PLANNED_AT.getOrDefault(colony.id(), Long.MIN_VALUE / 2) < PLAN_EVERY) {
            return;
        }

        PLANNED_AT.put(colony.id(), now);
        SET_ASIDE.entrySet().removeIf(entry -> now - entry.getValue() >= RETRY_AFTER);

        Set<Long> roads = roadsOf(colony.id());
        BlockPos hub = MinecraftTypeAdapter.toBlockPos(colony.center());

        for (Building building : VillageColonyMod.BUILDINGS.ofColony(colony.id())) {
            if (!building.finished() || SET_ASIDE.containsKey(building.id())) {
                continue;
            }

            Optional<Entrance> entrance = entranceOf(world, building);

            if (entrance.isEmpty() || linked(roads, entrance.get().front())) {
                continue;
            }

            Optional<List<PathStep>> steps = trace(world, colony.id(), entrance.get(), roads, hub, false);

            // Atalho ao chafariz quando a rede dá volta demais — decisão D5 (2×).
            if (steps.isPresent() && !steps.get().isEmpty() && !roads.isEmpty()) {
                PathStep join = steps.get().getLast();
                BlockPos front = entrance.get().front();
                int direct = Math.abs(front.getX() - hub.getX()) + Math.abs(front.getZ() - hub.getZ());

                if (NetworkDetour.needsShortcut(steps.get().size(),
                        NetworkDetour.stepsToHub(roads, join.x(), join.z(), hub.getX(), hub.getZ()), direct)) {
                    Optional<List<PathStep>> shortcut = trace(world, colony.id(), entrance.get(), roads, hub, true);

                    if (shortcut.isPresent() && !shortcut.get().isEmpty()) {
                        steps = shortcut;
                    }
                }
            }

            if (steps.isEmpty() || steps.get().isEmpty()) {
                SET_ASIDE.put(building.id(), now);
                VillageColonyMod.LOGGER.info("Colony {} — no path from the door of {} at {} to the network yet",
                        colony.id().toString().substring(0, 8), building.blueprint().path(),
                        entrance.get().front().toShortString());
                continue;
            }

            PLANS.put(colony.id(), new Plan(building.id(), steps.get()));
            VillageColonyMod.LOGGER.info("Colony {} traced a path of {} steps from the door of {} at {} (Regra 123)",
                    colony.id().toString().substring(0, 8), steps.get().size(), building.blueprint().path(),
                    entrance.get().front().toShortString());

            return;
        }
    }

    /** O traçado da porta até a rede, ou até o chafariz quando ainda não há rede. */
    static Optional<List<PathStep>> trace(ServerWorld world, UUID colonyId, Entrance entrance, Set<Long> roads,
                                          BlockPos hub, boolean toTheHub) {
        WorldPathTerrain terrain = new WorldPathTerrain(world, colonyId);
        BlockPos front = entrance.front();
        OptionalInt floor = terrain.groundY(front.getX(), front.getZ(), front.getY() - 1);

        if (floor.isEmpty()) {
            return Optional.empty();
        }

        VillagePathPlanner.Goal goal = new VillagePathPlanner.Goal() {
            @Override
            public boolean reached(int x, int floorY, int z) {
                return (!toTheHub && roads.contains(ColonyRoads.column(x, z)))
                        || (Math.abs(x - hub.getX()) <= 1 && Math.abs(z - hub.getZ()) <= 1);
            }

            @Override
            public int estimate(int x, int z) {
                return toTheHub || roads.isEmpty() ? Math.abs(x - hub.getX()) + Math.abs(z - hub.getZ()) : 0;
            }

            @Override
            public boolean isPath(int x, int floorY, int z) {
                return roads.contains(ColonyRoads.column(x, z));
            }
        };

        return VillagePathPlanner.plan(terrain, front.getX(), floor.getAsInt(), front.getZ(), entrance.out(), goal)
                .map(steps -> steps.isEmpty() ? steps : withTheFront(front, floor.getAsInt(), entrance.out(), steps));
    }

    /** O primeiro passo é o chão em frente à porta, que o traçado parte de cima dele. */
    private static List<PathStep> withTheFront(BlockPos front, int floorY, Side out, List<PathStep> steps) {
        List<PathStep> all = new java.util.ArrayList<>();
        all.add(new PathStep(front.getX(), floorY, front.getZ(), out, 0, 0));
        all.addAll(steps);

        return List.copyOf(all);
    }

    /** As colunas de caminho que a colônia conhece. */
    static Set<Long> roadsOf(UUID colonyId) {
        return RoadIndex.roadsOf(colonyId).map(roads -> (Set<Long>) new HashSet<>(roads.columns())).orElseGet(HashSet::new);
    }

    static boolean linked(Set<Long> roads, BlockPos front) {
        for (int dx = -LINKED; dx <= LINKED; dx++) {
            for (int dz = -LINKED; dz <= LINKED; dz++) {
                if (roads.contains(ColonyRoads.column(front.getX() + dx, front.getZ() + dz))) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * A porta da construção, lida do mundo: a metade de baixo de uma porta na borda da caixa, e o rumo
     * para o lado de fora dela. Vale também para as casas Vanilla, que não guardam a planta.
     */
    static Optional<Entrance> entranceOf(ServerWorld world, Building building) {
        BlockPos min = MinecraftTypeAdapter.toBlockPos(building.min());
        BlockPos max = MinecraftTypeAdapter.toBlockPos(building.max());

        for (BlockPos at : BlockPos.iterate(min, new BlockPos(max.getX(), Math.min(max.getY(), min.getY() + 3), max.getZ()))) {
            boolean edge = at.getX() == min.getX() || at.getX() == max.getX()
                    || at.getZ() == min.getZ() || at.getZ() == max.getZ();

            if (!edge || !world.isChunkLoaded(at)) {
                continue;
            }

            BlockState state = world.getBlockState(at);

            if (!state.isIn(BlockTags.DOORS) || state.get(DoorBlock.HALF) != DoubleBlockHalf.LOWER) {
                continue;
            }

            for (Direction out : Direction.Type.HORIZONTAL) {
                BlockPos front = at.offset(out);

                if (front.getX() < min.getX() || front.getX() > max.getX()
                        || front.getZ() < min.getZ() || front.getZ() > max.getZ()) {
                    return Optional.of(new Entrance(front.toImmutable(), sideOf(out)));
                }
            }
        }

        return Optional.empty();
    }

    private static Side sideOf(Direction direction) {
        return switch (direction) {
            case NORTH -> Side.NORTH;
            case SOUTH -> Side.SOUTH;
            case EAST -> Side.EAST;
            default -> Side.WEST;
        };
    }

    static void clearAll() {
        PLANS.clear();
        PLANNED_AT.clear();
        SET_ASIDE.clear();
    }
}
