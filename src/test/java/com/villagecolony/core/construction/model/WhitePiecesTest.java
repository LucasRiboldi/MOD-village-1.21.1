package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Cama, lã, tapete e estandarte coloridos saem brancos — item 5 das decisões de 27-10. */
class WhitePiecesTest {

    @Test
    void colouredPiecesTurnWhite() {
        assertEquals(ResourceId.vanilla("white_bed"), WhitePieces.whiteOf(ResourceId.vanilla("orange_bed")));
        assertEquals(ResourceId.vanilla("white_wool"), WhitePieces.whiteOf(ResourceId.vanilla("black_wool")));
        assertEquals(ResourceId.vanilla("white_carpet"), WhitePieces.whiteOf(ResourceId.vanilla("light_blue_carpet")));
        assertEquals(ResourceId.vanilla("white_banner"), WhitePieces.whiteOf(ResourceId.vanilla("brown_banner")));
        assertEquals(ResourceId.vanilla("white_wall_banner"),
                WhitePieces.whiteOf(ResourceId.vanilla("red_wall_banner")));
    }

    @Test
    void everythingElseStaysAsItIs() {
        for (String path : List.of("white_bed", "oak_planks", "orange_terracotta", "yellow_concrete", "bedrock",
                "red_sandstone", "magenta_glazed_terracotta")) {
            assertEquals(ResourceId.vanilla(path), WhitePieces.whiteOf(ResourceId.vanilla(path)), path);
        }

        assertEquals(new ResourceId("other", "red_bed"), WhitePieces.whiteOf(new ResourceId("other", "red_bed")));
    }

    @Test
    void theBakedPlanKeepsPlaceFacingAndStates() {
        BlueprintBlock bed = new BlueprintBlock(new ColonyPos(1, 2, 3), ResourceId.vanilla("yellow_bed"), true,
                Optional.empty(), Map.of("part", "head"));
        BlueprintBlock baked = WhitePieces.baked(List.of(bed)).getFirst();

        assertEquals(ResourceId.vanilla("white_bed"), baked.block());
        assertEquals(bed.offset(), baked.offset());
        assertEquals(bed.states(), baked.states());
        assertEquals(bed.furniture(), baked.furniture());
    }
}
