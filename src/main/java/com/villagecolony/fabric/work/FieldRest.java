package com.villagecolony.fabric.work;

import com.villagecolony.fabric.integration.RingSweep;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Quando a lavoura de uma colônia volta a ser varrida — P1.5, 2026-09-11.
 *
 * <p>Uma pergunta, um arquivo: é o corte que o {@code LumberjackWork}
 * levou em 2026-08-20, e vale aqui pelo mesmo motivo — o
 * {@code FarmerWork} já passava das quinhentas linhas antes de esta
 * questão existir.
 *
 * <p><b>Por que ela existe.</b> Desde que a varredura do campo passou
 * pelo {@link RingSweep}, uma volta de raio 32 fecha em cinco passagens —
 * e o fazendeiro pergunta <b>por tique</b>. Num campo sem nada maduro
 * isso é o campo inteiro varrido quatro vezes por segundo, para sempre:
 * na sessão de 2026-09-04 foram <b>86 ciclos seguidos</b> com
 * {@code no ripe crop}, a profissão inteira parada.
 *
 * <p>Antes do cursor o custo estava escondido atrás de um defeito maior —
 * a varredura nunca fechava a volta, então nunca havia uma resposta para
 * respeitar. Consertar a cobertura tornou o descanso necessário: é a
 * segunda metade do mesmo item.
 *
 * <p><b>Por colônia, e não por fazendeiro.</b> O campo é da vila — a
 * mesma razão que faz a busca partir do centro e não do aldeão. Dois
 * fazendeiros descansando em separado varreriam o mesmo campo vazio em
 * turnos, que é o gasto sem a economia.
 */
final class FieldRest {

    /**
     * Quanto o campo fica em paz depois de uma volta inteira sem nada.
     *
     * <p>Quatro ciclos — dois minutos —, que é o {@code Worker.REST_CYCLES}
     * do resto do projeto, escrito aqui como o {@code FarmerWork} escreve
     * o limite de travamento dele.
     *
     * <p><b>Curto de propósito, e pelos dois lados.</b> Lavoura leva
     * minutos para amadurecer, então revarrer em seguida é pagar para
     * reaprender o que não mudou — era o custo. Mas o jogador planta
     * quando quer, e um descanso longo o deixaria vendo o fazendeiro
     * ignorar o campo novo; dois minutos é o maior atraso que ele pode
     * sentir.
     */
    private static final int QUIET_TICKS = 4 * 600;

    /** Até que tique o campo de cada colônia fica sem ser varrido. */
    private static final Map<UUID, Long> QUIET_UNTIL = new HashMap<>();

    private FieldRest() {
    }

    /** Se a lavoura desta colônia ainda está de folga neste tique. */
    static boolean isResting(UUID colonyId, long now) {
        return now < QUIET_UNTIL.getOrDefault(colonyId, 0L);
    }

    /**
     * A volta fechou sem nada para fazer: o campo descansa.
     *
     * <p><b>Só depois de uma volta inteira.</b> Varredura que o orçamento
     * cortou não sabe se o campo está vazio, e descansar sobre uma
     * resposta que não se tem é exatamente o que o
     * {@code IdleReason.SWEEP_INCOMPLETE} existe para nomear. Quem chama
     * confere o {@code CropPatch.Field#incomplete} antes.
     */
    static void sweptAndFoundNothing(UUID colonyId, long now) {
        QUIET_UNTIL.put(colonyId, now + QUIET_TICKS);
    }

    /** O campo voltou a render, e a folga não vale mais. */
    static void thereIsWorkAgain(UUID colonyId) {
        QUIET_UNTIL.remove(colonyId);
    }

    /**
     * Esquece a folga e o cursor desta colônia.
     *
     * <p>Os dois juntos porque são a mesma varredura vista de dois
     * lados, e separá-los deixaria o teste de jogo limpar metade. O
     * mundo do gametest é um só: uma folga deixada para trás faria o
     * fazendeiro do teste seguinte não varrer o campo que ele acabou de
     * plantar, e a falha apareceria no teste errado.
     */
    static void forget(UUID colonyId) {
        QUIET_UNTIL.remove(colonyId);

        RingSweep.forget(colonyId);
    }

    /** Esquece tudo. Chamado ao descarregar o mundo. */
    static void clearAll() {
        QUIET_UNTIL.clear();
    }
}
