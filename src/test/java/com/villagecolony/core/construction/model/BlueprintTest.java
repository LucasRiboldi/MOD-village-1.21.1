package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.Side;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * O projeto de construção — TASK-030.
 *
 * <p>Uma lista de posições relativas e o bloco de cada uma. Onde a obra
 * será erguida não pertence a ele.
 */
class BlueprintTest {

    private static final ResourceId HOUSE = ResourceId.vanilla("village/plains/houses/small_house");

    private static final ResourceId PLANKS = ResourceId.vanilla("oak_planks");

    private static final ResourceId COBBLE = ResourceId.vanilla("cobblestone");

    @Test
    void legacyBlueprintHasNoAccessCandidates() {
        Blueprint blueprint = Blueprint.of(HOUSE, List.of(block(0, 0, 0, COBBLE)));

        assertEquals(List.of(), blueprint.accesses());
    }

    @Test
    void accessCandidatesAreImmutableAndPreserveMaterials() {
        var opening = new ArrayList<>(List.of(new ColonyPos(0, 1, 0)));
        var access = new BlueprintAccess(BlueprintAccess.Kind.DOOR, opening, Side.NORTH);
        var candidates = new ArrayList<>(List.of(access));
        Blueprint original = Blueprint.of(HOUSE, List.of(block(0, 0, 0, COBBLE)));
        Blueprint blueprint = original.withAccesses(candidates);
        opening.clear();
        candidates.clear();

        assertEquals(List.of(new ColonyPos(0, 1, 0)), access.opening());
        assertEquals(List.of(access), blueprint.accesses());
        assertEquals(List.of(), original.accesses());
        assertEquals(original.materials(), blueprint.materials());
        assertEquals(original.blocks(), blueprint.blocks());
        assertThrows(UnsupportedOperationException.class, () -> access.opening().clear());
        assertThrows(UnsupportedOperationException.class, () -> blueprint.accesses().clear());
    }

    @Test
    void streetLayerAndAccessMetadataCanBeSetInEitherOrder() {
        var access = new BlueprintAccess(BlueprintAccess.Kind.OPENING,
                List.of(new ColonyPos(0, 2, 0)), Side.WEST);
        Blueprint blueprint = Blueprint.of(HOUSE, List.of(block(0, 0, 0, COBBLE)));

        assertEquals(List.of(access), blueprint.withAccesses(List.of(access)).withStreetLayer(1).accesses());
        assertEquals(1, blueprint.withStreetLayer(1).withAccesses(List.of(access)).streetLayer());
    }

    @Test
    void accessRequiresAnOpeningAndCompleteNeutralMetadata() {
        var opening = List.of(new ColonyPos(0, 1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new BlueprintAccess(BlueprintAccess.Kind.DOOR, List.of(), Side.NORTH));
        assertThrows(NullPointerException.class,
                () -> new BlueprintAccess(null, opening, Side.NORTH));
        assertThrows(NullPointerException.class,
                () -> new BlueprintAccess(BlueprintAccess.Kind.DOOR, opening, null));
        assertThrows(NullPointerException.class,
                () -> new BlueprintAccess(BlueprintAccess.Kind.DOOR, null, Side.NORTH));
        assertThrows(NullPointerException.class,
                () -> new BlueprintAccess(BlueprintAccess.Kind.DOOR,
                        java.util.Arrays.asList((ColonyPos) null), Side.NORTH));
    }

    private static BlueprintBlock block(int x, int y, int z, ResourceId what) {
        return new BlueprintBlock(new ColonyPos(x, y, z), what);
    }

    @Test
    void templateBoundsKeepPaddingAndPivotThroughAllRotations() {
        var access = new BlueprintAccess(BlueprintAccess.Kind.OPENING, List.of(new ColonyPos(0, 1, 3)), Side.WEST);
        var base = Blueprint.of(HOUSE, List.of(block(1, 0, 2, COBBLE), block(8, 5, 11, PLANKS)));
        var bounded = base.withTemplateBounds(new ColonyPos(9, 6, 12)).withStreetLayer(0).withAccesses(List.of(access));
        assertEquals(new ColonyPos(8, 6, 10), base.size());
        assertEquals(base.materials(), bounded.materials());
        for (int turn = 0; turn < 4; turn++) {
            var rotated = bounded.rotated(turn);
            assertEquals(turn % 2 == 0 ? new ColonyPos(9, 6, 12) : new ColonyPos(12, 6, 9), rotated.size());
            assertEquals(bounded.blocks(), rotated.rotated(4 - turn).blocks());
            assertEquals(bounded.accesses(), rotated.rotated(4 - turn).accesses());
            assertEquals(0, rotated.streetLayer());
        }
    }

    @Test
    void templateBoundsRefusePiecesOutsideDeclaredDimensions() {
        var plan = Blueprint.of(HOUSE, List.of(block(1, 0, 2, COBBLE)));
        for (var invalid : List.of(new ColonyPos(0, 1, 3), new ColonyPos(1, 1, 3),
                new ColonyPos(2, 1, 2), new ColonyPos(2, -1, 3))) {
            assertThrows(IllegalArgumentException.class, () -> plan.withTemplateBounds(invalid));
        }
        assertThrows(IllegalArgumentException.class, () -> Blueprint.of(HOUSE, List.of(block(-1, 0, 0, COBBLE)))
                .withTemplateBounds(new ColonyPos(2, 2, 2)));
    }

    @Test
    void aBlueprintKeepsItsBlocksInOrder() {
        Blueprint blueprint = Blueprint.of(HOUSE, List.of(
                block(0, 0, 0, COBBLE),
                block(1, 0, 0, PLANKS)));

        assertEquals(2, blueprint.blockCount());
        assertEquals(COBBLE, blueprint.blocks().get(0).block());
        assertEquals(PLANKS, blueprint.blocks().get(1).block());
    }

    /**
     * Um projeto sem bloco algum é leitura de estrutura que falhou.
     *
     * <p>Deixá-lo passar produziria uma obra que termina antes de
     * começar, e a colônia diria que construiu.
     */
    @Test
    void anEmptyBlueprintIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> Blueprint.of(HOUSE, List.of()));
    }

    @Test
    void theMaterialListCountsEachBlock() {
        Blueprint blueprint = Blueprint.of(HOUSE, List.of(
                block(0, 0, 0, PLANKS),
                block(1, 0, 0, PLANKS),
                block(2, 0, 0, COBBLE)));

        assertEquals(Map.of(PLANKS, 2, COBBLE, 1), blueprint.materials());
    }

    @Test
    void oneBlockIsOneBlockWide() {
        Blueprint blueprint = Blueprint.of(HOUSE, List.of(block(0, 0, 0, PLANKS)));

        assertEquals(new ColonyPos(1, 1, 1), blueprint.size());
    }

    @Test
    void theSizeIsTheBoxAroundEverything() {
        Blueprint blueprint = Blueprint.of(HOUSE, List.of(
                block(0, 0, 0, PLANKS),
                block(4, 2, 6, PLANKS)));

        assertEquals(new ColonyPos(5, 3, 7), blueprint.size());
    }

    /** Estrutura lida do jogo pode vir com posição negativa. */
    @Test
    void theSizeSurvivesNegativeOffsets() {
        Blueprint blueprint = Blueprint.of(HOUSE, List.of(
                block(-2, 0, -2, PLANKS),
                block(2, 0, 2, PLANKS)));

        assertEquals(new ColonyPos(5, 1, 5), blueprint.size());
    }

    /** Duas obras podem correr juntas, e nenhuma altera o projeto da outra. */
    @Test
    void aBlueprintCannotBeChangedFromOutside() {
        Blueprint blueprint = Blueprint.of(HOUSE, List.of(block(0, 0, 0, PLANKS)));

        assertThrows(UnsupportedOperationException.class,
                () -> blueprint.blocks().add(block(1, 0, 0, COBBLE)));

        assertThrows(UnsupportedOperationException.class,
                () -> blueprint.materials().put(COBBLE, 99));
    }
}
