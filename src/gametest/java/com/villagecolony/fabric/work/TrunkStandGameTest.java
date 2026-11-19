package com.villagecolony.fabric.work;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

/**
 * O carvalho de copa baixa — Regra 324: com folha na altura da cabeça em toda a volta do tronco, o lugar de ficar é uma
 * das oito colunas encostadas, e não um a dois blocos dele, que a Regra 277 não deixa cortar.
 */
public class TrunkStandGameTest implements FabricGameTest {

    private static final int FLOOR = 2;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "trunk_stand_choice", tickLimit = 20)
    public void thePlaceToStandTouchesTheTrunkOfALowOak(TestContext context) {
        BlockPos base = lowOak(context);
        ServerWorld world = context.getWorld();
        BlockPos stand = TreeChoice.approachTo(world, base);

        context.assertTrue(TreeChoice.touches(world, stand, base),
                "o lugar de ficar " + stand.toShortString() + " não encosta no tronco " + base.toShortString());
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "trunk_stand_clear", tickLimit = 20)
    public void theLumberjackClearsTheLeavesBesideTheTrunk(TestContext context) {
        BlockPos base = lowOak(context);
        ServerWorld world = context.getWorld();
        VillagerEntity lumberjack = context.spawnEntity(EntityType.VILLAGER, new BlockPos(2, FLOOR, 4));

        try {
            lumberjack.setAiDisabled(true);
            int cleared = TrunkStand.clear(world, lumberjack, base);
            BlockPos stand = TreeChoice.approachTo(world, base);

            context.assertTrue(cleared > 0 && BuilderApproach.standable(world, stand)
                            && TreeChoice.touches(world, stand, base),
                    "a folha ao lado do tronco ficou: tirou " + cleared + ", lugar " + stand.toShortString());
        } finally {
            lumberjack.discard();
        }

        context.complete();
    }

    /** Chão de grama, tronco de 4 e folha natural de carvalho na altura da cabeça e acima, nas oito colunas em volta. */
    static BlockPos lowOak(TestContext context) {
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                context.setBlockState(new BlockPos(x, 0, z), Blocks.STONE);
                context.setBlockState(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);

                for (int y = FLOOR; y <= FLOOR + 6; y++) {
                    context.setBlockState(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }

        BlockPos trunk = new BlockPos(4, FLOOR, 4);

        for (int up = 0; up < 4; up++) {
            context.setBlockState(trunk.up(up), Blocks.OAK_LOG);
        }

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int up = 1; up <= 4; up++) {
                    if ((dx != 0 || dz != 0) && (Math.max(Math.abs(dx), Math.abs(dz)) == 1 || up >= 2)) {
                        context.setBlockState(trunk.add(dx, up, dz), Blocks.OAK_LEAVES);
                    }
                }
            }
        }

        return context.getAbsolutePos(trunk);
    }
}
