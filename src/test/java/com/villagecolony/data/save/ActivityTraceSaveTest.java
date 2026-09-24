package com.villagecolony.data.save;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.ColonyLifecycle;
import com.villagecolony.core.colony.model.ColonyState;
import com.villagecolony.core.telemetry.model.ActivityKind;
import com.villagecolony.core.telemetry.model.ActivityProfession;
import com.villagecolony.core.telemetry.model.ActivityState;
import com.villagecolony.core.telemetry.model.ActivityTrace;
import com.villagecolony.core.telemetry.model.ActivityTraceEvent;
import com.villagecolony.core.telemetry.model.ControlledReason;
import com.villagecolony.core.telemetry.model.TargetKind;
import com.villagecolony.core.type.ColonyPos;
import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O traço circular de atividade atravessa o fechar do mundo —
 * decisão 7B, 2026-09-24.
 */
class ActivityTraceSaveTest {

    private static final ColonyPos CENTER = new ColonyPos(10, 64, 10);

    private static ColonySavedData empty() {
        return ColonySavedData.TYPE.constructor().get();
    }

    private static ColonySavedData roundTrip(ColonySavedData data) {
        NbtCompound nbt = data.writeNbt(new NbtCompound(), null);

        return ColonySavedData.TYPE.deserializer().apply(nbt, null);
    }

    private static Colony colonyAt(UUID id) {
        return Colony.restore(id, CENTER, ColonyState.EXPANSION, ColonyLifecycle.ACTIVE);
    }

    private static ActivityTraceEvent event(int progress) {
        return new ActivityTraceEvent(
                UUID.randomUUID(),
                ActivityProfession.MINER,
                ActivityKind.MINING,
                ActivityState.ABANDONED,
                ControlledReason.WORK_STALLED,
                TargetKind.STONE,
                progress);
    }

    @Test
    void theTraceSurvivesTheRoundTrip() {
        UUID colonyId = UUID.randomUUID();

        ActivityTrace trace = new ActivityTrace();
        trace.append(event(1));
        trace.append(event(2));
        trace.append(event(3));

        ColonySavedData data = empty();
        data.sync(
                List.of(colonyAt(colonyId)), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(),
                Map.of(colonyId, trace));

        Map<UUID, ActivityTrace> loaded = roundTrip(data).activityTraces();

        assertEquals(1, loaded.size());

        List<ActivityTraceEvent> newest = loaded.get(colonyId).newestFirst(3);

        assertEquals(3, newest.get(0).progress());
        assertEquals(2, newest.get(1).progress());
        assertEquals(1, newest.get(2).progress());
    }

    @Test
    void everyFieldOfAnEventSurvives() {
        UUID colonyId = UUID.randomUUID();
        UUID workerId = UUID.randomUUID();

        ActivityTraceEvent original = new ActivityTraceEvent(
                workerId,
                ActivityProfession.SMELTER,
                ActivityKind.SMELTING,
                ActivityState.WAITING,
                ControlledReason.MISSING_MATERIAL,
                TargetKind.ORE,
                7);

        ActivityTrace trace = new ActivityTrace();
        trace.append(original);

        ColonySavedData data = empty();
        data.sync(
                List.of(colonyAt(colonyId)), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(),
                Map.of(colonyId, trace));

        ActivityTraceEvent back =
                roundTrip(data).activityTraces().get(colonyId).newestFirst(1).get(0);

        assertEquals(original, back);
    }

    /** Enum de uma versão futura não pode impedir o carregamento do mundo. */
    @Test
    void unknownSavedValuesBecomeUnknown() {
        UUID colonyId = UUID.randomUUID();

        ActivityTrace trace = new ActivityTrace();
        trace.append(event(1));

        ColonySavedData data = empty();
        data.sync(
                List.of(colonyAt(colonyId)), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(),
                Map.of(colonyId, trace));

        NbtCompound nbt = data.writeNbt(new NbtCompound(), null);
        nbt.getList("activityTraces", 10)
                .getCompound(0)
                .getList("events", 10)
                .getCompound(0)
                .putString("activity", "FUTURE_ACTIVITY");

        ActivityTraceEvent back = ColonySavedData.TYPE.deserializer().apply(nbt, null)
                .activityTraces().get(colonyId).newestFirst(1).get(0);

        assertEquals(ActivityKind.UNKNOWN, back.activity());
    }

    /** Save anterior a esta versão não tem a lista; a colônia abre sem traço. */
    @Test
    void aColonyWithoutATraceLoadsEmpty() {
        UUID colonyId = UUID.randomUUID();

        ColonySavedData data = empty();
        data.sync(List.of(colonyAt(colonyId)), List.of());

        assertTrue(roundTrip(data).activityTraces().isEmpty());
    }

    /** Traço de colônia desconhecida é descartado, como tudo o mais. */
    @Test
    void aTraceOfAnUnknownColonyIsDropped() {
        ActivityTrace trace = new ActivityTrace();
        trace.append(event(1));

        ColonySavedData data = empty();
        data.sync(
                List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(),
                Map.of(UUID.randomUUID(), trace));

        assertTrue(roundTrip(data).activityTraces().isEmpty());
    }

    /** O contador de estouro atravessa o save junto com os eventos. */
    @Test
    void overflowCountSurvivesTheRoundTrip() {
        UUID colonyId = UUID.randomUUID();

        ActivityTrace trace = new ActivityTrace();
        IntStream.range(0, ActivityTrace.CAPACITY + 5).forEach(index -> trace.append(event(index)));

        ColonySavedData data = empty();
        data.sync(
                List.of(colonyAt(colonyId)), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(),
                Map.of(colonyId, trace));

        ActivityTrace loaded = roundTrip(data).activityTraces().get(colonyId);

        assertEquals(5, loaded.overflowCount());
        assertEquals(ActivityTrace.CAPACITY, loaded.size());
    }
}
