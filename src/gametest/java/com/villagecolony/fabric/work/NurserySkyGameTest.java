package com.villagecolony.fabric.work;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

/**
 * Regra 365 (nota do jogo da 0.4.143, H3 🅰️: "não pode nascer bloco com rebento do mod dentro da mina"): o viveiro só
 * nasce onde a muda vê o céu. No túnel da mina, na caverna e debaixo de laje não se planta.
 */
public class NurserySkyGameTest implements FabricGameTest {

    /** Areia num túnel, com arenito por cima: nem a busca aceita o lugar, nem o plantio direto planta. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "nursery_sky_tunnel", tickLimit = 20)
    public void aTunnelFloorIsNeverANursery(TestContext context) {
        ServerWorld world = context.getWorld();
        // Longe da arena: a barreira em volta dela seria o teto de todas as colunas.
        BlockPos floor = context.getAbsolutePos(BlockPos.ORIGIN).add(2_600_000, 20, 2_600_000);
        BlockPos open = floor.add(4, 0, 0);

        world.setChunkForced(floor.getX() >> 4, floor.getZ() >> 4, true);

        try {
            for (BlockPos at : new BlockPos[] {floor, open}) {
                world.setBlockState(at.down(), Blocks.SANDSTONE.getDefaultState());
                world.setBlockState(at, Blocks.SAND.getDefaultState());
                for (int up = 1; up <= 6; up++) {
                    world.setBlockState(at.up(up), Blocks.AIR.getDefaultState());
                }
            }
            // O teto do túnel, três acima do chão.
            world.setBlockState(floor.up(3), Blocks.SANDSTONE.getDefaultState());

            context.assertTrue(TreeNursery.isSpotForANursery(world, open), "o chão a céu aberto deixou de servir");
            context.assertFalse(TreeNursery.isSpotForANursery(world, floor),
                    "Regra 365: o chão do túnel serviu de viveiro");
            context.assertFalse(TreeNursery.plantAt(world, floor, Blocks.OAK_SAPLING),
                    "Regra 365: o lenhador plantou dentro do túnel");
            context.assertTrue(world.getBlockState(floor).isOf(Blocks.SAND), "o chão do túnel mudou");
            context.assertTrue(world.getBlockState(floor.up()).isAir(), "nasceu muda no túnel");
        } finally {
            for (BlockPos at : new BlockPos[] {floor, open}) {
                for (int up = -1; up <= 6; up++) {
                    world.setBlockState(at.up(up), Blocks.AIR.getDefaultState());
                }
            }
            world.setChunkForced(floor.getX() >> 4, floor.getZ() >> 4, false);
        }

        context.complete();
    }
}
