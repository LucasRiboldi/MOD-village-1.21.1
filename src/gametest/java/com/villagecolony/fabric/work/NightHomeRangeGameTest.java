package com.villagecolony.fabric.work;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.Heightmap;

/**
 * À noite, quem tem caminho até a cama anda — Regra 142, PROJECT_AUDIT.md T4: a busca do caminho de casa vai a
 * 128 blocos; a do jogo para em 48, e a cama a 60 blocos parecia inalcançável. O cenário fica longe das arenas,
 * numa fileira de chunks carregados um a um, com chão plano entre o aldeão e a cama.
 */
public class NightHomeRangeGameTest implements FabricGameTest {

    private static final int FAR = 30_000;

    /** A distância até a cama: além dos 48 do jogo, dentro dos 128 da regra. */
    private static final int BED_AWAY = 60;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "night_home_range", tickLimit = 40)
    public void aVillagerFarFromItsBedWalksHomeInsteadOfTeleporting(TestContext context) {
        ServerWorld world = context.getWorld();
        int offset = (int) (Math.abs(context.getAbsolutePos(BlockPos.ORIGIN).asLong()) % 64) * 8;
        ChunkPos start = new ChunkPos(FAR + offset, FAR + 32);

        for (int chunk = 0; chunk <= (BED_AWAY + 20) / 16 + 1; chunk++) {
            world.getChunk(start.x + chunk, start.z);
        }

        int z = start.getCenterZ();
        int x = start.getStartX() + 2;
        int feet = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos foot = new BlockPos(x + BED_AWAY, feet, z);
        BlockState bed = Blocks.RED_BED.getDefaultState().with(BedBlock.FACING, Direction.EAST);

        world.setBlockState(foot, bed.with(BedBlock.PART, BedPart.FOOT), Block.NOTIFY_LISTENERS);
        world.setBlockState(foot.east(), bed.with(BedBlock.PART, BedPart.HEAD), Block.NOTIFY_LISTENERS);

        VillagerEntity villager = EntityType.VILLAGER.create(world);
        context.assertTrue(villager != null, "o aldeão não foi criado");
        villager.refreshPositionAndAngles(x + 0.5, feet, z + 0.5, 0, 0);
        world.spawnEntity(villager);
        // O chunk de longe não tem tique: o aldeão não pousa sozinho, e quem não está no chão não busca caminho.
        villager.setOnGround(true);
        villager.getBrain().remember(MemoryModuleType.HOME, GlobalPos.create(world.getRegistryKey(), foot.east()));

        try {
            BlockPos before = villager.getBlockPos();

            // A regra teleporta na segunda conferência parado sem caminho; com caminho, nenhuma teleporta.
            boolean first = NightHome.check(world, villager, villager.getUuid());
            boolean second = NightHome.check(world, villager, villager.getUuid());

            context.assertTrue(!first && !second && villager.getBlockPos().equals(before),
                    "havia caminho até a cama a " + BED_AWAY + " blocos e o aldeão foi teleportado de "
                            + before.toShortString() + " para " + villager.getBlockPos().toShortString());
        } finally {
            villager.discard();
        }

        context.complete();
    }
}
