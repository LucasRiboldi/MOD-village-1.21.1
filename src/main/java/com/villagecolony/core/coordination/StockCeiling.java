package com.villagecolony.core.coordination;

import com.villagecolony.core.type.ResourceType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * O teto geral do estoque da vila — regra do autor, 2026-10-11: <i>"64 blocos a
 * partir de outros blocos (exemplo: degrau de madeira), blocos crus limite de 256
 * (exemplo: tora de madeira)"</i>.
 *
 * <p>No playtest da 0.3.26 a vila guardava 363 escadas, 330 lajes de pedra lisa e
 * 382 tábuas, e as metas pediam 751 toras. Nenhuma meta passa do teto, a não ser
 * a que a obra aberta ainda pede: a obra nunca fica sem a peça por causa dele.
 * Quem chega ao teto fica sem tarefa do ofício e vai ajudar a obra
 * ({@code BuildHelper}).
 */
public final class StockCeiling {

    /** Bloco fabricado de outros blocos (bancada ou fornalha). */
    public static final int CRAFTED = 64;

    /** Bloco cru: colhido, minerado, cortado, tosquiado. */
    public static final int RAW = 256;

    private StockCeiling() {
    }

    /** O teto deste recurso, sem obra. */
    public static int base(ResourceType type) {
        return switch (type.production()) {
            case CRAFTED_WOOD, CRAFTED_STONE, SMELTED -> CRAFTED;
            case HARVESTED, FARMED, MINED, SURFACE_GATHERED, SOIL_GATHERED, SHEARED -> RAW;
        };
    }

    /**
     * As metas com o teto: cada uma vai até {@link #base} ou até o que a obra ainda
     * pede, o que for maior. A ordem das metas é mantida.
     */
    public static Map<ResourceType, Integer> cap(
            Map<ResourceType, Integer> goals, Map<ResourceType, Integer> buildNeeds) {

        Objects.requireNonNull(goals, "goals");
        Objects.requireNonNull(buildNeeds, "buildNeeds");

        Map<ResourceType, Integer> capped = new LinkedHashMap<>();

        goals.forEach((type, goal) -> capped.put(type,
                Math.min(goal, Math.max(base(type), buildNeeds.getOrDefault(type, 0)))));

        return Collections.unmodifiableMap(capped);
    }
}
