package com.villagecolony.client;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;

/** Desenha títulos de profissão orientados à câmera a partir do snapshot cliente. */
public final class ProfessionOverlayRenderer {

    private static final double MAX_DISTANCE = 32.0;
    private static final double MAX_DISTANCE_SQUARED = MAX_DISTANCE * MAX_DISTANCE;
    private static final float SCALE = 0.025F;

    private ProfessionOverlayRenderer() {
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(ProfessionOverlayRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null || context.matrixStack() == null
                || context.consumers() == null) {
            return;
        }
        Vec3d camera = context.camera().getPos();
        Box search = new Box(camera, camera).expand(MAX_DISTANCE);
        TextRenderer text = client.textRenderer;

        for (VillagerEntity villager : client.world.getEntitiesByClass(VillagerEntity.class, search,
                candidate -> ClientOverlayState.professionOf(candidate.getUuid()) != null)) {
            if (villager.squaredDistanceTo(camera) > MAX_DISTANCE_SQUARED) {
                continue;
            }
            String profession = ClientOverlayState.professionOf(villager.getUuid());
            Text label = Text.translatable("overlay.profession."
                    + profession.toLowerCase(Locale.ROOT));
            boolean showText = OverlayPreferences.professionTextVisible();
            PixelPanelLayout panel = PixelPanelLayout.singleLine(
                    PixelPanelLayout.professionTexture(profession), text.getWidth(label), showText);
            Vec3d position = villager.getLerpedPos(client.getRenderTickCounter().getTickDelta(true))
                    .add(0.0, villager.getHeight() + 0.55, 0.0)
                    .subtract(camera);
            context.matrixStack().push();
            context.matrixStack().translate(position.x, position.y, position.z);
            context.matrixStack().multiply(context.camera().getRotation());
            context.matrixStack().scale(-SCALE, -SCALE, SCALE);
            WorldPixelPanelRenderer.draw(context, text, panel, label, 0xFFFFFFFF, null, 0);
            context.matrixStack().pop();
        }
    }
}
