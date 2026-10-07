package com.villagecolony.core.coordination;

import com.villagecolony.core.type.ResourceType;

import java.util.List;
import java.util.Map;

/**
 * As peças da pedra da vila que o pedreiro adianta sem pedido de obra — ADR-036
 * item 8; cinco de cada por vez, em ciclo ({@link AdvanceStock}, ADR-038 P5b).
 *
 * <p>Pedregulho (planície): laje de pedregulho e tijolo de pedra. A escada de
 * pedregulho fica de fora de propósito: ela não é recurso acompanhado, e
 * passa pelo pedido direto ao artesão ({@code CraftsmanRequest}). Arenito
 * (deserto): escada, laje e arenito lavrado.
 */
public final class MasonStock {

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
}
