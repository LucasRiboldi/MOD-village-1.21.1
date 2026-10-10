package com.villagecolony.client;

/**
 * Geometria da moldura pixelada do overlay.
 *
 * <p>ADR-025: no painel de profissão, a moldura pertence ao texto e ao ícone.
 * O nome fica à esquerda e o sprite à direita, sempre dentro do mesmo
 * background, para não recriar a plaquinha Vanilla fora da moldura.
 */
record PixelPanelLayout(
        float width,
        float height,
        float iconLeft,
        float iconTop,
        float frameLeft,
        float frameTop,
        float frameWidth,
        float frameHeight,
        float firstTextLeft,
        float secondTextLeft,
        float textTop,
        boolean hasFrame) {

    static final float ICON_SIZE = 16.0F;
    static final float PADDING = 4.0F;
    static final float GAP = 4.0F;

    private static final float MIN_FRAME_WIDTH = 32.0F;

    static PixelPanelLayout iconOnly() {
        return new PixelPanelLayout(ICON_SIZE, ICON_SIZE,
                0.0F, -ICON_SIZE,
                0.0F, 0.0F, 0.0F, 0.0F,
                0.0F, 0.0F, 0.0F, false);
    }

    static PixelPanelLayout withText(float[] textWidths, float lineHeight) {
        if (textWidths.length == 0) {
            return iconOnly();
        }

        float widest = 0.0F;
        for (float width : textWidths) {
            widest = Math.max(widest, width);
        }

        float textHeight = textWidths.length * lineHeight - 1.0F;
        float contentHeight = Math.max(ICON_SIZE, textHeight);
        float frameWidth = Math.max(
                MIN_FRAME_WIDTH,
                widest + GAP + ICON_SIZE + 2.0F * PADDING);
        float frameHeight = contentHeight + 2.0F * PADDING;
        float width = frameWidth;
        float height = frameHeight;
        float frameLeft = 0.0F;
        float frameTop = -frameHeight;
        float iconLeft = frameLeft + frameWidth - PADDING - ICON_SIZE;
        float iconTop = frameTop + PADDING + (contentHeight - ICON_SIZE) / 2.0F;
        float firstTextLeft = frameLeft + PADDING;
        float secondTextLeft = firstTextLeft;
        float textTop = frameTop + PADDING + (contentHeight - textHeight) / 2.0F;

        return new PixelPanelLayout(width, height,
                iconLeft, iconTop,
                frameLeft, frameTop, frameWidth, frameHeight,
                firstTextLeft, secondTextLeft, textTop, true);
    }

    float iconRight() {
        return iconLeft + ICON_SIZE;
    }

    float iconBottom() {
        return iconTop + ICON_SIZE;
    }

    float frameRight() {
        return frameLeft + frameWidth;
    }

    float frameBottom() {
        return frameTop + frameHeight;
    }

    float textLeft(int line) {
        return line == 0 ? firstTextLeft : secondTextLeft;
    }

}
