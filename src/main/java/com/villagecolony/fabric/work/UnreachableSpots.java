package com.villagecolony.fabric.work;

import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Os lugares de pé que a navegação já disse não alcançar — playtest de
 * 2026-10-03.
 *
 * <p><b>O defeito, medido.</b> A casa do pastor de {@code -292, 65, 386}
 * levou 26 minutos para 172 blocos. O construtor escolhia o lugar de pé
 * mais perto dele dentro do alcance do bloco — e, com as paredes já de pé,
 * esse lugar ficava <b>dentro</b> da casa: ele parava do lado de fora, a
 * cinco ou oito blocos, até o guarda de imobilidade devolver a tarefa em
 * trezentos tiques (seis vezes na sessão) ou a peça ir para o fim da fila
 * em duzentos (dezenas de {@code sets … aside}).
 *
 * <p><b>O Vanilla já sabia na hora.</b> A {@code MoveToTargetTask} grava
 * {@link MemoryModuleType#CANT_REACH_WALK_TARGET_SINCE} assim que o caminho
 * calculado não chega ao destino. É a mesma lição do {@code isDry}: o dado
 * existia e era jogado fora. Com ele, o lugar é riscado em um segundo e o
 * construtor vai ao lado de fora, que alcança.
 */
final class UnreachableSpots {

    /** Tiques com o caminho sem chegar antes de riscar o lugar: um segundo. */
    static final int GIVE_UP_AFTER = 20;

    /**
     * Só perto do lugar o "não chega" é da geometria.
     *
     * <p>A busca de caminho do aldeão vai até o alcance de seguir dele —
     * 48 blocos. Vindo do baú do outro lado da vila, o caminho sai parcial
     * só pela distância, e riscar o lugar por isso jogaria fora o lado bom
     * da obra.
     */
    static final int NEAR = 24;

    /** Teto do conjunto: a obra muda com cada bloco, e o lugar riscado pode voltar a servir. */
    private static final int CAP = 64;

    private final Set<BlockPos> spots = new HashSet<>();

    private BlockPos current;

    Set<BlockPos> spots() {
        return Collections.unmodifiableSet(spots);
    }

    /**
     * Se a navegação desistiu deste destino; quando desistiu, ele fica riscado.
     *
     * <p>A marca do Vanilla não diz a que destino se refere, e sobrevive à
     * troca de destino. Por isso ela é apagada sempre que o destino muda: a
     * que aparecer depois é deste.
     */
    boolean gaveUp(ServerWorld world, VillagerEntity villager, BlockPos spot) {
        Brain<?> brain = villager.getBrain();

        if (!spot.equals(current)) {
            current = spot;
            forget(brain);

            return false;
        }

        Optional<Long> since = brain.hasMemoryModule(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE)
                ? brain.getOptionalRegisteredMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE)
                : Optional.empty();

        if (since.isEmpty() || world.getTime() - since.get() < GIVE_UP_AFTER
                || !villager.getBlockPos().isWithinDistance(spot, NEAR)) {
            return false;
        }

        if (spots.size() >= CAP) {
            spots.clear();
        }

        spots.add(spot.toImmutable());
        current = null;
        forget(brain);

        return true;
    }

    private static void forget(Brain<?> brain) {
        if (brain.hasMemoryModule(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE)) {
            brain.forget(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        }
    }
}
