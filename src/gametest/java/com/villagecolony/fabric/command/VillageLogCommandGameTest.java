package com.villagecolony.fabric.command;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * O {@code /vc log} responde a qualquer jogador e mostra o tempo dos ofícios —
 * ADR-041 #4 (AUD-17 e AUD-19).
 */
public class VillageLogCommandGameTest implements FabricGameTest {

    /** Guarda o que o comando escreve, em vez de mandar a um jogador. */
    private static final class Collected implements CommandOutput {

        final List<String> lines = new ArrayList<>();

        @Override
        public void sendMessage(Text message) {
            lines.add(message.getString());
        }

        @Override
        public boolean shouldReceiveFeedback() {
            return true;
        }

        @Override
        public boolean shouldTrackOutput() {
            return true;
        }

        @Override
        public boolean shouldBroadcastConsoleToOps() {
            return false;
        }
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_log_command")
    public void anyPlayerReadsTheTimeOfTheTrades(TestContext context) {
        ColonyFixture fixture = ColonyFixture.colonyAt(context, new BlockPos(1, 1, 1));
        MinecraftServer server = context.getWorld().getServer();
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        Collected output = new Collected();

        try {
            ColonyPos center = fixture.colony().center();
            player.refreshPositionAndAngles(center.x() + 0.5, center.y(), center.z() + 0.5, 0, 0);
            context.assertFalse(player.hasPermissionLevel(2), "o jogador de teste já era operador");

            server.getCommandManager().executeWithPrefix(player.getCommandSource().withOutput(output), "vc log");

            context.assertTrue(output.lines.stream().anyMatch(line -> line.startsWith("Tempo dos ofícios")),
                    "o /vc log não mostrou o tempo dos ofícios: " + output.lines);
            context.assertTrue(output.lines.stream().anyMatch(line -> line.startsWith("Ainda sem medida de tempo")),
                    "colônia nova devia dizer que ainda não há janela fechada: " + output.lines);
        } finally {
            server.getPlayerManager().remove(player);
            fixture.cleanUp();
        }

        context.complete();
    }
}
