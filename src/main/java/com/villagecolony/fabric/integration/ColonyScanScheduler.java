package com.villagecolony.fabric.integration;

import com.villagecolony.core.coordination.ScanReport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Limita a quantidade de colônias que entra na fase cara de varredura. */
public final class ColonyScanScheduler {

    @FunctionalInterface
    public interface SliceScanner {
        ScanReport scanOneSlice(UUID colonyId);
    }

    private final int budget;
    private final SliceScanner scanner;
    private UUID resumeAfter;

    private ColonyScanScheduler(int budget, SliceScanner scanner) {
        if (budget < 1) {
            throw new IllegalArgumentException("budget must be positive");
        }
        this.budget = budget;
        this.scanner = Objects.requireNonNull(scanner, "scanner");
    }

    public static ColonyScanScheduler withBudget(int budget, SliceScanner scanner) {
        return new ColonyScanScheduler(budget, scanner);
    }

    /** Executa no máximo uma fatia por colônia escolhida nesta passagem. */
    public int runCycle(Collection<UUID> colonyIds) {
        return choose(colonyIds, List.of()).stream()
                .map(scanner::scanOneSlice)
                .mapToInt(ScanReport::scannedColumns)
                .sum();
    }

    /** Seleciona a mesma ordem usada pelo planejador, com prioridade limitada. */
    public Set<UUID> choose(Collection<UUID> colonyIds, Collection<UUID> watched) {
        Objects.requireNonNull(colonyIds, "colonyIds");
        Objects.requireNonNull(watched, "watched");
        List<UUID> active = new ArrayList<>(new LinkedHashSet<>(colonyIds));
        Set<UUID> watchedIds = Set.copyOf(watched);

        if (active.isEmpty()) {
            resumeAfter = null;
            return Set.of();
        }
        if (active.size() <= budget) {
            resumeAfter = null;
            return Collections.unmodifiableSet(new LinkedHashSet<>(active));
        }

        Set<UUID> chosen = new LinkedHashSet<>(budget);
        for (UUID colonyId : active) {
            if (chosen.size() >= budget) {
                break;
            }
            if (watchedIds.contains(colonyId)) {
                chosen.add(colonyId);
            }
        }

        UUID lastRoundRobin = null;
        int start = startingAt(active);
        for (int step = 0; step < active.size() && chosen.size() < budget; step++) {
            UUID colonyId = active.get((start + step) % active.size());
            if (chosen.add(colonyId)) {
                lastRoundRobin = colonyId;
            }
        }
        if (lastRoundRobin != null) {
            resumeAfter = lastRoundRobin;
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(chosen));
    }

    public void clear() {
        resumeAfter = null;
    }

    private int startingAt(List<UUID> active) {
        if (resumeAfter == null) {
            return 0;
        }
        int last = active.indexOf(resumeAfter);
        return last < 0 ? 0 : (last + 1) % active.size();
    }
}
