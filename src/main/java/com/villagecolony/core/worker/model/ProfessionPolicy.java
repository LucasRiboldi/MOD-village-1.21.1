package com.villagecolony.core.worker.model;

/** Política editável de uma profissão, pertencente a um mundo de jogo. */
public record ProfessionPolicy(boolean enabled, int maximumWorkers, int searchRadius) {

    /** Sem teto extra: preserva o cálculo histórico por população. */
    public static final int UNLIMITED = 0;

    /** Raio decidido pelo comportamento original da profissão. */
    public static final int AUTOMATIC_RADIUS = -1;

    public ProfessionPolicy {
        if (maximumWorkers < UNLIMITED) {
            throw new IllegalArgumentException("maximumWorkers must not be negative");
        }
        if (searchRadius != AUTOMATIC_RADIUS && (searchRadius < 16 || searchRadius > 128)) {
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
}
