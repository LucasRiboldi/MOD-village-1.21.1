package com.villagecolony.core.construction.model;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * A nota do lote no mapa de interesse — escolha do autor, 2026-10-28 (estudo em
 * {@code docs/research/2026-10-28-ruas-naturais.md}, a partir de Emilien et al. 2012). Menor é melhor:
 * <ul>
 *   <li>a distância do meio do lote ao centro da vila — a vila adensa antes de esticar;</li>
 *   <li>{@link #HEIGHT_WEIGHT} por bloco de diferença de altura do centro — evita o morro (no jogo da 0.3.102
 *       uma casa nasceu a 140 blocos, 24 acima do centro);</li>
 *   <li>{@link #PATH_WEIGHT} por bloco de caminho até a rua — o lote colado na rua vence.</li>
 * </ul>
 * Puro: quem lê o mundo é o lado Fabric.
 */
public final class LotScore {

    /** O peso de cada bloco de diferença de altura. */
    public static final double HEIGHT_WEIGHT = 3.0;

    /** O peso de cada bloco de caminho da porta até a rua. */
    public static final double PATH_WEIGHT = 1.0;

    private LotScore() {
    }

    /** A nota de um lote pelo canto e pelo tamanho dele, para a vila com o centro dado. */
    public static double of(int centerX, int centerY, int centerZ, int originX, int originY, int originZ,
                            int sizeX, int sizeZ, int pathLength) {
        double middleX = originX + sizeX / 2.0;
        double middleZ = originZ + sizeZ / 2.0;
        double distance = Math.hypot(middleX - centerX, middleZ - centerZ);

        return distance + HEIGHT_WEIGHT * Math.abs(originY - centerY) + PATH_WEIGHT * pathLength;
    }

    /**
     * A ordem das colunas de rua, da mais perto de {@code (fromX, fromZ)} para a mais longe: a busca de lote
     * adensa a vila antes de esticar. Devolve os índices de {@code columns}.
     */
    public static int[] nearestFirst(List<Long> columns, int fromX, int fromZ) {
        Integer[] order = IntStream.range(0, columns.size()).boxed().toArray(Integer[]::new);

        Arrays.sort(order, Comparator.comparingLong(index -> {
            long column = columns.get(index);
            long dx = (long) ColonyRoads.xOf(column) - fromX;
            long dz = (long) ColonyRoads.zOf(column) - fromZ;

            return dx * dx + dz * dz;
        }));

        return Arrays.stream(order).mapToInt(Integer::intValue).toArray();
    }
}
