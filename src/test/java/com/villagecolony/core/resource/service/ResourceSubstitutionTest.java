package com.villagecolony.core.resource.service;

import com.villagecolony.core.resource.model.ResourceTally;
import com.villagecolony.core.type.ResourceGroup;
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
     * Pedregulho responde por arenito — e agora a parede concorda.
     *
     * <p><b>Esta asserção era o contrário até 2026-08-26</b>, e a
     * inversão é decisão do autor: <i>abre para pedra só</i>. Vale
     * registrar o que mudou e o que não mudou, porque a asserção antiga
     * nasceu de um defeito visto em jogo.
     *
     * <p>Em 2026-08-22 uma vila de deserto com 320 de pedregulho concluía
     * que a meta de arenito estava cumprida, o mineiro não ia cavar, e o
     * construtor esperava para sempre pelo arenito. <b>O defeito era a
     * discordância</b> entre a conta e o construtor — não a substituição.
     *
     * <p>Com a Regra 27 aberta para pedra, os dois voltam a dizer a mesma
     * coisa: a conta aceita pedregulho, e o construtor assenta
     * pedregulho. É {@link Substitution#ALTERNATIVE}, e é o nível que
     * significa exatamente isso.
     */
    @Test
    void cobblestoneAnswersForSandstoneAndTheWallAcceptsIt() {
        assertEquals(
                Substitution.ALTERNATIVE,
                ResourceSubstitution.levelOf(ResourceType.SANDSTONE, ResourceType.COBBLESTONE),
                "pedregulho devia servir por arenito, e até na parede");

        assertEquals(
                Substitution.ALTERNATIVE,
                ResourceSubstitution.levelOf(ResourceType.COBBLESTONE, ResourceType.SANDSTONE),
                "arenito devia servir por pedregulho, e até na parede");

        assertEquals(
                ResourceType.SANDSTONE,
                ResourceSubstitution.byPreference(ResourceType.SANDSTONE).get(0),
                "o preferido deixou de vir primeiro — a casa vai sair de pedregulho"
                        + " mesmo tendo arenito no baú");
    }

    /**
     * E o déficit acompanha: baú cheio de pedregulho, arenito pago.
     *
     * <p>O outro lado da mesma decisão. Isto só é seguro porque o
     * construtor assenta pedregulho: se ele ainda exigisse arenito, esta
     * asserção seria a obra dormindo para sempre — o defeito de 08-22.
     */
    @Test
    void aChestFullOfCobblestoneNowPaysForSandstone() {
        ResourceTally owned = ResourceTally.of(Map.of(ResourceType.COBBLESTONE, 320));

        Map<ResourceType, Integer> missing =
                ResourceDemand.deficit(Map.of(ResourceType.SANDSTONE, 93), owned);

        assertEquals(
                0,
                missing.getOrDefault(ResourceType.SANDSTONE, 0),
                "a vila continuou devendo arenito com o baú cheio de pedregulho — e o"
                        + " construtor já assenta pedregulho, então isso é o mineiro"
                        + " cavando o que ninguém precisa");
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

        // A pedra deixou de estar sozinha em 2026-08-26: são dois, ela e
        // a outra pedra. Nada mais entrou — ver theWallOnlyEverAcceptsStone.
        assertEquals(2, ResourceSubstitution.acceptedFor(ResourceType.SANDSTONE).size());
        assertEquals(2, ResourceSubstitution.acceptedFor(ResourceType.COBBLESTONE).size());
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
                Substitution.ALTERNATIVE,
                ResourceSubstitution.levelOf(ResourceType.SANDSTONE, ResourceType.COBBLESTONE),
                "a pedra é o único nível de parede que existe, e ela saiu dele");

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
     * A Regra 27 abriu para pedra, e só para pedra.
     *
     * <p><b>Este teste é o guarda da emenda, e não uma afirmação de
     * gosto.</b> {@code ALTERNATIVE} é o nível que o construtor assenta;
     * declarar um bloco nele sem que o construtor o aceite faz a colônia
     * parar de buscar o que a obra espera, e a obra dorme para sempre —
     * o defeito de 2026-08-22.
     *
     * <p>A emenda do autor em 2026-08-26 tem três palavras — <i>abre para
     * pedra só</i> — e é isto que as guarda. Alargar a emenda passa por
     * mudar este teste, e mudá-lo é a hora de reler o defeito.
     */
    @Test
    void theWallOnlyEverAcceptsStone() {
        for (ResourceType required : ResourceType.values()) {
            for (ResourceType offered : ResourceType.values()) {
                if (ResourceSubstitution.levelOf(required, offered)
                        != Substitution.ALTERNATIVE) {

                    continue;
                }

                assertEquals(
                        ResourceGroup.STONE,
                        required.group(),
                        required + " aceita substituto na parede e não é pedra —"
                                + " a Regra 27 só abriu para pedra");

                assertEquals(
                        ResourceGroup.STONE,
                        offered.group(),
                        offered + " foi declarado para a parede e não é pedra");
            }
        }
    }

    /**
     * A madeira continua fora da parede, e o autor sabe.
     *
     * <p>Ela é {@code ACCEPTABLE}: conta para a meta da colônia e o
     * construtor continua exigindo a espécie que a planta pede. <b>A
     * mesma discordância de 08-22 mora aqui</b> — uma colônia com
     * duzentas tábuas de bétula e nenhuma de carvalho declara a meta
     * cumprida enquanto a casa espera carvalho.
     *
     * <p>Está registrado no TODO e não foi mexido porque a decisão do
     * autor foi <i>pedra só</i>. Este teste existe para que a escolha
     * continue visível em vez de virar esquecimento.
     */
    @Test
    void woodCountsForTheGoalAndNotForTheWall() {
        assertEquals(
                Substitution.ACCEPTABLE,
                ResourceSubstitution.levelOf(ResourceType.OAK_PLANKS, ResourceType.BIRCH_PLANKS),
                "a tábua de bétula mudou de nível sem decisão registrada");
    }
}
