package com.villagecolony.fabric.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A repartição do ciclo da colônia — P2.1, 2026-09-11.
 *
 * <p>O ciclo mediu 112 ms contra o orçamento de 50, e o log tinha uma
 * linha só: <i>"Colony cycle took 112 ms"</i>. Ela diz que custou caro e
 * não diz onde, e com seis colônias e setenta e oito trabalhadores "onde"
 * tem pelo menos seis respostas.
 *
 * <p><b>As afirmações são sobre ordem e sobre a sobra</b>, e não sobre
 * milissegundos exatos. O relógio é lido dentro do {@code since}, então o
 * número de uma fase carrega os microssegundos da própria chamada; os
 * casos abaixo trabalham com diferenças de dezenas de milissegundos, onde
 * esse ruído não alcança. Um teste que exigisse o valor exato mediria a
 * máquina, e falharia na primeira que fosse mais lenta.
 */
class CycleCostTest {

    private static final Pattern OTHER = Pattern.compile("other (\\d+) ms");

    private static long ms(long millis) {
        return millis * 1_000_000L;
    }

    /** Quanto a linha atribuiu ao que nenhuma fase reclamou. */
    private static long otherIn(String line) {
        Matcher matcher = OTHER.matcher(line);

        assertTrue(matcher.find(), "a linha não tem sobra: " + line);

        return Long.parseLong(matcher.group(1));
    }

    @BeforeEach
    void forgetTheLastCycle() {
        CycleCost.startOver();
    }

    /**
     * A fase mais cara vem primeiro, porque é por onde se começa.
     *
     * <p>É a razão de a linha existir: o primeiro nome dela é a resposta
     * a <i>"onde otimizar"</i>. Ordem alfabética, ou a ordem do enum,
     * deixaria o leitor procurando o maior número à mão.
     */
    @Test
    void theMostExpensivePhaseComesFirst() {
        long now = System.nanoTime();

        CycleCost.since(CycleCost.Phase.WORKERS, now - ms(50));
        CycleCost.since(CycleCost.Phase.CHESTS, now - ms(100));
        CycleCost.since(CycleCost.Phase.PLANNER, now - ms(10));

        String line = CycleCost.breakdown(ms(200));

        assertTrue(line.indexOf("chests") < line.indexOf("workers"), line);
        assertTrue(line.indexOf("workers") < line.indexOf("planner"), line);
    }

    /**
     * Toda fase aparece, inclusive a que custou zero.
     *
     * <p><i>"O planejador custou zero"</i> é uma resposta. Omitir a fase
     * barata deixaria quem lê sem saber se ela é barata ou se ninguém a
     * mediu — que é a diferença entre um número e a falta dele, e o
     * defeito que o P0.2 desta mesma data custou ao plano.
     */
    @Test
    void everyPhaseAppearsEvenTheOnesThatCostNothing() {
        CycleCost.since(CycleCost.Phase.CHESTS, System.nanoTime());

        String line = CycleCost.breakdown(ms(10));

        for (CycleCost.Phase phase : CycleCost.Phase.values()) {
            assertTrue(line.contains(phase.label()),
                    "a fase " + phase.label() + " sumiu da linha: " + line);
        }

        assertTrue(line.contains("other"), line);
    }

    /**
     * O que nenhuma fase reclamou sai como sobra.
     *
     * <p>E a sobra é informação: crescer ali quer dizer que falta uma
     * fase na lista, e não que o custo sumiu.
     */
    @Test
    void whatNoPhaseClaimedShowsUpAsOther() {
        CycleCost.since(CycleCost.Phase.CHESTS, System.nanoTime() - ms(30));

        long other = otherIn(CycleCost.breakdown(ms(100)));

        assertTrue(other >= 68 && other <= 70,
                "cem menos trinta deviam sobrar setenta, e sobraram " + other);
    }

    /**
     * A mesma fase cobrada duas vezes soma, e não sobrescreve.
     *
     * <p>Não é hipótese: a detecção roda em dois momentos do mesmo ciclo
     * — em volta dos jogadores e a partir dos centros conhecidos —, e
     * cada colônia cobra as fases dela. Sobrescrever faria o ciclo de
     * seis colônias reportar o custo de uma.
     */
    @Test
    void aPhaseChargedTwiceAccumulates() {
        long now = System.nanoTime();

        CycleCost.since(CycleCost.Phase.DETECT, now - ms(20));
        CycleCost.since(CycleCost.Phase.DETECT, now - ms(20));

        long other = otherIn(CycleCost.breakdown(ms(100)));

        assertTrue(other >= 58 && other <= 60,
                "duas cobranças de vinte deviam somar quarenta, e sobrou " + other);
    }

    /**
     * O bastão anda: a marca devolvida serve de início da fase seguinte.
     *
     * <p>É o que garante que nenhum intervalo fique sem dono nem seja
     * contado duas vezes. Uma marca que não andasse cobraria à segunda
     * fase todo o tempo da primeira.
     */
    @Test
    void theBatonHandsTheNowToTheNextPhase() {
        long start = System.nanoTime() - ms(100);

        long afterChests = CycleCost.since(CycleCost.Phase.CHESTS, start);

        assertTrue(afterChests > start, "a marca devolvida não andou");

        CycleCost.since(CycleCost.Phase.WORKERS, afterChests);

        String line = CycleCost.breakdown(ms(150));

        assertTrue(line.indexOf("chests") < line.indexOf("workers"),
                "o tempo dos baús foi parar nas profissões: " + line);
    }

    /**
     * A sobra nunca sai negativa.
     *
     * <p>O total vem de fora, e uma fase que o ultrapasse — arredondamento,
     * ou relógio que andou entre as duas leituras — daria
     * {@code other -1 ms}. Medida impossível na linha faz duvidar do
     * resto dela, que é justamente o que esta linha existe para não
     * causar.
     */
    @Test
    void otherNeverGoesNegative() {
        CycleCost.since(CycleCost.Phase.CHESTS, System.nanoTime() - ms(100));

        assertEquals(0, otherIn(CycleCost.breakdown(ms(10))));
    }

    /**
     * E quando o corte machuca, a linha diz que machucou.
     *
     * <p>Fase somando mais que o ciclo inteiro só acontece por bastão mal
     * passado — um trecho cobrado a duas fases. Cortar em zero e calar
     * trocaria um número impossível por um plausível, e mandaria alguém
     * otimizar a fase inflada que ficou no topo da linha. É o
     * defeito-que-parece-número do V5, e é o mesmo que o P0.2 desta data
     * custou ao plano.
     */
    @Test
    void aBatonPassedTwiceSaysTheNumbersAreInflated() {
        long now = System.nanoTime();

        // Duas fases cobrando o mesmo trecho: cem de ciclo, cento e
        // sessenta cobrados.
        CycleCost.since(CycleCost.Phase.CHESTS, now - ms(80));
        CycleCost.since(CycleCost.Phase.WORKERS, now - ms(80));

        String line = CycleCost.breakdown(ms(100));

        assertEquals(0, otherIn(line));
        assertTrue(line.contains("phases overlap by"),
                "a sobra foi cortada em zero sem dizer por quê: " + line);
        assertTrue(line.contains("inflated"), line);
    }

    /** E no caso normal ela não inventa o aviso. */
    @Test
    void aHealthyCycleSaysNothingAboutOverlap() {
        CycleCost.since(CycleCost.Phase.CHESTS, System.nanoTime() - ms(30));

        assertTrue(!CycleCost.breakdown(ms(100)).contains("overlap"),
                "avisou de sobreposição onde o bastão andou certo");
    }

    /** Ciclo novo não herda a conta do anterior. */
    @Test
    void startingOverForgetsTheLastCycle() {
        CycleCost.since(CycleCost.Phase.CHESTS, System.nanoTime() - ms(50));

        CycleCost.startOver();

        String line = CycleCost.breakdown(ms(10));

        assertTrue(line.contains("chests 0 ms"), line);
        assertEquals(10, otherIn(line));
    }
}
