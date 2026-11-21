package com.villagecolony.core.construction.model;

import java.util.List;

/**
 * O miolo da vila na geração — Regra 347: a soma das caixas das peças, cada uma com a folga dela, e não o retângulo
 * que cerca todas. Só o miolo vira chão de degrau 1 e só nele a água vira terra (Regra 288); entre as peças e em volta
 * delas vale a mistura de {@link VillageBlend}, que parte da beira do miolo. Assim o lago ao lado da vila continua lago
 * e o morro entre duas casas afastadas continua morro.
 *
 * <p>Puro: a distância e a coluna do miolo mais perto de cada coluna da grade saem de uma conta só, na criação.
 */
public final class VillageCore {

    private final VillageBlend.Area bounds;

    private final boolean[][] inside;

    private final double[][] distance;

    private final int[][] nearX;

    private final int[][] nearZ;

    private VillageCore(VillageBlend.Area bounds, boolean[][] inside, double[][] distance, int[][] nearX,
                        int[][] nearZ) {
        this.bounds = bounds;
        this.inside = inside;
        this.distance = distance;
        this.nearX = nearX;
        this.nearZ = nearZ;
    }

    /**
     * O miolo feito das partes, numa grade {@code width × depth}; cada parte é cortada pela grade.
     *
     * @param parts as caixas das peças, já com a folga, nas coordenadas da grade
     */
    public static VillageCore of(List<VillageBlend.Area> parts, int width, int depth) {
        List<VillageBlend.Area> clipped = parts.stream()
                .map(part -> new VillageBlend.Area(Math.max(0, part.minX()), Math.max(0, part.minZ()),
                        Math.min(width - 1, part.maxX()), Math.min(depth - 1, part.maxZ())))
                .filter(part -> part.minX() <= part.maxX() && part.minZ() <= part.maxZ())
                .toList();

        if (clipped.isEmpty()) {
            throw new IllegalArgumentException("o miolo sem nenhuma parte dentro da grade");
        }

        VillageBlend.Area bounds = new VillageBlend.Area(
                clipped.stream().mapToInt(VillageBlend.Area::minX).min().orElseThrow(),
                clipped.stream().mapToInt(VillageBlend.Area::minZ).min().orElseThrow(),
                clipped.stream().mapToInt(VillageBlend.Area::maxX).max().orElseThrow(),
                clipped.stream().mapToInt(VillageBlend.Area::maxZ).max().orElseThrow());
        boolean[][] inside = new boolean[width][depth];
        double[][] distance = new double[width][depth];
        int[][] nearX = new int[width][depth];
        int[][] nearZ = new int[width][depth];

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                double best = Double.MAX_VALUE;

                for (VillageBlend.Area part : clipped) {
                    double apart = part.distance(x, z);

                    if (apart < best) {
                        best = apart;
                        nearX[x][z] = Math.max(part.minX(), Math.min(part.maxX(), x));
                        nearZ[x][z] = Math.max(part.minZ(), Math.min(part.maxZ(), z));
                    }
                }

                distance[x][z] = best;
                inside[x][z] = best == 0;
            }
        }

        return new VillageCore(bounds, inside, distance, nearX, nearZ);
    }

    /** O miolo de um retângulo só. */
    public static VillageCore of(VillageBlend.Area area, int width, int depth) {
        return of(List.of(area), width, depth);
    }

    /**
     * O tipo da coluna para a superfície — Regra 288: a água de dentro do miolo vira chão na altura dela; a de fora
     * fica água.
     */
    public static VillageSurface.Kind waterKind(boolean wet, boolean inCore) {
        return wet && !inCore ? VillageSurface.Kind.WATER : VillageSurface.Kind.FREE;
    }

    /** {@link #waterKind(boolean, boolean)} desta coluna. */
    public VillageSurface.Kind kindOf(boolean wet, int x, int z) {
        return waterKind(wet, contains(x, z));
    }

    /** O retângulo que cerca o miolo, dentro da grade. */
    public VillageBlend.Area bounds() {
        return bounds;
    }

    /** Se a coluna é do miolo; fora da grade, não. */
    public boolean contains(int x, int z) {
        return x >= 0 && z >= 0 && x < inside.length && z < inside[x].length && inside[x][z];
    }

    /** A distância em linha reta da coluna até o miolo; zero dentro dele. */
    double distance(int x, int z) {
        return distance[x][z];
    }

    /** O x da coluna do miolo mais perto. */
    int nearX(int x, int z) {
        return nearX[x][z];
    }

    /** O z da coluna do miolo mais perto. */
    int nearZ(int x, int z) {
        return nearZ[x][z];
    }
}
