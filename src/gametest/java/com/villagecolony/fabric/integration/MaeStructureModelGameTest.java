package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.ChestType;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Properties;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import java.util.UUID;

/** Estados completos e conteúdo seguro são provas distintas da circulação e dos materiais — ADR-046. */
public final class MaeStructureModelGameTest implements FabricGameTest {
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_model_states", tickLimit = 20)
    public void maeKeepsDoorChestAndCropStatesWithoutChangingLegacyReading(TestContext context) {
        var nbt = fixture();
        add(nbt, 1, 1, 1, Blocks.OAK_DOOR.getDefaultState().with(Properties.DOOR_HINGE, DoorHinge.RIGHT));
        add(nbt, 1, 2, 1, Blocks.OAK_DOOR.getDefaultState().with(Properties.DOOR_HINGE, DoorHinge.RIGHT)
                .with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
        add(nbt, 2, 0, 2, Blocks.FARMLAND.getDefaultState().with(Properties.MOISTURE, 7));
        add(nbt, 2, 1, 2, Blocks.WHEAT.getDefaultState().with(Properties.AGE_7, 7));
        add(nbt, 3, 1, 3, Blocks.CHEST.getDefaultState().with(Properties.CHEST_TYPE, ChestType.LEFT));
        add(nbt, 4, 1, 3, Blocks.CHEST.getDefaultState().with(Properties.CHEST_TYPE, ChestType.RIGHT));
        var id = load(context, "states", nbt);
        var blueprint = StructureBlueprintReader.readForMae(context.getWorld(), id).orElseThrow();
        var planned = MaeStructurePieces.planned(ConstructionProject.plan(UUID.randomUUID(), blueprint, new ColonyPos(0, 0, 0)));
        context.assertTrue(planned.get(new BlockPos(1, 1, 1)).get(Properties.DOOR_HINGE) == DoorHinge.RIGHT,
                "MAE perdeu a dobradiça da porta");
        context.assertTrue(planned.get(new BlockPos(2, 0, 2)).get(Properties.MOISTURE) == 7, "MAE perdeu umidade");
        context.assertTrue(planned.get(new BlockPos(2, 1, 2)).get(Properties.AGE_7) == 7, "MAE perdeu idade do cultivo");
        context.assertTrue(planned.get(new BlockPos(3, 1, 3)).get(Properties.CHEST_TYPE) == ChestType.LEFT,
                "MAE perdeu o tipo do baú");
        context.assertTrue(StructureBlueprintReader.read(context.getWorld(), id).orElseThrow().blocks().stream()
                .noneMatch(piece -> piece.states().containsKey("moisture")), "leitura legada mudou de política");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_model_payload", tickLimit = 20)
    public void maeRefusesInventoryLootAndArbitraryBlockData(TestContext context) {
        for (String key : java.util.List.of("Items", "LootTable", "CustomName")) {
            var nbt = fixture();
            var entry = add(nbt, 1, 1, 1, Blocks.CHEST.getDefaultState());
            var payload = new NbtCompound();
            if (key.equals("Items")) {
                var items = new NbtList();
                var item = new NbtCompound();
                item.putString("id", "minecraft:diamond");
                item.putInt("count", 64);
                items.add(item);
                payload.put(key, items);
            } else payload.putString(key, key.equals("LootTable") ? "minecraft:chests/simple_dungeon" : "unsafe");
            entry.put("nbt", payload);
            var id = load(context, key.toLowerCase(java.util.Locale.ROOT), nbt);
            boolean refused = false;
            try { StructureBlueprintReader.readForMae(context.getWorld(), id); }
            catch (IllegalArgumentException expected) { refused = "unsafe_structure_payload".equals(expected.getMessage()); }
            context.assertTrue(refused, "payload sem tratamento foi aceito: " + key);
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_model_rotation", tickLimit = 20)
    public void maeConnectionsFollowTheSameRotationAsThePieces(TestContext context) {
        var nbt = fixture();
        var state = Blocks.OAK_FENCE.getDefaultState().with(Properties.NORTH, true).with(Properties.WEST, true);
        add(nbt, 1, 1, 1, state);
        var blueprint = StructureBlueprintReader.readForMae(context.getWorld(), load(context, "rotation", nbt)).orElseThrow();
        for (int turn = 0; turn < 4; turn++) {
            var rotated = blueprint.rotated(turn);
            var fence = rotated.blocks().stream().filter(piece -> piece.block().path().equals("oak_fence")).findFirst().orElseThrow();
            var project = ConstructionProject.plan(UUID.randomUUID(), rotated, new ColonyPos(0, 0, 0));
            var expected = state;
            for (int i = 0; i < turn; i++) expected = expected.rotate(net.minecraft.util.BlockRotation.CLOCKWISE_90);
            context.assertTrue(com.villagecolony.fabric.integration.PaletteStates.planned(fence).equals(expected),
                    "conexões não acompanharam rotação " + turn);
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_model_orphan_chest", tickLimit = 20)
    public void maeRefusesAnUnpairedDoubleChest(TestContext context) {
        var nbt = fixture();
        add(nbt, 2, 1, 2, Blocks.CHEST.getDefaultState().with(Properties.CHEST_TYPE, ChestType.LEFT));
        refused(context, nbt, "chest_pair", "orphan_structure_chest");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_model_actual", tickLimit = 80)
    public void actualHousesWorkshopPenAndMineKeepTheirStates(TestContext context) {
        var failures = new java.util.ArrayList<String>();
        for (String style : java.util.List.of("plains", "desert", "savanna", "taiga", "snowy")) {
            String variant = style.equals("taiga") ? "2" : "1";
            var id = ResourceId.vanilla("village/" + style + "/houses/" + style + "_small_house_" + variant);
            actual(context, id, failures);
        }
        for (String name : java.util.List.of("minecraft:village/plains/houses/plains_tool_smith_1",
                "villagecolony:colony/large_animal_pen", "villagecolony:mine_entrance"))
            actual(context, ResourceId.parse(name), failures);
        context.assertTrue(failures.isEmpty(), "modelos sem prova de estados: " + failures);
        context.complete();
    }

    private static void actual(TestContext context, ResourceId id, java.util.List<String> failures) {
        try {
            if (StructureBlueprintReader.readForMae(context.getWorld(), id).isEmpty()) failures.add(id + ": missing");
        } catch (IllegalArgumentException blocked) {
            failures.add(id + ": " + blocked.getMessage()
                    + (blocked.getCause() == null ? "" : " (" + blocked.getCause().getMessage() + ")"));
        }
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_model_mine_encoding", tickLimit = 20)
    public void actualMineTemplateKeepsItsDeclaredSizeAndDistinctPositions(TestContext context) {
        var template = context.getWorld().getStructureTemplateManager()
                .getTemplate(Identifier.of("villagecolony", "mine_entrance")).orElseThrow();
        context.assertTrue(template.getSize().equals(new net.minecraft.util.math.Vec3i(9, 7, 5)),
                "dimensões do pórtico não foram lidas pelo Minecraft");
        var entries = template.writeNbt(new NbtCompound()).getList("blocks", NbtElement.COMPOUND_TYPE);
        var positions = new java.util.HashSet<ColonyPos>();
        for (int i = 0; i < entries.size(); i++) positions.add(StructureBlueprintReader.offsetOf(entries.getCompound(i)));
        context.assertTrue(entries.size() == 164 && positions.size() == 164, "coordenadas do pórtico colapsaram");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_model_payload_types", tickLimit = 20)
    public void maeRefusesMalformedInventoryAndExecutableFinalState(TestContext context) {
        var nbt = fixture();
        var chest = add(nbt, 1, 1, 1, Blocks.CHEST.getDefaultState());
        var payload = new NbtCompound();
        var items = new NbtList();
        items.add(NbtString.of("not an inventory entry"));
        payload.put("Items", items);
        chest.put("nbt", payload);
        refused(context, nbt, "bad_items", "unsafe_structure_payload");
        nbt = fixture();
        var jigsaw = add(nbt, 1, 1, 1, Blocks.JIGSAW.getDefaultState());
        payload = new NbtCompound();
        payload.putString("final_state", "minecraft:oak_stairs[facing=east]{Items:[]}");
        jigsaw.put("nbt", payload);
        refused(context, nbt, "final_nbt", "unsafe_structure_final_state");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_model_parts", tickLimit = 20)
    public void maeRefusesMissingDoorHalfAndUnsupportedRailRotation(TestContext context) {
        var nbt = fixture();
        add(nbt, 1, 1, 1, Blocks.OAK_DOOR.getDefaultState());
        refused(context, nbt, "missing_upper", "unsupported_structure_parts");
        nbt = fixture();
        add(nbt, 1, 1, 1, Blocks.RAIL.getDefaultState());
        refused(context, nbt, "rail", "unsupported_structure_rotation");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_model_tall_plant", tickLimit = 20)
    public void maeProjectsBothHalvesOfTallPlants(TestContext context) {
        var nbt = fixture();
        add(nbt, 1, 1, 1, Blocks.TALL_GRASS.getDefaultState());
        add(nbt, 1, 2, 1, Blocks.TALL_GRASS.getDefaultState()
                .with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
        var blueprint = StructureBlueprintReader.readForMae(context.getWorld(), load(context, "tall_plant", nbt)).orElseThrow();
        var project = ConstructionProject.plan(UUID.randomUUID(), blueprint, new ColonyPos(0, 0, 0));
        context.assertTrue(MaeStructurePieces.planned(project).get(new BlockPos(1, 2, 1))
                .get(Properties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER, "metade superior perdida");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_model_storage", tickLimit = 20)
    public void actualStorageAndSignsRemainBlockedUntilTheirPayloadHasTreatment(TestContext context) {
        for (String name : java.util.List.of("villagecolony:colony/plains/storage_majest",
                "minecraft:village/taiga/houses/taiga_small_house_1")) {
            boolean blocked = false;
            try { StructureBlueprintReader.readForMae(context.getWorld(), ResourceId.parse(name)); }
            catch (IllegalArgumentException expected) { blocked = "unsafe_structure_payload".equals(expected.getMessage()); }
            context.assertTrue(blocked, "modelo com dados funcionais não tratados deve permanecer bloqueado: " + name);
        }
        context.complete();
    }

    private static void refused(TestContext context, NbtCompound nbt, String name, String reason) {
        boolean blocked = false;
        try { StructureBlueprintReader.readForMae(context.getWorld(), load(context, name, nbt)); }
        catch (IllegalArgumentException expected) { blocked = reason.equals(expected.getMessage()); }
        context.assertTrue(blocked, "modelo deveria recusar: " + reason);
    }

    private static NbtCompound fixture() {
        var nbt = new NbtCompound();
        var size = new NbtList();
        for (int value : new int[]{5, 4, 5}) size.add(NbtInt.of(value));
        nbt.put("size", size);
        nbt.put("palette", new NbtList());
        nbt.put("blocks", new NbtList());
        nbt.put("entities", new NbtList());
        add(nbt, 0, 0, 0, Blocks.STONE.getDefaultState());
        add(nbt, 4, 3, 4, Blocks.STONE.getDefaultState());
        return nbt;
    }

    private static NbtCompound add(NbtCompound nbt, int x, int y, int z, BlockState state) {
        var palette = nbt.getList("palette", NbtElement.COMPOUND_TYPE);
        var entry = new NbtCompound();
        entry.putInt("state", palette.size());
        palette.add(NbtHelper.fromBlockState(state));
        var pos = new NbtList();
        for (int value : new int[]{x, y, z}) pos.add(NbtInt.of(value));
        entry.put("pos", pos);
        nbt.getList("blocks", NbtElement.COMPOUND_TYPE).add(entry);
        return entry;
    }

    private static ResourceId load(TestContext context, String name, NbtCompound nbt) {
        var id = Identifier.of("villagecolony", "test/mae_model_" + name);
        context.getWorld().getStructureTemplateManager().getTemplateOrBlank(id)
                .readNbt(Registries.BLOCK.getReadOnlyWrapper(), nbt);
        return ResourceId.parse(id.toString());
    }
}
