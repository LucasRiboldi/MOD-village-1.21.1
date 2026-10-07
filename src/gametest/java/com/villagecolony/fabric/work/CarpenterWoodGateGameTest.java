package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestDepositor;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * O portão do carpinteiro lê os mesmos baús da meta — playtest de 2026-10-03:
 * 12 tarefas de tábua fechadas com 0 peça em meia hora, porque a meta contava
 * a tora de todos os baús da vila e o carpinteiro só a dos baús de trabalhador.
 */
public final class CarpenterWoodGateGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "carpenter_wood_gate")
    public void logsInAnUnownedVillageChestMayBecomePlanks(TestContext context) {
        BlockPos own = new BlockPos(2, 2, 2);
        BlockPos village = new BlockPos(5, 2, 2);

        context.setBlockState(own, Blocks.CHEST.getDefaultState());
        context.setBlockState(village, Blocks.CHEST.getDefaultState());

        ColonyPos ownChest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(own));
        ColonyPos villageChest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(village));
        Colony colony = Colony.create(UUID.randomUUID(), ownChest);
        UUID carpenter = UUID.randomUUID();

        // A vila medida, como em jogo (Emenda 6): sem caixa, só o baú coberto
        // de casa entra, e o cenário a céu aberto mediria outra regra.
        colony.measure(VillageBounds.aroundPiece(ownChest, villageChest));
        VillageColonyMod.COLONIES.register(colony);
        VillageColonyMod.WORKERS.register(carpenter, colony.id()).assign(ProfessionType.CARPENTER);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(carpenter, ownChest));

        try {
            // O baú do carpinteiro vazio; as toras num baú da vila sem dono,
            // como o do jogador.
            ChestDepositor.deposit(context.getWorld(), villageChest, Items.OAK_LOG, 16);

            context.assertTrue(CraftingSteps.halfTheWoodMayStillBeConverted(context.getWorld(), colony.id()),
                    "16 toras num baú da vila e o carpinteiro diz que a metade fica em tora:"
                            + " a meta abriria tarefa que fecha com 0 peça");
        } finally {
            VillageColonyMod.STORAGES.remove(carpenter);
            VillageColonyMod.WORKERS.remove(carpenter);
            VillageColonyMod.COLONIES.remove(colony.id());
        }

        context.complete();
    }
}
