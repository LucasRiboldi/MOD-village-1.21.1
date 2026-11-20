package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ServerMemory;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A separação dos currais mistos — Regra 72, com a emenda da Regra 329: um animal de um cercado com mais de uma
 * espécie vai para o cercado só da espécie dele (ou um vazio) com lugar, para que cada tipo acabe no seu quando a vila
 * tem vários currais. Um por vez, só quando não há animal solto a recolher, e no máximo uma separação por dia em cada
 * vila.
 */
final class PenRegroup {

    static {
        ServerMemory.register(PenRegroup.class, PenRegroup::clearAll);
    }

    /** Colônia → o dia da última separação. */
    private static final Map<UUID, Long> DAY = new HashMap<>();

    private PenRegroup() {
    }

    /** Se a vila ainda pode separar um curral misto hoje (Regra 329). */
    static boolean mayRegroup(ServerWorld world, UUID colonyId) {
        return DAY.getOrDefault(colonyId, Long.MIN_VALUE) != ShepherdRound.dayOf(world);
    }

    /** A separação de hoje começou. */
    static void regrouped(ServerWorld world, UUID colonyId) {
        DAY.put(colonyId, ShepherdRound.dayOf(world));
    }

    static Optional<ShepherdHerding.Herd> separate(ServerWorld world, Colony colony, VillagerEntity shepherd) {
        if (!mayRegroup(world, colony.id())) {
            return Optional.empty();
        }

        for (ShepherdPens.Pen group : ShepherdPens.pensOf(world, colony)) {
            var source = ShepherdPens.reachableFrom(world, group, shepherd);
            if (source.isEmpty()) continue;
            var mixed = source.get();
            Map<EntityType<?>, Integer> inside = ShepherdPens.speciesIn(world, mixed);

            if (inside.size() < 2) {
                continue;
            }

            for (AnimalEntity animal : world.getEntitiesByClass(AnimalEntity.class,
                    ShepherdPens.areaOf(mixed),
                    candidate -> candidate.isAlive() && candidate.canBeLeashed() && !candidate.isLeashed()
                            && mixed.reach().contains(candidate.getBlockPos()))) {
                Optional<ShepherdPens.Pen> own =
                        ShepherdPens.forSpecies(world, colony, animal.getType(), shepherd, mixed, true);

                if (own.isPresent()) {
                    ShepherdHerding.Herd herd = new ShepherdHerding.Herd(animal.getUuid(), own.get().gate(), world.getTime());
                    herd.pen = own.get();
                    herd.from = mixed;
                    regrouped(world, colony.id());

                    VillageColonyMod.LOGGER.info("Colony {} separates the pens — a {} goes from the pen at {} to the one at {}",
                            colony.id().toString().substring(0, 8), animal.getType().getUntranslatedName(),
                            mixed.gate().toShortString(), own.get().gate().toShortString());

                    return Optional.of(herd);
                }
            }
        }

        return Optional.empty();
    }

    static void clearAll() {
        DAY.clear();
    }
}
