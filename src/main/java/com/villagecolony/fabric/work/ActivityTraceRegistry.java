package com.villagecolony.fabric.work;

import com.villagecolony.core.telemetry.model.ActivityTrace;
import com.villagecolony.core.telemetry.model.ActivityTraceEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * O {@link ActivityTrace} de cada colônia — decisão 7B, 2026-09-24.
 *
 * <p>Mesmo desenho de {@code MineRegistry}: um mapa em memória, uma
 * entrada por colônia, esvaziado ao parar o servidor e reconstruído do
 * save ao carregar. {@link ActivityTrace} é mutável de propósito — ver o
 * javadoc dela — e um registro trocasse o objeto inteiro a cada evento
 * pagaria o buffer inteiro por tique.
 */
public final class ActivityTraceRegistry {

    private final Map<UUID, ActivityTrace> traces = new HashMap<>();

    /** Acrescenta um evento ao traço desta colônia, criando-o se preciso. */
    public void append(UUID colonyId, ActivityTraceEvent event) {
        Objects.requireNonNull(colonyId, "colonyId");
        Objects.requireNonNull(event, "event");

        traces.computeIfAbsent(colonyId, id -> new ActivityTrace()).append(event);
    }

    /** O traço desta colônia, se ela já tiver um. */
    public Optional<ActivityTrace> of(UUID colonyId) {
        if (colonyId == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(traces.get(colonyId));
    }

    /** Recoloca no registro o traço que o save trouxe. */
    public void restore(UUID colonyId, List<ActivityTraceEvent> newestFirst) {
        Objects.requireNonNull(colonyId, "colonyId");
        Objects.requireNonNull(newestFirst, "newestFirst");

        if (newestFirst.isEmpty()) {
            return;
        }

        ActivityTrace trace = new ActivityTrace();

        // O save grava do mais novo para o mais velho — mesma ordem de
        // newestFirst(); append() precisa da ordem oposta para o traço
        // reconstruído continuar respondendo newestFirst() do jeito certo.
        for (int i = newestFirst.size() - 1; i >= 0; i--) {
            trace.append(newestFirst.get(i));
        }

        traces.put(colonyId, trace);
    }

    /** Todas as colônias com traço, para gravar. */
    public Map<UUID, ActivityTrace> all() {
        return Map.copyOf(traces);
    }

    /** Esquece o traço de uma colônia. Colônia abandonada. */
    public void removeOfColony(UUID colonyId) {
        if (colonyId == null) {
            return;
        }

        traces.remove(colonyId);
    }

    /** Esvazia o registro. Chamado ao abrir e ao fechar o mundo. */
    public void clear() {
        traces.clear();
    }
}
