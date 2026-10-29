package com.villagecolony.fabric.work;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

/**
 * O pedreiro alisa o chão da vila — autor, 2026-10-29: tira o calombo, tampa o buraco e deixa todo chão com degrau de no
 * máximo 1, pondo e tirando solo; casa, roça e o que não é chão ficam ({@link GroundSmoothing}).
 */
public class GroundSmoothingGameTest implements FabricGameTest {

    private static final int FLOOR = 2;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "ground_smoothing", tickLimit = 20)
    public void theMasonFlattensBumpsFillsHolesAndLeavesTheRest(TestContext context) {
        ServerWorld world = context.getWorld();

        for (int x = 0; x <= 14; x++) {
            for (int z = 0; z <= 14; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.DIRT);
                context.setBlockState(new BlockPos(x, FLOOR, z), Blocks.GRASS_BLOCK);

                for (int y = FLOOR + 1; y <= FLOOR + 20; y++) {
                    context.setBlockState(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }

        // Calombo de 3, buraco de 2 x 1 com 2 de fundo, um pilar que não é chão e uma terra arada.
        for (int y = FLOOR + 1; y <= FLOOR + 3; y++) {
            context.setBlockState(new BlockPos(4, y, 4), Blocks.DIRT);
        }

        context.setBlockState(new BlockPos(4, FLOOR + 3, 4), Blocks.GRASS_BLOCK);

        for (int z = 10; z <= 11; z++) {
            context.setBlockState(new BlockPos(10, FLOOR, z), Blocks.AIR);
            context.setBlockState(new BlockPos(10, FLOOR - 1, z), Blocks.AIR);
            context.setBlockState(new BlockPos(10, FLOOR - 2, z), Blocks.DIRT);
        }

        for (int y = FLOOR + 1; y <= FLOOR + 3; y++) {
            context.setBlockState(new BlockPos(7, y, 7), Blocks.COBBLESTONE);
        }

        context.setBlockState(new BlockPos(12, FLOOR, 3), Blocks.FARMLAND);

        BlockPos a = context.getAbsolutePos(new BlockPos(0, 0, 0));
        BlockPos b = context.getAbsolutePos(new BlockPos(14, 0, 14));
        int minX = Math.min(a.getX(), b.getX());
        int minZ = Math.min(a.getZ(), b.getZ());

        for (int round = 0; round < 10 && GroundSmoothing.smoothWindow(world, null, minX, minZ, 15, 64) > 0; round++) {
            // Até não sobrar o que alisar.
        }

        int floorY = context.getAbsolutePos(new BlockPos(0, FLOOR, 0)).getY();

        context.assertTrue(top(context, 4, 4) == floorY, "o calombo não desceu: topo em " + top(context, 4, 4));
        context.assertTrue(top(context, 10, 10) == floorY && top(context, 10, 11) == floorY,
                "o buraco não foi tampado: " + top(context, 10, 10) + ", " + top(context, 10, 11));
        context.assertTrue(world.getBlockState(context.getAbsolutePos(new BlockPos(7, FLOOR + 3, 7))).isOf(Blocks.COBBLESTONE),
                "o pilar, que não é chão, foi mexido");
        context.assertTrue(world.getBlockState(context.getAbsolutePos(new BlockPos(12, FLOOR, 3))).isOf(Blocks.FARMLAND),
                "a terra arada foi mexida");
        context.complete();
    }

    /**
     * O2 (autor, 2026-10-29): o construtor com a obra parada limpa e nivela o lote — sem tarefa, ele alisa o chão em
     * volta da obra aberta.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "ground_smoothing_builder", tickLimit = 20)
    public void theIdleBuilderLevelsTheGroundAroundTheOpenBuild(TestContext context) {
        ServerWorld world = context.getWorld();

        for (int x = 0; x <= 14; x++) {
            for (int z = 0; z <= 14; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.DIRT);
                context.setBlockState(new BlockPos(x, FLOOR, z), Blocks.GRASS_BLOCK);

                for (int y = FLOOR + 1; y <= FLOOR + 20; y++) {
                    context.setBlockState(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }

        for (int y = FLOOR + 1; y <= FLOOR + 3; y++) {
            context.setBlockState(new BlockPos(4, y, 4), Blocks.DIRT);
        }

        BlockPos site = context.getAbsolutePos(new BlockPos(7, FLOOR + 1, 7));
        com.villagecolony.core.colony.model.Colony colony = com.villagecolony.core.colony.model.Colony.create(
                java.util.UUID.randomUUID(), com.villagecolony.fabric.adapter.MinecraftTypeAdapter.toColonyPos(site));
        com.villagecolony.VillageColonyMod.COLONIES.register(colony);
        com.villagecolony.core.construction.model.ConstructionProject project =
                com.villagecolony.core.construction.model.ConstructionProject.plan(colony.id(),
                        com.villagecolony.core.construction.model.Blueprint.of(
                                com.villagecolony.core.type.ResourceId.vanilla("o2_test"),
                                java.util.List.of(new com.villagecolony.core.construction.model.BlueprintBlock(
                                        new com.villagecolony.core.type.ColonyPos(0, 0, 0),
                                        com.villagecolony.fabric.adapter.MinecraftTypeAdapter.toResourceId(Blocks.OAK_PLANKS)))),
                        com.villagecolony.fabric.adapter.MinecraftTypeAdapter.toColonyPos(site));
        project.moveTo(com.villagecolony.core.construction.model.ConstructionState.PREPARING);
        project.moveTo(com.villagecolony.core.construction.model.ConstructionState.BUILDING);
        com.villagecolony.VillageColonyMod.CONSTRUCTIONS.register(project);
        net.minecraft.entity.passive.VillagerEntity builder =
                context.spawnEntity(net.minecraft.entity.EntityType.VILLAGER, new BlockPos(10, FLOOR + 1, 10));

        try {
            for (int round = 0; round < 10; round++) {
                VillageHelpers.helps(world, colony, java.util.Optional.of(
                        com.villagecolony.core.worker.model.ProfessionType.BUILDER), builder);
            }

            int floorY = context.getAbsolutePos(new BlockPos(0, FLOOR, 0)).getY();

            context.assertTrue(top(context, 4, 4) == floorY,
                    "o construtor parado não nivelou o lote: o calombo ficou com topo em " + top(context, 4, 4));
        } finally {
            com.villagecolony.VillageColonyMod.CONSTRUCTIONS.forget(project.id(),
                    com.villagecolony.core.construction.service.RemovalAudit.playerCancellation());
            com.villagecolony.VillageColonyMod.COLONIES.remove(colony.id());
        }

        context.complete();
    }

    private static int top(TestContext context, int x, int z) {
        BlockPos at = context.getAbsolutePos(new BlockPos(x, 0, z));

        return context.getWorld().getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ()) - 1;
    }
}
