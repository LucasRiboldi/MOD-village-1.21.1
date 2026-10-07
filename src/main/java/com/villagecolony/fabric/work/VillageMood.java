package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageHappiness;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.VillagerScanner;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A reunião da vila mede a felicidade e decide os filhos — ADR-036 item 20.
 * A medida está em {@link VillageHappiness}; aqui, o efeito:
 * <ul>
 *   <li><b>infeliz</b>: nenhum filho novo — o tempo de espera de reprodução de
 *       cada adulto fica em pelo menos {@link #HOLD_TICKS} até a próxima reunião;</li>
 *   <li><b>feliz</b>: mais filhos — a espera cai pela metade, e quem tem menos de
 *       {@link VillageHappiness#FED} pontos de comida no bolso recebe um pão dos
 *       baús da colônia por reunião.</li>
 * </ul>
 */
public final class VillageMood {

    static {
        ServerMemory.register(VillageMood.class, VillageMood::clearAll);
    }

    /** Mais que o intervalo entre reuniões, para a espera não vencer no meio. */
    static final int HOLD_TICKS = 1_200;

    private static final Map<UUID, VillageHappiness> LAST = new HashMap<>();

    private VillageMood() {
    }

    public static void clearAll() {
        LAST.clear();
    }

    /** A última medida desta colônia, para o {@code /vc log}. */
    public static Optional<VillageHappiness> of(UUID colonyId) {
        return Optional.ofNullable(LAST.get(colonyId));
    }

    /** A reunião: mede, registra a mudança de humor e aplica o efeito. */
    public static VillageHappiness meet(ServerWorld world, Colony colony) {
        List<ColonyPos> chests = ColonyChests.nearestFirst(world, colony.id(), colony.center());
        int food = 0;

        for (Map.Entry<Item, Integer> value : VillagerEntity.ITEM_FOOD_VALUES.entrySet()) {
            food += ColonyChests.countIn(world, chests, value.getKey()) * value.getValue();
        }

        int finished = (int) VillageColonyMod.BUILDINGS.ofColony(colony.id()).stream()
                .filter(Building::finished).count();
        VillageHappiness happiness = VillageHappiness.measure(
                food, VillagerScanner.livingAdultPopulation(world, colony), colony.observedBeds(), finished);
        VillageHappiness before = LAST.put(colony.id(), happiness);

        if (before == null || before.mood() != happiness.mood()) {
            VillageColonyMod.LOGGER.info(
                    "Colony {} is {} — {} food points per adult, {} spare beds, {} finished buildings",
                    colony.id().toString().substring(0, 8), happiness.mood(), happiness.foodPerAdult(),
                    happiness.spareBeds(), happiness.finishedBuildings());
        }

        for (Worker worker : VillageColonyMod.WORKERS.ofColony(colony.id())) {
            if (world.getEntity(worker.villagerId()) instanceof VillagerEntity villager
                    && villager.isAlive() && !villager.isBaby()) {
                apply(world, villager, happiness.mood(), chests);
            }
        }

        return happiness;
    }

    static void apply(ServerWorld world, VillagerEntity villager, VillageHappiness.Mood mood, List<ColonyPos> chests) {
        int wait = villager.getBreedingAge();

        if (mood == VillageHappiness.Mood.UNHAPPY) {
            villager.setBreedingAge(Math.max(wait, HOLD_TICKS));

            return;
        }

        if (mood != VillageHappiness.Mood.HAPPY) {
            return;
        }

        if (wait > 0) {
            villager.setBreedingAge(wait / 2);
        }

        if (pocketFood(villager.getInventory()) < VillageHappiness.FED
                && ColonyChests.withdraw(world, chests, Items.BREAD, 1) == 1) {
            ItemStack left = villager.getInventory().addStack(new ItemStack(Items.BREAD));

            if (!left.isEmpty()) {
                ColonyChests.deposit(world, chests, Items.BREAD, left.getCount());
            }
        }
    }

    private static int pocketFood(SimpleInventory pocket) {
        int points = 0;

        for (int slot = 0; slot < pocket.size(); slot++) {
            ItemStack stack = pocket.getStack(slot);

            points += VillagerEntity.ITEM_FOOD_VALUES.getOrDefault(stack.getItem(), 0) * stack.getCount();
        }

        return points;
    }
}
