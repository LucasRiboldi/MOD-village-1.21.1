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

/** A boca nova nasce até 5 blocos para dentro da vila, no morro e longe da água (ADR-038 P3b). */
public class MineMouthInsideGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_mouth_inside", tickLimit = 40)
    public void theMouthOpensOnTheHillInsideTheVillage(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(0, 1, 0));
        ColonyFixture fixture = ColonyFixture.colonyAt(context, new BlockPos(12, 1, 12));
        Colony colony = fixture.colony();

        try {
            // Chão de pedra 25x25 a y=0 (relativo); a vila é a caixa de 1 a 23.
            for (int x = 0; x <= 24; x++) {
                for (int z = 0; z <= 24; z++) {
                    for (int y = 0; y <= 8; y++) {
                        world.setBlockState(origin.add(x, y - 1, z),
                                y == 0 ? Blocks.STONE.getDefaultState() : Blocks.AIR.getDefaultState());
                    }
                }
            }

            // Morro de pedra de 4 de altura na faixa leste, dentro da caixa.
            for (int x = 18; x <= 23; x++) {
                for (int z = 1; z <= 23; z++) {
                    for (int y = 1; y <= 4; y++) {
                        world.setBlockState(origin.add(x, y - 1, z), Blocks.STONE.getDefaultState());
                    }
                }
            }

            // Água encostada na borda oeste.
            for (int z = 1; z <= 23; z++) {
                world.setBlockState(origin.add(1, -1, z), Blocks.WATER.getDefaultState());
            }

            colony.measure(new VillageBounds(origin.getX() + 1, origin.getY() - 4, origin.getZ() + 1,
                    origin.getX() + 23, origin.getY() + 8, origin.getZ() + 23));
            VillageBounds box = colony.bounds().orElseThrow();
            BlockPos center = context.getAbsolutePos(new BlockPos(12, 1, 12));

            Optional<MineEdge.Choice> choice = MineEdge.inside(world, colony.id(), center, null);

            context.assertTrue(choice.isPresent(), "nenhuma boca dentro da vila");

            BlockPos mouth = choice.get().mouth();
            int inside = Math.min(Math.min(mouth.getX() - box.minX(), box.maxX() - mouth.getX()),
                    Math.min(mouth.getZ() - box.minZ(), box.maxZ() - mouth.getZ()));

            context.assertTrue(box.containsColumn(mouth.getX(), mouth.getZ()) && inside <= MineEdge.INSIDE,
                    "a boca " + mouth.toShortString() + " não está até 5 blocos para dentro da borda (" + inside + ")");
            context.assertTrue(mouth.getX() - origin.getX() >= 18,
                    "a boca " + mouth.toShortString() + " não nasceu no morro do leste");
        } finally {
            fixture.cleanUp();
        }

        context.complete();
    }

    /** Morro e água pesam cada um por si: com o outro fator igual, cada um decide sozinho (B6). */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_mouth_inside", tickLimit = 20)
    public void hillAndWaterEachCountOnTheirOwn(TestContext context) {
        context.assertTrue(MineEdge.score(MineEdge.WATER_LOOK, 5) > MineEdge.score(MineEdge.WATER_LOOK, 0),
                "com a água igual, o morro não pesou");
        context.assertTrue(MineEdge.score(MineEdge.WATER_LOOK, 0) > MineEdge.score(2, 0),
                "com o morro igual, a distância da água não pesou");
        context.complete();
    }
}
