package com.villagecolony.core.construction.model;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * As posições de um bloco que a planta guarda além do {@code facing} —
 * ADR-036 item 19: eixo ({@code axis}), metade ({@code half} de escada e
 * alçapão; {@code type} de laje) e formato de escada ({@code shape}).
 *
 * <p>Ficam como texto, nome e valor do Minecraft: o Core não conhece blocos
 * (ADR-005), só carrega o que o arquivo disse até a colocação. Só passam os
 * valores destas quatro famílias — o {@code half} de porta ({@code upper/lower})
 * e o {@code type} de baú ({@code left/right}) têm donos próprios.
 */
public final class ShapeStates {

    private static final Set<String> AXES = Set.of("x", "y", "z");

    private static final Set<String> HALVES = Set.of("top", "bottom");

    private static final Set<String> SLAB_TYPES = Set.of("top", "bottom", "double");

    private static final Set<String> STAIR_SHAPES =
            Set.of("straight", "inner_left", "inner_right", "outer_left", "outer_right");

    private ShapeStates() {
    }

    /** As propriedades que passam, de todas as que o arquivo trouxe. */
    public static Map<String, String> of(Map<String, String> properties) {
        Map<String, String> kept = new TreeMap<>();

        properties.forEach((name, value) -> {
            boolean keep = switch (name) {
                case "axis" -> AXES.contains(value);
                case "half" -> HALVES.contains(value);
                case "type" -> SLAB_TYPES.contains(value);
                case "shape" -> STAIR_SHAPES.contains(value);
                default -> false;
            };

            if (keep) {
                kept.put(name, value);
            }
        });

        return Map.copyOf(kept);
    }

    /** O mesmo, lendo o estado em texto: {@code minecraft:oak_log[axis=x]}. */
    public static Map<String, String> parse(String state) {
        int opens = state.indexOf('[');
        int closes = state.indexOf(']', opens + 1);

        if (opens < 0 || closes < 0) {
            return Map.of();
        }

        Map<String, String> properties = new TreeMap<>();

        String text = state.substring(opens + 1, closes);
        int start = 0;

        while (start < text.length()) {
            int comma = text.indexOf(',', start);
            int end = comma < 0 ? text.length() : comma;
            int equals = text.indexOf('=', start);

            if (equals > start && equals < end) {
                properties.put(text.substring(start, equals).trim(), text.substring(equals + 1, end).trim());
            }

            start = end + 1;
        }

        return of(properties);
    }

    /** Girar a planta um quarto de volta troca o eixo X pelo Z; o resto não muda. */
    public static Map<String, String> turned(Map<String, String> states, int turns) {
        String axis = states.get("axis");

        if (Math.floorMod(turns, 2) == 0 || axis == null || "y".equals(axis)) {
            return states;
        }

        Map<String, String> turned = new TreeMap<>(states);

        turned.put("axis", "x".equals(axis) ? "z" : "x");

        return Map.copyOf(turned);
    }
}
