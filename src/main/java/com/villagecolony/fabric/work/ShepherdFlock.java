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
 * O pastor cuida do rebanho — ADR-038 P2c e ADR-039 E2: alimenta para procriar,
 * um par por espécie a cada {@link #BETWEEN_PAIRS}, com a comida que ele aceita,
 * tirada de qualquer baú da vila. O animal solto também come; a coleta
 * ({@code ShepherdHerding}) o traz para a cerca depois. Até {@link #FLOCK_TARGET}
 * adultos de cada espécie: o rebanho tem teto.
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

        Box area = areaOf(colony);
        List<ColonyPos> chests = ColonyChests.nearestFirst(world, colony.id(), colony.center());
        boolean paired = false;

        for (EntityType<? extends AnimalEntity> species : SPECIES) {
            paired |= pairOf(world, colony, species, area, chests);
        }

        return paired;
    }

    /**
     * A lã faltou e nenhuma ovelha tem lã — E7, decisão do autor de 2026-10-08: só
     * procria sob <b>déficit real</b> do rebanho. Abaixo de {@link #FLOCK_TARGET}
     * ovelhas adultas, um par sai agora, fora do relógio de {@link #BETWEEN_PAIRS};
     * com o rebanho cheio, a lã volta pelo pasto e não se cria nada.
     *
     * @return se um par de ovelhas foi posto para procriar
     */
    static boolean breedForWool(ServerWorld world, UUID colonyId) {
        return VillageColonyMod.COLONIES.find(colonyId)
                .map(colony -> breedForWool(world, colony, areaOf(colony),
                        ColonyChests.nearestFirst(world, colony.id(), colony.center())))
                .orElse(false);
    }

    static boolean breedForWool(ServerWorld world, Colony colony, Box area, List<ColonyPos> chests) {
        int sheep = world.getEntitiesByType(EntityType.SHEEP, area,
                animal -> animal.isAlive() && !animal.isBaby()).size();

        if (sheep >= FLOCK_TARGET) {
            VillageColonyMod.LOGGER.info(
                    "Colony {} — no wool and the flock is full ({} sheep): the wool grows back on its own",
                    colony.id().toString().substring(0, 8), sheep);
            return false;
        }

        return pairOf(world, colony, EntityType.SHEEP, area, chests);
    }

    /**
     * Onde o pastor procura ovelha com lã: o raio dele e, com a vila medida,
     * a mesma área em que o rebanho é contado.
     *
     * <p>Só o raio em volta do centro deixava o curral de fora: na colônia
     * 33a6b9c4 a cerca fica 43 blocos ao norte do centro, o rebanho contava
     * 16 ovelhas "cheio" e o pastor dizia "nenhuma com lã em 32 blocos" com
     * 10 delas lanudas no curral.
     */
    static Box shearingArea(Colony colony, BlockPos center, int radius) {
        Box around = new Box(center).expand(radius);

        return colony.bounds().isPresent() ? around.union(areaOf(colony)) : around;
    }

    private static Box areaOf(Colony colony) {
        BlockPos center = MinecraftTypeAdapter.toBlockPos(colony.center());

        return colony.bounds()
                .map(box -> new Box(box.minX() - 10, box.minY() - 8, box.minZ() - 10,
                        box.maxX() + 11, box.maxY() + 8, box.maxZ() + 11))
                .orElseGet(() -> new Box(center).expand(RADIUS));
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
            if (animal.getBreedingAge() == 0 && !animal.isInLove()) {
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
