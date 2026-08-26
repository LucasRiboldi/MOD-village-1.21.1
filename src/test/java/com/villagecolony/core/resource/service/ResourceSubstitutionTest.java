package com.villagecolony.core.resource.service;

import com.villagecolony.core.resource.model.ResourceTally;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.type.Substitution;
import org.junit.jupiter.api.Test;

import java.util.List;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Grupo é classificação, e não equivalência — regra do autor, 2026-08-22.
 *
 * <p>O que estes testes travam é o <b>padrão</b>: sem declaração, uma
 * exigência se satisfaz só com ela mesma. Foi por um grupo valer como
 * equivalência que o pedregulho passou por arenito, e uma vila de
 * deserto inteira parou de minerar.
 */
class ResourceSubstitutionTest {

    /**
     * O defeito, dito na asserção: pedregulho não é arenito.
     *
     * <p>Os dois moram em {@code ResourceGroup.STONE}, e enquanto o
     * déficit somava o grupo, uma vila de deserto com 320 de pedregulho
     * concluía que a meta de arenito estava cumprida. Visto em jogo em
     * 2026-08-22.
     */
    @Test
    void cobblestoneDoesNotAnswerForSandstone() {
        assertFalse(
                ResourceSubstitution.accepts(ResourceType.SANDSTONE, ResourceType.COBBLESTONE),
                "pedregulho voltou a responder por arenito");

        assertFalse(
                ResourceSubstitution.accepts(ResourceType.COBBLESTONE, ResourceType.SANDSTONE),
                "arenito voltou a responder por pedregulho");
    }

    /** E o déficit acompanha: baú cheio de pedregulho, arenito faltando. */
    @Test
    void aChestFullOfCobblestoneStillOwesSandstone() {
        ResourceTally owned = ResourceTally.of(Map.of(ResourceType.COBBLESTONE, 320));

        Map<ResourceType, Integer> missing =
                ResourceDemand.deficit(Map.of(ResourceType.SANDSTONE, 93), owned);

        assertEquals(93, missing.getOrDefault(ResourceType.SANDSTONE, 0));
    }

    /**
     * Madeira por madeira continua valendo, porque está declarado.
     *
     * <p>É substituição de <b>estoque</b>: quem tem o baú cheio de abeto
     * não precisa de carvalho para responder "esta colônia tem madeira?".
     * A receita continua pedindo a espécie pelo nome.
     */
    @Test
    void anyLogAnswersForAnyOtherLog() {
        assertTrue(ResourceSubstitution.accepts(ResourceType.OAK_LOG, ResourceType.SPRUCE_LOG));
        assertTrue(ResourceSubstitution.accepts(ResourceType.SPRUCE_LOG, ResourceType.OAK_LOG));

        ResourceTally owned = ResourceTally.of(Map.of(ResourceType.SPRUCE_LOG, 64));

        assertEquals(64, ResourceSubstitution.availableFor(ResourceType.OAK_LOG, owned));
    }

    /** E tábua por tábua, pelo mesmo motivo. */
    @Test
    void anyPlankAnswersForAnyOtherPlank() {
        assertTrue(
                ResourceSubstitution.accepts(
                        ResourceType.OAK_PLANKS, ResourceType.ACACIA_PLANKS));
    }

    /** Tronco não é tábua: são grupos diferentes, e nenhum declara o outro. */
    @Test
    void aLogDoesNotAnswerForAPlank() {
        assertFalse(ResourceSubstitution.accepts(ResourceType.OAK_PLANKS, ResourceType.OAK_LOG));
    }

    /**
     * O padrão é não substituir, e é o que impede o defeito de voltar.
     *
     * <p>Tipo novo no {@code ResourceType} não ganha substituto por
     * acidente de grupo — que é exatamente como o arenito entrou.
     */
    @Test
    void everyResourceAnswersForItselfAndNothingElseIsAssumed() {
        for (ResourceType type : ResourceType.values()) {
            assertTrue(
                    ResourceSubstitution.acceptedFor(type).contains(type),
                    type + " deixou de servir para si mesmo");
        }

        assertEquals(1, ResourceSubstitution.acceptedFor(ResourceType.GLASS).size());
        assertEquals(1, ResourceSubstitution.acceptedFor(ResourceType.COAL).size());
        assertEquals(1, ResourceSubstitution.acceptedFor(ResourceType.SANDSTONE).size());
        assertEquals(1, ResourceSubstitution.acceptedFor(ResourceType.COBBLESTONE).size());
    }

    /**
     * Os quatro níveis da ADR-009 §3.10 — decisão do autor em 08-26.
     *
     * <p>O padrão não mudou de comportamento: o que era "não" é
     * {@code FORBIDDEN}, o que era "sim" é {@code ACCEPTABLE}. O que
     * nasceu foi a ordem — e é ela que separa aceitar de preferir.
     */
    @Test
    void theFourLevelsSayHowWellSomethingServes() {
        assertEquals(
                Substitution.PREFERRED,
                ResourceSubstitution.levelOf(ResourceType.OAK_LOG, ResourceType.OAK_LOG),
                "toda exigência se prefere a si mesma, e isso não se declara");

        assertEquals(
                Substitution.ACCEPTABLE,
                ResourceSubstitution.levelOf(ResourceType.OAK_LOG, ResourceType.SPRUCE_LOG),
                "abeto serve por carvalho sem ressalva — está declarado");

        assertEquals(
                Substitution.FORBIDDEN,
                ResourceSubstitution.levelOf(ResourceType.SANDSTONE, ResourceType.COBBLESTONE),
                "pedregulho voltou a responder por arenito — é o defeito de 08-22");

        assertEquals(
                Substitution.FORBIDDEN,
                ResourceSubstitution.levelOf(ResourceType.OAK_PLANKS, ResourceType.OAK_LOG),
                "tronco não é tábua: são grupos diferentes, e um vira o outro por receita");
    }

    /** O preferido vem primeiro, e o resto na ordem do nível. */
    @Test
    void preferenceOrdersWhatServes() {
        List<ResourceType> order = ResourceSubstitution.byPreference(ResourceType.SPRUCE_LOG);

        assertEquals(
                ResourceType.SPRUCE_LOG,
                order.get(0),
                "o que se pediu não veio na frente da lista de preferência");

        for (ResourceType offered : order) {
            assertTrue(
                    ResourceSubstitution.accepts(ResourceType.SPRUCE_LOG, offered),
                    offered + " entrou na ordem de preferência e não serve");
        }

        assertEquals(
                ResourceSubstitution.acceptedFor(ResourceType.SPRUCE_LOG).size(),
                order.size(),
                "a ordem de preferência perdeu ou inventou um recurso");
    }

    /**
     * Nada em ALTERNATIVE enquanto a Regra 27 valer.
     *
     * <p><b>Este teste é um guarda, e não uma afirmação de gosto.</b> A
     * substituição é lida por {@code ResourceDemand.deficit}, que decide
     * quais tarefas abrir — e não pelo construtor, que pela Regra 27
     * aguarda o bloco específico. Declarar que um bloco serve por outro
     * faz a colônia parar de mandar buscar o que o construtor espera, e
     * a obra dorme para sempre. É o defeito de 2026-08-22 por outro
     * caminho.
     *
     * <p>O dia em que a Regra 27 sair, ou em que a demanda de obra deixar
     * de virar meta nominal, este teste sai junto — e aí a variedade que
     * a ADR quer passa a caber.
     */
    @Test
    void nothingIsMerelyAlternativeWhileTheBuilderWaitsForTheExactBlock() {
        for (ResourceType required : ResourceType.values()) {
            for (ResourceType offered : ResourceType.values()) {
                assertTrue(
                        ResourceSubstitution.levelOf(required, offered)
                                != Substitution.ALTERNATIVE,
                        offered + " foi declarado ALTERNATIVE para " + required
                                + " — com a Regra 27 de pé isso faz a colônia parar de"
                                + " buscar o bloco que o construtor espera");
            }
        }
    }
}
