package com.villagecolony.fabric.integration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Onde cabe a casa fundadora, lido de um mapa de alturas — decisão do autor, 2026-10-20.
 *
 * <p>Plano exato primeiro, numa área até a borda da vila; sem plano exato, o lote mais nivelado,
 * com desnível de até {@link #MAX_SPREAD} blocos, que a fundação aplaina. No mesmo desnível, o lote
 * sem vegetação antes do que precisa de árvore cortada (G1, 2026-10-20), e então o mais perto do
 * centro. Puro: quem lê o mundo e confere a segurança do lote é
 * {@link BigHouseFoundation}.
 */
final class FoundationLotSearch {

    /** O maior desnível que a fundação aplaina. */
    static final int MAX_SPREAD = 2;

    /** Coluna fora do mundo carregado: o lote que a toca não é candidato. */
    static final int UNKNOWN = Integer.MIN_VALUE;

    private FoundationLotSearch() {
    }

    /**
     * Um lote possível.
     *
     * @param offsetX a coluna do canto do lote no mapa
     * @param offsetZ a linha do canto do lote no mapa
     * @param floor a altura do piso da casa: a mediana das colunas, que é a que menos mexe no chão
     * @param spread o desnível entre a coluna mais alta e a mais baixa
     * @param ring a distância (em anel) do meio do lote ao centro do mapa
     * @param clearing se alguma coluna do lote tem vegetação a tirar
     */
    record Lot(int offsetX, int offsetZ, int floor, int spread, int ring, boolean clearing) {
    }

    /**
     * Os lotes de {@code sizeX × sizeZ} com desnível até {@link #MAX_SPREAD}, na ordem de preferência.
     *
     * @param heights alturas do mapa, linha a linha ({@code heights[z * width + x]}); a altura é a
     *     do primeiro bloco livre acima do chão, descontada a vegetação
     * @param growth se a coluna tem vegetação acima do chão, na mesma ordem
     * @param width colunas do mapa; o centro dele é o centro da colônia
     */
    static List<Lot> candidates(int[] heights, boolean[] growth, int width, int depth, int sizeX, int sizeZ) {
        List<Lot> lots = new ArrayList<>();
        int[] footprint = new int[sizeX * sizeZ];
        int middleX = (width - 1) / 2;
        int middleZ = (depth - 1) / 2;

        for (int oz = 0; oz + sizeZ <= depth; oz++) {
            for (int ox = 0; ox + sizeX <= width; ox++) {
                if (!read(heights, width, ox, oz, sizeX, sizeZ, footprint)) {
                    continue;
                }

                Arrays.sort(footprint);
                int spread = footprint[footprint.length - 1] - footprint[0];

                if (spread > MAX_SPREAD) {
                    continue;
                }

                int ring = Math.max(Math.abs(ox + sizeX / 2 - middleX), Math.abs(oz + sizeZ / 2 - middleZ));
                lots.add(new Lot(ox, oz, footprint[(footprint.length - 1) / 2], spread, ring,
                        anyGrowth(growth, width, ox, oz, sizeX, sizeZ)));
            }
        }

        lots.sort(Comparator.comparingInt(Lot::spread)
                .thenComparing(Lot::clearing)
                .thenComparingInt(Lot::ring));

        return lots;
    }

    private static boolean anyGrowth(boolean[] growth, int width, int ox, int oz, int sizeX, int sizeZ) {
        for (int dz = 0; dz < sizeZ; dz++) {
            for (int dx = 0; dx < sizeX; dx++) {
                if (growth[(oz + dz) * width + ox + dx]) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean read(int[] heights, int width, int ox, int oz, int sizeX, int sizeZ, int[] into) {
        int i = 0;

        for (int dz = 0; dz < sizeZ; dz++) {
            for (int dx = 0; dx < sizeX; dx++) {
                int height = heights[(oz + dz) * width + ox + dx];

                if (height == UNKNOWN) {
                    return false;
                }

                into[i++] = height;
            }
        }

        return true;
    }
}
