package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ResourceId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * As últimas peças sem apoio que a obra larga para fechar — playtest 0.3.31.
 *
 * <p>Peça adiada por {@link SkipReason#UNSUPPORTED} espera o apoio mudar, e a obra
 * que só tem essas peças espera junto, com a vaga única de obra da colônia presa: a
 * {@code plains_medium_house_1} ficou 25 minutos com uma tocha de parede sem parede
 * e "no build task", e de novo 3 minutos com duas na sessão seguinte.
 *
 * <p>Larga só quando <b>todas</b> as que restam são sem apoio, todas estão adiadas e
 * são no máximo {@link #MOST}: tocha, lanterna, escada de mão de enfeite. Casa a que
 * falta parede não fecha por aqui.
 */
public final class UnsupportedRelease {

    /** Quantas peças sem apoio, no máximo, a obra larga para fechar. */
    public static final int MOST = 4;

    private UnsupportedRelease() {
    }

    /**
     * Tira da obra as peças sem apoio, se forem as únicas que restam e poucas.
     *
     * @return as peças largadas; vazio quando a obra não está nesse caso
     */
    public static List<ResourceId> release(ConstructionProject project) {
        Objects.requireNonNull(project, "project");

        List<ConstructionProject.DeferredPiece> deferred = project.deferredPieces();

        if (deferred.isEmpty()
                || deferred.size() > MOST
                || project.remainingCount() != deferred.size()
                || project.nextBlock().isPresent()
                || deferred.stream().anyMatch(piece -> piece.reason() != SkipReason.UNSUPPORTED)) {
            return List.of();
        }

        List<ResourceId> released = new ArrayList<>();

        for (BlueprintBlock block : project.remaining()) {
            released.add(block.block());
            project.markPlaced(block);
        }

        return List.copyOf(released);
    }
}
