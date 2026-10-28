package com.villagecolony.fabric.integration;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * O chão de rua da colônia — mistura gasta natural, e da vila (autor, 2026-10-28): o cascalho, a terra grossa e
 * o caminho numa coluna que a colônia assentou ninguém minera nem recolhe; o mesmo bloco fora da rua continua
 * coletável.
 */
public class StreetSurfaceGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "street_surface", tickLimit = 20)
    public void theColonyStreetIsProtectedAndItsEdgeIsWorn(TestContext context) {
        var world = context.getWorld();
        UUID colonyId = UUID.randomUUID();
        BlockPos street = context.getAbsolutePos(new BlockPos(2, 1, 2));
        BlockPos wild = context.getAbsolutePos(new BlockPos(6, 1, 6));

        context.setBlockState(new BlockPos(2, 1, 2), Blocks.GRAVEL);
        context.setBlockState(new BlockPos(6, 1, 6), Blocks.GRAVEL);
        SweepState.ROADS.put(colonyId, new com.villagecolony.core.construction.model.ColonyRoads(colonyId,
                com.villagecolony.fabric.adapter.MinecraftTypeAdapter.toColonyPos(street),
                java.util.List.of(com.villagecolony.core.construction.model.ColonyRoads.column(street.getX(), street.getZ()))));

        try {
            context.assertFalse(BlockProtection.mayBreak(world, street, world.getBlockState(street)),
                    "o cascalho da rua da colônia não pode ser recolhido");
            context.assertTrue(BlockProtection.mayBreak(world, wild, world.getBlockState(wild)),
                    "o cascalho fora da rua continua coletável");

            Set<net.minecraft.block.Block> seen = new HashSet<>();
            for (int i = 0; i < 64; i++) {
                BlockState edge = StreetSurface.edge(world, street, Blocks.GRASS_BLOCK.getDefaultState());
                context.assertTrue(StreetSurface.isStreetMaterial(edge), "borda fora da mistura: " + edge);
                seen.add(edge.getBlock());
            }
            context.assertTrue(seen.size() >= 2, "a borda devia variar entre caminho e desgaste: " + seen);
            context.assertTrue(StreetSurface.center(world, street, Blocks.GRASS_BLOCK.getDefaultState()).isOf(Blocks.DIRT_PATH),
                    "o meio da rua na grama é o caminho de terra");
        } finally {
            SweepState.ROADS.remove(colonyId);
        }

        context.complete();
    }
}
