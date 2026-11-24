package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.construction.model.SkipReason;
import com.villagecolony.core.construction.service.ConstructionService;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BlueprintPlacement;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.List;
import java.util.UUID;

/** Modelos, piso e acessos dos currais da colônia (Regras 72 e 168, Regra 253). */
public final class PenModelGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "pen_model", tickLimit = 20)
    public void mediumPensHaveOneGateFacingTheRoadInEveryRotation(TestContext context) {
        for (String group : java.util.List.of("cattle", "sheep", "pig", "poultry", "horse", "camelid")) {
            var id = new com.villagecolony.core.type.ResourceId("villagecolony", "colony/medium_animal_pen_" + group);
            context.assertTrue(BiomePieces.structuresOf("plains").contains(id), "modelo fora do catálogo: " + id);
            var plan = StructureBlueprintReader.read(context.getWorld(), id).orElseThrow();
            context.assertTrue(plan.hasStreetLayer() && plan.streetLayer() == 0, "curral acima da rua: " + id);
            var grass = plan.blocks().stream().filter(block -> block.block().path().equals("grass_block")).findFirst()
                    .orElseThrow();
            BlockPos soil = context.getAbsolutePos(new BlockPos(1, 1, 1));
            context.getWorld().setBlockState(soil, Blocks.DIRT.getDefaultState());
            context.assertTrue(BuriedPieces.mayShapePenFloor(context.getWorld(), plan, grass, soil),
                    "o piso da obra precisa substituir terra natural por grama");
            context.getWorld().setBlockState(soil, Blocks.OAK_PLANKS.getDefaultState());
            context.assertFalse(BuriedPieces.mayShapePenFloor(context.getWorld(), plan, grass, soil),
                    "a obra não pode trocar um bloco de construção por grama");
            for (var side : com.villagecolony.core.type.Side.values()) {
                var rotated = PlanPlacement.turnedToTheRoad(plan, side);
                var gates = rotated.blocks().stream().filter(b -> b.block().path().endsWith("_fence_gate")).toList();
                context.assertTrue(gates.size() == 1, "esperada uma porteira em " + id);
                var gate = gates.getFirst().offset();
                var out = com.villagecolony.fabric.adapter.MinecraftTypeAdapter.toDirection(side);
                var path = new com.villagecolony.core.type.ColonyPos(gate.x() + out.getOffsetX(),
                        gate.y() - 1, gate.z() + out.getOffsetZ());
                context.assertTrue(rotated.blocks().stream().anyMatch(b -> b.offset().equals(path)
                        && b.block().path().equals("dirt_path")), "a saída precisa apontar para a rua: " + side);
            }
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "pen_model_horse_walk", tickLimit = 240)
    public void aHorseWalksThroughTheOnlyGate(TestContext context) {
        var world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 50, 1));
        var template = world.getStructureTemplateManager().getTemplate(
                Identifier.of("villagecolony", "colony/medium_animal_pen_horse")).orElseThrow();
        template.place(world, origin, origin, new StructurePlacementData(), Random.create(1), Block.NOTIFY_LISTENERS);
        BlockPos gate = origin.add(6, 1, 1);
        // A faixa de terra do NBT continua no terreno externo, como num lote construído.
        for (BlockPos floor : BlockPos.iterate(origin.add(4, 0, -25), origin.add(8, 0, -1))) {
            world.setBlockState(floor, Blocks.GRASS_BLOCK.getDefaultState(), Block.NOTIFY_LISTENERS);
        }
        var found = ShepherdPens.penBehind(world, gate, java.util.List.of()).orElseThrow();
        var pen = new ShepherdPens.Pen(found.gate(), found.inside(), found.outside(), found.reach(), false,
                found.accesses(), java.util.Optional.of(MediumPenType.HORSE), false);
        context.assertTrue(pen.inside().equals(gate.south()) && pen.reach().size() == 100,
                "o lado interno e as cem células do NBT devem ser reconhecidos");
        var horse = java.util.Objects.requireNonNull(EntityType.HORSE.create(world));
        Vec3d approach = Vec3d.ofBottomCenter(pen.outside()).add(0, -1.0 / 16, 0);
        horse.refreshPositionAndAngles(approach.x, approach.y, approach.z, 0, 0);
        world.spawnEntity(horse);

        context.runAtEveryTick(() -> {
            boolean inside = ShepherdPens.isInside(pen, horse);
            if (!inside && context.getTick() < 200) {
                ShepherdPens.walkThrough(world, pen, horse, ShepherdPens.deeper(pen));
                return;
            }
            boolean closed = FencedIn.closeForDelivery(world, gate);
            context.assertTrue(closed || !inside, "a porteira ficou aberta");
            boolean gateOpen = world.getBlockState(gate).get(FenceGateBlock.OPEN);
            boolean fenceConnected = world.getBlockState(gate.east()).get(net.minecraft.state.property.Properties.WEST)
                    && world.getBlockState(gate.west()).get(net.minecraft.state.property.Properties.EAST);
            PenTether.release(horse);
            horse.discard();
            var size = template.getSize();
            for (BlockPos at : BlockPos.iterate(origin.north(25), origin.add(size.getX() - 1, size.getY() - 1, size.getZ() - 1))) {
                world.setBlockState(at, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
            }
            context.assertTrue(inside, "cavalo barrou na porteira: " + horse.getPos());
            context.assertFalse(gateOpen, "a porteira ficou aberta");
            context.assertTrue(fenceConnected, "os braços da cerca não se reconectaram");
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "pen_model", tickLimit = 20)
    public void theLargePenMoldsItsGrassFloorOnNaturalSoilAfterResuming(TestContext context) {
        var world = context.getWorld();
        BlockPos corner = context.getAbsolutePos(new BlockPos(1, 40, 1));
        ColonyPos origin = MinecraftTypeAdapter.toColonyPos(corner);
        Colony colony = Colony.create(UUID.randomUUID(), origin);
        VillageColonyMod.COLONIES.register(colony);
        ColonyFixture owned = ColonyFixture.create().owning(colony);

        try {
            var plan = BlueprintPlacement.blueprintOf(world, colony.id(), PenPlans.MODEL, origin).orElseThrow();
            var soils = List.of(Blocks.SAND, Blocks.SANDSTONE, Blocks.DIRT, Blocks.TERRACOTTA, Blocks.SNOW_BLOCK);
            var samples = plan.blocks().stream().filter(block -> block.block().equals(ResourceId.vanilla("grass_block"))
                    && block.offset().x() > 0 && block.offset().x() <= 4
                    && block.offset().z() > 0 && block.offset().z() <= 4).limit(soils.size() + 1).toList();
            ConstructionProject fresh = ConstructionProject.plan(colony.id(), plan, origin);
            for (int index = 0; index < soils.size(); index++) {
                BlockPos at = MinecraftTypeAdapter.toBlockPos(fresh.worldPositionOf(samples.get(index)));
                world.setBlockState(at, soils.get(index).getDefaultState(), Block.NOTIFY_LISTENERS);
            }
            BlockPos grass = MinecraftTypeAdapter.toBlockPos(fresh.worldPositionOf(samples.getLast()));
            world.setBlockState(grass, Blocks.GRASS_BLOCK.getDefaultState());

            BuriedPieces.markHeldByTheGround(world, fresh);
            for (int index = 0; index < soils.size(); index++) {
                context.assertTrue(fresh.remaining().contains(samples.get(index)),
                        "a abertura contou " + soils.get(index) + " como piso pronto");
            }
            context.assertFalse(fresh.remaining().contains(samples.getLast()), "a grama existente já satisfaz a planta");

            BlueprintBlock gate = plan.blocks().stream().filter(block -> block.block().path().endsWith("_fence_gate"))
                    .findFirst().orElseThrow();
            var gatePosition = MinecraftTypeAdapter.toBlockPos(fresh.worldPositionOf(gate));
            world.setBlockState(gatePosition, MinecraftTypeAdapter.toBlock(gate.block()).orElseThrow().getDefaultState());

            VillageColonyMod.CONSTRUCTIONS.registerPending(new ConstructionService.Pending(
                    fresh.id(), colony.id(), PenPlans.MODEL, origin, ConstructionState.BUILDING));
            ConstructionResume.resume(world, colony);
            ConstructionProject resumed = VillageColonyMod.CONSTRUCTIONS.openOf(colony.id()).orElseThrow();
            var task = Task.create(colony.id(), TaskType.BUILD, TaskPriority.CONSTRUCTION, ResourceType.GRASS_BLOCK, 1);
            var job = new BuilderWork.Job(task, resumed.id());
            UUID workerId = UUID.randomUUID();
            for (int index = 0; index < soils.size(); index++) {
                BlockPos at = MinecraftTypeAdapter.toBlockPos(fresh.worldPositionOf(samples.get(index)));
                BlueprintBlock piece = resumed.remaining().stream()
                        .filter(candidate -> resumed.worldPositionOf(candidate).equals(MinecraftTypeAdapter.toColonyPos(at)))
                        .findFirst().orElseThrow(() -> new AssertionError("a retomada perdeu o piso em " + at));
                context.assertTrue(BuilderPlacement.placeOne(world, resumed, job, workerId, piece, at),
                        "o piso natural deve ser moldado sem pedir material");
                context.assertTrue(world.getBlockState(at).isOf(Blocks.GRASS_BLOCK), "faltou grama em " + at);
            }
        } finally {
            owned.cleanUp();
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "pen_model", tickLimit = 20)
    public void theLargePenMoldsDeferredFloorWithoutOverwritingAChest(TestContext context) {
        var world = context.getWorld();
        BlockPos corner = context.getAbsolutePos(new BlockPos(1, 2, 1));
        ColonyPos origin = MinecraftTypeAdapter.toColonyPos(corner);
        Colony colony = Colony.create(UUID.randomUUID(), origin);
        ResourceId grass = ResourceId.vanilla("grass_block");
        var plan = Blueprint.of(PenPlans.MODEL, List.of(
                new BlueprintBlock(new ColonyPos(0, 0, 0), grass),
                new BlueprintBlock(new ColonyPos(1, 0, 0), grass),
                new BlueprintBlock(new ColonyPos(2, 0, 0), grass))).withStreetLayer(0);
        var project = ConstructionProject.plan(colony.id(), plan, origin);
        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);
        world.setBlockState(corner, Blocks.SAND.getDefaultState(), Block.NOTIFY_LISTENERS);
        world.setBlockState(corner.east(), Blocks.SANDSTONE.getDefaultState());
        world.setBlockState(corner.east(2), Blocks.CHEST.getDefaultState());

        ColonyFixture owned = ColonyFixture.create().owning(colony);
        try {
            UnreachablePieces.surge(world, project, new ConstructionProject.DeferredPiece(
                    origin, grass, SkipReason.UNREACHABLE, "sand"));
            BuildClosing.finishBySurging(world, colony, project);
            context.assertTrue(project.state() == ConstructionState.COMPLETED, "a obra devia fechar");
            context.assertTrue(world.getBlockState(corner).isOf(Blocks.GRASS_BLOCK), "faltou grama na peça sem alcance");
            context.assertTrue(world.getBlockState(corner.east()).isOf(Blocks.GRASS_BLOCK), "faltou grama no encerramento");
            context.assertTrue(world.getBlockState(corner.east(2)).isOf(Blocks.CHEST), "o baú deve ficar no lugar");
        } finally {
            owned.cleanUp();
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "pen_model", tickLimit = 20)
    public void theLargePenHasWaterCropsGrassGateAndPostTorches(TestContext context) {
        var plan = StructureBlueprintReader.read(context.getWorld(), PenPlans.MODEL).orElseThrow();
        context.assertTrue(plan.size().x() == 15 && plan.size().z() == 15,
                "o curral comum deveria ter piso de 15 por 15");
        context.assertTrue(plan.hasStreetLayer() && plan.streetLayer() == 0,
                "o piso do curral deve estar no nível da rua");
        long grass = plan.blocks().stream().filter(block -> block.block().path().equals("grass_block")).count();
        long water = plan.blocks().stream().filter(block -> block.block().path().equals("water")).count();
        long farmland = plan.blocks().stream().filter(block -> block.block().path().equals("farmland")).count();
        long crops = plan.blocks().stream().filter(block -> block.block().path().equals("wheat")).count();
        long gates = plan.blocks().stream().filter(block -> block.block().path().equals("oak_fence_gate")).count();
        long torches = plan.blocks().stream().filter(block -> block.block().path().equals("torch")).count();
        boolean rubble = plan.blocks().stream().anyMatch(block -> block.offset().y() > 0
                && (block.block().path().equals("dirt") || block.block().path().equals("stone")));
        context.assertTrue(grass == 212 && water == 1 && farmland == 12 && crops == 12
                        && gates == 4 && torches == 4 && !rubble,
                "água, cultivos, grama, portão, tochas ou interior do curral divergiram");
        for (int turn = 0; turn < 4; turn++) {
            var rotated = plan.rotated(turn);
            var positions = rotated.blocks().stream().filter(block -> block.block().path().equals("oak_fence_gate"))
                    .map(com.villagecolony.core.construction.model.BlueprintBlock::offset).collect(java.util.stream.Collectors.toSet());
            context.assertTrue(positions.equals(java.util.Set.of(
                    new com.villagecolony.core.type.ColonyPos(7, 1, 0), new com.villagecolony.core.type.ColonyPos(14, 1, 7),
                    new com.villagecolony.core.type.ColonyPos(7, 1, 14), new com.villagecolony.core.type.ColonyPos(0, 1, 7))),
                    "rotação perdeu acesso central de um dos quatro lados");
        }
        context.complete();
    }
}
