package com.villagecolony.client;

import com.villagecolony.fabric.overlay.OverlaySnapshotPayload;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

/** Desenha o progresso e o primeiro material faltante no espaço do lote. */
public final class ConstructionOverlayRenderer {

    private static final double MAX_DISTANCE = 64.0;
    private static final double MAX_DISTANCE_SQUARED = MAX_DISTANCE * MAX_DISTANCE;
    private static final float SCALE = 0.025F;
    private static final int LABEL_ABOVE_ORIGIN = 7;

    private ConstructionOverlayRenderer() {
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(ConstructionOverlayRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || context.matrixStack() == null || context.consumers() == null) {
            return;
        }
        Vec3d camera = context.camera().getPos();
        TextRenderer text = client.textRenderer;
        for (OverlaySnapshotPayload.ConstructionEntry construction : ClientOverlayState.constructions()) {
            Vec3d position = new Vec3d(construction.x() + 0.5,
                    construction.y() + LABEL_ABOVE_ORIGIN, construction.z() + 0.5);
            if (position.squaredDistanceTo(camera) > MAX_DISTANCE_SQUARED) {
                continue;
            }
            drawLine(context, text, camera, position,
                    Text.literal(construction.blueprint() + " " + construction.placed()
                            + "/" + construction.total()), 0xFF55FFFF, 0.0F);
            String missing = construction.missingMaterial();
            if (!missing.isBlank()) {
                drawLine(context, text, camera, position,
                        Text.translatable("overlay.construction.missing", missing), 0xFFFFAA00, 0.38F);
            } else {
                drawLine(context, text, camera, position,
                        Text.translatable("overlay.construction.state."
                                + construction.state().toLowerCase(java.util.Locale.ROOT)),
                        0xFF55FF55, 0.38F);
            }
        }
    }

    private static void drawLine(WorldRenderContext context, TextRenderer text, Vec3d camera,
            Vec3d position, Text line, int color, float yOffset) {
        Vec3d relative = position.subtract(camera);
        float width = text.getWidth(line);
        context.matrixStack().push();
        context.matrixStack().translate(relative.x, relative.y, relative.z);
        context.matrixStack().multiply(context.camera().getRotation());
        context.matrixStack().scale(-SCALE, -SCALE, SCALE);
        text.draw(line, -width / 2.0F, yOffset / SCALE, color, true,
                context.matrixStack().peek().getPositionMatrix(), context.consumers(),
                TextRenderer.TextLayerType.SEE_THROUGH, 0, 0xF000F0);
        context.matrixStack().pop();
    }
}
