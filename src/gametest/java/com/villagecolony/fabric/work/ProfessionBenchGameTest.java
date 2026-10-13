package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.content.ModContent;
import com.villagecolony.gametest.ColonyFixture;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * As bancadas do mod — decisão do autor, 2026-10-13: a estação do ofício vem do mod,
 * dobra a produção ({@link ModBenchBonus}) e emite luz ({@link ModContent#LIGHT}).
 */
public class ProfessionBenchGameTest implements FabricGameTest {

    /** O carpinteiro vinculado à bancada do mod dobra; sem ela, produção normal. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mod_bench_carpenter", tickLimit = 20)
    public void theCarpenterDoublesAtTheModBench(TestContext context) {
        ColonyFixture fixture = ColonyFixture.colonyAt(context, new BlockPos(1, 1, 1));
        UUID carpenter = UUID.randomUUID();
        BlockPos chest = context.getAbsolutePos(new BlockPos(2, 1, 2));
        BlockPos bench = context.getAbsolutePos(new BlockPos(5, 1, 2));

        fixture.owning(carpenter);
        VillageColonyMod.WORKERS.register(carpenter, fixture.colony().id()).assign(ProfessionType.CARPENTER);

        // Sem a bancada do mod: produção normal.
        context.assertTrue(ModBenchBonus.factor(context.getWorld(), carpenter, chest) == 1,
                "sem a bancada do mod o fator devia ser 1");

        context.getWorld().setBlockState(bench, ModContent.CARPENTER_BENCH.getDefaultState());

        // O POI entra no registro do mundo no tique seguinte.
        context.waitAndRun(2, () -> {
            try {
                BlockPos spot = CraftStation.spotFor(context.getWorld(), carpenter, chest);

                context.assertTrue(spot.equals(bench),
                        "o carpinteiro não foi para a bancada do mod: " + spot.toShortString());
                context.assertTrue(ModBenchBonus.usesModBench(context.getWorld(), carpenter, chest),
                        "a bancada do mod perto do baú não foi reconhecida");
                context.assertTrue(ModBenchBonus.factor(context.getWorld(), carpenter, chest) == 2,
                        "a bancada do mod não dobrou a produção");
            } finally {
                CraftStation.clearAll();
                context.getWorld().setBlockState(bench, Blocks.AIR.getDefaultState());
                fixture.cleanUp();
            }

            context.complete();
        });
    }

    /** A forja do mod dá dobro ao fundidor, que sem bancada não tinha nenhum. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mod_bench_smelter", tickLimit = 20)
    public void theSmelterDoublesAtTheModForge(TestContext context) {
        ColonyFixture fixture = ColonyFixture.colonyAt(context, new BlockPos(1, 1, 1));
        UUID smelter = UUID.randomUUID();
        BlockPos chest = context.getAbsolutePos(new BlockPos(2, 1, 2));
        BlockPos forge = context.getAbsolutePos(new BlockPos(5, 1, 2));

        fixture.owning(smelter);
        VillageColonyMod.WORKERS.register(smelter, fixture.colony().id()).assign(ProfessionType.SMELTER);

        context.assertTrue(ModBenchBonus.factor(context.getWorld(), smelter, chest) == 1,
                "sem a forja do mod o fator devia ser 1");

        context.getWorld().setBlockState(forge, ModContent.BLACKSMITH_FORGE.getDefaultState());

        context.waitAndRun(2, () -> {
            try {
                context.assertTrue(ModBenchBonus.factor(context.getWorld(), smelter, chest) == 2,
                        "a forja do mod não dobrou a produção do fundidor");
            } finally {
                CraftStation.clearAll();
                context.getWorld().setBlockState(forge, Blocks.AIR.getDefaultState());
                fixture.cleanUp();
            }

            context.complete();
        });
    }

    /** As bancadas do mod emitem luz nível {@link ModContent#LIGHT} (decisão do autor: 10). */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mod_bench_light", tickLimit = 1)
    public void theModBenchesEmitLight(TestContext context) {
        context.assertTrue(ModContent.CARPENTER_BENCH.getDefaultState().getLuminance() == ModContent.LIGHT,
                "a bancada do carpinteiro não emite luz " + ModContent.LIGHT);
        context.assertTrue(ModContent.MASON_CUTTER.getDefaultState().getLuminance() == ModContent.LIGHT,
                "o cortador do pedreiro não emite luz " + ModContent.LIGHT);
        context.assertTrue(ModContent.BLACKSMITH_FORGE.getDefaultState().getLuminance() == ModContent.LIGHT,
                "a forja do ferreiro não emite luz " + ModContent.LIGHT);

        context.complete();
    }

    /** A profissão sem bancada do mod (mineiro) nunca dobra. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mod_bench_none", tickLimit = 1)
    public void aProfessionWithoutAModBenchNeverDoubles(TestContext context) {
        ColonyFixture fixture = ColonyFixture.colonyAt(context, new BlockPos(1, 1, 1));
        UUID miner = UUID.randomUUID();
        BlockPos chest = context.getAbsolutePos(new BlockPos(2, 1, 2));

        try {
            fixture.owning(miner);
            VillageColonyMod.WORKERS.register(miner, fixture.colony().id()).assign(ProfessionType.MINER);

            context.assertTrue(ModContent.benchBlock(ProfessionType.MINER) == null,
                    "o mineiro não devia ter bancada do mod");
            context.assertTrue(ModBenchBonus.factor(context.getWorld(), miner, chest) == 1,
                    "o mineiro não devia dobrar");
        } finally {
            fixture.cleanUp();
        }

        context.complete();
    }
}
