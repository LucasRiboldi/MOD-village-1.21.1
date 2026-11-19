package com.villagecolony.fabric.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.RoadGraphMode;
import com.villagecolony.data.save.GraphModeSavedData;
import com.villagecolony.fabric.work.RoadGraphRefresh;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Arrays;
import java.util.Optional;

/**
 * {@code /vc grafo modo [atual|regioes|vizinhanca]} — ADR-057: mostra ou troca o modo do grafo de ruas. Só um fica ligado;
 * ligar um desliga o outro, e o grafo das colônias é refeito na releitura seguinte.
 */
public final class GraphModeCommand {

    private GraphModeCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("vc")
                .then(CommandManager.literal("grafo")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("modo")
                                .executes(GraphModeCommand::show)
                                .then(CommandManager.argument("modo", StringArgumentType.word())
                                        .suggests((context, builder) -> CommandSource.suggestMatching(
                                                Arrays.stream(RoadGraphMode.values()).map(RoadGraphMode::id), builder))
                                        .executes(GraphModeCommand::change)))));
    }

    private static int show(CommandContext<ServerCommandSource> context) {
        RoadGraphMode mode = GraphModeSavedData.get(context.getSource().getServer()).mode();

        context.getSource().sendFeedback(() -> Text.literal("Village Colony: grafo no modo " + mode.id())
                .withColor(mode.edgeColor()), false);

        return 1;
    }

    private static int change(CommandContext<ServerCommandSource> context) {
        String asked = StringArgumentType.getString(context, "modo");
        Optional<RoadGraphMode> mode = RoadGraphMode.parse(asked);

        if (mode.isEmpty()) {
            context.getSource().sendError(Text.literal("Modo desconhecido: " + asked + ". Use atual, regioes ou vizinhanca."));

            return 0;
        }

        boolean changed = set(context.getSource().getServer(), mode.get());

        context.getSource().sendFeedback(() -> Text.literal(changed
                        ? "Village Colony: grafo no modo " + mode.get().id() + "; o grafo é refeito na próxima releitura."
                        : "Village Colony: o grafo já está no modo " + mode.get().id() + ".")
                .formatted(Formatting.YELLOW), true);

        return 1;
    }

    /** Liga este modo e manda as colônias refazerem o grafo; diz se mudou. */
    public static boolean set(MinecraftServer server, RoadGraphMode mode) {
        GraphModeSavedData data = GraphModeSavedData.get(server);

        if (data.mode() == mode) {
            return false;
        }

        data.set(mode);
        RoadGraphRefresh.modeChanged();
        VillageColonyMod.LOGGER.info("VC_ROAD_GRAPH mode changed to {} (ADR-057)", mode.id());

        return true;
    }
}
