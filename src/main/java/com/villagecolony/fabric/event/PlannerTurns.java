package com.villagecolony.fabric.event;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Quais colônias decidem obra neste ciclo — 2026-09-15.
 *
 * <p><b>O que o log do autor mediu, em 2026-09-15:</b> ciclos de 98, 57 e
 * 54 ms contra o orçamento de 50 ms de um tique, com o planejador levando
 * <b>72 ms</b> do pior deles. Havia <b>29 colônias</b>, e nada limitava
 * quantas planejavam por ciclo — o laço de {@code runColonyCycles}
 * percorria a lista inteira, e o custo do planejador era a soma das vinte
 * e nove.
 *
 * <p>O planejador é a fase cara porque carrega a varredura de lote, e a
 * varredura já tem teto <b>por colônia</b>: 1.024 colunas por passagem.
 * O que faltava era teto <b>global</b> — mil colunas vezes vinte e nove
 * colônias cabem num tique só, e coube.
 *
 * <p><b>Por que uma cota de colônias, e não de tempo.</b> Orçamento por
 * relógio se auto-ajusta melhor, mas não é determinístico: o mesmo mundo
 * daria conjuntos diferentes em máquinas diferentes, e o teste mediria a
 * máquina — a lição que o {@code CycleCostTest} registrou em 09-11. A
 * cota é grosseira e é reproduzível, e reproduzível é o que este projeto
 * consegue provar.
 *
 * <p><b>O que isto NÃO faz:</b> não pula o ciclo da colônia. Só o
 * planejamento espera a vez — trabalhador, baú e tarefa continuam
 * andando todo ciclo para todas. É a mesma linha que
 * {@code ColonyAbandonment.plansConstruction} já separava desde 09-02,
 * e pelo mesmo motivo: pular o ciclo inteiro faz o trabalhador andar aos
 * soluços.
 */
final class PlannerTurns {

    /**
     * Quantas colônias decidem obra por ciclo.
     *
     * <p><b>Oito, e o número saiu da medição.</b> O log de 09-15 mediu 72
     * ms de planejador para 29 colônias — cerca de 2,5 ms por colônia. Com
     * oito, a fase volta para perto de 20 ms, e o ciclo inteiro cabe no
     * tique junto com a detecção e as profissões, que juntas levaram 21 ms
     * no mesmo ciclo.
     *
     * <p><b>O preço, dito por inteiro:</b> num mundo de 29 colônias, cada
     * uma passa a decidir obra a cada quatro ciclos — dois minutos, e não
     * trinta segundos. A construção fica mais lenta onde há muitas
     * colônias, que é exatamente onde o tique estava estourando. Num mundo
     * com oito ou menos, nada muda: a cota não aperta o que já cabia.
     *
     * <p>Não confundir com o teto de 1.024 colunas da varredura: aquele
     * limita quanto uma colônia varre por vez, este limita quantas varrem
     * ao mesmo tempo. Os dois juntos é que dão o teto do tique.
     */
    static final int PER_CYCLE = 8;

    /**
     * Onde a vez parou na volta passada.
     *
     * <p>Guarda a <b>colônia</b>, e não o índice: índice numa lista que
     * encolheu aponta para outra colônia, ou para fora. Colônia nasce,
     * adormece e é abandonada o tempo todo — o E9 é a marca de abandono
     * oscilando —, e o rodízio tem de sobreviver a isso sem pular ninguém
     * em silêncio.
     */
    private static UUID resumeAfter;

    private PlannerTurns() {
    }

    /**
     * As colônias da vez, até a cota.
     *
     * <p>Retoma de onde a volta anterior parou e dá a volta na lista, de
     * modo que a fila anda e ninguém fura: em {@code ceil(n / PER_CYCLE)}
     * ciclos toda colônia teve a sua vez, e exatamente uma.
     *
     * <p>A ordem de quem chega manda. Ela vem de
     * {@code ColonyService.all()} e é estável enquanto o mundo não muda —
     * o suficiente para o rodízio ser justo, e nada aqui depende de ela
     * ser sempre a mesma.
     *
     * @param active as colônias que podem planejar agora, na ordem do
     *     registro
     * @return quais delas planejam neste ciclo — todas, quando são menos
     *     que a cota
     */
    static Set<UUID> chooseFrom(List<UUID> active) {
        if (active.isEmpty()) {
            resumeAfter = null;

            return Set.of();
        }

        if (active.size() <= PER_CYCLE) {
            // Não há fila: todas passam, e o cursor não faz sentido. Zerá-lo
            // é o que faz o mundo que encolheu voltar a começar do princípio
            // em vez de guardar uma colônia que já não existe.
            resumeAfter = null;

            return Set.copyOf(active);
        }

        int from = startingAt(active);

        Set<UUID> chosen = new LinkedHashSet<>(PER_CYCLE);

        for (int step = 0; step < active.size() && chosen.size() < PER_CYCLE; step++) {
            chosen.add(active.get((from + step) % active.size()));
        }

        resumeAfter = active.get((from + chosen.size() - 1) % active.size());

        return chosen;
    }

    /**
     * Por onde a volta desta vez começa.
     *
     * <p>A colônia guardada já planejou, então a vez é da seguinte. Some
     * do mundo — abandonada, descarregada, o save recarregado — e a volta
     * recomeça do princípio: é a resposta certa quando não há mais como
     * saber onde a fila estava.
     */
    private static int startingAt(List<UUID> active) {
        if (resumeAfter == null) {
            return 0;
        }

        int last = active.indexOf(resumeAfter);

        return last < 0 ? 0 : (last + 1) % active.size();
    }

    /** Esquece a vez — para os testes, e para o mundo que foi descarregado. */
    static void clearAll() {
        resumeAfter = null;
    }
}
