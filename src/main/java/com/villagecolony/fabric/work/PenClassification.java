package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.work.ShepherdPens.Pen;
import com.villagecolony.fabric.world.GeneratedVillages;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;

import java.util.List;
import java.util.Optional;

/** Identifica a origem e o modelo dos currais encontrados no terreno (Regra 72). */
final class PenClassification {
    private PenClassification() {}

    static List<Pen> of(ServerWorld world, Colony colony, List<Pen> pens) {
        List<Building> built = VillageColonyMod.BUILDINGS.ofColony(colony.id()).stream()
                .filter(building -> building.finished() && PenPlans.isPen(building.blueprint())).toList();
        List<BlockBox> generated = GeneratedVillages.piecesAt(world, colony.center().x(), colony.center().z(), PenPlans.TYPE);
        return pens.stream().map(pen -> classify(pen, built, generated)).toList();
    }

    private static Pen classify(Pen pen, List<Building> built, List<BlockBox> generated) {
        ColonyPos gate = MinecraftTypeAdapter.toColonyPos(pen.gate());
        Optional<ResourceId> model = built.stream().filter(building -> building.contains(gate))
                .map(Building::blueprint).findFirst();
        boolean large = model.filter(PenPlans::isLargePen).isPresent();
        var theme = model.flatMap(MediumPenType::fromModel);
        boolean vanilla = model.map(id -> ResourceId.VANILLA.equals(id.namespace())).orElseGet(
                () -> generated.stream().anyMatch(box -> box.contains(pen.gate())));
        return new Pen(pen.gate(), pen.inside(), pen.outside(), pen.reach(), large, pen.accesses(), theme, vanilla);
    }
}
