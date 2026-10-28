package com.villagecolony.client;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;

/** Ícone e título da profissão sobre o aldeão, a partir do snapshot do cliente. */
public final class ProfessionOverlayRenderer {

    private static final double MAX_DISTANCE = 32.0;
    private static final double MAX_DISTANCE_SQUARED = MAX_DISTANCE * MAX_DISTANCE;

    private ProfessionOverlayRenderer() {
    }

    public static void register() {
        WorldRenderEvents.LAST.register(ProfessionOverlayRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.world == null || client.player == null || client.options.hudHidden) {
            return;
        }

        Vec3d camera = context.camera().getPos();
        Box search = new Box(camera, camera).expand(MAX_DISTANCE);
        float tickDelta = context.tickCounter().getTickDelta(true);
        VertexConsumerProvider.Immediate buffers = OverlayDrawing.buffers();

        for (VillagerEntity villager : client.world.getEntitiesByClass(VillagerEntity.class, search,
                candidate -> ClientOverlayState.professionOf(candidate.getUuid()) != null)) {
            if (villager.squaredDistanceTo(camera) > MAX_DISTANCE_SQUARED) {
                continue;
            }

            String profession = ClientOverlayState.professionOf(villager.getUuid());
            Vec3d above = villager.getLerpedPos(tickDelta).add(0.0, villager.getHeight() + 0.55, 0.0);

            OverlayDrawing.label(context.camera(), buffers, above,
                    OverlayDrawing.id(OverlaySprites.profession(profession)),
                    new Text[] {Text.translatable("overlay.profession." + profession.toLowerCase(Locale.ROOT))},
                    new int[] {colorOf(ClientOverlayState.badgeOf(villager.getUuid()))});
        }

        buffers.draw();
    }

    /**
     * A cor do nome pelo estado (autor, 2026-10-27): laranja no ofício, azul na ajuda, vermelho travado,
     * branco à toa no expediente, cinza fora dele.
     */
    static int colorOf(String badge) {
        return switch (badge) {
            case "DUTY" -> 0xFFFFA020;
            case "HELP" -> 0xFF55AAFF;
            case "STUCK" -> 0xFFFF4040;
            case "OFF" -> 0xFFA0A0A0;
            default -> 0xFFFFFFFF;
        };
    }
}
