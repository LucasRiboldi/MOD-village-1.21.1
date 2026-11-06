package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.WorkArea;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Inspeção conservadora do volume e margem, antes de qualquer escrita MAE — ADR-044. */
final class MaeProtection {
    private MaeProtection() { }

    /** O lote pedido serve? O que {@link MaeClearing} pode tirar não barra — Regra 200. */
    static void inspectClearing(ServerWorld world, UUID colonyId, WorkArea area) {
        if (!world.getServer().isOnThread()) {
            throw new IllegalStateException("not_server_thread");
        }
        if (!net.minecraft.world.World.OVERWORLD.equals(world.getRegistryKey())) {
            throw new IllegalStateException("wrong_dimension");
        }
        if (VillageColonyMod.COLONIES.find(colonyId).isEmpty()
                || area.min().y() < world.getBottomY() || area.max().y() >= world.getTopY()) {
            throw new IllegalStateException("unknown_colony_or_height");
        }
        Set<StructureStart> starts = new HashSet<>();
        for (int x = area.min().x() >> 4; x <= area.max().x() >> 4; x++) {
            for (int z = area.min().z() >> 4; z <= area.max().z() >> 4; z++) {
                var chunk = world.getChunkManager().getWorldChunk(x, z);
                if (chunk == null) {
                    throw new IllegalStateException("unknown_chunk");
                }
                starts.addAll(chunk.getStructureStarts().values());
                for (var references : chunk.getStructureReferences().entrySet()) {
                    for (long reference : references.getValue()) {
                        var pos = new ChunkPos(reference);
                        var source = world.getChunkManager().getWorldChunk(pos.x, pos.z);
                        if (source == null) {
                            throw new IllegalStateException("unknown_structure_reference");
                        }
                        StructureStart start = source.getStructureStart(references.getKey());
                        if (start == null || !start.hasChildren()) {
                            throw new IllegalStateException("invalid_structure_reference");
                        }
                        starts.add(start);
                    }
                }
            }
        }
        for (StructureStart start : starts) {
            if (start.hasChildren()) {
                var box = start.getBoundingBox();
                WorkArea occupied = new WorkArea(new ColonyPos(box.getMinX(), box.getMinY(), box.getMinZ()),
                        new ColonyPos(box.getMaxX(), box.getMaxY(), box.getMaxZ()));
                if (area.intersects(occupied)) {
                    throw new IllegalStateException("original_structure");
                }
            }
        }
        for (var other : VillageColonyMod.COLONIES.all()) {
            if (other.id().equals(colonyId)) {
                continue;
            }
            var bounds = other.bounds().orElseGet(() ->
                    com.villagecolony.core.colony.model.VillageBounds.typicalAround(other.center(),
                            world.getBottomY(), world.getTopY() - 1));
            if (area.min().x() <= bounds.maxX() && area.max().x() >= bounds.minX()
                    && area.min().z() <= bounds.maxZ() && area.max().z() >= bounds.minZ()) {
                throw new IllegalStateException("other_colony");
            }
        }
        // A geometria planejada é reconstruída uma vez por inspeção, nunca por célula — ADR-044.
        Set<ColonyPos> mineCells = new HashSet<>();
        for (var mine : VillageColonyMod.MINES.all()) {
            mineCells.addAll(mine.plannedCells());
        }
        for (BlockPos pos : BlockPos.iterate(area.min().x(), area.min().y(), area.min().z(),
                area.max().x(), area.max().y(), area.max().z())) {
            BlockState state = world.getBlockState(pos);
            if (MaeClearing.isGrowth(world, pos, state)) continue;
            if (!world.getWorldBorder().contains(pos) || state.hasBlockEntity()
                    || !state.getFluidState().isEmpty() || !(terrain(state) || vegetation(state))
                    || BlockProtection.isPlayerPlaced(state) || BlockProtection.isColonyBuilt(pos)
                    || BlockProtection.isVillageOriginal(world, pos)
                    || !BlockProtection.mayBreak(world, pos, state)
                    || mineCells.contains(MinecraftTypeAdapter.toColonyPos(pos))) {
                throw new IllegalStateException("protected_volume");
            }
            if (RoadIndex.isLaidByAColony(pos)) {
                throw new IllegalStateException("reserved_road");
            }
        }
    }

    /** Lista aprovada; tags genéricas não autorizam árvore, cultivo ou planta de duas metades — MAE Fase 2. */
    static boolean vegetation(BlockState state) {
        return state.isOf(Blocks.SHORT_GRASS) || state.isOf(Blocks.FERN) || state.isOf(Blocks.DANDELION)
                || state.isOf(Blocks.POPPY) || state.isOf(Blocks.BLUE_ORCHID) || state.isOf(Blocks.ALLIUM)
                || state.isOf(Blocks.AZURE_BLUET) || state.isOf(Blocks.RED_TULIP) || state.isOf(Blocks.ORANGE_TULIP)
                || state.isOf(Blocks.WHITE_TULIP) || state.isOf(Blocks.PINK_TULIP) || state.isOf(Blocks.OXEYE_DAISY)
                || state.isOf(Blocks.CORNFLOWER) || state.isOf(Blocks.LILY_OF_THE_VALLEY);
    }

    /** Chão natural do bioma e minério, além do caminho (Regra 201); vegetação, fluidos e blocos ativos ficam de fora — ADR-044. */
    static boolean terrain(BlockState state) {
        return state.isAir() || state.isIn(BlockTags.DIRT) || state.isIn(BlockTags.BASE_STONE_OVERWORLD)
                || state.isIn(BlockTags.SAND) || state.isIn(BlockTags.TERRACOTTA)
                || state.isOf(Blocks.GRAVEL) || state.isOf(Blocks.CLAY) || state.isOf(Blocks.SNOW_BLOCK)
                || state.isOf(Blocks.COBBLESTONE) || state.isOf(Blocks.SANDSTONE)
                || state.isOf(Blocks.RED_SANDSTONE) || state.isOf(Blocks.DIRT_PATH)
                || state.isIn(BlockTags.COAL_ORES) || state.isIn(BlockTags.IRON_ORES) || state.isIn(BlockTags.COPPER_ORES)
                || state.isIn(BlockTags.GOLD_ORES) || state.isIn(BlockTags.REDSTONE_ORES) || state.isIn(BlockTags.LAPIS_ORES)
                || state.isIn(BlockTags.DIAMOND_ORES) || state.isIn(BlockTags.EMERALD_ORES);
    }
}
