package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import java.util.LinkedHashMap;
import java.util.Map;

/** Explicita todas as partes previstas, sem escrita ou efeitos fora da caixa — ADR-046. */
final class MaeStructurePieces {
    private MaeStructurePieces() { }

    static Map<BlockPos, BlockState> planned(ConstructionProject project) {
        var lot = Building.of(project);
        var origin = MinecraftTypeAdapter.toBlockPos(project.origin());
        var pieces = new LinkedHashMap<BlockPos, BlockState>();
        for (var piece : project.blueprint().blocks()) {
            var at = origin.add(piece.offset().x(), piece.offset().y(), piece.offset().z());
            var state = PaletteStates.planned(piece);
            if (state.getBlock() instanceof DoorBlock || state.getBlock() instanceof TallPlantBlock) {
                if (state.get(Properties.DOUBLE_BLOCK_HALF) != DoubleBlockHalf.LOWER)
                    throw new IllegalArgumentException("orphan_structure_part");
                put(pieces, lot, at.up(), state.with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
            } else if (state.getBlock() instanceof BedBlock) {
                if (state.get(Properties.BED_PART) != BedPart.FOOT)
                    throw new IllegalArgumentException("orphan_structure_part");
                put(pieces, lot, at.offset(state.get(Properties.HORIZONTAL_FACING)), state.with(Properties.BED_PART, BedPart.HEAD));
            }
            put(pieces, lot, at, state);
        }
        return Map.copyOf(pieces);
    }

    private static void put(Map<BlockPos, BlockState> pieces, Building lot, BlockPos at, BlockState state) {
        if (!lot.contains(MinecraftTypeAdapter.toColonyPos(at)))
            throw new IllegalArgumentException("structure_effect_outside_building", new IllegalStateException(
                    at + " " + state + " bounds=" + lot.min() + ".." + lot.max()));
        if (pieces.putIfAbsent(at.toImmutable(), state) != null)
            throw new IllegalArgumentException("overlapping_structure_effect", new IllegalStateException(
                    at + " " + pieces.get(at) + " / " + state));
    }
}
