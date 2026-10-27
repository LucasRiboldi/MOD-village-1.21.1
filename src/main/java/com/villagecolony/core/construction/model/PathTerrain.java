package com.villagecolony.core.construction.model;

import java.util.OptionalInt;

/**
 * O terreno como o traçado de caminho o enxerga — Regra 123. Puro: quem lê o mundo é o lado Fabric.
 */
public interface PathTerrain {

    /**
     * A altura do chão natural da coluna — o y do bloco sólido de cima, em que se pisa — perto de
     * {@code nearY}. Vazio quando a coluna não tem chão que sirva: água, lava, ou fora do que se pode ler.
     */
    OptionalInt groundY(int x, int z, int nearY);

    /** A coluna não pode receber caminho: obra, curral, lote reservado, construção. */
    boolean blocked(int x, int z);
}
