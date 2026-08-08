package com.villagecolony.fabric.brain;

import net.minecraft.entity.ai.brain.BlockPosLookTarget;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.ai.brain.task.MultiTickTask;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.Optional;

/**
 * A task que faz o aldeão andar até o trabalho da colônia.
 *
 * <p>Existe porque {@code getNavigation().startMovingTo} não bastava: o
 * cérebro Vanilla reescreve o destino no mesmo tick, seguindo a agenda
 * dele. Quem manda no caminho do aldeão em 1.21.1 é a memória
 * {@code WALK_TARGET}, e as tasks Vanilla de movimento só começam quando
 * ela está vazia. Manter a memória posta enquanto houver destino é o que
 * segura o aldeão no caminho — sem cancelar nada e sem remover task
 * alguma, como manda a ADR-004 §4.
 *
 * <p>Sem destino em {@link WorkTargets} a task não começa, e o aldeão
 * segue a rotina Vanilla inteira. Com destino, ela só age no horário de
 * trabalho da agenda Vanilla: fora dele o aldeão dorme, come e
 * socializa como sempre. É a ADR-004 §6, e por trás dela o
 * PROJECT_CONSTITUTION §4.
 */
public final class GoToWorkTargetTask extends MultiTickTask<VillagerEntity> {

    /**
     * Passo de trabalho, não de fuga.
     *
     * <p>Mesma velocidade que o pedido anterior usava, para que a
     * mudança seja de quem manda no caminho e não do ritmo.
     */
    private static final float SPEED = 0.5f;

    /**
     * A que distância a navegação considera chegado.
     *
     * <p>Menor que o alcance de braço do lenhador, para ele parar dentro
     * do alcance em vez de na borda dele.
     */
    private static final int COMPLETION_RANGE = 2;

    /**
     * Quanto tempo a task pode correr sem ser reavaliada.
     *
     * <p>Um dia inteiro de caminhada não é excessivo: ela para sozinha
     * assim que o destino some, e o destino some quando a tarefa termina
     * ou é solta.
     */
    private static final int MAX_RUN_TIME = 24_000;

    public GoToWorkTargetTask() {
        super(Map.of(), MAX_RUN_TIME, MAX_RUN_TIME);
    }

    @Override
    protected boolean shouldRun(ServerWorld world, VillagerEntity villager) {
        return WorkHours.isWorkTime(world, villager)
                && WorkTargets.of(villager.getUuid()).isPresent();
    }

    @Override
    protected void run(ServerWorld world, VillagerEntity villager, long time) {
        aim(villager);
    }

    @Override
    protected boolean shouldKeepRunning(ServerWorld world, VillagerEntity villager, long time) {
        return WorkHours.isWorkTime(world, villager)
                && WorkTargets.of(villager.getUuid()).isPresent();
    }

    /**
     * Repõe o destino a cada tick.
     *
     * <p>Repor é de propósito. Uma task Vanilla de prioridade maior
     * ainda pode escrever {@code WALK_TARGET} num tick; no seguinte o
     * destino da colônia volta, e o aldeão retoma o caminho em vez de
     * ficar parado até o próximo ciclo.
     */
    @Override
    protected void keepRunning(ServerWorld world, VillagerEntity villager, long time) {
        aim(villager);
    }

    /**
     * Devolve o aldeão à agenda Vanilla.
     *
     * <p>Esquecer o {@code WALK_TARGET} é o que libera as tasks Vanilla
     * de movimento, que só começam com a memória vazia. Sem isto o
     * aldeão ficaria preso na última árvore para sempre.
     */
    @Override
    protected void finishRunning(ServerWorld world, VillagerEntity villager, long time) {
        villager.getBrain().forget(MemoryModuleType.WALK_TARGET);
    }

    /**
     * Põe o destino, se ele já não estiver posto.
     *
     * <p>A comparação não é economia de linha: {@code WalkTarget} novo é
     * destino novo para a task Vanilla que anda, e ela recalcula o
     * caminho inteiro quando o destino muda. Reescrever a memória a cada
     * tick faria um A* por aldeão por tick — o aldeão recomeçaria o
     * caminho antes de dar o passo, e ficaria parado gastando servidor.
     */
    private void aim(VillagerEntity villager) {
        Optional<BlockPos> target = WorkTargets.of(villager.getUuid());

        if (target.isEmpty() || isAlreadyAimedAt(villager, target.get())) {
            return;
        }

        BlockPosLookTarget look = new BlockPosLookTarget(target.get());

        villager.getBrain().remember(MemoryModuleType.LOOK_TARGET, look);
        villager.getBrain().remember(
                MemoryModuleType.WALK_TARGET, new WalkTarget(look, SPEED, COMPLETION_RANGE));
    }

    private boolean isAlreadyAimedAt(VillagerEntity villager, BlockPos target) {
        return villager.getBrain()
                .getOptionalRegisteredMemory(MemoryModuleType.WALK_TARGET)
                .map(walk -> walk.getLookTarget().getBlockPos().equals(target))
                .orElse(false);
    }
}
