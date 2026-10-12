package com.villagecolony.client;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Qual símbolo cada overlay usa — os arquivos de
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

    /** Os biomas das vilas Vanilla, que abrem o nome das peças delas. */
    static final Set<String> BIOMES = Set.of("plains", "desert", "savanna", "snowy", "taiga");

    /** As grafias Vanilla do mesmo tipo de construção, levadas a uma chave só. */
    private static final Map<String, String> SAME_KIND = Map.ofEntries(
            Map.entry("shepherd_house", "shepherds_house"),
            Map.entry("shepherd", "shepherds_house"),
            Map.entry("butchers_shop", "butcher_shop"),
            Map.entry("armorer", "armorer_house"),
            Map.entry("fisher", "fisher_cottage"),
            Map.entry("fisherman_cottage", "fisher_cottage"),
            Map.entry("cartographer_house", "cartographer"),
            Map.entry("mason", "masons_house"),
            Map.entry("weapon_smith", "weaponsmith"),
            Map.entry("farm", "small_farm"),
            Map.entry("lamp_post", "lamp"));

    /**
     * O nome da obra para a placa, em partes traduzíveis — pedido do autor,
     * 2026-10-11: os nomes em português. {@code plains_medium_house_1} vira o tipo
     * {@code medium_house} e o bioma {@code plains}; o renderizador busca
     * {@code overlay.building.<tipo>} e {@code overlay.biome.<bioma>} na língua do
     * jogo, e o nome curto fica de reserva para o tipo que a tradução não conhece.
     */
    public static BlueprintLabel label(String blueprint) {
        String name = shortName(blueprint).replace(' ', '_');
        String biome = "";
        int underscore = name.indexOf('_');

        if (underscore > 0 && BIOMES.contains(name.substring(0, underscore))) {
            biome = name.substring(0, underscore);
            name = name.substring(underscore + 1);
        }

        String kind = name.replaceAll("_\\d+$", "");

        return new BlueprintLabel(SAME_KIND.getOrDefault(kind, kind), biome, shortName(blueprint));
    }

    /** O tipo de construção, o bioma (vazio fora das vilas Vanilla) e o nome curto de reserva. */
    public record BlueprintLabel(String kind, String biome, String fallback) {
    }

    /**
     * O material que falta, como o servidor manda ({@code "minecraft:glass_pane 4"}):
     * o identificador do item e a quantidade, que vem vazia quando não há número.
     */
    public static String[] missingParts(String missing) {
        String trimmed = missing.trim();
        int space = trimmed.lastIndexOf(' ');

        if (space > 0 && trimmed.substring(space + 1).chars().allMatch(Character::isDigit)) {
            return new String[] {trimmed.substring(0, space), trimmed.substring(space + 1)};
        }

        return new String[] {trimmed, ""};
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
