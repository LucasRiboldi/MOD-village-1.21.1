package com.villagecolony.fabric.event;

import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.coordination.PlanningBudget;
import com.villagecolony.core.coordination.ScanReport;
import com.villagecolony.fabric.integration.ColonyScanScheduler;

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

    static {
        ServerMemory.register(PlannerTurns.class, PlannerTurns::clearAll);
    }

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

    /** A fila usa o mesmo rodízio limitado que atende as fatias do scanner. */
    private static final ColonyScanScheduler SCHEDULER =
            ColonyScanScheduler.withBudget(PER_CYCLE, ScanReport::empty);

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
        return chooseFrom(active, Set.of());
    }

    /**
     * O mesmo, com as colônias que algum jogador está vendo na frente.
     *
     * <p><b>Decisão do autor, 2026-09-15</b>, depois de entrar no jogo e
     * não ver casa crescendo pela terceira sessão seguida.
     *
     * <p><b>O log daquela sessão mostrou que o sistema funciona, e que a
     * lentidão era a fila.</b> A rua cresceu três vezes em vinte minutos —
     * {@code extended the road} às 21:45, 21:46 e 21:47 —, mas a colônia
     * só teve a vez do planejador <b>16 vezes</b> nesse período, porque
     * este rodízio reparte 29 colônias em oito por ciclo. Nesse ritmo
     * abrir espaço para uma casa leva mais que uma sessão inteira.
     *
     * <p><b>E 28 daquelas 29 estavam dormentes</b>, com os chunks
     * descarregados: o log registra uma única colônia reportando
     * atividade. A fila gastava a vez com colônias que não tinham o que
     * fazer, enquanto a que o jogador observava esperava quatro ciclos.
     *
     * <p><b>A cota não muda</b> — as observadas ocupam vagas dela, e não
     * vagas a mais. O rodízio nasceu para tirar o pico de tique de 214 ms
     * medido no arranque de 09-15, e uma prioridade que alargasse o
     * orçamento o devolveria pela porta dos fundos. Com mais colônias
     * observadas que a cota, passam as primeiras da cota e as outras
     * esperam a vez normal.
     *
     * <p>O resto da fila continua andando atrás delas, e o cursor avança
     * pelas que entraram por rodízio — de modo que ninguém fica para trás,
     * que é o que {@code everyColonyGetsItsTurnWithinOneFullRound} guarda.
     *
     * @param watched as colônias com jogador por perto. Vazio no servidor
     *     sem ninguém online, e aí vale o rodízio puro
     */
    static Set<UUID> chooseFrom(List<UUID> active, Set<UUID> watched) {
        return SCHEDULER.choose(active, watched, turns);
    }

    /**
     * A cota atual — começa no teto e se ajusta pelo custo, 2026-09-24.
     *
     * <p>Os oito de 09-15 viraram o teto, e não o valor: o ciclo de 24-09
     * gastou em média 255 ms de planejador com oito colônias, dez vezes o
     * que a calibração de 09-15 previa. Ver {@code PlanningBudget}.
     */
    private static int turns = PER_CYCLE;

    /** O ciclo terminou: ajusta a cota do próximo pelo custo deste. */
    static void observeCost(long plannerMs) {
        int next = PlanningBudget.nextTurns(turns, plannerMs);

        if (next != turns) {
            VillageColonyMod.LOGGER.info(
                    "Planner turns {} -> {} (last cycle spent {} ms planning, target {} ms)",
                    turns, next, plannerMs, PlanningBudget.TARGET_MS);
            turns = next;
        }
    }

    /** A cota atual, para o log e os testes. */
    static int turns() {
        return turns;
    }

    /** Esquece a vez — para os testes, e para o mundo que foi descarregado. */
    static void clearAll() {
        SCHEDULER.clear();
        turns = PER_CYCLE;
    }
}
