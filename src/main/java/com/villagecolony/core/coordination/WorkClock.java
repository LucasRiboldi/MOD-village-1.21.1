package com.villagecolony.core.coordination;

/**
 * O expediente da colônia — a Regra 18.
 *
 * <p>Só o relógio: recebe a hora do dia e responde se é hora de
 * trabalhar. Não conhece aldeão, mundo nem Minecraft, e é por isso que
 * mora aqui — a janela é uma decisão da colônia, e uma decisão que se
 * pode afirmar sem subir um servidor.
 *
 * <p><b>Por que existe.</b> Até 2026-08-19 quem respondia isso era a
 * {@code Schedule} do Vanilla, através da camada Fabric. A resposta
 * dela é curta: {@code villager_default} põe a Activity WORK do tique
 * 2.000 ao 9.000 e manda o aldeão para MEET e IDLE no resto do dia
 * claro. São 7.000 tiques de trabalho num dia de 24.000 — e, o que a
 * sessão de 2026-08-18 mostrou, 3.000 tiques de <b>sol</b> em que a
 * colônia inteira parava de colher, de fabricar e de construir.
 */
public final class WorkClock {

    /** Tiques de um dia do jogo. */
    public static final int DAY = 24_000;

    /**
     * O tique em que o expediente acaba.
     *
     * <p>É onde o Vanilla troca de MEET para IDLE, uma hora antes de
     * mandar o aldeão dormir, e a escolha é deliberada: a última hora
     * de sol fica para ele voltar para casa. Trabalhar até o anoitecer
     * o deixaria no mato quando os monstros nascem, e a colônia perderia
     * trabalhador por causa da própria regra.
     */
    public static final int DUSK = 11_000;

    private WorkClock() {
    }

    /**
     * É expediente nesta hora do dia?
     *
     * @param timeOfDay a hora do mundo, em tiques. Aceita o valor
     *     acumulado do mundo — o dia é tirado por resto, e valor
     *     negativo nunca chega aqui porque o relógio do jogo só cresce
     */
    public static boolean isWorkTime(long timeOfDay) {
        return Math.floorMod(timeOfDay, DAY) < DUSK;
    }
}
