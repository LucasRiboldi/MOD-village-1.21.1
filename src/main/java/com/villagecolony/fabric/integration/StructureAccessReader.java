package com.villagecolony.fabric.integration;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.BlueprintAccess;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.Identifier;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Candidatos do template carregado, sem certificação de circulação ou escrita — ADR-046. */
final class StructureAccessReader {
    private static final Identifier DECLARATIONS = Identifier.of("villagecolony", "mae/structure_access.json");
    private static final Comparator<ColonyPos> ORDER = Comparator.comparingInt(ColonyPos::y)
            .thenComparingInt(ColonyPos::x).thenComparingInt(ColonyPos::z);

    private StructureAccessReader() { }

    static List<BlueprintAccess> read(ServerWorld world, ResourceId model, StructureTemplate template) {
        NbtCompound nbt = template.writeNbt(new NbtCompound());
        NbtList palette = nbt.getList(StructureTemplate.PALETTE_KEY, NbtElement.COMPOUND_TYPE);
        NbtList entries = nbt.getList(StructureTemplate.BLOCKS_KEY, NbtElement.COMPOUND_TYPE);
        List<BlueprintAccess> candidates = new ArrayList<>();
        Map<ColonyPos, String> names = new HashMap<>();
        for (int i = 0; i < entries.size(); i++) {
            NbtCompound entry = entries.getCompound(i);
            int stateIndex = entry.getInt(StructureTemplate.BLOCKS_STATE_KEY);
            if (stateIndex < 0 || stateIndex >= palette.size()) continue;
            NbtCompound state = palette.getCompound(stateIndex);
            ResourceId id = ResourceId.parse(state.getString(StructureBlueprintReader.BLOCK_NAME_KEY));
            ColonyPos at = StructureBlueprintReader.offsetOf(entry);
            names.put(at, id.toString());
            if (StructureBlueprintReader.isJigsaw(id)) {
                NbtCompound data = entry.getCompound("nbt");
                if (!"minecraft:building_entrance".equals(data.getString("name"))) continue;
                String orientation = state.getCompound(StructureBlueprintReader.PROPERTIES_KEY)
                        .getString("orientation");
                int separator = orientation.indexOf('_');
                if (separator < 0) continue;
                side(orientation.substring(0, separator)).ifPresent(outward -> {
                    // O encaixe horizontal marca o apoio; a passagem começa acima dele — ADR-046.
                    ColonyPos feet = new ColonyPos(at.x(), at.y() + 1, at.z());
                    candidates.add(new BlueprintAccess(BlueprintAccess.Kind.OPENING, List.of(feet), outward));
                });
                continue;
            }
            var block = MinecraftTypeAdapter.toBlock(id);
            BlueprintAccess.Kind kind;
            if (block.orElse(null) instanceof DoorBlock) {
                if ("upper".equals(state.getCompound(StructureBlueprintReader.PROPERTIES_KEY).getString("half"))) continue;
                kind = BlueprintAccess.Kind.DOOR;
            } else if (block.orElse(null) instanceof FenceGateBlock) {
                kind = BlueprintAccess.Kind.GATE;
            } else continue;
            PaletteStates.facingOf(state).ifPresent(facing -> {
                // Facing não distingue interior de exterior; ambos seguem para a prova física — ADR-046.
                candidates.add(new BlueprintAccess(kind, List.of(at), facing));
                candidates.add(new BlueprintAccess(kind, List.of(at), facing.opposite()));
            });
        }
        List<BlueprintAccess> result = grouped(candidates);
        for (BlueprintAccess declared : declarations(world, model)) {
            boolean clear = declared.opening().stream().allMatch(at -> {
                ColonyPos head = new ColonyPos(at.x(), at.y() + 1, at.z());
                return inside(template, at) && inside(template, head) && air(names, at) && air(names, head);
            });
            if (clear) result.add(declared);
        }
        return result.stream().distinct().toList();
    }

    private static boolean inside(StructureTemplate template, ColonyPos at) {
        var size = template.getSize();
        return at.x() >= 0 && at.y() >= 0 && at.z() >= 0
                && at.x() < size.getX() && at.y() < size.getY() && at.z() < size.getZ();
    }

    private static boolean air(Map<ColonyPos, String> names, ColonyPos at) {
        String name = names.getOrDefault(at, "minecraft:air");
        return name.equals("minecraft:air") || name.equals("minecraft:cave_air") || name.equals("minecraft:void_air");
    }

    private static List<BlueprintAccess> grouped(List<BlueprintAccess> candidates) {
        List<BlueprintAccess> pending = new ArrayList<>(candidates);
        List<BlueprintAccess> result = new ArrayList<>();
        while (!pending.isEmpty()) {
            BlueprintAccess seed = pending.removeFirst();
            List<ColonyPos> opening = new ArrayList<>(seed.opening());
            if (seed.kind() != BlueprintAccess.Kind.OPENING) {
                boolean changed;
                do {
                    changed = false;
                    for (var iterator = pending.iterator(); iterator.hasNext();) {
                        BlueprintAccess next = iterator.next();
                        if (next.kind() != seed.kind() || next.outward() != seed.outward()) continue;
                        ColonyPos at = next.opening().getFirst();
                        boolean adjacent = opening.stream().anyMatch(other -> other.y() == at.y()
                                && (seed.outward().offsetX() == 0
                                ? other.z() == at.z() && Math.abs(other.x() - at.x()) == 1
                                : other.x() == at.x() && Math.abs(other.z() - at.z()) == 1));
                        if (adjacent) {
                            opening.add(at);
                            iterator.remove();
                            changed = true;
                        }
                    }
                } while (changed);
            }
            opening.sort(ORDER);
            result.add(new BlueprintAccess(seed.kind(), opening, seed.outward()));
        }
        return result;
    }

    private static java.util.Optional<Side> side(String value) {
        return switch (value) {
            case "north" -> java.util.Optional.of(Side.NORTH);
            case "south" -> java.util.Optional.of(Side.SOUTH);
            case "east" -> java.util.Optional.of(Side.EAST);
            case "west" -> java.util.Optional.of(Side.WEST);
            default -> java.util.Optional.empty();
        };
    }

    private static List<BlueprintAccess> declarations(ServerWorld world, ResourceId model) {
        var resource = world.getServer().getResourceManager().getResource(DECLARATIONS);
        if (resource.isEmpty()) return List.of();
        try (var reader = new InputStreamReader(resource.get().getInputStream(), StandardCharsets.UTF_8)) {
            var root = JsonParser.parseReader(reader).getAsJsonObject();
            if (!root.has(model.toString())) return List.of();
            List<BlueprintAccess> result = new ArrayList<>();
            for (JsonElement entry : root.getAsJsonArray(model.toString())) {
                var declaration = entry.getAsJsonObject();
                Side outward = side(declaration.get("outward").getAsString().toLowerCase(Locale.ROOT)).orElseThrow();
                List<ColonyPos> opening = new ArrayList<>();
                for (JsonElement value : declaration.getAsJsonArray("opening")) {
                    var pos = value.getAsJsonArray();
                    if (pos.size() != 3) throw new IllegalArgumentException("invalid_access_position");
                    opening.add(new ColonyPos(pos.get(0).getAsInt(), pos.get(1).getAsInt(), pos.get(2).getAsInt()));
                }
                result.add(new BlueprintAccess(BlueprintAccess.Kind.OPENING, opening, outward));
            }
            return result;
        } catch (Exception invalid) {
            VillageColonyMod.LOGGER.warn("Could not validate MAE access declarations for {}", model, invalid);
            return List.of();
        }
    }
}
