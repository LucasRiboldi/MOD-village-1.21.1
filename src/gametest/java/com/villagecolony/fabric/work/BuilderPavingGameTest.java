package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.UUID;

/**
 * A4, pedido do autor de 2026-10-08: o construtor sem obra calça a rua — a grama no
 * meio do caminho vira caminho; a grama na beira dele, não.
 *
 * <p>Sessenta acima do piso, para a varredura da caixa não ver o chão de outra arena.
 */
public class BuilderPavingGameTest implements FabricGameTest {

    private static final int Y = 60;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_paving")
    public void theGrassInTheMiddleOfAPathIsPavedAndTheVergeIsNot(TestContext context) {
        ServerWorld world = context.getWorld();

        // Um caminho em x de 0 a 6, na linha z 3, com um buraco de grama em x 3; grama dos lados.
        for (int x = 0; x <= 6; x++) {
            for (int z = 1; z <= 5; z++) {
                world.setBlockState(context.getAbsolutePos(new BlockPos(x, Y, z)),
                        z == 3 && x != 3 ? Blocks.DIRT_PATH.getDefaultState() : Blocks.GRASS_BLOCK.getDefaultState());
            }
        }

        BlockPos gap = context.getAbsolutePos(new BlockPos(3, Y, 3));
        BlockPos verge = context.getAbsolutePos(new BlockPos(2, Y, 4));
        BlockPos low = context.getAbsolutePos(new BlockPos(0, Y - 2, 1));
        BlockPos high = context.getAbsolutePos(new BlockPos(6, Y + 2, 5));
        Colony colony = Colony.create(UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(3, Y + 1, 3))));

        colony.measure(new VillageBounds(low.getX(), low.getY(), low.getZ(), high.getX(), high.getY(), high.getZ()));
        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        try {
            context.assertTrue(BuilderPaving.isGap(world, gap), "a grama no meio do caminho não foi vista");
            context.assertTrue(!BuilderPaving.isGap(world, verge), "a grama na beira do caminho virou buraco");

            Optional<BlockPos> found = BuilderPaving.nextGap(world, colony);

            context.assertTrue(found.map(gap::equals).orElse(false),
                    "a busca na caixa da vila não achou o buraco: " + found);

            BuilderPaving.pave(world, gap);

            context.assertTrue(world.getBlockState(gap).isOf(Blocks.DIRT_PATH), "o buraco não virou caminho");
            context.assertTrue(world.getBlockState(verge).isOf(Blocks.GRASS_BLOCK), "a beira do caminho foi calçada");
        } finally {
            for (int x = 0; x <= 6; x++) {
                for (int z = 1; z <= 5; z++) {
                    world.setBlockState(context.getAbsolutePos(new BlockPos(x, Y, z)), Blocks.AIR.getDefaultState());
                }
            }

            owned.cleanUp();
            BuilderPaving.clearAll();
        }

        context.complete();
    }
}
