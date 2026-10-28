package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.content.ModContent;
import com.villagecolony.fabric.integration.BenchPresence;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Regra 132: as passivas valem só com a bancada do ofício na vila. */
public class BenchPassivesGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "bench_passives", tickLimit = 20)
    public void theBenchesMakeTheMineTheStoneAndTheButcherYieldMore(TestContext context) {
        BlockPos center = new BlockPos(4, 2, 4);
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center)));

        try {
            List<ItemStack> before = new ArrayList<>(List.of(new ItemStack(Items.RAW_IRON)));
            BenchYield.mined(context.getWorld(), colony, Blocks.IRON_ORE.getDefaultState(), before);
            context.assertTrue(before.getFirst().getCount() == 1, "sem bancada o minério não dobra");
            context.assertTrue(BenchYield.butchered(context.getWorld(), colony, Items.BEEF, 2) == 2,
                    "sem bancada o abate não dobra");

            context.setBlockState(new BlockPos(2, 2, 2), ModContent.MINER_BENCH);
            context.setBlockState(new BlockPos(6, 2, 2), ModContent.MASON_CUTTER);
            context.setBlockState(new BlockPos(2, 2, 6), ModContent.SHEPHERD_BENCH);
            BenchPresence.forget(colony.id());

            List<ItemStack> iron = new ArrayList<>(List.of(new ItemStack(Items.RAW_IRON)));
            BenchYield.mined(context.getWorld(), colony, Blocks.IRON_ORE.getDefaultState(), iron);
            context.assertTrue(iron.getFirst().getCount() == 2, "o ferro devia render o dobro, veio " + iron);

            List<ItemStack> diamond = new ArrayList<>(List.of(new ItemStack(Items.DIAMOND)));
            BenchYield.mined(context.getWorld(), colony, Blocks.DIAMOND_ORE.getDefaultState(), diamond);
            context.assertTrue(diamond.getFirst().getCount() == 2, "o diamante devia render +1, veio " + diamond);

            int extra = 0;
            for (int i = 0; i < BenchYield.STONE_EVERY * 2; i++) {
                List<ItemStack> stone = new ArrayList<>(List.of(new ItemStack(Items.COBBLESTONE)));
                BenchYield.mined(context.getWorld(), colony, Blocks.STONE.getDefaultState(), stone);
                extra += stone.size() - 1;
            }
            context.assertTrue(extra == 2, "8 pedras deviam render 2 pedregulhos a mais, renderam " + extra);

            context.assertTrue(BenchYield.butchered(context.getWorld(), colony, Items.BEEF, 2) == 4,
                    "o abate devia render o dobro de carne");
            context.assertTrue(BenchYield.butchered(context.getWorld(), colony, Items.FEATHER, 2) == 2,
                    "pena não é carne nem couro");
        } finally {
            BenchPresence.forget(colony.id());
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "bench_passives", tickLimit = 20)
    public void theShiftClickPanelNamesTheHolders(TestContext context) {
        BlockPos bench = new BlockPos(3, 2, 3);
        context.setBlockState(bench, ModContent.CARPENTER_BENCH);
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(bench)));
        UUID carpenter = UUID.randomUUID();

        VillageColonyMod.COLONIES.register(colony);
        VillageColonyMod.WORKERS.register(carpenter, colony.id()).assign(ProfessionType.CARPENTER);

        try {
            List<Text> lines = BenchPanel.lines(context.getWorld(), context.getAbsolutePos(bench), ProfessionType.CARPENTER);

            context.assertTrue(lines.size() == 3, "cabeçalho, um titular e as passivas; vieram " + lines.size());
            context.assertTrue(lines.get(1).getString().contains(carpenter.toString().substring(0, 8)),
                    "o titular não aparece no painel: " + lines.get(1).getString());
        } finally {
            VillageColonyMod.WORKERS.remove(carpenter);
            VillageColonyMod.COLONIES.remove(colony.id());
            context.setBlockState(bench, net.minecraft.block.Blocks.AIR);
        }

        context.complete();
    }

    // Lote próprio: o teste do painel põe uma bancada de carpinteiro na arena vizinha, no mesmo instante, e a busca
    // de bancada (raio de 64 em volta da colônia) a achava — "sem bancada não encanta" falhava por acaso.
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "bench_passives_enchant", tickLimit = 20)
    public void theCarpenterEnchantsAndTheForgeFindsTheFurnaces(TestContext context) {
        BlockPos center = new BlockPos(5, 2, 5);
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center)));
        VillageColonyMod.COLONIES.register(colony);

        try {
            ItemStack plain = new ItemStack(Items.DIAMOND_PICKAXE);
            context.assertFalse(com.villagecolony.fabric.integration.CraftedEnchant.onCrafted(
                    context.getWorld(), context.getAbsolutePos(center), plain), "sem bancada não encanta");

            context.setBlockState(new BlockPos(1, 2, 1), ModContent.CARPENTER_BENCH);
            context.setBlockState(new BlockPos(1, 2, 9), ModContent.BLACKSMITH_FORGE);
            context.setBlockState(new BlockPos(9, 2, 1), ModContent.FARMER_BENCH);
            context.setBlockState(new BlockPos(3, 2, 3), Blocks.FURNACE);
            BenchPresence.forget(colony.id());

            ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
            context.assertTrue(com.villagecolony.fabric.integration.CraftedEnchant.onCrafted(
                    context.getWorld(), context.getAbsolutePos(center), pickaxe) && pickaxe.hasEnchantments(),
                    "a ferramenta feita na vila devia sair encantada");

            context.assertTrue(BenchUpkeep.furnacesNear(context.getWorld(), colony)
                    .contains(context.getAbsolutePos(new BlockPos(3, 2, 3))), "a fornalha perto da forja não foi achada");

        } finally {
            BenchPresence.forget(colony.id());
            VillageColonyMod.COLONIES.remove(colony.id());
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "bench_passives", tickLimit = 20)
    public void aRoadHoleLowerThanItsNeighboursIsFilled(TestContext context) {
        net.minecraft.server.world.ServerWorld world = context.getWorld();
        BlockPos base = context.getAbsolutePos(BlockPos.ORIGIN).add(4_500_000, 60, 4_500_000);
        List<Long> roads = new ArrayList<>();

        try {
            for (int dx = -1; dx <= 1; dx++) {
                world.getChunk(base.add(dx, 0, 0));
                world.setBlockState(base.add(dx, 0, 0), Blocks.DIRT_PATH.getDefaultState());
                roads.add(com.villagecolony.core.construction.model.ColonyRoads.column(base.getX() + dx, base.getZ()));
            }
            world.setBlockState(base, Blocks.AIR.getDefaultState());
            world.setBlockState(base.down(), Blocks.DIRT.getDefaultState());

            var filled = BenchUpkeep.fillIfHole(world, roads, base.down());

            context.assertTrue(filled.isPresent() && world.getBlockState(base).isOf(Blocks.DIRT_PATH),
                    "o buraco entre duas ruas devia ser tapado com o chão delas, veio " + world.getBlockState(base));
            context.assertTrue(BenchUpkeep.fillIfHole(world, roads, base).isEmpty(), "rua no nível não é buraco");
        } finally {
            for (BlockPos at : BlockPos.iterate(base.add(-1, -1, 0), base.add(1, 1, 0))) {
                world.setBlockState(at, Blocks.AIR.getDefaultState());
            }
        }

        context.complete();
    }
}
