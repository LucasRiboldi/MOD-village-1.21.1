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

    private record Attempt(UUID projectId, UUID buildingId, int standing) {
    }

    private static final Map<UUID, Attempt> ACTIVE = new HashMap<>();
    private static final Map<UUID, Set<UUID>> SKIP_ONCE = new HashMap<>();

    /**
     * As construções cuja lacuna não fecha — 2026-09-22, visto em jogo.
     *
     * <p>O log do autor tinha a mesma casa reabrindo a cada trinta segundos:
     * <i>9 blocks remain</i> na abertura e <i>0 blocks placed</i> no fim,
     * sem parar. Os nove eram {@code ladder} e {@code wall_torch}, e
     * {@code BuilderWork.placeOne} os risca por <i>nothing holds it</i> sem
     * assentar nada — são peças que pedem apoio que aquela parede não tem.
     *
     * <p>A obra então é dada por terminada, a casa entra no registro com a
     * lacuna intacta, e a varredura seguinte reencontra exatamente os mesmos
     * nove. A vaga de obra da colônia é única, então esse laço não era só
     * ruído: <b>ele impedia qualquer construção nova de nascer</b>.
     *
     * <p>Uma tentativa que termina sem aumentar o número de blocos de pé não
     * ganha outra. Isto não desliga o reparo — lacuna que o construtor
     * consegue fechar continua sendo fechada, e quem prova isso é
     * {@code FoundationRepairGameTest.anIncompleteProfessionHouseStillStartsRepair}.
     * O que acaba é a insistência no que não se fecha.
     */
    private static final Map<UUID, Set<UUID>> EXHAUSTED = new HashMap<>();

    private BuildingRepairPlanner() {
    }

    static Optional<ConstructionProject> open(ServerWorld world, Colony colony) {
        observeClosedAttempts(world, colony);

        Set<UUID> exhausted = EXHAUSTED.getOrDefault(colony.id(), Set.of());

        for (Building building : VillageColonyMod.BUILDINGS.ofColony(colony.id())) {
            if (building.blueprint().equals(StructureBlueprintReader.BIG_HOUSE_MOD)
                    || exhausted.contains(building.id())) {
                continue;
            }

            Optional<Blueprint> blueprint = PlanPlacement.blueprintOf(
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
            ACTIVE.put(colony.id(), new Attempt(repair.id(), building.id(), standing.size()));

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

    private static void observeClosedAttempts(ServerWorld world, Colony colony) {
        Attempt attempt = ACTIVE.get(colony.id());

        if (attempt == null) {
            return;
        }

        Optional<ConstructionProject> project =
                VillageColonyMod.CONSTRUCTIONS.find(attempt.projectId());

        if (project.isPresent() && project.get().state().isOpen()) {
            return;
        }

        ACTIVE.remove(colony.id());

        if (madeNoProgress(world, colony, attempt)) {
            EXHAUSTED.computeIfAbsent(colony.id(), ignored -> new HashSet<>())
                    .add(attempt.buildingId());

            VillageColonyMod.LOGGER.info(
                    "Colony {} gives up repairing building {} — the attempt closed with"
                            + " {} blocks standing, the same as when it opened."
                            + " The gap needs pieces the builder cannot place",
                    colony.id(),
                    attempt.buildingId(),
                    attempt.standing());

            return;
        }

        if (project.isEmpty()) {
            SKIP_ONCE.computeIfAbsent(colony.id(), ignored -> new HashSet<>())
                    .add(attempt.buildingId());
        }
    }

    /**
     * Se a tentativa fechou sem levantar um bloco sequer.
     *
     * <p>A conta é feita no mundo, e não no projeto: o que interessa é se a
     * casa tem mais peça de pé do que tinha, e o projeto pode ter riscado
     * peças que nunca encostaram em nada. Ver {@link #EXHAUSTED}.
     *
     * <p>Uma construção que sumiu do registro entre a abertura e agora não é
     * caso disto — sem casa não há lacuna que insista.
     */
    private static boolean madeNoProgress(ServerWorld world, Colony colony, Attempt attempt) {
        for (Building building : VillageColonyMod.BUILDINGS.ofColony(colony.id())) {
            if (!building.id().equals(attempt.buildingId())) {
                continue;
            }

            Optional<Blueprint> blueprint = PlanPlacement.blueprintOf(
                    world, colony.id(), building.blueprint(), building.min());

            return blueprint.isPresent()
                    && standingBlocks(world, blueprint.get(), building.min()).size()
                            <= attempt.standing();
        }

        return false;
    }

    static void clearAll() {
        ACTIVE.clear();
        SKIP_ONCE.clear();
        EXHAUSTED.clear();
    }
}
