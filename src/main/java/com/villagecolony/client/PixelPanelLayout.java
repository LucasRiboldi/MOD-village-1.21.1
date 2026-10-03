package com.villagecolony.client;

import net.minecraft.util.Identifier;

import java.util.Locale;
import java.util.Map;

/** Geometria imutável de uma moldura pixelada orientada à câmera. */
public record PixelPanelLayout(
        Identifier texture,
        int width,
        int height,
        int iconX,
        int iconY,
        int firstTextX,
        int secondTextX,
        boolean hasFrame) {

    static final int ICON_SIZE = 16;
    static final int PADDING = 3;
    static final int SINGLE_LINE_HEIGHT = 15;
    static final int TWO_LINE_HEIGHT = 27;
    static final Identifier UNKNOWN = texture("ui/warning");
    static final Identifier BACKGROUND = texture("ui/panel_background");

    private static final int MIN_FRAME_WIDTH = 32;
    private static final int ICON_ABOVE_FRAME_Y = -18;

    private static final Map<String, Identifier> PROFESSIONS = Map.ofEntries(
            Map.entry("BUILDER", texture("professions/builder")),
            Map.entry("CARPENTER", texture("professions/carpenter")),
            Map.entry("FARMER", texture("professions/farmer")),
            Map.entry("LUMBERJACK", texture("professions/lumberjack")),
            Map.entry("MASON", texture("professions/mason")),
            Map.entry("MINER", texture("professions/miner")),
            Map.entry("SHEPHERD", texture("professions/shepherd")),
            Map.entry("SMELTER", texture("professions/smelter")));
    private static final Map<String, Identifier> CONSTRUCTIONS = Map.of(
            "PLANNED", texture("construction/building"),
            "PREPARING", texture("construction/building"),
            "BUILDING", texture("construction/building"),
            "COMPLETED", texture("construction/completed"),
            "WAITING_RESOURCES", texture("construction/waiting_material"));

    public static PixelPanelLayout singleLine(Identifier icon, int textWidth, boolean showText) {
        if (!showText) {
            return new PixelPanelLayout(icon, ICON_SIZE, 0, 0, -ICON_SIZE, 0, 0, false);
        }
        int width = Math.max(MIN_FRAME_WIDTH, textWidth + 2 * PADDING);
        return new PixelPanelLayout(icon, width, SINGLE_LINE_HEIGHT,
                centered(width, ICON_SIZE), ICON_ABOVE_FRAME_Y,
                centered(width, textWidth), 0, true);
    }

    public static PixelPanelLayout twoLines(Identifier icon, int firstTextWidth, int secondTextWidth) {
        int width = Math.max(MIN_FRAME_WIDTH, Math.max(firstTextWidth, secondTextWidth) + 2 * PADDING);
        return new PixelPanelLayout(icon, width, TWO_LINE_HEIGHT,
                centered(width, ICON_SIZE), ICON_ABOVE_FRAME_Y,
                centered(width, firstTextWidth), centered(width, secondTextWidth), true);
    }

    public static Identifier professionTexture(String profession) {
        return PROFESSIONS.getOrDefault(normalize(profession), UNKNOWN);
    }

    public static Identifier constructionTexture(String state) {
        return CONSTRUCTIONS.getOrDefault(normalize(state), UNKNOWN);
    }

    private static int centered(int outer, int inner) {
        return (outer - inner) / 2;
    }

    private static Identifier texture(String path) {
        return Identifier.of("villagecolony", "textures/gui/overlays/" + path + ".png");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT);
    }
}
