package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Quem pode remodelar o terreno para escalar — regra do autor, 2026-10-15.
 *
 * <p><b>Só o mineiro</b> põe bloco (pilar) ou cava (túnel) para subir. As outras profissões sobem
 * apenas por degrau natural; sem um ao alcance, procuram num raio maior e, se nada, desistem sem tocar
 * o mundo. Separado do {@link ClimbOut} para ele caber no teto de 500 linhas (F2 do plano).
 */
final class ClimbPolicy {

    private ClimbPolicy() {
    }

    /** Se esta profissão pode pôr bloco ou cavar para escalar — só o mineiro. Lógica pura. */
    static boolean mayReshapeTerrain(ProfessionType profession) {
        return profession == ProfessionType.MINER;
    }

    /** A profissão deste trabalhador, ou {@code null} quando não se conhece. */
    static ProfessionType professionOf(UUID workerId) {
        return VillageColonyMod.WORKERS.find(workerId).flatMap(Worker::profession).orElse(null);
    }

    /** Se este trabalhador pode remodelar o terreno para escalar. */
    static boolean mayReshape(UUID workerId) {
        return mayReshapeTerrain(professionOf(workerId));
    }

    /**
     * Um não-mineiro encalhado sem degrau natural ao alcance larga a escalada e volta à agenda — sem
     * pôr bloco nem cavar (regra do autor, 2026-10-15). Não é desistir do ofício; é parar de insistir
     * num bolsão que ele não sobe sozinho: a navegação normal e a próxima tarefa o tiram daqui.
     *
     * @param forgetClimb apaga o estado de escalada deste trabalhador (o {@code CLIMBS.remove} do
     *     {@link ClimbOut})
     */
    static void leaveToNavigation(
            VillagerEntity villager, UUID workerId, BlockPos feet, String shortId,
            Consumer<UUID> forgetClimb) {

        VillageColonyMod.LOGGER.info(
                "Stranded worker {} found no natural step at {} — leaves it to the navigation",
                shortId, feet.toShortString());

        forgetClimb.accept(workerId);
        MineReturn.forget(workerId);
        villager.getBrain().forget(MemoryModuleType.WALK_TARGET);
    }
}
