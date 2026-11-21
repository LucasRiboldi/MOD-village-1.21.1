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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Regra 346: {@code /vc salvar} roda todos os comandos {@code /vc} de leitura, junta as linhas de análise do dia até
 * agora e grava tudo num arquivo só, na pasta de análise do mundo.
 */
public class SnapshotCommandGameTest implements FabricGameTest {

    private static final class Collected implements CommandOutput {

        private final List<String> lines = new ArrayList<>();

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

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "snapshot_command", tickLimit = 40)
    public void oneCommandSavesEveryAnalysisInOneFile(TestContext context) {
        ColonyFixture fixture = ColonyFixture.colonyAt(context, new BlockPos(1, 1, 1));
        MinecraftServer server = context.getWorld().getServer();
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        Collected output = new Collected();
        ColonyPos center = fixture.colony().center();

        player.refreshPositionAndAngles(center.x() + 0.5, center.y(), center.z() + 0.5, 0, 0);
        server.getCommandManager().executeWithPrefix(player.getCommandSource().withOutput(output), "vc salvar");

        context.runAtTick(20, () -> {
            try {
                String saved = output.lines.stream().filter(line -> line.startsWith("Análise salva em ")).findFirst()
                        .orElseThrow(() -> new AssertionError("Regra 346: o /vc salvar não disse onde gravou: " + output.lines));
                Path file = SnapshotCommand.lastFile().orElseThrow(() -> new AssertionError("nenhum arquivo gravado"));
                String text = Files.readString(file, StandardCharsets.UTF_8);

                for (String part : List.of("== /vc log", "== /vc bioma", "== /vc blocos", "== /vc dados",
                        "== /vc grafo modo", "VC_BLOCKS version=1", "VC_KEEP version=1", "VC_FLOW version=1",
                        "VC_VILLAGER_DAY version=1")) {
                    context.assertTrue(text.contains(part), "Regra 346: o arquivo não tem " + part + " (" + saved + ")");
                }

                Files.deleteIfExists(file);
            } catch (IOException failure) {
                throw new AssertionError("o teste não conseguiu ler o arquivo", failure);
            } finally {
                server.getPlayerManager().remove(player);
                fixture.cleanUp();
            }

            context.complete();
        });
    }
}
