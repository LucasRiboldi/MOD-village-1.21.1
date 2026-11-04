package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.ShapeStates;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;
import com.villagecolony.core.type.Side;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.Properties;

import java.util.Map;
import java.util.Optional;

/** O que a planta lê do estado de cada entrada da paleta: lado e forma. */
public final class PaletteStates {

    private PaletteStates() {
    }

    /** Estado MAE explícito, sem inferir a parede nem aceitar propriedade inválida — ADR-046. */
    public static BlockState planned(BlueprintBlock block) {
        BlockState state = MinecraftTypeAdapter.toBlock(block.block())
                .orElseThrow(() -> new IllegalArgumentException("unknown_structure_block")).getDefaultState();
        if (block.facing().isPresent() && state.contains(Properties.HORIZONTAL_FACING))
            state = state.with(Properties.HORIZONTAL_FACING, MinecraftTypeAdapter.toDirection(block.facing().orElseThrow()));
        // Barril e afins: o facing de seis direções, quando horizontal, gira pelo mesmo Side — ADR-046.
        else if (block.facing().isPresent() && state.contains(Properties.FACING))
            state = state.with(Properties.FACING, MinecraftTypeAdapter.toDirection(block.facing().orElseThrow()));
        for (var planned : block.states().entrySet()) {
            Property<?> property = state.getBlock().getStateManager().getProperty(planned.getKey());
            if (property == null || property.parse(planned.getValue()).isEmpty())
                throw new IllegalArgumentException("invalid_structure_property:" + planned.getKey());
            state = withValue(state, property, planned.getValue());
        }
        return state;
    }

    private static <T extends Comparable<T>> BlockState withValue(BlockState state, Property<T> property, String value) {
        return state.with(property, property.parse(value).orElseThrow());
    }

    /** Eixo, metade e formato da paleta — ADR-036 item 19. */
    static Map<String, String> shapeOf(NbtCompound paletteEntry) {
        NbtCompound properties = paletteEntry.getCompound(StructureBlueprintReader.PROPERTIES_KEY);
        Map<String, String> all = new java.util.HashMap<>();

        for (String key : properties.getKeys()) {
            all.put(key, properties.getString(key));
        }

        return ShapeStates.of(all);
    }

    /** A propriedade horizontal aprovada pela ADR-008, quando presente. */
    static Optional<Side> facingOf(NbtCompound paletteEntry) {
        return sideOf(paletteEntry.getCompound(StructureBlueprintReader.PROPERTIES_KEY)
                .getString(Properties.HORIZONTAL_FACING.getName()));
    }

    /** Le a mesma propriedade do estado textual carregado por um jigsaw. */
    static Optional<Side> facingOf(String state) {
        int opens = state.indexOf('[');
        int closes = state.indexOf(']', opens + 1);

        if (opens < 0 || closes < 0) {
            return Optional.empty();
        }

        String properties = state.substring(opens + 1, closes);
        int start = 0;

        while (start < properties.length()) {
            int comma = properties.indexOf(',', start);
            int end = comma < 0 ? properties.length() : comma;
            int equals = properties.indexOf('=', start);

            if (equals > start && equals < end
                    && properties.substring(start, equals)
                            .equals(Properties.HORIZONTAL_FACING.getName())) {
                return sideOf(properties.substring(equals + 1, end));
            }

            start = end + 1;
        }

        return Optional.empty();
    }

    private static Optional<Side> sideOf(String name) {
        return switch (name) {
            case "north" -> Optional.of(Side.NORTH);
            case "south" -> Optional.of(Side.SOUTH);
            case "east" -> Optional.of(Side.EAST);
            case "west" -> Optional.of(Side.WEST);
            default -> Optional.empty();
        };
    }
}
