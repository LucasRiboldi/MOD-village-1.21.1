package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BuildSiteScanner.Footprint;
import com.villagecolony.fabric.integration.BuildSiteScanner.Site;
import com.villagecolony.fabric.integration.SweepState.Sweep;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Busca perto da mina sem recentrar a varredura da vila — ADR-043. */
public final class MineHomeSiteSearch {
    private static final Map<UUID, Sweep> CURSORS = new HashMap<>();

    static {
        ServerMemory.register(MineHomeSiteSearch.class, CURSORS::clear);
    }

    private MineHomeSiteSearch() {
    }

    /** Vazio só libera outras obras quando a busca local terminou. */
    public record Result(Optional<Site> site, boolean complete) {
    }

    public static Result find(ServerWorld world, UUID colonyId, ColonyPos mouth,
                              int radius, List<Footprint> footprints) {
        BlockPos from = MinecraftTypeAdapter.toBlockPos(mouth);
        Sweep cursor = CURSORS.get(colonyId);
        if (cursor == null || !cursor.from().equals(mouth)) {
            cursor = new Sweep(0, 0, mouth);
        }
        int columns = 0;

        for (int ring = cursor.ring(); ring <= radius; ring++) {
            int inRing = 0;
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.abs(dx) != ring && Math.abs(dz) != ring) {
                        dz = ring - 1;
                        continue;
                    }
                    int column = inRing++;
                    if (ring == cursor.ring() && column < cursor.column()) {
                        continue;
                    }
                    if (++columns > BuildSiteScanner.MAX_COLUMNS || SweepDeadline.expired(columns)) {
                        CURSORS.put(colonyId, new Sweep(ring, column, mouth));
                        return new Result(Optional.empty(), false);
                    }
                    Optional<Site> site = siteAround(world, colonyId, from,
                            from.getX() + dx, from.getZ() + dz, footprints);
                    if (site.isPresent()) {
                        CURSORS.put(colonyId, new Sweep(ring, column + 1, mouth));
                        return new Result(site, false);
                    }
                }
            }
        }

        CURSORS.remove(colonyId);
        VillageColonyMod.LOGGER.info("VC_MINE_HOME colony={} radius={}: local search complete without lot; "
                + "home remains pending, allowing other construction", colonyId, radius);
        return new Result(Optional.empty(), true);
    }

    /**
     * O lote com a porta virada para esta coluna de chão natural, por onde o caminho vai passar (Regra 175): a boca da
     * mina fica longe das ruas, e exigir rua ao lado não achava lote nunca. Mesmas recusas do lote à beira da rua, e
     * fora da folga da escada da mina.
     */
    private static Optional<Site> siteAround(ServerWorld world, UUID colonyId, BlockPos mouth, int x, int z,
                                             List<Footprint> footprints) {
        Optional<BlockPos> ground = LotGround.groundInColumn(world, x, z, mouth.getY());

        if (ground.isEmpty() || !LotGround.isNaturalGround(world.getBlockState(ground.get()))
                || !world.getBlockState(ground.get().up()).isReplaceable()) {
            return Optional.empty();
        }

        int y = ground.get().getY();

        for (Footprint footprint : footprints) {
            Optional<Site> site = RoadsideSites.siteFor(world, colonyId, x, z, y, y, footprint.size(), footprint.doorSide())
                    .filter(found -> clearOfTheShaft(found, footprint, mouth));

            if (site.isPresent()) {
                return site;
            }
        }

        return Optional.empty();
    }

    private static boolean clearOfTheShaft(Site site, Footprint footprint, BlockPos mouth) {
        int clearance = WorldPathTerrain.MINE_CLEARANCE;

        return site.origin().x() > mouth.getX() + clearance
                || site.origin().x() + footprint.size().x() < mouth.getX() - clearance
                || site.origin().z() > mouth.getZ() + clearance
                || site.origin().z() + footprint.size().z() < mouth.getZ() - clearance;
    }
}
