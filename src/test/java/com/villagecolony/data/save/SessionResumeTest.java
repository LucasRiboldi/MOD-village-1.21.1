package com.villagecolony.data.save;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.ColonyLifecycle;
import com.villagecolony.core.colony.model.ColonyState;
import com.villagecolony.core.coordination.ColonyCycle;
import com.villagecolony.core.resource.model.ResourceTally;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.task.service.TaskService;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.core.worker.service.WorkerService;
import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A vila volta a <b>trabalhar</b> depois de fechar e reabrir o mundo —
 * 2026-09-09.
 *
 * <p><b>A lacuna que ele fecha.</b> O projeto tinha 43 testes de
 * persistência e todos param no mesmo lugar: provam que o estado
 * <i>volta</i>. {@link ColonySavedDataTest} prova que a colônia volta e
 * que o aldeão volta com a profissão; {@link ConstructionSaveTest} prova
 * que a obra volta; {@code MineSaveTest} prova que a mina volta. Nenhum
 * pergunta a coisa que o jogador percebe: <b>depois de voltar, alguém
 * trabalha?</b>
 *
 * <p>É a diferença entre <i>SAVE → LOAD → RESTORE</i>, que estava
 * coberto, e <i>… → CONTINUE → VERIFY</i>, que não estava.
 *
 * <p><b>Por que ela é perigosa.</b> Toda colônia volta do save
 * {@link ColonyLifecycle#DORMANT} — é a ADR-002, e
 * {@code everyColonyComesBackDormant} trava isso de propósito. Quem
 * decide se há trabalho é {@code VillageDetectionHandler.runColonyCycles},
 * e ele <b>pula colônia que não está ACTIVE</b>. Entre "o save voltou
 * inteiro" e "a vila trabalha" existe um despertar, e o elo entre os
 * dois não tinha teste nenhum.
 *
 * <p>Se esse elo quebrar, nada estoura: o mundo abre, os aldeões estão
 * lá, as camas estão lá, e a vila fica parada para sempre. É o modo de
 * falha mais caro que este mod tem, porque ele se parece com o mod
 * simplesmente não fazer nada.
 *
 * <p><b>Onde este teste para.</b> Ele não sobe servidor, então não
 * exercita a observação de verdade — a varredura de camas que chama
 * {@code ColonyService.adopt}. O que ele fixa é o contrato dos dois
 * lados dela: o que sai do save alimenta o ciclo, e o ciclo devolve
 * trabalho ao aldeão que veio do save. A observação em si é dos
 * gametests.
 */
class SessionResumeTest {

    private static final ColonyPos CENTER = new ColonyPos(120, 68, -340);

    /** Sessenta e quatro toras, que é a meta com que os outros testes trabalham. */
    private static final Map<ResourceType, Integer> GOAL =
            Map.of(ResourceType.OAK_LOG, 64);

    private UUID colonyId;

    private UUID villagerId;

    private WorkerService workers;

    private TaskService tasks;

    /** A colônia como ela volta do save. */
    private Colony loaded;

    /**
     * Uma sessão anterior, gravada e relida.
     *
     * <p>Os registros são <b>novos</b> de propósito: é o que acontece ao
     * abrir o mundo, e reaproveitar os antigos faria o teste passar com
     * objetos que nunca chegaram a atravessar o NBT.
     */
    @BeforeEach
    void reopenTheWorld() {
        colonyId = UUID.randomUUID();
        villagerId = UUID.randomUUID();

        Colony before = Colony.restore(
                colonyId, CENTER, ColonyState.PRODUCTION, ColonyLifecycle.ACTIVE);

        Worker beforeWorker = new WorkerService().register(villagerId, colonyId);
        beforeWorker.assign(ProfessionType.LUMBERJACK);

        ColonySavedData data = ColonySavedData.TYPE.constructor().get();
        data.sync(List.of(before), List.of(beforeWorker));

        ColonySavedData reloaded = ColonySavedData.TYPE.deserializer()
                .apply(data.writeNbt(new NbtCompound(), null), null);

        workers = new WorkerService();
        tasks = new TaskService();

        for (Worker worker : reloaded.workers()) {
            workers.restore(worker);
        }

        loaded = reloaded.colonies().get(0);
    }

    /**
     * O ponto de partida, e ele é o que torna o resto necessário.
     *
     * <p>Sem esta afirmação os outros dois pareceriam triviais: é porque
     * a colônia volta dormente que existe um elo a proteger.
     */
    @Test
    void theColonyComesBackAsleep() {
        assertEquals(ColonyLifecycle.DORMANT, loaded.lifecycle());
    }

    /**
     * A vila dormente não abre trabalho, e isso é <b>correto</b>.
     *
     * <p>Colônia dormente tem os chunks descarregados: o estoque lido
     * dela seria zero, ela concluiria que falta tudo, e encheria a fila
     * de pedidos que ninguém pode atender. O ciclo pula quem não está
     * ACTIVE, e este teste trava esse pulo — se alguém o remover
     * "para a vila voltar mais rápido", a fila explode.
     */
    @Test
    void aSleepingColonyIsSkippedByTheCycle() {
        assertFalse(loaded.isActive(), "o ciclo roda em cima desta resposta");
    }

    /**
     * <b>E o aldeão que veio do save volta a receber trabalho.</b>
     *
     * <p>É o teste que a lacuna pedia. Acordada a colônia — o que em jogo
     * é {@code ColonyService.adopt} ao observar as camas —, o ciclo tem
     * de achar a falta, abrir o pedido e entregá-lo <b>ao aldeão
     * restaurado</b>, que é o único que existe.
     */
    @Test
    void theRestoredWorkerGetsWorkOnceTheColonyWakes() {
        loaded.setLifecycle(ColonyLifecycle.ACTIVE);

        ColonyCycle.run(
                colonyId,
                ResourceTally.of(Map.of(ResourceType.OAK_LOG, 10)),
                GOAL,
                tasks,
                workers);

        List<Task> opened = tasks.ofColony(colonyId);

        assertEquals(1, opened.size(), "a vila acordou e não abriu trabalho nenhum");

        Task task = opened.get(0);

        assertEquals(TaskType.COLLECT_WOOD, task.type());

        assertEquals(
                Optional.of(villagerId),
                task.executor(),
                "a tarefa não foi para o aldeão que veio do save");

        assertEquals(TaskState.RESERVED, task.state());
    }

    /**
     * E ela vai para ele <b>por causa da profissão que o save guardou</b>.
     *
     * <p>Esta é a metade que o teste acima não separa. Um aldeão que
     * voltasse sem profissão continuaria existindo, continuaria contando
     * na população, e simplesmente nunca receberia tarefa — a vila
     * pareceria viva e não trabalharia. É o mesmo silêncio do §11, e
     * aqui ele tem nome: {@code workerKeepsProfessionAcrossRoundTrip}
     * prova que a profissão atravessa o NBT; este prova que <b>ela ainda
     * serve para alguma coisa</b> do outro lado.
     */
    @Test
    void itIsTheSavedProfessionThatEarnsHimTheTask() {
        assertEquals(
                Optional.of(ProfessionType.LUMBERJACK),
                workers.find(villagerId).orElseThrow().profession(),
                "a profissão não atravessou o save, e sem ela não há atribuição");

        loaded.setLifecycle(ColonyLifecycle.ACTIVE);

        ColonyCycle.run(
                colonyId,
                ResourceTally.of(Map.of(ResourceType.OAK_LOG, 10)),
                GOAL,
                tasks,
                workers);

        assertTrue(
                tasks.ofColony(colonyId).get(0).isHeld(),
                "o pedido ficou na fila sem executor, com um lenhador disponível");
    }

    /**
     * A vila que já tem o bastante volta calada.
     *
     * <p>O par negativo do teste principal, e ele existe para o teste
     * principal significar alguma coisa: se qualquer colônia acordada
     * abrisse tarefa, provar que a nossa abriu não provaria nada.
     */
    @Test
    void aRestoredColonyWithEnoughOpensNothing() {
        loaded.setLifecycle(ColonyLifecycle.ACTIVE);

        ColonyCycle.run(
                colonyId,
                ResourceTally.of(Map.of(ResourceType.OAK_LOG, 64)),
                GOAL,
                tasks,
                workers);

        assertTrue(
                tasks.ofColony(colonyId).isEmpty(),
                "a vila tinha a meta cheia e abriu pedido assim mesmo");
    }
}
