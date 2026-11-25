package com.villagecolony.fabric.event;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.coordination.ColonyMerge;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.ColonyPathing;
import com.villagecolony.fabric.integration.RoadNetwork;
import com.villagecolony.gametest.Arena;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.Schedule;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** O gatilho da fusão de colônias — ADR-007, 2026-09-30. */
public final class ColonyMergeTriggerGameTest implements FabricGameTest {

    /**
     * Duas colônias longe pelo centro, mas com casas encostadas, viram uma.
     *
     * <p>É o gatilho original da ADR-007 §4: não é distância, é encosto de
     * bloco. Os centros ficam a 60 blocos, além da sobreposição de 32.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "colony_merge")
    public void coloniesWhoseBuildingsTouchBecomeOne(TestContext context) {
        BlockPos base = context.getAbsolutePos(new BlockPos(1, 1, 1));
        Colony west = Colony.create(UUID.randomUUID(), at(base, -30));
        Colony east = Colony.create(UUID.randomUUID(), at(base, 30));
        VillageColonyMod.COLONIES.register(west);
        VillageColonyMod.COLONIES.register(east);

        try {
            VillageColonyMod.BUILDINGS.register(new Building(UUID.randomUUID(), west.id(),
                    ResourceId.vanilla("hut"), at(base, 0), at(base, 3)));
            VillageColonyMod.BUILDINGS.register(new Building(UUID.randomUUID(), east.id(),
                    ResourceId.vanilla("hut"), at(base, 4), at(base, 7)));

            context.assertTrue(ColonyMergeTrigger.shouldMerge(context.getWorld(), west, east),
                    "as casas encostadas não disparam a fusão");

            ColonyMerge.Result result = ColonyMerge.merge(west, east,
                    VillageColonyMod.COLONIES, VillageColonyMod.WORKERS, VillageColonyMod.TASKS,
                    VillageColonyMod.CONSTRUCTIONS, VillageColonyMod.BUILDINGS, VillageColonyMod.MINES);

            context.assertTrue(VillageColonyMod.BUILDINGS.ofColony(result.survivor()).size() == 2,
                    "a colônia resultante não ficou com as duas casas");
            context.assertTrue(VillageColonyMod.COLONIES.find(result.absorbed()).isEmpty(),
                    "a colônia absorvida continua no registro");
        } finally {
            cleanUp(west, east);
        }

        context.complete();
    }

    /** Sem jogador por perto, nem colônias encostadas se juntam (ADR-039 item 3, opção B). */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "colony_merge")
    public void onlyTheAttendedColonyMerges(TestContext context) {
        BlockPos base = context.getAbsolutePos(new BlockPos(1, 1, 1));
        Colony west = Colony.create(UUID.randomUUID(), at(base, -30));
        Colony east = Colony.create(UUID.randomUUID(), at(base, 30));
        VillageColonyMod.COLONIES.register(west);
        VillageColonyMod.COLONIES.register(east);

        try {
            VillageColonyMod.BUILDINGS.register(new Building(UUID.randomUUID(), west.id(),
                    ResourceId.vanilla("hut"), at(base, 0), at(base, 3)));
            VillageColonyMod.BUILDINGS.register(new Building(UUID.randomUUID(), east.id(),
                    ResourceId.vanilla("hut"), at(base, 4), at(base, 7)));

            int unseen = ColonyMergeTrigger.mergeTouchingColonies(context.getWorld(), id -> false);

            context.assertTrue(unseen == 0 && VillageColonyMod.COLONIES.find(west.id()).isPresent()
                            && VillageColonyMod.COLONIES.find(east.id()).isPresent(),
                    "sem jogador, as colônias encostadas se juntaram");

            int seen = ColonyMergeTrigger.mergeTouchingColonies(context.getWorld(), west.id()::equals);

            context.assertTrue(seen >= 1 && (VillageColonyMod.COLONIES.find(west.id()).isEmpty()
                            || VillageColonyMod.COLONIES.find(east.id()).isEmpty()),
                    "com o jogador numa delas, as colônias encostadas não se juntaram");
        } finally {
            cleanUp(west, east);
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "colony_merge_roads", tickLimit = 360)
    public void aVillagerWalksBetweenTheTwoOldRoadNetworks(TestContext context) {
        // O destino da colônia só move o aldeão no expediente (WorkHours).
        context.getWorld().setTimeOfDay(Schedule.WORK_TIME);
        BlockPos start = context.getAbsolutePos(new BlockPos(1, 2, 1));
        BlockPos end = start.east(28);
        BlockPos low = new BlockPos(-4, 0, -5);
        BlockPos high = new BlockPos(41, 5, 7);
        Arena.forceChunks(context, low, high, true);
        Colony west = Colony.create(UUID.randomUUID(), at(start, 0));
        Colony east = Colony.create(UUID.randomUUID(), at(start, 24));
        VillageColonyMod.COLONIES.register(west);
        VillageColonyMod.COLONIES.register(east);
        List<Long> westRoad = new ArrayList<>();
        List<Long> eastRoad = new ArrayList<>();
        // Calçada de caminho: o trecho da rede só para sobre chão de rua ou natural (Regra 151). Em volta, grama no
        // mesmo nível: a rua solta um bloco acima do chão da arena derrubava quem pisava na beira.
        for (int step = -4; step <= 32; step++) {
            BlockPos pavement = start.east(step).down();
            for (int dz = -5; dz <= 5; dz++) {
                BlockPos floor = pavement.south(dz);
                boolean road = step >= -2 && step <= 30 && Math.abs(dz) <= 3;
                context.getWorld().setBlockState(floor.down(), Blocks.DIRT.getDefaultState());
                context.getWorld().setBlockState(floor,
                        road ? Blocks.DIRT_PATH.getDefaultState() : Blocks.GRASS_BLOCK.getDefaultState());
                context.getWorld().setBlockState(floor.up(), Blocks.AIR.getDefaultState());
                context.getWorld().setBlockState(floor.up(2), Blocks.AIR.getDefaultState());
            }
            if (step >= 0 && step <= 28) {
                (step < 14 ? westRoad : eastRoad).add(ColonyRoads.column(pavement.getX(), pavement.getZ()));
            }
        }
        RoadNetwork.publish(west.id(), west.center(), westRoad);
        RoadNetwork.publish(east.id(), east.center(), eastRoad);
        BlockPos spawn = start.south(3);
        var villager = ColonyFixture.spawnAtWorld(context, EntityType.VILLAGER, spawn);
        VillageColonyMod.WORKERS.register(villager.getUuid(), west.id());
        Runnable clean = () -> {
            WorkTargets.clear(villager.getUuid());
            VillageColonyMod.WORKERS.remove(villager.getUuid());
            villager.discard();
            RoadNetwork.publish(west.id(), west.center(), List.of());
            RoadNetwork.publish(east.id(), east.center(), List.of());
            cleanUp(west, east);
            Arena.forceChunks(context, low, high, false);
        };
        try {
            context.assertTrue(ColonyMergeTrigger.mergeTouchingColonies(context.getWorld(), id -> id.equals(west.id()) || id.equals(east.id())) > 0
                            && (VillageColonyMod.COLONIES.find(west.id()).isEmpty()
                            || VillageColonyMod.COLONIES.find(east.id()).isEmpty()),
                    "as duas colônias do teste devem se unir");
        } catch (RuntimeException | AssertionError failure) {
            clean.run();
            throw failure;
        }
        context.waitAndRun(10, () -> {
            villager.refreshPositionAndAngles(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, 0, 0);
            villager.setOnGround(true);
            villager.getNavigation().stop();
            BlockPos waypoint = ColonyPathing.legToward(villager, end);
            if (!waypoint.equals(start)) {
                clean.run();
                context.throwGameTestException("a rota deve entrar na rede que une as duas vilas: " + waypoint
                        + ", início " + start + ", aldeão " + villager.getBlockPos());
            }
            // Como no jogo: o destino da colônia, que o Brain segue pela rede unida (GoToWorkTargetTask).
            WorkTargets.set(villager.getUuid(), end, 1);
        });
        // Rastro da caminhada, relativo ao início: posição, alvo que o Brain segue e caminho, para explicar quem trava.
        List<String> trail = new ArrayList<>();
        context.runAtEveryTick(() -> {
            if (context.getTick() > 10 && villager.getBlockPos().isWithinDistance(end, 2)) {
                clean.run();
                context.complete();
            } else if (context.getTick() >= 340) {
                clean.run();
                String last = String.join(" | ", trail.subList(Math.max(0, trail.size() - 12), trail.size()));
                trail.forEach(step -> VillageColonyMod.LOGGER.info("VC_TEST_MERGE_WALK {}", step));
                context.throwGameTestException("o aldeão deve chegar à antiga rua da vila absorvida: "
                        + villager.getBlockPos() + "; rastro: " + last);
            } else if (context.getTick() > 10 && context.getTick() % 5 == 0) {
                var now = villager.getNavigation().getCurrentPath();
                String walk = villager.getBrain().getOptionalRegisteredMemory(MemoryModuleType.WALK_TARGET)
                        .map(target -> offset(target.getLookTarget().getBlockPos(), start)).orElse("-");
                trail.add(String.format(java.util.Locale.ROOT, "t%d pos=%.1f,%.1f,%.1f ground=%s hit=%s walk=%s"
                                + " activity=%s path=%s", context.getTick(), villager.getX() - start.getX(),
                        villager.getY() - start.getY(), villager.getZ() - start.getZ(), villager.isOnGround(),
                        villager.horizontalCollision, walk,
                        villager.getBrain().getFirstPossibleNonCoreActivity().map(Object::toString).orElse("-"),
                        now == null ? "-" : now.getCurrentNodeIndex() + "/" + now.getLength()));
            }
        });
    }

    /** Longe, sem casa encostada e sem vila gerada em comum: continuam duas. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "colony_merge")
    public void coloniesApartStayApart(TestContext context) {
        BlockPos base = context.getAbsolutePos(new BlockPos(1, 1, 1));
        Colony west = Colony.create(UUID.randomUUID(), at(base, -30));
        Colony east = Colony.create(UUID.randomUUID(), at(base, 30));
        VillageColonyMod.COLONIES.register(west);
        VillageColonyMod.COLONIES.register(east);

        try {
            VillageColonyMod.BUILDINGS.register(new Building(UUID.randomUUID(), west.id(),
                    ResourceId.vanilla("hut"), at(base, -30), at(base, -27)));
            VillageColonyMod.BUILDINGS.register(new Building(UUID.randomUUID(), east.id(),
                    ResourceId.vanilla("hut"), at(base, 27), at(base, 30)));

            context.assertTrue(!ColonyMergeTrigger.shouldMerge(context.getWorld(), west, east),
                    "colônias a 60 blocos, sem nada encostado, foram fundidas");
        } finally {
            cleanUp(west, east);
        }

        context.complete();
    }

    private static String offset(BlockPos at, BlockPos origin) {
        return (at.getX() - origin.getX()) + "," + (at.getY() - origin.getY()) + "," + (at.getZ() - origin.getZ());
    }

    private static ColonyPos at(BlockPos base, int dx) {
        return MinecraftTypeAdapter.toColonyPos(base.add(dx, 0, 0));
    }

    private static void cleanUp(Colony... colonies) {
        for (Colony colony : colonies) {
            VillageColonyMod.BUILDINGS.removeOfColony(colony.id());
            VillageColonyMod.COLONIES.remove(colony.id());
        }
    }
}
