package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.CraftingLookup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O pastor colhe couro e carne do excedente do rebanho — decisão do autor, 2026-10-14.
 *
 * <p>Manutenção do ofício (não toca a obra, Regra 59): o pastor já cria até
 * {@link #FLOCK_FULL} adultos por espécie ({@link ShepherdFlock}); aqui ele <b>abate o excedente
 * acima de {@link #HARVEST_KEEP}</b>, um animal por ciclo, deixando sempre a reserva viva. A faixa
 * {@code HARVEST_KEEP}→{@code FLOCK_FULL} é a de colheita: a criação para em {@link #FLOCK_FULL} e o
 * abate para em {@link #HARVEST_KEEP}, com folga, então um não desfaz o outro.
 *
 * <p>O drop de cada espécie é depositado direto no baú (o padrão da tosquia em
 * {@link ShepherdWork}, não a loot table caindo no chão), e a <b>carne crua acima da reserva de
 * item é cozida</b> ({@link CraftingLookup#smelted}, sem combustível — como o fundidor). O ovo a
 * galinha já larga no chão sozinha (Vanilla); quem o recolhe é a coleta do chão, não esta classe.
 */
public final class FlockHarvest {

    /** Teto do rebanho que a criação busca ({@link ShepherdFlock#FLOCK_TARGET}). */
    static final int FLOCK_FULL = ShepherdFlock.FLOCK_TARGET;

    /** Abaixo disto o pastor nunca abate: a reserva viva por espécie. */
    static final int HARVEST_KEEP = 8;

    /** Quanto de cada item fica sempre guardado no baú antes de cozinhar/processar o excedente. */
    static final int ITEM_KEEP = 16;

    /** O drop que cada espécie rende ao ser abatida (média Vanilla, depositado direto). */
    private static final Map<EntityType<?>, List<ItemStack>> DROPS = Map.of(
            EntityType.COW, List.of(new ItemStack(Items.LEATHER, 1), new ItemStack(Items.BEEF, 2)),
            EntityType.PIG, List.of(new ItemStack(Items.PORKCHOP, 2)),
            EntityType.SHEEP, List.of(new ItemStack(Items.MUTTON, 2)),
            EntityType.CHICKEN, List.of(new ItemStack(Items.CHICKEN, 1), new ItemStack(Items.FEATHER, 1)));

    private FlockHarvest() {
    }

    /** Quantos adultos excedem a reserva viva (nunca negativo) — lógica pura, ver {@code FlockHarvestTest}. */
    static int surplus(int adults) {
        return Math.max(0, adults - HARVEST_KEEP);
    }

    /** Se há excedente para abater. */
    static boolean hasSurplus(int adults) {
        return surplus(adults) > 0;
    }

    /**
     * Uma passagem do abate: para a primeira espécie com excedente, abate um adulto e deposita o
     * drop; depois cozinha a carne crua que passa da reserva de item. Uma ação por ciclo, como a
     * criação de {@link ShepherdFlock}.
     *
     * @return se algo foi abatido
     */
    static boolean step(ServerWorld world, Colony colony, Box area, List<ColonyPos> chests) {
        Optional<UUID> shepherd = VillageColonyMod.WORKERS.ofColony(colony.id()).stream()
                .filter(w -> w.profession().filter(ProfessionType.SHEPHERD::equals).isPresent())
                .map(w -> w.villagerId())
                .findFirst();

        if (shepherd.isEmpty()) {
            return false;
        }

        boolean butchered = false;

        for (EntityType<? extends AnimalEntity> species : ShepherdFlock.SPECIES) {
            List<? extends AnimalEntity> adults = new ArrayList<>(world.getEntitiesByType(
                    species, area, animal -> animal.isAlive() && !animal.isBaby()));

            if (!hasSurplus(adults.size())) {
                continue;
            }

            AnimalEntity victim = adults.get(adults.size() - 1);

            depositDrops(world, species, chests, shepherd.get());
            victim.kill();
            butchered = true;

            break;
        }

        cookSurplusMeat(world, chests);

        return butchered;
    }

    private static void depositDrops(ServerWorld world, EntityType<?> species, List<ColonyPos> chests, UUID shepherd) {
        for (ItemStack drop : DROPS.getOrDefault(species, List.of())) {
            deposit(world, chests, drop.getItem(), drop.getCount());
            ActionJournal.action(shepherd, "BUTCHERED", ActionJournal.idOf(drop.getItem()), drop.getCount(), null, "");
        }
    }

    /** Carne crua acima da reserva de item vira assada (sem combustível), guardada no baú. */
    private static void cookSurplusMeat(ServerWorld world, List<ColonyPos> chests) {
        for (Item raw : List.of(Items.BEEF, Items.PORKCHOP, Items.MUTTON, Items.CHICKEN)) {
            int have = ColonyChests.countIn(world, chests, raw);

            if (have <= ITEM_KEEP) {
                continue;
            }

            Optional<ItemStack> cooked = CraftingLookup.smelted(world, new ItemStack(raw, 1));

            if (cooked.isEmpty()) {
                continue;
            }

            int toCook = have - ITEM_KEEP;

            if (ColonyChests.withdraw(world, chests, raw, toCook) != toCook) {
                continue;
            }

            deposit(world, chests, cooked.get().getItem(), cooked.get().getCount() * toCook);
        }
    }

    private static void deposit(ServerWorld world, List<ColonyPos> chests, Item item, int amount) {
        int left = amount;

        for (ColonyPos chest : chests) {
            if (left <= 0) {
                return;
            }

            left = ChestDepositor.deposit(world, chest, item, left);
        }
    }
}
