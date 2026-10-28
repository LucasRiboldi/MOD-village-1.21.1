package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;

import java.util.Objects;

/**
 * A geometria determinística de um nível da mina — Regra 119 (autor, 2026-10-27).
 *
 * <p><b>O ciclo.</b> Cada nível desce por um caracol de {@link #HELIX_FLIGHTS} lances curtos, que dá
 * uma volta inteira e desce um bloco por degrau; ao pé dele abre um salão de
 * {@value #HALL_SIDE} x {@value #HALL_SIDE} x 3. De cada um dos quatro cantos do salão sai um ramal: outro
 * caracol, virado para fora, que termina num salão do mesmo tamanho no quadrante diagonal daquele canto.
 * O nível seguinte começa no meio do salão do ramal zero, e o ciclo se repete.
 *
 * <p><b>O caminho de volta é o da descida.</b> Os degraus são três pistas de largura por três de altura,
 * e cada um fica um bloco abaixo do anterior; do pé do caracol para o salão e do canto do salão para o
 * ramal o passo também é de um bloco. Nenhuma célula planejada é o chão de um degrau ou de um salão do
 * mesmo nível: o caracol fica fora das colunas do salão, e cada ramal fica fora delas e das dos outros.
 * O {@code MineShaftLayoutTest} confere isso para vários níveis seguidos.
 *
 * <p>A ordem não usa sorteio de execução: o cursor salvo sempre volta à mesma posição.
 *
 * <p><b>A rampa</b> ({@code ascent > 0}) é o fim da mina no fundo do mundo — ADR-036 item 17: a partir
 * do piso do salão ({@code entry}), {@code ascent} degraus de três pistas por três alturas, um para a
 * frente e um para cima, no rumo {@code descent} (para longe do centro da vila). Um ramal só.
 */
public record MineShaft(ColonyPos entry, Side descent, Side gallery, int ascent) {

    /** Quatro lances: o caracol dá uma volta inteira. */
    public static final int HELIX_FLIGHTS = 4;

    /** Degraus de cada lance — poucos, para o caracol ficar curto. */
    public static final int HELIX_SIDE = 3;

    /** Quantos degraus o caracol inteiro desce. */
    public static final int DESCENT = HELIX_FLIGHTS * HELIX_SIDE;

    /** Altura livre de túneis e áreas de coleta. */
    public static final int HEADROOM = 3;

    /** Altura livre que cada degrau abre. */
    public static final int STAIR_HEADROOM = 3;

    /** As três pistas que deixam toda escada com passagem de três blocos. */
    public static final int STAIR_LANES = 3;

    /** Blocos planejados por degrau: três pistas por três alturas. */
    public static final int STAIR_STEP_BLOCKS = STAIR_HEADROOM * STAIR_LANES;

    /** Blocos do caracol do nível. */
    public static final int CARVED = DESCENT * STAIR_STEP_BLOCKS;

    /** O lado do salão, ímpar para ter meio. */
    public static final int HALL_SIDE = 17;

    /** Do meio do salão até a borda. */
    private static final int HALF = HALL_SIDE / 2;

    /** O salão do nível, ao pé do caracol. */
    public static final int SEARCH_AREA_BLOCKS = HALL_SIDE * HALL_SIDE * HEADROOM;

    /** Tudo que os quatro ramais compartilham antes de se separar. */
    public static final int SHARED_BLOCKS = CARVED + SEARCH_AREA_BLOCKS;

    /** Degraus do caracol de cada ramal. */
    public static final int ARM_STAIRS = DESCENT;

    /** O salão no fim de cada ramal, do mesmo tamanho do salão do nível. */
    public static final int ARM_AREA_BLOCKS = SEARCH_AREA_BLOCKS;

    /** Total de posições exclusivas de cada ramal. */
    public static final int ARM_BLOCKS = ARM_STAIRS * STAIR_STEP_BLOCKS + ARM_AREA_BLOCKS;

    private static final int MINEABLE_BOTTOM = -63;

    /** A menor altura do piso do salão do nível que ainda deixa o salão dos ramais acima da rocha-mãe. */
    public static final int DEEPEST = MINEABLE_BOTTOM + ARM_STAIRS - 1;

    public MineShaft {
        Objects.requireNonNull(entry, "entry");
        Objects.requireNonNull(descent, "descent");
        Objects.requireNonNull(gallery, "gallery");

        if (ascent < 0) {
            throw new IllegalArgumentException("Ascent must be non-negative: " + ascent);
        }
    }

    /** Um nível que desce. */
    public MineShaft(ColonyPos entry, Side descent, Side gallery) {
        this(entry, descent, gallery, 0);
    }

    /** A rampa de {@code steps} degraus que sobe do piso {@code floor} rumo a {@code away}. */
    public static MineShaft ramp(ColonyPos floor, Side away, int steps) {
        if (steps <= 0) {
            throw new IllegalArgumentException("A ramp needs at least one step: " + steps);
        }

        return new MineShaft(floor, away, away, steps);
    }

    public boolean isRamp() {
        return ascent > 0;
    }

    /** O piso (o bloco debaixo dos pés) no meio do salão deste nível, de onde a rampa do fundo parte. */
    public ColonyPos hallFloor() {
        return isRamp() ? entry : hallCenter();
    }

    /** Abre o caracol e posiciona o primeiro ramal no canto oposto ao rumo da descida. */
    public static MineShaft from(ColonyPos entry, Side descent) {
        return new MineShaft(entry, descent, descent.clockwise().clockwise());
    }

    /** O mesmo caracol, com o próximo ramal em sentido horário. */
    public MineShaft turned() {
        return new MineShaft(entry, descent, gallery.clockwise(), ascent);
    }

    /** Tenta a mesma boca com o caracol orientado para o próximo lado. */
    public MineShaft rerouted() {
        return isRamp() ? ramp(entry, descent.clockwise(), ascent) : from(entry, descent.clockwise());
    }

    /** O próximo nível começa no meio do salão do ramal zero, o do canto de {@code gallery}. */
    public MineShaft deepened() {
        return new MineShaft(armHallCenter(), descent, gallery);
    }

    /**
     * O caminho a pé da entrada deste nível até a do seguinte, em pisos (o bloco debaixo dos pés): o fim
     * de cada lance do caracol, a borda do salão, o canto do ramal zero, o fim de cada lance do caracol
     * dele, a borda do salão dele e o meio, de onde o próximo nível desce. Cada trecho é curto para a
     * navegação Vanilla — Regra 119.
     */
    public java.util.List<ColonyPos> waypoints() {
        java.util.List<ColonyPos> path = new java.util.ArrayList<>();
        int flight = HELIX_SIDE * STAIR_STEP_BLOCKS;

        for (int end = flight; end <= CARVED; end += flight) {
            path.add(floorOf(positionAt(end - STAIR_STEP_BLOCKS)));
        }

        path.add(floorOf(positionAt(CARVED)));
        path.add(corner());

        for (int end = flight; end <= ARM_STAIRS * STAIR_STEP_BLOCKS; end += flight) {
            path.add(floorOf(positionAt(SHARED_BLOCKS + end - STAIR_STEP_BLOCKS)));
        }

        path.add(floorOf(positionAt(SHARED_BLOCKS + ARM_STAIRS * STAIR_STEP_BLOCKS)));
        path.add(armHallCenter());

        return path;
    }

    private static ColonyPos floorOf(ColonyPos feet) {
        return new ColonyPos(feet.x(), feet.y() - 1, feet.z());
    }

    /** Não propõe posições abaixo da faixa minerável do mundo. */
    public boolean mayDeepen() {
        return !isRamp() && deepened().lowestPlannedY() >= MINEABLE_BOTTOM;
    }

    /** A posição da ordem de escavação. */
    public ColonyPos positionAt(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("Index must be non-negative: " + index);
        }

        if (isRamp()) {
            return rampCell(index);
        }

        if (beyondTheArm(index)) {
            throw new IllegalArgumentException("Index past the end of the arm: " + index);
        }

        if (index < CARVED) {
            return helix(entry, descent, index);
        }

        if (index < SHARED_BLOCKS) {
            return hall(index - CARVED);
        }

        int armIndex = index - SHARED_BLOCKS;

        if (armIndex < ARM_STAIRS * STAIR_STEP_BLOCKS) {
            return helix(corner(), gallery, armIndex);
        }

        return armHall(armIndex - ARM_STAIRS * STAIR_STEP_BLOCKS);
    }

    /**
     * Todas as células que este nível e este ramal planejam abrir — 2026-09-25, ADR-025.
     *
     * <p>É o que separa o vão de caverna da célula que a própria mina ainda vai cavar: o bloco sob a
     * sala comum é piso, e o sob a camada de cima da sala é a camada de baixo. Quem remenda o piso
     * pergunta aqui.
     */
    public java.util.Set<ColonyPos> plannedCells() {
        java.util.Set<ColonyPos> cells = new java.util.HashSet<>();
        int planned = isRamp() ? ascent * STAIR_STEP_BLOCKS : SHARED_BLOCKS + ARM_BLOCKS;

        for (int index = 0; index < planned; index++) {
            cells.add(positionAt(index));
        }

        return cells;
    }

    /** Cada ramal tem exatamente seu caracol e seu salão finitos. */
    public boolean beyondTheArm(int index) {
        return index >= (isRamp() ? ascent * STAIR_STEP_BLOCKS : SHARED_BLOCKS + ARM_BLOCKS);
    }

    /** O degrau da rampa: pés em {@code entry.y + degrau}, um à frente por degrau. */
    private ColonyPos rampCell(int index) {
        int step = index / STAIR_STEP_BLOCKS + 1;
        int within = index % STAIR_STEP_BLOCKS;
        int lane = within / STAIR_HEADROOM;
        int layer = within % STAIR_HEADROOM;
        Side sideways = descent.clockwise().opposite();

        return new ColonyPos(
                entry.x() + descent.offsetX() * step + sideways.offsetX() * lane,
                entry.y() + step + layer,
                entry.z() + descent.offsetZ() * step + sideways.offsetZ() * lane);
    }

    /**
     * Um caracol de {@link #HELIX_FLIGHTS} lances a partir de {@code top}: cada lance anda
     * {@link #HELIX_SIDE} degraus e vira à direita. As pistas ficam à esquerda de quem desce, para fora
     * da volta, e o miolo do caracol fica de rocha. O último degrau cai na coluna de {@code top}.
     */
    private static ColonyPos helix(ColonyPos top, Side first, int index) {
        int flight = index / (HELIX_SIDE * STAIR_STEP_BLOCKS);
        int within = index % (HELIX_SIDE * STAIR_STEP_BLOCKS);
        Side towards = first;
        int x = top.x();
        int z = top.z();

        for (int prior = 0; prior < flight; prior++) {
            x += towards.offsetX() * HELIX_SIDE;
            z += towards.offsetZ() * HELIX_SIDE;
            towards = towards.clockwise();
        }

        return stair(new ColonyPos(x, top.y() - flight * HELIX_SIDE, z), towards, within);
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

    /** O piso no pé do caracol que começa em {@code top}: a coluna de {@code top}, {@link #DESCENT} abaixo. */
    private static int bottomFloorY(ColonyPos top) {
        return top.y() - DESCENT;
    }

    /** O meio do salão do nível: à esquerda do pé do caracol, por onde o último lance sai. */
    private ColonyPos hallCenter() {
        Side out = descent.clockwise().opposite();

        return new ColonyPos(
                entry.x() + out.offsetX() * (HALF + 1),
                bottomFloorY(entry),
                entry.z() + out.offsetZ() * (HALF + 1));
    }

    /**
     * O salão do nível, fileira por fileira a partir da que encosta no caracol, e em cada fileira do
     * meio para os lados — o mineiro abre a área sem perder o caminho físico de volta.
     */
    private ColonyPos hall(int index) {
        Side along = descent.clockwise().opposite();
        int column = index / HEADROOM;
        int layer = index % HEADROOM;
        int row = column / HALL_SIDE - HALF;
        int withinRow = column % HALL_SIDE;
        int offset = withinRow == 0 ? 0 : (withinRow % 2 == 1 ? (withinRow + 1) / 2 : -withinRow / 2);
        ColonyPos center = hallCenter();

        return new ColonyPos(
                center.x() + along.offsetX() * row + descent.offsetX() * offset,
                center.y() + 1 + layer,
                center.z() + along.offsetZ() * row + descent.offsetZ() * offset);
    }

    /** O canto do salão de onde sai este ramal: o de fora nos rumos {@code gallery} e à esquerda dele. */
    private ColonyPos corner() {
        ColonyPos center = hallCenter();
        Side left = gallery.clockwise().opposite();

        return new ColonyPos(
                center.x() + (gallery.offsetX() + left.offsetX()) * HALF,
                center.y(),
                center.z() + (gallery.offsetZ() + left.offsetZ()) * HALF);
    }

    /**
     * O salão do ramal, no quadrante diagonal do canto: fileiras para a esquerda de {@code gallery} a
     * partir do pé do caracol do ramal, e colunas no rumo {@code gallery}, ambas para fora do salão do
     * nível.
     */
    private ColonyPos armHall(int index) {
        Side along = gallery.clockwise().opposite();
        int column = index / HEADROOM;
        int layer = index % HEADROOM;
        int row = column / HALL_SIDE + 1;
        int sideways = column % HALL_SIDE;
        ColonyPos corner = corner();
        int floorY = bottomFloorY(corner);

        return new ColonyPos(
                corner.x() + along.offsetX() * row + gallery.offsetX() * sideways,
                floorY + 1 + layer,
                corner.z() + along.offsetZ() * row + gallery.offsetZ() * sideways);
    }

    /** De quantos em quantos blocos vai a tocha de chão do salão (autor, 2026-10-27). */
    public static final int LIGHT_SPACING = 12;

    /**
     * Onde vão as tochas de chão deste ramal, nos pés: uma grade de {@link #LIGHT_SPACING} no salão do
     * nível e no do ramal, nunca no degrau. A grade fica a 6 do meio de cada salão, longe do caracol do
     * nível seguinte (que desce do meio do salão do ramal) e dos cantos de onde saem os ramais. A rampa do
     * fundo não leva tocha.
     */
    public java.util.List<ColonyPos> lightSpots() {
        if (isRamp()) {
            return java.util.List.of();
        }

        int off = LIGHT_SPACING / 2;
        Side along = descent.clockwise().opposite();
        Side armAlong = gallery.clockwise().opposite();
        ColonyPos hall = hallCenter();
        ColonyPos arm = armHallCenter();
        java.util.List<ColonyPos> spots = new java.util.ArrayList<>();

        for (int a : new int[] {-off, off}) {
            for (int b : new int[] {-off, off}) {
                spots.add(new ColonyPos(hall.x() + along.offsetX() * a + descent.offsetX() * b, hall.y() + 1,
                        hall.z() + along.offsetZ() * a + descent.offsetZ() * b));
                spots.add(new ColonyPos(arm.x() + armAlong.offsetX() * a + gallery.offsetX() * b, arm.y() + 1,
                        arm.z() + armAlong.offsetZ() * a + gallery.offsetZ() * b));
            }
        }

        return spots;
    }

    /** O meio do salão do ramal, piso: de onde o nível seguinte desce. */
    private ColonyPos armHallCenter() {
        Side along = gallery.clockwise().opposite();
        ColonyPos corner = corner();

        return new ColonyPos(
                corner.x() + along.offsetX() * (HALF + 1) + gallery.offsetX() * HALF,
                bottomFloorY(corner),
                corner.z() + along.offsetZ() * (HALF + 1) + gallery.offsetZ() * HALF);
    }

    private int lowestPlannedY() {
        return bottomFloorY(corner()) + 1;
    }
}
