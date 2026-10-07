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
 * Uma placa virada para a câmera: fundo em pixel art, ícone à esquerda e o
 * texto à direita — pedido do autor, 2026-10-03.
 *
 * <p><b>O fundo acompanha o texto.</b> A moldura de 16 × 16 é cortada em nove
 * partes: os cantos ficam do tamanho deles e o meio estica, então a placa
 * cresce e encolhe com o texto da obra sem deformar a borda.
 *
 * <p><b>Desenhado no {@code WorldRenderEvents.LAST}, com buffer próprio</b>,
 * e descarregado placa a placa: sem profundidade, quem é desenhado depois
 * fica por cima, e o fundo tem de vir antes do ícone e do texto.
 */
final class OverlayDrawing {

    /** Tamanho de uma unidade de texto no mundo: 40 unidades por bloco. */
    static final float SCALE = 0.025F;

    /** Lado do ícone, em unidades de texto. */
    private static final float ICON = 16.0F;

    /** Folga entre a borda do fundo e o conteúdo. */
    private static final float PAD = 4.0F;

    /** Espaço entre o ícone e o texto. */
    private static final float GAP = 4.0F;

    /** A moldura: 16 × 16, conteúdo entre x 1..14 e y 2..12, borda de 2. */
    private static final float TEXTURE = 16.0F;
    private static final float FRAME_MIN_U = 1;
    private static final float FRAME_MAX_U = 15;
    private static final float FRAME_MIN_V = 2;
    private static final float FRAME_MAX_V = 13;
    private static final float BORDER = 2;

    /** A borda desenhada, em unidades de texto. */
    private static final float BORDER_DRAWN = 3.0F;

    private static final String PANEL = "textures/gui/overlays/ui/panel_background.png";

    private static final int FULL_BRIGHT = 0xF000F0;

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
        float textWidth = 0;

        for (Text line : lines) {
            textWidth = Math.max(textWidth, text.getWidth(line));
        }

        float textHeight = lines.length * lineHeight - 1;
        float contentHeight = Math.max(ICON, textHeight);
        float width = PAD + ICON + GAP + textWidth + PAD;
        float height = PAD + contentHeight + PAD;
        float left = -width / 2;
        float top = -height;

        Vec3d relative = at.subtract(camera.getPos());
        MatrixStack matrices = new MatrixStack();

        matrices.translate(relative.x, relative.y, relative.z);
        matrices.multiply(camera.getRotation());
        matrices.scale(-SCALE, -SCALE, SCALE);

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        panel(buffers.getBuffer(RenderLayer.getTextSeeThrough(OverlayDrawing.id(PANEL))), matrix,
                left, top, left + width, top + height);
        buffers.draw();

        // O ícone no alto, à esquerda.
        quad(buffers.getBuffer(RenderLayer.getTextSeeThrough(icon)), matrix,
                left + PAD, top + PAD, left + PAD + ICON, top + PAD + ICON, 0, 0, 1, 1);
        buffers.draw();

        // O texto à direita; uma linha só fica no meio da altura do ícone.
        float textTop = top + PAD + Math.max(0, (contentHeight - textHeight) / 2);

        for (int i = 0; i < lines.length; i++) {
            text.draw(lines[i], left + PAD + ICON + GAP, textTop + i * lineHeight, colors[i], true, matrix,
                    buffers, TextRenderer.TextLayerType.SEE_THROUGH, 0, FULL_BRIGHT);
        }

        buffers.draw();
    }

    /** A moldura em nove partes: cantos do tamanho deles, o meio esticado. */
    private static void panel(VertexConsumer buffer, Matrix4f matrix, float x0, float y0, float x1, float y1) {
        float[] xs = {x0, x0 + BORDER_DRAWN, x1 - BORDER_DRAWN, x1};
        float[] ys = {y0, y0 + BORDER_DRAWN, y1 - BORDER_DRAWN, y1};
        float[] us = {FRAME_MIN_U, FRAME_MIN_U + BORDER, FRAME_MAX_U - BORDER, FRAME_MAX_U};
        float[] vs = {FRAME_MIN_V, FRAME_MIN_V + BORDER, FRAME_MAX_V - BORDER, FRAME_MAX_V};

        for (int col = 0; col < 3; col++) {
            for (int row = 0; row < 3; row++) {
                quad(buffer, matrix, xs[col], ys[row], xs[col + 1], ys[row + 1],
                        us[col] / TEXTURE, vs[row] / TEXTURE, us[col + 1] / TEXTURE, vs[row + 1] / TEXTURE);
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
