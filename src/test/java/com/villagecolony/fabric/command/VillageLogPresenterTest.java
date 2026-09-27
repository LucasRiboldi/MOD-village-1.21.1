package com.villagecolony.fabric.command;

import com.villagecolony.core.telemetry.model.ActivityKind;
import com.villagecolony.core.telemetry.model.ActivityProfession;
import com.villagecolony.core.telemetry.model.ActivityState;
import com.villagecolony.core.telemetry.model.ActivityTrace;
import com.villagecolony.core.telemetry.model.ActivityTraceEvent;
import com.villagecolony.core.telemetry.model.ControlledReason;
import com.villagecolony.core.telemetry.model.TargetKind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Garante que o menu não despeje o traço técnico no chat do jogador. */
class VillageLogPresenterTest {

    @Test
    void explainsWhenTheVillageHasNotReportedAnyActivityYet() {
        assertEquals(
                List.of("Ainda não há atividades registradas nesta vila."),
                VillageLogPresenter.entries(new ActivityTrace(), 8));
    }

    @Test
    void keepsOnlyTheNewestStatusOfEachWorker() {
        UUID miner = UUID.randomUUID();
        ActivityTrace trace = new ActivityTrace();
        trace.append(event(miner, ActivityProfession.MINER, ActivityState.IDLE,
                ControlledReason.NONE, TargetKind.STONE));
        trace.append(event(miner, ActivityProfession.MINER, ActivityState.WAITING,
                ControlledReason.NO_TARGET, TargetKind.STONE));

        assertEquals(
                List.of("[AGUARDANDO] Mineiro: procurando pedra para minerar."),
                VillageLogPresenter.entries(trace, 8));
    }

    @Test
    void marksAnAbandonedTaskAsAProblemThePlayerCanUnderstand() {
        ActivityTrace trace = new ActivityTrace();
        trace.append(event(UUID.randomUUID(), ActivityProfession.LUMBERJACK,
                ActivityState.ABANDONED, ControlledReason.WORK_STALLED, TargetKind.WOOD));

        assertEquals(
                List.of("[TRAVADO] Lenhador: a atividade parou por falta de progresso."),
                VillageLogPresenter.entries(trace, 8));
    }

    private static ActivityTraceEvent event(
            UUID workerId,
            ActivityProfession profession,
            ActivityState state,
            ControlledReason reason,
            TargetKind target) {
        return new ActivityTraceEvent(
                workerId, profession, ActivityKind.MINING, state, reason, target, 0);
    }
}
