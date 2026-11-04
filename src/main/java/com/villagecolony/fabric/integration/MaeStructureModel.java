package com.villagecolony.fabric.integration;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.WhitePieces;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Properties;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/** Prova estados e partes do template carregado; não concede materiais nem permissão de escrita — ADR-046. */
final class MaeStructureModel {
    private MaeStructureModel() { }

    static Blueprint prove(Blueprint legacy, StructureTemplate template) {
        var nbt = template.writeNbt(new NbtCompound());
        if (!nbt.getList("entities", NbtElement.COMPOUND_TYPE).isEmpty())
            throw new IllegalArgumentException("structure_entities_not_supported");
        var palette = nbt.getList("palette", NbtElement.COMPOUND_TYPE);
        var expected = new LinkedHashMap<BlockPos, BlockState>();
        var entries = nbt.getList("blocks", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.getCompound(i);
            int index = entry.getInt("state");
            if (index < 0 || index >= palette.size()) throw new IllegalArgumentException("invalid_structure_palette");
            var state = NbtHelper.toBlockState(Registries.BLOCK.getReadOnlyWrapper(), palette.getCompound(index));
            if (state.isOf(Blocks.JIGSAW)) state = promised(entry.getCompound("nbt"));
            else safePayload(entry.getCompound("nbt"));
            if (state.isAir() || state.isOf(Blocks.STRUCTURE_VOID) || state.isOf(Blocks.STRUCTURE_BLOCK)) continue;
            var offset = MinecraftTypeAdapter.toBlockPos(StructureBlueprintReader.offsetOf(entry));
            if (expected.putIfAbsent(offset, state) != null)
                throw new IllegalArgumentException("overlapping_structure_effect", new IllegalStateException(
                        offset + " " + expected.get(offset) + " / " + state));
        }
        for (var entry : expected.entrySet()) {
            var state = entry.getValue();
            if (state.getBlock() instanceof ChestBlock && state.get(Properties.CHEST_TYPE) != ChestType.SINGLE) {
                var other = expected.get(entry.getKey().offset(ChestBlock.getFacing(state)));
                if (other == null || other.getBlock() != state.getBlock()
                        || other.get(Properties.HORIZONTAL_FACING) != state.get(Properties.HORIZONTAL_FACING)
                        || other.get(Properties.CHEST_TYPE) != (state.get(Properties.CHEST_TYPE) == ChestType.LEFT
                        ? ChestType.RIGHT : ChestType.LEFT))
                    throw new IllegalArgumentException("orphan_structure_chest");
            }
        }
        var pieces = new ArrayList<BlueprintBlock>();
        for (var piece : legacy.blocks()) {
            var at = MinecraftTypeAdapter.toBlockPos(piece.offset());
            var state = expected.get(at);
            if (state == null) throw new IllegalArgumentException("structure_model_mismatch");
            pieces.add(withState(piece, state));
        }
        var normalized = WhitePieces.baked(DesertSand.baked(legacy.id(), pieces));
        var dimensions = template.getSize();
        var result = Blueprint.of(legacy.id(), normalized).withAccesses(legacy.accesses())
                .withTemplateBounds(MinecraftTypeAdapter.toColonyPos(new BlockPos(dimensions)));
        if (legacy.hasStreetLayer()) result = result.withStreetLayer(legacy.streetLayer());
        var project = ConstructionProject.plan(UUID.randomUUID(), result, new ColonyPos(0, 0, 0));
        var planned = MaeStructurePieces.planned(project);
        if (!planned.keySet().equals(expected.keySet()))
            throw new IllegalArgumentException("unsupported_structure_parts", new IllegalStateException(
                    "model=" + legacy.id() + " missing=" + expected.entrySet().stream()
                    .filter(entry -> !planned.containsKey(entry.getKey())).limit(3).toList()
                    + " extra=" + planned.keySet().stream().filter(at -> !expected.containsKey(at)).limit(3).toList()));
        for (var entry : expected.entrySet()) {
            var offset = MinecraftTypeAdapter.toColonyPos(entry.getKey());
            var raw = new BlueprintBlock(offset, ResourceId.parse(Registries.BLOCK.getId(entry.getValue().getBlock()).toString()),
                    false, Optional.empty());
            var normalizedPart = WhitePieces.baked(DesertSand.baked(legacy.id(), java.util.List.of(withState(raw, entry.getValue())))).getFirst();
            if (!PaletteStates.planned(normalizedPart).equals(planned.get(entry.getKey())))
                throw new IllegalArgumentException("structure_part_state_mismatch");
        }
        // A rotação do próprio bloco comprova propriedades direcionais não tratadas pelo Core — ADR-046.
        for (int turn = 1; turn < 4; turn++) {
            var rotated = result.rotated(turn);
            for (int i = 0; i < result.blocks().size(); i++) {
                var actual = PaletteStates.planned(result.blocks().get(i));
                for (int t = 0; t < turn; t++) actual = actual.rotate(BlockRotation.CLOCKWISE_90);
                if (!actual.equals(PaletteStates.planned(rotated.blocks().get(i))))
                    throw new IllegalArgumentException("unsupported_structure_rotation");
            }
        }
        return result;
    }

    private static BlueprintBlock withState(BlueprintBlock piece, BlockState state) {
        Map<String, String> properties = new TreeMap<>();
        var serialized = NbtHelper.fromBlockState(state).getCompound("Properties");
        for (String key : serialized.getKeys()) properties.put(key, serialized.getString(key));
        var facing = piece.facing();
        if (state.contains(Properties.HORIZONTAL_FACING)) {
            facing = Optional.of(MinecraftTypeAdapter.toSide(state.get(Properties.HORIZONTAL_FACING)));
            properties.remove("facing");
        }
        return new BlueprintBlock(piece.offset(), piece.block(), piece.furniture(), facing, properties);
    }

    private static BlockState promised(NbtCompound payload) {
        try {
            var reader = new StringReader(payload.getString("final_state"));
            var result = BlockArgumentParser.block(Registries.BLOCK.getReadOnlyWrapper(), reader, false);
            reader.skipWhitespace();
            if (reader.canRead() || result.blockState().isOf(Blocks.JIGSAW))
                throw new IllegalArgumentException("unsafe_structure_final_state");
            return result.blockState();
        } catch (CommandSyntaxException invalid) {
            throw new IllegalArgumentException("unsafe_structure_final_state", invalid);
        }
    }

    private static void safePayload(NbtCompound payload) {
        for (String key : payload.getKeys()) {
            boolean safe = Set.of("id", "x", "y", "z").contains(key)
                    || (key.equals("Items") && payload.get(key) instanceof NbtList items && items.isEmpty())
                    || (key.equals("components") && payload.contains(key, NbtElement.COMPOUND_TYPE)
                    && payload.getCompound(key).isEmpty());
            if (!safe) throw new IllegalArgumentException("unsafe_structure_payload");
        }
    }
}
