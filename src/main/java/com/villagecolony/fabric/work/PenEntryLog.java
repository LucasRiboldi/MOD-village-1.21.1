package com.villagecolony.fabric.work;

import org.jspecify.annotations.Nullable;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.fabric.brain.WorkTargets;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * Para onde o aldeão ia quando entrou no curral — estudo de 01-10, §8-A.
 *
 * <p>O E52 mostrou aldeões entrando "de cima do feno encostado na cerca", e
 * eles já saem em segundos ({@link PenEscape}). Impedir a entrada tem três
 * caminhos (memória de curral, penalidade no pathfinding, aceitar), e qual
 * deles depende de uma pergunta que o log não respondia: <b>ele ia para um
 * alvo de ofício ou passeava como Vanilla?</b> Esta linha responde, no
 * instante em que o curral é visto — antes de a fuga trocar o destino.
 *
 * <p>Sinal no log: {@code entered a pen}. Alvo de ofício ({@code work}) ou
 * tarefa dentro do curral pede a memória de curral (opção B); só destino
 * Vanilla ({@code walk}) pede a opção C ou aceitar.
 */
final class PenEntryLog {

    private PenEntryLog() {
    }

    /**
     * Registra a entrada, com o que o aldeão tinha na cabeça.
     *
     * @param lastFree onde ele foi visto solto perto de uma cerca pela última
     *     vez, ou {@code null} se não foi
     */
    static void record(ServerWorld world, MobEntity mob, String who, BlockPos feet, @Nullable BlockPos lastFree) {
        UUID id = mob.getUuid();

        BlockPos navigation = mob.getNavigation().getTargetPos();
        String work = WorkTargets.of(id).map(BlockPos::toShortString).orElse("none");
        // O golem não registra WALK_TARGET, e pedir memória não registrada
        // derruba o servidor — o GameTest do curral do golem pegou isso.
        String walk = !mob.getBrain().hasMemoryModule(MemoryModuleType.WALK_TARGET) ? "none"
                : mob.getBrain().getOptionalRegisteredMemory(MemoryModuleType.WALK_TARGET)
                        .map(target -> target.getLookTarget().getBlockPos().toShortString())
                        .orElse("none");
        String task = VillageColonyMod.TASKS.assignedTo(id).stream()
                .map(Task::type).map(Enum::name).findFirst().orElse("none");
        String under = Registries.BLOCK.getId(world.getBlockState(feet.down()).getBlock()).getPath();

        VillageColonyMod.LOGGER.info(line(who, id.toString().substring(0, 8), feet.toShortString(),
                navigation == null ? "none" : navigation.toShortString(), work, walk, task,
                lastFree == null ? "unknown" : lastFree.toShortString(), under));
    }

    /** A frase, separada para o teste: a pergunta do §8-A cabe nela inteira. */
    static String line(String who, String id, String feet, String navigation, String work, String walk,
            String task, String lastFree, String under) {

        return who + " " + id + " entered a pen at " + feet + " standing on " + under
                + " — path to " + navigation + ", work target " + work + ", walk target " + walk
                + ", task " + task + "; last seen free at " + lastFree;
    }
}
