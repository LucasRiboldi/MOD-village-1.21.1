package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.BlueprintKind;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.integration.VillageFarms;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * As roças da colônia — Regra 52 (autor, 2026-10-08): <i>"cada fazendeiro deve
 * ter uma fazenda, a fazenda deve entrar na propriedade da obra"</i>.
 *
 * <p>São três origens, contadas juntas: a roça que a vila gerada já tinha, a que
 * a colônia levantou, e a obra de roça em andamento (para a vaga não abrir duas
 * vezes). Só as de pé recebem dono ({@link FarmOwners}).
 */
public final class ColonyFarms {

    /** Uma roça e de onde ela veio. */
    public record Farm(ResourceId plan, BlockBox box, Source source) {

        /** Se já há o que colher ou semear nela. */
        public boolean standing() {
            return source != Source.UNDER_CONSTRUCTION;
        }
    }

    public enum Source { VILLAGE, BUILT, UNDER_CONSTRUCTION }

    /** Sem vila medida, a área da roça da vila é o raio da detecção. */
    private static final int UNMEASURED_RADIUS = 64;

    private ColonyFarms() {
    }

    /** Todas as roças da colônia: da vila, construídas e em obra. */
    public static List<Farm> of(ServerWorld world, Colony colony) {
        List<Farm> farms = new ArrayList<>();

        for (VillageFarms.Farm farm : VillageFarms.within(world, colony.id(), areaOf(colony))) {
            farms.add(new Farm(farm.plan(), farm.box(), Source.VILLAGE));
        }

        for (Building building : VillageColonyMod.BUILDINGS.ofColony(colony.id())) {
            if (building.finished() && BlueprintKind.isFarm(building.blueprint())) {
                farms.add(new Farm(building.blueprint(), boxOf(building), Source.BUILT));
            }
        }

        for (ConstructionProject project : VillageColonyMod.CONSTRUCTIONS.ofColony(colony.id())) {
            if (project.state() != ConstructionState.COMPLETED
                    && BlueprintKind.isFarm(project.blueprint().id())) {

                Building site = Building.of(project, false);
                farms.add(new Farm(site.blueprint(), boxOf(site), Source.UNDER_CONSTRUCTION));
            }
        }

        return List.copyOf(farms);
    }

    /** Quantos fazendeiros a colônia tem. */
    public static int farmers(UUID colonyId) {
        return (int) VillageColonyMod.WORKERS.ofColony(colonyId).stream()
                .filter(worker -> worker.profession().filter(ProfessionType.FARMER::equals).isPresent())
                .count();
    }

    /** Se algum fazendeiro ainda não tem roça — a vez da roça no rodízio. */
    public static boolean owedToTheFarmers(ServerWorld world, Colony colony) {
        return owed(of(world, colony).size(), farmers(colony.id()));
    }

    /** A conta da Regra 52: uma roça por fazendeiro. */
    static boolean owed(int farms, int farmers) {
        return farms < farmers;
    }

    private static BlockBox areaOf(Colony colony) {
        return colony.bounds()
                .map(ColonyFarms::boxOf)
                .orElseGet(() -> new BlockBox(
                        colony.center().x() - UNMEASURED_RADIUS, colony.center().y() - 32,
                        colony.center().z() - UNMEASURED_RADIUS,
                        colony.center().x() + UNMEASURED_RADIUS, colony.center().y() + 32,
                        colony.center().z() + UNMEASURED_RADIUS));
    }

    private static BlockBox boxOf(VillageBounds bounds) {
        return new BlockBox(bounds.minX(), bounds.minY(), bounds.minZ(),
                bounds.maxX(), bounds.maxY(), bounds.maxZ());
    }

    static BlockBox boxOf(Building building) {
        return new BlockBox(building.min().x(), building.min().y(), building.min().z(),
                building.max().x(), building.max().y(), building.max().z());
    }
}
