package com.villagecolony.fabric.integration;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** A terracota cede lugar a tijolos de pedra quando falta argila (Regra 352). */
public final class ConstructionPalette {

    private static final ResourceId STONE_BRICKS = ResourceId.vanilla("stone_bricks");

    private ConstructionPalette() {
    }

    /** A argila precisa estar nos baús ou exposta no alcance de coleta. */
    public static boolean hasClay(ServerWorld world, Colony colony) {
        return ClayNearby.around(world, colony)
                || ColonySupply.canProvide(world, colony.id(), colony.center(), Items.CLAY);
    }

    /** A paleta do catálogo de adiantamento, sem uma obra já colocada no mundo. */
    public static Blueprint forStock(ServerWorld world, Colony colony, Blueprint plan) {
        return apply(world, colony, plan, Optional.empty());
    }

    /** Preserva tanto a terracota quanto o substituto já assentado nas posições da planta. */
    public static Blueprint atSite(ServerWorld world, Colony colony, Blueprint plan, ColonyPos origin) {
        return apply(world, colony, plan, Optional.of(MinecraftTypeAdapter.toBlockPos(origin)));
    }

    /** Inclui as variantes vitrificadas, que têm o mesmo insumo. */
    public static boolean isTerracotta(Block block) {
        return block.getDefaultState().isIn(BlockTags.TERRACOTTA)
                || Registries.BLOCK.getId(block).getPath().endsWith("_glazed_terracotta");
    }

    private static Blueprint apply(
            ServerWorld world, Colony colony, Blueprint plan, Optional<BlockPos> origin) {
        List<ResourceId> terracotta = plan.materials().keySet().stream()
                .filter(id -> MinecraftTypeAdapter.toBlock(id).filter(ConstructionPalette::isTerracotta).isPresent())
                .toList();

        if (terracotta.isEmpty()) {
            return plan;
        }

        boolean hasClay = hasClay(world, colony);
        List<BlueprintBlock> blocks = new ArrayList<>(plan.blocks().size());

        for (BlueprintBlock piece : plan.blocks()) {
            if (!terracotta.contains(piece.block())) {
                blocks.add(piece);
                continue;
            }

            ResourceId material = origin.flatMap(at -> standingMaterial(world, at, piece))
                    .orElse(hasClay ? piece.block() : STONE_BRICKS);
            blocks.add(material.equals(piece.block()) ? piece : new BlueprintBlock(
                    piece.offset(), material, piece.furniture(), piece.facing(), piece.states()));
        }

        Blueprint adapted = Blueprint.of(plan.id(), blocks);
        return plan.hasStreetLayer() ? adapted.withStreetLayer(plan.streetLayer()) : adapted;
    }

    private static Optional<ResourceId> standingMaterial(ServerWorld world, BlockPos origin, BlueprintBlock piece) {
        BlockPos at = origin.add(piece.offset().x(), piece.offset().y(), piece.offset().z());
        if (!world.isChunkLoaded(at)) {
            return Optional.empty();
        }

        Block standing = world.getBlockState(at).getBlock();
        return standing == Blocks.STONE_BRICKS || isTerracotta(standing)
                ? Optional.of(MinecraftTypeAdapter.toResourceId(standing))
                : Optional.empty();
    }
}
