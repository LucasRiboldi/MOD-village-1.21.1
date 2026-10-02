package com.villagecolony.fabric.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.telemetry.model.ActivityTrace;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.work.HousePlans;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

/** Registra o resumo de atividade da colônia para jogadores no chat. */
public final class VillageLogCommand {

    private static final int ENTRY_LIMIT = 8;

    private VillageLogCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                register(dispatcher));
    }

    private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("vc")
                .then(CommandManager.literal("log").executes(VillageLogCommand::show)));
    }

    private static int show(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            source.sendError(Text.literal("Este menu só pode ser aberto por um jogador."));
            return 0;
        }

        if (!player.getServerWorld().getRegistryKey().equals(World.OVERWORLD)) {
            source.sendFeedback(
                    () -> Text.literal("Village Colony: o diagnóstico da vila funciona no mundo principal.")
                            .formatted(Formatting.YELLOW),
                    false);
            return 1;
        }

        Optional<Colony> colony = VillageColonyMod.COLONIES.findNearest(
                new ColonyPos(player.getBlockX(), player.getBlockY(), player.getBlockZ()),
                VillageDetector.SEARCH_RADIUS);

        if (colony.isEmpty() || !colony.get().isActive()) {
            source.sendFeedback(
                    () -> Text.literal("Village Colony: nenhuma vila ativa está perto de você. "
                                    + "Uma vila só trabalha enquanto o jogo simula a região dela.")
                            .formatted(Formatting.YELLOW),
                    false);
            return 1;
        }

        Colony nearby = colony.get();
        int workers = VillageColonyMod.WORKERS.countOfColony(nearby.id());
        ActivityTrace trace = VillageColonyMod.ACTIVITY_TRACES.of(nearby.id())
                .orElseGet(ActivityTrace::new);

        source.sendFeedback(
                () -> Text.literal("Village Colony - situação da vila")
                        .formatted(Formatting.AQUA, Formatting.BOLD),
                false);
        source.sendFeedback(
                () -> Text.literal("Colônia ativa perto de você | " + workers + " profissionais registrados")
                        .formatted(Formatting.GRAY),
                false);
        String constructionPriority = VillageLogPresenter.constructionPriority(
                com.villagecolony.fabric.work.ConstructionTurn.priorityFor(source.getWorld(), nearby), workers, nearby.observedBeds());
        source.sendFeedback(
                () -> Text.literal(constructionPriority).formatted(Formatting.YELLOW),
                false);
        source.sendFeedback(
                () -> Text.literal("Atividades mais recentes:").formatted(Formatting.WHITE),
                false);

        List<String> entries = VillageLogPresenter.entries(trace, ENTRY_LIMIT);
        for (String entry : entries) {
            source.sendFeedback(() -> statusText(entry), false);
        }

        if (trace.overflowCount() > 0) {
            source.sendFeedback(
                    () -> Text.literal("Histórico resumido: " + trace.overflowCount()
                                    + " eventos antigos foram descartados para evitar peso.")
                            .formatted(Formatting.GRAY),
                    false);
        }

        return 1;
    }

    private static Text statusText(String entry) {
        Formatting color = entry.startsWith("[TRAVADO]") ? Formatting.RED
                : entry.startsWith("[AGUARDANDO]") ? Formatting.YELLOW
                : entry.startsWith("[ATENÇÃO]") ? Formatting.GOLD
                : Formatting.GREEN;

        return Text.literal(entry).formatted(color);
    }
}
