package com.villagecolony.fabric.brain;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.coordination.WorkClock;
import com.villagecolony.core.worker.model.Worker;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.EntityLookTarget;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
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

    /** Profissionais da colônia ficam fora das interações sociais durante o expediente (ADR-004). */
    public static boolean canSocialize(LivingEntity entity) {
        return !(entity instanceof VillagerEntity villager
                && villager.getWorld() instanceof ServerWorld world
                && isWorkTime(world, villager)
                && VillageColonyMod.WORKERS.find(villager.getUuid()).flatMap(Worker::profession).isPresent());
    }

    /** Reavalia também conversas que já estavam em curso quando o expediente começou. */
    public static boolean canTalkToCurrentTarget(VillagerEntity villager) {
        var target = villager.getBrain().getOptionalRegisteredMemory(MemoryModuleType.INTERACTION_TARGET);
        if (target.orElse(null) instanceof VillagerEntity partner
                && (!canSocialize(villager) || !canSocialize(partner))) {
            dropSocialWalk(villager, partner);
            dropSocialWalk(partner, villager);
            return false;
        }
        return true;
    }

    private static void dropSocialWalk(VillagerEntity villager, VillagerEntity partner) {
        var brain = villager.getBrain();
        var target = brain.getOptionalRegisteredMemory(MemoryModuleType.WALK_TARGET).map(WalkTarget::getLookTarget);
        if (target.orElse(null) instanceof EntityLookTarget look && look.getEntity() == partner
                && brain.getOptionalRegisteredMemory(MemoryModuleType.BREED_TARGET).orElse(null) != partner) {
            brain.forget(MemoryModuleType.WALK_TARGET);
            villager.getNavigation().stop();
        }
    }
}
