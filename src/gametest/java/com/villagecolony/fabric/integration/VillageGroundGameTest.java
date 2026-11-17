package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.VillageBlend;
import com.villagecolony.core.construction.model.VillageSurface;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.UpgradeData;

import java.util.EnumSet;
import java.util.List;

/**
 * A árvore em pé sobre o chão cortado sai inteira — autor, 2026-10-29, depois do jogo da 0.3.108 ("deixou partes
 * voando"; escolha: tirar inteira, tronco e folhas). A árvore vizinha, que não encosta nela, fica.
 */
public class VillageGroundGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_ground_safety", tickLimit = 5)
    public void aBuriedChestRejectsTheWholeTerrainCut(TestContext context) {
        BlockPos at = context.getAbsolutePos(new BlockPos(2, 3, 2));
        context.setBlockState(new BlockPos(2, 1, 2), Blocks.DIRT);
        context.setBlockState(new BlockPos(2, 2, 2), Blocks.CHEST);
        context.setBlockState(new BlockPos(2, 3, 2), Blocks.GRASS_BLOCK);
        context.assertFalse(VillageGround.safeColumn(context.getWorld().getChunk(at), at.getX(), at.getZ(),
                at.getY(), at.getY() - 2, false), "o corte aceitou baú enterrado");
        context.complete();
    }

    private static void tree(ServerWorld world, BlockPos base, int height) {
        for (int dy = 0; dy < height; dy++) {
            world.setBlockState(base.up(dy), Blocks.OAK_LOG.getDefaultState(), Block.FORCE_STATE);
        }

        for (BlockPos leaf : BlockPos.iterate(base.add(-2, height - 2, -2), base.add(2, height, 2))) {
            if (world.getBlockState(leaf).isAir()) {
                world.setBlockState(leaf.toImmutable(), Blocks.OAK_LEAVES.getDefaultState(), Block.FORCE_STATE);
            }
        }
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_ground", tickLimit = 5)
    public void aTreeOnCutGroundIsRemovedWhole(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos cut = context.getAbsolutePos(new BlockPos(2, 2, 2));
        BlockPos neighbour = context.getAbsolutePos(new BlockPos(9, 2, 2));

        tree(world, cut, 5);
        tree(world, neighbour, 5);

        try {
            int removed = VillageGround.removeTree(world, cut);

            context.assertTrue(removed == 5 + 5 * 5 * 3 - 2, "saíram " + removed + " blocos, não a árvore inteira");

            for (BlockPos at : BlockPos.iterate(cut.add(-2, 0, -2), cut.add(2, 5, 2))) {
                context.assertTrue(world.getBlockState(at).isAir(), "sobrou " + world.getBlockState(at).getBlock()
                        + " em " + at.toShortString());
            }

            context.assertTrue(world.getBlockState(neighbour).isOf(Blocks.OAK_LOG), "a árvore vizinha saiu junto");
            context.assertTrue(world.getBlockState(neighbour.add(0, 4, 0)).isOf(Blocks.OAK_LOG),
                    "o alto da árvore vizinha saiu junto");
        } finally {
            for (BlockPos at : BlockPos.iterate(cut.add(-2, 0, -2), neighbour.add(2, 5, 2))) {
                world.setBlockState(at, Blocks.AIR.getDefaultState(), Block.FORCE_STATE);
            }
        }

        context.complete();
    }

    /** O que não é árvore — o chão, a casa — nunca sai pela busca da árvore. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_ground", tickLimit = 5)
    public void onlyLogsAndLeavesLeave(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos cut = context.getAbsolutePos(new BlockPos(2, 2, 2));

        tree(world, cut, 4);
        world.setBlockState(cut.add(1, 0, 0), Blocks.OAK_PLANKS.getDefaultState(), Block.FORCE_STATE);
        world.setBlockState(cut.down(), Blocks.DIRT.getDefaultState(), Block.FORCE_STATE);

        try {
            VillageGround.removeTree(world, cut);

            context.assertTrue(world.getBlockState(cut.add(1, 0, 0)).isOf(Blocks.OAK_PLANKS), "a tábua encostada saiu");
            context.assertTrue(world.getBlockState(cut.down()).isOf(Blocks.DIRT), "o chão embaixo do tronco saiu");
            context.assertTrue(world.getBlockState(cut.up(2)).isAir(), "o tronco ficou");
        } finally {
            for (BlockPos at : BlockPos.iterate(cut.add(-2, -1, -2), cut.add(2, 4, 2))) {
                world.setBlockState(at, Blocks.AIR.getDefaultState(), Block.FORCE_STATE);
            }
        }

        context.complete();
    }

    /** Flor e muda no topo não seguram a coluna: o corte as leva junto (Regra 251). */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_ground_height", tickLimit = 5)
    public void aFlowerOnTheTopDoesNotHoldTheColumnUp(TestContext context) {
        ServerWorld world = context.getWorld();
        ProtoChunk chunk = new ProtoChunk(new ChunkPos(90_001, 90_001), UpgradeData.NO_UPGRADE_DATA, world,
                world.getRegistryManager().get(RegistryKeys.BIOME), null);
        int x = chunk.getPos().getStartX();
        int z = chunk.getPos().getStartZ();
        net.minecraft.block.Block[] plants = {Blocks.POPPY, Blocks.OAK_SAPLING, Blocks.CORNFLOWER};

        for (int dx = 0; dx < plants.length; dx++) {
            for (int y = 60; y < 70; y++) {
                chunk.setBlockState(new BlockPos(x + dx, y, z), Blocks.DIRT.getDefaultState(), false);
            }

            chunk.setBlockState(new BlockPos(x + dx, 70, z), Blocks.GRASS_BLOCK.getDefaultState(), false);
            chunk.setBlockState(new BlockPos(x + dx, 71, z), plants[dx].getDefaultState(), false);

            int top = VillageGround.topOf(chunk, x + dx, z, 80);

            context.assertTrue(top == 70, plants[dx] + " segurou o topo em " + top + ", não em 70");
        }

        context.complete();
    }

    /** O acessório da vila é enfeite: não recebe entrada (Regra 252). */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_ground_height", tickLimit = 5)
    public void anAccessoryPieceIsAnOrnament(TestContext context) {
        context.assertTrue(com.villagecolony.fabric.world.GeneratedVillages.isOrnament(
                "village/plains/houses/plains_accessory_1"), "o acessório da planície não é enfeite");
        context.assertFalse(com.villagecolony.fabric.world.GeneratedVillages.isStructure(
                "village/plains/houses/plains_accessory_1"), "o acessório da planície recebe entrada");
        context.assertTrue(com.villagecolony.fabric.world.GeneratedVillages.isStructure(
                "village/plains/houses/plains_small_house_4"), "a casa deixou de receber entrada");
        context.complete();
    }

    /**
     * A rua do Vanilla segue {@code WORLD_SURFACE_WG}, que o chunk em geração não acompanha: a coluna cortada ou
     * aterrada leva a altura junto, ou a rua nasce na altura antiga, no ar ou enterrada (Regra 250).
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_ground_height", tickLimit = 5)
    public void theStreetHeightFollowsTheShapedGround(TestContext context) {
        ServerWorld world = context.getWorld();
        // Um chunk solto, só em memória, no status em que as features da vila rodam.
        ProtoChunk chunk = new ProtoChunk(new ChunkPos(90_000, 90_000), UpgradeData.NO_UPGRADE_DATA, world,
                world.getRegistryManager().get(RegistryKeys.BIOME), null);
        int x = chunk.getPos().getStartX();
        int z = chunk.getPos().getStartZ();

        for (int dx = 0; dx < 2; dx++) {
            for (int y = 60; y < 70; y++) {
                chunk.setBlockState(new BlockPos(x + dx, y, z), Blocks.DIRT.getDefaultState(), false);
            }

            chunk.setBlockState(new BlockPos(x + dx, 70, z), Blocks.GRASS_BLOCK.getDefaultState(), false);
        }

        Heightmap.populateHeightmaps(chunk, EnumSet.of(Heightmap.Type.WORLD_SURFACE_WG, Heightmap.Type.OCEAN_FLOOR_WG));
        chunk.setStatus(ChunkStatus.CARVERS);

        VillageGround.column(world, chunk, x, z, 70, 66, false);
        VillageGround.column(world, chunk, x + 1, z, 70, 73, false);

        for (Heightmap.Type type : List.of(Heightmap.Type.WORLD_SURFACE_WG, Heightmap.Type.OCEAN_FLOOR_WG)) {
            context.assertTrue(chunk.sampleHeightmap(type, 0, 0) == 66,
                    type + " da coluna cortada ficou em " + chunk.sampleHeightmap(type, 0, 0) + ", não em 66");
            context.assertTrue(chunk.sampleHeightmap(type, 1, 0) == 73,
                    type + " da coluna aterrada ficou em " + chunk.sampleHeightmap(type, 1, 0) + ", não em 73");
        }

        context.complete();
    }

    /**
     * Regra 288: dentro da vila, a coluna com água vira chão até a superfície da água, com o solo seco do próprio
     * chunk por cima — também quando a conta da vila a quer mais baixa que a água.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_ground_height", tickLimit = 5)
    public void theWaterInsideTheVillageBecomesGround(TestContext context) {
        ServerWorld world = context.getWorld();
        ProtoChunk chunk = new ProtoChunk(new ChunkPos(90_002, 90_002), UpgradeData.NO_UPGRADE_DATA, world,
                world.getRegistryManager().get(RegistryKeys.BIOME), null);
        int x = chunk.getPos().getStartX();
        int z = chunk.getPos().getStartZ();

        // Duas colunas de lago (leito de areia em 59, água de 60 a 62) e uma seca, de grama, ao lado.
        for (int dx = 0; dx < 2; dx++) {
            for (int y = 50; y < 59; y++) {
                chunk.setBlockState(new BlockPos(x + dx, y, z), Blocks.STONE.getDefaultState(), false);
            }

            chunk.setBlockState(new BlockPos(x + dx, 59, z), Blocks.SAND.getDefaultState(), false);

            for (int y = 60; y <= 62; y++) {
                chunk.setBlockState(new BlockPos(x + dx, y, z), Blocks.WATER.getDefaultState(), false);
            }
        }

        for (int y = 50; y < 62; y++) {
            chunk.setBlockState(new BlockPos(x + 3, y, z), Blocks.DIRT.getDefaultState(), false);
        }

        chunk.setBlockState(new BlockPos(x + 3, 62, z), Blocks.GRASS_BLOCK.getDefaultState(), false);
        Heightmap.populateHeightmaps(chunk, EnumSet.of(Heightmap.Type.WORLD_SURFACE_WG, Heightmap.Type.OCEAN_FLOOR_WG));
        chunk.setStatus(ChunkStatus.CARVERS);

        VillageGround.column(world, chunk, x, z, VillageGround.topOf(chunk, x, z, 80), 62, true);
        VillageGround.column(world, chunk, x + 1, z, VillageGround.topOf(chunk, x + 1, z, 80), 58, true);

        for (int dx = 0; dx < 2; dx++) {
            for (int y = 60; y <= 62; y++) {
                context.assertTrue(chunk.getFluidState(new BlockPos(x + dx, y, z)).isEmpty(),
                        "a água ficou na coluna " + dx + " em y=" + y);
            }

            context.assertTrue(chunk.getBlockState(new BlockPos(x + dx, 62, z)).isOf(Blocks.GRASS_BLOCK),
                    "o topo da coluna " + dx + " não é o solo seco do chunk");
            context.assertTrue(chunk.getBlockState(new BlockPos(x + dx, 61, z)).isOf(Blocks.DIRT),
                    "o meio da coluna " + dx + " não é o subsolo do chunk");
            context.assertTrue(chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE_WG, dx, 0) == 62,
                    "a altura de geração da coluna " + dx + " não acompanhou o chão novo");
        }

        context.complete();
    }

    /**
     * Regra 288: a peça fixa (casa, lampião) sobre água ganha base até o piso dela; o lago dentro da vila é chão, e o
     * de fora fica.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_ground_height", tickLimit = 5)
    public void aPieceOverWaterGetsAFoundation(TestContext context) {
        VillageSurface.Kind free = VillageSurface.Kind.FREE;
        VillageSurface.Kind piece = VillageSurface.Kind.FIXED;
        VillageSurface.Kind[][] kinds = {{free, free, free}, {free, piece, free}, {free, free, free}};
        int[][] natural = {{62, 62, 62}, {62, 63, 62}, {62, 62, 62}};
        VillageBlend.Area core = new VillageBlend.Area(0, 0, 2, 2);
        VillageGround.Plan plan = new VillageGround.Plan(0, 0, kinds, new boolean[3][3],
                VillageBlend.shape(natural, kinds, core, core), java.util.Map.of(), java.util.Map.of());

        context.assertTrue(plan.founds(1, 1), "a peça sobre a água fica sem base");
        context.assertFalse(plan.founds(0, 0), "o chão livre virou base de peça");
        context.assertTrue(plan.targetFor(1, 1, 62) == 63, "a base não sobe até o piso da peça");
        context.assertTrue(VillageGround.waterKind(true, true) == VillageSurface.Kind.FREE,
                "o lago dentro da vila ficou água");
        context.assertTrue(VillageGround.waterKind(true, false) == VillageSurface.Kind.WATER,
                "o lago fora da vila virou chão");
        context.assertTrue(VillageGround.waterKind(false, false) == VillageSurface.Kind.FREE,
                "o chão seco fora da vila deixou de ser moldado");
        context.complete();
    }

    /** Regra 292: na geração, a coluna com minério desce até o alvo como as outras, e não fica de torre. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_ground_height", tickLimit = 5)
    public void theGenerationCutsAColumnWithOre(TestContext context) {
        ServerWorld world = context.getWorld();
        ProtoChunk chunk = new ProtoChunk(new ChunkPos(90_003, 90_003), UpgradeData.NO_UPGRADE_DATA, world,
                world.getRegistryManager().get(RegistryKeys.BIOME), null);
        int x = chunk.getPos().getStartX();
        int z = chunk.getPos().getStartZ();

        for (int y = 55; y < 70; y++) {
            chunk.setBlockState(new BlockPos(x, y, z), Blocks.DIRT.getDefaultState(), false);
        }

        chunk.setBlockState(new BlockPos(x, 66, z), Blocks.COPPER_ORE.getDefaultState(), false);
        chunk.setBlockState(new BlockPos(x, 70, z), Blocks.GRASS_BLOCK.getDefaultState(), false);
        Heightmap.populateHeightmaps(chunk, EnumSet.of(Heightmap.Type.WORLD_SURFACE_WG, Heightmap.Type.OCEAN_FLOOR_WG));
        chunk.setStatus(ChunkStatus.CARVERS);

        VillageGround.column(world, chunk, x, z, 70, 62, false);

        context.assertTrue(chunk.getBlockState(new BlockPos(x, 66, z)).isAir(),
                "a coluna com minério ficou de pé, como torre");
        context.assertTrue(chunk.getBlockState(new BlockPos(x, 62, z)).isOf(Blocks.GRASS_BLOCK),
                "a coluna com minério não desceu até o alvo");
        context.complete();
    }
}
