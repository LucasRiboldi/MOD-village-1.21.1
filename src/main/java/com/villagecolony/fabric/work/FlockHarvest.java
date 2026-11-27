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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O pastor colhe couro e carne do excedente do rebanho — decisão do autor, 2026-10-14.
 *
 * <p>Abate um adulto por ciclo quando o total da espécie nos currais excede a meta de criação.
 * O curral da vítima precisa ter ao menos três adultos da espécie antes do abate (Regras 167 e 238).
 *
 * <p>O drop de cada espécie é depositado direto no baú (o padrão da tosquia em
 * {@link ShepherdWork}, não a loot table caindo no chão), e a <b>carne crua acima da reserva de
 * item é cozida</b> ({@link CraftingLookup#smelted}, sem combustível — como o fundidor). O ovo a
 * galinha já larga no chão sozinha (Vanilla); quem o recolhe é a coleta do chão, não esta classe.
 */
public final class FlockHarvest {

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

    /** Nenhum abate da espécie com menos adultos que isto, seja qual for a meta (Regra 380). */
    static final int MIN_BEFORE_HARVEST = 6;

    /** Quantos adultos excedem a meta de criação da espécie, com o piso de {@value #MIN_BEFORE_HARVEST} (Regra 380). */
    static int surplus(int adults, int target) {
        return Math.max(0, adults - Math.max(target, MIN_BEFORE_HARVEST));
    }

    /** Abate um adulto excedente e guarda os produtos no baú. */
    static boolean step(ServerWorld world, Colony colony, PenFlock flock, List<ColonyPos> chests,
            Map<EntityType<?>, Integer> targets) {
        if (!ShepherdFlock.hasShepherd(colony)) {
            return false;
        }

        for (var species : ShepherdFlock.LIVESTOCK) {
            if (!DROPS.containsKey(species)) {
                continue;
            }
            List<AnimalEntity> adults = flock.adults(species);
            int target = targets.getOrDefault(species, ShepherdFlock.FLOCK_TARGET);
            if (surplus(adults.size(), target) == 0) {
                continue;
            }

            for (PenFlock.Stock stock : flock.pens()) {
                List<AnimalEntity> local = stock.adults(species);
                if (local.size() < MIN_PEN_ADULTS) {
                    continue;
                }
                for (AnimalEntity victim : local) {
                    if (!canButcher(world, victim)) {
                        continue;
                    }
                    var shepherd = ShepherdFlock.nearbyShepherd(world, colony, victim);
                    if (shepherd == null) {
                        continue;
                    }
                    depositDrops(world, colony, species, chests, shepherd.getUuid());
                    victim.kill();
                    VillageColonyMod.LOGGER.info(
                            "VC_FLOCK_HARVEST version=2 colony={} animal={} species={} gate={} at={} "
                                    + "adults={} adults_after={} target={} pen_adults={} pen_adults_after={} "
                                    + "young={} pen_young={} in_pen=true",
                            colony.id().toString().substring(0, 8), victim.getUuid(), species.getUntranslatedName(),
                            stock.pen().gate().toShortString(), victim.getBlockPos().toShortString(),
                            adults.size(), adults.size() - 1, target, local.size(), local.size() - 1,
                            flock.young(species), stock.young(species));
                    cookSurplusMeat(world, chests);
                    return true;
                }
            }
        }
        return false;
    }

    /** Regra 299: poupa animais em condução, abrigados ou já selecionados para reprodução. */
    private static boolean canButcher(ServerWorld world, AnimalEntity animal) {
        return !animal.isInLove()
                && !(animal.getLeashHolder() instanceof net.minecraft.entity.passive.VillagerEntity)
                && !ShepherdHerding.isHerded(animal.getUuid())
                && !HerdCandidates.sheltersAnimal(world, animal.getBlockPos());
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
