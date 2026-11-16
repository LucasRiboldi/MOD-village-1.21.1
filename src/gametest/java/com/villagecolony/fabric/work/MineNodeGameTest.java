package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.MineGate;
import com.villagecolony.fabric.integration.MineYard;
import com.villagecolony.fabric.integration.WorldPathTerrain;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Optional;
import java.util.UUID;

/** Regra 302: o nó de entrada da mina é o chão sob o arco, no meio do vão, com o bloco de caminho. */
public class MineNodeGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_node", tickLimit = 5)
    public void theMineNodeIsTheGroundUnderTheArch(TestContext context) {
        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 7; z++) {
                context.setBlockState(new BlockPos(x, 0, z), Blocks.DIRT);
                context.setBlockState(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
            }
        }

        BlockPos mouth = context.getAbsolutePos(new BlockPos(3, 2, 4));

        try {
            MineYard.lay(context.getWorld(), mouth, Direction.SOUTH, 3);

            Optional<ColonyPos> front = MineGates.frontOf(context.getWorld(), new MineGate.Standing(mouth, Direction.SOUTH),
                    new WorldPathTerrain(context.getWorld(), UUID.randomUUID()));

            context.assertTrue(front.equals(Optional.of(MinecraftTypeAdapter.toColonyPos(
                    MineYard.underTheArch(mouth, Direction.SOUTH)))), "Regra 302: o nó da mina não ficou sob o arco: " + front);
        } finally {
            for (BlockPos at : BlockPos.iterate(0, 0, 0, 7, 7, 7)) {
                context.setBlockState(at.toImmutable(), at.getY() == 0 ? Blocks.STONE : Blocks.AIR);
            }
        }

        context.complete();
    }
}
