package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.coordination.SupplyException;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.Supplies;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Regra 289: a plantação grande trabalha — a obra a entrega semeada, a semente surge quando nenhum baú tem, o
 * fazendeiro ara a terra que voltou a ser comum na roça dele, e só dois fazendeiros cuidam da plantação grande.
 */
public class LargeFarmSeedsGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(0, 1, 0);

    /** Sem semente em baú nenhum e com canteiro vazio na roça, a semente surge no baú do dono. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "large_farm_seeds", tickLimit = 5)
    public void seedsSurgeWhenNoChestHasOne(TestContext context) {
        context.setBlockState(CHEST, Blocks.CHEST);
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));
        Colony colony = Colony.create(UUID.randomUUID(), chest);
        UUID farmer = UUID.randomUUID();

        VillageColonyMod.COLONIES.register(colony);

        for (int x = 2; x <= 4; x++) {
            context.setBlockState(new BlockPos(x, 1, 2), Blocks.FARMLAND);
        }

        FarmOwners.own(farmer, box(context, new BlockPos(2, 1, 2), new BlockPos(4, 1, 2)));

        try {
            context.assertTrue(LargeFarmSeeds.surgeIfOut(context.getWorld(), colony.id(), farmer, chest),
                    "a semente não surgiu com a roça vazia e nenhum baú com semente");
            context.assertTrue(ColonyChests.countIn(context.getWorld(), List.of(chest), Items.WHEAT_SEEDS)
                    == LargeFarmSeeds.SURGE, "o baú do dono não recebeu a semente do trigo, a lavoura que falta");
            context.assertTrue(Supplies.sessionCount(colony.id(), SupplyException.SEED_FOR_THE_FARM) > 0,
                    "a semente surgida não foi declarada no VC_SUPPLY");
            context.assertFalse(LargeFarmSeeds.surgeIfOut(context.getWorld(), colony.id(), farmer, chest),
                    "a semente surgiu de novo com semente no baú");
        } finally {
            FarmOwners.forget(farmer);
            VillageColonyMod.COLONIES.remove(colony.id());
        }

        context.complete();
    }

    /** A plantação grande tem duas vagas de dono; o terceiro fazendeiro não trabalha nela. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "large_farm_seeds", tickLimit = 5)
    public void aLargeFarmTakesTwoFarmers(TestContext context) {
        BlockBox large = box(context, new BlockPos(1, 1, 1), new BlockPos(8, 1, 8));
        List<ColonyFarms.Farm> farms = List.of(
                new ColonyFarms.Farm(LargeFarmPlans.MODEL, large, ColonyFarms.Source.BUILT));
        List<UUID> farmers = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        ColonyPos near = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));
        Map<UUID, ColonyPos> chests = Map.of(farmers.get(0), near, farmers.get(1), near, farmers.get(2), near);

        Map<UUID, BlockBox> owners = FarmOwners.pair(farmers, chests, FarmOwners.slots(farms), Map.of());
        Set<UUID> ofLarge = new HashSet<>();
        owners.forEach((farmer, farm) -> {
            if (farm.equals(large)) {
                ofLarge.add(farmer);
            }
        });

        context.assertTrue(ofLarge.size() == LargeFarmSeeds.FARMERS,
                "a plantação grande ficou com " + ofLarge.size() + " donos, não com 2");

        UUID third = farmers.stream().filter(farmer -> !ofLarge.contains(farmer)).findFirst().orElseThrow();
        BlockPos inside = context.getAbsolutePos(new BlockPos(4, 1, 4));

        try {
            ofLarge.forEach(farmer -> FarmOwners.own(farmer, large));
            context.assertTrue(FarmOwners.isFullFor(third, inside),
                    "o terceiro fazendeiro ainda pode trabalhar na plantação grande");
            context.assertFalse(FarmOwners.isFullFor(ofLarge.iterator().next(), inside),
                    "o dono foi barrado da própria plantação");
        } finally {
            farmers.forEach(FarmOwners::forget);
        }

        context.complete();
    }

    /** A terra que voltou a ser comum na roça é canteiro vazio: o fazendeiro a ara antes de plantar. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "large_farm_seeds", tickLimit = 5)
    public void theFarmerTillsTheDirtInHisFarm(TestContext context) {
        BlockPos dirt = new BlockPos(3, 1, 3);
        context.setBlockState(dirt, Blocks.DIRT);
        // O vizinho é canteiro plantado: a terra comum fica no meio da roça, e não é o único candidato da conta.
        context.setBlockState(new BlockPos(2, 1, 3), Blocks.FARMLAND);
        context.setBlockState(new BlockPos(2, 2, 3), Blocks.WHEAT);
        BlockBox farm = box(context, new BlockPos(2, 1, 2), new BlockPos(4, 1, 4));

        var found = OwnFarm.work(context.getWorld(), farm, true);

        context.assertTrue(found.isPresent() && found.get().at().equals(context.getAbsolutePos(dirt)),
                "a terra comum dentro da roça não virou canteiro a semear");
        context.assertTrue(OwnFarm.tillIfDirt(context.getWorld(), context.getAbsolutePos(dirt)),
                "o fazendeiro não arou a terra comum da roça");
        context.expectBlock(Blocks.FARMLAND, dirt);
        context.complete();
    }

    /** Ao fechar, a plantação grande sai semeada, com lavouras variadas; outra obra não. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "large_farm_seeds", tickLimit = 5)
    public void theBuilderHandsTheLargeFarmSown(TestContext context) {
        for (int x = 1; x <= 4; x++) {
            context.setBlockState(new BlockPos(x, 1, 2), Blocks.FARMLAND);
        }

        BlockBox farm = box(context, new BlockPos(1, 1, 2), new BlockPos(4, 2, 2));

        context.assertTrue(LargeFarmSeeds.sowAtCompletion(context.getWorld(),
                new ResourceId("villagecolony", "colony/small_house"), farm) == 0, "uma casa saiu semeada");

        int sown = LargeFarmSeeds.sowAtCompletion(context.getWorld(), LargeFarmPlans.MODEL, farm);
        Set<Block> crops = new HashSet<>();

        for (int x = 1; x <= 4; x++) {
            crops.add(context.getBlockState(new BlockPos(x, 2, 2)).getBlock());
        }

        context.assertTrue(sown == 4, "a plantação grande saiu com " + sown + " canteiros semeados, não 4");
        context.assertTrue(crops.equals(Set.of(Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS)),
                "as lavouras não variam: " + crops);
        context.complete();
    }

    private static BlockBox box(TestContext context, BlockPos from, BlockPos to) {
        return BlockBox.create(context.getAbsolutePos(from), context.getAbsolutePos(to));
    }
}
