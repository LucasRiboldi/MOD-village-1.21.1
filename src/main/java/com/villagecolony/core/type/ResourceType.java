package com.villagecolony.core.type;

import java.util.Objects;

/**
 * Um recurso que a colônia sabe contar.
 *
 * <p>Três no MVP, conforme TASK-017. Não é a lista de tudo que existe no
 * Minecraft: é a lista do que a colônia acompanha. Um item fora dela
 * continua no baú, apenas não é contado.
 *
 * <p>Tipo próprio do Core: nenhuma classe daqui conhece {@code Item}. A
 * conversão a partir do item Vanilla mora em
 * {@code fabric.adapter.MinecraftTypeAdapter}. Ver ADR-005.
 *
 * <p>O modelo de Resource-System.md §"Modelo de Recurso" também prevê
 * {@code quantity} e {@code locations}. Eles não estão aqui de
 * propósito: isto é o tipo do recurso, que é fixo, e não o estoque, que
 * muda. A quantidade vive em {@link ResourceTally}.
 */
public enum ResourceType {

    OAK_LOG(ResourceCategory.NATURAL),

    OAK_PLANKS(ResourceCategory.PROCESSED),

    COBBLESTONE(ResourceCategory.NATURAL);

    private final ResourceCategory category;

    ResourceType(ResourceCategory category) {
        this.category = Objects.requireNonNull(category);
    }

    public ResourceCategory category() {
        return category;
    }
}
