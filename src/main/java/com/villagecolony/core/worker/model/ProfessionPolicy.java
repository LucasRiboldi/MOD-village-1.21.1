package com.villagecolony.core.worker.model;

/** Política editável de uma profissão, pertencente a um mundo de jogo. */
public record ProfessionPolicy(boolean enabled, int maximumWorkers, int searchRadius) {

    /** Sem teto extra: preserva o cálculo histórico por população. */
    public static final int UNLIMITED = 0;

    /** Teto defensivo para evitar configuração acidentalmente inviável. */
    public static final int MAXIMUM_WORKERS_LIMIT = 512;

    public static final int MINIMUM_SEARCH_RADIUS = 16;

    public static final int MAXIMUM_SEARCH_RADIUS = 128;

    /** Raio decidido pelo comportamento original da profissão. */
    public static final int AUTOMATIC_RADIUS = -1;

    public ProfessionPolicy {
        if (maximumWorkers < UNLIMITED || maximumWorkers > MAXIMUM_WORKERS_LIMIT) {
            throw new IllegalArgumentException("maximumWorkers must be between zero and "
                    + MAXIMUM_WORKERS_LIMIT);
        }
        if (searchRadius != AUTOMATIC_RADIUS && (searchRadius < MINIMUM_SEARCH_RADIUS
                || searchRadius > MAXIMUM_SEARCH_RADIUS)) {
            throw new IllegalArgumentException("searchRadius must be automatic or between 16 and 128");
        }
    }

    public static ProfessionPolicy defaults() {
        return new ProfessionPolicy(true, UNLIMITED, AUTOMATIC_RADIUS);
    }

    public boolean limitsWorkers() {
        return maximumWorkers != UNLIMITED;
    }

    public boolean hasConfiguredRadius() {
        return searchRadius != AUTOMATIC_RADIUS;
    }

    /**
     * O raio que a profissão usa: o do jogador, ou {@code automatic} quando
     * ele não configurou (ADR-030). Decisão única para lenhador, fazendeiro e
     * pastor — ADR-035 §4.
     */
    public int searchRadiusOr(int automatic) {
        return hasConfiguredRadius() ? searchRadius : automatic;
    }
}
