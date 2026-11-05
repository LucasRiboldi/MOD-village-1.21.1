package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.MineHomeSiteSearch;
import com.villagecolony.fabric.integration.VillageOrigin;
import com.villagecolony.fabric.world.GeneratedVillages;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/** A primeira casa junto à boca da mina pertence somente a vilas novas — ADR-043. */
final class MineHomePlans {
    static final int RADIUS = 20;

    /** O modelo, gerado por {@code scripts/gen_miner_house.py} — Regra 175. */
    static final String MODEL = "miner_house";

    private MineHomePlans() {
    }

    static boolean isOwed(ServerWorld world, Colony colony) {
        var mine = VillageColonyMod.MINES.of(colony.id());
        if (!isCurrentVillage(world, colony.center()) || mine.isEmpty()) {
            return false;
        }
        BlockPos mouth = MinecraftTypeAdapter.toBlockPos(mine.orElseThrow().shaft().entry());
        return VillageColonyMod.BUILDINGS.ofColony(colony.id()).stream()
                .filter(building -> HousePlans.isDwelling(building.blueprint()) || isMinerHouse(building.blueprint()))
                .noneMatch(building -> near(mouth, building));
    }

    /** A casa pequena do mineiro — Regra 175: a do estilo da vila, ou a de todos os estilos. */
    static List<Blueprint> planFor(ServerWorld world, Colony colony) {
        String style = HousePlans.paletteOf(world, colony.center()).style();

        for (String path : List.of("colony/" + style + "/" + MODEL, "colony/" + MODEL)) {
            var plan = com.villagecolony.fabric.integration.StructureBlueprintReader.read(world,
                    new com.villagecolony.core.type.ResourceId("villagecolony", path));

            if (plan.isPresent()) {
                return List.of(plan.get());
            }
        }

        return List.of();
    }

    static boolean isMinerHouse(com.villagecolony.core.type.ResourceId id) {
        return "villagecolony".equals(id.namespace()) && id.path().endsWith("/" + MODEL);
    }

    static MineHomeSiteSearch.Result findSite(ServerWorld world, Colony colony, List<Blueprint> houses) {
        BlockPos mouth = MinecraftTypeAdapter.toBlockPos(VillageColonyMod.MINES.of(colony.id()).orElseThrow().shaft().entry());
        return MineHomeSiteSearch.find(world, colony.id(), MinecraftTypeAdapter.toColonyPos(mouth), RADIUS,
                SiteOpening.footprintsFor(houses));
    }

    static boolean near(BlockPos mouth, Building building) {
        return mouth.getSquaredDistance(MinecraftTypeAdapter.toBlockPos(building.min())) <= RADIUS * RADIUS
                || mouth.getSquaredDistance(MinecraftTypeAdapter.toBlockPos(building.max())) <= RADIUS * RADIUS;
    }

    static boolean isCurrentVillage(ServerWorld world, ColonyPos center) {
        return GeneratedVillages.villagesReaching(world, center.x() >> 4, center.z() >> 4).stream()
                .anyMatch(VillageOrigin::isCurrent);
    }
}
