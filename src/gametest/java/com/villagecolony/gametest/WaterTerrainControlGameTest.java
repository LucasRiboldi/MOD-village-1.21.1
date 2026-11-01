package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ColonyPathing;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Controle de terreno sem inferir causalidade da escolha de rota — ADR-035 §7. */
public class WaterTerrainControlGameTest implements FabricGameTest {
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "water_terrain_control", tickLimit = 40)
    public void externalFloorAbsentPresentRemovedKeepsTheProbeInvariant(TestContext context) {
        runControl(context, false, false);
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "water_budget_control", tickLimit = 40)
    public void legacyWaterGeometryGatesTheBudgetComparisonOnReproducingBad(TestContext context) {
        runControl(context, true, false);
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "water_open_edge_control", tickLimit = 40)
    public void originalFloorWithOpenEdgeVariesOnlyTheExternalBottom(TestContext context) {
        runControl(context, false, true);
    }

    private static void runControl(TestContext context, boolean budgetControl, boolean openEdge) {
        var world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(BlockPos.ORIGIN);
        Map<BlockPos, BlockState> saved = new HashMap<>();
        ColonyFixture owned = ColonyFixture.create();
        VillagerEntity villager = EntityType.VILLAGER.create(world);
        context.assertTrue(villager != null, "não criou aldeão de controle");
        Runnable cleanup = () -> {
            villager.getNavigation().resetRangeMultiplier();
            villager.discard();
            owned.cleanUp();
            saved.forEach((at, state) -> world.setBlockState(at, state, Block.NOTIFY_LISTENERS));
        };
        try {
            for (BlockPos at : BlockPos.iterate(origin.add(-2, -4, -2), origin.add(20, 5, 10))) {
                BlockPos key = at.toImmutable();
                saved.put(key, world.getBlockState(key));
                BlockPos relative = key.subtract(origin);
                if (openEdge) {
                    // Piso original e barreira observada no log; sem teto ou fechamento inferior — ADR-035 §7.
                    Block block = Blocks.AIR;
                    int x = relative.getX();
                    int y = relative.getY();
                    int z = relative.getZ();
                    if (x >= 0 && x <= 14 && z >= 0 && z <= 8 && y == 1) block = Blocks.STONE;
                    if (x >= 0 && x <= 12 && z == 3 && y >= 0 && y <= 1) block = Blocks.WATER;
                    if (x == -1 && z >= 0 && z <= 8 && y >= 1 && y <= 4) block = Blocks.BARRIER;
                    if (x >= 15 && x <= 18 && z >= 0 && z <= 4 && y == 1) block = Blocks.STONE;
                    BlockState state = relative.equals(BlockPos.ORIGIN) ? saved.get(key) : block.getDefaultState();
                    world.setBlockState(key, state, Block.NOTIFY_LISTENERS);
                    continue;
                }
                if (relative.getX() < -1 || relative.getX() > 19 || relative.getZ() < -1
                        || relative.getZ() > 9 || relative.getY() < -1) continue;
                boolean boundary = relative.getX() == -1 || relative.getX() == 19
                        || relative.getZ() == -1 || relative.getZ() == 9
                        || relative.getY() == -1 || relative.getY() == 5;
                Block block = boundary ? Blocks.BARRIER : relative.getY() == 0 ? Blocks.STONE : Blocks.AIR;
                if (!boundary && relative.getY() == 1 && relative.getX() <= 14) {
                    block = relative.getZ() == 3 && relative.getX() <= 12 ? Blocks.WATER : Blocks.STONE;
                }
                if (budgetControl) {
                    if (relative.getY() == 0) block = Blocks.AIR;
                    if (!boundary && relative.getZ() == 3 && relative.getX() <= 12
                            && relative.getY() >= 0 && relative.getY() <= 1) block = Blocks.WATER;
                    if (!boundary && relative.getX() >= 15 && relative.getX() <= 18
                            && relative.getZ() <= 4 && relative.getY() == 1) block = Blocks.STONE;
                }
                world.setBlockState(key, block.getDefaultState(), Block.NOTIFY_LISTENERS);
            }
            Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(origin.add(0, 2, 1)));
            VillageColonyMod.COLONIES.register(colony);
            owned.owning(colony);
            villager.refreshPositionAndAngles(origin.getX() + 0.5, origin.getY() + 2.25,
                    origin.getZ() + 1.5, 0, 0);
            villager.setBreedingAge(0);
            context.assertTrue(world.spawnEntity(villager), "mundo recusou aldeão de controle");
            VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id()).assign(ProfessionType.MASON);
            owned.owning(villager.getUuid());
            villager.setPathfindingPenalty(PathNodeType.WATER, ColonyPathing.WATER_COST);
            villager.setPathfindingPenalty(ColonyPathing.OFF_STREET, ColonyPathing.OFF_STREET_COST);
        } catch (RuntimeException | AssertionError failure) {
            cleanup.run();
            throw failure;
        }
        int initialAge = villager.age;
        double initialY = villager.getY();
        int[] settlingChecks = {0};
        boolean[] finished = {false};
        context.runAtEveryTick(() -> {
            if (finished[0]) return;
            settlingChecks[0]++;
            if ((!villager.isOnGround() || villager.age <= initialAge) && settlingChecks[0] < 20) return;
            finished[0] = true;
            try {
                BlockPos support = villager.getBlockPos().down();
                VillageColonyMod.LOGGER.info(
                        "WATER_ABA_SETTLE uuid={} checks={} ageBefore={} ageAfter={} yBefore={} yAfter={} ground={} support={}",
                        villager.getUuid(), settlingChecks[0], initialAge, villager.age, initialY, villager.getY(),
                        villager.isOnGround(), world.getBlockState(support));
                context.assertTrue(villager.age > initialAge
                                && villager.getY() < initialY && villager.isOnGround()
                                && world.getBlockState(support).isSideSolidFullSquare(world, support, Direction.UP)
                                && world.isSpaceEmpty(villager, villager.getBoundingBox()),
                        "controle não assentou por ticks físicos sobre apoio sólido livre de colisão");
                // AI desabilitada bloqueia travel; congelar só após assentamento normal — ADR-035 §7.
                villager.setAiDisabled(true);
                var position = villager.getPos();
                var velocity = villager.getVelocity();
                long time = world.getTime();
                int age = villager.age;
                Map<PathNodeType, Float> penalties = new EnumMap<>(PathNodeType.class);
                for (PathNodeType type : PathNodeType.values()) {
                    penalties.put(type, villager.getPathfindingPenalty(type));
                }
                Map<BlockPos, BlockState> baseline = new HashMap<>();
                saved.keySet().forEach(at -> baseline.put(at, world.getBlockState(at)));
                float[] budgets = {1.0f, 4.0f, 16.0f, 1.0f};
                boolean reproducedOpenBad = false;
                for (int phase = 0; phase < (budgetControl ? budgets.length : 3); phase++) {
                    boolean present = phase == 1;
                    for (int x = 15; !budgetControl && x <= 18; x++) {
                        for (int z = 0; z <= (openEdge ? 8 : 4); z++) {
                            world.setBlockState(origin.add(x, openEdge ? 0 : 1, z),
                                    (present ? Blocks.STONE : Blocks.AIR).getDefaultState(), Block.NOTIFY_LISTENERS);
                        }
                    }
                    if (budgetControl) {
                        baseline.forEach((at, state) -> context.assertTrue(world.getBlockState(at).equals(state),
                                "terreno mudou durante controle de orçamento em " + at));
                        villager.getNavigation().setRangeMultiplier(budgets[phase]);
                    } else {
                        assertTerrain(context, origin, baseline, present, openEdge);
                    }
                    villager.getNavigation().stop();
                    // Mesmo domínio nas três fases, incluindo a faixa externa — ADR-035 §7.
                    for (BlockPos at : BlockPos.iterate(origin.add(-2, -4, -2), origin.add(20, 5, 10))) {
                        world.getPathNodeTypeCache().invalidate(at);
                    }
                    Path path = villager.getNavigation().findPathTo(origin.add(0, 2, 7), 0);
                    logRoute(origin, openEdge ? "open-bottom-" + phase + "-present=" + present
                            : budgetControl ? "budget-" + phase + "-x" + budgets[phase]
                            : phase == 0 ? "A1-absent" : present ? "B-present" : "A2-removed", path, villager, time);
                    if (openEdge && isBad(path)) {
                        reproducedOpenBad = true;
                        for (float multiplier : budgets) {
                            villager.getNavigation().setRangeMultiplier(multiplier);
                            villager.getNavigation().stop();
                            for (BlockPos at : BlockPos.iterate(origin.add(-2, -4, -2), origin.add(20, 5, 10))) {
                                world.getPathNodeTypeCache().invalidate(at);
                            }
                            Path budgetPath = villager.getNavigation().findPathTo(origin.add(0, 2, 7), 0);
                            logRoute(origin, "open-bottom-" + phase + "-budget-x" + multiplier,
                                    budgetPath, villager, time);
                            context.assertTrue(villager.getWorld() == world && villager.getPos().equals(position)
                                            && villager.getVelocity().equals(velocity) && villager.isOnGround()
                                            && world.getTime() == time && villager.age == age,
                                    "orçamento alterou estado físico ou tique do aldeão");
                            penalties.forEach((type, penalty) -> context.assertTrue(
                                    Float.compare(villager.getPathfindingPenalty(type), penalty) == 0,
                                    "orçamento alterou penalidade " + type));
                            assertTerrain(context, origin, baseline, present, true);
                        }
                        villager.getNavigation().resetRangeMultiplier();
                    }
                    context.assertTrue(villager.getWorld() == world && villager.getPos().equals(position)
                                    && villager.getVelocity().equals(velocity) && villager.isOnGround()
                                    && world.getTime() == time && villager.age == age,
                            "consulta alterou mundo, posição, velocidade, apoio ou tique do mesmo aldeão");
                    penalties.forEach((type, penalty) -> context.assertTrue(
                            Float.compare(villager.getPathfindingPenalty(type), penalty) == 0,
                            "consulta alterou penalidade " + type));
                    if (budgetControl) {
                        baseline.forEach((at, state) -> context.assertTrue(world.getBlockState(at).equals(state),
                                "consulta mudou terreno em " + at));
                        if (phase == 0) {
                            boolean bad = path != null && path.reachesTarget() && path.getLength() == 7;
                            boolean water = false;
                            if (path != null) {
                                for (int i = 0; i < path.getLength(); i++) {
                                    if (path.getNode(i).type == PathNodeType.WATER) water = true;
                                }
                            }
                            VillageColonyMod.LOGGER.info("WATER_BUDGET reproducedBad={} time={} uuid={}",
                                    bad && water, time, villager.getUuid());
                            if (!bad || !water) break;
                        }
                    } else {
                        assertTerrain(context, origin, baseline, present, openEdge);
                    }
                }
                if (openEdge) {
                    VillageColonyMod.LOGGER.info("WATER_OPEN_BOTTOM reproducedBad={} result={} time={} uuid={}",
                            reproducedOpenBad, reproducedOpenBad ? "compare-logged-routes" : "inconclusive-no-bad",
                            time, villager.getUuid());
                }
            } finally {
                cleanup.run();
            }
            context.complete();
        });
    }

    private static void assertTerrain(TestContext context, BlockPos origin,
                                      Map<BlockPos, BlockState> baseline, boolean present, boolean openEdge) {
        baseline.forEach((at, initial) -> {
            BlockPos relative = at.subtract(origin);
            boolean external = relative.getX() >= 15 && relative.getX() <= 18
                    && relative.getZ() >= 0 && relative.getZ() <= (openEdge ? 8 : 4)
                    && relative.getY() == (openEdge ? 0 : 1);
            BlockState expected = external ? (present ? Blocks.STONE : Blocks.AIR).getDefaultState() : initial;
            context.assertTrue(context.getWorld().getBlockState(at).equals(expected),
                    "mundo fora do controle esperado em " + relative.toShortString());
        });
    }

    private static boolean isBad(Path path) {
        if (path == null || !path.reachesTarget() || path.getLength() != 7) return false;
        for (int i = 0; i < path.getLength(); i++) {
            if (path.getNode(i).type == PathNodeType.WATER) return true;
        }
        return false;
    }

    private static void logRoute(BlockPos origin, String phase, Path path, VillagerEntity villager, long time) {
        if (path == null) {
            VillageColonyMod.LOGGER.info("WATER_ABA phase={} uuid={} time={} pos={} waterPenalty={} path=null",
                    phase, villager.getUuid(), time, villager.getPos(), villager.getPathfindingPenalty(PathNodeType.WATER));
            return;
        }
        StringBuilder nodes = new StringBuilder();
        int water = 0;
        double length = 0;
        double penalties = 0;
        for (int i = 0; i < path.getLength(); i++) {
            var node = path.getNode(i);
            if (node.type == PathNodeType.WATER) water++;
            penalties += node.penalty;
            if (i > 0) length += path.getNode(i - 1).getDistance(node);
            nodes.append(node.getBlockPos().subtract(origin).toShortString()).append('/')
                    .append(node.type).append('/').append(node.penalty).append(';');
        }
        VillageColonyMod.LOGGER.info(
                "WATER_ABA phase={} uuid={} time={} pos={} waterPenalty={} reached={} n={} length={} penalties={} water={} nodes={}",
                phase, villager.getUuid(), time, villager.getPos(), villager.getPathfindingPenalty(PathNodeType.WATER),
                path.reachesTarget(), path.getLength(), length, penalties, water, nodes);
    }
}
