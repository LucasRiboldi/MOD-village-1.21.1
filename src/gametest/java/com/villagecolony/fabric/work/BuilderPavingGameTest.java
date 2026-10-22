package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.gametest.ColonyFixture;
import com.villagecolony.core.worker.model.ProfessionType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
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

            // Corredor de um bloco (regra do autor de 2026-10-22): o canto com caminho em três lados e o
            // buraco com caminho ao lado não se fecham — fechá-los fazia o caminho crescer em mancha.
            BlockPos corner = context.getAbsolutePos(new BlockPos(5, Y, 4));
            world.setBlockState(context.getAbsolutePos(new BlockPos(4, Y, 4)), Blocks.DIRT_PATH.getDefaultState());
            world.setBlockState(context.getAbsolutePos(new BlockPos(6, Y, 4)), Blocks.DIRT_PATH.getDefaultState());

            context.assertTrue(!BuilderPaving.isGap(world, corner),
                    "o bloco com caminho em três lados virou buraco: o caminho alarga");

            for (BlockPos undo : List.of(new BlockPos(4, Y, 4), new BlockPos(6, Y, 4))) {
                world.setBlockState(context.getAbsolutePos(undo), Blocks.GRASS_BLOCK.getDefaultState());
            }

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

    /** O carpinteiro sem tarefa também calça a rua — regra do autor, 2026-10-12. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "carpenter_paving")
    public void anIdleCarpenterPavesTheRoad(TestContext context) {
        ServerWorld world = context.getWorld();

        for (int x = 0; x <= 6; x++) {
            for (int z = 1; z <= 5; z++) {
                world.setBlockState(context.getAbsolutePos(new BlockPos(x, Y, z)),
                        z == 3 && x != 3 ? Blocks.DIRT_PATH.getDefaultState() : Blocks.GRASS_BLOCK.getDefaultState());
            }
        }

        BlockPos low = context.getAbsolutePos(new BlockPos(0, Y - 2, 1));
        BlockPos high = context.getAbsolutePos(new BlockPos(6, Y + 2, 5));
        Colony colony = Colony.create(UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(3, Y + 1, 3))));

        colony.measure(new VillageBounds(low.getX(), low.getY(), low.getZ(), high.getX(), high.getY(), high.getZ()));
        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);
        VillagerEntity carpenter = EntityType.VILLAGER.create(world);
        BlockPos stand = context.getAbsolutePos(new BlockPos(1, Y + 1, 2));

        carpenter.refreshPositionAndAngles(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0, 0);
        world.spawnEntity(carpenter);
        world.setTimeOfDay(2_000);
        VillageColonyMod.WORKERS.register(carpenter.getUuid(), colony.id()).assign(ProfessionType.CARPENTER);
        owned.owning(carpenter.getUuid());

        try {
            BuilderPaving.pass(world, colony);

            // Ao alcance do buraco, ele calça na mesma passada; longe, sai andando até lá.
            context.assertTrue(BuilderPaving.isPaving(carpenter.getUuid())
                            || world.getBlockState(context.getAbsolutePos(new BlockPos(3, Y, 3))).isOf(Blocks.DIRT_PATH),
                    "o carpinteiro sem tarefa não foi calçar a rua");
        } finally {
            for (int x = 0; x <= 6; x++) {
                for (int z = 1; z <= 5; z++) {
                    world.setBlockState(context.getAbsolutePos(new BlockPos(x, Y, z)), Blocks.AIR.getDefaultState());
                }
            }

            carpenter.discard();
            owned.cleanUp();
            BuilderPaving.clearAll();
        }

        context.complete();
    }
}
