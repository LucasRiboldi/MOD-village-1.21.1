package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.WorkArea;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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

    static WorkArea margin(WorkArea area) {
        return new WorkArea(new ColonyPos(area.min().x() - 2, area.min().y() - 2, area.min().z() - 2),
                new ColonyPos(area.max().x() + 2, area.max().y() + 2, area.max().z() + 2));
    }

    static void inspect(ServerWorld world, UUID colonyId, WorkArea area) {
        inspect(world, colonyId, area, false);
    }

    /** Política explícita não altera proteções ou prova; somente admite plantas da lista fechada — MAE Fase 2. */
    static void inspectTerrain(ServerWorld world, UUID colonyId, WorkArea area) {
        inspect(world, colonyId, area, true);
    }

    static void inspectTerrain(ServerWorld world, UUID colonyId, WorkArea area, Set<BlockPos> readOnlyRoads) {
        inspect(world, colonyId, area, true, readOnlyRoads);
    }

    private static void inspect(ServerWorld world, UUID colonyId, WorkArea area, boolean lowVegetation) {
        inspect(world, colonyId, area, lowVegetation, Set.of());
    }

    private static void inspect(ServerWorld world, UUID colonyId, WorkArea area, boolean lowVegetation, Set<BlockPos> readOnlyRoads) {
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
            boolean roadColumn = readOnlyRoads.stream().anyMatch(road -> road.getX() == pos.getX() && road.getZ() == pos.getZ());
            boolean roadFloor = readOnlyRoads.contains(pos);
            if (!world.getWorldBorder().contains(pos) || state.hasBlockEntity()
                    || !state.getFluidState().isEmpty() || !(terrain(state) || (lowVegetation && vegetation(state))
                        || (roadFloor && MaeRoadExecution.accepts(state)))
                    || BlockProtection.isPlayerPlaced(state) || BlockProtection.isColonyBuilt(pos)
                    || BlockProtection.isVillageOriginal(world, pos)
                    || (!roadColumn && !BlockProtection.mayBreak(world, pos, state))
                    || mineCells.contains(MinecraftTypeAdapter.toColonyPos(pos))) {
                throw new IllegalStateException("protected_volume");
            }
            if (RoadIndex.isLaidByAColony(pos) && !roadColumn) {
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

    /** Somente materiais passivos nesta fundação; vegetação, fluidos e blocos ativos ficam bloqueados — ADR-044. */
    static boolean terrain(BlockState state) {
        return state.isAir() || state.isOf(Blocks.DIRT) || state.isOf(Blocks.GRASS_BLOCK)
                || state.isOf(Blocks.COARSE_DIRT) || state.isOf(Blocks.STONE)
                || state.isOf(Blocks.COBBLESTONE) || state.isOf(Blocks.SANDSTONE)
                || state.isOf(Blocks.RED_SANDSTONE) || state.isOf(Blocks.DIRT_PATH);
    }
}
