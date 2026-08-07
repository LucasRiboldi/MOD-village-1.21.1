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
    EXPANSION,

    /**
     * A vila deixou de ser viável: menos de 3 camas ou nenhum aldeão vivo.
     *
     * <p>A colônia não é apagada — isso destruiria o registro de Buildings
     * e violaria PROJECT_CONSTITUTION.md §10. Ver ADR-003 §6.
     *
     * <p><b>ADR-003 §7 chama este valor de DORMANT.</b> O nome foi trocado
     * porque {@link ColonyLifecycle} já usa DORMANT para outra coisa —
     * chunk descarregado (ADR-002). São condições diferentes:
     *
     * <ul>
     *   <li>ColonyLifecycle.DORMANT — ninguém está observando;
     *   <li>ColonyState.ABANDONED — não há mais vila para observar.
     * </ul>
     *
     * <p>Uma vila abandonada com jogador ao lado é ABANDONED e ACTIVE ao
     * mesmo tempo. Dois DORMANT com significados distintos no mesmo objeto
     * seriam uma armadilha.
     */
    ABANDONED
}
