package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import org.junit.jupiter.api.Test;

import java.util.List;
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
}
