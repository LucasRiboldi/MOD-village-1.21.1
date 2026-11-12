package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;

import java.util.List;
import java.util.Optional;

/**
 * A planta do poste de luz — Regra 261 (autor, 2026-11-12): base de 1 bloco de pedra do bioma, duas cercas da madeira do
 * bioma e a tocha da colônia no topo. A base fica na linha do solo, que é a camada da rua.
 *
 * <p>Os dois materiais vão no nome da planta ({@code villagecolony:colony/lamp_post/<pedra>/<cerca>}): a obra que volta
 * do save e o reparo releem a planta pelo nome, e não há arquivo de estrutura por bioma. O nome traz "lamp", e o poste é
 * enfeite, sem entrada no grafo (Regras 223 e 252).
 */
public final class LampPostPlan {

    /** O começo do nome de toda planta de poste. */
    public static final String PREFIX = "colony/lamp_post/";

    /** O espaço de nomes do mod. */
    static final String NAMESPACE = "villagecolony";

    /** A tocha da colônia, no topo (Regra 127). */
    public static final ResourceId COLONY_TORCH = new ResourceId(NAMESPACE, "colony_torch");

    private LampPostPlan() {
    }

    /** O nome da planta do poste com esta pedra e esta cerca, as duas do jogo. */
    public static ResourceId idOf(ResourceId stone, ResourceId fence) {
        if (!ResourceId.VANILLA.equals(stone.namespace()) || !ResourceId.VANILLA.equals(fence.namespace())) {
            throw new IllegalArgumentException("lamp post materials must be vanilla: " + stone + ", " + fence);
        }

        return new ResourceId(NAMESPACE, PREFIX + stone.path() + "/" + fence.path());
    }

    /** Se este nome é de uma planta de poste. */
    public static boolean isLampPost(ResourceId id) {
        return NAMESPACE.equals(id.namespace()) && id.path().startsWith(PREFIX);
    }

    /** A planta pelo nome; vazio se o nome não for de poste ou não trouxer os dois materiais. */
    public static Optional<Blueprint> blueprintOf(ResourceId id) {
        if (!isLampPost(id)) {
            return Optional.empty();
        }

        String materials = id.path().substring(PREFIX.length());
        int slash = materials.indexOf('/');

        if (slash <= 0 || slash == materials.length() - 1 || materials.indexOf('/', slash + 1) >= 0) {
            return Optional.empty();
        }

        ResourceId stone = ResourceId.vanilla(materials.substring(0, slash));
        ResourceId fence = ResourceId.vanilla(materials.substring(slash + 1));

        return Optional.of(Blueprint.of(id, List.of(
                new BlueprintBlock(new ColonyPos(0, 0, 0), stone),
                new BlueprintBlock(new ColonyPos(0, 1, 0), fence),
                new BlueprintBlock(new ColonyPos(0, 2, 0), fence),
                new BlueprintBlock(new ColonyPos(0, 3, 0), COLONY_TORCH))).withStreetLayer(0));
    }

    /** A planta com esta pedra e esta cerca. */
    public static Blueprint blueprint(ResourceId stone, ResourceId fence) {
        return blueprintOf(idOf(stone, fence)).orElseThrow();
    }

    /** A cerca da madeira destas tábuas: {@code oak_planks} dá {@code oak_fence}. */
    public static ResourceId fenceOf(ResourceId planks) {
        String path = planks.path();
        String species = path.endsWith("_planks") ? path.substring(0, path.length() - "_planks".length()) : "oak";

        return ResourceId.vanilla(species + "_fence");
    }
}
