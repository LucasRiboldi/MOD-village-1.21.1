package com.villagecolony.client;

import com.villagecolony.fabric.overlay.OverlaySnapshotPayload;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;

/** Ícone do estado, progresso e o primeiro material faltante sobre o lote. */
public final class ConstructionOverlayRenderer {

    private static final double MAX_DISTANCE = 64.0;
    private static final double MAX_DISTANCE_SQUARED = MAX_DISTANCE * MAX_DISTANCE;
    private static final int LABEL_ABOVE_ORIGIN = 7;

    private ConstructionOverlayRenderer() {
    }

    public static void register() {
        WorldRenderEvents.LAST.register(ConstructionOverlayRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null || client.options.hudHidden) {
            return;
        }

        Vec3d camera = context.camera().getPos();
        VertexConsumerProvider.Immediate buffers = OverlayDrawing.buffers();

        for (OverlaySnapshotPayload.ConstructionEntry construction : ClientOverlayState.constructions()) {
            Vec3d position = new Vec3d(construction.x() + 0.5,
                    construction.y() + LABEL_ABOVE_ORIGIN, construction.z() + 0.5);

            if (position.squaredDistanceTo(camera) > MAX_DISTANCE_SQUARED) {
                continue;
            }

            String missing = construction.missingMaterial();
            Text status = missing.isBlank()
                    ? Text.translatable("overlay.construction.state."
                            + construction.state().toLowerCase(Locale.ROOT))
                    : Text.translatable("overlay.construction.missing", missing);

            OverlayDrawing.label(context.camera(), buffers, position,
                    OverlayDrawing.id(OverlaySprites.construction(construction.state(), missing)),
                    new Text[] {
                        Text.literal(construction.blueprint() + " " + construction.placed() + "/" + construction.total()),
                        status
                    },
                    new int[] {0xFF55FFFF, missing.isBlank() ? 0xFF55FF55 : 0xFFFFAA00});
        }

        buffers.draw();
    }
}
