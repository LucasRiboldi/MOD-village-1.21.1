package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.ShapeStates;
import com.villagecolony.core.type.Side;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.Properties;

import java.util.Map;
import java.util.Optional;

/** O que a planta lê do estado de cada entrada da paleta: lado e forma. */
final class PaletteStates {

    private PaletteStates() {
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
