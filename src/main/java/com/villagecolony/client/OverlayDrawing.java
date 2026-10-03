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
 * Um rótulo virado para a câmera: ícone em pixel art e linhas de texto.
 *
 * <p><b>Desenhado no {@code WorldRenderEvents.LAST}, com buffer próprio</b> —
 * 2026-10-03. O {@code AFTER_ENTITIES} usava os {@code consumers} do jogo e
 * saía calado quando vinham nulos; o cliente do autor roda um pacote de
 * shaders do Iris, que troca esse pipeline. No {@code LAST} a Fabric API
 * garante a matriz de visão da câmera e manda desenhar direto no framebuffer,
 * que é o que um buffer imediato próprio faz.
 */
final class OverlayDrawing {

    /** Tamanho de uma unidade de texto no mundo: 40 unidades por bloco. */
    static final float SCALE = 0.025F;

    /** Lado do ícone, em unidades de texto: meio bloco. */
    private static final float ICON = 20.0F;

    private static final int FULL_BRIGHT = 0xF000F0;

    private OverlayDrawing() {
    }

    /** O buffer imediato do cliente; quem desenha chama {@code draw()} no fim. */
    static VertexConsumerProvider.Immediate buffers() {
        return MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
    }

    /**
     * Ícone acima de {@code at} e as linhas abaixo dele.
     *
     * @param lines texto e cor (ARGB) de cada linha, de cima para baixo
     */
    static void label(Camera camera, VertexConsumerProvider buffers, Vec3d at, Identifier icon,
            Text[] lines, int[] colors) {

        TextRenderer text = MinecraftClient.getInstance().textRenderer;
        Vec3d relative = at.subtract(camera.getPos());
        MatrixStack matrices = new MatrixStack();

        matrices.translate(relative.x, relative.y, relative.z);
        matrices.multiply(camera.getRotation());
        matrices.scale(-SCALE, -SCALE, SCALE);

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        quad(buffers.getBuffer(RenderLayer.getTextSeeThrough(icon)), matrix,
                -ICON / 2, -ICON - 2, ICON / 2, -2);

        for (int i = 0; i < lines.length; i++) {
            float width = text.getWidth(lines[i]);

            text.draw(lines[i], -width / 2.0F, i * (text.fontHeight + 1), colors[i], true, matrix, buffers,
                    TextRenderer.TextLayerType.SEE_THROUGH, 0, FULL_BRIGHT);
        }
    }

    /** Um quadrado texturizado, nas duas faces: a escala negativa vira a frente. */
    private static void quad(VertexConsumer buffer, Matrix4f matrix, float x0, float y0, float x1, float y1) {
        vertex(buffer, matrix, x0, y1, 0, 1);
        vertex(buffer, matrix, x1, y1, 1, 1);
        vertex(buffer, matrix, x1, y0, 1, 0);
        vertex(buffer, matrix, x0, y0, 0, 0);

        vertex(buffer, matrix, x0, y0, 0, 0);
        vertex(buffer, matrix, x1, y0, 1, 0);
        vertex(buffer, matrix, x1, y1, 1, 1);
        vertex(buffer, matrix, x0, y1, 0, 1);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f matrix, float x, float y, float u, float v) {
        buffer.vertex(matrix, x, y, 0.0F).color(0xFFFFFFFF).texture(u, v).light(FULL_BRIGHT);
    }

    static Identifier id(String path) {
        return Identifier.of(OverlaySprites.NAMESPACE, path);
    }
}
