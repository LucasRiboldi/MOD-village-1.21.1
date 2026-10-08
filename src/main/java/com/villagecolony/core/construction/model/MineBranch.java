package com.villagecolony.core.construction.model;

/**
 * Em que pé está um ramal da mina — E2, decisão do autor de 2026-10-08: reserva
 * explícita de ramal, com dono, e o estado dito pelo nome.
 *
 * <p>O relatório dizia "1 of 4 taken" enquanto a escada ainda era um ramal só
 * ({@link Mine#branchesOpenNow}), e o segundo mineiro parecia esperar com três
 * ramais livres. O estado de cada ramal responde a pergunta sem conta de cabeça.
 */
public enum MineBranch {

    /** A escada e as salas ainda são um caminho só: este ramal abre quando a galeria começar. */
    NOT_OPEN_YET,

    /** Aberto e sem dono: o próximo mineiro sem ramal fica com ele. */
    OPEN,

    /** Aberto e com dono (MineClaims). */
    RESERVED,

    /** Acabou neste nível: a ponta chegou ao teto de raio ou bateu em recusas demais. */
    EXHAUSTED;

    /** O estado do ramal {@code index}, sabendo se alguém o reservou. */
    public static MineBranch of(Mine mine, int index, boolean reserved) {
        if (mine.arm(index).isDone()) {
            return EXHAUSTED;
        }

        if (index >= mine.branchesOpenNow()) {
            return NOT_OPEN_YET;
        }

        return reserved ? RESERVED : OPEN;
    }
}
