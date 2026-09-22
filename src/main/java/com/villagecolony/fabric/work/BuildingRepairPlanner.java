package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import net.minecraft.block.Block;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Varre construções do mod que ficaram incompletas e abre uma tentativa
 * limitada de reparo antes de aceitar uma obra nova.
 *
 * <p>A construção registrada é a lista de candidatos; a planta e o mundo
 * decidem os blocos. Uma tentativa abandonada é pulada uma vez, para que
 * material ou acesso impossível não congele a vila. No ciclo seguinte,
 * depois que a vaga teve oportunidade de seguir, ela volta à varredura.
 */
final class BuildingRepairPlanner {

    private record Attempt(UUID projectId, UUID buildingId) {
    }

    private static final Map<UUID, Attempt> ACTIVE = new HashMap<>();
    private static final Map<UUID, Set<UUID>> SKIP_ONCE = new HashMap<>();

    private BuildingRepairPlanner() {
    }

    static Optional<ConstructionProject> open(ServerWorld world, Colony colony) {
        observeClosedAttempts(colony.id());

        for (Building building : VillageColonyMod.BUILDINGS.ofColony(colony.id())) {
            if (building.blueprint().equals(StructureBlueprintReader.BIG_HOUSE_MOD)) {
                continue;
            }

            Optional<Blueprint> blueprint = HousePlans.blueprintOf(
                    world, colony.id(), building.blueprint(), building.min());

            if (blueprint.isEmpty()) {
                continue;
            }

            Set<UUID> skipped = SKIP_ONCE.get(colony.id());

            if (skipped != null && skipped.remove(building.id())) {
                if (skipped.isEmpty()) {
                    SKIP_ONCE.remove(colony.id());
                }
                continue;
            }

            Set<ColonyPos> standing = standingBlocks(
                    world, blueprint.get(), building.min());

            if (standing.size() == blueprint.get().blockCount()) {
                continue;
            }

            ConstructionProject repair = ConstructionProject.repair(
                    colony.id(), blueprint.get(), building.min(), standing);

            VillageColonyMod.CONSTRUCTIONS.register(repair);
            ACTIVE.put(colony.id(), new Attempt(repair.id(), building.id()));

            VillageColonyMod.LOGGER.info(
                    "Colony {} starts repair sweep for {} at {} — {} blocks remain",
                    colony.id(), building.blueprint(), building.min(), repair.remainingCount());

            return Optional.of(repair);
        }

        return Optional.empty();
    }

    private static Set<ColonyPos> standingBlocks(
            ServerWorld world, Blueprint blueprint, ColonyPos origin) {

        Set<ColonyPos> standing = new HashSet<>();

        for (BlueprintBlock block : blueprint.blocks()) {
            Optional<Block> expected = MinecraftTypeAdapter.toBlock(block.block());

            if (expected.isEmpty()) {
                continue;
            }

            BlockPos position = new BlockPos(
                    origin.x() + block.offset().x(),
                    origin.y() + block.offset().y(),
                    origin.z() + block.offset().z());

            if (!world.isChunkLoaded(position.getX() >> 4, position.getZ() >> 4)) {
                continue;
            }

            if (world.getBlockState(position).isOf(expected.get())) {
                standing.add(MinecraftTypeAdapter.toColonyPos(position));
            }
        }

        return standing;
    }

    private static void observeClosedAttempts(UUID colonyId) {
        Attempt attempt = ACTIVE.get(colonyId);

        if (attempt == null) {
            return;
        }

        Optional<ConstructionProject> project =
                VillageColonyMod.CONSTRUCTIONS.find(attempt.projectId());

        if (project.isPresent() && project.get().state().isOpen()) {
            return;
        }

        ACTIVE.remove(colonyId);

        if (project.isEmpty()) {
            SKIP_ONCE.computeIfAbsent(colonyId, ignored -> new HashSet<>())
                    .add(attempt.buildingId());
        }
    }

    static void clearAll() {
        ACTIVE.clear();
        SKIP_ONCE.clear();
    }
}
