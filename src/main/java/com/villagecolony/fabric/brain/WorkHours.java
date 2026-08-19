package com.villagecolony.fabric.brain;

import com.villagecolony.core.coordination.WorkClock;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Quando a colônia pode mandar no aldeão.
 *
 * <p>Uma regra só, em um lugar só, porque três pessoas perguntam: a
 * {@link GoToWorkTargetTask}, para decidir se age; o guarda de
 * travamento do lenhador, para não acusar de preso quem apenas não
 * pode andar; e o log, para dizer por que nada aconteceu. Se as
 * respostas pudessem divergir, o log passaria a mentir justamente no
 * caso que ele existe para explicar.
 */
public final class WorkHours {

    private WorkHours() {
    }

    /**
     * É hora de trabalhar, e nada mais urgente está acontecendo?
     *
     * <p><b>O dia inteiro é expediente</b>, e não a fatia que o Vanilla
     * chama de WORK — a janela é do {@link WorkClock}, e o porquê dela
     * está lá. Esta camada acrescenta só o que depende do aldeão.
     *
     * <p>Foi o que a sessão de 2026-08-18 mostrou: as linhas
     * {@code off hours} e {@code work time} alternando no relatório do
     * lenhador com o sol alto, e o relógio de travamento congelando em
     * 2.282 de 2.400 porque o expediente acabava antes de o guarda
     * fechar. A árvore inalcançável nunca era marcada, e o lenhador
     * voltava a ela no dia seguinte.
     *
     * <p>O que a Schedule dava de graça e agora é dito aqui: <b>criança
     * não trabalha</b>. A Schedule do bebê não tem WORK em hora alguma,
     * e ao deixar de perguntar a ela a colônia precisa perguntar isso
     * por conta própria.
     *
     * <p>Pânico e esconderijo continuam vindo antes: quando o sino toca
     * ou a incursão começa, o Brain torna essas Activities possíveis e o
     * trabalho espera.
     */
    public static boolean isWorkTime(ServerWorld world, VillagerEntity villager) {
        if (villager.isBaby()) {
            return false;
        }

        if (villager.getBrain().hasActivity(Activity.PANIC)
                || villager.getBrain().hasActivity(Activity.HIDE)) {
            return false;
        }

        return WorkClock.isWorkTime(world.getTimeOfDay());
    }
}
