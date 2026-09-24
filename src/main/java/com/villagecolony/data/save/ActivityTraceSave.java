package com.villagecolony.data.save;

import com.villagecolony.core.telemetry.model.ActivityKind;
import com.villagecolony.core.telemetry.model.ActivityProfession;
import com.villagecolony.core.telemetry.model.ActivityState;
import com.villagecolony.core.telemetry.model.ActivityTrace;
import com.villagecolony.core.telemetry.model.ActivityTraceEvent;
import com.villagecolony.core.telemetry.model.ControlledReason;
import com.villagecolony.core.telemetry.model.TargetKind;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * O traço circular de atividade de cada colônia, indo e voltando do disco
 * — decisão 7B, 2026-09-24.
 *
 * <p>Cada enum do evento declara {@code UNKNOWN}, e é para ele que um
 * valor de uma versão futura ou de um save editado à mão cai — a mesma
 * régua de {@code readState}/{@code readProfession} em
 * {@link ColonySavedData}: um enum desconhecido nunca pode impedir o
 * carregamento do mundo.
 *
 * <p><b>Ordem de gravação: mais antigo primeiro.</b> É a ordem natural de
 * {@link ActivityTrace#newestFirst}, invertida — grava-se do jeito que o
 * {@link ActivityTrace} reconstruído precisa receber os {@code append}
 * de volta, para que o mais novo do save continue sendo o mais novo
 * depois de reler.
 */
final class ActivityTraceSave {

    private static final String TRACES = "activityTraces";
    private static final String COLONY_ID = "colonyId";
    private static final String EVENTS = "events";
    private static final String OVERFLOW = "overflow";

    private static final String WORKER_ID = "workerId";
    private static final String PROFESSION = "profession";
    private static final String ACTIVITY = "activity";
    private static final String STATE = "state";
    private static final String REASON = "reason";
    private static final String TARGET = "target";
    private static final String PROGRESS = "progress";

    private ActivityTraceSave() {
    }

    static void write(NbtCompound nbt, Map<UUID, ActivityTrace> traces) {
        NbtList list = new NbtList();

        for (Map.Entry<UUID, ActivityTrace> entry : traces.entrySet()) {
            NbtCompound colonyEntry = new NbtCompound();

            colonyEntry.putUuid(COLONY_ID, entry.getKey());

            ActivityTrace trace = entry.getValue();
            List<ActivityTraceEvent> newestFirst = trace.newestFirst(ActivityTrace.CAPACITY);

            NbtList events = new NbtList();

            // Do mais antigo para o mais novo — o inverso de newestFirst —
            // para que restore() precise só de append() em ordem, sem
            // reordenar nada.
            for (int i = newestFirst.size() - 1; i >= 0; i--) {
                events.add(writeEvent(newestFirst.get(i)));
            }

            colonyEntry.put(EVENTS, events);
            colonyEntry.putLong(OVERFLOW, trace.overflowCount());

            list.add(colonyEntry);
        }

        nbt.put(TRACES, list);
    }

    private static NbtCompound writeEvent(ActivityTraceEvent event) {
        NbtCompound entry = new NbtCompound();

        entry.putUuid(WORKER_ID, event.workerId());
        entry.putString(PROFESSION, event.profession().name());
        entry.putString(ACTIVITY, event.activity().name());
        entry.putString(STATE, event.state().name());
        entry.putString(REASON, event.reason().name());
        entry.putString(TARGET, event.target().name());
        entry.putInt(PROGRESS, event.progress());

        return entry;
    }

    /**
     * Os traços do disco, sem os de colônia desconhecida.
     *
     * <p>{@link #OVERFLOW} não é reconstruído por {@code append()}: uma
     * colônia com 20.000 eventos já viveu 3.616 estouros antes desta
     * sessão, e recontar do zero mentiria sobre quanto histórico já se
     * perdeu. O contador lido é aplicado depois de todo evento entrar.
     */
    static Map<UUID, ActivityTrace> read(NbtCompound nbt, Set<UUID> knownColonies) {
        Map<UUID, ActivityTrace> found = new HashMap<>();

        NbtList list = nbt.getList(TRACES, NbtElement.COMPOUND_TYPE);

        for (int i = 0; i < list.size(); i++) {
            NbtCompound colonyEntry = list.getCompound(i);

            if (!colonyEntry.containsUuid(COLONY_ID)) {
                continue;
            }

            UUID colonyId = colonyEntry.getUuid(COLONY_ID);

            if (!knownColonies.contains(colonyId)) {
                continue;
            }

            ActivityTrace trace = new ActivityTrace();

            NbtList events = colonyEntry.getList(EVENTS, NbtElement.COMPOUND_TYPE);

            for (int e = 0; e < events.size(); e++) {
                readEvent(events.getCompound(e)).ifPresent(trace::append);
            }

            long overflow = colonyEntry.getLong(OVERFLOW);

            trace.restoreOverflow(Math.max(0, overflow));

            found.put(colonyId, trace);
        }

        return found;
    }

    private static Optional<ActivityTraceEvent> readEvent(NbtCompound entry) {
        if (!entry.containsUuid(WORKER_ID)) {
            return Optional.empty();
        }

        int progress = entry.getInt(PROGRESS);

        if (progress < 0) {
            return Optional.empty();
        }

        return Optional.of(new ActivityTraceEvent(
                entry.getUuid(WORKER_ID),
                enumOf(ActivityProfession.class, entry.getString(PROFESSION), ActivityProfession.UNKNOWN),
                enumOf(ActivityKind.class, entry.getString(ACTIVITY), ActivityKind.UNKNOWN),
                enumOf(ActivityState.class, entry.getString(STATE), ActivityState.UNKNOWN),
                enumOf(ControlledReason.class, entry.getString(REASON), ControlledReason.UNKNOWN),
                enumOf(TargetKind.class, entry.getString(TARGET), TargetKind.UNKNOWN),
                progress));
    }

    private static <E extends Enum<E>> E enumOf(Class<E> type, String name, E fallback) {
        for (E value : type.getEnumConstants()) {
            if (value.name().equals(name)) {
                return value;
            }
        }

        return fallback;
    }
}
