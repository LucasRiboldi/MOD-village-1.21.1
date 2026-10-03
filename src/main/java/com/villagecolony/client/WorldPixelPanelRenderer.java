package com.villagecolony.client;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

/** Desenha a moldura em nove partes para os cantos não esticarem com o texto. */
final class WorldPixelPanelRenderer {
    private static final float BORDER = 6.0F;
    private static final float SOURCE_WIDTH = 64.0F;
    private static final float SOURCE_HEIGHT = 32.0F;
    private static final float FIRST_LINE_Y = 3.0F;
    private static final float SECOND_LINE_Y = 15.0F;

    private WorldPixelPanelRenderer() {
    }

    static void draw(WorldRenderContext context, TextRenderer textRenderer, PixelPanelLayout layout,
            Text firstLine, int firstColor, Text secondLine, int secondColor) {
        Matrix4f matrix = context.matrixStack().peek().getPositionMatrix();
        float left = -layout.width() / 2.0F;

        if (layout.hasFrame()) {
            drawFrame(context, matrix, left, 0.0F, layout.width(), layout.height());
        }
        drawQuad(context, layout.texture(), matrix,
                left + layout.iconX(), layout.iconY(),
                PixelPanelLayout.ICON_SIZE, PixelPanelLayout.ICON_SIZE,
                0.0F, 0.0F, 1.0F, 1.0F);

        if (layout.hasFrame()) {
            textRenderer.draw(firstLine, left + layout.firstTextX(), FIRST_LINE_Y, firstColor,
                    false, matrix, context.consumers(), TextRenderer.TextLayerType.SEE_THROUGH,
                    0, 0xF000F0);
            if (secondLine != null) {
                textRenderer.draw(secondLine, left + layout.secondTextX(), SECOND_LINE_Y, secondColor,
                        false, matrix, context.consumers(), TextRenderer.TextLayerType.SEE_THROUGH,
                        0, 0xF000F0);
            }
        }
    }

    private static void drawFrame(WorldRenderContext context, Matrix4f matrix,
            float x, float y, float width, float height) {
        float middleWidth = width - 2.0F * BORDER;
        float middleHeight = height - 2.0F * BORDER;
        float u = BORDER / SOURCE_WIDTH;
        float v = BORDER / SOURCE_HEIGHT;

        drawQuad(context, PixelPanelLayout.BACKGROUND, matrix, x, y, BORDER, BORDER,
                0.0F, 0.0F, u, v);
        drawQuad(context, PixelPanelLayout.BACKGROUND, matrix, x + BORDER, y, middleWidth, BORDER,
                u, 0.0F, 1.0F - u, v);
        drawQuad(context, PixelPanelLayout.BACKGROUND, matrix, x + width - BORDER, y, BORDER, BORDER,
                1.0F - u, 0.0F, 1.0F, v);
        drawQuad(context, PixelPanelLayout.BACKGROUND, matrix, x, y + BORDER, BORDER, middleHeight,
                0.0F, v, u, 1.0F - v);
        drawQuad(context, PixelPanelLayout.BACKGROUND, matrix, x + BORDER, y + BORDER,
                middleWidth, middleHeight, u, v, 1.0F - u, 1.0F - v);
        drawQuad(context, PixelPanelLayout.BACKGROUND, matrix, x + width - BORDER, y + BORDER,
                BORDER, middleHeight, 1.0F - u, v, 1.0F, 1.0F - v);
        drawQuad(context, PixelPanelLayout.BACKGROUND, matrix, x, y + height - BORDER,
                BORDER, BORDER, 0.0F, 1.0F - v, u, 1.0F);
        drawQuad(context, PixelPanelLayout.BACKGROUND, matrix, x + BORDER, y + height - BORDER,
                middleWidth, BORDER, u, 1.0F - v, 1.0F - u, 1.0F);
        drawQuad(context, PixelPanelLayout.BACKGROUND, matrix, x + width - BORDER,
                y + height - BORDER, BORDER, BORDER, 1.0F - u, 1.0F - v, 1.0F, 1.0F);
    }

    private static void drawQuad(WorldRenderContext context, Identifier texture, Matrix4f matrix,
            float x, float y, float width, float height, float u0, float v0, float u1, float v1) {
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }
        VertexConsumer vertices = context.consumers().getBuffer(RenderLayer.getTextSeeThrough(texture));
        vertex(vertices, matrix, x, y + height, u0, v1);
        vertex(vertices, matrix, x + width, y + height, u1, v1);
        vertex(vertices, matrix, x + width, y, u1, v0);
        vertex(vertices, matrix, x, y, u0, v0);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, float x, float y, float u, float v) {
        vertices.vertex(matrix, x, y, 0.0F)
                .color(255, 255, 255, 255)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(0xF000F0)
                .normal(0.0F, 0.0F, 1.0F);
    }
}
