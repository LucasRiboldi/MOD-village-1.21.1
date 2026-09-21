package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ColonyEdits;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.event.PlayerWorldChangeHandler;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/** Regressoes do cancelamento manual de canteiros por Tocha das Almas. */
public class ConstructionCancellationGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "construction_cancel")
    public void aSoulTorchCancelsTheOrdinaryConstruction(TestContext context) {
        UUID colonyId = UUID.randomUUID();
        ColonyPos origin = MinecraftTypeAdapter.toColonyPos(
                context.getAbsolutePos(new BlockPos(2, 2, 2)));
        Blueprint blueprint = Blueprint.of(
                ResourceId.parse("minecraft:village/plains/houses/plains_small_house_1"),
                List.of(new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("oak_planks"))));
        ConstructionProject project = ConstructionProject.plan(colonyId, blueprint, origin);
        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);

        Task task = null;
        try {
            VillageColonyMod.CONSTRUCTIONS.register(project);
            task = VillageColonyMod.TASKS.create(
                    colonyId,
                    com.villagecolony.core.task.model.TaskType.BUILD,
                    com.villagecolony.core.task.model.TaskPriority.CONSTRUCTION,
                    ResourceType.OAK_PLANKS,
                    1);
            BlockPos marker = context.getAbsolutePos(new BlockPos(2, 2, 2));
            context.getWorld().setBlockState(marker, Blocks.SOUL_TORCH.getDefaultState());

            context.assertTrue(
                    context.getWorld().getBlockState(marker).isOf(Blocks.SOUL_TORCH),
                    "o marcador de teste nao ficou como Tocha das Almas");
            context.assertTrue(
                    VillageColonyMod.CONSTRUCTIONS.find(project.id()).isPresent(),
                    "o projeto de teste nao ficou registrado");
            context.assertTrue(
                    Building.of(project).contains(MinecraftTypeAdapter.toColonyPos(marker)),
                    "a caixa do projeto nao contem a posicao do marcador");

            context.assertTrue(
                    ConstructionCancellation.cancelAtSoulTorch(context.getWorld(), marker),
                    "a Tocha das Almas dentro do canteiro nao cancelou a obra");
            context.assertTrue(
                    VillageColonyMod.CONSTRUCTIONS.openOf(colonyId).isEmpty(),
                    "o projeto cancelado continuou ocupando a vaga da vila");
            context.assertTrue(
                    !task.state().isOpen(),
                    "a tarefa de construcao continuou aberta apos o cancelamento");
        } finally {
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colonyId);
            if (task != null) {
                VillageColonyMod.TASKS.remove(task.id());
            }
            VillageColonyMod.COLONIES.remove(colonyId);
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "construction_cancel")
    public void aSoulTorchDoesNotCancelTheBigHouseFoundation(TestContext context) {
        UUID colonyId = UUID.randomUUID();
        ColonyPos origin = MinecraftTypeAdapter.toColonyPos(
                context.getAbsolutePos(new BlockPos(2, 2, 2)));
        Blueprint blueprint = Blueprint.of(
                StructureBlueprintReader.BIG_HOUSE_MOD,
                List.of(new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("oak_planks"))));
        ConstructionProject project = ConstructionProject.plan(colonyId, blueprint, origin);
        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);

        try {
            VillageColonyMod.CONSTRUCTIONS.register(project);
            BlockPos marker = context.getAbsolutePos(new BlockPos(2, 2, 2));
            context.getWorld().setBlockState(marker, Blocks.SOUL_TORCH.getDefaultState());

            context.assertFalse(
                    ConstructionCancellation.cancelAtSoulTorch(context.getWorld(), marker),
                    "a Tocha das Almas cancelou a BigHouseMOD especial");
            context.assertTrue(
                    VillageColonyMod.CONSTRUCTIONS.openOf(colonyId).isPresent(),
                    "a BigHouseMOD saiu do registro por uma regra das profissoes");
        } finally {
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colonyId);
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "construction_cancel")
    public void aSoulTorchChangeIsNotSwallowedByAnOwnEditMarker(TestContext context) {
        UUID colonyId = UUID.randomUUID();
        ColonyPos origin = MinecraftTypeAdapter.toColonyPos(
                context.getAbsolutePos(new BlockPos(2, 2, 2)));
        Blueprint blueprint = Blueprint.of(
                ResourceId.parse("minecraft:village/plains/houses/plains_small_house_1"),
                List.of(new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("oak_planks"))));
        ConstructionProject project = ConstructionProject.plan(colonyId, blueprint, origin);
        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);

        Task task = null;
        try {
            VillageColonyMod.CONSTRUCTIONS.register(project);
            task = VillageColonyMod.TASKS.create(
                    colonyId,
                    com.villagecolony.core.task.model.TaskType.BUILD,
                    com.villagecolony.core.task.model.TaskPriority.CONSTRUCTION,
                    ResourceType.OAK_PLANKS,
                    1);
            BlockPos marker = context.getAbsolutePos(new BlockPos(2, 2, 2));
            context.getWorld().setBlockState(marker, Blocks.SOUL_TORCH.getDefaultState());
            ColonyEdits.remember(MinecraftTypeAdapter.toColonyPos(marker));

            context.assertTrue(
                    context.getWorld().getBlockState(marker).isOf(Blocks.SOUL_TORCH),
                    "o marcador de teste nao ficou como Tocha das Almas");
            context.assertTrue(
                    VillageColonyMod.CONSTRUCTIONS.find(project.id()).isPresent(),
                    "o projeto de teste nao ficou registrado");
            context.assertTrue(
                    Building.of(project).contains(MinecraftTypeAdapter.toColonyPos(marker)),
                    "a caixa do projeto nao contem a posicao do marcador");

            PlayerWorldChangeHandler.onBlockChanged(context.getWorld(), marker);

            context.assertTrue(
                    VillageColonyMod.CONSTRUCTIONS.openOf(colonyId).isEmpty(),
                    "a Tocha das Almas ficou ignorada porque a posição estava marcada como edição do mod");
            context.assertTrue(
                    !task.state().isOpen(),
                    "a tarefa continuou aberta quando a tocha foi processada pelo evento do mundo");
        } finally {
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colonyId);
            if (task != null) {
                VillageColonyMod.TASKS.remove(task.id());
            }
            VillageColonyMod.COLONIES.remove(colonyId);
        }

        context.complete();
    }
}
