package com.villagecolony.core.worker.model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Conjunto completo e ordenado das políticas de profissão de um mundo. */
public final class ProfessionPolicySet {

    private final Map<ProfessionType, ProfessionPolicy> policies;
    private final List<ProfessionType> hiringOrder;

    public ProfessionPolicySet(Map<ProfessionType, ProfessionPolicy> policies,
            List<ProfessionType> hiringOrder) {
        Objects.requireNonNull(policies, "policies");
        Objects.requireNonNull(hiringOrder, "hiringOrder");
        EnumMap<ProfessionType, ProfessionPolicy> copy = new EnumMap<>(ProfessionType.class);
        for (ProfessionType type : ProfessionType.values()) {
            copy.put(type, Objects.requireNonNull(policies.get(type), "missing policy: " + type));
        }
        if (hiringOrder.size() != ProfessionType.values().length
                || hiringOrder.stream().distinct().count() != ProfessionType.values().length
                || !hiringOrder.containsAll(List.of(ProfessionType.values()))) {
            throw new IllegalArgumentException("hiringOrder must contain every profession exactly once");
        }
        this.policies = Map.copyOf(copy);
        this.hiringOrder = List.copyOf(hiringOrder);
    }

    public static ProfessionPolicySet defaults() {
        EnumMap<ProfessionType, ProfessionPolicy> defaults = new EnumMap<>(ProfessionType.class);
        for (ProfessionType type : ProfessionType.values()) {
            defaults.put(type, ProfessionPolicy.defaults());
        }
        return new ProfessionPolicySet(defaults, List.of(
                ProfessionType.MINER, ProfessionType.LUMBERJACK, ProfessionType.MASON,
                ProfessionType.SMELTER, ProfessionType.CARPENTER, ProfessionType.FARMER,
                ProfessionType.SHEPHERD, ProfessionType.BUILDER));
    }

    public ProfessionPolicy policyOf(ProfessionType type) {
        return policies.get(Objects.requireNonNull(type, "type"));
    }

    public Map<ProfessionType, ProfessionPolicy> policies() {
        return policies;
    }

    public List<ProfessionType> hiringOrder() {
        return hiringOrder;
    }

    /** Preserva a ordem global, removendo papéis que não participam desta fase. */
    public List<ProfessionType> orderFor(List<ProfessionType> eligible) {
        Objects.requireNonNull(eligible, "eligible");
        List<ProfessionType> ordered = new ArrayList<>();
        for (ProfessionType type : hiringOrder) {
            if (eligible.contains(type)) {
                ordered.add(type);
            }
        }
        return List.copyOf(ordered);
    }
}
