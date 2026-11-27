package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ServerMemory;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * O diário dos blocos — Regra 384 (autor 26-11, K3 do jogo da 0.4.148: "uma função para registrar cada bloco que o zelador
 * muda, cada bloco que o player muda para poder comparar depois"). Cada bloco que o jogador muda e cada bloco que um
 * sistema do mod muda pelo {@link EditLedger} vira uma linha em {@code logs/villagecolony-blocks-<sessão>.jsonl}: o tique,
 * quem (jogador, ou o sistema do mod), a posição, o bloco de antes quando se sabe e o de depois.
 * {@code scripts/block_report.py} compara os dois lados.
 */
public final class BlockJournal {

    /** De quanto em quanto tempo o lote vai para o disco. */
    static final int FLUSH_EVERY = 200;

    /** Acima disto o lote vai para o disco na hora. */
    static final int MAX_PENDING = 2_000;

    private static final DateTimeFormatter SESSION = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private static final List<String> PENDING = new ArrayList<>();

    private static @Nullable Path file;

    static {
        ServerMemory.register(BlockJournal.class, BlockJournal::flush);
    }

    private BlockJournal() {
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(BlockJournal::open);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> flush());
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getOverworld().getTime() % FLUSH_EVERY == 0) {
                flush();
            }
        });
    }

    private static void open(MinecraftServer server) {
        Path logs = server.getRunDirectory().resolve("logs");

        try {
            Files.createDirectories(logs);
            file = logs.resolve("villagecolony-blocks-" + LocalDateTime.now(ZoneId.systemDefault()).format(SESSION)
                    + ".jsonl");
        } catch (IOException failure) {
            VillageColonyMod.LOGGER.warn("The block journal is off — cannot create {}", logs, failure);
            file = null;
        }
    }

    /** Um bloco mudou: {@code who} é {@code player} ou {@code mod:<sistema>}; {@code before} vazio quando não se sabe. */
    static void record(long tick, String who, BlockPos at, String before, String after) {
        PENDING.add(line(tick, who, at, before, after));

        if (PENDING.size() >= MAX_PENDING) {
            flush();
        }
    }

    /** A linha do diário. */
    static String line(long tick, String who, BlockPos at, String before, String after) {
        return "{\"t\":" + tick + ",\"who\":\"" + who + "\",\"x\":" + at.getX() + ",\"y\":" + at.getY() + ",\"z\":"
                + at.getZ() + (before.isEmpty() ? "" : ",\"was\":\"" + before + "\"") + ",\"now\":\"" + after + "\"}";
    }

    /** O lote vai para o disco; falha de disco desliga o diário, nunca o jogo. */
    public static void flush() {
        Path target = file;

        if (target == null || PENDING.isEmpty()) {
            PENDING.clear();
            return;
        }

        try {
            Files.write(target, PENDING, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException failure) {
            VillageColonyMod.LOGGER.warn("The block journal stopped — cannot write {}", target, failure);
            file = null;
        }

        PENDING.clear();
    }

    /** Para o teste: o que ainda não foi para o disco. */
    static List<String> pending() {
        return List.copyOf(PENDING);
    }
}
