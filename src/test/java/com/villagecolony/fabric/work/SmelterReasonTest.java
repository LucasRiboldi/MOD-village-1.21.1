package com.villagecolony.fabric.work;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O motivo que o fundidor dá quando não acha o que fundir — P0.3.
 *
 * <p><b>O defeito que esta linha consertou.</b> A frase era só
 * {@code "nothing in the colony chests to smelt"}, e com ela três estados
 * bem diferentes saíam idênticos: <i>não há baú nenhum registrado</i>,
 * <i>há seis e estão vazios</i>, <i>há seis e nenhum tem ferro cru</i>. Na
 * sessão de 2026-09-04 ela saiu <b>34 vezes</b> ao lado do ferro dele.
 *
 * <p><b>Por que existia sem teste até 2026-09-18.</b> O fundidor tem três
 * gametests e os três são de <b>caminho feliz</b> — areia vira vidro,
 * ferro cru vira lingote, o minério da boca da mina é contado. Nenhum
 * exercita o caminho de falha, então a frase que existe <b>para
 * diagnosticar</b> estava ela mesma sem diagnóstico: trocá-la de volta
 * pela versão ambígua não quebraria nada, e é exatamente assim que uma
 * instrumentação morre em silêncio.
 *
 * <p>É o §11 se voltando contra o próprio remédio, e esta base já pagou
 * por isso: quando a divisão do fabricante entrou, removido o
 * {@code continue} dela, 701 unitários e 275 testes de jogo continuavam
 * verdes.
 */
class SmelterReasonTest {

    /**
     * Sem baú nenhum, o fundidor não é o assunto.
     *
     * <p>É a única das três em que o problema não é o que ele procura —
     * não adianta dizer o que faltava se não havia onde procurar. Por
     * isso a frase não cita o material.
     */
    @Test
    void withNoChestTheAnswerIsNotAboutWhatHeWanted() {
        String said = SmelterWork.lookedButFound(0, "minecraft:raw_iron");

        assertEquals("no colony chest to look in", said);

        assertTrue(
                !said.contains("raw_iron"),
                "sem baú para olhar, citar o material desvia de quem tem o problema: " + said);
    }

    /**
     * Com baús, a frase diz <b>quantos</b> e <b>o quê</b>.
     *
     * <p>Os dois números juntos são o que separa "há seis e estão vazios"
     * de "há seis e nenhum tem ferro cru" — quem lê o log vai ao baú
     * conferir, e precisa saber o que procurar lá dentro.
     */
    @Test
    void withChestsItSaysHowManyAndWhatWasMissing() {
        String said = SmelterWork.lookedButFound(6, "minecraft:raw_iron");

        assertEquals("none of 6 colony chests had minecraft:raw_iron to smelt", said);
    }

    /**
     * Um baú só continua sendo o caso "procurei e não achei".
     *
     * <p>A fronteira importa: {@code 1} não pode cair no caminho do
     * "nenhum baú", senão a vila com um baú vazio volta a dar a resposta
     * de quem não tem baú — e o diagnóstico erra o alvo de novo.
     */
    @Test
    void asingleChestIsStillASearch() {
        String said = SmelterWork.lookedButFound(1, "minecraft:sand");

        assertTrue(said.startsWith("none of 1 colony chests had"), said);

        assertNotEquals("no colony chest to look in", said);
    }

    /**
     * Os três estados dão três frases, que é a razão de a linha existir.
     *
     * <p>Se duas delas colidirem, o P0.3 volta: o log passa a ter uma
     * frase para dois mundos diferentes, e a sessão seguinte gasta tempo
     * descobrindo qual deles era.
     */
    @Test
    void theThreeStatesNeverCollide() {
        String noChest = SmelterWork.lookedButFound(0, "minecraft:raw_iron");
        String emptyChests = SmelterWork.lookedButFound(6, "minecraft:raw_iron");
        String otherMaterial = SmelterWork.lookedButFound(6, "minecraft:sand");

        assertNotEquals(noChest, emptyChests, "sem baú e com baús deram a mesma frase");

        assertNotEquals(
                emptyChests, otherMaterial, "materiais diferentes deram a mesma frase");
    }
}
