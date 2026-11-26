package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.GradePlan;
import net.minecraft.util.math.BlockBox;

import java.util.List;

/**
 * A cova funda perto das construções — Regra 368 (autor 25-11, notas do jogo da 0.4.143, H4 🅱️: "buraco de dois andares
 * errado, não pode ter"). A coluna de chão natural com três ou quatro vizinhas pelo menos dois blocos acima vira âncora do
 * plano de alturas (ADR-063) um abaixo da mais baixa delas, e o Zelador a enche. Só a até {@value #NEAR} blocos de uma
 * estrutura: o resto do terreno fica como o plano decide.
 */
final class GradeHoles {

    /** Até quantos blocos de uma estrutura a cova é enchida. */
    static final int NEAR = 12;

    /** Quantos blocos abaixo da vizinha fazem a cova. */
    static final int DEPTH = 2;

    /** Quantas das quatro vizinhas precisam estar acima. */
    static final int WALLS = 3;

    private static final int[][] SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private GradeHoles() {
    }

    /** Marca cada cova funda perto de uma estrutura como âncora na altura que a enche. */
    static void fill(GradePlan.Kind[][] kinds, int[][] anchors, int[][] natural, int minX, int minZ,
                     List<BlockBox> structures) {
        for (int x = 1; x < kinds.length - 1; x++) {
            for (int z = 1; z < kinds[0].length - 1; z++) {
                if (kinds[x][z] != GradePlan.Kind.FREE || !near(minX + x, minZ + z, structures)) {
                    continue;
                }

                int walls = 0;
                int lowest = Integer.MAX_VALUE;

                for (int[] side : SIDES) {
                    int around = natural[x + side[0]][z + side[1]];

                    if (kinds[x + side[0]][z + side[1]] != GradePlan.Kind.OUT && around - natural[x][z] >= DEPTH) {
                        walls++;
                        lowest = Math.min(lowest, around);
                    }
                }

                if (walls >= WALLS) {
                    kinds[x][z] = GradePlan.Kind.ANCHOR;
                    anchors[x][z] = lowest - 1;
                }
            }
        }
    }

    private static boolean near(int x, int z, List<BlockBox> structures) {
        for (BlockBox box : structures) {
            if (x >= box.getMinX() - NEAR && x <= box.getMaxX() + NEAR && z >= box.getMinZ() - NEAR
                    && z <= box.getMaxZ() + NEAR) {
                return true;
            }
        }

        return false;
    }
}
