package com.villagecolony.core.worker.service;

import com.villagecolony.core.type.Capability;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A linha de reserva: quem desiste do ofício não o recebe de volta —
 * decisão do autor, 2026-09-10.
 *
 * <p><b>O achado que fez esta peça existir.</b> A decisão foi
 * <i>"reavaliação de ofício: o trabalhador travado devolve o posto e é
 * recontratado no ciclo seguinte"</i>. Construída ao pé da letra, ela é
 * um {@code rest} caro: o {@link ProfessionAssigner} escolhe a profissão
 * de <b>menor contagem</b> na colônia, e quem devolve o posto acaba de
 * abrir a própria vaga — no ciclo seguinte ele é o mais escasso e volta
 * a ser o que era, para o mesmo trabalho que o travou, tendo escrito no
 * save por nada.
 *
 * <p>O que falta é a exclusão, e ela sobe a mesma escada de prazos do
 * {@code TreeMarks} e do {@code MineMarks}, contada em passagens da
 * distribuição porque o Core não conhece {@code world.getTime()}
 * (ADR-005).
 */
class ProfessionShunTest {

    private static final UUID COLONY = UUID.randomUUID();

    private WorkerService workers;

    @BeforeEach
    void setUp() {
        workers = new WorkerService();
    }

    private Worker aWorker() {
        UUID villagerId = UUID.randomUUID();

        workers.register(villagerId, COLONY);

        return workers.find(villagerId).orElseThrow();
    }

    private Set<UUID> everyone() {
        Set<UUID> ids = new HashSet<>();

        for (Worker worker : workers.all()) {
            ids.add(worker.villagerId());
        }

        return ids;
    }

    private static void cyclesGoBy(Worker worker, int howMany) {
        for (int i = 0; i < howMany; i++) {
            worker.aCycleWentBy();
        }
    }

    /**
     * Enche a colônia até o teto em todo ofício menos os nomeados.
     *
     * <p><b>Sem isto o teste não mede nada</b>, e é o defeito que a
     * mutação pegou na primeira versão deste arquivo: numa colônia vazia
     * todas as contagens são zero, o empate cai na ordem de declaração de
     * {@link ProfessionType}, e o trabalhador sai lenhador quer a exclusão
     * exista quer não. O ofício que se quer evitar precisa ser o
     * <b>unicamente mais escasso</b> — que é justamente o estado em que
     * devolver o posto deixa a colônia.
     */
    private void fillEveryProfessionExcept(ProfessionType... spared) {
        Set<ProfessionType> open = EnumSet.noneOf(ProfessionType.class);

        for (ProfessionType type : spared) {
            open.add(type);
        }

        for (ProfessionType type : ProfessionType.values()) {
            if (open.contains(type)) {
                continue;
            }

            for (int i = 0; i < ProfessionAssigner.MAX_PER_PROFESSION; i++) {
                aWorker().assign(type);
            }
        }
    }

    /**
     * <b>Uma desistência não tira ninguém do ofício</b>, e é metade do
     * desenho: a árvore atrás do rio e a pedra emparedada são o caso
     * comum e saudável, e para elas o {@code rest} já dá a resposta
     * certa. Punir a primeira faria a colônia trocar de ofício o dia
     * inteiro.
     */
    @Test
    void oneGiveUpIsNotEnoughToLeaveTheTrade() {
        Worker worker = aWorker();

        worker.assign(ProfessionType.MINER);

        worker.rest(Capability.COLLECT_STONE);

        assertTrue(worker.hasProfession(), "ele largou o ofício no primeiro tropeço");
    }

    /**
     * <b>Três dentro da janela tiram</b> — é o verificador escalando. A
     * terceira é prova de que não é a parede, é a capacidade inteira:
     * mina sem pedra alcançável, roça fora do raio.
     */
    @Test
    void threeGiveUpsInTheWindowLeaveTheTrade() {
        Worker worker = aWorker();

        worker.assign(ProfessionType.MINER);

        worker.rest(Capability.COLLECT_STONE);
        worker.rest(Capability.COLLECT_STONE);
        worker.rest(Capability.COLLECT_STONE);

        assertFalse(
                worker.hasProfession(),
                "ele teimou três vezes e continuou no mesmo ofício");

        assertTrue(worker.isShunning(ProfessionType.MINER));
    }

    /**
     * <b>E a janela existe</b>: três azares espalhados não são teimosia.
     * Sem ela, um trabalhador perderia o ofício por tropeçar uma vez a
     * cada meia hora.
     */
    @Test
    void giveUpsSpreadOutDoNotAddUp() {
        Worker worker = aWorker();

        worker.assign(ProfessionType.MINER);

        for (int i = 0; i < 3; i++) {
            worker.rest(Capability.COLLECT_STONE);

            cyclesGoBy(worker, 3 * Worker.REST_CYCLES);
        }

        assertTrue(
                worker.hasProfession(),
                "desistências espalhadas somaram como se fossem seguidas");
    }

    /** E a soma é por capacidade: pedra e madeira não se misturam. */
    @Test
    void strikesAreCountedPerCapability() {
        Worker worker = aWorker();

        worker.assign(ProfessionType.MINER);

        worker.rest(Capability.COLLECT_STONE);
        worker.rest(Capability.COLLECT_WOOD);
        worker.rest(Capability.COLLECT_STONE);

        assertEquals(2, worker.strikesOn(Capability.COLLECT_STONE));
        assertTrue(worker.hasProfession());
    }

    @Test
    void givingUpTheProfessionLeavesTheWorkerWithoutOne() {
        Worker worker = aWorker();

        worker.assign(ProfessionType.MINER);

        worker.giveUpProfession();

        assertFalse(worker.hasProfession());
    }

    @Test
    void theAbandonedProfessionIsShunned() {
        Worker worker = aWorker();

        worker.assign(ProfessionType.MINER);

        worker.giveUpProfession();

        assertTrue(worker.isShunning(ProfessionType.MINER));
    }

    /** E só ele: desistir da mina não fecha a roça. */
    @Test
    void theOthersAreStillOpenToHim() {
        Worker worker = aWorker();

        worker.assign(ProfessionType.MINER);

        worker.giveUpProfession();

        assertFalse(worker.isShunning(ProfessionType.FARMER));
    }

    /** Aldeão sem função chamado por engano não ganha castigo por isso. */
    @Test
    void givingUpWithNoProfessionIsSilent() {
        Worker worker = aWorker();

        worker.giveUpProfession();

        for (ProfessionType type : ProfessionType.values()) {
            assertFalse(worker.isShunning(type), type + " ficou de castigo à toa");
        }
    }

    /**
     * <b>Este é o teste que a decisão precisava</b>, e o que ela seria sem
     * a exclusão: um mineiro sozinho na colônia desiste, e a recontratação
     * lhe devolve exatamente o ofício que ele acabou de largar — porque
     * largá-lo é o que o tornou o mais escasso.
     */
    @Test
    void theMinerWhoGaveUpIsNotHiredBackAsAMiner() {
        // Todo o resto no teto: a mina é a única vaga da colônia, e é
        // exatamente o estado que devolver o posto produz — ele abriu a
        // própria vaga e virou o mais escasso.
        fillEveryProfessionExcept(ProfessionType.MINER);

        Worker worker = aWorker();

        worker.assign(ProfessionType.MINER);

        worker.giveUpProfession();

        ProfessionAssigner.assignMissing(workers, COLONY, everyone());

        assertNotEquals(
                ProfessionType.MINER,
                worker.profession().orElse(null),
                "ele voltou para o mesmo ramal que o travou — a reavaliação"
                        + " não fez nada além de escrever no save");
    }

    /**
     * E ele sai com ofício, e não de mãos vazias: a reserva é trabalho.
     *
     * <p>A roça fica com uma vaga aberta e a mina com duas — sem a
     * exclusão, a mina ganha por ser mais escassa, e é isso que discrimina
     * este teste.
     */
    @Test
    void heIsHiredIntoSomethingElseInstead() {
        fillEveryProfessionExcept(ProfessionType.MINER, ProfessionType.FARMER);

        aWorker().assign(ProfessionType.FARMER);

        Worker worker = aWorker();

        worker.assign(ProfessionType.MINER);

        worker.giveUpProfession();

        ProfessionAssigner.assignMissing(workers, COLONY, everyone());

        assertEquals(
                ProfessionType.FARMER,
                worker.profession().orElse(null),
                "a linha de reserva não o levou para a vaga que sobrava");
    }

    /**
     * O castigo vence e o ofício volta a ser dele — é o que separa
     * castigo de aposentadoria. O jogador conserta o terreno e o mod muda
     * de ideia.
     */
    @Test
    void thePunishmentEndsAndTheProfessionComesBack() {
        Worker worker = aWorker();

        worker.assign(ProfessionType.MINER);

        worker.giveUpProfession();

        cyclesGoBy(worker, Worker.SHUN_CYCLES);

        assertFalse(worker.isShunning(ProfessionType.MINER));
    }

    /**
     * <b>E a segunda desistência dura mais que a primeira</b> — a escada.
     * Sem ela toda desistência é a primeira, o prazo nunca cresce, e o
     * trabalhador fica indo e voltando do mesmo ofício impossível a cada
     * oito passagens.
     */
    @Test
    void theSecondGiveUpLastsLongerThanTheFirst() {
        Worker worker = aWorker();

        worker.assign(ProfessionType.MINER);
        worker.giveUpProfession();

        cyclesGoBy(worker, Worker.SHUN_CYCLES);

        worker.assign(ProfessionType.MINER);
        worker.giveUpProfession();

        cyclesGoBy(worker, Worker.SHUN_CYCLES);

        assertTrue(
                worker.isShunning(ProfessionType.MINER),
                "a segunda desistência venceu no mesmo prazo da primeira");
    }

    /**
     * <b>Um trabalhador de molho não congela a contratação da colônia.</b>
     * A vaga que ele recusa fica para o próximo da fila, e não faz a
     * passagem inteira parar — que é o que aconteceria se o vazio por
     * castigo fosse tratado como colônia lotada.
     */
    @Test
    void aShunningWorkerDoesNotBlockTheHiringOfTheOthers() {
        Worker shunning = aWorker();

        for (ProfessionType type : ProfessionType.values()) {
            shunning.assign(type);
            shunning.giveUpProfession();
        }

        Worker fresh = aWorker();

        ProfessionAssigner.assignMissing(workers, COLONY, everyone());

        assertTrue(
                fresh.hasProfession(),
                "o aldeão seguinte ficou sem função por causa do castigo do primeiro");

        assertFalse(
                shunning.hasProfession(),
                "quem está de castigo em tudo fica sem função — é o piso da linha");
    }

    /**
     * E o piso é seguro: sem função não há alvo distante nem material
     * exigido, então não há como falhar de novo — e os castigos continuam
     * andando, porque a distribuição conta ciclo para quem está ocioso.
     */
    @Test
    void theFloorLetsHimBackInEventually() {
        Worker worker = aWorker();

        for (ProfessionType type : ProfessionType.values()) {
            worker.assign(type);
            worker.giveUpProfession();
        }

        cyclesGoBy(worker, Worker.SHUN_CYCLES);

        ProfessionAssigner.assignMissing(workers, COLONY, everyone());

        assertTrue(
                worker.hasProfession(),
                "vencidos os castigos, ele volta a ser contratável");
    }
}
