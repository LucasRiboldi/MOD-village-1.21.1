package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.coordination.WorkClock;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ColonyChests;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

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
 * <p><b>Só com cama sobrando.</b> Com a vila cheia a procriação não
 * aconteceria, e a comida sairia do baú para nada.
 */
public final class VillageMeals {

    /** Os pontos que o Vanilla exige para o aldeão querer procriar. */
    static final int BREEDING_FOOD = 12;

    /** O fim da hora à toa: depois disso a agenda manda dormir. */
    private static final int REST = 12_000;

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

        if (living >= colony.observedBeds()) {
            return 0;
        }

        List<ColonyPos> chests = ColonyChests.nearestFirst(world, colony.id(), colony.center());
        int fed = 0;

        for (Worker worker : VillageColonyMod.WORKERS.ofColony(colony.id())) {
            Entity entity = world.getEntity(worker.villagerId());

            if (!(entity instanceof VillagerEntity villager)
                    || !villager.isAlive()
                    || villager.isBaby()
                    || villager.getBreedingAge() != 0
                    || villager.isReadyToBreed()) {
                continue;
            }

            if (feed(world, chests, villager) > 0) {
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

    /** Tira do baú até doze pontos de comida e põe no inventário dele. */
    private static int feed(ServerWorld world, List<ColonyPos> chests, VillagerEntity villager) {
        SimpleInventory pocket = villager.getInventory();
        int points = 0;

        for (Item food : MENU) {
            int value = VillagerEntity.ITEM_FOOD_VALUES.getOrDefault(food, 0);

            while (value > 0 && points < BREEDING_FOOD) {
                ItemStack one = new ItemStack(food);

                if (!pocket.canInsert(one)
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
