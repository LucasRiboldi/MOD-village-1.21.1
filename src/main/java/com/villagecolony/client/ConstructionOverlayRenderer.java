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
            Text title = Text.literal(construction.blueprint() + " " + construction.placed()
                    + "/" + construction.total());
            String missing = construction.missingMaterial();
            Text status;
            int statusColor;
            if (!missing.isBlank()) {
                status = Text.translatable("overlay.construction.missing", missing);
                statusColor = 0xFFFFAA00;
            } else {
                status = Text.translatable("overlay.construction.state."
                        + construction.state().toLowerCase(java.util.Locale.ROOT));
                statusColor = 0xFF55FF55;
            }
            drawPanel(context, text, camera, position, construction.state(), title, status, statusColor);
        }
    }

    private static void drawPanel(WorldRenderContext context, TextRenderer text, Vec3d camera,
            Vec3d position, String state, Text title, Text status, int statusColor) {
        Vec3d relative = position.subtract(camera);
        PixelPanelLayout panel = PixelPanelLayout.twoLines(PixelPanelLayout.constructionTexture(state),
                text.getWidth(title), text.getWidth(status));
        context.matrixStack().push();
        context.matrixStack().translate(relative.x, relative.y, relative.z);
        context.matrixStack().multiply(context.camera().getRotation());
        context.matrixStack().scale(-SCALE, -SCALE, SCALE);
        WorldPixelPanelRenderer.draw(context, text, panel, title, 0xFF55FFFF, status, statusColor);
        context.matrixStack().pop();
    }
}
