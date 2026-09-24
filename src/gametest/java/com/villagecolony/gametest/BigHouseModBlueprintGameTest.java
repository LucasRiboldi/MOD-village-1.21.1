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

    private static final ResourceId BIG_HOUSE_MOD =
            new ResourceId("villagecolony", "houses/big_house_mod");

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE,
            batchId = "bighousemod_blueprint")
    public void theModBlueprintContainsSixBedsAndSixChests(TestContext context) {
        Blueprint blueprint = StructureBlueprintReader.read(
                        context.getWorld(), BIG_HOUSE_MOD)
                .orElseThrow(() -> new AssertionError("BigHouseMOD nao foi encontrada"));

        long beds = blueprint.blocks().stream()
                .filter(block -> block.block().equals(ResourceId.vanilla("white_bed")))
                .count();
        long chests = blueprint.blocks().stream()
                .filter(block -> block.block().equals(ResourceId.vanilla("chest")))
                .count();

        context.assertTrue(blueprint.size().equals(new ColonyPos(7, 10, 11)),
                "dimensao inesperada: " + blueprint.size());
        context.assertTrue(beds == 6,
                "BigHouseMOD tem " + beds + " pes de cama, esperado 6");
        context.assertTrue(chests == 6,
                "BigHouseMOD tem " + chests + " baus, esperado 6");
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
            if (name.equals("minecraft:oak_door") && pos.getInt(1) == 0) {
                hasRoadLevelDoor = true;
            }
            if (isRemovedFurniturePosition(pos)) {
                throw new AssertionError("mobiliario removido permaneceu em " + pos);
            }
        }

        context.assertTrue(beds == 12,
                "NBT deve conter as 12 metades de 6 camas, encontrou " + beds);
        context.assertTrue(chests == 6,
                "NBT deve conter 6 baus, encontrou " + chests);
        context.assertTrue(placedBlocks == 295,
                "a base removida deve deixar 295 blocos, encontrou " + placedBlocks);
        context.assertTrue(hasRoadLevelDoor,
                "a metade inferior da porta precisa ficar no nivel da rua");
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
                ResourceId.vanilla("wall_torch"),
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
                        block -> block.offset().equals(new ColonyPos(3, 1, 5))),
                "o corredor central nao ficou livre");
        context.assertFalse(blueprint.materials().containsKey(
                        ResourceId.vanilla("air")),
                "ar nao pode virar material da casa");
        context.complete();
    }

    private static boolean isRemovedFurniturePosition(NbtList pos) {
        if (pos.size() != 3) {
            return false;
        }

        int x = pos.getInt(0);
        int y = pos.getInt(1);
        int z = pos.getInt(2);
        return y == 1 && ((x == 2 && (z == 5 || z == 6 || z == 7))
                || (x == 3 && (z == 6 || z == 7))
                || (x == 4 && z == 7));
    }

}
