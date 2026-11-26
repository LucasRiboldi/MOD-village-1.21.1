package com.villagecolony.gametest;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.Identifier;

import java.util.Set;

/** Contrato do blueprint modificado da casa grande da fundacao. */
public class BigHouseModBlueprintGameTest implements FabricGameTest {

    /** Quantos blocos a planta tem, o número que {@code scripts/houses/gen_big_house.py} imprime (Regra 369). */
    private static final int PLACED_BLOCKS = 441;

    private static final ResourceId BIG_HOUSE_MOD =
            new ResourceId("villagecolony", "houses/big_house_mod");

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE,
            batchId = "bighousemod_blueprint")
    public void theModBlueprintContainsSevenBedsAndSevenChests(TestContext context) {
        Blueprint blueprint = StructureBlueprintReader.read(
                        context.getWorld(), BIG_HOUSE_MOD)
                .orElseThrow(() -> new AssertionError("BigHouseMOD nao foi encontrada"));

        long beds = blueprint.blocks().stream()
                .filter(block -> block.block().equals(ResourceId.vanilla("white_bed")))
                .count();
        long chests = blueprint.blocks().stream()
                .filter(block -> block.block().equals(ResourceId.vanilla("chest")))
                .count();

        context.assertTrue(blueprint.size().equals(new ColonyPos(9, 12, 11)),
                "dimensao inesperada: " + blueprint.size());
        context.assertTrue(beds == 7,
                "BigHouseMOD tem " + beds + " pes de cama, esperado 7");
        context.assertTrue(chests == 7,
                "BigHouseMOD tem " + chests + " baus, esperado 7");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE,
            batchId = "bighousemod_blueprint")
    public void theRawTemplateHasNoGeneratorBlocksOrRemovedFurniture(
            TestContext context) {
        StructureTemplate template = context.getWorld().getStructureTemplateManager()
                .getTemplate(Identifier.of("villagecolony", "houses/big_house_mod"))
                .orElseThrow(() -> new AssertionError("template ausente"));
        NbtCompound nbt = template.writeNbt(new NbtCompound());
        NbtList palette = nbt.getList("palette", NbtElement.COMPOUND_TYPE);
        NbtList blocks = nbt.getList("blocks", NbtElement.COMPOUND_TYPE);
        int beds = 0;
        int chests = 0;
        int placedBlocks = 0;
        boolean hasRoadLevelDoor = false;

        for (int index = 0; index < blocks.size(); index++) {
            NbtCompound block = blocks.getCompound(index);
            placedBlocks++;
            String name = palette.getCompound(block.getInt("state")).getString("Name");
            context.assertFalse(name.equals("minecraft:jigsaw")
                            || name.equals("minecraft:structure_block")
                            || name.equals("minecraft:structure_void"),
                    "bloco de geracao permaneceu no NBT: " + name);
            if (name.equals("minecraft:white_bed")) {
                beds++;
            } else if (name.equals("minecraft:chest")) {
                chests++;
            }

            NbtList pos = block.getList("pos", NbtElement.INT_TYPE);
            // O piso de pedra é a camada 0, no lugar do chão; a porta fica na camada 1, a de quem anda (Regra 369).
            if (name.equals("minecraft:oak_door") && pos.getInt(1) == 1) {
                hasRoadLevelDoor = true;
            }
        }

        context.assertTrue(beds == 14,
                "NBT deve conter as 14 metades de 7 camas, encontrou " + beds);
        context.assertTrue(chests == 7,
                "NBT deve conter 7 baus, encontrou " + chests);
        context.assertTrue(placedBlocks == PLACED_BLOCKS,
                "a planta gerada por scripts/houses/gen_big_house.py tem " + PLACED_BLOCKS + " blocos, encontrou "
                        + placedBlocks);
        context.assertTrue(hasRoadLevelDoor,
                "a metade inferior da porta precisa ficar na camada 1, logo acima do piso");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE,
            batchId = "bighousemod_blueprint")
    public void theModBlueprintHasNoVanillaFurnitureOrGeneratorScaffolding(
            TestContext context) {
        Blueprint blueprint = StructureBlueprintReader.read(
                        context.getWorld(), BIG_HOUSE_MOD)
                .orElseThrow(() -> new AssertionError("BigHouseMOD nao foi encontrada"));

        Set<ResourceId> forbidden = Set.of(
                // A tocha de parede por fora, acima da porta, é da planta.
                ResourceId.vanilla("torch"),
                ResourceId.vanilla("lantern"),
                ResourceId.vanilla("jigsaw"),
                ResourceId.vanilla("structure_block"),
                ResourceId.vanilla("structure_void"));

        for (BlueprintBlock block : blueprint.blocks()) {
            context.assertFalse(forbidden.contains(block.block()),
                    "moveis/andaimes vanilla entraram no blueprint: " + block.block());
        }

        context.assertTrue(blueprint.blocks().stream().noneMatch(
                        block -> block.offset().equals(new ColonyPos(2, 1, 5))
                                || block.offset().equals(new ColonyPos(3, 1, 5))),
                "o corredor central nao ficou livre");
        context.assertFalse(blueprint.materials().containsKey(
                        ResourceId.vanilla("air")),
                "ar nao pode virar material da casa");
        context.complete();
    }

    /**
     * Regra 35: o carpinteiro é titular, logo depois do lenhador, com cama e baú à direita da porta e corredor livre.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "big_house_carpenter")
    public void theCarpenterHasItsPlaceRightOfTheDoor(TestContext context) {
        var order = com.villagecolony.core.worker.service.ProfessionAssigner.FOUNDATION_ORDER;

        context.assertTrue(order.indexOf(com.villagecolony.core.worker.model.ProfessionType.CARPENTER)
                        == order.indexOf(com.villagecolony.core.worker.model.ProfessionType.LUMBERJACK) + 1,
                "Regra 35: o carpinteiro não vem logo depois do lenhador");

        Blueprint blueprint = StructureBlueprintReader.read(context.getWorld(), BIG_HOUSE_MOD)
                .orElseThrow(() -> new AssertionError("BigHouseMOD nao foi encontrada"));
        ColonyPos door = blueprint.blocks().stream()
                .filter(block -> block.block().equals(ResourceId.vanilla("oak_door")))
                .map(BlueprintBlock::offset)
                .min(java.util.Comparator.comparingInt(ColonyPos::y))
                .orElseThrow(() -> new AssertionError("a casa não tem porta"));
        ColonyPos chest = new ColonyPos(door.x() + 1, door.y(), door.z() + 1);
        ColonyPos bed = new ColonyPos(door.x() + 1, door.y(), door.z() + 2);

        context.assertTrue(blueprint.blocks().stream().anyMatch(block -> block.offset().equals(chest)
                && block.block().equals(ResourceId.vanilla("chest"))), "Regra 35: o baú não está à direita da porta");
        context.assertTrue(blueprint.blocks().stream().anyMatch(block -> block.offset().equals(bed)
                && block.block().equals(ResourceId.vanilla("white_bed"))), "Regra 35: a cama não está à direita da porta");

        for (int ahead = 1; ahead <= 2; ahead++) {
            ColonyPos corridor = new ColonyPos(door.x() + ahead, door.y(), door.z());

            context.assertTrue(blueprint.blocks().stream().noneMatch(block -> block.offset().equals(corridor)
                    && !block.block().equals(ResourceId.vanilla("air"))), "Regra 35: o corredor da porta não está livre");
        }

        context.complete();
    }

    /**
     * Regra 369: o piso de pedra da casa grande fica no lugar do chão, na altura de quem anda em volta, e a porta logo
     * acima dele — sem degrau para entrar.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "big_house_floor", tickLimit = 40)
    public void theBigHouseFloorSitsAtTheRoad(TestContext context) {
        net.minecraft.server.world.ServerWorld world = context.getWorld();
        // Longe da arena e acima do mundo plano: o campo é todo do teste.
        net.minecraft.util.math.BlockPos low = context.getAbsolutePos(net.minecraft.util.math.BlockPos.ORIGIN)
                .add(3_100_000, 0, 3_100_000).withY(100);
        int side = 40;
        java.util.UUID colonyId = java.util.UUID.randomUUID();

        try {
            for (int cx = (low.getX() >> 4) - 1; cx <= ((low.getX() + side) >> 4) + 1; cx++) {
                for (int cz = (low.getZ() >> 4) - 1; cz <= ((low.getZ() + side) >> 4) + 1; cz++) {
                    world.setChunkForced(cx, cz, true);
                }
            }
            for (int x = 0; x < side; x++) {
                for (int z = 0; z < side; z++) {
                    world.setBlockState(low.add(x, -1, z), net.minecraft.block.Blocks.STONE.getDefaultState(), 18);
                    world.setBlockState(low.add(x, 0, z), net.minecraft.block.Blocks.GRASS_BLOCK.getDefaultState(), 18);
                }
            }

            com.villagecolony.core.colony.model.Colony colony = com.villagecolony.core.colony.model.Colony.create(
                    colonyId, com.villagecolony.fabric.adapter.MinecraftTypeAdapter.toColonyPos(
                            low.add(side / 2, 1, side / 2)));
            var result = com.villagecolony.fabric.integration.BigHouseFoundation.ensure(world, colony);

            context.assertTrue(result.building().isPresent(), "a casa grande não foi posta");

            var box = result.building().get();
            net.minecraft.util.math.BlockPos door = null;

            for (net.minecraft.util.math.BlockPos at : net.minecraft.util.math.BlockPos.iterate(
                    com.villagecolony.fabric.adapter.MinecraftTypeAdapter.toBlockPos(box.min()),
                    com.villagecolony.fabric.adapter.MinecraftTypeAdapter.toBlockPos(box.max()))) {
                if (world.getBlockState(at).isOf(net.minecraft.block.Blocks.OAK_DOOR) && (door == null
                        || at.getY() < door.getY())) {
                    door = at.toImmutable();
                }
            }

            context.assertTrue(door != null, "a casa grande não tem porta");
            context.assertTrue(door.getY() == low.getY() + 1, "Regra 369: a porta está em y " + door.getY()
                    + ", e quem anda em volta pisa em y " + (low.getY() + 1));
            context.assertTrue(world.getBlockState(door.east().down()).isOf(net.minecraft.block.Blocks.STONE_BRICKS),
                    "Regra 369: o piso de dentro não é de pedra na altura do chão: "
                            + world.getBlockState(door.east().down()));
        } finally {
            com.villagecolony.VillageColonyMod.BUILDINGS.removeOfColony(colonyId);
            for (net.minecraft.util.math.BlockPos at : net.minecraft.util.math.BlockPos.iterate(low.add(0, -1, 0),
                    low.add(side - 1, 14, side - 1))) {
                world.setBlockState(at, net.minecraft.block.Blocks.AIR.getDefaultState(), 18);
            }
            for (int cx = (low.getX() >> 4) - 1; cx <= ((low.getX() + side) >> 4) + 1; cx++) {
                for (int cz = (low.getZ() >> 4) - 1; cz <= ((low.getZ() + side) >> 4) + 1; cz++) {
                    world.setChunkForced(cx, cz, false);
                }
            }
        }

        context.complete();
    }
}
