package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.BlueprintKind;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.CropBlock;

import java.util.ArrayList;
import java.util.List;

/**
 * A planta da roça como a colônia a constrói: sem a lavoura e sem a camada da
 * rua.
 *
 * <p>Uma só forma para quem planeja e para quem relê a obra do save ou do
 * registro de construções. Com duas, a obra retomada pedia trigo ao
 * carpinteiro e media a fundação na camada da rua, que a roça planejada não
 * tem — a trava de reserva recusava para sempre (playtest de 2026-10-07).
 */
public final class FarmBlueprint {

    private FarmBlueprint() {
    }

    /** A planta como será construída: a roça perde a lavoura; o resto não muda. */
    public static Blueprint asBuilt(Blueprint blueprint) {
        return BlueprintKind.isFarm(blueprint.id()) ? withoutTheCrops(blueprint) : blueprint;
    }

    /**
     * A mesma roça, sem a lavoura plantada.
     *
     * <p>A obra faz o canteiro e o fazendeiro planta: cobrar trigo, cenoura e
     * batata do baú pararia a roça esperando semente.
     *
     * <p>Sem a camada da rua: a base da roça fica um acima do chão, que é a
     * altura que o canal de água pede (pedido do autor, 2026-10-02).
     */
    public static Blueprint withoutTheCrops(Blueprint farm) {
        List<BlueprintBlock> kept = new ArrayList<>();

        for (BlueprintBlock block : farm.blocks()) {
            if (!isCrop(block.block())) {
                kept.add(block);
            }
        }

        return Blueprint.of(farm.id(), kept);
    }

    /** Se este bloco é lavoura — pergunta ao próprio bloco ({@link CropBlock}). */
    private static boolean isCrop(ResourceId block) {
        return MinecraftTypeAdapter.toBlock(block)
                .map(found -> found instanceof CropBlock)
                .orElse(false);
    }
}
