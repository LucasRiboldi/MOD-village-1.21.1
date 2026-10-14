package com.villagecolony.client;

import com.villagecolony.fabric.overlay.OverlaySnapshotPayload;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;

/** Ícone do estado, progresso e o primeiro material faltante, logo acima da placa da obra. */
public final class ConstructionOverlayRenderer {

    private static final double MAX_DISTANCE = 64.0;
    private static final double MAX_DISTANCE_SQUARED = MAX_DISTANCE * MAX_DISTANCE;
    /**
     * Do pé da placa até logo acima do nome dela: o nome do suporte de armadura
     * fica a 2,475 do pé, e a linha de texto ocupa uns 0,25.
     */
    private static final double ABOVE_SIGN = 2.8;

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
            // Logo acima da placa da obra, centrado nela.
            Vec3d position = new Vec3d(construction.x(), construction.y() + ABOVE_SIGN, construction.z());

            if (position.squaredDistanceTo(camera) > MAX_DISTANCE_SQUARED) {
                continue;
            }

            String missing = construction.missingMaterial();
            Text status = missing.isBlank()
                    ? Text.translatable("overlay.construction.state."
                            + construction.state().toLowerCase(Locale.ROOT))
                    : Text.translatable("overlay.construction.missing", missingText(missing));

            OverlayDrawing.label(context.camera(), buffers, position,
                    OverlayDrawing.id(OverlaySprites.construction(construction.state(), missing)),
                    new Text[] {
                        name(construction.blueprint())
                                .append("  " + construction.placed() + "/" + construction.total()),
                        status
                    },
                    new int[] {0xFF55FFFF, missing.isBlank() ? 0xFF55FF55 : 0xFFFFAA00});
        }

        buffers.draw();
    }

    /**
     * O nome da obra na língua do jogo: o tipo e o bioma pelas chaves
     * {@code overlay.building.*} e {@code overlay.biome.*}; o nome curto da planta
     * quando a tradução não conhece o tipo.
     */
    private static MutableText name(String blueprint) {
        OverlaySprites.BlueprintLabel label = OverlaySprites.label(blueprint);
        MutableText name = Text.translatableWithFallback("overlay.building." + label.kind(), label.fallback());

        if (!label.biome().isEmpty()) {
            name.append(" (").append(Text.translatable("overlay.biome." + label.biome())).append(")");
        }

        return name;
    }

    /** O item que falta pelo nome do jogo, com a quantidade: "Vidraça ×4". */
    private static Text missingText(String missing) {
        String[] parts = OverlaySprites.missingParts(missing);
        Identifier id = Identifier.tryParse(parts[0]);
        MutableText item = id != null && Registries.ITEM.containsId(id)
                ? Registries.ITEM.get(id).getName().copy()
                : Text.literal(parts[0]);

        return parts[1].isEmpty() ? item : item.append(" ×" + parts[1]);
    }
}
