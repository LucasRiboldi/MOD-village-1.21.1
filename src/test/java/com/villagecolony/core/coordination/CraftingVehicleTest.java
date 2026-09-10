package com.villagecolony.core.coordination;

import com.villagecolony.core.resource.model.ResourceTally;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.task.service.TaskService;
import com.villagecolony.core.type.Production;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.service.WorkerService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A obra pede, e a oficina ganha tarefa.
 *
 * <p><b>O defeito, medido em jogo na sessão de 2026-09-10 às 03:49</b>, que
 * durou quatro horas e vinte: a colônia tinha <b>477 toras e 1.911
 * tábuas</b> nos baús, a casa parada em <b>doze blocos</b> com
 * {@code WAITING_RESOURCES ... waiting for minecraft:stripped_oak_log}, e
 * <b>zero descascadas</b> na sessão inteira. O motivo de ociosidade mais
 * frequente do log era {@code no carpenter work: no task open for it}, 67
 * transições.
 *
 * <p><b>A causa não era o descascar — era o veículo.</b> O
 * {@code produceForWork} só roda dentro de uma tarefa, e a única tarefa de
 * carpintaria nascia da meta de <b>tábua</b>. Com 1.911 delas a meta estava
 * satisfeita para sempre, nenhuma tarefa abria, e a viga que a obra pedia
 * não era trabalho de ninguém. O pedreiro tinha o mesmo problema, e por isso
 * não trabalhou uma vez sequer.
 *
 * <p><b>E havia uma segunda metade, mais silenciosa:</b> a viga descascada
 * <b>não era {@code ResourceType}</b>. Ela nunca apareceu na linha de
 * estoque da colônia — que listava tora, tábua, pedregulho, areia e batata —
 * porque não existia para a contagem. Uma colônia não pede o que não sabe
 * que lhe falta.
 *
 * <p>Este arquivo afirma as duas metades: que a viga é recurso declarado do
 * carpinteiro, e que uma meta dela vira tarefa <b>da oficina certa</b>.
 */
class CraftingVehicleTest {

    private final UUID colony = UUID.randomUUID();

    private TaskService tasks;

    private WorkerService workers;

    @BeforeEach
    void freshColony() {
        tasks = new TaskService();
        workers = new WorkerService();
    }

    /**
     * A viga descascada existe, e é obra do carpinteiro.
     *
     * <p>A metade silenciosa do defeito. Sem esta declaração a colônia não
     * conta a viga no estoque, não sabe que lhe falta, e nenhuma meta pode
     * nascer — o conserto do veículo não teria o que carregar.
     */
    @Test
    void theStrippedBeamIsADeclaredResourceOfTheCarpenter() {
        assertEquals(
                Production.CRAFTED_WOOD,
                ResourceType.STRIPPED_OAK_LOG.production(),
                "a viga descascada deixou de ser trabalho do carpinteiro");

        assertEquals(
                TaskType.CRAFT_WOOD_MATERIAL,
                ColonyCycle.typeFor(ResourceType.STRIPPED_OAK_LOG),
                "a viga virou trabalho de outra oficina");

        // Família própria, e ela não é decoração — a primeira versão desta
        // linha dizia NONE, e a bateria a derrubou. Sem família, o
        // MaterialChoice deixa de oferecer as outras espécies e o
        // carpinteiro para de descascar cerejeira para uma planta que pede
        // carvalho: o defeito de 2026-09-05, com 295 toras no baú.
        assertEquals(
                ResourceGroup.STRIPPED,
                ResourceType.STRIPPED_OAK_LOG.group(),
                "a viga perdeu a família, e deixa de se substituir entre espécies");
    }

    /**
     * <b>A meta de viga vira tarefa, mesmo com o baú cheio de tábua.</b>
     *
     * <p>É a reprodução do defeito da sessão, em miniatura: o estoque tem
     * tábua de sobra — 1.911 lá, 64 aqui — e a obra pede viga. Antes do
     * conserto nenhuma tarefa de carpintaria abria, porque o único veículo
     * era a meta de tábua e ela estava satisfeita.
     */
    @Test
    void theBeamOpensCarpentryEvenWithPlanksToSpare() {
        workers.register(UUID.randomUUID(), colony).assign(ProfessionType.CARPENTER);

        int assigned = ColonyCycle.run(
                colony,
                ResourceTally.of(Map.of(ResourceType.OAK_PLANKS, 64)),
                Map.of(
                        ResourceType.OAK_PLANKS, 64,
                        ResourceType.STRIPPED_OAK_LOG, 16),
                tasks,
                workers);

        assertEquals(
                1,
                assigned,
                "a obra pedia viga, havia carpinteiro, e nenhuma tarefa foi distribuída"
                        + " — é a casa parada em doze blocos com 1.911 tábuas no baú");

        assertTrue(
                tasks.ofColony(colony).stream().anyMatch(task ->
                        task.type() == TaskType.CRAFT_WOOD_MATERIAL
                                && task.targetResource() == ResourceType.STRIPPED_OAK_LOG),
                "a tarefa aberta não é a da viga: " + tasks.ofColony(colony));
    }

    /**
     * A tábua satisfeita continua sem abrir tarefa.
     *
     * <p>A outra ponta, e ela impede o conserto de virar o defeito
     * contrário: o carpinteiro não pode passar a fabricar tábua para sempre
     * só porque a obra a lista. A meta de tábua é de {@code ColonyGoals}, com
     * teto de armazém e a regra de deixar metade da madeira em tora, e a
     * peneira da obra a deixa de fora de propósito.
     */
    @Test
    void satisfiedPlanksStillOpenNothing() {
        workers.register(UUID.randomUUID(), colony).assign(ProfessionType.CARPENTER);

        int assigned = ColonyCycle.run(
                colony,
                ResourceTally.of(Map.of(ResourceType.OAK_PLANKS, 64)),
                Map.of(ResourceType.OAK_PLANKS, 64),
                tasks,
                workers);

        assertEquals(
                0,
                assigned,
                "a meta de tábua estava cumprida e mesmo assim abriu trabalho");
    }

    /**
     * E o pedreiro ganha o dele pela mesma porta.
     *
     * <p>O defeito era um só para as duas oficinas, e o conserto também: a
     * peneira colhe da obra tudo que sai de bancada ou fornalha e não tem
     * outro dono, e a produção declarada manda cada peça ao seu ofício.
     */
    @Test
    void theMasonGetsItsVehicleTheSameWay() {
        workers.register(UUID.randomUUID(), colony).assign(ProfessionType.MASON);

        ColonyCycle.run(
                colony,
                ResourceTally.empty(),
                Map.of(ResourceType.STONE_BRICKS, 8),
                tasks,
                workers);

        assertTrue(
                tasks.ofColony(colony).stream().anyMatch(task ->
                        task.type() == TaskType.CRAFT_STONE_MATERIAL),
                "o pedreiro não ganhou tarefa pela mesma porta: " + tasks.ofColony(colony));
    }

    /**
     * As duas oficinas não recebem a mesma peça.
     *
     * <p>Guarda contra o conserto ir longe demais: abrir o veículo não pode
     * apagar a divisão. A viga é do carpinteiro e o tijolo é do pedreiro, e
     * quem decide continua sendo a produção declarada.
     */
    @Test
    void eachBeamAndBrickKeepsItsOwnShop() {
        assertNotEquals(
                ColonyCycle.typeFor(ResourceType.STRIPPED_OAK_LOG),
                ColonyCycle.typeFor(ResourceType.STONE_BRICKS),
                "a viga e o tijolo caíram na mesma oficina");
    }
}
