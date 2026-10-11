package com.villagecolony.fabric.work;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O prazo de aproximação do mineiro — E44, 2026-09-10.
 *
 * <p>A pergunta que estes casos fixam é a que os dois guardas antigos não
 * faziam: <b>ele está chegando mais perto?</b> O de imobilidade pergunta
 * se o aldeão saiu do bloco, e quem anda em círculos sai; o de
 * travamento pergunta há quanto tempo ele anda, e paga 2.400 tiques de
 * expediente antes de responder. Entre um e outro cabe a sessão das
 * 08:33, em que o mineiro andou o orçamento inteiro <i>"19 blocks below
 * it and unable to climb"</i>.
 *
 * <p>Distância é {@code double} de propósito: é a mesma conta do
 * {@link MinerReach#distanceTo}, e testá-la aqui não sobe servidor.
 */
class MineLeaseTest {

    @Test
    @DisplayName("quem se aproxima renova o prazo e não o perde nunca")
    void aMinerThatGetsCloserKeepsTheStone() {
        MineLease lease = new MineLease();

        // Oitenta blocos de caminhada, a 0,05 por tique: quatro vezes o
        // prazo, e sem piso. O piso era a primeira versão deste caso, e
        // ele falhou com razão — parado a um bloco da pedra, o prazo
        // vence em quatrocentos tiques como venceria em qualquer outro
        // lugar. Em jogo isso não acontece porque um bloco está DENTRO
        // do braço, e o ramo que chama esta classe é o de quem está
        // fora dele; na bateria, era só a fixture inventando um mineiro
        // que congela colado na pedra.
        double distance = 90.0;

        for (int tick = 0; tick < 4 * MineLease.LIMIT; tick++) {
            assertFalse(lease.ranOut(distance), "desistiu de quem se aproximava");
            distance -= 0.05;
        }
    }

    @Test
    @DisplayName("quem não se aproxima perde a pedra ao fim do prazo, e não antes")
    void aMinerThatNeverGetsCloserLosesTheStone() {
        MineLease lease = new MineLease();

        // A primeira leitura é a régua: ela não conta como parada.
        assertFalse(lease.ranOut(9.9));

        for (int tick = 1; tick < MineLease.LIMIT; tick++) {
            assertFalse(lease.ranOut(9.9), "desistiu no tique " + tick);
        }

        assertTrue(lease.ranOut(9.9), "não desistiu no fim do prazo");
    }

    @Test
    @DisplayName("andar para longe é o contrário de progresso")
    void walkingAwayIsNotProgress() {
        MineLease lease = new MineLease();

        double distance = 9.0;

        for (int tick = 0; tick <= MineLease.LIMIT; tick++) {
            lease.ranOut(distance);
            distance += 0.01;
        }

        assertTrue(lease.expired(), "afastar-se manteve o prazo de pé");
    }

    @Test
    @DisplayName("o tremor do caminhante não é aproximação")
    void jitterIsNotProgress() {
        MineLease lease = new MineLease();

        // Meio bloco para cá, meio para lá — é o que a navegação faz
        // quando ela se dá por chegada e o mod ainda diz "fora de
        // alcance". Nenhuma dessas idas bate a régua por MARGEM.
        for (int tick = 0; tick <= MineLease.LIMIT; tick++) {
            lease.ranOut(tick % 2 == 0 ? 9.9 : 9.75);
        }

        assertTrue(lease.expired(), "oscilar no lugar renovou o prazo");
    }

    @Test
    @DisplayName("aproximação menor que a margem não renova nada")
    void creepingCloserBelowTheMarginIsNotProgress() {
        MineLease lease = new MineLease();

        double distance = 9.9;

        // Um centésimo de bloco por tique: em 400 tiques ele anda quatro
        // blocos, e ainda assim cada passo isolado fica abaixo da margem.
        // O que salva este caso é a régua ser o MELHOR de todos os
        // tiques, e não o tique anterior — ver MineLease.closest.
        for (int tick = 0; tick <= MineLease.LIMIT; tick++) {
            assertFalse(lease.ranOut(distance), "desistiu de quem andava");
            distance -= 0.01;
        }
    }

    @Test
    @DisplayName("pedra nova, prazo novo — mesmo se ela estiver mais longe")
    void aNewStoneStartsANewLease() {
        MineLease lease = new MineLease();

        for (int tick = 0; tick <= MineLease.LIMIT; tick++) {
            lease.ranOut(4.5);
        }

        assertTrue(lease.expired());

        lease.reset();

        // Quarenta blocos é pior que os 4,5 de antes. Se a régua tivesse
        // sobrevivido ao alvo, isto seria "andou para longe" e ele
        // desistiria da pedra nova sem ter dado um passo por ela.
        assertFalse(lease.ranOut(40.0));
        assertFalse(lease.expired());
    }

    @Test
    @DisplayName("o prazo cabe na faixa que o autor pediu: 200 a 400 tiques")
    void theLeaseIsShortEnoughToBeWorthHaving() {
        assertTrue(MineLease.LIMIT >= 200 && MineLease.LIMIT <= 400,
                "o prazo saiu da faixa: " + MineLease.LIMIT);

        // E o ponto todo: ele responde antes do guarda de travamento, que
        // é 4 * 600. Escrito à mão porque MinerWork não carrega fora do
        // jogo — os estáticos dele pedem o registro de itens.
        assertTrue(MineLease.LIMIT < 4 * 600);
    }

    /**
     * A descida pela boca tem prazo próprio — 2.2 do roteiro de 10-10.
     *
     * <p>Antes, enquanto havia perna de descida, o prazo era zerado a cada
     * tique: o mineiro que ia e voltava entre a boca (55 blocos da pedra) e um
     * patamar de pedra maciça (59) segurou o único ramal três sessões inteiras.
     */
    @Test
    @DisplayName("na descida, quem vai e volta sem chegar perde a pedra no prazo longo")
    void aDescentThatGoesBackAndForthRunsOutOnTheLongDeadline() {
        MineLease lease = new MineLease();
        int deadline = MineLease.LIMIT * MineLease.DESCENT_FACTOR;

        for (int tick = 0; tick < deadline; tick++) {
            double distance = (tick / 20) % 2 == 0 ? 55.0 : 59.0;

            assertFalse(lease.ranOutOnDescent(distance, 66), "desistiu da descida no tique " + tick);
        }

        assertTrue(lease.ranOutOnDescent(55.0, 66), "o vai e volta da descida não esgotou o prazo");
    }

    @Test
    @DisplayName("na descida, ir até a boca e depois descer não perde a pedra")
    void aDescentThatFirstWalksAwayAndThenComesDownKeepsTheStone() {
        MineLease lease = new MineLease();
        double distance = 60.0;
        int y = 70;

        // Até a boca, na superfície: quarenta blocos para longe da pedra, a passo de aldeão.
        for (int tick = 0; tick < 400; tick++) {
            assertFalse(lease.ranOutOnDescent(distance, y), "desistiu indo até a boca, no tique " + tick);
            distance += 0.1;
        }

        // Pela escada em caracol: um bloco mais fundo a cada vinte tiques, sem
        // chegar mais perto da pedra — só a profundidade conta como avanço.
        for (int tick = 0; tick < 1000; tick++) {
            if (tick % 20 == 0) {
                y--;
            }

            assertFalse(lease.ranOutOnDescent(distance, y), "desistiu descendo a escada, no tique " + tick);
        }
    }
}
