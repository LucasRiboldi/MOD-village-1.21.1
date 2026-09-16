package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A lista de plantas depois de descontar as que a colônia não consegue —
 * 2026-09-12.
 *
 * <p>É a metade da correção que decide, e ela não toca o mundo: quem
 * pergunta ao baú é o {@code PlanRefusals}, e quem escolhe o que sobra da
 * lista é isto.
 *
 * <p><b>Por que existe separado.</b> Na sessão de 09-12 a colônia escolheu
 * a mesma casa impossível duas vezes e o autor não viu construção nenhuma
 * nascer. O conserto é a lista <b>descer um degrau</b> em vez de reoferecer
 * a que morreu — e um filtro sem teste é o defeito que esta base já pagou
 * uma vez: quando a divisão do fabricante entrou, removido o {@code
 * continue} dele, 701 unitários e 275 testes de jogo continuavam verdes.
 */
class HousePlansTest {

    private static final ResourceId BIG =
            ResourceId.parse("minecraft:village/plains/houses/plains_butcher_shop_2");

    private static final ResourceId MEDIUM =
            ResourceId.parse("minecraft:village/plains/houses/plains_medium_house_1");

    private static final ResourceId SMALL =
            ResourceId.parse("minecraft:village/plains/houses/plains_small_house_1");

    /**
     * Uma planta qualquer com aquele id.
     *
     * <p>O conteúdo não importa para esta decisão — ela olha só o id —, e
     * um bloco basta para a planta ser válida.
     */
    private static Blueprint plan(ResourceId id) {
        return Blueprint.of(
                id,
                List.of(new BlueprintBlock(
                        new ColonyPos(0, 0, 0), ResourceId.vanilla("oak_planks"))));
    }

    /** Uma construção qualquer daquela planta, terminada ou não. */
    private static Building building(ResourceId blueprint, boolean finished) {
        return new Building(
                UUID.randomUUID(),
                UUID.randomUUID(),
                blueprint,
                new ColonyPos(0, 0, 0),
                new ColonyPos(1, 1, 1),
                finished);
    }

    /** A ordem da Regra 25: da maior para a menor. */
    private static List<Blueprint> catalog() {
        return List.of(plan(BIG), plan(MEDIUM), plan(SMALL));
    }

    /** Sem marca nenhuma, a lista é a do catálogo, na ordem dele. */
    @Test
    void withNothingSkippedTheListIsUntouched() {
        List<Blueprint> offered = HousePlans.without(catalog(), Set.of());

        assertEquals(3, offered.size());
        assertEquals(BIG, offered.get(0).id(), "a Regra 25 perdeu a ordem decrescente");
    }

    /**
     * A planta marcada sai, e a lista desce um degrau.
     *
     * <p>É o conserto em uma frase: a casa que morreu esperando material
     * não é a escolhida de novo, e quem assume é a seguinte — que pela
     * ordem da Regra 25 é menor.
     */
    @Test
    void theSkippedPlanLeavesAndTheListStepsDown() {
        List<Blueprint> offered = HousePlans.without(catalog(), Set.of(BIG));

        assertFalse(
                offered.stream().anyMatch(plan -> plan.id().equals(BIG)),
                "a planta marcada continuou na lista, e a escolha vai reoferecê-la");

        assertEquals(
                MEDIUM,
                offered.get(0).id(),
                "a lista não desceu para a planta seguinte");
    }

    /**
     * Com todas marcadas, sobra a menor — e não o vazio.
     *
     * <p>Lista vazia faria a colônia parar de planejar por completo, que é
     * exatamente o travamento que a Regra 25 foi escrita para evitar em
     * 2026-08-20. A menor vai morrer esperando material outra vez, e a
     * linha de desistência continua dizendo o que falta: melhor que
     * silêncio.
     */
    @Test
    void withEverythingSkippedTheSmallestStillStands() {
        List<Blueprint> offered =
                HousePlans.without(catalog(), Set.of(BIG, MEDIUM, SMALL));

        assertEquals(1, offered.size(), "sobrou mais de uma planta, ou nenhuma");

        assertEquals(
                SMALL,
                offered.get(0).id(),
                "a que sobrou não é a menor — a ordem é decrescente, então é a última");
    }

    /** Catálogo vazio continua vazio: não há planta a inventar. */
    @Test
    void anEmptyCatalogStaysEmpty() {
        assertTrue(
                HousePlans.without(List.of(), Set.of(BIG)).isEmpty(),
                "inventou planta onde o catálogo não tem nenhuma");
    }

    /** Marca de planta que não está no catálogo não tira nada. */
    @Test
    void aMarkForSomethingElseChangesNothing() {
        List<Blueprint> offered = HousePlans.without(
                catalog(), Set.of(ResourceId.parse("minecraft:village/desert/houses/x")));

        assertEquals(3, offered.size(), "uma marca de outro bioma encurtou a lista");
    }
    /**
     * <b>A primeira casa da colônia é a menor</b> — decisão do autor,
     * 2026-09-15: <i>"dar preferência para a primeira ser uma casa
     * pequena"</i>.
     *
     * <p><b>O que o autor viu em jogo, às 21:02:</b> nenhuma construção
     * nascendo. O log mostrou a colônia abrindo
     * {@code plains_butcher_shop_2} — 382 blocos — às 20:54:35, e a obra
     * parada em <i>"382 blocks left"</i> por sete minutos e meio, segurando
     * a vaga única: <i>"no building work: one is already open"</i>. É a
     * terceira sessão seguida em que a maior planta do catálogo trava a
     * vila antes de a primeira casa existir.
     *
     * <p><b>A Regra 25 continua valendo, e ganha uma exceção.</b> Ela
     * manda levantar a maior planta que couber, e o motivo dela é real —
     * em 2026-08-20 exigir a casa grande em todo lugar fez a vila parar de
     * crescer. O que muda é só o <b>arranque</b>: sem nenhuma casa de pé,
     * a colônia começa pela menor, que é a que ela levanta sozinha. Da
     * segunda em diante a Regra 25 volta inteira.
     */
    @Test
    void theFirstHouseOfAColonyIsTheSmallest() {
        List<Blueprint> offered = HousePlans.smallestFirst(catalog(), true);

        assertEquals(
                SMALL,
                offered.get(0).id(),
                "a primeira casa da colônia não foi a menor planta");

        assertEquals(
                3,
                offered.size(),
                "a preferência da primeira casa encurtou o catálogo em vez de reordená-lo");
    }

    /**
     * Com uma casa de pé, a Regra 25 volta a mandar.
     *
     * <p>É a metade que protege a decisão de 2026-08-20: a exceção é do
     * arranque, e não uma inversão. Uma vila que só levantasse cabana
     * perderia as casas do jogo para sempre.
     */
    @Test
    void afterTheFirstHouseTheBiggestPlanLeadsAgain() {
        List<Blueprint> offered = HousePlans.smallestFirst(catalog(), false);

        assertEquals(
                BIG,
                offered.get(0).id(),
                "a Regra 25 não voltou depois de a colônia ter a primeira casa");
    }

    /** Catálogo de uma planta só não tem o que reordenar. */
    @Test
    void asinglePlanIsTheSameEitherWay() {
        List<Blueprint> one = List.of(plan(SMALL));

        assertEquals(SMALL, HousePlans.smallestFirst(one, true).get(0).id());
        assertEquals(SMALL, HousePlans.smallestFirst(one, false).get(0).id());
    }
    /**
     * <b>Obra abandonada não conta como casa</b> — 2026-09-15.
     *
     * <p><b>O defeito que a investigação de 21:50 achou</b>, e ele estava
     * na correção da véspera. A preferência pela planta menor pergunta se
     * a colônia ainda não tem casa, e perguntava isso ao registro de
     * construções — onde a obra <b>abandonada</b> também entra, porque
     * {@code WaitingWork.giveUp} guarda a caixa dela para o lote não
     * parecer livre.
     *
     * <p>O açougue que a colônia largou às 21:42 virou {@code Building}, a
     * colônia passou a "ter casa" sem ter nenhuma, e a preferência pela
     * pequena <b>nunca dispararia</b> naquela vila. O log do mundo do autor
     * registra <b>56 buildings</b> e <b>zero</b> {@code house is up}.
     *
     * <p>Quem responde agora é {@link Building#finished()}: verdadeiro só
     * para a casa que o construtor terminou.
     */
    @Test
    void anAbandonedProjectDoesNotCountAsAHouse() {
        List<Building> onlyAbandoned = List.of(
                building(BIG, false),
                building(MEDIUM, false));

        assertTrue(
                HousePlans.hasNoHouseYet(onlyAbandoned),
                "duas obras abandonadas passaram por casa levantada, e é o que impede a"
                        + " colônia de preferir a planta pequena");
    }

    /** Uma casa terminada conta, e a Regra 25 volta. */
    @Test
    void aFinishedHouseCounts() {
        List<Building> one = List.of(building(BIG, false), building(SMALL, true));

        assertFalse(
                HousePlans.hasNoHouseYet(one),
                "a colônia tem uma casa de pé e continuou preferindo a planta pequena");
    }

    /** Sem construção nenhuma, a colônia obviamente não tem casa. */
    @Test
    void anEmptyRegistryMeansNoHouse() {
        assertTrue(HousePlans.hasNoHouseYet(List.of()), "registro vazio não é colônia com casa");
    }
}
