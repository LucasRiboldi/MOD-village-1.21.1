package com.villagecolony.core.coordination;

/**
 * O teto da regra do ócio — Regra 51 (ADR-041 #3): mais de {@link #IDLE_SHARE}% de
 * ocioso + bloqueado + encalhado no expediente é ofício a melhorar.
 *
 * <p><b>Só medida.</b> A troca de ofício que esta classe fazia (o ofício parado cedia
 * uma pessoa) foi desligada pelo autor em 2026-10-11: no playtest da 0.3.29 foram 19
 * saídas em 90 minutos, de um ofício parado para outro também parado — "vira um tanto
 * faz". O número continua sendo o critério do {@code time_ledger} e do {@code /vc log}.
 */
public final class IdleYield {

    public static final int IDLE_SHARE = 15;

    private IdleYield() {
    }
}
