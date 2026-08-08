package com.villagecolony.fabric.brain;

import com.google.common.collect.ImmutableList;
import com.villagecolony.VillageColonyMod;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.passive.VillagerEntity;

/**
 * Põe a task da colônia no Brain do aldeão.
 *
 * <p>Chamado pelo mixin em {@code VillagerEntity.initBrain}, que só
 * delega — ADR-004 §4, regra 3.
 *
 * <p><b>Onde a task entra, e por que não numa Activity própria.</b> A
 * ADR-004 §5 previa registrar {@code villagecolony:colony_work} como
 * Activity ao nível de {@code WORK}. Quem escolhe a Activity ativa em
 * 1.21.1 é a {@code Schedule} do aldeão, através de uma task Vanilla de
 * CORE; uma Activity que a Schedule não conhece nunca seria escolhida, e
 * fazê-la ser escolhida exigiria justamente uma task de CORE chamando
 * {@code doExclusively} a cada tick — mais peça para o mesmo efeito.
 *
 * <p>Então a task vive em CORE e carrega ela mesma as duas condições que
 * a Activity daria: só age com destino posto em {@link WorkTargets}, e
 * só durante {@code WORK}. O resultado em jogo é o que a ADR descreve; o
 * que muda é o lugar do registro, e ele muda para o lado mais barato e
 * de menos conflito.
 */
public final class ColonyBrainInitializer {

    /**
     * Prioridade da task dentro de CORE.
     *
     * <p>Depois de tudo que Vanilla põe em CORE hoje — água, portas,
     * pânico, acordar, sino, incursão, e a task que anda até o
     * {@code WALK_TARGET}. Ficar depois é de propósito: pânico e
     * incursão decidem primeiro, e o destino escrito aqui só é lido no
     * tick seguinte.
     */
    private static final int PRIORITY = 5;

    private ColonyBrainInitializer() {
    }

    /**
     * Registra a task, sem tocar em nada que já esteja lá.
     *
     * <p>{@code setTaskList} acrescenta à lista da Activity em vez de
     * substituí-la, e o conjunto de memórias exigidas de CORE é vazio
     * tanto em Vanilla quanto aqui — nenhuma task Vanilla é removida e
     * nenhum índice de lista é assumido, como pede a ADR-004 §7.
     */
    public static void install(Brain<VillagerEntity> brain) {
        try {
            brain.setTaskList(
                    Activity.CORE, PRIORITY, ImmutableList.of(new GoToWorkTargetTask()));
        } catch (RuntimeException failure) {
            // ADR-004 §4, regra 4: nunca lançar de dentro de um método
            // Vanilla. Um aldeão sem a task é um aldeão Vanilla, que é
            // exatamente o estado de antes desta mudança.
            VillageColonyMod.LOGGER.warn(
                    "Could not install the colony brain task — this villager stays vanilla", failure);
        }
    }
}
