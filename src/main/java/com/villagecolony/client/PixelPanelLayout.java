package com.villagecolony.client;

/**
 * Geometria da moldura pixelada do overlay.
 *
 * <p>ADR-025: no painel de profissão, a moldura pertence ao texto e o ícone
 * fica centralizado acima dela. Assim o nome traduzido não disputa largura com
 * o sprite e sempre cabe dentro do background.
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
        float frameWidth = Math.max(MIN_FRAME_WIDTH, widest + 2.0F * PADDING);
        float frameHeight = textHeight + 2.0F * PADDING;
        float width = Math.max(frameWidth, ICON_SIZE);
        float height = ICON_SIZE + GAP + frameHeight;
        float frameLeft = (width - frameWidth) / 2.0F;
        float frameTop = -frameHeight;
        float iconLeft = (width - ICON_SIZE) / 2.0F;
        float iconTop = frameTop - GAP - ICON_SIZE;
        float firstTextLeft = centeredText(frameLeft, frameWidth, textWidths[0]);
        float secondTextLeft = textWidths.length > 1
                ? centeredText(frameLeft, frameWidth, textWidths[1])
                : firstTextLeft;

        return new PixelPanelLayout(width, height,
                iconLeft, iconTop,
                frameLeft, frameTop, frameWidth, frameHeight,
                firstTextLeft, secondTextLeft, frameTop + PADDING, true);
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

    private static float centeredText(float frameLeft, float frameWidth, float textWidth) {
        return frameLeft + (frameWidth - textWidth) / 2.0F;
    }
}
