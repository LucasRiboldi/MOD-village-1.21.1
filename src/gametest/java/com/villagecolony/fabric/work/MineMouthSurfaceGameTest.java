package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * Regra 359: a boca da mina nasce no chão aberto para o céu e alcançado a pé da vila — nunca no oco de uma caverna dentro
 * do morro (jogo da 0.4.139, mundo 1: o arco ficou 22 blocos abaixo do topo da montanha).
 */
public class MineMouthSurfaceGameTest implements FabricGameTest {

    /** A altura do topo do morro, relativa ao chão do vale. */
    private static final int TOP = 7;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_mouth_surface", tickLimit = 40)
    public void theMouthOpensOnTheMountainTopAndNotInTheCaveBelow(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(0, 1, 0));
        ColonyFixture fixture = ColonyFixture.colonyAt(context, new BlockPos(12, 1, 12));
        Colony colony = fixture.colony();

        try {
            for (int x = 0; x <= 24; x++) {
                for (int z = 0; z <= 24; z++) {
                    for (int y = -1; y <= TOP + 3; y++) {
                        boolean pit = x >= 10 && x <= 14 && z >= 10 && z <= 14 && y >= 0;
                        // A subida em degraus de 1 do vale ao topo, que vira antes da linha das bocas (x = 18).
                        int step = rampStep(x, z);
                        boolean ramp = step >= 0 && y > step;
                        boolean edge = x <= 6 || x >= 18 || z <= 6 || z >= 18;
                        // O oco: dois de altura sob o morro, em toda a faixa da borda, com o morro por cima.
                        boolean cave = edge && (y == 0 || y == 1);
                        boolean solid = y <= TOP && !pit && !ramp && !cave;

                        world.setBlockState(origin.add(x, y, z),
                                solid ? Blocks.STONE.getDefaultState() : Blocks.AIR.getDefaultState());
                    }
                }
            }

            colony.measure(new VillageBounds(origin.getX() + 1, origin.getY() - 4, origin.getZ() + 1,
                    origin.getX() + 23, origin.getY() + TOP + 3, origin.getZ() + 23));
            BlockPos center = context.getAbsolutePos(new BlockPos(12, 1, 12));

            Optional<MineEdge.Choice> choice = MineEdge.inside(world, colony.id(), center, null);

            context.assertTrue(choice.isPresent(), "nenhuma boca no morro");
            BlockPos mouth = choice.get().mouth();

            context.assertTrue(mouth.getY() == origin.getY() + TOP,
                    "Regra 359: a boca nasceu em " + mouth.toShortString() + ", e não no topo do morro (y "
                            + (origin.getY() + TOP) + ")");
        } finally {
            fixture.cleanUp();
        }

        context.complete();
    }

    /** A areia que cai por cima da boca sai, e o arco fica (Regra 359). */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_mouth_surface", tickLimit = 20)
    public void theSandOverTheMouthGoesAndTheArchStays(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos mouth = context.getAbsolutePos(new BlockPos(6, 1, 6));
        BlockPos sand = null;
        BlockPos arch = null;

        for (int x = 0; x <= 12; x++) {
            for (int z = 0; z <= 12; z++) {
                // A arena é reaproveitada: o morro do teste vizinho não pode sobrar em cima da boca.
                for (int y = 1; y <= 2 + MineSky.OVERHANG_TOP; y++) {
                    world.setBlockState(context.getAbsolutePos(new BlockPos(x, y, z)),
                            y == 1 ? Blocks.STONE.getDefaultState() : Blocks.AIR.getDefaultState());
                }
            }
        }

        for (int dy = 1; dy <= MineSky.OVERHANG_TOP; dy++) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos at = mouth.add(dx, dy, dz);
                    boolean part = com.villagecolony.fabric.integration.MineGate.isPart(mouth, net.minecraft.util.math.Direction.EAST, at);

                    if (part && arch == null) {
                        arch = at;
                    } else if (!part && dx == 0 && dz == 0 && dy >= 3 && sand == null) {
                        sand = at;
                    }
                }
            }
        }

        context.assertTrue(sand != null && arch != null, "o teste não achou onde pôr a areia e o arco");
        world.setBlockState(sand, Blocks.SAND.getDefaultState(), net.minecraft.block.Block.FORCE_STATE);
        world.setBlockState(sand.down(), Blocks.SANDSTONE.getDefaultState());
        world.setBlockState(arch, Blocks.SANDSTONE.getDefaultState());

        int cleared = MineStairClearing.clearSky(world, mouth, net.minecraft.util.math.Direction.EAST, 16);

        context.assertTrue(world.getBlockState(sand).isAir(), "Regra 359: a areia ficou por cima da boca (" + cleared + ")");
        context.assertTrue(world.getBlockState(arch).isOf(Blocks.SANDSTONE), "Regra 359: o Zelador tirou o arco");
        context.complete();
    }

    /** O chão da subida nesta coluna, ou -1 fora dela: três para o leste, depois quatro para o sul. */
    private static int rampStep(int x, int z) {
        if (z == 12 && x >= 15 && x <= 17) {
            return x - 15;
        }

        return x == 17 && z >= 13 && z <= 16 ? z - 10 : -1;
    }
}
