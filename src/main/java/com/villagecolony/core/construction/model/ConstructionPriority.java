package com.villagecolony.core.construction.model;

/**
 * Motivo observável que decide a família da próxima construção.
 *
 * <p>A prioridade é recalculada a partir do mundo a cada planejamento; ela não
 * é estado de save. A escolha da planta concreta permanece na fronteira que
 * conhece os catálogos de estruturas.
 */
public enum ConstructionPriority {
    HOUSING_DEFICIT,
    FIRST_HOUSE,
    ROTATION_NON_RESIDENTIAL,
    ROTATION_HOUSE;

    /** Decide a prioridade sem depender de Minecraft, Fabric ou catálogo de plantas. */
    public static ConstructionPriority decide(
            boolean hasPreviousAttempt, boolean lastAttemptWasHouse, int adults, int beds) {
        if (adults < 0 || beds < 0) {
            throw new IllegalArgumentException("adults and beds must not be negative");
        }

        if (adults > beds) {
            return HOUSING_DEFICIT;
        }

        if (!hasPreviousAttempt) {
            return FIRST_HOUSE;
        }

        return lastAttemptWasHouse ? ROTATION_NON_RESIDENTIAL : ROTATION_HOUSE;
    }

    /** Se a prioridade atual exige uma planta de moradia. */
    public boolean requiresHouse() {
        return this != ROTATION_NON_RESIDENTIAL;
    }
}
