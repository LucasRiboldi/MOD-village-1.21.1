package com.villagecolony.core.worker.model;

/**
 * Ferramenta inicial de uma profissão.
 *
 * <p>Tipo próprio do Core: nenhuma classe daqui conhece {@code Item} ou
 * {@code Items.WOODEN_AXE}. A conversão para o item Vanilla acontece na
 * fronteira, em {@code fabric.adapter.MinecraftTypeAdapter}. Ver ADR-005.
 *
 * <p>A evolução da ferramenta (madeira → pedra → ferro → diamante) não
 * pertence ao MVP. Ver Profession-System.md §"Evolução das Ferramentas".
 */
public enum ToolType {

    /** A profissão trabalha de mãos vazias. */
    NONE,

    WOODEN_AXE,

    /** Do mineiro. Pedra pede picareta, e a de madeira é a inicial. */
    WOODEN_PICKAXE,

    /** Do pastor. Tesoura não tem grau, então é ela mesma. */
    SHEARS,

    WOODEN_HOE
}
