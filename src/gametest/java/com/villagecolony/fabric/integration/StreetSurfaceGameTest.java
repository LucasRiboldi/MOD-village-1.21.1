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
 * O chão de rua da colônia é só o caminho do chão, e é da vila (autor, 2026-10-28, depois do jogo da 0.3.104: "voltar
 * para utilizar apenas blocos de caminho"): a borda é igual ao meio, o desgaste que a colônia já assentou volta a ser
 * caminho, e o bloco de uma coluna que a colônia assentou ninguém minera nem recolhe.
 */
public class StreetSurfaceGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "street_surface", tickLimit = 20)
    public void theColonyStreetIsProtectedAndItsEdgeIsThePath(TestContext context) {
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
                seen.add(edge.getBlock());
            }
            context.assertTrue(seen.equals(Set.of(Blocks.DIRT_PATH)), "a borda na grama devia ser só caminho: " + seen);
            context.assertTrue(StreetSurface.center(world, street, Blocks.GRASS_BLOCK.getDefaultState()).isOf(Blocks.DIRT_PATH),
                    "o meio da rua na grama é o caminho de terra");
        } finally {
            SweepState.ROADS.remove(colonyId);
        }

        context.complete();
    }

    /** O cascalho e a terra grossa que a colônia assentou na borda voltam a ser caminho; o de fora da rua fica. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "street_surface_unworn", tickLimit = 20)
    public void theWornEdgeTheColonyLaidBecomesThePath(TestContext context) {
        var world = context.getWorld();
        UUID colonyId = UUID.randomUUID();
        BlockPos gravel = context.getAbsolutePos(new BlockPos(2, 2, 2));
        BlockPos coarse = context.getAbsolutePos(new BlockPos(3, 2, 2));
        BlockPos wild = context.getAbsolutePos(new BlockPos(6, 2, 6));

        for (BlockPos at : new BlockPos[] {new BlockPos(2, 1, 2), new BlockPos(3, 1, 2), new BlockPos(6, 1, 6)}) {
            context.setBlockState(at, Blocks.DIRT);
        }

        context.setBlockState(new BlockPos(2, 2, 2), Blocks.GRAVEL);
        context.setBlockState(new BlockPos(3, 2, 2), Blocks.COARSE_DIRT);
        context.setBlockState(new BlockPos(6, 2, 6), Blocks.GRAVEL);
        SweepState.ROADS.put(colonyId, new com.villagecolony.core.construction.model.ColonyRoads(colonyId,
                com.villagecolony.fabric.adapter.MinecraftTypeAdapter.toColonyPos(gravel), java.util.List.of(
                        com.villagecolony.core.construction.model.ColonyRoads.column(gravel.getX(), gravel.getZ()),
                        com.villagecolony.core.construction.model.ColonyRoads.column(coarse.getX(), coarse.getZ()))));

        try {
            context.assertTrue(StreetSurface.unworn(world, gravel), "o cascalho da borda não foi trocado");
            context.assertTrue(StreetSurface.unworn(world, coarse), "a terra grossa da borda não foi trocada");
            context.assertFalse(StreetSurface.unworn(world, wild), "o cascalho fora da rua foi trocado");

            context.assertTrue(world.getBlockState(gravel).isOf(Blocks.DIRT_PATH), "cascalho: " + world.getBlockState(gravel));
            context.assertTrue(world.getBlockState(coarse).isOf(Blocks.DIRT_PATH), "terra grossa: " + world.getBlockState(coarse));
            context.assertTrue(world.getBlockState(wild).isOf(Blocks.GRAVEL), "fora da rua: " + world.getBlockState(wild));
        } finally {
            SweepState.ROADS.remove(colonyId);
        }

        context.complete();
    }
}
