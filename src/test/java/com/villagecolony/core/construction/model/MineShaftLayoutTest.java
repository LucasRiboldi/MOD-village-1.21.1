package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O caminho que desce é o caminho de volta — Regra 119: degraus de um bloco, e nenhuma célula da mina
 * é o chão de um degrau ou de um salão.
 */
class MineShaftLayoutTest {

    private static final int STAIR = MineShaft.CARVED;

    private static final int ARM_STAIR = MineShaft.ARM_STAIRS * MineShaft.STAIR_STEP_BLOCKS;

    @Test
    void everyStepIsOneBlockBelowTheLastAndBesideIt() {
        for (Side descent : Side.values()) {
            for (MineShaft arm : armsOf(MineShaft.from(new ColonyPos(0, 60, 0), descent))) {
                List<ColonyPos> down = new ArrayList<>();
                down.add(new ColonyPos(arm.entry().x(), arm.entry().y() + 1, arm.entry().z()));
                down.addAll(stepFeet(arm, 0, STAIR));
                down.add(arm.positionAt(STAIR));
                assertWalkable(down, descent + "/" + arm.gallery() + " caracol do nível");

                // Do canto do salão, pelo caracol do ramal, até o salão dele.
                ColonyPos first = arm.positionAt(MineShaft.SHARED_BLOCKS);
                ColonyPos corner = new ColonyPos(first.x() - arm.gallery().offsetX(), first.y() + 1,
                        first.z() - arm.gallery().offsetZ());
                assertTrue(hallFeet(arm).contains(corner), "o ramal não sai de uma célula do salão: " + corner);

                List<ColonyPos> branch = new ArrayList<>();
                branch.add(corner);
                branch.addAll(stepFeet(arm, MineShaft.SHARED_BLOCKS, ARM_STAIR));
                branch.add(arm.positionAt(MineShaft.SHARED_BLOCKS + ARM_STAIR));
                assertWalkable(branch, descent + "/" + arm.gallery() + " ramal");
            }
        }
    }

    @Test
    void noCellOfTheLevelIsTheFloorOfAStepOrOfAHall() {
        for (Side descent : Side.values()) {
            List<MineShaft> arms = armsOf(MineShaft.from(new ColonyPos(0, 60, 0), descent));
            Set<ColonyPos> dug = new HashSet<>();
            Set<ColonyPos> floors = new HashSet<>();

            for (MineShaft arm : arms) {
                dug.addAll(arm.plannedCells());
                floors.addAll(floorsOf(arm));
            }

            Set<ColonyPos> broken = new HashSet<>(floors);
            broken.retainAll(dug);

            assertTrue(broken.isEmpty(), descent + ": células cavadas no chão de quem anda: " + broken);
        }
    }

    @Test
    void theNextLevelDoesNotBreakTheStairsAbove() {
        MineShaft level = MineShaft.from(new ColonyPos(0, 60, 0), Side.EAST);

        for (int depth = 0; depth < 2; depth++) {
            MineShaft next = level.deepened();
            Set<ColonyPos> stairFloors = new HashSet<>();

            for (MineShaft arm : armsOf(level)) {
                stairFloors.addAll(below(stepFeet(arm, 0, STAIR)));
                stairFloors.addAll(below(stepFeet(arm, MineShaft.SHARED_BLOCKS, ARM_STAIR)));
                stairFloors.addAll(below(landingFeet(arm)));
            }

            Set<ColonyPos> dug = new HashSet<>();

            for (MineShaft arm : armsOf(next)) {
                dug.addAll(arm.plannedCells());
            }

            dug.retainAll(stairFloors);
            assertTrue(dug.isEmpty(), "o nível " + (depth + 1) + " corta degraus do de cima: " + dug);
            level = next;
        }
    }

    /**
     * A tocha de chão fica num salão (célula aberta) e o chão dela nunca é cavado depois — nem pelos
     * ramais deste nível, nem pelo nível seguinte: a tocha da parede caía quando o mineiro cavava a
     * parede (playtest 0.3.97).
     */
    @Test
    void theFloorUnderEveryTorchIsNeverDug() {
        for (Side descent : Side.values()) {
            MineShaft level = MineShaft.from(new ColonyPos(0, 60, 0), descent);
            Set<ColonyPos> dug = new HashSet<>();

            for (MineShaft arm : armsOf(level)) {
                dug.addAll(arm.plannedCells());
            }
            for (MineShaft arm : armsOf(level.deepened())) {
                dug.addAll(arm.plannedCells());
            }

            for (MineShaft arm : armsOf(level)) {
                assertEquals(8, arm.lightSpots().size(), descent + ": quatro tochas por salão");
                for (ColonyPos spot : arm.lightSpots()) {
                    assertTrue(arm.plannedCells().contains(spot), descent + ": tocha fora do salão em " + spot);
                    ColonyPos floor = new ColonyPos(spot.x(), spot.y() - 1, spot.z());
                    assertFalse(dug.contains(floor), descent + ": o chão da tocha em " + spot + " é cavado depois");
                }
            }
        }
    }

    @Test
    void theFourArmsAreFourCornersAndFourHalls() {
        List<MineShaft> arms = armsOf(MineShaft.from(new ColonyPos(0, 60, 0), Side.SOUTH));
        Set<ColonyPos> exclusive = new HashSet<>();
        int planned = 0;

        for (MineShaft arm : arms) {
            for (int index = MineShaft.SHARED_BLOCKS; index < MineShaft.SHARED_BLOCKS + MineShaft.ARM_BLOCKS; index++) {
                exclusive.add(arm.positionAt(index));
                planned++;
            }
        }

        assertEquals(planned, exclusive.size(), "dois ramais cavam a mesma célula");
        int end = MineShaft.ARM_END + arms.getFirst().landingBlocks();

        assertTrue(arms.getFirst().beyondTheArm(end));
        assertFalse(arms.getFirst().beyondTheArm(end - 1));
    }

    @Test
    void eachLevelGoesDownTwoStairs() {
        MineShaft shaft = MineShaft.from(new ColonyPos(0, 60, 0), Side.WEST);

        assertEquals(60 - MineShaft.DESCENT - MineShaft.ARM_STAIRS, shaft.deepened().entry().y());
    }

    /** O patamar fica na altura do último degrau do lance, encostado nele, com 3 de fundo (Regra 254). */
    @Test
    void everyLandingIsLevelWithTheLastStepAndBesideIt() {
        for (Side descent : Side.values()) {
            for (MineShaft arm : armsOf(MineShaft.from(new ColonyPos(0, 60, 0), descent))) {
                List<ColonyPos> landing = landingFeet(arm);
                Set<ColonyPos> steps = new HashSet<>(stepFeet(arm, 0, STAIR));
                steps.addAll(stepFeet(arm, MineShaft.SHARED_BLOCKS, ARM_STAIR));

                assertEquals(arm.landingBlocks() / MineShaft.HEADROOM, landing.size(), descent + ": pés do patamar");
                assertEquals(arm.isArmZero() ? 6 : 3, arm.landingLights().size(), descent + ": uma tocha por patamar");

                for (ColonyPos feet : landing) {
                    assertTrue(reachesAStep(feet, landing, steps), descent + "/" + arm.gallery()
                            + ": o patamar em " + feet + " não chega a um degrau na mesma altura");
                }
            }
        }
    }

    /** A parede que segura a tocha do patamar nunca é cavada: nem por este nível, nem pelo seguinte (Regra 254). */
    @Test
    void theWallBehindEveryLandingTorchIsNeverDug() {
        for (Side descent : Side.values()) {
            MineShaft level = MineShaft.from(new ColonyPos(0, 60, 0), descent);
            Set<ColonyPos> dug = new HashSet<>();

            for (MineShaft arm : armsOf(level)) {
                dug.addAll(arm.plannedCells());
            }
            for (MineShaft arm : armsOf(level.deepened())) {
                dug.addAll(arm.plannedCells());
            }

            for (MineShaft arm : armsOf(level)) {
                for (MineShaft.WallLight light : arm.landingLights()) {
                    ColonyPos wall = new ColonyPos(light.at().x() + light.wall().offsetX(), light.at().y(),
                            light.at().z() + light.wall().offsetZ());

                    assertTrue(arm.plannedCells().contains(light.at()), descent + ": tocha fora do patamar em " + light.at());
                    assertFalse(dug.contains(wall), descent + ": a parede da tocha em " + wall + " é cavada");
                }
            }
        }
    }

    /** O nível que já estava sendo cavado termina sem patamar; o seguinte já o tem (escolha do autor, 12-11). */
    @Test
    void aLevelOpenedBeforeTheRuleKeepsItsShape() {
        MineShaft old = new MineShaft(new ColonyPos(0, 60, 0), Side.NORTH, Side.SOUTH);

        assertEquals(0, old.landingBlocks());
        assertTrue(old.beyondTheArm(MineShaft.ARM_END));
        assertTrue(old.landingLights().isEmpty());
        assertFalse(old.turned().landings());
        assertTrue(old.deepened().landings());
        assertTrue(MineShaft.from(new ColonyPos(0, 60, 0), Side.NORTH).rerouted().landings());
        assertEquals(MineShaft.ARM_END + 2 * MineShaft.HELIX_LANDING_BLOCKS,
                MineShaft.from(new ColonyPos(0, 60, 0), Side.NORTH).plannedCells().size());
    }

    // ---------------------------------------------------------------------

    /** Os pés do patamar: a camada de baixo de cada coluna dele. */
    private static List<ColonyPos> landingFeet(MineShaft shaft) {
        List<ColonyPos> feet = new ArrayList<>();

        for (int index = MineShaft.ARM_END; !shaft.beyondTheArm(index); index += MineShaft.HEADROOM) {
            feet.add(shaft.positionAt(index));
        }

        return feet;
    }

    /** Se, andando pelo patamar na mesma altura, se chega ao lado de um degrau dessa altura. */
    private static boolean reachesAStep(ColonyPos from, List<ColonyPos> landing, Set<ColonyPos> steps) {
        Set<ColonyPos> seen = new HashSet<>();
        java.util.ArrayDeque<ColonyPos> open = new java.util.ArrayDeque<>(List.of(from));

        while (!open.isEmpty()) {
            ColonyPos at = open.poll();

            if (!seen.add(at)) {
                continue;
            }

            for (Side side : Side.values()) {
                ColonyPos next = new ColonyPos(at.x() + side.offsetX(), at.y(), at.z() + side.offsetZ());

                if (steps.contains(next)) {
                    return true;
                }
                if (landing.contains(next)) {
                    open.add(next);
                }
            }
        }

        return false;
    }

    private static List<MineShaft> armsOf(MineShaft shaft) {
        List<MineShaft> arms = new ArrayList<>();
        MineShaft arm = shaft;

        for (int turn = 0; turn < Mine.ARMS; turn++) {
            arms.add(arm);
            arm = arm.turned();
        }

        return arms;
    }

    /** Os pés de cada degrau na pista de dentro (a camada de baixo da pista zero). */
    private static List<ColonyPos> stepFeet(MineShaft shaft, int from, int blocks) {
        List<ColonyPos> feet = new ArrayList<>();

        for (int index = from; index < from + blocks; index += MineShaft.STAIR_STEP_BLOCKS) {
            feet.add(shaft.positionAt(index));
        }

        return feet;
    }

    /** O chão de tudo em que se pisa: o bloco sob a camada de baixo de cada degrau e de cada salão. */
    private static Set<ColonyPos> floorsOf(MineShaft shaft) {
        Set<ColonyPos> floors = new HashSet<>();
        for (int index = 0; !shaft.beyondTheArm(index); index += MineShaft.HEADROOM) {
            ColonyPos feet = shaft.positionAt(index);
            floors.add(new ColonyPos(feet.x(), feet.y() - 1, feet.z()));
        }

        return floors;
    }

    private static List<ColonyPos> below(List<ColonyPos> feet) {
        return feet.stream().map(at -> new ColonyPos(at.x(), at.y() - 1, at.z())).toList();
    }

    /** Os pés de cada coluna do salão do nível. */
    private static Set<ColonyPos> hallFeet(MineShaft shaft) {
        Set<ColonyPos> feet = new HashSet<>();

        for (int index = STAIR; index < MineShaft.SHARED_BLOCKS; index += MineShaft.HEADROOM) {
            feet.add(shaft.positionAt(index));
        }

        return feet;
    }

    private static void assertWalkable(List<ColonyPos> walk, String which) {
        for (int at = 1; at < walk.size(); at++) {
            ColonyPos from = walk.get(at - 1);
            ColonyPos to = walk.get(at);
            int flat = Math.abs(from.x() - to.x()) + Math.abs(from.z() - to.z());

            assertTrue(flat == 1 && Math.abs(from.y() - to.y()) <= 1,
                    which + ": de " + from + " para " + to + " não é um passo de um bloco");
        }
    }
}
