package com.villagecolony.fabric.brain;

import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Quando a colônia pode mandar no aldeão.
 *
 * <p>Uma regra só, em um lugar só, porque duas pessoas perguntam: a
 * {@link GoToWorkTargetTask}, para decidir se age, e o log do lenhador,
 * para dizer por que nada aconteceu. Se as duas respostas pudessem
 * divergir, o log passaria a mentir justamente no caso que ele existe
 * para explicar.
 */
public final class WorkHours {

    private WorkHours() {
    }

    /**
     * É hora de trabalhar, e nada mais urgente está acontecendo?
     *
     * <p>A pergunta é feita à {@code Schedule} do próprio aldeão, e não
     * a {@code hasActivity(WORK)}. Motivo concreto: a Activity WORK de
     * Vanilla exige memória de {@code JOB_SITE}, e o candidato
     * preferencial a trabalhador da colônia é o aldeão <b>sem</b>
     * workstation — ADR-004 §6. Ele nunca teria WORK ativa, e a colônia
     * inteira ficaria parada.
     *
     * <p>Pânico e esconderijo continuam vindo antes: quando o sino toca
     * ou a incursão começa, o Brain torna essas Activities possíveis e o
     * trabalho espera.
     */
    public static boolean isWorkTime(ServerWorld world, VillagerEntity villager) {
        if (villager.getBrain().hasActivity(Activity.PANIC)
                || villager.getBrain().hasActivity(Activity.HIDE)) {
            return false;
        }

        int timeOfDay = (int) (world.getTimeOfDay() % 24000L);

        return villager.getBrain().getSchedule().getActivityForTime(timeOfDay) == Activity.WORK;
    }
}
