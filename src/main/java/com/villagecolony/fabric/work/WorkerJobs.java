package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;

import java.util.UUID;

/**
 * Larga o trabalho em curso de um aldeão — 2026-10-01.
 *
 * <p><b>Dois condutores no mesmo aldeão.</b> Na sessão das 09:16 o mineiro
 * 199ad062 foi marcado encalhado a y=16, e o trabalho de mineiro continuou
 * correndo ao lado da fuga: no mesmo segundo, "takes a detour", "took 1 from"
 * e "dug a step toward the village". Cada um mandava o aldeão para um lado, e
 * os 32 degraus da fuga foram gastos indo e voltando entre y=16 e y=18.
 *
 * <p>Quem fica preso larga a tarefa: ela volta à fila para outro, e o
 * {@code WorkEligibility} não a devolve a ele enquanto estiver preso. É a
 * mesma limpeza da morte ({@code VillagerLifecycleHandler.forget}), sem
 * esquecer o trabalhador nem o baú dele.
 */
final class WorkerJobs {

    private WorkerJobs() {
    }

    /** Solta as tarefas dele e esquece o trabalho de cada ofício. Devolve quantas tarefas voltaram à fila. */
    static int dropAll(UUID workerId) {
        int released = VillageColonyMod.TASKS.releaseAllOf(workerId);

        MinerWork.forget(workerId);
        SmelterWork.forget(workerId);
        SurfaceGatheringWork.forget(workerId);
        ShepherdWork.forget(workerId);
        FarmerWork.forget(workerId);
        LumberjackWork.forget(workerId);
        CraftingWork.forget(workerId);
        BuilderWork.forget(workerId);

        return released;
    }
}
