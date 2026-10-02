package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.coordination.LastShortage;
import com.villagecolony.core.task.model.Task;

/**
 * O pedido seguinte nasce quando o anterior termina — F-2, 2026-10-02.
 *
 * <p><b>O que se via.</b> O F10 (30-09) entrega a tarefa aberta a quem ficou
 * livre, uma vez por segundo. Mas a tarefa só <b>nascia</b> no ciclo da
 * colônia, a cada 30 s: o lenhador que fechava o pedido de madeira ficava
 * parado ao lado do baú até o ciclo seguinte abrir outro igual — com a mesma
 * falta de madeira.
 *
 * <p><b>O que muda.</b> Concluído um pedido de recurso, se o último ciclo
 * mediu falta desse recurso e não há outro pedido dele à espera, abre-se um
 * igual na hora; o {@code IdleHands} o entrega no segundo seguinte. Um por
 * conclusão, então a fila não passa do número de mãos que o ciclo abriu. Quem
 * cancela o pedido que perdeu o motivo continua sendo o ciclo.
 */
final class TaskChain {

    private TaskChain() {
    }

    /** O pedido {@code done} acabou de ser concluído. */
    static void next(Task done) {
        if (!done.type().isResourceRequest()
                || !LastShortage.stillMissing(done.colonyId(), done.targetResource())) {
            return;
        }

        for (Task waiting : VillageColonyMod.TASKS.availableFor(done.colonyId())) {
            if (waiting.type() == done.type() && waiting.targetResource() == done.targetResource()) {
                return;
            }
        }

        VillageColonyMod.TASKS.create(
                done.colonyId(), done.type(), done.priority(), done.targetResource(), done.amount());

        VillageColonyMod.LOGGER.info(
                "Colony {} chained the next {} request for {} as soon as one was done — the shortage remains",
                done.colonyId(), done.type(), done.targetResource());
    }
}
