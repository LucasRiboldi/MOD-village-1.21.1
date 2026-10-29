package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.integration.ColonyChests;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;

import java.util.List;

/**
 * O pastor sem tarefa cuida do rebanho — O3 (autor, 2026-10-28): com o rebanho abaixo da meta
 * ({@link ShepherdFlock#FLOCK_TARGET}), ele dá {@link #WHEAT} trigos dos baús públicos a duas ovelhas adultas, e elas
 * entram no cio; o cruzamento é o do Minecraft. Acima da meta quem cuida é o abate do excedente ({@code FlockHarvest}).
 */
final class FlockCare {

    /** Até onde, em volta do pastor, ele procura as ovelhas. */
    static final int REACH = 16;

    /** O trigo de um cruzamento: um por ovelha. */
    static final int WHEAT = 2;

    private FlockCare() {
    }

    /** Alimenta duas ovelhas adultas para cruzar e devolve quantas alimentou. */
    static int feed(ServerWorld world, Colony colony, VillagerEntity shepherd) {
        Box around = shepherd.getBoundingBox().expand(REACH);
        List<SheepEntity> flock = world.getEntitiesByClass(SheepEntity.class, around, SheepEntity::isAlive);

        if (flock.size() >= ShepherdFlock.FLOCK_TARGET) {
            return 0;
        }

        List<SheepEntity> ready = flock.stream()
                .filter(sheep -> !sheep.isBaby() && sheep.getBreedingAge() == 0 && !sheep.isInLove())
                .limit(2)
                .toList();

        if (ready.size() < 2) {
            return 0;
        }

        List<ColonyPos> chests = PublicChests.of(world, colony.id(), shepherd.getBlockPos());

        if (ColonyChests.countIn(world, chests, Items.WHEAT) < WHEAT
                || ColonyChests.withdraw(world, chests, Items.WHEAT, WHEAT) < WHEAT) {
            return 0;
        }

        ready.forEach(sheep -> sheep.lovePlayer(null));
        ActionJournal.action(shepherd.getUuid(), "FED", "minecraft:wheat", WHEAT, ready.getFirst().getBlockPos(),
                "the flock breeds");

        return ready.size();
    }
}
