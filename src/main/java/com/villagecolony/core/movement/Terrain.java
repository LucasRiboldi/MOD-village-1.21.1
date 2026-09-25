package com.villagecolony.core.movement;

import com.villagecolony.core.type.ColonyPos;

/** O mundo como o planejador o vê — uma {@link Cell} por posição. */
@FunctionalInterface
public interface Terrain {

    Cell at(ColonyPos pos);
}
