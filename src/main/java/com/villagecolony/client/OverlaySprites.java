package com.villagecolony.client;

import java.util.Locale;

/**
 * Qual arte em pixel cada overlay usa — os arquivos de
 * {@code assets/villagecolony/textures/gui/overlays/}.
 *
 * <p><b>Por que existe.</b> As quatorze texturas entraram em c40d4a1f
 * (2026-10-02) e nenhum código as desenhava: os dois renderizadores
 * escreviam só texto. O autor jogou em 2026-10-03 e não viu a arte. Aqui fica
 * só o nome do arquivo, sem nada do cliente, para o teste de unidade
 * conferir que cada nome aponta para um arquivo que existe.
 */
public final class OverlaySprites {

    public static final String NAMESPACE = "villagecolony";

    private static final String ROOT = "textures/gui/overlays/";

    private OverlaySprites() {
    }

    /**
     * O nome curto da planta para a placa: {@code
     * minecraft:village/plains/houses/plains_shepherds_house_1} vira {@code
     * plains shepherds house 1}.
     */
    public static String shortName(String blueprint) {
        String path = blueprint.substring(blueprint.indexOf(':') + 1);

        return path.substring(path.lastIndexOf('/') + 1).replace('_', ' ');
    }

    /** O ícone da profissão, pelo nome do enum que o servidor manda. */
    public static String profession(String profession) {
        return ROOT + "professions/" + profession.toLowerCase(Locale.ROOT) + ".png";
    }

    /**
     * O ícone da obra: esperando material quando falta peça ou o estado é de
     * espera; pronta quando completa; em obra no resto.
     */
    public static String construction(String state, String missingMaterial) {
        if (!missingMaterial.isBlank() || "WAITING_RESOURCES".equals(state)) {
            return ROOT + "construction/waiting_material.png";
        }

        return ROOT + ("COMPLETED".equals(state) ? "construction/completed.png" : "construction/building.png");
    }
}
