package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * E7, decisão do autor de 2026-10-08: sem ovelha com lã, o pastor só procria sob
 * déficit real do rebanho — abaixo do teto, um par sai na hora; com o rebanho
 * cheio, a lã volta pelo pasto.
 *
 * <p>Um lote de bateria para cada caso: a área conta ovelha, e arenas do mesmo lote
 * rodam lado a lado.
 */
public class ShepherdWoolDeficitGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "wool_deficit_small")
    public void aShornSmallFlockBreedsForWool(TestContext context) {
        List<SheepEntity> sheep = shorn(context, 2);

        try {
            boolean bred = ShepherdFlock.breedForWool(context.getWorld(), colonyAt(context),
                    area(context), chestWithWheat(context));

            context.assertTrue(bred, "o rebanho de 2 ovelhas tosquiadas não procriou pela lã");
            context.assertTrue(sheep.stream().allMatch(SheepEntity::isInLove),
                    "as ovelhas não ficaram prontas para procriar");
        } finally {
            sheep.forEach(SheepEntity::discard);
        }

        context.complete();
    }

    /** Controle: com o rebanho no teto, nada se cria — a lã volta sozinha. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "wool_deficit_full")
    public void aFullShornFlockDoesNotBreed(TestContext context) {
        List<SheepEntity> sheep = shorn(context, ShepherdFlock.FLOCK_TARGET);

        try {
            boolean bred = ShepherdFlock.breedForWool(context.getWorld(), colonyAt(context),
                    area(context), chestWithWheat(context));

            context.assertTrue(!bred, "o rebanho cheio procriou pela lã");
        } finally {
            sheep.forEach(SheepEntity::discard);
        }

        context.complete();
    }

    private static List<SheepEntity> shorn(TestContext context, int count) {
        ServerWorld world = context.getWorld();
        List<SheepEntity> flock = new ArrayList<>();

        for (int index = 0; index < count; index++) {
            SheepEntity sheep = EntityType.SHEEP.create(world);
            BlockPos at = context.getAbsolutePos(new BlockPos(1 + index % 4, 2, 1 + index / 4));

            sheep.refreshPositionAndAngles(at, 0, 0);
            sheep.setSheared(true);
            sheep.setAiDisabled(true);
            world.spawnEntity(sheep);
            flock.add(sheep);
        }

        return flock;
    }

    private static List<ColonyPos> chestWithWheat(TestContext context) {
        BlockPos at = context.getAbsolutePos(new BlockPos(6, 2, 6));

        context.getWorld().setBlockState(at, Blocks.CHEST.getDefaultState());

        if (context.getWorld().getBlockEntity(at) instanceof ChestBlockEntity chest) {
            chest.setStack(0, new ItemStack(Items.WHEAT, 16));
        }

        return List.of(MinecraftTypeAdapter.toColonyPos(at));
    }

    private static Colony colonyAt(TestContext context) {
        return Colony.create(UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(3, 2, 3))));
    }

    private static Box area(TestContext context) {
        return new Box(context.getAbsolutePos(new BlockPos(0, 0, 0))).expand(8);
    }
}
