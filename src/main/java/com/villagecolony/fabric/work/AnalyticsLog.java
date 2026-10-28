package com.villagecolony.fabric.work;

import com.google.gson.JsonObject;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Os dados que ficam — pedido do autor (2026-10-28): o retrato de cada vila ({@link ColonyState}) a cada
 * cinco minutos, e cada nota do jogador, gravados num arquivo por dia na pasta do mundo
 * ({@code <mundo>/villagecolony/analytics/AAAA-MM-DD.jsonl}), uma linha JSON por registro. O log do jogo
 * gira e some; este arquivo fica com o mundo, entre sessões e versões, para o
 * {@code scripts/state_report.py} comparar. Cada registro também sai no log como {@code VC_STATE}.
 */
public final class AnalyticsLog {

    /** Cinco minutos, o passo do {@code VC_TIME} e do {@code VC_BIOME}. */
    static final long EVERY = 6_000;

    /** O arquivo passa disto e o dia segue noutro: o registro nunca cresce sem teto. */
    static final long MAX_BYTES = 32L * 1024 * 1024;

    private AnalyticsLog() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % EVERY == EVERY / 2) {
                recordAll(server.getOverworld());
            }
        });
    }

    /** Um retrato de cada vila atendida agora. */
    static void recordAll(ServerWorld world) {
        long now = world.getTime();

        for (Colony colony : List.copyOf(VillageColonyMod.COLONIES.all())) {
            if (colony.canWork() && colony.isAttended(now)) {
                record(world, colony, "state");
            }
        }
    }

    /** Grava o retrato desta vila; devolve o arquivo, se gravou. */
    public static Optional<Path> record(ServerWorld world, Colony colony, String kind) {
        JsonObject state = ColonyState.of(world, colony);
        state.addProperty("kind", kind);
        VillageColonyMod.LOGGER.info("VC_STATE {}", state);

        return append(world, state);
    }

    /** Grava uma nota do jogador ({@code /vc bioma nota}) junto dos retratos. */
    public static Optional<Path> note(ServerWorld world, JsonObject note) {
        note.addProperty("kind", "note");
        note.addProperty("schema", ColonyState.SCHEMA);
        note.addProperty("mod", ColonyState.version());
        note.addProperty("tick", world.getTime());

        return append(world, note);
    }

    private static Optional<Path> append(ServerWorld world, JsonObject record) {
        // O instante real, para cruzar com o Spark e com o log do jogo.
        record.addProperty("at", java.time.Instant.now().toString());
        Path folder = world.getServer().getSavePath(WorldSavePath.ROOT).resolve("villagecolony").resolve("analytics");
        Path file = folder.resolve(LocalDate.now(java.time.ZoneId.systemDefault()) + ".jsonl");

        try {
            Files.createDirectories(folder);

            if (Files.exists(file) && Files.size(file) > MAX_BYTES) {
                file = folder.resolve(LocalDate.now(java.time.ZoneId.systemDefault()) + "-" + world.getTime() + ".jsonl");
            }

            Files.writeString(file, record + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            return Optional.of(file);
        } catch (IOException failed) {
            VillageColonyMod.LOGGER.warn("Village Colony could not write the analytics record to {}: {}", file,
                    failed.getMessage());

            return Optional.empty();
        }
    }
}
