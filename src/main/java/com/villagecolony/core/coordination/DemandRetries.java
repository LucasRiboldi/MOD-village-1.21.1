package com.villagecolony.core.coordination;

import java.util.HashMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Conta falhas por demanda durante o dia atual do mundo. */
public final class DemandRetries {

    public record Retry(DemandKey demand, long day, int count) {
    }

    private final Map<DemandKey, Entry> entries = new HashMap<>();

    /** Registra uma falha e devolve sua ordem no dia informado. */
    public int recordFailure(DemandKey demand, long day) {
        Objects.requireNonNull(demand, "demand");
        Entry entry = entries.get(demand);

        if (entry == null || entry.day != day) {
            entries.put(demand, new Entry(day, 1));
            return 1;
        }

        entry.count++;
        return entry.count;
    }

    public List<Retry> snapshot() {
        return entries.entrySet().stream()
                .map(entry -> new Retry(entry.getKey(), entry.getValue().day, entry.getValue().count))
                .toList();
    }

    /** Só falhas do dia atual atravessam um salvamento. */
    public List<Retry> snapshot(long day) {
        return entries.entrySet().stream()
                .filter(entry -> entry.getValue().day == day)
                .map(entry -> new Retry(entry.getKey(), entry.getValue().day, entry.getValue().count))
                .toList();
    }

    public void restore(Collection<Retry> saved) {
        entries.clear();
        for (Retry retry : saved) {
            if (retry.count() > 0) {
                entries.put(retry.demand(), new Entry(retry.day(), retry.count()));
            }
        }
    }

    private static final class Entry {

        private final long day;

        private int count;

        private Entry(long day, int count) {
            this.day = day;
            this.count = count;
        }
    }
}
