package com.villagecolony.core.coordination;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.colony.service.ColonyService;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ColonyPos;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * O que faz a vila crescer — decisão do autor, 2026-09-30, ADR-003 Emenda 6.
 *
 * <p>Só o que a colônia constrói ou abre: construção registrada, obra aberta
 * (o lote) e rua assentada. Mina, escada de fuga, bosque e cama solta não
 * entram — um túnel de mineiro arrastaria a vila para o lado.
 */
public final class VillageGrowth {

    private VillageGrowth() {
    }

    /**
     * Construção ou lote: a borda fica {@link VillageBounds#GROWTH_MARGIN}
     * além da caixa da peça.
     *
     * @return a caixa nova, quando a vila cresceu
     */
    public static Optional<VillageBounds> byPiece(ColonyService colonies, Building piece) {
        Objects.requireNonNull(piece, "piece");

        return grow(colonies, piece.colonyId(), VillageBounds.aroundPiece(piece.min(), piece.max()));
    }

    /**
     * Rua: a caixa passa a conter cada bloco assentado.
     *
     * @return a caixa nova, quando a vila cresceu
     */
    public static Optional<VillageBounds> byRoad(
            ColonyService colonies, UUID colonyId, Collection<ColonyPos> laid) {

        Objects.requireNonNull(laid, "laid");

        Optional<VillageBounds> grown = Optional.empty();

        for (ColonyPos block : laid) {
            Optional<VillageBounds> step = grow(colonies, colonyId, VillageBounds.block(block));

            if (step.isPresent()) {
                grown = step;
            }
        }

        return grown;
    }

    private static Optional<VillageBounds> grow(
            ColonyService colonies, UUID colonyId, VillageBounds piece) {

        Objects.requireNonNull(colonies, "colonies");

        Optional<Colony> colony = colonies.find(colonyId);

        if (colony.isEmpty() || !colony.get().grow(piece)) {
            return Optional.empty();
        }

        return colony.get().bounds();
    }
}
