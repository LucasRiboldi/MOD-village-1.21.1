package com.villagecolony.fabric.work;

import com.google.gson.JsonObject;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BedPart;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.UUID;

/**
 * As camas da caixa (Regra 145) e a auditoria do diário da vila (Regra 147): o número que o mod guarda é
 * conferido contra o mundo, e a diferença aparece sozinha.
 */
public class VillageDiaryGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_diary", tickLimit = 40)
    public void theBedsOfTheBoxAreCountedAndTheAuditSeesTheDifference(TestContext context) {
        placeBed(context, new BlockPos(2, 1, 3));
        placeBed(context, new BlockPos(5, 1, 3));

        Colony colony = Colony.create(UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(4, 1, 4))));
        colony.measure(VillageBounds.aroundPiece(
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(0, 0, 0))),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(7, 3, 7))), 0));
        // Fora do registro de colônias: o tique do BoxBeds não corrige o número antes da auditoria.
        colony.countBeds(5);

        context.waitAndRun(5, () -> {
            var box = colony.bounds().orElseThrow();

            context.assertTrue(BoxBeds.count(context.getWorld(), box).orElse(-1) == 2,
                    "as duas camas da caixa deviam ser contadas: " + BoxBeds.count(context.getWorld(), box));

            JsonObject audit = VillageDiary.audit(context.getWorld(), colony);

            context.assertTrue(audit.get("bedsInTheWorld").getAsInt() == 2 && audit.get("beds").getAsInt() == 5,
                    "a auditoria devia comparar 5 guardadas com 2 no mundo: " + audit);
            context.assertTrue(audit.get("problems").getAsInt() >= 1, "a diferença de camas não virou problema");

            context.complete();
        });
    }

    private static void placeBed(TestContext context, BlockPos foot) {
        BlockState bed = Blocks.RED_BED.getDefaultState().with(BedBlock.FACING, Direction.NORTH);

        context.setBlockState(foot, bed.with(BedBlock.PART, BedPart.FOOT));
        context.setBlockState(foot.north(), bed.with(BedBlock.PART, BedPart.HEAD));
    }
}
