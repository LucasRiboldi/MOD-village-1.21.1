package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.Side;

/**
 * Um passo do eixo do caminho — Regra 123: a coluna, a altura do piso (o bloco em que se pisa), o
 * rumo em que se chegou e quanto o piso fica acima ({@code fill}) ou abaixo ({@code cut}) do chão natural
 * no eixo.
 */
public record PathStep(int x, int floorY, int z, Side heading, int fill, int cut) {

    /** A coluna de um lado do eixo, a {@code offset} blocos: −1 à esquerda, +1 à direita. */
    public int sideX(int offset) {
        return x + heading.clockwise().offsetX() * offset;
    }

    public int sideZ(int offset) {
        return z + heading.clockwise().offsetZ() * offset;
    }
}
