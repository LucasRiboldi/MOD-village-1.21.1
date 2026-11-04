package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import java.util.List;
import java.util.UUID;

/** Quatro entradas físicas continuam sendo um único curral — ADR-046. */
public final class PenAccessGameTest implements FabricGameTest {
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "pen_four_access", tickLimit = 40)
    public void theShepherdChoosesTheNearGateOfOnePhysicalPen(TestContext context) {
        var origin = context.getAbsolutePos(new BlockPos(3, 60, 3));
        floor(context, origin, 6);
        for (int x = 0; x <= 6; x++) for (int z = 0; z <= 6; z++) {
            if (x == 0 || x == 6 || z == 0 || z == 6) {
                context.getWorld().setBlockState(origin.add(x, 1, z), Blocks.OAK_FENCE.getDefaultState(), 3);
            }
        }
        for (var side : Direction.Type.HORIZONTAL) {
            var gate = origin.add(3, 1, 3).offset(side, 3);
            context.getWorld().setBlockState(gate, Blocks.OAK_FENCE_GATE.getDefaultState()
                    .with(FenceGateBlock.FACING, side), 3);
        }
        var colony = colony(origin, 6);
        try {
            context.assertTrue(ShepherdPens.pensOf(context.getWorld(), colony).size() == 1, "quatro portões multiplicaram currais");
            for (var side : Direction.Type.HORIZONTAL) {
                var from = origin.add(3, 1, 3).offset(side, 5);
                var selected = ShepherdPens.withRoom(context.getWorld(), colony, from).orElseThrow();
                context.assertTrue(selected.gate().equals(origin.add(3, 1, 3).offset(side, 3)),
                        "pastor perdeu o portão próximo no lado " + side);
                context.assertTrue(ShepherdPens.capacityOf(selected) == 6, "entradas multiplicaram capacidade");
            }
            var north = origin.add(3, 1, -1);
            context.getWorld().setBlockState(north.up(), Blocks.STONE.getDefaultState(), 3);
            var selected = ShepherdPens.withRoom(context.getWorld(), colony, north).orElseThrow();
            context.assertTrue(!selected.gate().equals(north.south()), "portão exterior obstruído foi escolhido");
            context.getWorld().setBlockState(north.up(), Blocks.AIR.getDefaultState(), 3);
            context.getWorld().setBlockState(north.south().up(), Blocks.STONE.getDefaultState(), 3);
            selected = ShepherdPens.withRoom(context.getWorld(), colony, north).orElseThrow();
            context.assertTrue(!selected.gate().equals(north.south()), "portão com cabeça obstruída foi escolhido");
            context.assertTrue(ShepherdPens.forSpecies(context.getWorld(), colony,
                    net.minecraft.entity.EntityType.COW, north, selected, false).isEmpty(),
                    "outro portão do mesmo curral foi tratado como destino diferente");
        } finally {
            ShepherdPens.clearAll();
            for (var at : BlockPos.iterate(origin.add(-3, 0, -3), origin.add(9, 4, 9))) {
                context.getWorld().setBlockState(at, Blocks.AIR.getDefaultState(), 18);
            }
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "pen_large_access", tickLimit = 40)
    public void theActualLargeTemplateIsRecognizedFromItsSide(TestContext context) {
        var origin = context.getAbsolutePos(new BlockPos(3, 90, 3));
        floor(context, origin, 14);
        var colony = colony(origin, 14);
        var plan = StructureBlueprintReader.readForMae(context.getWorld(), PenPlans.MODEL).orElseThrow();
        var project = com.villagecolony.core.construction.model.ConstructionProject.plan(colony.id(), plan,
                MinecraftTypeAdapter.toColonyPos(origin));
        for (var piece : plan.blocks()) {
            context.getWorld().setBlockState(origin.add(piece.offset().x(), piece.offset().y(), piece.offset().z()),
                    com.villagecolony.fabric.integration.PaletteStates.planned(piece), 18);
        }
        var pens = ShepherdPens.scan(context.getWorld(), colony.bounds().orElseThrow());
        context.assertTrue(pens.size() == 1, "o curral físico 15x15 não foi reconhecido: " + pens.stream()
                .map(pen -> pen.gate().subtract(origin) + ":" + pen.reach().size() + ":" + pen.inside().subtract(origin)).toList());
        context.assertTrue(pens.getFirst().reach().contains(origin.add(13, 1, 13)), "canto distante ficou fora do cercado");
        var cow = net.minecraft.entity.EntityType.COW.create(context.getWorld());
        context.assertTrue(cow != null, "vaca da fixture ausente");
        cow.setAiDisabled(true);
        cow.refreshPositionAndAngles(origin.getX() + 13.5, origin.getY() + 1, origin.getZ() + 13.5, 0, 0);
        context.getWorld().spawnEntity(cow);
        var animals = new java.util.ArrayList<net.minecraft.entity.passive.CowEntity>();
        animals.add(cow);
        var building = com.villagecolony.core.construction.model.Building.of(project, true);
        com.villagecolony.VillageColonyMod.BUILDINGS.register(building);
        try {
            context.assertTrue(ShepherdFlock.isPenned(context.getWorld(), cow), "animal no canto do curral foi tratado como solto");
            context.assertTrue(ShepherdPens.animalsIn(context.getWorld(), pens.getFirst()) == 1,
                    "animal no canto não entrou na contagem");
            var pen = ShepherdPens.pensOf(context.getWorld(), colony).getFirst();
            context.assertTrue(ShepherdPens.capacityOf(pen) == 20 && pen.accesses().size() == 4,
                    "registro perdeu capacidade ou acessos do curral único");
            for (var at : pen.reach().stream().filter(at -> context.getWorld().getBlockState(at.down()).isOf(Blocks.GRASS_BLOCK))
                    .filter(at -> !at.equals(cow.getBlockPos())).sorted(java.util.Comparator.comparingLong(BlockPos::asLong))
                    .limit(19).toList()) {
                var animal = net.minecraft.entity.EntityType.COW.create(context.getWorld());
                context.assertTrue(animal != null, "vaca da fixture ausente");
                animal.setAiDisabled(true);
                animal.refreshPositionAndAngles(at.getX() + .5, at.getY(), at.getZ() + .5, 0, 0);
                context.getWorld().spawnEntity(animal);
                animals.add(animal);
            }
            context.assertTrue(ShepherdPens.animalsIn(context.getWorld(), pen) == 20
                    && !ShepherdPens.hasRoom(context.getWorld(), pen), "quatro entradas multiplicaram a lotação total de 20");
            ShepherdPens.bringIn(context.getWorld(), pen, cow);
            FencedIn.tick(context.getWorld());
            context.assertTrue(pen.accesses().stream().noneMatch(access ->
                    context.getWorld().getBlockState(access.gate()).get(FenceGateBlock.OPEN)), "portão ficou aberto após entrega");
            context.assertTrue(animals.stream().allMatch(animal -> pen.reach().contains(animal.getBlockPos())),
                    "a entrega controlada retirou animais do cercado");
        } finally {
            animals.forEach(net.minecraft.entity.passive.CowEntity::discard);
            com.villagecolony.VillageColonyMod.BUILDINGS.remove(building.id());
            ShepherdPens.clearAll();
            FencedIn.clearAll();
            for (var at : BlockPos.iterate(origin.add(-3, 0, -3), origin.add(17, 4, 17))) {
                context.getWorld().setBlockState(at, Blocks.AIR.getDefaultState(), 18);
            }
        }
        context.complete();
    }

    private static Colony colony(BlockPos origin, int size) {
        var colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(origin));
        colony.measure(new VillageBounds(origin.getX(), origin.getY() - 2, origin.getZ(),
                origin.getX() + size, origin.getY() + 4, origin.getZ() + size));
        return colony;
    }

    private static void floor(TestContext context, BlockPos origin, int size) {
        for (var at : BlockPos.iterate(origin.add(-3, 0, -3), origin.add(size + 3, 4, size + 3))) {
            context.getWorld().setBlockState(at, at.getY() == origin.getY()
                    ? Blocks.GRASS_BLOCK.getDefaultState() : Blocks.AIR.getDefaultState(), 18);
        }
    }
}
