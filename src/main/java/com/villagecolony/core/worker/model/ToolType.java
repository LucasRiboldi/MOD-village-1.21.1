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

    /**
     * Sobrou de quando o mineiro começava de madeira.
     *
     * <p>Fica no catálogo porque o enum é a linguagem das ferramentas, e
     * não a lista do que está em uso: uma profissão futura que comece
     * pobre volta a pedi-la.
     */
    WOODEN_PICKAXE,

    /**
     * Do mineiro, e é decisão do autor — ver {@code MinerWork.TOOL}.
     *
     * <p>São vinte blocos de descida antes de a mina render alguma coisa,
     * e com picareta de madeira isso é uma sessão inteira. O tempo de
     * quebra já era calculado com diamante desde 2026-08-20; o catálogo
     * só foi alcançá-lo em 08-27.
     */
    DIAMOND_PICKAXE,

    /** Do pastor. Tesoura não tem grau, então é ela mesma. */
    SHEARS,

    WOODEN_HOE
}
