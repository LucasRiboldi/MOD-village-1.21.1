package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ColonyChests;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/**
 * O fundidor ajudante leva o minério da boca da mina para os baús públicos — Regra 149, PROJECT_AUDIT.md T5.
 */
public class VillageHelpersGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_helpers", tickLimit = 40)
    public void theSmelterHaulsTheMineMouthToThePublicChests(TestContext context) {
        BlockPos mouth = new BlockPos(6, 1, 6);
        BlockPos mouthChest = mouth.east();
        BlockPos publicChest = new BlockPos(1, 1, 1);

        context.setBlockState(mouthChest, Blocks.CHEST);
        context.setBlockState(publicChest, Blocks.CHEST);

        UUID colonyId = UUID.randomUUID();
        Colony colony = Colony.create(colonyId, at(context, new BlockPos(4, 1, 4)));
        colony.measure(VillageBounds.aroundPiece(at(context, BlockPos.ORIGIN), at(context, new BlockPos(8, 4, 8)), 0));
        VillageColonyMod.COLONIES.register(colony);
        VillageColonyMod.MINES.restore(Mine.restore(colonyId, MineShaft.from(at(context, mouth), Side.EAST), 0));

        ChestDepositor.deposit(context.getWorld(), at(context, mouthChest), Items.RAW_IRON, 10);
        VillagerEntity smelter = context.spawnEntity(EntityType.VILLAGER, new BlockPos(2, 2, 2));

        try {
            VillageHelpers.smelterHelps(context.getWorld(), colony, smelter);

            int atTheMouth = ColonyChests.countIn(context.getWorld(), List.of(at(context, mouthChest)), Items.RAW_IRON);
            int inThePublic = ColonyChests.countIn(context.getWorld(), List.of(at(context, publicChest)), Items.RAW_IRON);

            context.assertTrue(atTheMouth == 0 && inThePublic == 10,
                    "o minério devia ir da boca ao baú público: boca " + atTheMouth + ", público " + inThePublic);
        } finally {
            VillageColonyMod.MINES.removeOfColony(colonyId);
            VillageColonyMod.COLONIES.remove(colonyId);
        }

        context.complete();
    }

    private static ColonyPos at(TestContext context, BlockPos relative) {
        return MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(relative));
    }
}
