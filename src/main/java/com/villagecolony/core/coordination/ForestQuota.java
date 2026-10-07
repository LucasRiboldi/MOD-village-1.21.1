package com.villagecolony.core.coordination;

/**
 * Quantas árvores a vila ganha — ADR-037 F1 e F2.
 *
 * <p>Cinco árvores nas bordas para cada dez aldeões: na fundação, e de novo a
 * cada dezena nova. E, quando a obra espera madeira, uma árvore natural da
 * espécie pedida para cada cinco aldeões.
 */
public final class ForestQuota {

    /** Árvores por dezena de aldeões. */
    public static final int TREES_PER_TEN = 5;

    /** Aldeões por árvore pedida pela obra. */
    public static final int VILLAGERS_PER_REQUESTED_TREE = 5;

    private ForestQuota() {
    }

    /** As árvores da fundação: cinco por dezena, e cinco mesmo na vila de menos de dez. */
    public static int foundingTrees(int adults) {
        return TREES_PER_TEN * Math.max(1, adults / 10);
    }

    /** A dezena já coberta pela fundação, para o marco começar dali. */
    public static int foundingMilestone(int adults) {
        return Math.max(0, adults / 10) * 10;
    }

    /** As árvores que a obra pede quando espera madeira: uma a cada cinco aldeões, ao menos uma. */
    public static int requestedTrees(int adults) {
        return Math.max(1, adults / VILLAGERS_PER_REQUESTED_TREE);
    }
}
