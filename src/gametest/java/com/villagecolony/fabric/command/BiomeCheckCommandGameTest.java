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
 * Os comandos que a triagem do playtest usa respondem dentro da vila — {@code /vc bioma}, {@code /vc dados} e
 * {@code /vc bioma nota} ({@code CLAUDE.md} §0.2.2, passo 1). VF-019 da varredura de 12-11: sem teste, um comando
 * quebrado só aparecia no jogo do autor. O {@code /vc log} tem o próprio teste ({@link VillageLogCommandGameTest}).
 */
public class BiomeCheckCommandGameTest implements FabricGameTest {

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

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "biome_check_command")
    public void theBiomeBulletinShowsTheVillage(TestContext context) {
        List<String> lines = run(context, "vc bioma");

        context.assertTrue(lines.stream().anyMatch(line -> line.startsWith("Village Colony — boletim do bioma")),
                "o /vc bioma não abriu o boletim: " + lines);
        context.assertTrue(lines.stream().anyMatch(line -> line.startsWith("Bioma ")),
                "o /vc bioma não disse o bioma: " + lines);
        context.assertTrue(lines.stream().anyMatch(line -> line.startsWith("Camas ")),
                "o /vc bioma não contou camas e aldeões: " + lines);
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "biome_check_command")
    public void theDataCommandWritesThePortrait(TestContext context) {
        List<String> lines = run(context, "vc dados");

        context.assertTrue(lines.stream().anyMatch(line -> line.startsWith("Retrato gravado em ")),
                "o /vc dados não gravou o retrato: " + lines);
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "biome_check_command")
    public void theNoteIsRecordedWhereThePlayerStands(TestContext context) {
        List<String> lines = run(context, "vc bioma nota o pastor parou no portão");

        context.assertTrue(lines.stream().anyMatch(line -> line.startsWith("Nota registrada em ")),
                "o /vc bioma nota não registrou a nota: " + lines);
        context.complete();
    }

    /** Põe um jogador comum no centro de uma colônia da arena, roda o comando e devolve o que ele escreveu. */
    private static List<String> run(TestContext context, String command) {
        ColonyFixture fixture = ColonyFixture.colonyAt(context, new BlockPos(1, 1, 1));
        MinecraftServer server = context.getWorld().getServer();
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        Collected output = new Collected();

        try {
            ColonyPos center = fixture.colony().center();
            player.refreshPositionAndAngles(center.x() + 0.5, center.y(), center.z() + 0.5, 0, 0);
            server.getCommandManager().executeWithPrefix(player.getCommandSource().withOutput(output), command);

            return output.lines;
        } finally {
            server.getPlayerManager().remove(player);
            fixture.cleanUp();
        }
    }
}
