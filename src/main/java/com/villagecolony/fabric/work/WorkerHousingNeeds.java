package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ServerMemory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Necessidade derivada de moradia segura para trabalhadores sem bau. */
public final class WorkerHousingNeeds {

    static {
        ServerMemory.register(WorkerHousingNeeds.class, WorkerHousingNeeds::clearAll);
    }

    private static final Map<UUID, Set<UUID>> WORKERS = new HashMap<>();

    private WorkerHousingNeeds() {
    }

    public static void mark(UUID colonyId, UUID workerId) {
        WORKERS.computeIfAbsent(colonyId, ignored -> new HashSet<>()).add(workerId);
    }

    public static void resolve(UUID colonyId, UUID workerId) {
        Set<UUID> workers = WORKERS.get(colonyId);
        if (workers == null) {
            return;
        }

        workers.remove(workerId);
        if (workers.isEmpty()) {
            WORKERS.remove(colonyId);
        }
    }

    public static boolean needsHouse(UUID colonyId) {
        return WORKERS.containsKey(colonyId);
    }

    public static void clearAll() {
        WORKERS.clear();
    }
}
