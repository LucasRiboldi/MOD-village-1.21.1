package com.villagecolony.fabric.integration;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Fertilizable;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldEvents;
import net.minecraft.world.chunk.WorldChunk;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Plantas, gelo e neve que as casas da vila pedem e ninguém colhia — regra do autor,
 * 2026-10-11: o fazendeiro colhe flores, grama, samambaia, bambu e cana no terreno
 * em volta da vila e replanta o que colhe; o fundidor recolhe gelo e neve, como a
 * areia ({@code StructureCoverageGameTest}: eram itens "do mundo", sem dono).
 *
 * <p><b>Replantio.</b> Bambu e cana são cortados do segundo bloco para cima, e a base
 * volta a crescer (como o cacto). Flor, grama e samambaia saem inteiras, e o
 * fazendeiro põe uma farinha de osso na grama de onde tirou — o jogo faz nascer
 * grama e flor em volta. Sem farinha de osso nos baús, não há replantio.
 */
public final class PlantPatch {

    /** Do fazendeiro (colheita de solo). */
    public static final Set<ResourceType> PLANTS = EnumSet.of(
            ResourceType.POPPY, ResourceType.DANDELION, ResourceType.CORNFLOWER, ResourceType.RED_TULIP,
            ResourceType.OXEYE_DAISY, ResourceType.SHORT_GRASS, ResourceType.FERN, ResourceType.BAMBOO,
            ResourceType.SUGAR_CANE);

    /** Do fundidor (coleta de superfície). */
    public static final Set<ResourceType> COLD = EnumSet.of(ResourceType.ICE, ResourceType.SNOWBALL);

    private static final Map<ResourceType, List<Block>> BLOCKS = Map.ofEntries(
            Map.entry(ResourceType.POPPY, List.of(Blocks.POPPY)),
            Map.entry(ResourceType.DANDELION, List.of(Blocks.DANDELION)),
            Map.entry(ResourceType.CORNFLOWER, List.of(Blocks.CORNFLOWER)),
            Map.entry(ResourceType.RED_TULIP, List.of(Blocks.RED_TULIP)),
            Map.entry(ResourceType.OXEYE_DAISY, List.of(Blocks.OXEYE_DAISY)),
            Map.entry(ResourceType.SHORT_GRASS, List.of(Blocks.SHORT_GRASS)),
            Map.entry(ResourceType.FERN, List.of(Blocks.FERN)),
            Map.entry(ResourceType.BAMBOO, List.of(Blocks.BAMBOO)),
            Map.entry(ResourceType.SUGAR_CANE, List.of(Blocks.SUGAR_CANE)),
            Map.entry(ResourceType.ICE, List.of(Blocks.ICE)),
            Map.entry(ResourceType.SNOWBALL, List.of(Blocks.SNOW, Blocks.SNOW_BLOCK)));

    /** Os que crescem em coluna: a base fica. */
    private static final Set<ResourceType> TALL = EnumSet.of(ResourceType.BAMBOO, ResourceType.SUGAR_CANE);

    /** Os que saem com tesoura. */
    private static final Set<ResourceType> SHEARED = EnumSet.of(ResourceType.SHORT_GRASS, ResourceType.FERN);

    private static final int WINDOW_UP = 6;

    private static final int WINDOW_DOWN = 12;

    private PlantPatch() {
    }

    public static boolean handles(ResourceType resource) {
        return BLOCKS.containsKey(resource);
    }

    /** O bloco deste recurso nesta coluna, perto da altura da vila; chunk só se carregado. */
    public static Optional<BlockPos> in(ServerWorld world, BlockPos column, int aroundY, ResourceType resource) {
        List<Block> wanted = BLOCKS.get(resource);
        WorldChunk chunk = world.getChunkManager().getWorldChunk(column.getX() >> 4, column.getZ() >> 4);

        if (wanted == null || chunk == null) {
            return Optional.empty();
        }

        for (int y = aroundY + WINDOW_UP; y >= aroundY - WINDOW_DOWN; y--) {
            BlockPos at = new BlockPos(column.getX(), y, column.getZ());
            BlockState state = chunk.getBlockState(at);

            if (wanted.stream().noneMatch(state::isOf)) {
                continue;
            }

            // Bambu e cana: o topo, e só com a base embaixo — ela volta a crescer.
            if (TALL.contains(resource) && !chunk.getBlockState(at.down()).isOf(state.getBlock())) {
                return Optional.empty();
            }

            return BlockProtection.mayBreak(world, at, state) ? Optional.of(at) : Optional.empty();
        }

        return Optional.empty();
    }

    /** A ferramenta da colheita: tesoura na grama e na samambaia, toque suave no gelo. */
    public static Optional<ItemStack> toolFor(ServerWorld world, ResourceType resource) {
        if (SHEARED.contains(resource)) {
            return Optional.of(new ItemStack(Items.SHEARS));
        }

        if (resource == ResourceType.ICE) {
            ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
            var silkTouch = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT)
                    .getEntry(Enchantments.SILK_TOUCH).orElseThrow();

            EnchantmentHelper.apply(pickaxe, enchantments -> enchantments.set(silkTouch, 1));

            return Optional.of(pickaxe);
        }

        if (resource == ResourceType.SNOWBALL) {
            return Optional.of(new ItemStack(Items.IRON_SHOVEL));
        }

        return Optional.empty();
    }

    /**
     * Depois de tirar flor, grama ou samambaia: uma farinha de osso dos baús na grama
     * de onde saiu, e o jogo faz nascer grama e flor em volta.
     *
     * @return se replantou
     */
    public static boolean replant(ServerWorld world, UUID colonyId, ColonyPos near, BlockPos picked, ResourceType resource) {
        if (!PLANTS.contains(resource) || TALL.contains(resource)) {
            return false;
        }

        BlockPos ground = picked.down();
        BlockState state = world.getBlockState(ground);
        List<ColonyPos> chests = ColonyChests.nearestFirst(world, colonyId, near);

        if (!state.isOf(Blocks.GRASS_BLOCK) || !(state.getBlock() instanceof Fertilizable grass)
                || !grass.isFertilizable(world, ground, state)
                || ColonyChests.withdraw(world, chests, Items.BONE_MEAL, 1) == 0) {
            return false;
        }

        grass.grow(world, world.getRandom(), ground, state);
        world.syncWorldEvent(WorldEvents.BONE_MEAL_USED, ground, 15);

        return true;
    }
}
