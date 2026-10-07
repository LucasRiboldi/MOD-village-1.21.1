package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.gametest.ColonyFixture;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/** O artesão trabalha diante da bancada do ofício (ADR-038 P3c). */
public class CraftStationGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "craft_station", tickLimit = 20)
    public void theCarpenterWorksAtTheBenchAndTheMasonWithoutOneAtTheChest(TestContext context) {
        ColonyFixture fixture = ColonyFixture.colonyAt(context, new BlockPos(1, 1, 1));
        UUID carpenter = UUID.randomUUID();
        UUID mason = UUID.randomUUID();
        BlockPos chest = context.getAbsolutePos(new BlockPos(2, 1, 2));
        BlockPos bench = context.getAbsolutePos(new BlockPos(5, 1, 2));

        try {
            fixture.owning(carpenter).owning(mason);
            VillageColonyMod.WORKERS.register(carpenter, fixture.colony().id()).assign(ProfessionType.CARPENTER);
            VillageColonyMod.WORKERS.register(mason, fixture.colony().id()).assign(ProfessionType.MASON);
            context.getWorld().setBlockState(bench, Blocks.CRAFTING_TABLE.getDefaultState());

            BlockPos carpenterSpot = CraftStation.spotFor(context.getWorld(), carpenter, chest);
            BlockPos masonSpot = CraftStation.spotFor(context.getWorld(), mason, chest);

            context.assertTrue(carpenterSpot.equals(bench),
                    "o carpinteiro foi para " + carpenterSpot.toShortString() + " e não para a bancada");
            context.assertTrue(masonSpot.equals(chest),
                    "sem cortador nem sino, o pedreiro foi para " + masonSpot.toShortString() + " e não ficou no baú");
        } finally {
            CraftStation.clearAll();
            fixture.cleanUp();
        }

        context.complete();
    }
}
