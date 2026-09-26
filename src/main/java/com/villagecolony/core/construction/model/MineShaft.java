package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;

import java.util.Objects;

/**
 * A geometria determinística de um nível da mina.
 *
 * <p>Cada nível abre um caracol de dez degraus, limpa uma área de
 * cinquenta blocos e então libera quatro ramais. Cada ramal desce dez
 * degraus e limpa mais cinquenta blocos. A ordem não usa sorteio de
 * execução: o cursor salvo sempre volta à mesma posição.
 */
public record MineShaft(ColonyPos entry, Side descent, Side gallery) {

    /** Quatro curvas formam o caracol. Os lados alternam três e dois degraus. */
    public static final int HELIX_FLIGHTS = 4;

    /** Quantos degraus o caracol inteiro desce. */
    public static final int DESCENT = 10;

    /** Maior lado do caracol, exposto para os testes de largura. */
    public static final int HELIX_SIDE = 3;

    /** Altura livre de túneis e áreas de coleta. */
    public static final int HEADROOM = 3;

    /** Altura livre que cada degrau abre. */
    public static final int STAIR_HEADROOM = 3;

    /** As duas pistas que permitem a passagem em sentidos opostos. */
    public static final int STAIR_LANES = 2;

    /** Blocos planejados por degrau: duas pistas por três alturas. */
    private static final int STAIR_STEP_BLOCKS = STAIR_HEADROOM * STAIR_LANES;

    /** Blocos da escada inicial compartilhada. */
    public static final int CARVED = DESCENT * STAIR_STEP_BLOCKS;

    /** Área de exploração comum após o caracol. */
    public static final int SEARCH_AREA_BLOCKS = 50;

    /** Tudo que os quatro ramais compartilham antes de se separar. */
    public static final int SHARED_BLOCKS = CARVED + SEARCH_AREA_BLOCKS;

    /** Degraus de cada um dos quatro ramais. */
    public static final int ARM_STAIRS = 10;

    /** Blocos de área limpos por ramal. */
    public static final int ARM_AREA_BLOCKS = 50;

    /** Total de posições exclusivas de cada ramal. */
    public static final int ARM_BLOCKS = ARM_STAIRS * STAIR_STEP_BLOCKS + ARM_AREA_BLOCKS;

    private static final int SEARCH_WIDTH = 5;
    private static final int SEARCH_LENGTH = SEARCH_AREA_BLOCKS / (SEARCH_WIDTH * 2);
    private static final int MINEABLE_BOTTOM = -63;

    /** A menor altura do piso central que ainda deixa o último ramal acima da rocha-mãe. */
    public static final int DEEPEST = MINEABLE_BOTTOM + ARM_STAIRS - 2;

    public MineShaft {
        Objects.requireNonNull(entry, "entry");
        Objects.requireNonNull(descent, "descent");
        Objects.requireNonNull(gallery, "gallery");
    }

    /** Abre a escada e posiciona o primeiro ramal à direita da entrada. */
    public static MineShaft from(ColonyPos entry, Side descent) {
        return new MineShaft(entry, descent, descent.clockwise().clockwise());
    }

    /** A mesma escada, com o próximo ramal em sentido horário. */
    public MineShaft turned() {
        return new MineShaft(entry, descent, gallery.clockwise());
    }

    /** Tenta a mesma boca com o caracol orientado para o próximo lado. */
    public MineShaft rerouted() {
        return from(entry, descent.clockwise());
    }

    /** O próximo nível começa no piso central desta escada. */
    public MineShaft deepened() {
        return new MineShaft(levelFloor(), descent, gallery);
    }

    /** Não propõe posições abaixo da faixa minerável do mundo. */
    public boolean mayDeepen() {
        return deepened().lowestPlannedY() >= MINEABLE_BOTTOM;
    }

    /** A posição da ordem de escavação. */
    public ColonyPos positionAt(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("Index must be non-negative: " + index);
        }

        if (index < CARVED) {
            return helix(index);
        }

        if (index < SHARED_BLOCKS) {
            return search(levelFloor(), commonDirection(), index - CARVED);
        }

        int armIndex = index - SHARED_BLOCKS;

        if (armIndex < ARM_STAIRS * STAIR_STEP_BLOCKS) {
            return stair(branchTop(), gallery, armIndex);
        }

        return search(branchFloor(), gallery, armIndex - ARM_STAIRS * STAIR_STEP_BLOCKS);
    }

    /**
     * Todas as células que este nível e este ramal planejam abrir — 2026-09-25,
     * ADR-025.
     *
     * <p>É o que separa o vão de caverna da célula que a própria mina ainda
     * vai cavar: o bloco sob a sala comum é piso, e o sob a camada de cima da
     * sala é a camada de baixo. Quem remenda o piso pergunta aqui.
     */
    public java.util.Set<ColonyPos> plannedCells() {
        java.util.Set<ColonyPos> cells = new java.util.HashSet<>();

        for (int index = 0; index < SHARED_BLOCKS + ARM_BLOCKS; index++) {
            cells.add(positionAt(index));
        }

        return cells;
    }

    /** Cada ramal tem exatamente sua escada e sua área finitas. */
    public boolean beyondTheArm(int index) {
        return index >= SHARED_BLOCKS + ARM_BLOCKS;
    }

    private ColonyPos helix(int index) {
        int remaining = index;

        for (int flight = 0; flight < HELIX_FLIGHTS; flight++) {
            int blocks = flightLength(flight) * STAIR_STEP_BLOCKS;

            if (remaining < blocks) {
                return stair(cornerOf(flight), facingOn(flight), remaining);
            }

            remaining -= blocks;
        }

        throw new IllegalArgumentException("Helix index outside the shared stair: " + index);
    }

    private static int flightLength(int flight) {
        return flight % 2 == 0 ? HELIX_SIDE : HELIX_SIDE - 1;
    }

    private ColonyPos cornerOf(int flight) {
        int x = entry.x();
        int y = entry.y();
        int z = entry.z();

        for (int prior = 0; prior < flight; prior++) {
            Side towards = facingOn(prior);
            int length = flightLength(prior);
            x += towards.offsetX() * length;
            z += towards.offsetZ() * length;
            y -= length;
        }

        return new ColonyPos(x, y, z);
    }

    private Side facingOn(int flight) {
        Side towards = descent;

        for (int turn = 0; turn < flight; turn++) {
            towards = towards.clockwise();
        }

        return towards;
    }

    private static ColonyPos stair(ColonyPos top, Side towards, int index) {
        int step = index / STAIR_STEP_BLOCKS + 1;
        int within = index % STAIR_STEP_BLOCKS;
        int lane = within / STAIR_HEADROOM;
        int layer = within % STAIR_HEADROOM;
        Side sideways = towards.clockwise().opposite();

        return new ColonyPos(
                top.x() + towards.offsetX() * step + sideways.offsetX() * lane,
                top.y() - step + 1 + layer,
                top.z() + towards.offsetZ() * step + sideways.offsetZ() * lane);
    }

    /** O centro do próximo ciclo, dez blocos abaixo da entrada do ciclo atual. */
    private ColonyPos levelFloor() {
        return cornerOf(HELIX_FLIGHTS);
    }

    private Side commonDirection() {
        return descent.clockwise().clockwise();
    }

    private ColonyPos branchTop() {
        ColonyPos floor = levelFloor();

        return new ColonyPos(
                floor.x() + gallery.offsetX() * SEARCH_LENGTH,
                floor.y() + 1,
                floor.z() + gallery.offsetZ() * SEARCH_LENGTH);
    }

    private ColonyPos branchFloor() {
        ColonyPos top = branchTop();
        return new ColonyPos(
                top.x() + gallery.offsetX() * ARM_STAIRS,
                top.y() - ARM_STAIRS,
                top.z() + gallery.offsetZ() * ARM_STAIRS);
    }

    /**
     * Uma sala de 5 x 5 x 2. A ordem alterna os lados a partir do centro,
     * variando a área explorada sem perder um caminho físico de volta.
     */
    private ColonyPos search(ColonyPos floor, Side towards, int index) {
        if (index < 0 || index >= SEARCH_AREA_BLOCKS) {
            throw new IllegalArgumentException("Search index outside the area: " + index);
        }

        int column = index / 2;
        int layer = index % 2;
        int row = column / SEARCH_WIDTH;
        int withinRow = column % SEARCH_WIDTH;
        int offset = switch (withinRow) {
            case 0 -> 0;
            case 1 -> 1;
            case 2 -> -1;
            case 3 -> 2;
            default -> -2;
        };
        Side sideways = towards.clockwise();

        return new ColonyPos(
                floor.x() + towards.offsetX() * row + sideways.offsetX() * offset,
                floor.y() + 1 + layer,
                floor.z() + towards.offsetZ() * row + sideways.offsetZ() * offset);
    }

    private int lowestPlannedY() {
        return branchFloor().y() + 1;
    }
}
