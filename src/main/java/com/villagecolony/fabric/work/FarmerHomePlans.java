package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.integration.MineHomeSiteSearch;
import net.minecraft.server.world.ServerWorld;

import java.util.List;
import java.util.Optional;

/**
 * A casa do fazendeiro perto da plantação grande — Regra 382 (autor 26-11, K2 do jogo da 0.4.148: "adiciona a necessidade
 * de construir a casa do fazendeiro perto da fazenda grande"). Com a plantação grande pronta e nenhuma casa de morar a até
 * {@value #RADIUS} blocos dela, a casa da vila é procurada em anéis a partir do meio da plantação, como a casa do mineiro
 * em volta da boca da mina; o fazendeiro passa a dormir nela pela troca de cama da Regra 74.
 */
final class FarmerHomePlans {

    /** Até onde da plantação grande a casa do fazendeiro conta como perto. */
    static final int RADIUS = 20;

    private FarmerHomePlans() {
    }

    /** Se a vila deve a casa do fazendeiro: plantação grande pronta e nenhuma casa a até {@value #RADIUS} dela. */
    static boolean isOwed(List<Building> buildings) {
        Optional<Building> farm = largeFarm(buildings);

        return farm.isPresent() && buildings.stream()
                .filter(building -> HousePlans.isHouse(building.blueprint()))
                .noneMatch(house -> gap(house, farm.get()) <= RADIUS);
    }

    /** A casa da vila, em anéis a partir do meio da plantação grande. */
    static MineHomeSiteSearch.Result findSite(ServerWorld world, Colony colony, List<Blueprint> houses) {
        Building farm = largeFarm(VillageColonyMod.BUILDINGS.ofColony(colony.id())).orElseThrow();
        ColonyPos middle = new ColonyPos((farm.min().x() + farm.max().x()) / 2, farm.min().y(),
                (farm.min().z() + farm.max().z()) / 2);

        return MineHomeSiteSearch.find(world, colony.id(), middle, RADIUS, SiteOpening.footprintsFor(houses));
    }

    private static Optional<Building> largeFarm(List<Building> buildings) {
        return buildings.stream()
                .filter(building -> building.finished() && LargeFarmPlans.MODEL.equals(building.blueprint()))
                .findFirst();
    }

    /** A folga, em blocos na horizontal, entre as duas caixas: zero quando se tocam. */
    private static int gap(Building a, Building b) {
        int dx = Math.max(0, Math.max(a.min().x() - b.max().x(), b.min().x() - a.max().x()));
        int dz = Math.max(0, Math.max(a.min().z() - b.max().z(), b.min().z() - a.max().z()));

        return Math.max(dx, dz);
    }
}
