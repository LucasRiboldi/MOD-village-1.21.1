package com.villagecolony.core.coordination;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Resultado imutável de uma única fatia de varredura de lote. */
public record ScanReport(
        UUID colonyId,
        int scannedColumns,
        Map<ScanRefusalReason, Integer> refusalCounts,
        boolean complete) {

    public ScanReport {
        Objects.requireNonNull(colonyId, "colonyId");
        Objects.requireNonNull(refusalCounts, "refusalCounts");
        if (scannedColumns < 0) {
            throw new IllegalArgumentException("scannedColumns must not be negative");
        }
        refusalCounts.forEach((reason, count) -> {
            Objects.requireNonNull(reason, "refusal reason");
            if (count == null || count < 0) {
                throw new IllegalArgumentException("refusal counts must not be negative");
            }
        });
        refusalCounts = Map.copyOf(refusalCounts);
    }

    public int refusalCount(ScanRefusalReason reason) {
        return refusalCounts.getOrDefault(reason, 0);
    }

    public static ScanReport empty(UUID colonyId) {
        return new ScanReport(colonyId, 0, Map.of(), true);
    }
}
