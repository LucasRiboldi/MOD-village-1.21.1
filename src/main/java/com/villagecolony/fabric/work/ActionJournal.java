package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskTransitions;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.Worker;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * O diário de ações: uma linha JSON por ação de cada aldeão, em
 * {@code logs/villagecolony-actions-<sessão>.jsonl} — pedido do autor,
 * 2026-10-08, para diagnosticar e avaliar o mod sem ler frase solta do log.
 *
 * <p>Duas fontes: toda transição de tarefa ({@link TaskTransitions}, as oito
 * profissões pelo mesmo caminho) e as ações concretas ({@link #action}: bloco
 * posto, pedra tirada, árvore derrubada, peça feita, colheita, tosquia,
 * encalhe). {@code scripts/action_report.py} lê o arquivo.
 *
 * <p>Grava em lote a cada {@link #FLUSH_EVERY} tiques e ao parar o servidor;
 * {@code -Dvillagecolony.actions=false} desliga.
 */
public final class ActionJournal {

    static {
        ServerMemory.register(ActionJournal.class, ActionJournal::clearAll);
    }

    /** De quanto em quanto tempo o lote vai para o disco: 10 s. */
    static final int FLUSH_EVERY = 200;

    /** Teto do lote em memória: passa disto e grava antes da hora. */
    static final int MAX_PENDING = 2_000;

    private static final DateTimeFormatter SESSION = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private static final List<String> PENDING = new ArrayList<>();

    /** Quando cada tarefa foi pega, para a linha de fim dizer quanto durou. */
    private static final Map<UUID, Long> TAKEN_AT = new HashMap<>();

    private static @Nullable Path file;

    private static long now;

    private ActionJournal() {
    }

    public static void register() {
        if (!Boolean.parseBoolean(System.getProperty("villagecolony.actions", "true"))) {
            return;
        }

        ServerLifecycleEvents.SERVER_STARTED.register(ActionJournal::open);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> flush());
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            now = server.getOverworld().getTime();

            if (now % FLUSH_EVERY == 0) {
                flush();
            }
        });
        TaskTransitions.observe(ActionJournal::onTask);
    }

    private static void open(MinecraftServer server) {
        Path logs = server.getRunDirectory().resolve("logs");

        try {
            Files.createDirectories(logs);
            file = logs.resolve("villagecolony-actions-" + LocalDateTime.now(ZoneId.systemDefault()).format(SESSION) + ".jsonl");
        } catch (IOException failure) {
            VillageColonyMod.LOGGER.warn("The action journal is off — cannot create {}", logs, failure);
            file = null;
        }
    }

    /** Cada passo de tarefa: pegou, começou, terminou, soltou, cancelou. */
    static void onTask(Task task, TaskState from, TaskState to, UUID worker) {
        String action = switch (to) {
            case RESERVED -> "TASK_TAKEN";
            case EXECUTING -> "TASK_STARTED";
            case COMPLETED -> "TASK_DONE";
            case AVAILABLE -> "TASK_RELEASED";
            case CANCELLED -> "TASK_CANCELLED";
        };

        if (to == TaskState.RESERVED) {
            TAKEN_AT.put(task.id(), now);
        }

        Long since = to == TaskState.RESERVED || to == TaskState.EXECUTING
                ? null
                : TAKEN_AT.remove(task.id());

        append(worker, action, task.type().name(), task.targetResource().name(), task.amount(), null,
                since == null ? -1 : now - since, from.name());
    }

    /**
     * Uma ação concreta de um aldeão.
     *
     * @param action o verbo, em maiúsculas: {@code PLACED}, {@code MINED}, {@code CRAFTED}…
     * @param what o bloco ou item
     * @param at onde, ou nulo
     * @param detail uma frase curta, ou vazio
     */
    public static void action(UUID worker, String action, String what, int amount, @Nullable BlockPos at,
            String detail) {
        append(worker, action, "", what, amount, at, -1, detail);
    }

    /** O nome curto de um item, para a coluna {@code what}. */
    public static String idOf(net.minecraft.item.Item item) {
        return net.minecraft.registry.Registries.ITEM.getId(item).getPath();
    }

    private static void append(UUID worker, String action, String task, String what, int amount,
            @Nullable BlockPos at, long ticks, String detail) {

        if (file == null) {
            return;
        }

        String profession = VillageColonyMod.WORKERS.find(worker)
                .flatMap(Worker::profession)
                .map(Enum::name)
                .orElse("NONE");
        String colony = VillageColonyMod.WORKERS.find(worker)
                .map(found -> found.colonyId().toString().substring(0, 8))
                .orElse("");

        StringBuilder line = new StringBuilder(160)
                .append("{\"t\":").append(now)
                .append(",\"colony\":\"").append(colony)
                .append("\",\"worker\":\"").append(worker.toString(), 0, 8)
                .append("\",\"prof\":\"").append(profession)
                .append("\",\"action\":\"").append(action).append('"');

        field(line, "task", task);
        field(line, "what", what.toLowerCase(Locale.ROOT));

        if (amount != 0) {
            line.append(",\"n\":").append(amount);
        }

        if (at != null) {
            line.append(",\"x\":").append(at.getX()).append(",\"y\":").append(at.getY())
                    .append(",\"z\":").append(at.getZ());
        }

        if (ticks >= 0) {
            line.append(",\"ticks\":").append(ticks);
        }

        field(line, "detail", detail);

        PENDING.add(line.append('}').toString());

        if (PENDING.size() >= MAX_PENDING) {
            flush();
        }
    }

    private static void field(StringBuilder line, String name, String value) {
        if (value.isEmpty()) {
            return;
        }

        line.append(",\"").append(name).append("\":\"");

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            if (c == '"' || c == '\\') {
                line.append('\\').append(c);
            } else if (c >= ' ') {
                line.append(c);
            }
        }

        line.append('"');
    }

    /** O lote vai para o disco; falha de disco desliga o diário, nunca o jogo. */
    static void flush() {
        Path target = file;

        if (target == null || PENDING.isEmpty()) {
            return;
        }

        try {
            Files.write(target, PENDING, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException failure) {
            VillageColonyMod.LOGGER.warn("The action journal stopped — cannot write {}", target, failure);
            file = null;
        }

        PENDING.clear();
    }

    /** Para o teste: a linha montada, sem disco. */
    static List<String> pending() {
        return List.copyOf(PENDING);
    }

    /** Para o teste: o arquivo de agora, para devolvê-lo depois. */
    static @Nullable Path file() {
        return file;
    }

    /** Para o teste: grava neste arquivo em vez do da sessão. */
    static void writeTo(@Nullable Path target) {
        file = target;
    }

    /** Grava o que falta antes de esquecer: a troca de mundo não perde o último lote. */
    static void clearAll() {
        flush();
        PENDING.clear();
        TAKEN_AT.clear();
    }
}
