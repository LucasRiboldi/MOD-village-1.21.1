package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;

import java.util.Objects;

/**
 * A geometria determinística de um nível da mina.
 *
 * <p>Cada nível abre dois lances de cinco degraus, limpa um salão de
 * 10 x 10 x 3 e então libera quatro ramais. Cada ramal desce dez
 * degraus e limpa outro salão. A ordem não usa sorteio de
 * execução: o cursor salvo sempre volta à mesma posição.
 */
public record MineShaft(ColonyPos entry, Side descent, Side gallery) {

    /** Dois lances formam cada ciclo de descida. */
    public static final int HELIX_FLIGHTS = 2;

    /** Quantos degraus o caracol inteiro desce. */
    public static final int DESCENT = 10;

    /** Maior lado do caracol, exposto para os testes de largura. */
    public static final int HELIX_SIDE = 5;

    /** Altura livre de túneis e áreas de coleta. */
    public static final int HEADROOM = 3;

    /** Altura livre que cada degrau abre. */
    public static final int STAIR_HEADROOM = 3;

    /** As três pistas que deixam toda escada com passagem de três blocos. */
    public static final int STAIR_LANES = 3;

    /** Blocos planejados por degrau: três pistas por três alturas. */
    public static final int STAIR_STEP_BLOCKS = STAIR_HEADROOM * STAIR_LANES;

    /** Blocos da escada inicial compartilhada. */
    public static final int CARVED = DESCENT * STAIR_STEP_BLOCKS;

    /** Salão comum de 10 x 10 colunas por três blocos de altura. */
    public static final int SEARCH_AREA_BLOCKS = 300;

    /** Tudo que os quatro ramais compartilham antes de se separar. */
    public static final int SHARED_BLOCKS = CARVED + SEARCH_AREA_BLOCKS;

    /** Degraus de cada um dos quatro ramais. */
    public static final int ARM_STAIRS = 10;

    /** Blocos do salão de 10 x 10 x 3 limpo por ramal. */
    public static final int ARM_AREA_BLOCKS = 300;

    /** Total de posições exclusivas de cada ramal. */
    public static final int ARM_BLOCKS = ARM_STAIRS * STAIR_STEP_BLOCKS + ARM_AREA_BLOCKS;

    private static final int SEARCH_WIDTH = 10;
    private static final int SEARCH_HEIGHT = 3;
    private static final int SEARCH_LENGTH = SEARCH_AREA_BLOCKS / (SEARCH_WIDTH * SEARCH_HEIGHT);
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
            int blocks = flightLength() * STAIR_STEP_BLOCKS;

            if (remaining < blocks) {
                return stair(cornerOf(flight), facingOn(flight), remaining);
            }

            remaining -= blocks;
        }

        throw new IllegalArgumentException("Helix index outside the shared stair: " + index);
    }

    private static int flightLength() {
        return HELIX_SIDE;
    }

    private ColonyPos cornerOf(int flight) {
        int x = entry.x();
        int y = entry.y();
        int z = entry.z();

        for (int prior = 0; prior < flight; prior++) {
            Side towards = facingOn(prior);
            int length = flightLength();
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
     * Um salão de 10 x 10 x 3. A ordem alterna os lados a partir do centro,
     * variando a área explorada sem perder um caminho físico de volta.
     */
    private ColonyPos search(ColonyPos floor, Side towards, int index) {
        if (index < 0 || index >= SEARCH_AREA_BLOCKS) {
            throw new IllegalArgumentException("Search index outside the area: " + index);
        }

        int column = index / SEARCH_HEIGHT;
        int layer = index % SEARCH_HEIGHT;
        int row = column / SEARCH_WIDTH;
        int withinRow = column % SEARCH_WIDTH;
        int offset = withinRow == 0 ? 0
                : (withinRow % 2 == 1 ? (withinRow + 1) / 2 : -withinRow / 2);
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
