package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.type.ServerMemory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * O ferro descansa onde a mina não o tem — decisão do autor, 2026-10-21.
 *
 * <p>A vila sempre pede um piso de ferro cru ({@code ColonyGoals.MINERAL_FLOOR}). No deserto raso
 * não há ferro, e os mineiros seguravam a tarefa "quero 64 de ferro" cavando arenito sem achar
 * nenhum: 12 tarefas pegas, 11 soltas, nenhuma concluída numa sessão. Agora, quando um mineiro larga
 * a tarefa de ferro depois de cavar {@link #DUG_WITHOUT_IRON} blocos sem achar ferro, o ferro descansa
 * na colônia por {@link #REST_TICKS}: a meta de ferro cru sai e a tarefa de ferro não é reservada, e a
 * mina cava o que existe (carvão, arenito, pedra). Obra que pede ferro continua pedindo.
 */
public final class IronRest {

    static {
        ServerMemory.register(IronRest.class, IronRest::clearAll);
    }

    /** Blocos cavados sem um ferro antes de o ferro descansar. */
    public static final int DUG_WITHOUT_IRON = 32;

    /** Quanto o ferro descansa: um dia de jogo. */
    public static final long REST_TICKS = 24_000;

    private static final Map<UUID, Long> UNTIL = new HashMap<>();

    private IronRest() {
    }

    /** O mineiro largou a tarefa de ferro: se cavou bastante sem achar nenhum, o ferro descansa. */
    public static void minerGaveUp(UUID colonyId, ResourceType wanted, int ironFound, int dug, long now) {
        if (wanted != ResourceType.RAW_IRON || ironFound > 0 || dug < DUG_WITHOUT_IRON) {
            return;
        }

        UNTIL.put(colonyId, now + REST_TICKS);
        VillageColonyMod.LOGGER.info("Colony {} lets iron rest for a day — the mine dug {} blocks and found none",
                colonyId.toString().substring(0, 8), dug);
    }

    /** Se o ferro descansa agora nesta colônia. */
    public static boolean isResting(UUID colonyId, long now) {
        return UNTIL.getOrDefault(colonyId, Long.MIN_VALUE) > now;
    }

    /** Se esta tarefa é de ferro cru e o ferro descansa: então ela não é reservada. */
    public static boolean blocks(UUID colonyId, Task task, long now) {
        return task.targetResource() == ResourceType.RAW_IRON && isResting(colonyId, now);
    }

    static void clearAll() {
        UNTIL.clear();
    }
}
