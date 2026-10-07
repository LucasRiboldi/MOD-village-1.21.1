package com.villagecolony.core.construction.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Eixo, metade e formato saem na posição da planta (ADR-036 19). */
class ShapeStatesTest {

    @Test
    void axisHalfSlabTypeAndStairShapePass() {
        assertEquals(Map.of("axis", "x"), ShapeStates.of(Map.of("axis", "x")));
        assertEquals(Map.of("half", "top", "shape", "outer_left"),
                ShapeStates.of(Map.of("half", "top", "shape", "outer_left", "facing", "east")));
        assertEquals(Map.of("type", "double"), ShapeStates.of(Map.of("type", "double")));
    }

    @Test
    void doorHalvesChestTypesAndOtherStatesStayOut() {
        assertEquals(Map.of(), ShapeStates.of(Map.of("half", "upper")), "metade de porta tem dono próprio");
        assertEquals(Map.of(), ShapeStates.of(Map.of("type", "left")), "tipo de baú tem dono próprio");
        assertEquals(Map.of(), ShapeStates.of(Map.of("waterlogged", "true", "open", "false")));
        assertEquals(Map.of(), ShapeStates.of(Map.of("shape", "ascending_east")), "trilho gira diferente");
    }

    @Test
    void theStateTextOfAJigsawIsRead() {
        assertEquals(Map.of("half", "top", "shape", "straight"),
                ShapeStates.parse("minecraft:oak_stairs[facing=east,half=top,shape=straight,waterlogged=false]"));
        assertEquals(Map.of(), ShapeStates.parse("minecraft:stone"));
    }

    @Test
    void aQuarterTurnSwapsTheHorizontalAxis() {
        assertEquals(Map.of("axis", "z"), ShapeStates.turned(Map.of("axis", "x"), 1));
        assertEquals(Map.of("axis", "x"), ShapeStates.turned(Map.of("axis", "x"), 2));
        assertEquals(Map.of("axis", "x"), ShapeStates.turned(Map.of("axis", "z"), 3));
        assertEquals(Map.of("axis", "y"), ShapeStates.turned(Map.of("axis", "y"), 1));
        assertEquals(Map.of("half", "top"), ShapeStates.turned(Map.of("half", "top"), 1));
    }
}
