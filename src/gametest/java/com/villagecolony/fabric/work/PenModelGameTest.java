package com.villagecolony.fabric.work;

import com.villagecolony.fabric.integration.StructureBlueprintReader;
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

/** Modelos, piso e acessos dos currais da colônia (Regras 72 e 168). */
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
            context.assertTrue(BuriedPieces.soilOverStone(context.getWorld(), plan, grass, soil),
                    "o piso da obra precisa substituir terra natural por grama");
            context.getWorld().setBlockState(soil, Blocks.OAK_PLANKS.getDefaultState());
            context.assertFalse(BuriedPieces.soilOverStone(context.getWorld(), plan, grass, soil),
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
