package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;

import java.util.List;
import java.util.Objects;

/** Posições dos pés na passagem candidata, sem prova física ou autorização de escrita — ADR-046. */
public record BlueprintAccess(Kind kind, List<ColonyPos> opening, Side outward) {

    public enum Kind { DOOR, GATE, OPENING }

    public BlueprintAccess {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(outward, "outward");
        opening = List.copyOf(opening);
        if (opening.isEmpty()) {
            throw new IllegalArgumentException("Access opening must not be empty");
        }
    }
}
