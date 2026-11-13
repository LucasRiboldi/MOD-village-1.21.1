package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.MineHomeSiteSearch;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/**
 * A casa do mineiro ao lado da boca da mina. A boca é a da superfície na mina de hoje (Regra 211), e uma casa pronta
 * basta: a boca que muda não pede outra (Regra 266).
 */
final class MineHomePlans {
    static final int RADIUS = 20;

    /** O modelo, gerado por {@code scripts/gen_miner_house.py} — Regra 175. */
    static final String MODEL = "miner_house";

    private MineHomePlans() {
    }

    static boolean isOwed(ServerWorld world, Colony colony) {
        var mine = VillageColonyMod.MINES.of(colony.id());
        if (mine.isEmpty()) {
            return false;
        }
        return isOwedAt(colony.id());
    }

    /** Falta casa do mineiro na vila: uma pronta basta, mesmo que a boca da mina mude (Regra 266, emenda a 211). */
    static boolean isOwedAt(UUID colonyId) {
        return VillageColonyMod.BUILDINGS.ofColony(colonyId).stream().noneMatch(building -> isMinerHouse(building.blueprint()));
    }

    private static BlockPos surfaceMouth(ServerWorld world, Colony colony) {
        var mine = VillageColonyMod.MINES.of(colony.id()).orElseThrow();

        return MinecraftTypeAdapter.toBlockPos(MineEntrance.surfaceOf(world, colony.id(), mine));
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
        BlockPos mouth = surfaceMouth(world, colony);
        return MineHomeSiteSearch.find(world, colony.id(), MinecraftTypeAdapter.toColonyPos(mouth), RADIUS,
                SiteOpening.footprintsFor(houses));
    }

    static boolean near(BlockPos mouth, Building building) {
        // A busca anda em anéis quadrados: a mesma régua aqui, da boca até a borda da caixa.
        int gapX = Math.max(0, Math.max(building.min().x() - mouth.getX(), mouth.getX() - building.max().x()));
        int gapZ = Math.max(0, Math.max(building.min().z() - mouth.getZ(), mouth.getZ() - building.max().z()));

        return Math.max(gapX, gapZ) <= RADIUS;
    }

}
