package com.villagecolony.fabric.event;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.VillageFocus;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * A regra da vila em foco com um jogador de verdade no mundo — P1 da auditoria
 * de simulação de 26-09: até 02-10 toda a bateria rodava sem jogador, e as
 * regras que dependem dele (só a vila com jogador dentro trabalha; a vila
 * descansa sem ele) só tinham o atalho de "sem jogador, trabalha", que existe
 * para os próprios testes.
 */
public final class VillageFocusPlayerGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "focus_player")
    public void aPlayerInsideAttendsTheVillageAndOneFarAwayDoesNot(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos center = context.getAbsolutePos(new BlockPos(1, 2, 1));
        ColonyPos at = MinecraftTypeAdapter.toColonyPos(center);
        Colony near = Colony.create(UUID.randomUUID(), at);
        Colony far = Colony.create(UUID.randomUUID(), new ColonyPos(at.x() + 2_000, at.y(), at.z()));
        FakePlayer player = FakePlayer.get(world);

        VillageColonyMod.COLONIES.register(near);
        VillageColonyMod.COLONIES.register(far);

        try {
            player.refreshPositionAndAngles(center.getX() + 0.5, center.getY(), center.getZ() + 0.5, 0, 0);
            world.onPlayerConnected(player);

            context.assertTrue(world.getPlayers().contains(player), "o jogador de teste não entrou no mundo");

            // O jogador já estava dentro há um minuto (ADR-036 item 11): a
            // presença contínua é marcada a cada 20 tiques, como o foco faz.
            long now = world.getTime();
            for (long tick = now - Colony.SETTLE_TICKS; tick < now; tick += 20) {
                near.attend(tick);
            }

            VillageFocus.attend(world);

            context.assertTrue(near.isAttended(now), "jogador dentro da vila e ela não ficou atendida");
            context.assertFalse(far.isAttended(now), "a vila a 2.000 blocos ficou atendida sem jogador dentro");
            context.assertTrue(VillageFocus.isWorking(world, near.id()) == near.isActive(),
                    "a vila atendida trabalha se estiver ativa");
            context.assertFalse(VillageFocus.isWorking(world, far.id()),
                    "com jogador no mundo, a vila longe dele não trabalha (Emenda 6)");
        } finally {
            world.removePlayer(player, Entity.RemovalReason.DISCARDED);
            VillageColonyMod.COLONIES.remove(near.id());
            VillageColonyMod.COLONIES.remove(far.id());
        }

        context.assertFalse(world.getPlayers().contains(player), "o jogador de teste ficou no mundo");
        context.complete();
    }
}
