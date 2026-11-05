package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.coordination.TaskLoopGuard;
import com.villagecolony.core.task.model.Task;

/** O resultado de um pedido de oficina ou fornalha, para o guarda de laço, com a linha {@code VC_LOOP} (Regra 191). */
final class TaskOutcome {

    private TaskOutcome() {
    }

    static void fruitless(Task task, String why) {
        int hold = TaskLoopGuard.fruitless(task);

        if (hold > 0) {
            VillageColonyMod.LOGGER.info("VC_LOOP colony={} type={} target={} holdCycles={} reason={}",
                    task.colonyId().toString().substring(0, 8), task.type(), task.targetResource(), hold, why);
        }
    }

    static void fruitful(Task task) {
        TaskLoopGuard.fruitful(task);
    }
}
