package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ResourceId;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

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
}
