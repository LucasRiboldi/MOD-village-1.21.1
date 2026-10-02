package com.villagecolony.gametest;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ActionTool;
import com.villagecolony.fabric.integration.BlockBreakTime;
import com.villagecolony.fabric.integration.ToolUpgrade;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * A ferramenta que ele tem, e a melhor do baú — Regras 2-e1 e 37-e1, pedido
 * do autor de 2026-10-02: <i>"o aldeão minera na velocidade da ferramenta que
 * ele possui (começa com ferro); se houver no baú dele uma ferramenta melhor
 * (mais rápida ou com encantamentos), ele troca a de ferro pela melhor"</i>.
 */
public final class BetterToolGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(2, 1, 2);

    private static ItemStack enchanted(TestContext context, RegistryKey<Enchantment> key, int level) {
        ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);

        pickaxe.addEnchantment(context.getWorld().getRegistryManager().get(RegistryKeys.ENCHANTMENT)
                .getEntry(key).orElseThrow(), level);

        return pickaxe;
    }

    /** Eficiência V soma 26 à picareta de ferro, como para o jogador. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "better_tool", tickLimit = 20)
    public void efficiencyMakesTheSamePickaxeFaster(TestContext context) {
        BlockState stone = Blocks.STONE.getDefaultState();
        ItemStack plain = new ItemStack(Items.IRON_PICKAXE);
        ItemStack fast = enchanted(context, Enchantments.EFFICIENCY, 5);

        context.assertTrue(ActionTool.speedOf(plain, stone) == 6.0f,
                "picareta de ferro na pedra devia valer 6, vale " + ActionTool.speedOf(plain, stone));
        context.assertTrue(ActionTool.speedOf(fast, stone) == 32.0f,
                "Eficiência V devia somar 26: vale " + ActionTool.speedOf(fast, stone));

        BlockPos at = context.getAbsolutePos(new BlockPos(4, 1, 4));
        context.setBlockState(new BlockPos(4, 1, 4), stone);

        VillagerEntity miner = context.spawnEntity(EntityType.VILLAGER, new BlockPos(5, 2, 5));

        miner.equipStack(EquipmentSlot.MAINHAND, plain);
        int slow = BlockBreakTime.ticksFor(context.getWorld(), at, stone, miner);

        miner.equipStack(EquipmentSlot.MAINHAND, fast);
        int quick = BlockBreakTime.ticksFor(context.getWorld(), at, stone, miner);

        miner.discard();

        context.assertTrue(quick < slow, "com Eficiência V ele quebrou em " + quick + " tiques, sem ela em " + slow);
        context.complete();
    }

    /** Na mesma velocidade, a encantada do baú vence a de ferro da mão — e fica. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "better_tool", tickLimit = 20)
    public void anEnchantedPickaxeOfTheSameSpeedReplacesThePlainOne(TestContext context) {
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        ChestBlockEntity chest = (ChestBlockEntity) context.getBlockEntity(CHEST);
        ItemStack unbreaking = enchanted(context, Enchantments.UNBREAKING, 3);

        chest.setStack(0, unbreaking.copy());

        ColonyPos at = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));
        ItemStack iron = new ItemStack(Items.IRON_PICKAXE);

        Optional<ItemStack> better = ToolUpgrade.betterThan(
                context.getWorld(), ProfessionType.MINER, iron, iron, at);

        context.assertTrue(better.isPresent() && ActionTool.enchantmentsOf(better.get()) == 1,
                "a picareta encantada do baú não foi escolhida no lugar da de ferro");
        context.assertTrue(ToolUpgrade.worthKeeping(ProfessionType.MINER, unbreaking, iron),
                "a encantada na mão seria trocada de volta pela de ferro, e destruída");
        context.assertTrue(ToolUpgrade.betterThan(context.getWorld(), ProfessionType.MINER, unbreaking, iron, at)
                        .isEmpty(),
                "com a encantada na mão, a mesma do baú não é melhora: trocaria a cada ciclo");
        context.complete();
    }

    /** A de diamante do baú é mais rápida que a de ferro: troca. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "better_tool", tickLimit = 20)
    public void aFasterPickaxeInTheChestReplacesIron(TestContext context) {
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        ChestBlockEntity chest = (ChestBlockEntity) context.getBlockEntity(CHEST);

        chest.setStack(0, new ItemStack(Items.DIAMOND_PICKAXE));

        ColonyPos at = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));
        ItemStack iron = new ItemStack(Items.IRON_PICKAXE);

        Optional<ItemStack> better = ToolUpgrade.betterThan(
                context.getWorld(), ProfessionType.MINER, iron, iron, at);

        context.assertTrue(better.isPresent() && better.get().isOf(Items.DIAMOND_PICKAXE),
                "a picareta de diamante do baú não substituiu a de ferro");
        context.complete();
    }
}
