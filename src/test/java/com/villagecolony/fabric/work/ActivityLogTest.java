package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.coordination.IdleReason;
import com.villagecolony.core.worker.model.ProfessionType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Garante o contrato maquina-legivel da telemetria, sem dados do jogador. */
class ActivityLogTest {

    @Test
    void controlledTransitionsUseTheVersionedActivityFormat() {
        Captured captured = Captured.attached();
        try {
            ActivityLog.waiting("collect_stone", IdleReason.NO_TASK);
            ActivityLog.recovered("collect_stone", IdleReason.NO_TASK);
            ActivityLog.failed(ProfessionType.BUILDER, "build_house");
            ActivityLog.abandoned(ProfessionType.BUILDER, "build_house");
        } finally {
            captured.detach();
        }

        assertEquals(4, captured.lines.size());
        assertTrue(captured.lines.contains(
                "VC_ACTIVITY version=1 profession=MINER activity=MINING outcome=WAITING reason=NO_TASK"));
        assertTrue(captured.lines.contains(
                "VC_ACTIVITY version=1 profession=MINER activity=MINING outcome=RECOVERED reason=NO_TASK"));
        assertTrue(captured.lines.contains(
                "VC_ACTIVITY version=1 profession=BUILDER activity=BUILD_HOUSE outcome=ERROR reason=WORK_STALLED"));
        assertTrue(captured.lines.contains(
                "VC_ACTIVITY version=1 profession=BUILDER activity=BUILD_HOUSE outcome=ABANDONED reason=WORK_STALLED"));
    }

    private static final class Captured extends AbstractAppender {

        private final List<String> lines = new ArrayList<>();

        private Captured() {
            super("village-colony-activity-log-test", null, null, true, Property.EMPTY_ARRAY);
        }

        static Captured attached() {
            Captured captured = new Captured();
            captured.start();
            ((org.apache.logging.log4j.core.Logger)
                    LogManager.getLogger(VillageColonyMod.MOD_ID)).addAppender(captured);
            return captured;
        }

        void detach() {
            ((org.apache.logging.log4j.core.Logger)
                    LogManager.getLogger(VillageColonyMod.MOD_ID)).removeAppender(this);
            stop();
        }

        @Override
        public void append(LogEvent event) {
            String line = event.getMessage().getFormattedMessage();
            if (line.startsWith("VC_ACTIVITY ")) {
                lines.add(line);
            }
        }
    }
}
