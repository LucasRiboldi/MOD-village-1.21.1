package com.villagecolony.fabric.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.coordination.WorkClock;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.integration.ItemFlow;
import com.villagecolony.fabric.integration.SnapshotHistory;
import com.villagecolony.fabric.work.ActionJournal;
import com.villagecolony.fabric.work.BlockLedger;
import com.villagecolony.fabric.work.KeepLedger;
import com.villagecolony.fabric.work.MineEntrance;
import com.villagecolony.fabric.work.MineMarks;
import com.villagecolony.fabric.work.VillagerDay;
import com.villagecolony.fabric.work.VillagerTimeline;
import com.villagecolony.fabric.work.WorkTime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.WorldSavePath;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * {@code /vc salvar} — Regra 346: salva todas as análises da vila em volta do jogador num arquivo só,
 * {@code <mundo>/villagecolony/analytics/snapshot-AAAAMMDD-HHMMSS.txt}. Roda os comandos {@code /vc} de leitura (os de
 * jogador e, só leitura, o modo do grafo e o estado das obras pedidas), captura o que cada um responderia no chat, junta
 * as linhas de análise do dia até agora ({@code VC_BLOCKS}, {@code VC_KEEP}, {@code VC_FLOW}, {@code VC_VILLAGER_DAY}),
 * sem zerar as contas diárias, acrescenta mina, obras e o histórico da sessão e grava o diário de ações pendente.
 *
 * <p>Na 1.21.1 o comando chamado de dentro de outro vai para a fila e só roda depois dele
 * ({@code CommandExecutionContext.enqueueCommand}); por isso o {@code /vc salvar} só agenda, e o trabalho sai no fim do
 * tique, fora de qualquer comando.
 */
public final class SnapshotCommand {

    /** Os comandos de jogador, na ordem do arquivo. */
    static final List<String> PLAYER_READS = List.of("vc log", "vc bioma", "vc blocos", "vc dados");

    /** Os de operador que só leem; rodam com permissão de leitura, nunca os que mudam algo. */
    static final List<String> OPERATOR_READS = List.of("vc grafo modo", "vc mae estado");

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private static final Deque<ServerCommandSource> PENDING = new ArrayDeque<>();

    private static @Nullable Path last;

    static {
        ServerMemory.register(SnapshotCommand.class, () -> {
            PENDING.clear();
            last = null;
        });
    }

    private SnapshotCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
        ServerTickEvents.END_SERVER_TICK.register(SnapshotCommand::drain);
    }

    private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("vc")
                .then(CommandManager.literal("salvar").executes(SnapshotCommand::schedule)));
    }

    private static int schedule(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayerEntity)) {
            source.sendError(Text.literal("Este comando só pode ser usado por um jogador."));
            return 0;
        }

        PENDING.add(source);
        source.sendFeedback(() -> Text.literal("Village Colony: salvando todas as análises da vila…")
                .formatted(Formatting.GRAY), false);
        return 1;
    }

    private static void drain(MinecraftServer server) {
        while (!PENDING.isEmpty()) {
            save(server, PENDING.poll());
        }
    }

    private static void save(MinecraftServer server, ServerCommandSource source) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            return;
        }

        Optional<Colony> colony = VillageLogCommand.villageAtPlayerColumn(player,
                new ColonyPos(player.getBlockX(), player.getBlockY(), player.getBlockZ()));

        if (colony.isEmpty()) {
            source.sendFeedback(() -> Text.literal("Village Colony: nenhuma vila ativa está perto de você; nada salvo.")
                    .formatted(Formatting.YELLOW), false);
            return;
        }

        ServerWorld world = player.getServerWorld();
        long day = world.getTimeOfDay() / WorkClock.DAY;
        StringBuilder text = new StringBuilder();

        text.append("Village Colony — análise salva ").append(LocalDateTime.now(java.time.ZoneId.systemDefault())).append('\n')
                .append("mod ").append(net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer(VillageColonyMod.MOD_ID)
                        .map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("?")).append(", colônia ").append(colony.get().id())
                .append(", dia ").append(day).append(", tique ").append(world.getTime()).append(", jogador em ")
                .append(player.getBlockPos().toShortString()).append("\n\n");

        for (String command : PLAYER_READS) {
            if ("vc blocos".equals(command)) {
                text.append("== /vc blocos\n");
                BlockCommand.showRows(world, colony.get(), Integer.MAX_VALUE,
                        line -> text.append(line.getString()).append('\n'));
                text.append('\n');
            } else {
                run(server, source, command, text);
            }
        }

        for (String command : OPERATOR_READS) {
            run(server, source.withLevel(2), command, text);
        }

        text.append("== linhas do dia até agora (parciais; as contas diárias não foram zeradas)\n")
                .append(BlockLedger.report(world, colony.get(), day)).append('\n')
                .append(KeepLedger.peek(day)).append('\n')
                .append(ItemFlow.peek(world, day)).append('\n')
                .append(VillagerDay.peek(colony.get().id(), day)).append('\n');

        VillagerTimeline.append(colony.get().id(), world.getTime(), text);
        appendWorks(world, colony.get(), text);
        SnapshotHistory.append(colony.get().id(), WorkTime.currentLine(colony.get().id(), world.getTime()), text);
        ActionJournal.flush();

        try {
            Path folder = server.getSavePath(WorldSavePath.ROOT).resolve("villagecolony").resolve("analytics");
            Files.createDirectories(folder);
            Path file = folder.resolve("snapshot-" + LocalDateTime.now(java.time.ZoneId.systemDefault()).format(STAMP) + ".txt");
            Files.writeString(file, text.toString(), StandardCharsets.UTF_8);
            last = file;
            VillageColonyMod.LOGGER.info("VC_SNAPSHOT version=1 colony={} file={}",
                    colony.get().id().toString().substring(0, 8), file.getFileName());
            source.sendFeedback(() -> Text.literal("Análise salva em " + file.getFileName()
                    + " (pasta do mundo, villagecolony/analytics)").formatted(Formatting.GREEN), false);
        } catch (IOException failure) {
            VillageColonyMod.LOGGER.warn("Could not save the analysis snapshot", failure);
            source.sendError(Text.literal("Não foi possível gravar a análise: " + failure.getMessage()));
        }
    }

    private static void appendWorks(ServerWorld world, Colony colony, StringBuilder text) {
        var id = colony.id();
        text.append("\n== mina\n");
        VillageColonyMod.MINES.of(id).ifPresentOrElse(mine -> text.append("entrada=").append(mine.entry())
                .append(" níveis=").append(MineEntrance.levelsDown(world, id, mine))
                .append(" hélices=").append(mine.helicesTried())
                .append(" recusas ativas no traçado=").append(MineMarks.activeIn(world, mine))
                .append(" cortes por braço=").append(java.util.Arrays.toString(mine.cuts())).append('\n'),
                () -> text.append("Nenhuma mina aberta.\n"));

        text.append("\n== obras e peças postas de lado\n");
        var buildings = VillageColonyMod.BUILDINGS.ofColony(id);
        text.append("Construções registradas: ").append(buildings.size()).append('\n');
        buildings.forEach(building -> text.append(building.blueprint()).append(" em ").append(building.min())
                .append(" pronta=").append(building.finished()).append('\n'));
        var projects = VillageColonyMod.CONSTRUCTIONS.ofColony(id);
        text.append("Obras registradas: ").append(projects.size()).append('\n');
        for (var project : projects) {
            text.append(project.blueprint().id()).append(" em ").append(project.origin())
                    .append(" estado=").append(project.state()).append(" faltam=").append(project.remainingCount())
                    .append(" de lado=").append(project.deferredPieces().size()).append('\n');
            project.deferredPieces().forEach(piece -> text.append("  ").append(piece.block())
                    .append(" em ").append(piece.position()).append(" motivo=").append(piece.reason()).append('\n'));
        }
        text.append('\n');
    }

    /** Roda um comando e põe no texto o que ele responderia no chat. */
    private static void run(MinecraftServer server, ServerCommandSource source, String command, StringBuilder text) {
        Collected output = new Collected();

        server.getCommandManager().executeWithPrefix(source.withOutput(output), command);
        text.append("== /").append(command).append('\n');
        output.lines.forEach(line -> text.append(line).append('\n'));
        text.append('\n');
    }

    /** O último arquivo gravado, para o teste. */
    static Optional<Path> lastFile() {
        return Optional.ofNullable(last);
    }

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
}
