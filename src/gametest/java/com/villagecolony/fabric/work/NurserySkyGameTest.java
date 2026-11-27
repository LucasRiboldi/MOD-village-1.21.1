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

    /**
     * Regra 385 (notas do jogo da 0.4.148, K4 🅱️: "rebentos não devem ser plantados pelo mod um do lado do outro, deve ter
     * uma área de segurança ... e não pode ser plantada perto das estruturas"): sem muda a até 5 de outra muda ou tronco,
     * nem a até 8 de uma construção da colônia.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "nursery_room", tickLimit = 20)
    public void aNurseryKeepsItsDistance(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos base = context.getAbsolutePos(BlockPos.ORIGIN).add(2_650_000, 20, 2_650_000);
        BlockPos alone = base;
        BlockPos besideASapling = base.add(20, 0, 0);
        BlockPos besideAHouse = base.add(40, 0, 0);
        java.util.UUID colony = java.util.UUID.randomUUID();

        for (int cx = (base.getX() >> 4) - 1; cx <= ((base.getX() + 50) >> 4) + 1; cx++) {
            world.setChunkForced(cx, base.getZ() >> 4, true);
        }

        try {
            for (BlockPos at : BlockPos.iterate(base.add(-10, -1, -10), base.add(52, 6, 10))) {
                world.setBlockState(at, at.getY() == base.getY() - 1 ? Blocks.STONE.getDefaultState()
                        : at.getY() == base.getY() ? Blocks.GRASS_BLOCK.getDefaultState()
                        : Blocks.AIR.getDefaultState());
            }
            world.setBlockState(besideASapling.add(3, 1, 0), Blocks.OAK_SAPLING.getDefaultState());
            com.villagecolony.VillageColonyMod.BUILDINGS.register(new com.villagecolony.core.construction.model.Building(
                    java.util.UUID.randomUUID(), colony, com.villagecolony.core.type.ResourceId.vanilla("test/house"),
                    com.villagecolony.fabric.adapter.MinecraftTypeAdapter.toColonyPos(besideAHouse.add(6, 1, -2)),
                    com.villagecolony.fabric.adapter.MinecraftTypeAdapter.toColonyPos(besideAHouse.add(9, 4, 2))));

            context.assertTrue(TreeNursery.isSpotForANursery(world, alone), "o lugar livre deixou de servir");
            context.assertFalse(TreeNursery.isSpotForANursery(world, besideASapling),
                    "Regra 385: a muda nasceu a 3 blocos de outra");
            context.assertFalse(TreeNursery.isSpotForANursery(world, besideAHouse),
                    "Regra 385: a muda nasceu a 6 blocos de uma construção");
        } finally {
            com.villagecolony.VillageColonyMod.BUILDINGS.removeOfColony(colony);
            for (BlockPos at : BlockPos.iterate(base.add(-10, -1, -10), base.add(52, 6, 10))) {
                world.setBlockState(at, Blocks.AIR.getDefaultState());
            }
            for (int cx = (base.getX() >> 4) - 1; cx <= ((base.getX() + 50) >> 4) + 1; cx++) {
                world.setChunkForced(cx, base.getZ() >> 4, false);
            }
        }

        context.complete();
    }

    /**
     * Regra 385 (a mesma nota: "espalhar bem mais, podendo sair da zona da vila"): o anel do viveiro começa no alcance do
     * lenhador ({@link LumberjackWork#SEARCH_RADIUS}), e não mais 8 blocos para dentro dele.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "nursery_spread", tickLimit = 20)
    public void theNurserySpreadsToTheLumberjacksReach(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos centre = context.getAbsolutePos(BlockPos.ORIGIN).add(2_700_000, 20, 2_700_000);
        BlockPos far = centre.add(LumberjackWork.SEARCH_RADIUS, 0, 0);
        BlockPos near = centre.add(56, 0, 0);

        world.setChunkForced(far.getX() >> 4, far.getZ() >> 4, true);
        world.setChunkForced(near.getX() >> 4, near.getZ() >> 4, true);

        try {
            for (BlockPos at : new BlockPos[] {far, near}) {
                world.setBlockState(at.down(), Blocks.STONE.getDefaultState());
                world.setBlockState(at, Blocks.GRASS_BLOCK.getDefaultState());
            }

            java.util.Optional<BlockPos> spot = LumberjackNursery.spotOnTheEdge(world, centre, 0.0);

            context.assertTrue(spot.filter(far::equals).isPresent(),
                    "Regra 385: o viveiro não foi até o alcance do lenhador: " + spot);
        } finally {
            for (BlockPos at : new BlockPos[] {far, near}) {
                world.setBlockState(at.down(), Blocks.AIR.getDefaultState());
                world.setBlockState(at, Blocks.AIR.getDefaultState());
            }
            world.setChunkForced(far.getX() >> 4, far.getZ() >> 4, false);
            world.setChunkForced(near.getX() >> 4, near.getZ() >> 4, false);
        }

        context.complete();
    }
}
