package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ColonyChests;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A obra aberta reserva o cru que ainda vai pôr — P3b, 2026-10-11: a laje de
 * estoque não come o pedregulho que a parede espera.
 */
public class BuildReserveGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(1, 2, 1);

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_reserve", tickLimit = 20)
    public void aStockSlabDoesNotEatTheCobblestoneTheWallNeeds(TestContext context) {
        Optional<Item> withTen = reservedWith(context, 10);
        Optional<Item> withThirty = reservedWith(context, 30);

        context.assertTrue(withTen.filter(Items.COBBLESTONE::equals).isPresent(),
                "a obra pede 10 pedregulhos e o baú tem 10: a laje de estoque não podia sair: " + withTen);
        context.assertTrue(withThirty.isEmpty(),
                "com 30 no baú sobram 20 acima da obra: a laje podia sair: " + withThirty);
        context.complete();
    }

    /** O que a laje de estoque tiraria da obra, com {@code cobblestone} pedregulhos no baú. */
    private static Optional<Item> reservedWith(TestContext context, int cobblestone) {
        UUID worker = UUID.randomUUID();
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));
        Colony colony = Colony.create(UUID.randomUUID(), chest);
        List<BlueprintBlock> wall = new ArrayList<>();

        for (int x = 0; x < 10; x++) {
            wall.add(new BlueprintBlock(new ColonyPos(x, 0, 0), ResourceId.vanilla("cobblestone")));
        }

        ConstructionProject project = ConstructionProject.plan(
                colony.id(), Blueprint.of(ResourceId.vanilla("test_wall"), wall),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(4, 2, 4))));
        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);

        context.setBlockState(CHEST, Blocks.AIR.getDefaultState());
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        ChestDepositor.deposit(context.getWorld(), chest, Items.COBBLESTONE, cobblestone);

        try {
            VillageColonyMod.COLONIES.register(colony);
            VillageColonyMod.WORKERS.register(worker, colony.id());
            VillageColonyMod.STORAGES.register(WorkerStorage.of(worker, chest));
            VillageColonyMod.CONSTRUCTIONS.register(project);

            return BuildReserve.takenFromTheBuild(context.getWorld(), colony,
                    ColonyChests.nearestFirst(context.getWorld(), colony.id(), chest), Items.COBBLESTONE_SLAB);
        } finally {
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colony.id());
            VillageColonyMod.STORAGES.remove(worker);
            VillageColonyMod.WORKERS.remove(worker);
            VillageColonyMod.COLONIES.remove(colony.id());
        }
    }
}
