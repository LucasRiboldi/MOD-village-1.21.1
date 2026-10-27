package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.CropPatch;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** As ajudas de quem está sem tarefa — item 8 de 27-10: o pastor colhe e replanta. */
public class IdleHelpsGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "idle_helps_field", tickLimit = 20)
    public void theShepherdHarvestsAndReplantsTheKnownRipeCrop(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos farmland = context.getAbsolutePos(new BlockPos(2, 1, 2));
        BlockPos chestAt = context.getAbsolutePos(new BlockPos(5, 2, 5));
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(chestAt));

        world.setBlockState(farmland, Blocks.FARMLAND.getDefaultState());
        world.setBlockState(farmland.up(), Blocks.WHEAT.getDefaultState().with(CropBlock.AGE, CropBlock.MAX_AGE));
        world.setBlockState(chestAt, Blocks.CHEST.getDefaultState());
        UUID keeper = UUID.randomUUID();

        VillageColonyMod.COLONIES.register(colony);
        VillageColonyMod.WORKERS.register(keeper, colony.id());
        VillageColonyMod.STORAGES.register(com.villagecolony.core.storage.model.WorkerStorage.of(keeper,
                MinecraftTypeAdapter.toColonyPos(chestAt)));

        try {
            com.villagecolony.fabric.integration.KnownFarmland.register(colony.id(), farmland);

            Optional<BlockPos> ripe = com.villagecolony.fabric.integration.KnownFarmland.ripeNear(world, colony.id(), chestAt);

            context.assertTrue(ripe.map(farmland.up()::equals).orElse(false), "a lavoura madura conhecida não foi achada");

            int stored = IdleHelps.harvestAt(world, colony.id(), farmland.up());

            context.assertTrue(stored > 0, "nada da colheita foi guardado");
            context.assertTrue(world.getBlockState(farmland.up()).isOf(Blocks.WHEAT)
                            && world.getBlockState(farmland.up()).get(CropBlock.AGE) == 0,
                    "a lavoura não foi replantada: " + world.getBlockState(farmland.up()));
            context.assertTrue(ColonyChests.countIn(world, List.of(MinecraftTypeAdapter.toColonyPos(chestAt)),
                    Items.WHEAT) > 0, "o trigo não foi para o baú da colônia");
            context.assertTrue(com.villagecolony.fabric.integration.KnownFarmland.ripeNear(world, colony.id(), chestAt).isEmpty(),
                    "replantada, a lavoura não está mais madura");
        } finally {
            CropPatch.forget(colony.id());
            VillageColonyMod.STORAGES.remove(keeper);
            VillageColonyMod.WORKERS.remove(keeper);
            VillageColonyMod.COLONIES.remove(colony.id());
        }

        context.complete();
    }
}
