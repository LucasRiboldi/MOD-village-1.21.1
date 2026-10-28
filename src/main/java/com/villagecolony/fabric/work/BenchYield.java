package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.fabric.integration.BenchPresence;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.ProfessionType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * O que as bancadas fazem render — passivas da Regra 132 (autor, 2026-10-27), valendo só com a bancada do
 * ofício na vila:
 * <ul>
 *   <li><b>mineiro:</b> minério de ferro, cobre, carvão, ouro, lápis e redstone rende o dobro; diamante,
 *       esmeralda, ametista e quartzo rendem um a mais; 1 em {@link #TOOL_ODDS} o minério de ferro, ouro e
 *       diamante solta também uma ferramenta do material;</li>
 *   <li><b>pedreiro:</b> a cada {@link #STONE_EVERY} pedregulhos minerados, um a mais;</li>
 *   <li><b>pastor:</b> o abate rende o dobro de carne e couro.</li>
 * </ul>
 */
public final class BenchYield {

    static {
        ServerMemory.register(BenchYield.class, BenchYield::clearAll);
    }

    /** Uma ferramenta em cada tantos minérios de ferro, ouro e diamante. */
    static final int TOOL_ODDS = 100;

    /** Um pedregulho a mais a cada tantos. */
    static final int STONE_EVERY = 4;

    static final Set<Item> MEAT_AND_LEATHER = Set.of(Items.BEEF, Items.PORKCHOP, Items.MUTTON, Items.CHICKEN,
            Items.RABBIT, Items.LEATHER, Items.RABBIT_HIDE);

    private static final Map<UUID, Integer> STONES = new HashMap<>();

    private BenchYield() {
    }

    /** Ajusta o que o mineiro tirou deste bloco, conforme as bancadas da vila. */
    static void mined(ServerWorld world, UUID colonyId, BlockState state, List<ItemStack> drops) {
        VillageColonyMod.COLONIES.find(colonyId).ifPresent(colony -> mined(world, colony, state, drops));
    }

    static void mined(ServerWorld world, Colony colony, BlockState state, List<ItemStack> drops) {
        if (drops.isEmpty()) {
            return;
        }

        if (BenchPresence.has(world, colony, ProfessionType.MINER)) {
            if (isDoubled(state)) {
                drops.forEach(stack -> stack.setCount(stack.getCount() * 2));
            } else if (isGem(state)) {
                drops.getFirst().increment(1);
            }

            List<Item> tools = toolsOf(state);

            if (!tools.isEmpty() && world.getRandom().nextInt(TOOL_ODDS) == 0) {
                drops.add(new ItemStack(tools.get(world.getRandom().nextInt(tools.size()))));
            }
        }

        if (state.isOf(Blocks.COBBLESTONE) || state.isOf(Blocks.STONE)) {
            if (BenchPresence.has(world, colony, ProfessionType.MASON)
                    && STONES.merge(colony.id(), 1, Integer::sum) % STONE_EVERY == 0) {
                drops.add(new ItemStack(Items.COBBLESTONE));
            }
        }
    }

    /** Quanto o abate deposita deste item: o dobro de carne e couro com a bancada do pastor. */
    static int butchered(ServerWorld world, Colony colony, Item item, int count) {
        return MEAT_AND_LEATHER.contains(item) && BenchPresence.has(world, colony, ProfessionType.SHEPHERD)
                ? count * 2 : count;
    }

    static boolean isDoubled(BlockState state) {
        return state.isIn(BlockTags.IRON_ORES) || state.isIn(BlockTags.COPPER_ORES) || state.isIn(BlockTags.COAL_ORES)
                || state.isIn(BlockTags.GOLD_ORES) || state.isIn(BlockTags.LAPIS_ORES)
                || state.isIn(BlockTags.REDSTONE_ORES);
    }

    static boolean isGem(BlockState state) {
        return state.isIn(BlockTags.DIAMOND_ORES) || state.isIn(BlockTags.EMERALD_ORES)
                || state.isOf(Blocks.NETHER_QUARTZ_ORE) || state.isOf(Blocks.AMETHYST_CLUSTER);
    }

    /** As ferramentas do material deste minério, ou nenhuma. */
    static List<Item> toolsOf(BlockState state) {
        if (state.isIn(BlockTags.IRON_ORES)) {
            return List.of(Items.IRON_PICKAXE, Items.IRON_AXE, Items.IRON_SHOVEL, Items.IRON_HOE, Items.IRON_SWORD);
        }

        if (state.isIn(BlockTags.GOLD_ORES)) {
            return List.of(Items.GOLDEN_PICKAXE, Items.GOLDEN_AXE, Items.GOLDEN_SHOVEL, Items.GOLDEN_HOE,
                    Items.GOLDEN_SWORD);
        }

        if (state.isIn(BlockTags.DIAMOND_ORES)) {
            return List.of(Items.DIAMOND_PICKAXE, Items.DIAMOND_AXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE,
                    Items.DIAMOND_SWORD);
        }

        return List.of();
    }

    static void clearAll() {
        STONES.clear();
    }
}
