package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * As tentativas de resgate das peças de lado de uma obra que não tem mais nada a pôr — Regra 113.
 *
 * <p>A cada ciclo da colônia, cada peça que segue de lado volta à fila ({@link Verdict#RETRY}); passadas
 * {@link #TRIES} tentativas ela surge no lugar ({@link Verdict#SURGE}), qualquer que seja o motivo de ter
 * ficado de lado (material, apoio ou alcance).
 */
public final class PieceRescue {

    /** Quantas vezes a peça volta à fila antes de surgir. */
    public static final int TRIES = 4;

    /** O que fazer com a peça de lado neste ciclo. */
    public enum Verdict { RETRY, SURGE }

    private final Map<ColonyPos, Integer> tries = new HashMap<>();

    /** Conta mais uma vez de lado para esta posição e diz o que fazer com ela. */
    public Verdict next(ColonyPos position) {
        int seen = tries.merge(Objects.requireNonNull(position, "position"), 1, Integer::sum);

        return seen > TRIES ? Verdict.SURGE : Verdict.RETRY;
    }

    /** Quantas vezes esta posição já voltou à fila. */
    public int triesAt(ColonyPos position) {
        return Math.min(tries.getOrDefault(position, 0), TRIES);
    }
}
