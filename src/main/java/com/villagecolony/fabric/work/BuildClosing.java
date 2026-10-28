package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.coordination.SupplyException;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.Supplies;
import com.villagecolony.fabric.integration.SupplyGapLedger;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A obra nunca fica largada meio pronta — Regra 148 (autor, 2026-10-28; taiga simulada: a casa do pedreiro
 * ficou 28 minutos em "0 blocks left" sem tarefa e foi largada meio pronta).
 *
 * <ul>
 *   <li>Sem peça a pôr, a obra fecha pelo ciclo da colônia ({@link #closeIfDone}) — quem fechava era só o
 *       construtor, e sem peça nenhuma tarefa de obra abria.</li>
 *   <li>Parada no fim — só peças adiadas que o material ou o apoio não deixaram pôr, ou nenhuma peça assentada
 *       pela janela inteira de paciência numa obra já começada —, o que falta surge no lugar
 *       ({@link #finishBySurging}). Cada peça surgida vai para o registro de faltas ({@code supply-gaps.jsonl},
 *       motivo {@value #END_OF_BUILD}), para a rota dela ser corrigida depois.</li>
 * </ul>
 */
final class BuildClosing {

    /** O motivo no registro de faltas: a peça surgiu para fechar a obra. */
    static final String END_OF_BUILD = "END_OF_BUILD";

    private BuildClosing() {
    }

    /** Fecha a obra que não tem mais peça a pôr; diz se fechou. */
    static boolean closeIfDone(ServerWorld world, Colony colony, ConstructionProject project) {
        if (!project.isFinished() || !isOpen(project)) {
            return false;
        }

        complete(colony, project, 0, "every piece is in place");

        return true;
    }

    /** Faz surgir tudo o que falta, registra cada peça e fecha a obra. */
    static void finishBySurging(ServerWorld world, Colony colony, ConstructionProject project) {
        if (!isOpen(project)) {
            return;
        }

        Map<Item, Integer> surged = new LinkedHashMap<>();

        for (BlueprintBlock block : project.remaining()) {
            Optional<Block> material = MinecraftTypeAdapter.toBlock(block.block());
            BlockPos target = MinecraftTypeAdapter.toBlockPos(project.worldPositionOf(block));

            if (material.isPresent() && world.isChunkLoaded(target)) {
                BlockState now = world.getBlockState(target);

                // Bloco que já está ali, ou que não é substituível (peça de vila, coisa do jogador), fica.
                if (!now.isOf(material.get()) && now.isReplaceable()) {
                    BlockState state = BuilderPlacement.shaped(world, project, block, material.get(), target);
                    world.setBlockState(target, state, Block.NOTIFY_ALL);
                    BlockShaping.placeSecondHalf(world, target, state);
                    surged.merge(material.get().asItem(), 1, Integer::sum);
                }
            }

            project.markPlaced(block);
        }

        int total = surged.values().stream().mapToInt(Integer::intValue).sum();

        surged.forEach((item, count) -> {
            SupplyGapLedger.record(world, colony.id(), item, count, ProfessionType.BUILDER, END_OF_BUILD,
                    "the build stopped with this piece missing; it surged so the house closes (Regra 148)");
            Supplies.record(SupplyException.END_OF_BUILD, colony.id(), Registries.ITEM.getId(item).getPath(), count);
        });

        VillageColonyMod.LOGGER.warn("VC_END_OF_BUILD version=1 colony={} building={} at={} surged={} pieces={}",
                colony.id().toString().substring(0, 8), project.blueprint().id(), project.origin(), total, surged);

        complete(colony, project, total, total + " pieces surged at the end");
    }

    private static boolean isOpen(ConstructionProject project) {
        return project.state() == ConstructionState.BUILDING
                || project.state() == ConstructionState.WAITING_RESOURCES;
    }

    private static void complete(Colony colony, ConstructionProject project, int surged, String why) {
        if (project.state() == ConstructionState.WAITING_RESOURCES) {
            project.moveTo(ConstructionState.BUILDING);
        }

        project.moveTo(ConstructionState.COMPLETED);
        VillageColonyMod.BUILDINGS.registerOrMerge(Building.of(project, true));
        WaitingWork.cancelBuildTasks(colony);

        VillageColonyMod.LOGGER.info("Colony {} closes {} at {} — {}",
                colony.id(), project.blueprint().id(), project.origin(), why);
        VillageDiary.buildFinished(project, surged, null);
    }
}
