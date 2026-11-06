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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Explicita todas as partes previstas, sem escrita ou efeitos fora da caixa — ADR-046. */
final class MaeStructurePieces {
    private MaeStructurePieces() { }

    /** Uma peça e todas as suas partes; a primeira é a principal, que define o material — ADR-046. */
    record Piece(List<Map.Entry<BlockPos, BlockState>> parts) {
        Piece {
            parts = List.copyOf(parts);
        }

        BlockState main() {
            return parts.getFirst().getValue();
        }

        /** A água com as vizinhas dela, numa escrita só — Regra 203. */
        boolean tank() {
            return main().isOf(net.minecraft.block.Blocks.WATER) && parts.size() > 1;
        }
    }

    static Map<BlockPos, BlockState> planned(ConstructionProject project) {
        var pieces = new LinkedHashMap<BlockPos, BlockState>();
        for (var piece : ordered(project)) {
            piece.parts().forEach(part -> pieces.put(part.getKey(), part.getValue()));
        }
        return Map.copyOf(pieces);
    }

    /** Ordem estável de baixo para cima, o tanque primeiro na camada, depois x e z; o cursor do structure_v3 depende dela — ADR-046 P3. */
    static List<Piece> ordered(ConstructionProject project) {
        var lot = Building.of(project);
        var origin = MinecraftTypeAdapter.toBlockPos(project.origin());
        var taken = new LinkedHashMap<BlockPos, BlockState>();
        var result = new ArrayList<Piece>();
        for (var piece : project.blueprint().blocks()) {
            var at = origin.add(piece.offset().x(), piece.offset().y(), piece.offset().z());
            var state = PaletteStates.planned(piece);
            var parts = new ArrayList<Map.Entry<BlockPos, BlockState>>();
            if (state.getBlock() instanceof DoorBlock || state.getBlock() instanceof TallPlantBlock) {
                if (state.get(Properties.DOUBLE_BLOCK_HALF) != DoubleBlockHalf.LOWER)
                    throw new IllegalArgumentException("orphan_structure_part");
                var upper = state.with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
                put(taken, lot, at.up(), upper);
                parts.add(Map.entry(at.up().toImmutable(), upper));
            } else if (state.getBlock() instanceof BedBlock) {
                if (state.get(Properties.BED_PART) != BedPart.FOOT)
                    throw new IllegalArgumentException("orphan_structure_part");
                var headAt = at.offset(state.get(Properties.HORIZONTAL_FACING));
                var head = state.with(Properties.BED_PART, BedPart.HEAD);
                put(taken, lot, headAt, head);
                parts.add(Map.entry(headAt.toImmutable(), head));
            }
            put(taken, lot, at, state);
            parts.addFirst(Map.entry(at.toImmutable(), state));
            result.add(new Piece(parts));
        }
        var pieces = withClosedTanks(result);
        var water = new java.util.HashSet<BlockPos>();
        for (var piece : pieces) if (piece.main().isOf(net.minecraft.block.Blocks.WATER)) water.add(piece.parts().getFirst().getKey());
        pieces.sort(Comparator.comparingInt((Piece piece) -> piece.parts().getFirst().getKey().getY())
                .thenComparingInt(piece -> water.contains(piece.parts().getFirst().getKey()) ? 0
                        : net.minecraft.util.math.Direction.Type.HORIZONTAL.stream().anyMatch(side ->
                                water.contains(piece.parts().getFirst().getKey().offset(side))) ? 1 : 2)
                .thenComparingInt(piece -> piece.parts().getFirst().getKey().getX())
                .thenComparingInt(piece -> piece.parts().getFirst().getKey().getZ()));
        return List.copyOf(pieces);
    }

    /**
     * A água leva junto as vizinhas de uma peça só, na mesma camada: nenhum atraso da obra deixa a água escorrer, e o
     * solo arado já nasce úmido ao lado dela — Regra 203, ADR-046.
     */
    private static List<Piece> withClosedTanks(List<Piece> pieces) {
        var single = new java.util.HashMap<BlockPos, Piece>();
        for (var piece : pieces) {
            if (piece.parts().size() == 1 && !piece.main().isOf(net.minecraft.block.Blocks.WATER)) {
                single.put(piece.parts().getFirst().getKey(), piece);
            }
        }
        var absorbed = new java.util.HashSet<Piece>();
        var tanks = new ArrayList<Piece>();
        for (var piece : pieces) {
            if (!piece.main().isOf(net.minecraft.block.Blocks.WATER)) continue;
            var parts = new ArrayList<>(piece.parts());
            for (var side : net.minecraft.util.math.Direction.Type.HORIZONTAL) {
                var neighbour = single.get(piece.parts().getFirst().getKey().offset(side));
                if (neighbour != null && absorbed.add(neighbour)) parts.add(neighbour.parts().getFirst());
            }
            tanks.add(new Piece(parts));
        }
        var result = new ArrayList<>(tanks);
        for (var piece : pieces) {
            if (!piece.main().isOf(net.minecraft.block.Blocks.WATER) && !absorbed.contains(piece)) result.add(piece);
        }
        return result;
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
