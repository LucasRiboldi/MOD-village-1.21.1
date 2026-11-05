package com.villagecolony.client;

import com.villagecolony.fabric.overlay.RoadMapPayload;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/** Desenha o sinalizador dos caminhos ({@code RoadMapSync}): linhas pouco acima do chão e caixas nos marcadores. */
public final class RoadMapRenderer {

    /** Quanto acima do bloco de caminho a linha passa. */
    private static final double LIFT = 1.2;

    private static volatile RoadMapPayload map = new RoadMapPayload(List.of(), List.of());

    private RoadMapRenderer() {
    }

    public static void register() {
        WorldRenderEvents.LAST.register(RoadMapRenderer::render);
    }

    public static void replace(RoadMapPayload payload) {
        map = payload;
    }

    private static void render(WorldRenderContext context) {
        RoadMapPayload current = map;

        if (current.lines().isEmpty() && current.markers().isEmpty()) {
            return;
        }

        Vec3d camera = context.camera().getPos();
        MatrixStack matrices = new MatrixStack();

        // Neste evento a rotação da câmera já está na matriz do jogo (como em OverlayDrawing): só falta a posição.
        matrices.translate(-camera.x, -camera.y, -camera.z);

        VertexConsumerProvider.Immediate buffers =
                MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer lines = buffers.getBuffer(RenderLayer.getLines());
        MatrixStack.Entry entry = matrices.peek();

        for (RoadMapPayload.Line line : current.lines()) {
            float red = ((line.color() >> 16) & 0xFF) / 255.0F;
            float green = ((line.color() >> 8) & 0xFF) / 255.0F;
            float blue = (line.color() & 0xFF) / 255.0F;

            for (int index = 1; index < line.points().size(); index++) {
                segment(lines, entry, BlockPos.fromLong(line.points().get(index - 1)),
                        BlockPos.fromLong(line.points().get(index)), red, green, blue);
            }
        }

        for (RoadMapPayload.Marker marker : current.markers()) {
            BlockPos at = BlockPos.fromLong(marker.position());
            float red = ((marker.color() >> 16) & 0xFF) / 255.0F;
            float green = ((marker.color() >> 8) & 0xFF) / 255.0F;
            float blue = (marker.color() & 0xFF) / 255.0F;

            WorldRenderer.drawBox(matrices, lines, new Box(at.up()).contract(0.3), red, green, blue, 1.0F);
        }

        buffers.draw(RenderLayer.getLines());
    }

    private static void segment(VertexConsumer lines, MatrixStack.Entry entry, BlockPos from, BlockPos to,
                                float red, float green, float blue) {
        float x1 = from.getX() + 0.5F;
        float y1 = from.getY() + (float) LIFT;
        float z1 = from.getZ() + 0.5F;
        float x2 = to.getX() + 0.5F;
        float y2 = to.getY() + (float) LIFT;
        float z2 = to.getZ() + 0.5F;
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float length = Math.max(0.001F, (float) Math.sqrt(dx * dx + dy * dy + dz * dz));

        lines.vertex(entry, x1, y1, z1).color(red, green, blue, 1.0F).normal(entry, dx / length, dy / length, dz / length);
        lines.vertex(entry, x2, y2, z2).color(red, green, blue, 1.0F).normal(entry, dx / length, dy / length, dz / length);
    }
}
