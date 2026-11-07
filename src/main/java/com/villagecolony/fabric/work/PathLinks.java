package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.PathStep;
import com.villagecolony.core.type.ServerMemory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O trecho do grafo que está sendo calçado agora, por colônia — Regra 214: quem decide o caminho é o grafo
 * ({@link RoadGraphRefresh}), e quem assenta é o {@link PathPaving}.
 */
public final class PathLinks {

    static {
        ServerMemory.register(PathLinks.class, PathLinks::clearAll);
    }

    /** Um trecho em curso: os passos e o próximo a assentar. */
    static final class Plan {
        final List<PathStep> steps;
        int next;

        Plan(List<PathStep> steps) {
            this.steps = steps;
        }

        boolean done() {
            return next >= steps.size();
        }
    }

    private static final Map<UUID, Plan> PLANS = new HashMap<>();

    private PathLinks() {
    }

    /** O grafo oferece o que falta calçar; só entra quando o trecho anterior acabou. */
    static void offerRepair(UUID colonyId, List<PathStep> steps) {
        if (planOf(colonyId).isEmpty()) {
            PLANS.put(colonyId, new Plan(List.copyOf(steps)));
        }
    }

    /** O trecho em curso desta colônia, se ainda tem passo por assentar. */
    static Optional<Plan> planOf(UUID colonyId) {
        return Optional.ofNullable(PLANS.get(colonyId)).filter(plan -> !plan.done());
    }

    /** O mundo mudou debaixo do trecho: ele sai, e a próxima releitura oferece outro. */
    static void drop(UUID colonyId) {
        PLANS.remove(colonyId);
    }

    static void clearAll() {
        PLANS.clear();
    }
}
