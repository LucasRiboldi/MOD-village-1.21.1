package com.villagecolony.core.coordination;

import com.villagecolony.core.resource.model.ResourceTally;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.task.service.TaskService;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.service.WorkerService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O material que ninguém sabe fazer deixa de ser pulado calado.
 *
 * <p><b>O defeito, e ele é de log e não de lógica</b> — 2026-09-09.
 * {@code ColonyCycle.requestMissing} pula o pedido quando nenhuma
 * profissão da colônia tem a capacidade que ele exige, e <b>pular está
 * certo</b>: tarefa sem executor possível ficaria na fila para sempre.
 * O que estava errado é que ele pulava sem dizer nada.
 *
 * <p>O que o autor via em jogo era {@code assigned 0 tasks (0 open)} sem
 * causa aparente — o mesmo sintoma da roça que travou a vila nesta mesma
 * data, e que custou uma hora de sessão até alguém ler o código para
 * descobrir por quê. Uma colônia sem fundidor nunca faz vidro, e nada no
 * log dizia isso.
 *
 * <p><b>O que este arquivo afirma é o canal</b>, não a linha de log: quem
 * escreve a linha é {@code VillageDetectionHandler.reportHands}, na
 * camada Fabric, porque o {@code IdleLog} vive lá e a ADR-006 §6 proíbe
 * {@code core} de importar {@code fabric}. Aqui se afirma que a
 * coordenação <b>entrega o número</b> — e o entrega nos dois casos, que é
 * a metade fácil de esquecer: sem o caso {@code hands > 0} o registrador
 * nunca é mandado esquecer, e a colônia que perde a profissão duas vezes
 * fica muda na segunda.
 */
class ProductionHandsTest {

    private UUID colony;

    private TaskService tasks;

    private WorkerService workers;

    /** O que o relatório recebeu, na ordem. */
    private record Counted(ResourceType resource, TaskType type, int hands) {
    }

    private final List<Counted> heard = new ArrayList<>();

    @BeforeEach
    void freshColony() {
        colony = UUID.randomUUID();
        tasks = new TaskService();
        workers = new WorkerService();
        heard.clear();
    }

    private int runWith(Map<ResourceType, Integer> goal) {
        return ColonyCycle.run(
                colony,
                ResourceTally.empty(),
                goal,
                tasks,
                workers,
                worker -> true,
                (resource, type, hands) -> heard.add(new Counted(resource, type, hands)));
    }

    /**
     * A colônia sem a profissão diz qual material ficou sem dono.
     *
     * <p>É o caso da sessão: vidro pedido, fundidor nenhum, e o pedido
     * pulado. O número tem de chegar a quem escreve o log, e tem de ser
     * <b>zero</b> — é o zero que distingue "ninguém sabe fazer" de
     * "ninguém está livre".
     */
    @Test
    void aColonyWithoutTheProfessionSaysWhichMaterialHasNobody() {
        int assigned = runWith(Map.of(ResourceType.GLASS, 12));

        assertEquals(0, assigned, "não havia trabalhador nenhum e mesmo assim distribuiu");

        assertEquals(
                List.of(new Counted(ResourceType.GLASS, TaskType.SMELT_MATERIAL, 0)),
                heard,
                "o vidro sem fundidor não chegou ao relatório, e o pedido foi pulado calado"
                        + " — é o assigned 0 tasks (0 open) sem causa da sessão de 09-09");

        assertTrue(
                tasks.availableFor(colony).isEmpty(),
                "abriu tarefa que ninguém pode pegar, e ela ficaria na fila para sempre");
    }

    /**
     * Com a profissão na colônia, o relatório traz o número e não o zero.
     *
     * <p><b>É a metade que faz o log voltar a falar.</b> O {@code IdleLog}
     * registra transições: quem parou de falar precisa ser mandado
     * esquecer quando o trabalho volta, e é este caso que dá a ordem. Sem
     * ele, a colônia que perde o fundidor, contrata outro e o perde de
     * novo ficaria muda na segunda vez.
     */
    @Test
    void withTheProfessionTheReportCarriesTheCount() {
        workers.register(UUID.randomUUID(), colony).assign(ProfessionType.SMELTER);
        workers.register(UUID.randomUUID(), colony).assign(ProfessionType.SMELTER);

        int assigned = runWith(Map.of(ResourceType.GLASS, 12));

        assertEquals(
                List.of(new Counted(ResourceType.GLASS, TaskType.SMELT_MATERIAL, 2)),
                heard,
                "o relatório não trouxe as duas mãos, e sem o caso hands > 0 o log"
                        + " nunca é mandado esquecer");

        // Pela distribuição, e não por availableFor: um pedido por mão é
        // aberto e os dois fundidores os reservam no mesmo ciclo, então a
        // fila de disponíveis fecha vazia. Afirmar o contrário foi o erro
        // da primeira versão deste teste.
        assertEquals(
                2,
                assigned,
                "havia dois fundidores e o pedido de vidro não virou trabalho deles");
    }

    /**
     * Todo material que falta passa pelo relatório, tenha dono ou não.
     *
     * <p>Afirmação sobre a <b>cobertura</b> do canal, e ela pega o erro
     * de pôr a chamada dentro do {@code if}: um relatório que só fala do
     * que está órfão deixa o registrador sem a ordem de esquecer, e a
     * consequência está no teste acima.
     */
    @Test
    void everyMissingMaterialGoesThroughTheReport() {
        workers.register(UUID.randomUUID(), colony).assign(ProfessionType.MINER);

        runWith(Map.of(
                ResourceType.COBBLESTONE, 32,
                ResourceType.GLASS, 4));

        assertEquals(
                2,
                heard.size(),
                "faltou material no relatório: ele tem de falar dos dois, o que tem"
                        + " mineiro e o que não tem fundidor — ouviu " + heard);

        assertTrue(
                heard.stream().anyMatch(c ->
                        c.resource() == ResourceType.COBBLESTONE && c.hands() == 1),
                "o pedregulho com um mineiro não veio com uma mão: " + heard);

        assertTrue(
                heard.stream().anyMatch(c ->
                        c.resource() == ResourceType.GLASS && c.hands() == 0),
                "o vidro sem fundidor não veio com zero: " + heard);
    }

    /**
     * Quem chama sem se interessar continua chamando como antes.
     *
     * <p>A sobrecarga de seis argumentos é a que a bateria inteira usa, e
     * ela não pode ter mudado de comportamento por causa desta correção.
     */
    @Test
    void theOlderCallStillWorksWithoutAReport() {
        workers.register(UUID.randomUUID(), colony).assign(ProfessionType.MINER);

        int assigned = ColonyCycle.run(
                colony,
                ResourceTally.empty(),
                Map.of(ResourceType.COBBLESTONE, 32),
                tasks,
                workers,
                worker -> true);

        assertEquals(1, assigned, "a chamada de seis argumentos parou de distribuir");

        assertTrue(heard.isEmpty(), "a chamada sem relatório falou com o relatório");
    }
}
