package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.work.SmeltInput;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * A trava SMELT_INPUT — playtest de 2026-10-08: sem minério em nenhum baú, o
 * fundidor pegou e soltou a tarefa de lingote 191 vezes em 12 minutos. Pedido de
 * fundir só é elegível com o cru num baú da colônia — o dele, ou o de peça que as
 * obras usam (o plano B do fundidor), por isso o cenário começa com o baú vazio.
 */
public class SmeltInputGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(2, 2, 2);

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "smelt_input")
    public void anIngotOrderWithoutOreIsNotEligible(TestContext context) {
        ServerWorld world = context.getWorld();

        context.setBlockState(new BlockPos(2, 1, 2), Blocks.DIRT.getDefaultState());
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        Colony colony = Colony.create(UUID.randomUUID(), chest);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);
        UUID smelter = UUID.randomUUID();

        VillageColonyMod.WORKERS.register(smelter, colony.id()).assign(ProfessionType.SMELTER);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(smelter, chest));
        owned.owning(smelter);

        try {
            context.assertTrue(!SmeltInput.inChests(world, colony.id(), ResourceType.IRON_INGOT),
                    "o pedido de lingote ficou elegível com o baú vazio");

            // Controle: com pedra no baú, o pedido de pedra lisa é elegível. Outro
            // pedido, porque a resposta vale pelo tique.
            ChestDepositor.deposit(world, chest, Items.STONE, 4);

            context.assertTrue(SmeltInput.inChests(world, colony.id(), ResourceType.SMOOTH_STONE),
                    "o pedido de pedra lisa não ficou elegível com pedra no baú");
        } finally {
            owned.cleanUp();
        }

        context.complete();
    }
}
