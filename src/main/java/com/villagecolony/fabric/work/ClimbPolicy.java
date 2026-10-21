package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * Quem pode remodelar o terreno para escalar — regra do autor, 2026-10-15.
 *
 * <p><b>Só o mineiro</b> põe bloco (pilar) ou cava (túnel) para subir — e, desde 2026-10-21 (autor),
 * também qualquer profissão encalhada abaixo do caminho ({@link StrandedWorkers#wayOf}), que sobe até a
 * altura do destino: na vila do deserto a cama do lenhador fica na parte baixa, e o teleporte o
 * devolvia ao mesmo buraco. Fora desse caso, as outras profissões sobem
 * apenas por degrau natural; sem um ao alcance, procuram num raio maior e, se nada, voltam à vila pelo
 * teleporte da cama ({@link #giveUp}), sem tocar o mundo. Separado do {@link ClimbOut} para ele caber
 * no teto de 500 linhas (F2 do plano).
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

    /** Se este trabalhador pode remodelar o terreno para escalar: o mineiro, ou quem está abaixo do caminho. */
    static boolean mayReshape(UUID workerId) {
        return mayReshapeTerrain(professionOf(workerId)) || StrandedWorkers.wayOf(workerId).isPresent();
    }

    /**
     * Um não-mineiro encalhado sem degrau natural ao alcance volta para a vila pelo teleporte da cama
     * ({@link BedTeleport#toBed}) — decisão do autor, 2026-10-20 (G3 da varredura). Ele não remodela o
     * terreno, então não há como sair sozinho do bolsão.
     *
     * <p>O defeito G3 era largar só o estado de escalada: o aldeão seguia marcado em
     * {@link StrandedWorkers}, fora da fila de trabalho, e a passagem seguinte recomeçava a busca e o
     * log, para sempre. O {@code toBed} já tira dos encalhados e encerra a escalada. Sem cama, faz o
     * mesmo sem o teleporte: libera e devolve à fila, e a navegação normal tenta tirá-lo dali.
     */
    static void giveUp(ServerWorld world, VillagerEntity villager, UUID workerId, BlockPos feet) {
        String why = "no natural step to climb out of " + feet.toShortString()
                + " and it does not reshape the terrain";

        if (BedTeleport.toBed(world, villager, workerId, why)) {
            return;
        }

        VillageColonyMod.LOGGER.info(
                "Stranded worker {} has no bed to go back to — {}; back in the work queue",
                workerId.toString().substring(0, 8), why);

        StrandedWorkers.release(workerId);
        ClimbOut.finish(world, villager, workerId);
        MineReturn.finish(workerId);
        StrandedEscape.forget(workerId);
        villager.getBrain().forget(MemoryModuleType.WALK_TARGET);
    }
}
