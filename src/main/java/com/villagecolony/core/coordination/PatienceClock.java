package com.villagecolony.core.coordination;

import com.villagecolony.core.colony.service.VillageDetector;

/**
 * Quanto tempo uma obra espera por material antes de sair da frente.
 *
 * <p>Só o relógio: recebe quando a espera começou e quando é agora, e
 * responde se acabou. Não conhece obra, mundo nem Minecraft, e é por
 * isso que mora aqui — como o {@link WorkClock}, é uma decisão da
 * colônia que se pode afirmar sem subir um servidor.
 *
 * <p><b>Por que existe.</b> Até 2026-08-20 nada tirava da frente uma
 * obra parada em {@code WAITING_RESOURCES}. Quem planeja não abre obra
 * nova enquanto houver uma aberta, e o resultado era terminal: a casa de
 * planície pede 43 pedregulhos que a colônia não minera, e se o jogador
 * não os guardasse no baú a vila <b>parava de crescer para sempre</b>.
 * Não era lentidão.
 *
 * <p>O lenhador já tinha o remédio — o guarda de travamento que devolve
 * a tarefa e esquece a árvore. A obra não tinha nada equivalente, e a
 * diferença nunca foi deliberada: era um buraco.
 */
public final class PatienceClock {

    /**
     * Quantos ciclos a obra espera pelo material que falta.
     *
     * <p><b>O número não é escolhido no chute, e o ponto de apoio é a
     * alternativa.</b> Desistir custa uma varredura de lote inteira, e
     * essa varredura leva dezessete ciclos: dezesseis mil colunas no
     * raio de 64, mil por passagem. Uma paciência menor que isso faria a
     * colônia desistir de uma obra antes mesmo de ter terminado de
     * procurar onde pôr a próxima — trocaria uma casa parada por
     * varredura sem fim, que é pior.
     *
     * <p>Vinte é o primeiro número redondo acima disso. Em tiques de
     * jogo são dez minutos, que é uma janela em que dá para ir minerar
     * o pedregulho e voltar — e esta espera, ao contrário de todas as
     * outras do mod, é por uma <b>pessoa</b>, não pelo mundo.
     */
    public static final int CYCLES = 20;

    /** A mesma espera, em tiques. */
    public static final int TICKS = CYCLES * VillageDetector.CYCLE_TICKS;

    private PatienceClock() {
    }

    /**
     * A espera que começou em {@code since} já acabou em {@code now}?
     *
     * @param since o tique em que a obra entrou em espera
     * @param now o tique de agora
     */
    public static boolean ranOut(long since, long now) {
        return now - since >= TICKS;
    }
}
