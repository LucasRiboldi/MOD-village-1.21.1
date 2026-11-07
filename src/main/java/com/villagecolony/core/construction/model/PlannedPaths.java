package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * O caminho que o grafo já traçou e ainda falta calçar — Regra 214. Uma releitura pode deixar de pedir um trecho e a seguinte
 * voltar a pedi-lo: ele não é esquecido por isso. Sai quando foi calçado ou recusado, ou depois de {@value #KEEP_READS} releituras
 * sem ser pedido.
 */
public final class PlannedPaths {

    /** Quantas releituras um trecho fica sem ser pedido: 30 minutos, no ritmo de quem tem calçada pendente. */
    public static final int KEEP_READS = 60;

    private final Map<ColonyPos, Integer> lastWanted = new HashMap<>();

    /**
     * O resultado de uma releitura.
     *
     * @param read a vez desta releitura
     * @param wanted o que ela pediu para calçar
     * @param finished se a coluna já foi calçada ou recusada
     */
    public void read(int read, Collection<ColonyPos> wanted, Predicate<ColonyPos> finished) {
        wanted.forEach(at -> lastWanted.put(at, read));
        lastWanted.entrySet().removeIf(entry -> finished.test(entry.getKey()) || read - entry.getValue() > KEEP_READS);
    }

    public Set<ColonyPos> cells() {
        return new HashSet<>(lastWanted.keySet());
    }
}
