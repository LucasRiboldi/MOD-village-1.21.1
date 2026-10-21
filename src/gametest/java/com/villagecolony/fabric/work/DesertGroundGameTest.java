package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.IronRest;
import com.villagecolony.fabric.world.DesertGround;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/**
 * O deserto jogado de 2026-10-21 — pedidos do autor: o chão da vila de areia não desmorona, o ferro
 * que a mina não tem descansa, e o fundidor atende primeiro a peça de fornalha que a obra espera.
 */
public class DesertGroundGameTest implements FabricGameTest {

    /** A areia da superfície fica; as duas camadas de baixo viram arenito; a terceira não muda. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "desert_ground")
    public void theTwoLayersUnderTheSandSurfaceBecomeSandstone(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos top = context.getAbsolutePos(new BlockPos(1, 6, 1)).add(1_000_000, 0, 2_700_000);
        BlockPos roofed = top.add(3, 0, 0);
        world.getChunk(top);

        for (BlockPos column : new BlockPos[] {top, roofed}) {
            for (int depth = 0; depth <= 3; depth++) {
                world.setBlockState(column.down(depth), Blocks.SAND.getDefaultState());
            }
        }
        // Controle: chão de casa (arenito liso) em cima da areia não é superfície de areia.
        world.setBlockState(roofed.up(), Blocks.SMOOTH_SANDSTONE.getDefaultState());

        try {
            int firmed = DesertGround.firmColumn(world, top.getX(), top.getZ());

            context.assertTrue(firmed == DesertGround.LAYERS, "deviam virar " + DesertGround.LAYERS + " blocos: " + firmed);
            context.assertTrue(world.getBlockState(top).isOf(Blocks.SAND), "a areia da superfície mudou");
            context.assertTrue(world.getBlockState(top.down()).isOf(Blocks.SANDSTONE)
                    && world.getBlockState(top.down(2)).isOf(Blocks.SANDSTONE), "as camadas de baixo não viraram arenito");
            context.assertTrue(world.getBlockState(top.down(3)).isOf(Blocks.SAND), "a terceira camada mudou");
            context.assertTrue(DesertGround.firmColumn(world, roofed.getX(), roofed.getZ()) == 0
                            && world.getBlockState(roofed.down()).isOf(Blocks.SAND),
                    "controle: debaixo do chão de casa a areia virou arenito");
        } finally {
            for (BlockPos column : new BlockPos[] {top, roofed}) {
                for (int depth = -1; depth <= 3; depth++) {
                    world.setBlockState(column.down(depth), Blocks.AIR.getDefaultState());
                }
            }
        }

        context.complete();
    }

    /** Cavou bastante sem achar ferro: o ferro descansa; achou um, não. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "desert_ground")
    public void ironTheMineDoesNotHaveRests(TestContext context) {
        UUID empty = UUID.randomUUID();
        UUID found = UUID.randomUUID();
        long now = context.getWorld().getTime();

        IronRest.minerGaveUp(empty, ResourceType.RAW_IRON, 0, IronRest.DUG_WITHOUT_IRON, now);
        IronRest.minerGaveUp(found, ResourceType.RAW_IRON, 1, IronRest.DUG_WITHOUT_IRON * 4, now);
        Task iron = VillageColonyMod.TASKS.create(empty, TaskType.COLLECT_STONE, TaskPriority.PRODUCTION,
                ResourceType.RAW_IRON, 64);
        Task coal = VillageColonyMod.TASKS.create(empty, TaskType.COLLECT_STONE, TaskPriority.PRODUCTION,
                ResourceType.COAL, 64);

        try {
            context.assertTrue(IronRest.isResting(empty, now) && IronRest.blocks(empty, iron, now),
                    "a mina cavou sem ferro e o ferro não descansou");
            context.assertFalse(IronRest.blocks(empty, coal, now), "o descanso do ferro barrou o carvão");
            context.assertFalse(IronRest.isResting(found, now), "controle: a mina achou ferro e ele descansou");
            context.assertFalse(IronRest.isResting(empty, now + IronRest.REST_TICKS), "o descanso não acabou no prazo");
        } finally {
            VillageColonyMod.TASKS.remove(iron.id());
            VillageColonyMod.TASKS.remove(coal.id());
        }

        context.complete();
    }

    /** A obra pôs o arenito liso de lado por falta: é a peça de fornalha que o fundidor atende primeiro. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "desert_ground")
    public void theFurnacePieceTheBuildWaitsForComesFirst(TestContext context) {
        ServerWorld world = context.getWorld();
        ColonyPos origin = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(2, 2, 2)));
        Colony colony = Colony.create(UUID.randomUUID(), origin);
        VillageColonyMod.COLONIES.register(colony);
        ConstructionProject project = ConstructionProject.plan(colony.id(), Blueprint.of(
                ResourceId.vanilla("village/desert/houses/test_smooth"), List.of(
                        new BlueprintBlock(new ColonyPos(0, 0, 0), MinecraftTypeAdapter.toResourceId(Blocks.SMOOTH_SANDSTONE)),
                        new BlueprintBlock(new ColonyPos(1, 0, 0), MinecraftTypeAdapter.toResourceId(Blocks.GLASS)))),
                origin);
        VillageColonyMod.CONSTRUCTIONS.register(project);
        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);

        try {
            context.assertTrue(SmelterFallback.waitedFor(world, colony.id()).isEmpty(),
                    "controle: sem peça de lado, a obra já esperava alguma coisa");

            project.deferForMaterial(project.nextBlock().orElseThrow());

            context.assertTrue(SmelterFallback.waitedFor(world, colony.id()).contains(Items.SMOOTH_SANDSTONE),
                    "o arenito liso de lado não virou a peça esperada: " + SmelterFallback.waitedFor(world, colony.id()));
            context.assertTrue(SmelterFallback.wanted(world, colony).getFirst() == Items.SMOOTH_SANDSTONE,
                    "a peça esperada não veio primeiro: " + SmelterFallback.wanted(world, colony));
        } finally {
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colony.id());
            VillageColonyMod.COLONIES.remove(colony.id());
        }

        context.complete();
    }
}
