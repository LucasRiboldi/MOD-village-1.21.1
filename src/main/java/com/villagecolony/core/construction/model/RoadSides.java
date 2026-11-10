package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;

/**
 * De que lado da vila fica uma coluna — Regra 229 (autor, 2026-11-10): a ponta azul é do lado norte, sul, leste ou oeste,
 * pelo eixo em que ela mais se afasta do centro, e cresce para fora por esse lado. Puro.
 */
public final class RoadSides {

    private RoadSides() {
    }

    /** O lado da coluna em relação ao centro; no empate, o leste ou o oeste. */
    public static Side of(int x, int z, ColonyPos center) {
        int dx = x - center.x();
        int dz = z - center.z();

        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx >= 0 ? Side.EAST : Side.WEST;
        }

        return dz >= 0 ? Side.SOUTH : Side.NORTH;
    }
}
