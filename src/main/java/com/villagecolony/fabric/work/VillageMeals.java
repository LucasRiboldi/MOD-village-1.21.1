package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.coordination.WorkClock;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.VillageFocus;
import com.villagecolony.fabric.integration.ColonyChests;

import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.inventory.SimpleInventory;
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
 * A comida do fim do expediente — N1, 2026-09-24.
 *
 * <p><b>Decisão do autor:</b> a fundação não repõe mais quem morre; a vila
 * cresce como no Vanilla, por procriação. O Vanilla pede duas coisas para
 * isso: cama livre e aldeão disposto, e disposto quer dizer doze pontos de
 * comida no inventário ({@link VillagerEntity#ITEM_FOOD_VALUES}: pão vale 4,
 * cenoura, batata e beterraba valem 1). No Vanilla quem espalha a comida é o
 * fazendeiro, jogando-a para os outros; aqui a colheita vai para o baú, e
 * sem esta classe ninguém nunca ficaria disposto.
 *
 * <p><b>Quando.</b> Uma vez por dia, ao fim do expediente
 * ({@link WorkClock#DUSK}), que é quando a agenda Vanilla põe o aldeão à
 * toa até a hora de dormir — e é na hora à toa que ele procria.
 *
 * <p><b>É a reunião da vila</b> — ADR-037 R1, Regra 217: a comida de qualquer baú é
 * doada a cada adulto até {@link #BREEDING_FOOD} pontos, com cama sobrando ou
 * não, e antes o fazendeiro assa o trigo guardado ({@link FarmerBakery}).
 * Todos alimentados é o convívio que a felicidade conta ({@link VillageMood}).
 * Com pastor na vila, um par da comida de cada espécie do rebanho fica no baú
 * ({@link ShepherdFlock#breedingFoodToKeep}, D2, decisão do autor de 2026-10-21).
 */
public final class VillageMeals {

    static {
        ServerMemory.register(VillageMeals.class, VillageMeals::clearAll);
    }

    /** Os pontos que o Vanilla exige para o aldeão querer procriar. */
    static final int BREEDING_FOOD = 12;

    /** O fim da hora à toa: depois disso a agenda manda dormir. */
    private static final int REST = 12_000;

    /** O que um pão vale de disposição. */
    private static final int LOAF_POINTS = 4;

    private static final int EVERY_TICKS = 20;

    /** Pão primeiro: o mesmo dia de comida em menos espaço de inventário. */
    private static final List<Item> MENU = List.of(
            Items.BREAD, Items.CARROT, Items.POTATO, Items.BEETROOT);

    private static final Map<UUID, Long> SERVED_ON = new HashMap<>();

    private static int ticks;

    private VillageMeals() {
    }

    public static void tick(ServerWorld world) {
        if (++ticks % EVERY_TICKS != 0) {
            return;
        }

        long time = world.getTimeOfDay();
        int hour = (int) Math.floorMod(time, (long) WorkClock.DAY);

        if (hour < WorkClock.DUSK || hour >= REST) {
            return;
        }

        long day = Math.floorDiv(time, (long) WorkClock.DAY);

        for (Colony colony : List.copyOf(VillageColonyMod.COLONIES.all())) {
            if (!VillageFocus.isWorking(world, colony.id())) {
                continue;
            }

            if (SERVED_ON.getOrDefault(colony.id(), Long.MIN_VALUE) == day) {
                continue;
            }

            SERVED_ON.put(colony.id(), day);
            serve(world, colony);
        }
    }

    /**
     * Dá comida a cada adulto da colônia que ainda não está disposto.
     *
     * @return quantos aldeões receberam comida
     */
    public static int serve(ServerWorld world, Colony colony) {
        BlockPos center = MinecraftTypeAdapter.toBlockPos(colony.center());
        Box area = Box.of(center.toCenterPos(), 128.0, 128.0, 128.0);
        int living = world.getEntitiesByClass(
                VillagerEntity.class, area, VillagerEntity::isAlive).size();

        List<ColonyPos> chests = ColonyChests.nearestFirst(world, colony.id(), colony.center());
        // A comida de cria do rebanho fica no baú (ShepherdFlock.breedingFoodToKeep, D2).
        Map<Item, Integer> keep = ShepherdFlock.breedingFoodToKeep(world, colony);
        // Todo adulto da colônia, com ofício ou sem: o VillagerScanner registra cada aldeão da vila em WORKERS.
        List<VillagerEntity> hungry = new ArrayList<>();
        boolean farmer = false;

        for (Worker worker : VillageColonyMod.WORKERS.ofColony(colony.id())) {
            farmer |= worker.profession().filter(ProfessionType.FARMER::equals).isPresent();

            if (world.getEntity(worker.villagerId()) instanceof VillagerEntity villager
                    && villager.isAlive()
                    && !villager.isBaby()
                    && villager.getBreedingAge() == 0
                    && !villager.isReadyToBreed()) {
                hungry.add(villager);
            }
        }

        // O fazendeiro assa o trigo guardado antes da ceia — ADR-037 C7 c, Regra 217.
        if (farmer) {
            FarmerBakery.bakeForSupper(world, chests,
                    hungry.size() * BREEDING_FOOD / LOAF_POINTS - ColonyChests.countIn(world, chests, Items.BREAD));
        }

        int fed = 0;

        for (VillagerEntity villager : hungry) {
            if (feed(world, chests, villager, keep) > 0) {
                fed++;
            }
        }

        if (fed > 0) {
            VillageColonyMod.LOGGER.info(
                    "Colony {} shared supper with {} villagers ({} living, {} beds)",
                    colony.id(), fed, living, colony.observedBeds());
        }

        return fed;
    }

    /** Tira do baú até doze pontos de comida, acima do que fica para o rebanho, e põe no inventário dele. */
    private static int feed(ServerWorld world, List<ColonyPos> chests, VillagerEntity villager, Map<Item, Integer> keep) {
        SimpleInventory pocket = villager.getInventory();
        int points = 0;

        for (Item food : MENU) {
            int value = VillagerEntity.ITEM_FOOD_VALUES.getOrDefault(food, 0);
            int kept = keep.getOrDefault(food, 0);

            while (value > 0 && points < BREEDING_FOOD) {
                ItemStack one = new ItemStack(food);

                if (!pocket.canInsert(one)
                        || (kept > 0 && ColonyChests.countIn(world, chests, food) <= kept)
                        || ColonyChests.withdraw(world, chests, food, 1) == 0) {
                    break;
                }

                pocket.addStack(one);
                points += value;
            }
        }

        return points;
    }

    /** O servidor fechou: o dia de um mundo não vale para o próximo. */
    public static void clearAll() {
        SERVED_ON.clear();
        ticks = 0;
    }
}
