package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.server.world.ServerWorld;

import java.util.Optional;
import java.util.UUID;

/** Reconstitui uma planta lida do mundo na orientacao da rua atual. */
public final class BlueprintPlacement {

    private BlueprintPlacement() {
    }

    public static Optional<Blueprint> blueprintOf(
            ServerWorld world, UUID colonyId, ResourceId id, ColonyPos origin) {
        return StructureBlueprintReader.read(world, id)
                .map(FarmBlueprint::asBuilt)
                .map(blueprint -> blueprint.doorSide()
                        .map(door -> blueprint.rotated(
                                door.turnsTo(roadSideOf(world, colonyId, origin, blueprint))))
                        .orElse(blueprint));
    }

    public static Side roadSideOf(
            ServerWorld world, UUID colonyId, ColonyPos origin, Blueprint blueprint) {
        return RoadIndex.roadSideOf(world, colonyId, origin, blueprint.size())
                .map(MinecraftTypeAdapter::toSide)
                .orElse(Side.NORTH);
    }
}
