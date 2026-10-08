package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.service.ColonyAbandonment;
import com.villagecolony.core.coordination.PlanningBudget;
import com.villagecolony.core.coordination.WorkAssignment;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.fabric.integration.RoadExtension;
import com.villagecolony.fabric.integration.SweepDeadline;
import com.villagecolony.fabric.integration.SweepState;
import net.minecraft.server.world.ServerWorld;

import java.util.List;

/**
 * Passagens extras da busca de lote para a vila com jogador dentro — estudo
 * de 01-10, §7-A (Emenda 7).
 *
 * <p>Com a busca pela caixa da vila, uma volta inteira levava ~8,5 min no
 * raio 64, ~26 min numa caixa de 144 e ~1 h 40 numa de 300, a uma passagem
 * por ciclo de 30 s — e a rua só cresce depois de uma volta sem lote. O que
 * se mudou é <b>quando</b>, não <b>o quê</b>: a passagem extra roda o mesmo
 * {@link LotSeeking#seek} do ciclo, com o mesmo prazo de
 * {@link PlanningBudget#DEADLINE_MS}, e só <b>continua</b> uma varredura já em
 * curso — nunca começa uma, nunca abre reparo, nunca passa à frente de obra
 * aberta.
 *
 * <p>Uma passagem por segundo, para uma colônia só: o custo fica em até
 * 15 ms de varredura por segundo. Sem jogador no mundo não roda — o servidor
 * vazio e a bateria de jogo continuam no ritmo do ciclo.
 */
final class SweepCadence {

    private SweepCadence() {
    }

    /** Uma vez por segundo: a primeira vila atendida com varredura em curso ganha uma passagem. */
    static void pass(ServerWorld world) {
        if (world.getPlayers().isEmpty()) {
            return;
        }

        long now = world.getTime();

        for (Colony colony : List.copyOf(VillageColonyMod.COLONIES.all())) {
            if (colony.isActive() && colony.isAttended(now) && passFor(world, colony)) {
                return;
            }
        }
    }

    /**
     * Continua a varredura desta colônia, se ela está no meio de uma.
     *
     * @return se houve passagem
     */
    static boolean passFor(ServerWorld world, Colony colony) {
        // A obra que o save trouxe também ocupa a vaga: ela renasce no
        // planejador do ciclo, e abrir lote antes disso dava duas obras.
        if (VillageColonyMod.CONSTRUCTIONS.openOf(colony.id()).isPresent()
                || VillageColonyMod.CONSTRUCTIONS.pendingOf(colony.id()).isPresent()
                || !SweepState.stillLookingForALot(colony.id())
                || RoadExtension.isGrowing(colony.id())
                || !ColonyAbandonment.plansConstruction(colony)) {
            return false;
        }

        int builders = WorkAssignment.countCapableOf(
                colony.id(), TaskType.BUILD.required(), VillageColonyMod.WORKERS);

        if (builders == 0) {
            return false;
        }

        SweepDeadline.within(PlanningBudget.DEADLINE_MS, () -> LotSeeking.seek(world, colony, builders));

        return true;
    }
}
