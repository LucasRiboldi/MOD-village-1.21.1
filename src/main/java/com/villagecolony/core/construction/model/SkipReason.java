package com.villagecolony.core.construction.model;

/** Por que uma peça permaneceu pendente na obra. */
public enum SkipReason {

    /** O estado do bloco não pode ser colocado sem um apoio físico. */
    UNSUPPORTED,

    /**
     * O construtor andou e não alcançou a peça: ela fica de lado e a obra segue
     * pela próxima — A-6, 2026-10-02. Volta a ser tentada no ciclo seguinte.
     */
    UNREACHABLE
}
