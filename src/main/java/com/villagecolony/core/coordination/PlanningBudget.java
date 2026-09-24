package com.villagecolony.core.coordination;

/**
 * Quanto o planejador pode gastar por ciclo — 2026-09-24.
 *
 * <p><b>O defeito medido.</b> Na sessão de 24-09 o ciclo da colônia passou do
 * tique do servidor 196 vezes em 6,7 horas, e o planejador foi 91% desse
 * tempo: média de 255 ms, máximo de 554 ms, contra os 50 ms de um tique. A
 * cota de oito colônias por ciclo tinha sido calibrada em 09-15 com 2,5 ms
 * por colônia; com a varredura de hoje cada uma custa perto de 30 ms, e
 * havia exatamente oito colônias carregadas — todas planejavam no mesmo
 * tique.
 *
 * <p><b>Duas travas, e as duas se medem pelo relógio.</b>
 *
 * <ul>
 *   <li>{@link #DEADLINE_MS}: quanto uma chamada do planejador pode varrer
 *       antes de guardar o cursor e deixar o resto para o ciclo seguinte.</li>
 *   <li>{@link #nextTurns}: quantas colônias planejam no próximo ciclo,
 *       ajustado pelo custo do anterior — a cota deixa de ser um número
 *       fixo calibrado num mundo que mudou.</li>
 * </ul>
 */
public final class PlanningBudget {

    /** O custo de planejador que o ciclo mira, em milissegundos. */
    public static final int TARGET_MS = 20;

    /** Quanto uma chamada do planejador varre antes de pausar, em ms. */
    public static final int DEADLINE_MS = 15;

    /** O teto da cota: o valor fixo que valia antes, desde 09-15. */
    public static final int MAX_TURNS = 8;

    private PlanningBudget() {
    }

    /**
     * A cota do próximo ciclo, pelo custo do planejador no ciclo que passou.
     *
     * <p>Acima do alvo, cai pela metade (nunca abaixo de um: uma colônia
     * sempre anda). Abaixo de um quarto do alvo, sobe um. Entre os dois,
     * fica — é a folga que impede a cota de oscilar a cada ciclo.
     */
    public static int nextTurns(int current, long lastPlannerMs) {
        int turns = Math.max(1, Math.min(MAX_TURNS, current));

        if (lastPlannerMs > TARGET_MS) {
            return Math.max(1, turns / 2);
        }

        if (lastPlannerMs < TARGET_MS / 4) {
            return Math.min(MAX_TURNS, turns + 1);
        }

        return turns;
    }
}
