package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.fabric.brain.WorkHours;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Quem está livre para uma ajuda — uma lista só: toda ajuda nova entra aqui, e o aldeão nunca fica com dois alvos ao mesmo tempo.
 * Quem já faz uma ajuda continua livre para ela mesma.
 */
final class IdleWorkers {

    /** As ajudas de quem está sem tarefa do ofício. */
    enum Help { GROVE, SNOW, PATH, ROAD_CLEARING, GROUND_PICKUP }

    private IdleWorkers() {
    }

    /** No expediente, acordado, sem tarefa do ofício, solto, e sem outra ajuda em curso além de {@code own}. */
    static boolean isFree(ServerWorld world, VillagerEntity villager, @Nullable Help own) {
        UUID id = villager.getUuid();

        return villager.isAlive() && !villager.isSleeping()
                && VillageColonyMod.TASKS.assignedTo(id).isEmpty()
                && WorkHours.isWorkTime(world, villager)
                && !StrandedWorkers.isStranded(id) && !PenEscape.isEscaping(id) && !ShepherdHerding.isHerding(id)
                && (own == Help.GROVE || !IdleHelps.isHelping(id))
                && (own == Help.SNOW || !SnowShovel.isShoveling(id))
                && (own == Help.PATH || !PathPaving.isPaving(id))
                && (own == Help.ROAD_CLEARING || !BuilderPaving.isPaving(id))
                && (own == Help.GROUND_PICKUP || !GroundPickup.isHelping(id));
    }
}
