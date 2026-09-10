package com.villagecolony.core.coordination;

import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.Production;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.type.Capability;
import com.villagecolony.core.worker.model.Profession;
import com.villagecolony.core.worker.service.ProfessionRegistry;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Todo material tem uma profissão responsável, e ela existe.
 *
 * <p><b>A corrente que este teste guarda</b>, e que hoje mora em quatro
 * arquivos:
 *
 * <pre>
 * ResourceType.production()   diz de onde o material vem
 *        ↓
 * ColonyCycle.typeFor()       traduz produção em tarefa
 *        ↓
 * TaskType.required()         diz que capacidade a tarefa exige
 *        ↓
 * ProfessionRegistry          diz que profissão tem essa capacidade
 * </pre>
 *
 * <p><b>Os dois primeiros elos o compilador já garante</b>: o
 * {@code switch} de {@code typeFor} é exaustivo sobre
 * {@link Production} e não tem {@code default}, então produção nova não
 * compila sem resposta. <b>Os dois últimos não tinham guarda nenhuma</b>,
 * e é isso que entra aqui.
 *
 * <p><b>Por que importa, e não é zelo abstrato.</b> Quando nenhuma
 * profissão registrada tem a capacidade que a tarefa exige,
 * {@code ColonyCycle.requestMissing} faz {@code continue} — <b>calado</b>,
 * sem log e sem exceção. A colônia simplesmente nunca produz aquele
 * material, e o que se vê em jogo é {@code assigned 0 tasks (0 open)} sem
 * causa aparente: exatamente o sintoma que custou uma hora de sessão em
 * 2026-09-09 pela roça que travava a vila, e que só foi diagnosticado
 * depois de o autor reclamar de não ver trabalhador nenhum trabalhando.
 *
 * <p>Um recurso novo declarado com uma produção cuja profissão foi
 * removida, ou uma capacidade retirada de uma profissão, abriria esse
 * buraco sem quebrar nada — e ninguém saberia até a sessão.
 *
 * <p><b>Ele chama {@code typeFor} de dentro do pacote, de propósito.</b>
 * Reimplementar o switch aqui validaria a cópia e não a regra, que é a
 * lição de 2026-09-05: o {@code MinerGameTest} reimplementou o
 * {@code footingIn} e passou a medir o predicado antigo enquanto a
 * produção já estava corrigida.
 */
class ProfessionResponsibilityTest {

    /**
     * Todo recurso do jogo tem quem o produza.
     *
     * <p>A afirmação é sobre a colônia inteira: não que exista um
     * trabalhador vivo com a profissão — isso é da sessão —, mas que a
     * profissão <b>exista no registro</b> com a capacidade que a tarefa
     * daquele material exige.
     */
    @Test
    void everyResourceHasAProfessionThatCanProduceIt() {
        List<String> orphans = new ArrayList<>();

        for (ResourceType resource : ResourceType.values()) {
            TaskType task = ColonyCycle.typeFor(resource);

            if (ProfessionRegistry.withCapability(task.required()).isEmpty()) {
                orphans.add(resource
                        + " (" + resource.production()
                        + " → " + task
                        + " → " + task.required() + ")");
            }
        }

        assertTrue(
                orphans.isEmpty(),
                "há material que nenhuma profissão sabe produzir, e a colônia o"
                        + " pularia calada em ColonyCycle.requestMissing: " + orphans);
    }

    /**
     * Toda produção declarada tem profissão, mesmo a que ninguém usa
     * ainda.
     *
     * <p>Separado do teste acima porque as perguntas são diferentes.
     * Aquele varre os recursos que <b>existem</b>; este varre as
     * produções <b>possíveis</b>. Uma produção pode ficar sem nenhum
     * recurso por um tempo — é o estado de quem foi declarada antes do
     * material que a usaria —, e nesse intervalo o teste acima não a
     * cobre, mas o buraco na cadeia já está lá esperando o primeiro
     * recurso que a declare.
     */
    @Test
    void everyDeclaredProductionHasAProfession() {
        List<String> orphans = new ArrayList<>();

        for (Production production : Production.values()) {
            ResourceType sample = anyResourceProducedBy(production);

            if (sample == null) {
                // Produção sem nenhum recurso: nada a afirmar por ela
                // aqui, e o teste vizinho a cobre no dia em que houver.
                continue;
            }

            TaskType task = ColonyCycle.typeFor(sample);

            if (ProfessionRegistry.withCapability(task.required()).isEmpty()) {
                orphans.add(production + " → " + task + " → " + task.required());
            }
        }

        assertTrue(
                orphans.isEmpty(),
                "há produção declarada sem profissão que a atenda: " + orphans);
    }

    /**
     * Nenhuma capacidade de coleta ou produção fica sem dono.
     *
     * <p>A pergunta ao contrário da primeira, e ela pega outra coisa: uma
     * capacidade que <b>nenhuma tarefa exige</b> é código morto, e uma
     * que nenhuma profissão tem é a colônia sem o ofício. Aqui interessa
     * a segunda.
     *
     * <p>{@code BUILD_STRUCTURE} entra como as outras: a obra não nasce
     * de {@code typeFor} — o planejador a abre —, mas construtor nenhum
     * registrado é a vila que não levanta casa.
     */
    @Test
    void everyTaskCapabilityHasSomeoneWhoDeclaresIt() {
        Map<Capability, List<TaskType>> unmanned = new EnumMap<>(Capability.class);

        for (TaskType task : TaskType.values()) {
            if (ProfessionRegistry.withCapability(task.required()).isEmpty()) {
                unmanned.computeIfAbsent(task.required(), key -> new ArrayList<>())
                        .add(task);
            }
        }

        assertTrue(
                unmanned.isEmpty(),
                "há tarefa cuja capacidade nenhuma profissão declara — a colônia"
                        + " abriria trabalho que ninguém pode pegar: " + unmanned);
    }

    /**
     * Nenhuma profissão fica sem material do qual responder.
     *
     * <p>É a pergunta que o autor fez em 2026-09-09 — <i>"a
     * responsabilidade de cada profissão, e o material de que cada uma é
     * responsável"</i> — virada em afirmação.
     *
     * <p>Profissão sem material declarado não é erro de compilação nem
     * quebra sessão: ela simplesmente nunca recebe pedido de produção, e
     * o aldeão fica com placa, baú e ferramenta sem nunca trabalhar. Foi
     * literalmente o estado do fazendeiro até 2026-08-27, e está escrito
     * no javadoc de {@code ResourceType.WHEAT}: <i>"das sete profissões,
     * o fazendeiro era a única sem trabalho"</i>.
     *
     * <p><b>O construtor é a exceção declarada</b>, e não um esquecimento:
     * ele não produz material nenhum — consome. O trabalho dele nasce do
     * {@code ConstructionPlanner}, não de uma meta de recurso.
     */
    @Test
    void everyProfessionAnswersForSomeMaterial() {
        Set<Capability> answeredFor = EnumSet.noneOf(Capability.class);

        for (ResourceType resource : ResourceType.values()) {
            answeredFor.add(ColonyCycle.typeFor(resource).required());
        }

        List<String> idle = new ArrayList<>();

        for (Profession profession : ProfessionRegistry.all()) {
            boolean answers = profession.capabilities().stream().anyMatch(answeredFor::contains);

            if (!answers && !profession.capabilities().contains(Capability.BUILD_STRUCTURE)) {
                idle.add(profession.type() + " " + profession.capabilities());
            }
        }

        assertTrue(
                idle.isEmpty(),
                "há profissão que nenhum material convoca — ela receberia placa,"
                        + " baú e ferramenta e nunca trabalharia, que foi o estado do"
                        + " fazendeiro até 2026-08-27: " + idle);
    }

    /**
     * O mineiro responde pela pedra, e o lenhador não.
     *
     * <p>Uma amostra nomeada da matriz, e ela existe para o teste
     * <b>falhar quando a responsabilidade trocar de dono</b> — os quatro
     * acima passam com qualquer atribuição, desde que exista alguma.
     *
     * <p>Os pares foram escolhidos onde a troca é plausível e o estrago
     * é grande: madeira e lavoura já se confundiram uma vez — está no
     * javadoc de {@link Production#FARMED}, <i>"sem um valor próprio a
     * colheita viraria tarefa de madeira e o lenhador iria derrubar
     * árvore para atender fome"</i> —, e pedra e areia saem os dois da
     * picareta.
     */
    @Test
    void theMaterialGoesToTheProfessionThatOwnsIt() {
        assertSameTask(ResourceType.OAK_LOG, TaskType.COLLECT_WOOD);
        assertSameTask(ResourceType.WHEAT, TaskType.COLLECT_FOOD);
        assertSameTask(ResourceType.COBBLESTONE, TaskType.COLLECT_STONE);
        assertSameTask(ResourceType.SANDSTONE, TaskType.COLLECT_STONE);
        assertSameTask(ResourceType.SAND, TaskType.COLLECT_STONE);
        assertSameTask(ResourceType.WHITE_WOOL, TaskType.COLLECT_WOOL);
        assertSameTask(ResourceType.OAK_PLANKS, TaskType.CRAFT_WOOD_MATERIAL);
        assertSameTask(ResourceType.GLASS, TaskType.SMELT_MATERIAL);
        assertSameTask(ResourceType.SMOOTH_SANDSTONE, TaskType.SMELT_MATERIAL);
        assertSameTask(ResourceType.IRON_INGOT, TaskType.SMELT_MATERIAL);

        // E o que não pode acontecer: lavoura virando trabalho de
        // lenhador é o defeito que o valor FARMED existe para impedir.
        assertFalse(
                ColonyCycle.typeFor(ResourceType.WHEAT) == TaskType.COLLECT_WOOD,
                "a lavoura virou tarefa de madeira, e o lenhador vai derrubar"
                        + " árvore para atender fome");
    }

    private static void assertSameTask(ResourceType resource, TaskType expected) {
        TaskType actual = ColonyCycle.typeFor(resource);

        assertTrue(
                actual == expected,
                resource + " deveria ser trabalho de " + expected + " e é de " + actual);
    }

    private static ResourceType anyResourceProducedBy(Production production) {
        for (ResourceType resource : ResourceType.values()) {
            if (resource.production() == production) {
                return resource;
            }
        }

        return null;
    }
}
