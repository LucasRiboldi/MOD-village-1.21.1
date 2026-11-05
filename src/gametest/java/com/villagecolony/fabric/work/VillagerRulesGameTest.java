package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.integration.ConstructionZone;
import com.villagecolony.fabric.integration.VanillaProfessionGuard;
import com.villagecolony.fabric.integration.VillagerMeals;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.VillagerProfession;

import java.util.List;

/** O corpo do aldeão: Regras 44, 94, 161, 171, 172 e 181. Tudo em coordenada do mundo: a arena pode vir girada. */
public class VillagerRulesGameTest implements FabricGameTest {

    private static VillagerEntity standing(TestContext context, BlockPos feet) {
        context.getWorld().setBlockState(feet.down(), Blocks.STONE.getDefaultState(), 18);

        VillagerEntity villager = ColonyFixture.spawnAtWorld(context, EntityType.VILLAGER, feet);

        villager.setAiDisabled(true);

        return villager;
    }

    private static Path pathThrough(BlockPos target, BlockPos... nodes) {
        return new Path(List.of(new PathNode(nodes[0].getX(), nodes[0].getY(), nodes[0].getZ()),
                new PathNode(nodes[1].getX(), nodes[1].getY(), nodes[1].getZ()),
                new PathNode(target.getX(), target.getY(), target.getZ())), target, true);
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "villager_jump", tickLimit = 20)
    public void theHighJumpIsOnlyForObstacles(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos feet = context.getAbsolutePos(new BlockPos(1, 62, 1));
        VillagerEntity villager = standing(context, feet);

        try {
            context.assertFalse(VillagerMoves.needsHighJump(world, villager), "Regra 171: sem caminho, pulo maior");

            villager.getNavigation().startMovingAlong(pathThrough(feet.east(4), feet.east(), feet.east(2)), 0.5);
            context.assertFalse(VillagerMoves.needsHighJump(world, villager), "Regra 171: no plano, pulo maior");

            villager.getNavigation().startMovingAlong(pathThrough(feet.east(4), feet.east().up(2), feet.east(2).up(2)), 0.5);
            context.assertTrue(VillagerMoves.needsHighJump(world, villager), "Regra 171: degrau de 2 sem pulo maior");

            BlockPos fence = feet.east();

            world.setBlockState(fence, Blocks.OAK_FENCE.getDefaultState(), 18);
            villager.getNavigation().startMovingAlong(pathThrough(feet.east(5), fence.up(), feet.east(3)), 0.5);
            context.assertTrue(VillagerMoves.needsHighJump(world, villager), "Regra 171: cerca no caminho sem pulo maior");
        } finally {
            world.setBlockState(feet.east(), Blocks.AIR.getDefaultState(), 18);
            villager.discard();
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "villager_water", tickLimit = 20)
    public void openWaterNeedsMoreThanFiveBlocksAhead(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos feet = context.getAbsolutePos(new BlockPos(1, 66, 1));
        VillagerEntity villager = standing(context, feet);

        try {
            villager.refreshPositionAndAngles(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, 0, 0);
            villager.setBodyYaw(0);

            context.assertTrue(VillagerMoves.WATER_AHEAD_MIN == 6, "Regra 172: \"mais de 5 blocos\" é 6");

            for (int step = 1; step <= VillagerMoves.WATER_AHEAD_MIN; step++) {
                world.setBlockState(feet.south(step), Blocks.WATER.getDefaultState(), 18);
            }

            context.assertTrue(VillagerMoves.hasOpenWaterAhead(world, villager), "Regra 172: seis blocos de água não contaram");

            world.setBlockState(feet.south(VillagerMoves.WATER_AHEAD_MIN), Blocks.AIR.getDefaultState(), 18);
            context.assertFalse(VillagerMoves.hasOpenWaterAhead(world, villager), "Regra 172: cinco blocos de água contaram");
        } finally {
            for (int step = 1; step <= VillagerMoves.WATER_AHEAD_MIN; step++) {
                world.setBlockState(feet.south(step), Blocks.AIR.getDefaultState(), 18);
            }

            villager.discard();
        }

        context.complete();
    }

    /** O caminho do aldeão da colônia sobe 2 blocos só onde o corpo cabe (Regra 181); o controle prova que ele subiria. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "villager_ceiling", tickLimit = 40)
    public void theJumpChecksTheCeiling(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos floor = context.getAbsolutePos(new BlockPos(1, 70, 1));

        for (int x = -1; x <= 5; x++) {
            for (int z = -1; z <= 1; z++) {
                world.setBlockState(floor.add(x, 0, z), Blocks.STONE.getDefaultState(), 18);

                for (int y = 1; y <= 7; y++) {
                    boolean wall = x == -1 || x == 5 || z != 0;

                    world.setBlockState(floor.add(x, y, z), wall ? Blocks.STONE.getDefaultState() : Blocks.AIR.getDefaultState(), 18);
                }
            }
        }

        // Um degrau de 2 no corredor, e o destino em cima dele.
        BlockPos step = floor.add(3, 1, 0);
        BlockPos target = step.up(2);

        world.setBlockState(step, Blocks.STONE.getDefaultState(), 18);
        world.setBlockState(step.up(), Blocks.STONE.getDefaultState(), 18);

        ColonyFixture fixture = ColonyFixture.colonyAtWorld(floor.up());

        fixture.colony().measure(new VillageBounds(floor.getX() - 20, floor.getY() - 5, floor.getZ() - 20,
                floor.getX() + 20, floor.getY() + 10, floor.getZ() + 20));

        var worker = fixture.emptyHandedWorker(context, ProfessionType.MASON, new BlockPos(2, 2, 2));

        context.waitAndRun(10, () -> {
            try {
                VillagerEntity villager = (VillagerEntity) world.getEntity(worker.villagerId());

                context.assertTrue(villager != null, "o aldeão sumiu");
                // Fora da caixa do teste o chunk pode ainda não tiquear, e a navegação só sai de quem está no chão.
                villager.refreshPositionAndAngles(floor.getX() + 0.5, floor.getY() + 1, floor.getZ() + 0.5, 0, 0);
                villager.setOnGround(true);
                villager.getNavigation().stop();

                var open = villager.getNavigation().findPathTo(target, 0);

                context.assertTrue(open != null && open.reachesTarget(), "controle: sem teto o aldeão devia subir o degrau de 2");

                villager.getNavigation().stop();
                world.setBlockState(floor.add(2, 4, 0), Blocks.STONE.getDefaultState(), 18);

                var roofed = villager.getNavigation().findPathTo(target, 0);

                context.assertTrue(roofed == null || !roofed.reachesTarget(), "Regra 181: o caminho subiu embaixo do teto");
            } finally {
                fixture.cleanUp();

                for (var at : BlockPos.iterate(floor.add(-1, 0, -1), floor.add(5, 7, 1))) {
                    world.setBlockState(at, Blocks.AIR.getDefaultState(), 18);
                }
            }

            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "villager_trade", tickLimit = 20)
    public void aColonyWorkerHasNoVanillaTrade(TestContext context) {
        ServerWorld world = context.getWorld();
        ColonyFixture fixture = ColonyFixture.colonyAt(context, new BlockPos(1, 2, 1));
        var worker = fixture.emptyHandedWorker(context, ProfessionType.MASON, new BlockPos(2, 2, 2));
        VillagerEntity colonist = (VillagerEntity) world.getEntity(worker.villagerId());
        VillagerEntity stranger = context.spawnEntity(EntityType.VILLAGER, new BlockPos(5, 2, 5));

        try {
            colonist.setVillagerData(colonist.getVillagerData().withProfession(VillagerProfession.FARMER));
            context.assertTrue(colonist.getVillagerData().getProfession() == VillagerProfession.NONE, "Regra 44: o ofício do mod aceitou ofício Vanilla");

            stranger.setVillagerData(stranger.getVillagerData().withProfession(VillagerProfession.FARMER));
            context.assertTrue(stranger.getVillagerData().getProfession() == VillagerProfession.FARMER, "Regra 44: tirou o ofício de quem não é da colônia");

            var nitwit = colonist.getVillagerData().withProfession(VillagerProfession.NITWIT);

            context.assertTrue(VanillaProfessionGuard.filter(colonist, nitwit).getProfession() == VillagerProfession.NITWIT, "Regra 44: o nitwit deixou de ser nitwit");
        } finally {
            stranger.discard();
            fixture.cleanUp();
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "villager_meals", tickLimit = 20)
    public void eatingFillsTheHealth(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos at = context.getAbsolutePos(new BlockPos(2, 2, 2));
        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(2, 2, 2));

        try {
            villager.setHealth(5);
            VillagerMeals.picksUp(villager, new ItemEntity(world, at.getX(), at.getY(), at.getZ(), new ItemStack(Items.STONE)));
            context.assertTrue(villager.getHealth() == 5, "Regra 161: pegar pedra encheu a vida");

            VillagerMeals.picksUp(villager, new ItemEntity(world, at.getX(), at.getY(), at.getZ(), new ItemStack(Items.BREAD)));
            context.assertTrue(villager.getHealth() == villager.getMaxHealth(), "Regra 161: receber pão não encheu a vida");

            villager.setHealth(5);
            villager.eatForBreeding();
            context.assertTrue(villager.getHealth() == villager.getMaxHealth(), "Regra 161: comer para cruzar não encheu a vida");
        } finally {
            villager.discard();
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "construction_zone", tickLimit = 20)
    public void theBuildingBandFollowsTheRoad(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos road = context.getAbsolutePos(new BlockPos(1, 74, 1));

        world.setBlockState(road, Blocks.DIRT_PATH.getDefaultState(), 18);

        try {
            context.assertTrue(ConstructionZone.nearRoad(world, road.east(ConstructionZone.BAND - 1)), "Regra 94: dentro da faixa não contou");
            context.assertFalse(ConstructionZone.nearRoad(world, road.east(ConstructionZone.BAND)), "Regra 94: fora da faixa contou");

            context.assertTrue(ConstructionZone.isWeed(Blocks.OAK_SAPLING.getDefaultState()), "Regra 94: muda não tira o lote");
            context.assertTrue(ConstructionZone.isWeed(Blocks.ROSE_BUSH.getDefaultState()), "Regra 94: flor alta não tira o lote");
            context.assertTrue(ConstructionZone.isWeed(Blocks.SWEET_BERRY_BUSH.getDefaultState()), "Regra 94: arbusto não tira o lote");
            context.assertFalse(ConstructionZone.isWeed(Blocks.SHORT_GRASS.getDefaultState()), "Regra 94: grama tira o lote");
            context.assertFalse(ConstructionZone.isWeed(Blocks.POPPY.getDefaultState()), "Regra 94: flor pequena tira o lote");
        } finally {
            world.setBlockState(road, Blocks.AIR.getDefaultState(), 18);
        }

        context.complete();
    }
}
