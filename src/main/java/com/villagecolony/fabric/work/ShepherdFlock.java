package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ColonyChests;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * O pastor cuida do rebanho — ADR-038 P2c: alimenta para procriar, um par por
 * espécie a cada {@link #BETWEEN_PAIRS}, todo animal criável amarrado em cerca
 * ou dentro de curral, com a comida que ele aceita, tirada de qualquer baú da
 * vila. Até {@link #FLOCK_TARGET} adultos de cada espécie: o rebanho tem teto.
 */
public final class ShepherdFlock {

    static {
        ServerMemory.register(ShepherdFlock.class, ShepherdFlock::clearAll);
    }

    /** Adultos de cada espécie a partir dos quais o pastor para de criar. */
    public static final int FLOCK_TARGET = 12;

    /** Entre um par e o seguinte da mesma colônia. */
    static final int BETWEEN_PAIRS = 6_000;

    private static final int RADIUS = 32;

    /** As espécies que o pastor cria. */
    static final List<EntityType<? extends AnimalEntity>> SPECIES =
            List.of(EntityType.SHEEP, EntityType.COW, EntityType.PIG, EntityType.CHICKEN);

    /** A comida dos baús que algum deles aceita. */
    private static final List<Item> FOOD = List.of(
            Items.WHEAT, Items.CARROT, Items.POTATO, Items.BEETROOT,
            Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS);

    private static final Map<UUID, Long> LAST = new HashMap<>();

    private ShepherdFlock() {
    }

    /**
     * Um par por espécie, se a vila tem pastor, comida e animais guardados.
     *
     * @return se algum par foi posto para procriar
     */
    public static boolean tend(ServerWorld world, Colony colony) {
        long now = world.getTime();
        Long last = LAST.get(colony.id());
        if (last != null && now - last < BETWEEN_PAIRS) {
            return false;
        }

        boolean hasShepherd = VillageColonyMod.WORKERS.ofColony(colony.id()).stream()
                .anyMatch(worker -> worker.profession().filter(ProfessionType.SHEPHERD::equals).isPresent());
        if (!hasShepherd) {
            return false;
        }

        LAST.put(colony.id(), now);

        BlockPos center = MinecraftTypeAdapter.toBlockPos(colony.center());
        Box area = colony.bounds()
                .map(box -> new Box(box.minX() - 10, box.minY() - 8, box.minZ() - 10,
                        box.maxX() + 11, box.maxY() + 8, box.maxZ() + 11))
                .orElseGet(() -> new Box(center).expand(RADIUS));
        List<ColonyPos> chests = ColonyChests.nearestFirst(world, colony.id(), colony.center());
        boolean paired = false;

        for (EntityType<? extends AnimalEntity> species : SPECIES) {
            paired |= pairOf(world, colony, species, area, chests);
        }

        return paired;
    }

    private static boolean pairOf(ServerWorld world, Colony colony, EntityType<? extends AnimalEntity> species,
            Box area, List<ColonyPos> chests) {
        List<? extends AnimalEntity> adults = world.getEntitiesByType(
                species, area, animal -> animal.isAlive() && !animal.isBaby());

        if (adults.size() >= FLOCK_TARGET) {
            return false;
        }

        List<AnimalEntity> ready = new ArrayList<>();
        for (AnimalEntity animal : adults) {
            if (animal.getBreedingAge() == 0 && !animal.isInLove() && isKept(world, animal)) {
                ready.add(animal);
            }
            if (ready.size() == 2) {
                break;
            }
        }

        if (ready.size() < 2) {
            return false;
        }

        for (Item food : FOOD) {
            if (!ready.get(0).isBreedingItem(new ItemStack(food))
                    || ColonyChests.countIn(world, chests, food) < 2) {
                continue;
            }

            ColonyChests.withdraw(world, chests, food, 2);

            for (AnimalEntity animal : ready) {
                animal.lovePlayer(null);
            }

            VillageColonyMod.LOGGER.info(
                    "Colony {} — the shepherd fed two {} with {} to breed ({} adults, target {})",
                    colony.id().toString().substring(0, 8), species.getUntranslatedName(),
                    food, adults.size(), FLOCK_TARGET);
            return true;
        }

        VillageColonyMod.LOGGER.info("Colony {} — the shepherd has no food for the {} ({} adults)",
                colony.id().toString().substring(0, 8), species.getUntranslatedName(), adults.size());
        return false;
    }

    /** Guardado: amarrado numa cerca, ou dentro de curral (cercado e com cerca perto). */
    static boolean isKept(ServerWorld world, AnimalEntity animal) {
        return animal.getLeashHolder() instanceof LeashKnotEntity
                || (FencedIn.isNearAFence(world, animal.getBlockPos())
                        && FencedIn.check(world, animal.getBlockPos()).enclosed());
    }

    public static void clearAll() {
        LAST.clear();
    }
}
