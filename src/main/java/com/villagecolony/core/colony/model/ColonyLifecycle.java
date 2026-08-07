package com.villagecolony.core.colony.model;

/**
 * Se a colônia está sendo simulada.
 *
 * <p>Definido em ADR-002. Este eixo depende apenas do carregamento de
 * chunk e é independente de {@link ColonyState}: uma colônia DORMANT
 * conserva o {@code ColonyState} em que parou e retoma nele.
 */
public enum ColonyLifecycle {

    /** Chunk carregado. O loop de simulação executa. */
    ACTIVE,

    /**
     * Chunk descarregado. Nenhuma task executa e nenhum bloco é colocado.
     *
     * <p>O estado permanece salvo e íntegro. Colônias DORMANT não contam
     * para o orçamento de tick.
     */
    DORMANT
}
