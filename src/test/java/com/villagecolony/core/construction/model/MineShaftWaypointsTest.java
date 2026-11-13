package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Os valores do caminho a pé da mina (Regra 119), da rampa do fundo e das tochas de chão, e não só a forma deles —
 * VF-003: o PIT achava 33 mutantes vivos no {@link MineShaft}.
 */
class MineShaftWaypointsTest {

    private static final ColonyPos ENTRY = new ColonyPos(100, 64, 200);

    /** Degraus por lance do caracol: cada lance desce {@link MineShaft#HELIX_SIDE} blocos. */
    private static final int FLIGHT_DROP = MineShaft.HELIX_SIDE;

    private static MineShaft shaft() {
        return MineShaft.from(ENTRY, Side.NORTH);
    }

    private static ColonyPos above(ColonyPos floor) {
        return new ColonyPos(floor.x(), floor.y() + 1, floor.z());
    }

    @Test
    void theWalkHasFourFlightEndsTheHallTheCornerFourArmFlightEndsTheArmHallAndItsMiddle() {
        // 4 fins de lance do caracol + borda do salão + canto + 4 fins de lance do ramal + borda do salão dele + meio.
        assertEquals(12, shaft().waypoints().size());
    }

    @Test
    void eachFlightEndOfTheLevelSpiralIsThreeBlocksBelowThePreviousOne() {
        List<ColonyPos> path = shaft().waypoints();

        // O primeiro degrau tem os pés na altura da entrada; o fim do lance k (de 0) tem os pés 3k + 2 abaixo, e o
        // ponto do caminho é o piso, um abaixo dos pés.
        for (int flight = 0; flight < MineShaft.HELIX_FLIGHTS; flight++) {
            assertEquals(ENTRY.y() - FLIGHT_DROP * (flight + 1), path.get(flight).y(), "fim do lance " + flight);
        }
    }

    @Test
    void eachFlightEndOfTheArmSpiralIsThreeBlocksBelowThePreviousOne() {
        List<ColonyPos> path = shaft().waypoints();

        for (int index = 7; index <= 9; index++) {
            assertEquals(path.get(index - 1).y() - FLIGHT_DROP, path.get(index).y(), "lance do ramal no ponto " + index);
        }
    }

    @Test
    void everyStairAndHallWaypointIsTheFloorUnderACarvedCell() {
        MineShaft shaft = shaft();
        List<ColonyPos> path = shaft.waypoints();
        Set<ColonyPos> carved = shaft.plannedCells();

        // 5 é o canto e 11 o meio do salão do ramal: os dois já são piso por construção, e não pés de degrau.
        for (int index : new int[] {0, 1, 2, 3, 4, 6, 7, 8, 9, 10}) {
            ColonyPos floor = path.get(index);

            assertTrue(carved.contains(above(floor)), "ponto " + index + " não é o piso de um bloco cavado: " + floor);
            assertFalse(carved.contains(floor), "ponto " + index + " está dentro do vão, e não no piso: " + floor);
        }
    }

    @Test
    void theHallWaypointIsTheFirstHallCellAndComesRightAfterTheLastFlight() {
        MineShaft shaft = shaft();
        ColonyPos hallFeet = shaft.positionAt(MineShaft.CARVED);

        assertEquals(new ColonyPos(hallFeet.x(), hallFeet.y() - 1, hallFeet.z()), shaft.waypoints().get(4));
    }

    @Test
    void aRampNeedsAtLeastOneStep() {
        assertThrows(IllegalArgumentException.class, () -> MineShaft.ramp(ENTRY, Side.EAST, 0));
        assertEquals(MineShaft.STAIR_STEP_BLOCKS, MineShaft.ramp(ENTRY, Side.EAST, 1).plannedCells().size());
    }

    @Test
    void eachRampStepIsThreeLanesWideToTheLeftAndThreeBlocksTall() {
        // Rumo leste: o lado das faixas é o horário do rumo, invertido — sul invertido é norte, z menor.
        MineShaft ramp = MineShaft.ramp(ENTRY, Side.EAST, 2);
        Set<ColonyPos> first = new HashSet<>();
        Set<ColonyPos> second = new HashSet<>();

        for (int index = 0; index < MineShaft.STAIR_STEP_BLOCKS; index++) {
            first.add(ramp.positionAt(index));
            second.add(ramp.positionAt(MineShaft.STAIR_STEP_BLOCKS + index));
        }

        assertEquals(stepCells(1), first);
        assertEquals(stepCells(2), second);
    }

    private static Set<ColonyPos> stepCells(int step) {
        Set<ColonyPos> cells = new HashSet<>();

        for (int lane = 0; lane < MineShaft.STAIR_LANES; lane++) {
            for (int layer = 0; layer < MineShaft.STAIR_HEADROOM; layer++) {
                cells.add(new ColonyPos(ENTRY.x() + step, ENTRY.y() + step + layer, ENTRY.z() - lane));
            }
        }

        return cells;
    }

    @Test
    void onlyTheFirstArmOpensTheLevelSpiral() {
        MineShaft armZero = shaft();

        assertTrue(armZero.isArmZero());
        assertFalse(armZero.turned().isArmZero());
        assertFalse(armZero.turned().turned().turned().isArmZero());
    }

    @Test
    void theFloorTorchesAreEightDistinctFootCellsOfTheHallsAndNeverOnAStair() {
        MineShaft shaft = shaft();
        List<ColonyPos> spots = shaft.lightSpots();
        Set<ColonyPos> carved = shaft.plannedCells();
        Set<ColonyPos> stairs = new HashSet<>();

        for (int index = 0; index < MineShaft.CARVED; index++) {
            stairs.add(shaft.positionAt(index));
        }

        assertEquals(8, new HashSet<>(spots).size(), "tochas repetidas: " + spots);

        for (ColonyPos spot : spots) {
            assertTrue(carved.contains(spot), "tocha fora do que se cava: " + spot);
            assertFalse(carved.contains(new ColonyPos(spot.x(), spot.y() - 1, spot.z())), "tocha no ar: " + spot);
            assertFalse(stairs.contains(spot), "tocha no degrau: " + spot);
        }
    }

    @Test
    void theRampHasNoFloorTorch() {
        assertTrue(MineShaft.ramp(ENTRY, Side.EAST, 3).lightSpots().isEmpty());
    }
}
