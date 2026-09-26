package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ResourceId;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A placa da obra — 2026-09-15.
 *
 * <p><b>Pedido do autor:</b> <i>"precisa sinalizar um texto igual o nome
 * dos aldeoes mostrando o cone do material que falta, quantos tem em
 * estoque e quantos falta para a construcao finalizar"</i>.
 *
 * <p>O log de 23:41 tinha a resposta e o autor não: a obra em
 * {@code 2503,63,-3031} repetia <i>"WAITING_RESOURCES, 382 blocks left,
 * waiting for minecraft:grass_block"</i> a cada ciclo, com o estoque da
 * colônia em {@code {OAK_LOG=21, OAK_PLANKS=86, COBBLESTONE=96, DIRT=17}}
 * — zero grama. Dentro do jogo, nada disso aparecia.
 *
 * <p><b>O texto é montado no Core e afirmado sem mundo</b>, pelo mesmo
 * motivo do {@code SiteOutline}: o que costuma ter defeito é a conta e o
 * nome, não o desenho.
 */
class SiteLabelTest {

    private static final ResourceId GRASS = ResourceId.vanilla("grass_block");

    private static Map<ResourceId, Integer> remaining(int howMany) {
        Map<ResourceId, Integer> tally = new LinkedHashMap<>();
        tally.put(GRASS, howMany);

        return tally;
    }

    /**
     * A linha traz as três coisas que o autor pediu: o quê, quanto há e
     * quanto falta.
     */
    @Test
    void theLabelSaysWhatIsMissingHowManyAreStockedAndHowManyAreLeft() {
        String line = SiteLabel.of(remaining(64), Map.of(GRASS, 17), 382);

        assertTrue(line.contains("grass_block"), "a linha não diz o material: " + line);
        assertTrue(line.contains("17"), "a linha não diz o estoque: " + line);
        assertTrue(line.contains("64"), "a linha não diz o que falta do material: " + line);
        assertTrue(line.contains("382"), "a linha não diz quanto falta da obra: " + line);
    }

    /**
     * O material que a colônia não tem aparece com zero, e não some.
     *
     * <p>É o caso do log: {@code grass_block} não estava no estoque, e um
     * texto que omitisse o zero deixaria o autor sem saber se o material
     * está faltando ou se a placa é que não o conhece.
     */
    @Test
    void aMaterialWithNoStockShowsZero() {
        String line = SiteLabel.of(remaining(64), Map.of(), 382);

        assertTrue(line.contains("0"), "o estoque zero sumiu da linha: " + line);
        assertTrue(line.contains("grass_block"), "o material sumiu junto: " + line);
    }

    /**
     * Sem material faltando, a placa diz que a obra está andando.
     *
     * <p>Distingue os dois estados que o autor precisa separar de longe:
     * parada esperando insumo, ou construindo.
     */
    @Test
    void withNothingMissingTheLabelSaysItIsBuilding() {
        String line = SiteLabel.of(Map.of(), Map.of(), 12);

        assertTrue(line.contains("12"), "a linha não diz quanto falta da obra: " + line);
        assertTrue(
                !line.contains("waiting") && !line.contains("falta:"),
                "a obra está andando e a placa falou de espera: " + line);
    }

    /**
     * O prefixo do namespace vanilla sai — a placa é para ler de longe.
     *
     * <p>{@code minecraft:grass_block} gasta dez caracteres dizendo o que
     * o jogador já sabe, e a placa flutua sobre o lote.
     */
    @Test
    void theVanillaNamespaceIsTrimmed() {
        String line = SiteLabel.of(remaining(64), Map.of(GRASS, 17), 382);

        assertTrue(!line.contains("minecraft:"), "a placa levou o namespace: " + line);
    }

    /** Muitos materiais faltando: a placa mostra o primeiro, não todos. */
    @Test
    void onlyTheFirstMissingMaterialIsShown() {
        Map<ResourceId, Integer> many = new LinkedHashMap<>();
        many.put(GRASS, 64);
        many.put(ResourceId.vanilla("oak_planks"), 30);

        String line = SiteLabel.of(many, Map.of(), 382);

        assertTrue(line.contains("grass_block"), "sumiu o primeiro material: " + line);
        assertEquals(
                -1,
                line.indexOf("oak_planks"),
                "a placa listou tudo e vira parede de texto: " + line);
    }

    // --- o nome na língua do jogo, 2026-09-17 ---

    /**
     * <b>A placa diz o nome do bloco, e não o id.</b>
     *
     * <p>Pedido do autor: <i>"deve estar escrito com o nome dos blocos em
     * português, sinalizando quantos tem nos estoques e quantos faltam na
     * estrutura"</i>. A conta já existia desde 09-15; o nome não.
     *
     * <p>Quem traduz é o jogo, por {@code Block.getName()} — aqui a
     * função é dada pelo teste, e é justamente isso que mantém a regra
     * afirmável sem servidor.
     */
    @Test
    void theLabelUsesTheBlockNameAndNotTheId() {
        String line = SiteLabel.of(
                remaining(64),
                Map.of(GRASS, 17),
                382,
                id -> "Bloco de Grama");

        assertTrue(line.contains("Bloco de Grama"), "a placa não traduziu o nome: " + line);

        assertEquals(
                -1,
                line.indexOf("grass_block"),
                "o id vazou para a placa junto com o nome: " + line);

        // E as três contas do pedido continuam na linha.
        assertTrue(line.contains("17"), "sumiu o estoque: " + line);
        assertTrue(line.contains("64"), "sumiu o que falta do material: " + line);
        assertTrue(line.contains("382"), "sumiu o que falta da obra: " + line);
    }

    /**
     * Nome vazio cai no id, em vez de deixar a placa muda.
     *
     * <p>É o bloco que este jogo não conhece — mod removido, versão
     * diferente. O id é feio e informa; uma placa dizendo
     * {@code "falta : 0/64"} não informa nada.
     */
    @Test
    void anUnknownBlockFallsBackToTheId() {
        String line = SiteLabel.of(remaining(64), Map.of(), 382, id -> "");

        assertTrue(line.contains("grass_block"), "ficou sem nome e sem id: " + line);
    }

    /** E a sobrecarga sem tradução continua valendo, com o id. */
    @Test
    void theOverloadWithoutNamingStillUsesTheId() {
        assertTrue(SiteLabel.of(remaining(64), Map.of(), 382).contains("grass_block"));
    }

    // --- o bloco que falta de verdade, 2026-09-25 (visto em jogo) ---
    //
    // A placa mostrava o PRIMEIRO material restante da planta. No templo de
    // 25-09 a obra parou por falta de tocha (sem carvão), com 34 pedregulhos
    // no baú — e a placa podia anunciar o pedregulho como o que faltava.

    private static final ResourceId COBBLE = ResourceId.vanilla("cobblestone");

    private static final ResourceId TORCH = ResourceId.vanilla("torch");

    private static Map<ResourceId, Integer> templeRemaining() {
        Map<ResourceId, Integer> tally = new LinkedHashMap<>();
        tally.put(COBBLE, 9);
        tally.put(TORCH, 4);
        return tally;
    }

    /**
     * O construtor parou na tocha: é ela que a placa diz, em português.
     *
     * <p>O pedregulho também está curto aqui, e vem antes na planta — sem
     * a regra do próximo bloco, a placa diria "Pedregulho", e o teste pega.
     */
    @Test
    void theLabelNamesTheBlockTheBuilderIsStuckOn() {
        String line = SiteLabel.of(templeRemaining(), Map.of(COBBLE, 5), 13,
                id -> id.equals(TORCH) ? "Tocha" : "Pedregulho", Optional.of(TORCH));

        assertEquals("Obra · falta Tocha: 0/4 · 13 blocos", line);
    }

    /** Sem saber o próximo bloco, o que a vila tem de sobra não é "falta". */
    @Test
    void aMaterialTheColonyHasIsNeverSaidToBeMissing() {
        String line = SiteLabel.of(templeRemaining(), Map.of(COBBLE, 34), 13,
                id -> id.equals(TORCH) ? "Tocha" : "Pedregulho", Optional.empty());

        assertEquals("Obra · falta Tocha: 0/4 · 13 blocos", line);
    }

    /**
     * Nada em falta: a obra está andando, e a placa só conta os blocos.
     *
     * <p>O baú tem <b>exatamente</b> o que a obra pede — ter o bastante não é
     * faltar.
     */
    @Test
    void withEverythingInTheChestsTheLabelOnlyCountsTheBlocks() {
        String line = SiteLabel.of(templeRemaining(), Map.of(COBBLE, 9, TORCH, 4), 13,
                id -> "x", Optional.of(COBBLE));

        assertEquals("Obra: 13 blocos", line);
    }

    /**
     * Peças esperando apoio aparecem na placa — 2026-09-25, visto em jogo:
     * sem isto ela dizia só "Obra: 9 blocos" e parecia que a obra regrediu.
     */
    @Test
    void piecesWaitingForSupportAreCounted() {
        assertEquals("Obra: 9 blocos · 9 sem apoio",
                SiteLabel.of(Map.of(), Map.of(), 9, id -> "x", Optional.empty(), 9));
        assertEquals("Obra: 9 blocos",
                SiteLabel.of(Map.of(), Map.of(), 9, id -> "x", Optional.empty(), 0));
    }
}
