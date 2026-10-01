package com.villagecolony.core.coordination;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageCandidate;
import com.villagecolony.core.colony.service.ColonyService;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.service.BuildingRegistry;
import com.villagecolony.core.type.ColonyPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * A que colônia um aglomerado de camas pertence — E51, 2026-09-30.
 *
 * <p><b>O defeito.</b> A identidade da ADR-003 (passo 6) só perguntava se o
 * centro do aglomerado estava a até {@link VillageDetector#DUPLICATE_DISTANCE}
 * do centro de uma colônia. Uma vila gerada pode passar de 128 blocos de ponta
 * a ponta, e o aglomerado da ponta virava colônia nova: no playtest de 30-09
 * cada uma ergueu uma BigHouseMOD e criou 7 adultos, e só a fusão do fim do
 * ciclo as absorvia. A ADR-003 §5 já mandava o contrário — vila partida em
 * dois aglomerados continua uma colônia só.
 *
 * <p><b>Três perguntas, na ordem.</b> Centro perto (a regra de antes); a
 * mesma vila gerada pelo jogo, que a camada Fabric responde; ou uma cama do
 * aglomerado a até {@link #NEIGHBOUR_GAP} blocos de uma construção da
 * colônia — a mesma régua que junta duas camas no mesmo aglomerado.
 */
public final class ColonyIdentity {

    /**
     * A folga horizontal que ainda é a mesma vila: a distância entre camas do
     * mesmo aglomerado, ADR-003 passo 2.
     */
    public static final int NEIGHBOUR_GAP = VillageDetector.CLUSTER_DISTANCE;

    /** Por que o aglomerado é desta colônia. */
    public enum Reason {
        NEAR_ITS_CENTER,
        SAME_GENERATED_VILLAGE,
        NEAR_ITS_BUILDINGS
    }

    /** A dona e o motivo. */
    public record Owner(Colony colony, Reason reason) {

        public Owner {
            Objects.requireNonNull(colony, "colony");
            Objects.requireNonNull(reason, "reason");
        }
    }

    private ColonyIdentity() {
    }

    /**
     * A colônia dona do aglomerado, ou vazio quando ele é uma vila nova.
     *
     * @param sameGeneratedVillage responde se a colônia ocupa a mesma vila
     *     gerada que o aglomerado; o Core não conhece estrutura de mundo
     */
    public static Optional<Owner> ownerOf(
            VillageCandidate candidate,
            ColonyService colonies,
            BuildingRegistry buildings,
            Predicate<Colony> sameGeneratedVillage) {

        Objects.requireNonNull(candidate, "candidate");
        Objects.requireNonNull(colonies, "colonies");
        Objects.requireNonNull(buildings, "buildings");
        Objects.requireNonNull(sameGeneratedVillage, "sameGeneratedVillage");

        Optional<Colony> nearest =
                colonies.findNearest(candidate.center(), VillageDetector.DUPLICATE_DISTANCE);

        if (nearest.isPresent()) {
            return Optional.of(new Owner(nearest.get(), Reason.NEAR_ITS_CENTER));
        }

        for (Colony colony : colonies.all()) {
            if (sameGeneratedVillage.test(colony)) {
                return Optional.of(new Owner(colony, Reason.SAME_GENERATED_VILLAGE));
            }
        }

        List<ColonyPos> points = new ArrayList<>(candidate.beds());
        points.add(candidate.center());

        for (Building building : buildings.all()) {
            Optional<Colony> colony = colonies.find(building.colonyId());

            if (colony.isEmpty()) {
                continue;
            }

            for (ColonyPos point : points) {
                if (isNear(gapSquared(point, point, building), NEIGHBOUR_GAP)) {
                    return Optional.of(new Owner(colony.get(), Reason.NEAR_ITS_BUILDINGS));
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Se alguma construção de {@code a} fica a até {@link #NEIGHBOUR_GAP} de
     * uma de {@code b} — "a poucos blocos uma da outra", pedido do autor em
     * 30-09, ao lado do "encostar" da ADR-007 §4.
     */
    public static boolean buildingsNear(UUID a, UUID b, BuildingRegistry buildings) {
        Objects.requireNonNull(buildings, "buildings");

        for (Building mine : buildings.ofColony(a)) {
            for (Building theirs : buildings.ofColony(b)) {
                if (isNear(gapSquared(mine.min(), mine.max(), theirs), NEIGHBOUR_GAP)) {
                    return true;
                }
            }
        }

        return false;
    }

    // A folga horizontal entre a caixa [min, max] e a construção, ao quadrado;
    // zero quando uma alcança a outra.
    private static long gapSquared(ColonyPos min, ColonyPos max, Building building) {
        long dx = Math.max(0, Math.max(building.min().x() - max.x(), min.x() - building.max().x()));
        long dz = Math.max(0, Math.max(building.min().z() - max.z(), min.z() - building.max().z()));

        return dx * dx + dz * dz;
    }

    private static boolean isNear(long gapSquared, int gap) {
        return gapSquared <= (long) gap * gap;
    }
}
