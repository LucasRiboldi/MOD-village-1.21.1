package com.villagecolony.core.coordination;

import com.villagecolony.core.type.ResourceType;

import java.util.List;
import java.util.Map;

/**
 * O pedreiro não fica parado: sem pedido de obra, ele mantém um estoque das
 * peças da pedra da vila — ADR-036 item 8, como a tábua faz com o
 * carpinteiro.
 *
 * <p>Pedregulho (planície): laje de pedregulho e tijolo de pedra. A escada de
 * pedregulho fica de fora de propósito: ela não é recurso acompanhado, e
 * passa pelo pedido direto ao artesão ({@code CraftsmanRequest}). Arenito
 * (deserto): escada, laje e arenito lavrado.
 */
public final class MasonStock {

    /** O mínimo de cada peça no baú. */
    public static final int FLOOR = 16;

    private static final Map<ResourceType, List<ResourceType>> PIECES = Map.of(
            ResourceType.COBBLESTONE, List.of(ResourceType.COBBLESTONE_SLAB, ResourceType.STONE_BRICKS),
            ResourceType.SANDSTONE, List.of(
                    ResourceType.SANDSTONE_STAIRS, ResourceType.SANDSTONE_SLAB, ResourceType.CUT_SANDSTONE));

    private MasonStock() {
    }

    /** As peças que o pedreiro mantém para a pedra desta vila. */
    public static List<ResourceType> piecesFor(ResourceType villageStone) {
        return PIECES.getOrDefault(villageStone, List.of());
    }

    /** Põe o piso de cada peça nas metas, sem rebaixar uma meta maior que a obra já pôs. */
    public static void addTo(Map<ResourceType, Integer> goals, ResourceType villageStone) {
        for (ResourceType piece : piecesFor(villageStone)) {
            goals.merge(piece, FLOOR, Math::max);
        }
    }
}
