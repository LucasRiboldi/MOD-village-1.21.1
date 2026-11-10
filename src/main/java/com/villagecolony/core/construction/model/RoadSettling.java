package com.villagecolony.core.construction.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * O trecho que ficou — Regra 225 (autor, 2026-11-10, ADR-050): coluna de linha que passa {@value #DAYS} dias de jogo sem sair
 * do grafo fica fixa. A releitura não a move, e o resto do traçado se ajeita em volta. Sai da linha, ou some o caminho
 * dela no mundo, e a conta recomeça. Puro: a coluna é {@link ColonyRoads#column}, e o dia, o do relógio do mundo.
 */
public final class RoadSettling {

    /** Quantos dias de jogo sem mudar fazem o trecho fixo: longo de propósito, para ser a versão final (autor). */
    public static final int DAYS = 10;

    private RoadSettling() {
    }

    /** As colunas fixas: na linha desde {@value #DAYS} dias ou mais. */
    public static Set<Long> fixed(Map<Long, Long> since, long today) {
        Set<Long> fixed = new HashSet<>();

        since.forEach((column, day) -> {
            if (today - day >= DAYS) {
                fixed.add(column);
            }
        });

        return fixed;
    }

    /**
     * O dia de entrada de cada coluna, depois desta releitura: a que segue na linha guarda o dia, a nova começa hoje e a que
     * saiu é esquecida.
     */
    public static Map<Long, Long> track(Map<Long, Long> since, Set<Long> line, long today) {
        Map<Long, Long> next = new HashMap<>();

        for (long column : line) {
            next.put(column, since.getOrDefault(column, today));
        }

        return next;
    }
}
