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

    /** O lado oposto a este. */
    public Side opposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case EAST -> WEST;
            case WEST -> EAST;
        };
    }
}
