package com.villagecolony.core.coordination;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.resource.model.ResourceTally;
import com.villagecolony.core.resource.service.ResourceDemand;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.Capability;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Production;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.service.ProfessionRegistry;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A corrente do fazendeiro, do recurso à tarefa — 2026-08-27.
 *
 * <p><b>Ele era só uma etiqueta.</b> A colônia lhe dava enxada, baú,
 * placa com o nome e nunca mais falava com ele: não havia recurso de
 * lavoura, não havia produção que o descrevesse, não havia tarefa que
 * pedisse comida, e nenhuma meta que abrisse essa tarefa. Das sete
 * profissões, era a única sem um trabalho — as outras seis buscam e
 * guardam desde a Fase 10.
 *
 * <p>O elo que faltava era inteiro, e é ele que estes testes afirmam:
 *
 * <pre>
 * ResourceType.WHEAT   é lavoura, e vem de FARMED
 * Production.FARMED    vira TaskType.COLLECT_FOOD
 * COLLECT_FOOD         pede Capability.MAINTAIN_FOOD
 * MAINTAIN_FOOD        é o que o FARMER tem
 * a meta de comida     abre a tarefa sem obra nenhuma
 * </pre>
 */
class FarmerChainTest {

    private static Colony colony() {
        return Colony.create(UUID.randomUUID(), new ColonyPos(0, 64, 0));
    }

    /** A lavoura é recurso do mundo, e sai plantada. */
    @Test
    void cropsAreNaturalAndFarmed() {
        for (ResourceType crop : new ResourceType[] {
                ResourceType.WHEAT, ResourceType.CARROT,
                ResourceType.POTATO, ResourceType.BEETROOT}) {

            assertEquals(Production.FARMED, crop.production(), crop + " não vem da lavoura");
            assertEquals(ResourceGroup.CROPS, crop.group(), crop + " não é lavoura");
        }
    }

    /**
     * Qualquer lavoura satisfaz a meta de comida.
     *
     * <p>Mesma razão do grupo da madeira: a pergunta é "esta colônia tem
     * o que comer?", e batata responde tão bem quanto trigo. Sem o grupo,
     * uma vila de cenoura plantaria para sempre porque a meta de trigo
     * nunca cairia.
     */
    @Test
    void anyCropAnswersTheFoodGoal() {
        ResourceTally potatoes = ResourceTally.of(
                Map.of(ResourceType.POTATO, ColonyGoals.FOOD_FLOOR));

        Map<ResourceType, Integer> missing = ResourceDemand.deficit(
                ColonyGoals.of(colony(), potatoes, 64), potatoes);

        assertFalse(missing.containsKey(ResourceType.WHEAT), "pediu trigo com a despensa cheia");
    }

    /** O que é plantado é colhido, e quem colhe é o fazendeiro. */
    @Test
    void farmedResourcesBecomeFoodTasks() {
        assertEquals(
                Capability.MAINTAIN_FOOD,
                TaskType.COLLECT_FOOD.required(),
                "a tarefa de comida não pede a capacidade do fazendeiro");

        assertTrue(
                ProfessionRegistry.of(ProfessionType.FARMER).canPerform(Capability.MAINTAIN_FOOD),
                "o fazendeiro não sabe manter comida");
    }

    /**
     * A comida tem piso, como a pedra ganhou de manhã.
     *
     * <p>Sem obra nenhuma o fazendeiro não teria tarefa, e seria o
     * mineiro das 21:06 de novo: capaz, com baú, e sem nada para fazer a
     * sessão inteira. Comida não depende de obra — a vila come todo dia.
     */
    @Test
    void foodIsAGoalEvenWithNoWorkOpen() {
        Map<ResourceType, Integer> goal = ColonyGoals.of(
                colony(), ResourceTally.empty(), 64);

        assertEquals(ColonyGoals.FOOD_FLOOR, goal.get(ResourceType.WHEAT));
    }

    /** Alcançado o piso, a colônia para de pedir. */
    @Test
    void aFedColonyStopsAsking() {
        ResourceTally stock = ResourceTally.of(
                Map.of(ResourceType.WHEAT, ColonyGoals.FOOD_FLOOR));

        assertFalse(
                ResourceDemand.deficit(ColonyGoals.of(colony(), stock, 64), stock)
                        .containsKey(ResourceType.WHEAT));
    }
}
