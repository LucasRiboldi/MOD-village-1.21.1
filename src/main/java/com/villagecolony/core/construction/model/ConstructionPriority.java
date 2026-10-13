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
     * A casa de um ofício que ainda não tem a sua — Regra 49, 2026-10-02: sem
     * falta de cama, cada ofício ganha a sua casa antes de qualquer outra obra.
     */
    WORKSHOP,
    FIRST_HOUSE,
    ROTATION_NON_RESIDENTIAL,
    ROTATION_HOUSE;

    /** Decide a prioridade sem depender de Minecraft, Fabric ou catálogo de plantas. */
    public static ConstructionPriority decide(
            boolean hasPreviousAttempt, boolean lastAttemptWasHouse, int adults, int beds) {
        return decide(hasPreviousAttempt, lastAttemptWasHouse, adults, beds, false);
    }

    /**
     * O mesmo, sabendo se falta a casa de algum ofício — Regra 49, pedido do
     * autor de 2026-10-02: <i>"intercalado com a necessidade de camas: daí
     * entra casa de moradia e depois volta à construção de cada ofício; só
     * depois da estrutura de cada ofício as restantes entram na lista,
     * sempre priorizando casa se a vila precisar de cama"</i>.
     */
    public static ConstructionPriority decide(
            boolean hasPreviousAttempt, boolean lastAttemptWasHouse, int adults, int beds,
            boolean workshopMissing) {
        if (adults < 0 || beds < 0) {
            throw new IllegalArgumentException("adults and beds must not be negative");
        }

        if (adults > beds) {
            return HOUSING_DEFICIT;
        }

        if (workshopMissing) {
            return WORKSHOP;
        }

        if (!hasPreviousAttempt) {
            return FIRST_HOUSE;
        }

        return lastAttemptWasHouse ? ROTATION_NON_RESIDENTIAL : ROTATION_HOUSE;
    }

    /** Se a prioridade atual exige uma planta de moradia. */
    public boolean requiresHouse() {
        return this != ROTATION_NON_RESIDENTIAL && this != WORKSHOP;
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
