package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Completa de forma idempotente as camas físicas da vila vanilla observada. */
public final class VanillaBedChests {

    private VanillaBedChests() {
    }

    /**
     * A lista vem do mesmo cluster de POIs que qualificou a vila. Chamadas
     * posteriores reparam observações parciais e apenas reconhecem os baús
     * que já foram colocados corretamente.
     */
    public static void ensure(ServerWorld world, List<ColonyPos> observedBeds) {
        Set<BlockPos> seen = new HashSet<>();
        for (ColonyPos observed : observedBeds) {
            BlockPos bed = MinecraftTypeAdapter.toBlockPos(observed);
            if (!seen.add(bed) || BigHouseFoundation.containsHouseBlock(bed)) {
                continue;
            }
            Optional<BlockBox> piece = originalVillagePiece(world, bed);
            if (piece.isEmpty()) {
                log(ChestPlacer.Outcome.SKIPPED_NOT_A_COMPLETE_BED);
                continue;
            }
            log(ChestPlacer.placeForOriginalVillageBed(world, bed, piece.get()).outcome());
        }
    }

    /** Baús privados de cama não entram no estoque público da vila. */
    public static boolean isPrivateBedChest(ServerWorld world, BlockPos chest) {
        for (Direction direction : Direction.Type.HORIZONTAL) {
            Optional<BlockBox> piece = originalVillagePiece(world, chest.offset(direction));
            if (piece.isPresent()
                    && ChestPlacer.isCompliantVillageBedChest(world, chest, piece.get())) {
                return true;
            }
        }
        return false;
    }

    /** A peça vanilla que contém a posição, quando ela pertence a uma vila gerada. */
    static Optional<BlockBox> originalVillagePiece(ServerWorld world, BlockPos pos) {
        StructureStart village = world.getStructureAccessor()
                .getStructureContaining(pos, StructureTags.VILLAGE);
        if (village == null || village == StructureStart.DEFAULT || !village.hasChildren()) {
            return Optional.empty();
        }
        return village.getChildren().stream()
                .map(piece -> piece.getBoundingBox())
                .filter(box -> box.contains(pos))
                .findFirst();
    }

    private static void log(ChestPlacer.Outcome outcome) {
        if (outcome == ChestPlacer.Outcome.PLACED) {
            VillageColonyMod.LOGGER.info("VC_VILLAGE_BED_CHEST outcome=CREATED reason={}", outcome);
        } else if (outcome != ChestPlacer.Outcome.ALREADY_PRESENT) {
            VillageColonyMod.LOGGER.debug("VC_VILLAGE_BED_CHEST outcome=SKIPPED reason={}", outcome);
        }
    }
}
