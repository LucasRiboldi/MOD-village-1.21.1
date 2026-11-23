package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ColonyPos;
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
 * abate para em {@link #HARVEST_KEEP}, com folga, então um não desfaz o outro. O curral
 * da vítima também precisa ter pelo menos {@link #MIN_PEN_ADULTS} adultos da espécie (Regra 167).
 *
 * <p>O drop de cada espécie é depositado direto no baú (o padrão da tosquia em
 * {@link ShepherdWork}, não a loot table caindo no chão), e a <b>carne crua acima da reserva de
 * item é cozida</b> ({@link CraftingLookup#smelted}, sem combustível — como o fundidor). O ovo a
 * galinha já larga no chão sozinha (Vanilla); quem o recolhe é a coleta do chão, não esta classe.
 */
public final class FlockHarvest {

    /** Teto do rebanho que a criação busca ({@link ShepherdFlock#FLOCK_TARGET}). */
    static final int FLOCK_FULL = ShepherdFlock.FLOCK_TARGET;

    /** Abaixo disto o pastor nunca abate: a reserva viva por espécie — autor, 2026-10-22 (eram 8). */
    static final int HARVEST_KEEP = 6;

    /** Adultos da espécie exigidos no curral antes do abate, para preservar dois (Regra 167). */
    private static final int MIN_PEN_ADULTS = 3;

    /** Quanto de cada item fica sempre guardado no baú antes de cozinhar/processar o excedente. */
    static final int ITEM_KEEP = 16;

    /** O drop que cada espécie rende ao ser abatida (média Vanilla, depositado direto). */
    private static final Map<EntityType<?>, List<ItemStack>> DROPS = Map.of(
            EntityType.COW, List.of(new ItemStack(Items.LEATHER, 1), new ItemStack(Items.BEEF, 2)),
            EntityType.PIG, List.of(new ItemStack(Items.PORKCHOP, 2)),
            EntityType.SHEEP, List.of(new ItemStack(Items.MUTTON, 2)),
            EntityType.CHICKEN, List.of(new ItemStack(Items.CHICKEN, 1), new ItemStack(Items.FEATHER, 1)),
            EntityType.RABBIT, List.of(new ItemStack(Items.RABBIT, 1), new ItemStack(Items.RABBIT_HIDE, 1)),
            EntityType.MOOSHROOM, List.of(new ItemStack(Items.LEATHER, 1), new ItemStack(Items.BEEF, 2)));

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
        if (!ShepherdFlock.hasShepherd(colony)) {
            return false;
        }

        boolean butchered = false;

        for (EntityType<? extends AnimalEntity> species : ShepherdFlock.LIVESTOCK.stream().filter(DROPS::containsKey).toList()) {
            List<? extends AnimalEntity> adults = new ArrayList<>(world.getEntitiesByType(
                    species, area, animal -> animal.isAlive() && !animal.isBaby()));

            if (!hasSurplus(adults.size())) {
                continue;
            }

            AnimalEntity victim = victimAmong(world, colony, species, adults);
            if (victim == null) {
                continue;
            }

            depositDrops(world, colony, species, chests,
                    ShepherdFlock.nearbyShepherd(world, colony, victim).getUuid());
            var at = victim.getBlockPos();
            VillageColonyMod.LOGGER.info("VC_FLOCK_HARVEST version=1 colony={} species={} at={} adults={} in_pen={}",
                    colony.id().toString().substring(0, 8), species.getUntranslatedName(), at.toShortString(),
                    adults.size(), HerdCandidates.isInAnyPen(world, colony, victim));
            victim.kill();
            butchered = true;

            break;
        }

        if (butchered) {
            cookSurplusMeat(world, chests);
        }

        return butchered;
    }

    /** Só abate dentro de um curral com reserva local (Regra 167), fora de guia e coleta (Regra 299). */
    private static AnimalEntity victimAmong(ServerWorld world, Colony colony,
            EntityType<? extends AnimalEntity> species, List<? extends AnimalEntity> adults) {
        List<ShepherdPens.Pen> eligiblePens = ShepherdPens.pensOf(world, colony).stream()
                .filter(pen -> world.getEntitiesByType(species, ShepherdPens.areaOf(pen),
                        animal -> animal.isAlive() && !animal.isBaby()
                                && pen.reach().contains(animal.getBlockPos())).size() >= MIN_PEN_ADULTS)
                .toList();

        return adults.stream()
                .filter(animal -> eligiblePens.stream().anyMatch(pen -> pen.reach().contains(animal.getBlockPos())))
                .filter(animal -> !(animal.getLeashHolder() instanceof net.minecraft.entity.passive.VillagerEntity)
                        && !ShepherdHerding.isHerded(animal.getUuid()))
                .filter(animal -> ShepherdFlock.nearbyShepherd(world, colony, animal) != null)
                .filter(animal -> !HerdCandidates.sheltersAnimal(world, animal.getBlockPos()))
                .findFirst().orElse(null);
    }

    private static void depositDrops(ServerWorld world, Colony colony, EntityType<?> species, List<ColonyPos> chests,
            UUID shepherd) {
        for (ItemStack drop : DROPS.getOrDefault(species, List.of())) {
            int count = BenchYield.butchered(world, colony, drop.getItem(), drop.getCount()); // bancada do pastor, Regra 132
            deposit(world, chests, drop.getItem(), count);
            ActionJournal.action(shepherd, "BUTCHERED", ActionJournal.idOf(drop.getItem()), count, null, "");
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
