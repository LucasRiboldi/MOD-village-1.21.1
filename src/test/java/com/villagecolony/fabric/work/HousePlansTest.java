package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.integration.VillageStructures;
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

    private static final ResourceId FARM =
            ResourceId.parse("minecraft:village/plains/houses/plains_small_farm_1");

    private static final ResourceId ANIMAL_PEN =
            ResourceId.parse("minecraft:village/plains/houses/plains_animal_pen_1");

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

    @Test
    void theModBigHouseIsNeverAProfessionBuild() {
        ResourceId bigHouseMod = ResourceId.parse(
                "villagecolony:houses/big_house_mod");

        assertFalse(
                VillageStructures.isProfessionBuildable(bigHouseMod),
                "a BigHouseMOD entrou no catalogo das profissoes");
        assertTrue(
                VillageStructures.isProfessionBuildable(SMALL),
                "uma estrutura Vanilla valida foi retirada do catalogo profissional");
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

    /**
     * A fundação alterna moradia e infraestrutura — 2026-09-20.
     *
     * <p>A falha observada no jogo era {@code house -> house}: a escolha
     * seguinte consultava apenas o catálogo de moradias e nunca registrava
     * que a vez posterior à casa era de outro tipo. A regra agora é
     * {@code house -> A -> house -> B}, com {@code B != A}.
     */
    @Test
    void constructionTurnsAlternateAndTheSecondOtherTypeDiffers() {
        Building house = building(SMALL, true);
        Building farm = building(FARM, true);

        assertTrue(
                HousePlans.nextConstructionIsHouse(List.of()),
                "a primeira construção precisa ser uma casa");

        assertFalse(
                HousePlans.nextConstructionIsHouse(List.of(house)),
                "depois da primeira casa a vez não pode voltar para outra casa");

        assertEquals(
                "farm",
                HousePlans.nextNonHouseType(List.of(house), List.of("farm", "animal_pen"))
                        .orElseThrow(),
                "a primeira construção não residencial deveria escolher o tipo A");

        assertTrue(
                HousePlans.nextConstructionIsHouse(List.of(house, farm)),
                "depois do tipo A a vez deveria voltar para uma casa");

        assertEquals(
                "animal_pen",
                HousePlans.nextNonHouseType(
                                List.of(house, farm, building(SMALL, true)),
                                List.of("farm", "animal_pen"))
                        .orElseThrow(),
                "o tipo B precisa ser diferente do tipo A anterior");
    }

    /**
     * A moradia passa, e a peça que não é casa não — 2026-09-18.
     *
     * <p>O autor pediu variedade de <b>casas</b>, e a pasta
     * {@code houses} do jogo guarda junto tudo que um lote pode receber.
     * Sem este filtro a vila podia levantar um poço no lugar de uma casa
     * e a variedade sairia errada.
     */
    @Test
    void theDwellingPassesAndTheRestDoesNot() {
        assertTrue(
                HousePlans.isDwelling(SMALL),
                "a casa pequena é moradia e foi barrada");

        assertTrue(
                HousePlans.isDwelling(BIG),
                "o açougue é moradia de profissão e foi barrado");

        for (String other : List.of(
                "minecraft:village/plains/houses/plains_animal_pen_1",
                "minecraft:village/plains/houses/plains_meeting_point_4",
                "minecraft:village/plains/houses/plains_temple_3",
                "minecraft:village/plains/houses/plains_stable_1",
                "minecraft:village/plains/houses/plains_accessory_1")) {

            assertFalse(
                    HousePlans.isDwelling(ResourceId.parse(other)),
                    other + " não é moradia e passou pelo filtro");
        }
    }

    /**
     * A roça sai da lista de casas — e continua no catálogo.
     *
     * <p>Duas afirmações numa: {@code FarmPlans.farmsFor} lê da mesma
     * pasta e depende dessas peças, então o filtro tinha de dizer "isto
     * não é casa" sem dizer "isto não existe". Se um dia alguém movê-lo
     * para o {@code load}, este teste continua verde e o
     * {@code FarmPlans.isFarm} abaixo é quem cai.
     */
    @Test
    void theFarmIsNotADwellingButStaysInTheCatalog() {
        ResourceId farm =
                ResourceId.parse("minecraft:village/plains/houses/plains_small_farm_1");

        assertFalse(HousePlans.isDwelling(farm), "a roça entrou na lista de casas");

        assertTrue(FarmPlans.isFarm(farm), "a roça sumiu do catálogo de roças");
    }

    /**
     * A irmã que só cabe girada não fica de fora — 2026-09-18.
     *
     * <p>Quem chama {@code siblingsOf} compara o tamanho <b>depois</b> do
     * giro da Regra 17 — o conserto de 09-16, feito porque uma 13×11
     * aprovada num lote 13×11 vira 11×13 e ocupa treze blocos onde onze
     * foram conferidos.
     *
     * <p>Se a busca de irmãs casasse só o eixo do arquivo, a irmã
     * retangular seria trazida e descartada logo adiante, e a que
     * <b>só cabe girada</b> nunca seria considerada — a variedade voltaria
     * a morrer justamente nas plantas retangulares.
     */
    @Test
    void theSiblingThatOnlyFitsRotatedStillCounts() {
        ColonyPos site = new ColonyPos(13, 7, 11);

        assertTrue(
                HousePlans.fitsEitherWay(new ColonyPos(13, 7, 11), site),
                "a pegada idêntica foi recusada");

        assertTrue(
                HousePlans.fitsEitherWay(new ColonyPos(11, 7, 13), site),
                "a irmã que cabe girada ficou de fora do sorteio");

        assertFalse(
                HousePlans.fitsEitherWay(new ColonyPos(13, 9, 11), site),
                "girar não muda altura, e a planta mais alta passou");

        assertFalse(
                HousePlans.fitsEitherWay(new ColonyPos(9, 7, 11), site),
                "uma pegada que não cabe de jeito nenhum passou");
    }

    /**
     * O filtro atravessa os cinco estilos — 2026-09-18.
     *
     * <p>Os nomes do jogo não têm convenção entre biomas:
     * {@code butcher_shop} na planície e {@code butchers_shop} na savana,
     * {@code mason_1} no deserto e {@code masons_house_1} na taiga. Uma
     * lista de nomes exatos quebraria em quatro dos cinco, e é por isso
     * que o filtro é por substring.
     */
    @Test
    void theFilterCrossesEveryStyle() {
        for (String dwelling : List.of(
                "minecraft:village/savanna/houses/savanna_butchers_shop_1",
                "minecraft:village/desert/houses/desert_mason_1",
                "minecraft:village/taiga/houses/taiga_masons_house_1",
                "minecraft:village/snowy/houses/snowy_weapon_smith_1")) {

            assertTrue(
                    HousePlans.isDwelling(ResourceId.parse(dwelling)),
                    dwelling + " é moradia e foi barrada");
        }

        for (String other : List.of(
                "minecraft:village/savanna/houses/savanna_animal_pen_3",
                "minecraft:village/desert/houses/desert_temple_2",
                "minecraft:village/taiga/houses/taiga_large_farm_2",
                "minecraft:village/snowy/houses/snowy_farm_1")) {

            assertFalse(
                    HousePlans.isDwelling(ResourceId.parse(other)),
                    other + " não é moradia e passou pelo filtro");
        }
    }
}
