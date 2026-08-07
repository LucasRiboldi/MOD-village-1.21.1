package com.villagecolony.core.colony.model;

/**
 * O que a colônia está fazendo.
 *
 * <p>Definido em Data-Model.md. Este eixo descreve a atividade da colônia
 * e é independente de {@link ColonyLifecycle}, que descreve se ela está
 * sendo simulada.
 */
public enum ColonyState {

    /** Sem demanda pendente. Mantém o que existe. */
    STABLE,

    /** Coletando e transformando recursos. */
    PRODUCTION,

    /** Construindo novas estruturas. */
    EXPANSION
}
