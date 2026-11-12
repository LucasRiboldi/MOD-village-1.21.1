package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;

import java.util.List;

/**
 * Por que o curral da vila não recebeu o animal agora — Regra 255. O pastor não amarra mais com curral na vila; a
 * linha diz se faltou lugar, portão com chão livre dos dois lados ou caminho até ele, para o jogo seguinte provar a
 * causa. Uma linha por tentativa, que tem o intervalo da coleta ({@code ShepherdHerding.COOLDOWN}).
 */
final class PenRefusal {

    private PenRefusal() {
    }

    static void log(ServerWorld world, Colony colony, VillagerEntity shepherd, AnimalEntity animal) {
        VillageColonyMod.LOGGER.info("Shepherd {} found no pen for the {} at {} — {}",
                shepherd.getUuid().toString().substring(0, 8), Registries.ENTITY_TYPE.getId(animal.getType()).getPath(),
                animal.getBlockPos().toShortString(), reason(world, colony, shepherd));
    }

    /** A primeira falta, na ordem em que o curral é conferido. */
    static String reason(ServerWorld world, Colony colony, VillagerEntity shepherd) {
        List<ShepherdPens.Pen> pens = ShepherdPens.pensOf(world, colony);
        List<ShepherdPens.Pen> withRoom = pens.stream().filter(pen -> ShepherdPens.hasRoom(world, pen)).toList();

        if (withRoom.isEmpty()) {
            return "all " + pens.size() + " pen(s) are full";
        }

        List<ShepherdPens.Pen> standing = withRoom.stream()
                .filter(pen -> ShepherdPens.nearestAccess(world, pen, shepherd.getBlockPos(), access -> true).isPresent())
                .toList();

        if (standing.isEmpty()) {
            return withRoom.size() + " pen(s) with room, but no gate is loaded, open above and free to stand on both sides";
        }

        return standing.size() + " pen(s) with room and a gate, but no path from the shepherd to any gate";
    }
}
