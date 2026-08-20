package com.villagecolony.core.type;

/**
 * Um dos quatro lados do horizonte.
 *
 * <p>Tipo próprio do Core, como {@link ColonyPos}: a planta precisa
 * saber para que lado a casa se abre, e nenhuma classe daqui conhece
 * {@code Direction} do jogo. A conversão mora em
 * {@code fabric.adapter.MinecraftTypeAdapter}. Ver ADR-005.
 *
 * <p>Só o horizonte. Cima e baixo não são lado de casa, e incluí-los
 * obrigaria todo {@code switch} sobre este tipo a tratar dois casos que
 * nunca acontecem.
 */
public enum Side {

    NORTH,

    SOUTH,

    EAST,

    WEST;

    /**
     * Quantos quartos de volta, no sentido horário, deste lado até o
     * outro.
     *
     * <p>Serve à Regra 17 com planta lida do jogo: a casa vem com a
     * porta num lado que o arquivo escolheu, e girar a planta é a única
     * forma de pô-la na rua. Norte, leste, sul, oeste — nesta ordem, que
     * é a do relógio visto de cima.
     */
    public int turnsTo(Side other) {
        return Math.floorMod(clockwise(other) - clockwise(this), 4);
    }

    private static int clockwise(Side side) {
        return switch (side) {
            case NORTH -> 0;
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
        };
    }

    /** O lado oposto a este. */
    /**
     * Quanto este lado anda em x — a mesma convenção do jogo.
     *
     * <p>Norte é z negativo, leste é x positivo. Escrito aqui porque a
     * mina precisa andar em linha reta e {@code core} não conhece
     * {@code Direction}.
     */
    public int offsetX() {
        return switch (this) {
            case EAST -> 1;
            case WEST -> -1;
            case NORTH, SOUTH -> 0;
        };
    }

    /** Quanto este lado anda em z. */
    public int offsetZ() {
        return switch (this) {
            case SOUTH -> 1;
            case NORTH -> -1;
            case EAST, WEST -> 0;
        };
    }

    /** O lado à direita deste, olhando de cima. */
    public Side clockwise() {
        return switch (this) {
            case NORTH -> EAST;
            case EAST -> SOUTH;
            case SOUTH -> WEST;
            case WEST -> NORTH;
        };
    }

    public Side opposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case EAST -> WEST;
            case WEST -> EAST;
        };
    }
}
