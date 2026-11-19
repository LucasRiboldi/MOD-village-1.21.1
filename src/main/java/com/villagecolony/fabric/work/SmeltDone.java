package com.villagecolony.fabric.work;

import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.item.Item;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A fundição que fez a quantidade pedida do item da tarefa se conclui — Regra 308. Antes ela só parava quando o cru
 * acabava, e com a argila que surge (Regra 301) uma tarefa fez 1.000 terracotas. O que sai para outras obras no meio
 * (SmelterFallback) não conta.
 */
final class SmeltDone {

    static {
        ServerMemory.register(SmeltDone.class, SmeltDone::clearAll);
    }

    private static final Map<UUID, Integer> MADE = new HashMap<>();

    private SmeltDone() {
    }

    /** Soma o que saiu do item da tarefa; ao chegar na quantidade, conclui a tarefa e diz verdadeiro. */
    static boolean reached(Task task, Item made, int count) {
        if (MinecraftTypeAdapter.toItem(task.targetResource()).filter(made::equals).isEmpty()) {
            return false;
        }

        int total = MADE.merge(task.id(), count, Integer::sum);

        if (total < task.amount()) {
            return false;
        }

        MADE.remove(task.id());

        if (task.state() == TaskState.RESERVED) {
            task.start();
        }

        if (task.state() == TaskState.EXECUTING) {
            task.complete();
        }

        return true;
    }

    static void clearAll() {
        MADE.clear();
    }
}
