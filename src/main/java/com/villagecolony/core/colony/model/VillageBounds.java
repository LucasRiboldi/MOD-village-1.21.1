package com.villagecolony.core.colony.model;

import com.villagecolony.core.type.ColonyPos;

import java.util.Objects;

/**
 * A caixa da vila — decisão do autor, 2026-09-30, ADR-003 Emenda 6.
 *
 * <p>A vila deixa de ser um centro com um raio e passa a ser uma caixa: nasce
 * com a medida da vila gerada pelo jogo (ou {@link #TYPICAL_SIDE} de lado,
 * quando não há vila gerada), e cresce quando a colônia constrói ou abre rua
 * perto da borda. <b>Nunca encolhe.</b> O centro da colônia é o meio dela.
 *
 * <p>As regras de crescimento são as do autor:
 * <ul>
 *   <li>construção ou lote: a borda passa a ficar {@link #GROWTH_MARGIN}
 *       blocos além da peça — vila de 100 × 100 com construção no bloco 95
 *       vai a 100 × 108 ({@link #aroundPiece});</li>
 *   <li>rua: a caixa passa a conter o bloco assentado — rua 1 bloco fora de
 *       uma vila de 100 × 100 a leva a 100 × 101 ({@link #block}).</li>
 * </ul>
 *
 * <p>A altura conta só para as camas: abaixo do solo da vila (a Trial Chamber
 * a Y −20 embaixo da vila do autor) e no céu acima dela não há cama da vila —
 * ver {@link #holdsBedAt}.
 */
public record VillageBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

    /** O lado da vila sem vila gerada: a mediana do lado maior das 35 vilas medidas. */
    public static final int TYPICAL_SIDE = 144;

    /** Quanto a borda fica além de uma construção ou lote novo perto dela. */
    public static final int GROWTH_MARGIN = 12;

    /**
     * A folga em volta da caixa que ainda é a mesma vila. O menor vão entre
     * duas vilas geradas distintas, no save medido, é 37 blocos.
     */
    public static final int IDENTITY_MARGIN = 16;

    /** Quanto abaixo do chão da vila uma cama ainda pode ser dela. */
    public static final int BEDS_BELOW = 8;

    /** Quanto acima do topo da vila uma cama ainda pode ser dela. */
    public static final int BEDS_ABOVE = 24;

    public VillageBounds {
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            throw new IllegalArgumentException("inverted bounds: " + minX + "," + minY + ","
                    + minZ + " -> " + maxX + "," + maxY + "," + maxZ);
        }
    }

    /** A vila sem vila gerada: {@link #TYPICAL_SIDE} de lado em volta do ponto. */
    public static VillageBounds typicalAround(ColonyPos center, int minY, int maxY) {
        Objects.requireNonNull(center, "center");

        int half = TYPICAL_SIDE / 2;

        return new VillageBounds(
                center.x() - half, Math.min(minY, maxY), center.z() - half,
                center.x() + half - 1, Math.max(minY, maxY), center.z() + half - 1);
    }

    /** Uma construção ou lote, com {@link #GROWTH_MARGIN} de folga em X e Z. */
    public static VillageBounds aroundPiece(ColonyPos min, ColonyPos max) {
        Objects.requireNonNull(min, "min");
        Objects.requireNonNull(max, "max");

        return new VillageBounds(
                min.x() - GROWTH_MARGIN, min.y(), min.z() - GROWTH_MARGIN,
                max.x() + GROWTH_MARGIN, max.y(), max.z() + GROWTH_MARGIN);
    }

    /** Um bloco só — o de rua. */
    public static VillageBounds block(ColonyPos at) {
        Objects.requireNonNull(at, "at");

        return new VillageBounds(at.x(), at.y(), at.z(), at.x(), at.y(), at.z());
    }

    /** A caixa que contém as duas. Devolve esta mesma quando nada cresce. */
    public VillageBounds union(VillageBounds other) {
        Objects.requireNonNull(other, "other");

        VillageBounds joined = new VillageBounds(
                Math.min(minX, other.minX), Math.min(minY, other.minY), Math.min(minZ, other.minZ),
                Math.max(maxX, other.maxX), Math.max(maxY, other.maxY), Math.max(maxZ, other.maxZ));

        return joined.equals(this) ? this : joined;
    }

    public int sizeX() {
        return maxX - minX + 1;
    }

    public int sizeZ() {
        return maxZ - minZ + 1;
    }

    public int centerX() {
        return Math.floorDiv(minX + maxX + 1, 2);
    }

    public int centerZ() {
        return Math.floorDiv(minZ + maxZ + 1, 2);
    }

    /** Se a coluna está dentro da caixa, em X e Z. */
    public boolean containsColumn(int x, int z) {
        return containsColumn(x, z, 0);
    }

    /** Se a coluna está a até {@code margin} blocos da caixa, em X e Z. */
    public boolean containsColumn(int x, int z, int margin) {
        return x >= minX - margin && x <= maxX + margin
                && z >= minZ - margin && z <= maxZ + margin;
    }

    /** Se as duas caixas ficam a até {@code margin} blocos uma da outra, em X e Z. */
    public boolean touches(VillageBounds other, int margin) {
        Objects.requireNonNull(other, "other");

        return other.minX <= maxX + margin && other.maxX >= minX - margin
                && other.minZ <= maxZ + margin && other.maxZ >= minZ - margin;
    }

    /** Se uma cama nesta altura pode ser desta vila. */
    public boolean holdsBedAt(int y) {
        return y >= bedFloor() && y <= bedCeiling();
    }

    public int bedFloor() {
        return minY - BEDS_BELOW;
    }

    public int bedCeiling() {
        return maxY + BEDS_ABOVE;
    }

    @Override
    public String toString() {
        return sizeX() + "x" + sizeZ() + " [" + minX + "," + minZ + " -> " + maxX + "," + maxZ
                + ", y " + minY + ".." + maxY + "]";
    }
}
