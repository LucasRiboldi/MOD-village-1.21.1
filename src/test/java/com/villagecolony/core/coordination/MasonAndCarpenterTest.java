package com.villagecolony.core.coordination;

import com.villagecolony.core.resource.model.ResourceTally;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.task.service.TaskService;
import com.villagecolony.core.type.Capability;
import com.villagecolony.core.type.Production;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.service.ProfessionRegistry;
import com.villagecolony.core.worker.service.WorkerService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O fabricante virou dois ofícios, e os dois trabalham.
 *
 * <p><b>A divisão é de 2026-09-10, a pedido do autor</b>, e o risco dela
 * tem nome: <b>profissão decorativa</b>. Um pedreiro sem material
 * declarado nunca recebe pedido, e o aldeão ganharia placa, baú e
 * ferramenta sem nunca trabalhar — que foi o estado do fazendeiro até
 * 2026-08-27.
 *
 * <p>E o risco tinha uma segunda camada, mais sutil, que quase passou:
 * <b>declarar o material não basta</b>. {@code ProfessionResponsibilityTest}
 * afirma que todo recurso tem profissão capaz, e ele ficaria <b>verde</b>
 * com um pedreiro que nunca recebesse tarefa, porque nada criava meta de
 * tijolo. Este arquivo afirma o degrau seguinte: que a meta <b>vira
 * tarefa</b>, e tarefa <b>do pedreiro</b>.
 */
class MasonAndCarpenterTest {

    private final UUID colony = UUID.randomUUID();

    private TaskService tasks;

    private WorkerService workers;

    @BeforeEach
    void freshColony() {
        tasks = new TaskService();
        workers = new WorkerService();
    }

    /** As duas oficinas existem, e cada uma com a sua capacidade. */
    @Test
    void theTwoShopsAreRegisteredApart() {
        assertTrue(
                ProfessionRegistry.of(ProfessionType.CARPENTER)
                        .canPerform(Capability.CRAFT_WOOD),
                "o carpinteiro não sabe lavrar madeira");

        assertTrue(
                ProfessionRegistry.of(ProfessionType.MASON)
                        .canPerform(Capability.CRAFT_STONE),
                "o pedreiro não sabe lavrar pedra");

        assertFalse(
                ProfessionRegistry.of(ProfessionType.CARPENTER)
                        .canPerform(Capability.CRAFT_STONE),
                "o carpinteiro ainda lavra pedra — a divisão foi só de nome");

        assertFalse(
                ProfessionRegistry.of(ProfessionType.MASON)
                        .canPerform(Capability.CRAFT_WOOD),
                "o pedreiro ainda lavra madeira — a divisão foi só de nome");
    }

    /**
     * A tábua é do carpinteiro, e o tijolo é do pedreiro.
     *
     * <p>É a afirmação nomeada da divisão, e ela falha quando alguém
     * troca os dois de dono — coisa que os testes de existência acima
     * não pegam.
     */
    @Test
    void eachMaterialGoesToItsOwnShop() {
        assertEquals(
                Production.CRAFTED_WOOD,
                ResourceType.OAK_PLANKS.production(),
                "a tábua deixou de ser do carpinteiro");

        assertEquals(
                Production.CRAFTED_STONE,
                ResourceType.STONE_BRICKS.production(),
                "o tijolo deixou de ser do pedreiro");

        assertEquals(
                TaskType.CRAFT_WOOD_MATERIAL,
                ColonyCycle.typeFor(ResourceType.OAK_PLANKS),
                "a tábua virou trabalho de outra oficina");

        assertEquals(
                TaskType.CRAFT_STONE_MATERIAL,
                ColonyCycle.typeFor(ResourceType.STONE_BRICKS),
                "o tijolo virou trabalho de outra oficina");
    }

    /**
     * <b>A meta de tijolo vira tarefa, e do pedreiro.</b>
     *
     * <p>É o teste que impede a profissão decorativa. Sem o degrau que
     * ele afirma — a meta chegando ao ciclo e o ciclo abrindo tarefa da
     * capacidade certa — o pedreiro existiria no registro, teria material
     * declarado, passaria em {@code ProfessionResponsibilityTest} e
     * <b>ainda assim nunca trabalharia</b>.
     */
    @Test
    void theMasonGetsATaskOfItsOwn() {
        workers.register(UUID.randomUUID(), colony).assign(ProfessionType.MASON);

        ColonyCycle.run(
                colony,
                ResourceTally.empty(),
                Map.of(ResourceType.STONE_BRICKS, 16),
                tasks,
                workers);

        assertEquals(
                1,
                countOf(TaskType.CRAFT_STONE_MATERIAL),
                "a meta de tijolo não virou tarefa do pedreiro — ele é decorativo");
    }

    /**
     * E o carpinteiro não pega a tarefa do pedreiro.
     *
     * <p>O caso que o gametest não alcança e o registro sozinho não
     * afirma: uma colônia que só tem carpinteiro <b>não</b> abre trabalho
     * de alvenaria, porque abrir tarefa que ninguém pode pegar a deixaria
     * na fila para sempre. Desde 2026-09-09 ela também <b>diz</b> isso —
     * ver {@link ProductionHands}.
     */
    @Test
    void theCarpenterDoesNotTakeTheMasonsWork() {
        workers.register(UUID.randomUUID(), colony).assign(ProfessionType.CARPENTER);

        ColonyCycle.run(
                colony,
                ResourceTally.empty(),
                Map.of(ResourceType.STONE_BRICKS, 16),
                tasks,
                workers);

        assertEquals(
                0,
                countOf(TaskType.CRAFT_STONE_MATERIAL),
                "a colônia abriu alvenaria sem pedreiro, e a tarefa ficaria na fila"
                        + " para sempre");

        assertEquals(
                0,
                countOf(TaskType.CRAFT_WOOD_MATERIAL),
                "o pedido de tijolo virou trabalho de madeira");
    }

    /**
     * A cadeia do tijolo tem os três degraus, e cada um é de um ofício.
     *
     * <p>Pedregulho é do mineiro, pedra é do fundidor, tijolo é do
     * pedreiro. Se algum degrau trocar de dono, a colônia para no meio —
     * e foi por não ter o degrau do meio que o tijolo não existia até
     * agora.
     */
    @Test
    void theBrickChainHasThreeOwners() {
        assertEquals(
                TaskType.COLLECT_STONE,
                ColonyCycle.typeFor(ResourceType.COBBLESTONE),
                "o pedregulho deixou de ser do mineiro");

        assertEquals(
                TaskType.SMELT_MATERIAL,
                ColonyCycle.typeFor(ResourceType.STONE),
                "a pedra deixou de sair da fornalha, e o tijolo é feito dela");

        assertEquals(
                TaskType.CRAFT_STONE_MATERIAL,
                ColonyCycle.typeFor(ResourceType.STONE_BRICKS),
                "o tijolo deixou de ser do pedreiro");
    }

    private long countOf(TaskType type) {
        return tasks.ofColony(colony).stream()
                .filter(task -> task.type() == type)
                .map(Task::type)
                .count();
    }
}
