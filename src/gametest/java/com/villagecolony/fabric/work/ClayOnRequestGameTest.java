package com.villagecolony.fabric.work;

import com.villagecolony.core.coordination.SupplyException;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.Supplies;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/**
 * Regra 301: a argila e a bola de argila que o fundidor pede aparecem no baú dele na hora do pedido, quando nenhum baú
 * da vila tem — sem esperar faltas nem buscas. O que não é argila segue o caminho de sempre.
 */
public class ClayOnRequestGameTest implements FabricGameTest {

    private static final BlockPos OWN = new BlockPos(1, 1, 1);
    private static final BlockPos OTHER = new BlockPos(5, 1, 5);

    /** Sem argila em baú nenhum, ela aparece no baú do fundidor no primeiro pedido, declarada no VC_SUPPLY. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "clay_on_request", tickLimit = 5)
    public void clayAppearsAtTheFirstRequest(TestContext context) {
        context.setBlockState(OWN, Blocks.CHEST);
        context.setBlockState(OTHER, Blocks.CHEST);
        ColonyPos own = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(OWN));
        ColonyPos other = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(OTHER));
        UUID colony = UUID.randomUUID();
        long before = Supplies.sessionCount(colony, SupplyException.CLAY_ON_REQUEST);

        context.assertTrue(ClayOnRequest.stockIfMissing(context.getWorld(), colony, List.of(own, other), own,
                List.of(Items.CLAY), true), "Regra 301: a argila não apareceu no primeiro pedido");
        context.assertTrue(ColonyChests.countIn(context.getWorld(), List.of(own), Items.CLAY) == ClayOnRequest.SURGE,
                "Regra 301: a argila não está no baú do fundidor");
        context.assertTrue(Supplies.sessionCount(colony, SupplyException.CLAY_ON_REQUEST) > before,
                "Regra 301: a argila surgida não foi declarada no VC_SUPPLY");
        context.assertTrue(ClayOnRequest.stockIfMissing(context.getWorld(), colony, List.of(own, other), own,
                List.of(Items.CLAY_BALL), true), "Regra 301: a bola de argila não apareceu no pedido");
        context.complete();
    }

    /** Com argila num baú da vila, nada surge; o que não é argila (areia) também não. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "clay_on_request", tickLimit = 5)
    public void nothingAppearsWithClayInAChestOrForSand(TestContext context) {
        context.setBlockState(OWN, Blocks.CHEST);
        context.setBlockState(OTHER, Blocks.CHEST);
        ((Inventory) context.getWorld().getBlockEntity(context.getAbsolutePos(OTHER))).setStack(0, new ItemStack(Items.CLAY, 4));
        ColonyPos own = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(OWN));
        ColonyPos other = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(OTHER));
        UUID colony = UUID.randomUUID();

        context.assertFalse(ClayOnRequest.stockIfMissing(context.getWorld(), colony, List.of(own, other), own,
                List.of(Items.CLAY), true), "a argila surgiu com argila num baú da vila");
        context.assertFalse(ClayOnRequest.stockIfMissing(context.getWorld(), colony, List.of(own, other), own,
                List.of(Items.SAND), true), "a areia surgiu como se fosse argila");
        context.complete();
    }

    /** Regra 308: com 64 bolas de argila guardadas, a argila não surge: o teto é da argila guardada, das duas formas. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "clay_on_request_cap", tickLimit = 5)
    public void clayStopsAppearingAtTheStoredCeiling(TestContext context) {
        context.setBlockState(OWN, Blocks.CHEST);
        context.setBlockState(OTHER, Blocks.CHEST);
        ((Inventory) context.getWorld().getBlockEntity(context.getAbsolutePos(OTHER)))
                .setStack(0, new ItemStack(Items.CLAY_BALL, 64));
        ColonyPos own = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(OWN));
        ColonyPos other = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(OTHER));

        context.assertFalse(ClayOnRequest.stockIfMissing(context.getWorld(), UUID.randomUUID(), List.of(own, other), own,
                List.of(Items.CLAY), true), "Regra 308: a argila surgiu com 64 bolas de argila guardadas");
        context.complete();
    }
}
