package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dois congelamentos no mesmo lugar marcam o encalhado — E47, 2026-09-24.
 *
 * <p>O caso real: um pedreiro parado em {@code -202, 62, -937} por três
 * horas e meia, e três construtores diferentes presos no mesmo ponto em
 * {@code -211, 66, -954}.
 */
class StrandedWorkersTest {

    private static final BlockPos PIT = new BlockPos(-202, 62, -937);

    @AfterEach
    void clear() {
        StrandedWorkers.clearAll();
        VillageColonyMod.WORKERS.clear();
    }

    private static Worker mason() {
        Worker worker = VillageColonyMod.WORKERS.register(UUID.randomUUID(), UUID.randomUUID());
        worker.assign(ProfessionType.MASON);

        return worker;
    }

    @Test
    void oneFreezeIsNotEnough() {
        Worker worker = mason();

        assertFalse(StrandedWorkers.frozeAt(worker.villagerId(), PIT));
        assertFalse(worker.isStranded(),
                "um congelamento pode ser porta fechada ou aldeao no caminho");
    }

    @Test
    void twoFreezesOnTheSameSpotStrandTheWorker() {
        Worker worker = mason();

        StrandedWorkers.frozeAt(worker.villagerId(), PIT);

        assertTrue(StrandedWorkers.frozeAt(worker.villagerId(), PIT.east()));
        assertTrue(worker.isStranded());
        assertTrue(StrandedWorkers.isStranded(worker.villagerId()));
    }

    @Test
    void twoFreezesFarApartAreBadLuckNotATrap() {
        Worker worker = mason();

        StrandedWorkers.frozeAt(worker.villagerId(), PIT);

        assertFalse(StrandedWorkers.frozeAt(worker.villagerId(), PIT.east(10)));
        assertFalse(worker.isStranded());
    }

    @Test
    void releasingPutsTheWorkerBackInTheQueue() {
        Worker worker = mason();

        StrandedWorkers.frozeAt(worker.villagerId(), PIT);
        StrandedWorkers.frozeAt(worker.villagerId(), PIT);
        StrandedWorkers.release(worker.villagerId());

        assertFalse(worker.isStranded());
        assertFalse(StrandedWorkers.isStranded(worker.villagerId()));
    }

    /** Aldeão que não é trabalhador da colônia não vira encalhado de ninguém. */
    @Test
    void anUnknownVillagerIsNeverStranded() {
        UUID stranger = UUID.randomUUID();

        StrandedWorkers.frozeAt(stranger, PIT);

        assertFalse(StrandedWorkers.frozeAt(stranger, PIT));
        assertFalse(StrandedWorkers.isStranded(stranger));
    }
}
