package com.villagecolony.core.construction.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Regra 347: o miolo da vila é a soma das caixas das peças, não o retângulo que cerca todas — o lago entre duas casas
 * afastadas fica no cenário, e o chão quadrado some.
 */
class VillageCoreTest {

    private static final int WIDTH = 70;

    private static final int DEPTH = 20;

    private static final int VILLAGE = 64;

    private static final VillageBlend.Area EVERYWHERE = new VillageBlend.Area(-1000, -1000, 1000, 1000);

    /** Duas casas, uma em cada ponta; entre elas, 40 colunas sem peça. */
    private static final List<VillageBlend.Area> PIECES = List.of(
            new VillageBlend.Area(5, 5, 14, 14), new VillageBlend.Area(55, 5, 64, 14));

    private static int[][] flat() {
        int[][] natural = new int[WIDTH][DEPTH];

        for (int[] row : natural) {
            Arrays.fill(row, VILLAGE);
        }

        return natural;
    }

    private static VillageSurface.Kind[][] free() {
        VillageSurface.Kind[][] kinds = new VillageSurface.Kind[WIDTH][DEPTH];

        for (VillageSurface.Kind[] row : kinds) {
            Arrays.fill(row, VillageSurface.Kind.FREE);
        }

        return kinds;
    }

    @Test
    void aLakeBetweenThePiecesIsNotPartOfTheCoreAndStaysALake() {
        int[][] natural = flat();
        VillageSurface.Kind[][] kinds = free();
        VillageCore core = VillageCore.of(PIECES, WIDTH, DEPTH);

        for (int x = 25; x <= 44; x++) {
            for (int z = 0; z < DEPTH; z++) {
                natural[x][z] = VILLAGE - 2;
                kinds[x][z] = core.kindOf(true, x, z);
            }
        }

        int[][] offsets = VillageBlend.offsets(natural, kinds, core, EVERYWHERE);

        for (int x = 25; x <= 44; x++) {
            for (int z = 0; z < DEPTH; z++) {
                assertFalse(core.contains(x, z), "Regra 347: o lago entre as casas entrou no miolo e vira terra");
                assertEquals(0, offsets[x][z], "Regra 347: a água entre as casas mudou de altura em x=" + x);
            }
        }
    }

    @Test
    void theCoreIsFlatAroundEachPieceAndTheLevelComesFromTheNearestPiece() {
        int[][] natural = flat();
        int[][] offsets = VillageBlend.offsets(natural, free(), VillageCore.of(PIECES, WIDTH, DEPTH), EVERYWHERE);

        assertEquals(0, offsets[10][10], "o chão plano da casa mudou");
        assertEquals(0, offsets[60][10], "o chão plano da outra casa mudou");
    }
}
