package com.villagecolony.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Uma placa virada para a câmera: texto à esquerda e símbolo simples à
 * direita, ambos dentro da mesma moldura — pedido do autor, 2026-10-03.
 *
 * <p><b>O fundo acompanha o texto.</b> A moldura de 64 × 32 é cortada em nove
 * partes: os cantos ficam nítidos e o meio estica, então a placa cresce e
 * encolhe com o texto da obra sem deformar a borda.
 *
 * <p><b>Desenhado no {@code WorldRenderEvents.LAST}, com buffer próprio</b>,
 * e descarregado placa a placa: sem profundidade, quem é desenhado depois
 * fica por cima, e o fundo tem de vir antes do ícone e do texto.
 */
final class OverlayDrawing {

    /** Tamanho de uma unidade de texto no mundo: 40 unidades por bloco. */
    static final float SCALE = 0.025F;

    /** A moldura nítida: 64 × 32, com borda de madeira e metal nos cantos. */
    private static final float TEXTURE_WIDTH = 64.0F;
    private static final float TEXTURE_HEIGHT = 32.0F;
    private static final float FRAME_MIN_U = 0;
    private static final float FRAME_MAX_U = 64;
    private static final float FRAME_MIN_V = 0;
    private static final float FRAME_MAX_V = 32;
    private static final float BORDER_U = 8;
    private static final float BORDER_V = 8;

    /** A borda desenhada, em unidades de texto. */
    private static final float BORDER_DRAWN = 3.0F;

    private static final String PANEL = "textures/gui/overlays/ui/panel_background.png";

    private static final int FULL_BRIGHT = 0xF000F0;

    /** Camadas separadas para o fundo nunca cobrir texto ou símbolo. */
    static final float PANEL_Z = 0.0F;
    static final float ICON_Z = 0.01F;
    static final float TEXT_Z = 0.02F;

    private OverlayDrawing() {
    }

    /** O buffer imediato do cliente; cada placa é descarregada ao terminar. */
    static VertexConsumerProvider.Immediate buffers() {
        return MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
    }

    /**
     * A placa inteira, com o fundo logo acima de {@code at}.
     *
     * @param lines texto e cor (ARGB) de cada linha, de cima para baixo
     */
    static void label(Camera camera, VertexConsumerProvider.Immediate buffers, Vec3d at, Identifier icon,
            Text[] lines, int[] colors) {

        TextRenderer text = MinecraftClient.getInstance().textRenderer;
        float lineHeight = text.fontHeight + 1;
        float[] textWidths = new float[lines.length];

        for (int i = 0; i < lines.length; i++) {
            textWidths[i] = text.getWidth(lines[i]);
        }

        PixelPanelLayout layout = PixelPanelLayout.withText(textWidths, lineHeight);
        float left = -layout.width() / 2.0F;

        Vec3d relative = at.subtract(camera.getPos());
        MatrixStack matrices = new MatrixStack();

        matrices.translate(relative.x, relative.y, relative.z);
        matrices.multiply(camera.getRotation());
        matrices.scale(-SCALE, -SCALE, SCALE);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Matrix4f panelMatrix = new Matrix4f(matrix).translate(0.0F, 0.0F, PANEL_Z);
        Matrix4f iconMatrix = new Matrix4f(matrix).translate(0.0F, 0.0F, ICON_Z);
        Matrix4f textMatrix = new Matrix4f(matrix).translate(0.0F, 0.0F, TEXT_Z);

        if (layout.hasFrame()) {
            panel(buffers.getBuffer(RenderLayer.getTextSeeThrough(OverlayDrawing.id(PANEL))), panelMatrix,
                    left + layout.frameLeft(), layout.frameTop(),
                    left + layout.frameRight(), layout.frameBottom());
            buffers.draw();
        }

        quad(buffers.getBuffer(RenderLayer.getTextSeeThrough(icon)), iconMatrix,
                left + layout.iconLeft(), layout.iconTop(),
                left + layout.iconRight(), layout.iconBottom(), 0, 0, 1, 1);
        buffers.draw();

        for (int i = 0; i < lines.length; i++) {
            text.draw(lines[i], left + layout.textLeft(i), layout.textTop() + i * lineHeight, colors[i], true, textMatrix,
                    buffers, TextRenderer.TextLayerType.SEE_THROUGH, 0, FULL_BRIGHT);
        }

        buffers.draw();
    }

    /** A moldura em nove partes: cantos do tamanho deles, o meio esticado. */
    private static void panel(VertexConsumer buffer, Matrix4f matrix, float x0, float y0, float x1, float y1) {
        float[] xs = {x0, x0 + BORDER_DRAWN, x1 - BORDER_DRAWN, x1};
        float[] ys = {y0, y0 + BORDER_DRAWN, y1 - BORDER_DRAWN, y1};
        float[] us = {FRAME_MIN_U, FRAME_MIN_U + BORDER_U, FRAME_MAX_U - BORDER_U, FRAME_MAX_U};
        float[] vs = {FRAME_MIN_V, FRAME_MIN_V + BORDER_V, FRAME_MAX_V - BORDER_V, FRAME_MAX_V};

        for (int col = 0; col < 3; col++) {
            for (int row = 0; row < 3; row++) {
                quad(buffer, matrix, xs[col], ys[row], xs[col + 1], ys[row + 1],
                        us[col] / TEXTURE_WIDTH, vs[row] / TEXTURE_HEIGHT,
                        us[col + 1] / TEXTURE_WIDTH, vs[row + 1] / TEXTURE_HEIGHT);
            }
        }
    }

    /** Um quadrado texturizado, nas duas faces: a escala negativa vira a frente. */
    private static void quad(VertexConsumer buffer, Matrix4f matrix, float x0, float y0, float x1, float y1,
            float u0, float v0, float u1, float v1) {
        vertex(buffer, matrix, x0, y1, u0, v1);
        vertex(buffer, matrix, x1, y1, u1, v1);
        vertex(buffer, matrix, x1, y0, u1, v0);
        vertex(buffer, matrix, x0, y0, u0, v0);

        vertex(buffer, matrix, x0, y0, u0, v0);
        vertex(buffer, matrix, x1, y0, u1, v0);
        vertex(buffer, matrix, x1, y1, u1, v1);
        vertex(buffer, matrix, x0, y1, u0, v1);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f matrix, float x, float y, float u, float v) {
        buffer.vertex(matrix, x, y, 0.0F).color(0xFFFFFFFF).texture(u, v).light(FULL_BRIGHT);
    }

    static Identifier id(String path) {
        return Identifier.of(OverlaySprites.NAMESPACE, path);
    }
}
