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
    /**
     * Falta uma plantação para a população — Regra 67, 2026-10-13: ao passar de
     * {@link #ADULTS_PER_FARM} adultos (o 16º), a obra de fazenda fura a fila e vem antes
     * das oficinas e do rodízio (só a falta de cama vem antes).
     */
    FARM_DEFICIT,
    /**
     * A casa de um ofício que ainda não tem a sua — Regra 49, 2026-10-02: sem
     * falta de cama, cada ofício ganha a sua casa antes de qualquer outra obra.
     */
    WORKSHOP,
    FIRST_HOUSE,
    ROTATION_NON_RESIDENTIAL,
    ROTATION_HOUSE;

    /** Adultos por plantação — Regra 67: uma a cada 15, e o 16º a torna prioridade. */
    public static final int ADULTS_PER_FARM = 15;

    /** Decide a prioridade sem depender de Minecraft, Fabric ou catálogo de plantas. */
    public static ConstructionPriority decide(
            boolean hasPreviousAttempt, boolean lastAttemptWasHouse, int adults, int beds) {
        return decide(hasPreviousAttempt, lastAttemptWasHouse, adults, beds, false, false);
    }

    /** Compatível: sem o sinal de falta de fazenda. */
    public static ConstructionPriority decide(
            boolean hasPreviousAttempt, boolean lastAttemptWasHouse, int adults, int beds,
            boolean workshopMissing) {
        return decide(hasPreviousAttempt, lastAttemptWasHouse, adults, beds, workshopMissing, false);
    }

    /**
     * O mesmo, sabendo se falta a casa de algum ofício (Regra 49) e se falta plantação para
     * a população (Regra 67, 16º adulto). A falta de cama vem antes de tudo; a de fazenda,
     * logo depois, antes das oficinas e do rodízio.
     */
    public static ConstructionPriority decide(
            boolean hasPreviousAttempt, boolean lastAttemptWasHouse, int adults, int beds,
            boolean workshopMissing, boolean farmDeficit) {
        if (adults < 0 || beds < 0) {
            throw new IllegalArgumentException("adults and beds must not be negative");
        }

        if (adults > beds) {
            return HOUSING_DEFICIT;
        }

        if (farmDeficit) {
            return FARM_DEFICIT;
        }

        if (workshopMissing) {
            return WORKSHOP;
        }

        if (!hasPreviousAttempt) {
            return FIRST_HOUSE;
        }

        return lastAttemptWasHouse ? ROTATION_NON_RESIDENTIAL : ROTATION_HOUSE;
    }

    /**
     * Quantas plantações a vila deve ter para esta população: uma a cada
     * {@link #ADULTS_PER_FARM} adultos, no mínimo uma desde a fundação — Regra 67.
     */
    public static int farmsFor(int adults) {
        return Math.max(1, (adults + ADULTS_PER_FARM - 1) / ADULTS_PER_FARM);
    }

    /** Regra 163: a maior das contas — uma a cada 15 adultos (Regra 67) ou uma por fazendeiro. */
    public static int farmsFor(int adults, int farmers) {
        return Math.max(farmsFor(adults), farmers);
    }

    /** Se falta plantação para a população e isso já virou prioridade (passou dos 15, o 16º). */
    public static boolean farmIsDue(int adults, int farms) {
        return farmIsDue(adults, 0, farms);
    }

    /**
     * Se falta plantação e isso já virou prioridade: o 16º adulto (Regra 67) ou um fazendeiro
     * sem roça (Regra 163).
     */
    public static boolean farmIsDue(int adults, int farmers, int farms) {
        return farms < farmsFor(adults, farmers) && (adults > ADULTS_PER_FARM || farmers > farms);
    }

    /**
     * Regra 164: sem outro tipo com lote, a casa só sai se for obrigatória ou se ainda cabe
     * cama no teto de {@link #roomForAHouse}.
     */
    public static boolean fallsBackToAHouse(boolean obligatoryHouse, int adults, int beds) {
        return obligatoryHouse || roomForAHouse(adults, beds);
    }

    /** Se a prioridade atual exige uma planta de moradia. */
    public boolean requiresHouse() {
        return this != ROTATION_NON_RESIDENTIAL && this != WORKSHOP && this != FARM_DEFICIT;
    }

    /**
     * Quantas camas a mais que adultos a vila pode ter antes de parar de levantar casa no
     * rodízio — opção 1A do autor, 2026-10-13: o rodízio casa ↔ outro tipo volta, com teto.
     */
    public static final int HOUSE_SURPLUS = 4;

    /** Se ainda cabe casa no rodízio: camas abaixo de adultos + {@link #HOUSE_SURPLUS}. */
    public static boolean roomForAHouse(int adults, int beds) {
        return beds < adults + HOUSE_SURPLUS;
    }

    /** Se esta vez levanta casa: falta de cama, a primeira, ou a vez de casa do rodízio com teto. */
    public boolean buildsHouse(int adults, int beds) {
        return buildsHouse() || (this == ROTATION_HOUSE && roomForAHouse(adults, beds));
    }

    /** Se esta vez levanta casa por obrigação: falta de cama ou a primeira casa da vila. */
    public boolean buildsHouse() {
        return this == HOUSING_DEFICIT || this == FIRST_HOUSE;
    }
}
