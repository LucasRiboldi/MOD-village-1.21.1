package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A obra larga as últimas poucas peças sem apoio e fecha — playtest 0.3.31. */
class UnsupportedReleaseTest {

    private static final ResourceId HOUSE = ResourceId.vanilla("village/plains/houses/small_house");

    private static final ResourceId TORCH = ResourceId.vanilla("wall_torch");

    private static final ResourceId PLANKS = ResourceId.vanilla("oak_planks");

    private static ConstructionProject house(int torches, int planks) {
        List<BlueprintBlock> blocks = new ArrayList<>();

        for (int x = 0; x < torches; x++) {
            blocks.add(new BlueprintBlock(new ColonyPos(x, 1, 0), TORCH));
        }

        for (int x = 0; x < planks; x++) {
            blocks.add(new BlueprintBlock(new ColonyPos(x, 0, 0), PLANKS));
        }

        return ConstructionProject.plan(UUID.randomUUID(), Blueprint.of(HOUSE, blocks), new ColonyPos(10, 64, 10));
    }

    private static void deferUnsupported(ConstructionProject project, ResourceId block) {
        for (BlueprintBlock piece : project.remaining()) {
            if (piece.block().equals(block)) {
                project.defer(piece,
                        ConstructionOutcome.skipped(project.worldPositionOf(piece), SkipReason.UNSUPPORTED), "air");
            }
        }
    }

    private static void placeAll(ConstructionProject project, ResourceId block) {
        for (BlueprintBlock piece : project.remaining()) {
            if (piece.block().equals(block)) {
                project.markPlaced(piece);
            }
        }
    }

    @Test
    void theLastTorchesWithoutAWallAreLetGoAndTheHouseFinishes() {
        ConstructionProject project = house(2, 3);

        placeAll(project, PLANKS);
        deferUnsupported(project, TORCH);

        assertEquals(List.of(TORCH, TORCH), UnsupportedRelease.release(project));
        assertTrue(project.isFinished(), "a obra não fechou depois de largar as tochas");
    }

    @Test
    void aHouseWithAnotherPieceToPlaceKeepsItsTorches() {
        ConstructionProject project = house(2, 3);

        deferUnsupported(project, TORCH);

        assertTrue(UnsupportedRelease.release(project).isEmpty(), "largou as tochas com tábua por pôr");
        assertEquals(5, project.remainingCount());
    }

    @Test
    void aPieceWaitingForMaterialIsNotLetGo() {
        ConstructionProject project = house(1, 1);

        deferUnsupported(project, TORCH);
        project.deferForMaterial(project.remaining().stream()
                .filter(piece -> piece.block().equals(PLANKS)).findFirst().orElseThrow());

        assertTrue(UnsupportedRelease.release(project).isEmpty(), "largou a peça que espera material");
    }

    @Test
    void tooManyPiecesWithoutSupportAreNotLetGo() {
        ConstructionProject project = house(UnsupportedRelease.MOST + 1, 1);

        placeAll(project, PLANKS);
        deferUnsupported(project, TORCH);

        assertTrue(UnsupportedRelease.release(project).isEmpty(), "largou a casa a que falta muito");
        assertFalse(project.isFinished());
    }
}
