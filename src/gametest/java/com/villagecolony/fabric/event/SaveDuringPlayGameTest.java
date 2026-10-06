package com.villagecolony.fabric.event;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.data.save.ColonySavedData;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.server.MinecraftServer;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * O registro da colônia vai ao save a cada salvamento do mundo — ADR-035 §1.
 *
 * <p>O teste dispara o próprio evento da Fabric API em vez de chamar o
 * handler: assim prova também que o handler está inscrito nele. O mundo não é
 * gravado em disco; só o {@code PersistentState} é conferido.
 */
public final class SaveDuringPlayGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "save_during_play")
    public void anAutosaveCopiesTheColonyIntoTheSave(TestContext context) {
        MinecraftServer server = context.getWorld().getServer();
        Colony colony = colonyAt(context);

        VillageColonyMod.COLONIES.register(colony);

        try {
            ServerLifecycleEvents.BEFORE_SAVE.invoker().onBeforeSave(server, false, false);

            context.assertTrue(isSaved(server, colony.id()),
                    "o salvamento do mundo não copiou a colônia para o save");
        } finally {
            VillageColonyMod.COLONIES.remove(colony.id());
        }

        context.complete();
    }

    /**
     * Depois do {@code SERVER_STOPPING} os registros estão vazios; o
     * salvamento que o Minecraft faz em seguida não pode copiar o vazio por
     * cima do que o fechamento acabou de gravar.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "save_during_play")
    public void theSaveThatFollowsTheShutdownCopiesNothing(TestContext context) {
        MinecraftServer server = context.getWorld().getServer();
        Colony colony = colonyAt(context);

        VillageColonyMod.COLONIES.register(colony);
        ServerLifecycleHandler.markStopping(true);

        try {
            ServerLifecycleEvents.BEFORE_SAVE.invoker().onBeforeSave(server, false, false);

            context.assertFalse(isSaved(server, colony.id()),
                    "o salvamento depois do fechamento copiou o registro de novo");
        } finally {
            ServerLifecycleHandler.markStopping(false);
            VillageColonyMod.COLONIES.remove(colony.id());
        }

        context.complete();
    }

    private static Colony colonyAt(TestContext context) {
        ColonyPos center = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(1, 2, 1)));

        return Colony.create(UUID.randomUUID(), center);
    }

    private static boolean isSaved(MinecraftServer server, UUID colonyId) {
        return ColonySavedData.get(server).colonies().stream().anyMatch(saved -> saved.id().equals(colonyId));
    }
}
