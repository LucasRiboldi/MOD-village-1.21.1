package com.villagecolony.core.coordination;

/**
 * O prazo único do socorro contra parada — item F4 do autor, 2026-10-13:
 * <i>"as profissões devem trabalhar em ritmo contínuo … sem travamentos ou bloqueios"</i>.
 *
 * <p>Era seis cópias de {@code 4 * VillageDetector.CYCLE_TICKS} (2.400 tiques, dois minutos)
 * espalhadas pelos ofícios que <b>andam</b> sem chegar — construtor, carpinteiro, pedreiro,
 * fazendeiro, pastor, lenhador. Cada um guardava o seu, e a reanálise estrutural de 13-10
 * mostrou que prazo divergente para o mesmo problema é conflito de regra (F6). Aqui há um só.
 *
 * <p>A ação de parada continua sendo a de cada ofício (devolver a tarefa, marcar o alvo, fugir
 * do encalhe); o que esta constante unifica é <b>quando</b> ela dispara. O guarda de ficar
 * imóvel no mesmo bloco é outro, mais curto, e segue em {@link com.villagecolony.fabric.work.WorkStall}.
 */
public final class WorkRescue {

    /**
     * Tiques de expediente andando sem chegar ao alvo antes do socorro: 1.200, um minuto.
     * Metade dos dois minutos de antes — contínuo o bastante para o autor, folgado o bastante
     * para o lenhador que anda até uma árvore distante sem ser expulso no caminho.
     */
    public static final int LIMIT = 1_200;

    private WorkRescue() {
    }
}
