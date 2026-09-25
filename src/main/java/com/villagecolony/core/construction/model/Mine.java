package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A mina de uma colônia, e até onde cada ramal dela já foi cavado.
 *
 * <p>{@link MineShaft} é a forma — a geometria da escada, das salas e da
 * galeria. {@link MineArm} é um ramal: a mesma escada, um rumo de
 * galeria, e o cursor daquele rumo. Isto aqui é a <b>mina daquela
 * colônia</b>: o poço mais os quatro ramais que saem dele.
 *
 * <p><b>Por que ela precisava existir.</b> Até 2026-08-20 a mina era
 * campo de um {@code Job} do {@code MinerWork} — memória, e só. Fechar o
 * mundo apagava as duas coisas que custam a refazer:
 *
 * <ul>
 *   <li><b>a boca.</b> A sessão seguinte reprocurava o primeiro bloco
 *       sólido na coluna do fim da vila, e esse bloco tinha sido cavado:
 *       a busca descia mais, achava outro, e a mina de ontem ganhava uma
 *       entrada nova alguns blocos abaixo — duas escadas para a mesma
 *       colônia;
 *   <li><b>o cursor.</b> Voltava a zero, e o mineiro revarria índice por
 *       índice tudo o que já estava aberto para chegar onde parou.
 * </ul>
 *
 * <p><b>Eram um cursor e um dono até 2026-09-04</b>, e são quatro ramais
 * desde então — decisão do autor: <i>"mineiros distintos escolhem
 * caminhos de perfuração distintos dentro das minas"</i>, na forma de
 * <i>ramais da mesma escada</i>. O que mudou não foi a geometria: o nível
 * já era um anel de quatro braços em volta do poço, percorridos <b>em
 * sequência</b> por um mineiro só. São os mesmos quatro, agora ao mesmo
 * tempo.
 *
 * <p>Mutável de propósito, como {@code Colony}: o cursor de um ramal anda
 * a cada posição olhada, e trocar o registro inteiro sessenta e quatro
 * vezes por busca seria cerimônia sem ganho.
 */
public final class Mine {

    /**
     * Quantos ramais saem do poço.
     *
     * <p>Quatro, e o número não é escolha nova: é o
     * {@code TURNS_PER_LEVEL} de antes, com outro nome. A galeria vira no
     * sentido horário, e na quarta curva ela está de volta ao rumo em que
     * começou — o nível deu a volta. Um quinto ramal recavaria o
     * primeiro.
     *
     * <p>É também o teto de mineiros simultâneos numa mina. O quinto
     * mineiro de uma colônia espera, e isso é a geometria falando: não há
     * onde pôr mais um sem dois na mesma frente.
     */
    public static final int ARMS = 4;

    private final UUID colonyId;

    /**
     * O poço: boca, descida, e o rumo do primeiro ramal.
     *
     * <p>Os quatro ramais partilham boca e descida — é a mesma escada — e
     * diferem só no rumo da galeria. Guardar o poço à parte é o que faz
     * {@link #entry()} continuar tendo uma resposta só.
     */
    private MineShaft shaft;

    private final List<MineArm> arms = new ArrayList<>(ARMS);

    /**
     * Se o arco da boca já foi erguido uma vez — 2026-09-11.
     *
     * <p><b>Existe porque ler o mundo não basta aqui.</b> O arco é posto
     * de graça e reconferido a cada passagem de {@code MineDigging.mineOf},
     * e o único teste que ele tinha era se o lugar estava substituível.
     * Quebrar a pedra deixa ar, ar é substituível, e o arco voltava: para
     * o código, <i>"o dono do mundo desfez"</i> e <i>"ainda não construí"</i>
     * eram o mesmo estado. O autor achou em jogo, e a frase foi <i>"deve
     * permitir que seja destruído normalmente e não reaparecendo
     * infinitamente"</i>.
     *
     * <p>O baú da boca <b>não</b> é governado por isto, e de propósito:
     * ele é lido do mundo por {@code ColonyChests} e {@code MinerHaul},
     * que precisam achá-lo onde ele está. O vestígio dele é ele mesmo —
     * um baú é um baú, e {@code furnish} já sai quando acha um.
     *
     * <p>Falso numa mina que o save trouxe sem a chave, e é o que mantém
     * o conserto que a chamada repetida existia para fazer: mina anterior
     * à Regra 30 (2026-08-22) ganha o arco na primeira passagem, registra,
     * e não tenta mais.
     */
    private boolean archRaised;

    /**
     * Quantas vezes o ramal fechou sem uma picareta — E45, 2026-09-16.
     *
     * <p><b>Guarda que o caminho de falha zera não é guarda.</b> O
     * {@code MineArm.blocked} é apagado por {@code finish()} e por
     * {@code restartAt}, isto é, exatamente pelo caminho que o laço
     * percorre — e por isso ele nunca chegou a limite nenhum. Este
     * contador mora na <b>mina</b>, e não no braço, porque o braço é
     * solto e reocupado a cada passagem: só o que sobrevive à passagem
     * consegue contá-las.
     *
     * <p>Quem o zera é a picareta tirando bloco do mundo — ver
     * {@link #pickaxeTook()} —, e <b>só</b> ela. É o mesmo dono adotado
     * em 2026-09-11, pelo mesmo motivo: quem prova progresso é o bloco
     * saindo, não o servir da posição.
     *
     * <p>O playtest de 2026-09-16 fez esta conta chegar a 17.518 em
     * trinta e um minutos, com zero pedra quebrada.
     */
    private int turnsWithoutAPickaxe;

    /**
     * Quantas hélices já foram tentadas neste nível — E45, 2026-09-16.
     *
     * <p>Separado de {@link #turnsWithoutAPickaxe} porque conta outra
     * coisa: aquele mede a paciência com <b>este</b> desenho, e este
     * mede quantos desenhos já se tentou antes de culpar a boca. Girar
     * zera o primeiro e incrementa o segundo.
     *
     * <p>Não é gravado no save: uma sessão nova merece tentar de novo, e
     * a mina que renasce do disco pode ter o mundo mudado em volta.
     */
    private int helicesTried;

    private Mine(UUID colonyId, MineShaft shaft, int[] cuts) {
        this.colonyId = Objects.requireNonNull(colonyId, "colonyId");
        this.shaft = Objects.requireNonNull(shaft, "shaft");

        MineShaft heading = shaft;

        for (int index = 0; index < ARMS; index++) {
            arms.add(new MineArm(heading, index < cuts.length ? cuts[index] : 0));

            heading = heading.turned();
        }
    }

    /** Uma mina que começa agora, com as picaretas no primeiro degrau. */
    public static Mine open(UUID colonyId, MineShaft shaft) {
        return new Mine(colonyId, shaft, new int[ARMS]);
    }

    /**
     * A mina que o save trouxe, com um ramal só.
     *
     * <p>É o caminho do save anterior a 2026-09-04, e o dos testes que
     * falam de uma frente só. Os outros três ramais nascem no primeiro
     * degrau, que é onde estariam se ninguém os tivesse cavado.
     *
     * @param cut a fronteira gravada. Nunca negativa — quem lê o disco
     *     corrige antes de chegar aqui
     */
    public static Mine restore(UUID colonyId, MineShaft shaft, int cut) {
        return restore(colonyId, shaft, new int[] {cut});
    }

    /** A mina que o save trouxe, com a fronteira de cada ramal. */
    public static Mine restore(UUID colonyId, MineShaft shaft, int[] cuts) {
        for (int cut : cuts) {
            if (cut < 0) {
                throw new IllegalArgumentException("cut negativo: " + cut);
            }
        }

        return new Mine(colonyId, shaft, cuts);
    }

    public UUID colonyId() {
        return colonyId;
    }

    /**
     * O poço da mina.
     *
     * <p>Serve a quem quer a <b>boca</b> ou a <b>descida</b>, que são as
     * mesmas em todos os ramais. Quem vai percorrer a ordem de cavar tem
     * de pedir a forma ao ramal — {@link MineArm#shaft()} —, porque é lá
     * que o rumo da galeria muda.
     */
    public MineShaft shaft() {
        return shaft;
    }

    /** Onde a mina começa: a boca, no fim da vila. */
    public ColonyPos entry() {
        return shaft.entry();
    }

    /** Os quatro ramais, na ordem em que os rumos giram. */
    public List<MineArm> arms() {
        return Collections.unmodifiableList(arms);
    }

    /** As células planejadas do nível atual, somando os quatro ramais — ADR-025. */
    public java.util.Set<ColonyPos> plannedCells() {
        java.util.Set<ColonyPos> cells = new java.util.HashSet<>();

        for (MineArm arm : arms) {
            cells.addAll(arm.shaft().plannedCells());
        }

        return cells;
    }

    /** O ramal de índice {@code i}. */
    public MineArm arm(int index) {
        return arms.get(index);
    }

    /**
     * Um ramal que ainda aceita picareta, se houver.
     *
     * <p>Quem escolhe qual mineiro fica com qual é o {@code MineClaims};
     * aqui só se diz que existe frente livre.
     */
    public Optional<Integer> firstArmStillOpen() {
        for (int index = 0; index < arms.size(); index++) {
            if (!arms.get(index).isDone()) {
                return Optional.of(index);
            }
        }

        return Optional.empty();
    }

    /**
     * Quantos ramais podem ser repartidos agora — 2026-09-04.
     *
     * <p><b>Um, enquanto o poço não estiver aberto.</b> Os índices abaixo
     * de {@link MineShaft#CARVED} são a escada e as duas salas, e eles
     * apontam para as <b>mesmas</b> posições em todos os ramais — é o que
     * faz deles ramais da mesma escada. Repartir antes disso é entregar o
     * mesmo bloco a dois mineiros, que é exatamente o defeito de
     * 2026-08-26 que a reserva veio consertar: os dois andam para o mesmo
     * lugar, os dois escrevem {@code could not reach the stone} no mesmo
     * tique, e o recuo do cursor roda duas vezes por um bloco.
     *
     * <p>O primeiro mineiro abre o poço sozinho, portanto, e os outros
     * três entram quando a galeria começa — que é onde os rumos passam a
     * divergir. O gametest
     * {@code twoMinersGetTwoBranchesAndNeverTheSameBlock} é quem afirma
     * isso, e foi ele quem pegou o defeito.
     */
    public int branchesOpenNow() {
        return !arms.get(0).isDone() && arms.get(0).cut() >= MineShaft.SHARED_BLOCKS
                ? ARMS
                : 1;
    }

    /** Se os quatro ramais deste nível acabaram. */
    public boolean everyArmIsDone() {
        return arms.stream().allMatch(MineArm::isDone);
    }

    /**
     * Se todos os ramais entregáveis neste momento terminaram.
     *
     * <p>Antes de {@link MineShaft#SHARED_BLOCKS}, só o ramal zero é entregável:
     * os outros três ainda são a mesma escada. Se o zero fecha nesse
     * trecho, esperar pelos outros prende a mina entre "não posso
     * entregar" e "não posso descer".
     */
    private boolean everyOpenArmIsDone() {
        int open = branchesOpenNow();

        for (int index = 0; index < open; index++) {
            if (!arms.get(index).isDone()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Fechados os quatro ramais, a mina desce um nível.
     *
     * <p><b>É a regra de antes, e ela não mudou de conteúdo.</b> Quatro
     * curvas fechavam o nível e a galeria descia; agora são quatro ramais
     * fechados. O que mudou é que os quatro podem ser fechados por
     * quatro aldeões ao mesmo tempo, em vez de um só, quatro vezes.
     *
     * <p>Por que isso importa: a sessão de 2026-09-02 trabalhou em
     * {@code y=44}, e o pico do diamante em 1.21 é {@code y=-59}. Uma
     * mina que não desce não tem como achar minério melhor.
     *
     * <p>Os cursores voltam a zero porque o poço do nível novo ainda não
     * foi cavado — são duas descidas e duas salas antes de a galeria
     * começar.
     *
     * @return se desceu agora
     */
    public LevelAdvance advanceIfEveryArmIsDone() {
        if (!everyArmIsDone()) {
            return LevelAdvance.WAITING;
        }

        if (!shaft.mayDeepen()) {
            return LevelAdvance.EXHAUSTED;
        }

        shaft = shaft.deepened();

        MineShaft heading = shaft;

        for (MineArm arm : arms) {
            arm.restartAt(heading);

            heading = heading.turned();
        }

        return LevelAdvance.DEEPENED;
    }

    /**
     * Desce quando tudo que a reserva pode entregar agora terminou.
     *
     * <p>No poço partilhado, os ramais ainda fechados são só outras
     * vistas da mesma escada. Encerrá-los junto evita o limbo em que só
     * o ramal zero está aberto, ele acabou, e os outros ainda não podem
     * receber mineiro.
     */
    public LevelAdvance advanceIfEveryOpenArmIsDone() {
        if (!everyOpenArmIsDone()) {
            return LevelAdvance.WAITING;
        }

        if (!everyArmIsDone()) {
            for (MineArm arm : arms) {
                arm.finish();
            }
        }

        return advanceIfEveryArmIsDone();
    }

    /** Compatibilidade para as chamadas que só precisam saber se a mina desceu. */
    public boolean deepenIfEveryArmIsDone() {
        return advanceIfEveryArmIsDone() == LevelAdvance.DEEPENED;
    }

    /** Compatibilidade para as chamadas que só precisam saber se a mina desceu. */
    public boolean deepenIfEveryOpenArmIsDone() {
        return advanceIfEveryOpenArmIsDone() == LevelAdvance.DEEPENED;
    }

    /** Resultado explícito do fim de um nível. */
    public enum LevelAdvance {
        WAITING,
        DEEPENED,
        EXHAUSTED
    }

    /**
     * Quantas hélices a mina tenta antes de culpar a boca — E45.
     *
     * <p>Quatro, que é o número de rumos que {@link MineShaft#rerouted()}
     * percorre antes de voltar ao primeiro: girar uma quinta vez seria
     * reofertar a escada que já falhou. Esgotadas as quatro, o
     * impedimento não é o rumo — é a boca.
     */
    public static final int HELICES_BEFORE_BLAMING_THE_MOUTH = MineShaft.HELIX_FLIGHTS;

    /**
     * Quantas voltas sem picareta bastam para desconfiar do desenho.
     *
     * <p>Três, e o número é folgado de propósito. Uma volta sem picareta
     * é normal — o ramal pode ter acabado num vão, e o
     * {@code BLOCKED_BEFORE_TURNING} já absorve oito recusas antes de
     * fechar. Três voltas <b>seguidas</b> sem um bloco sair do mundo é
     * outra coisa: é o desenho que não se cava.
     *
     * <p>O playtest de 2026-09-16 passaria deste limite em menos de um
     * segundo, contra os 16.657 segundos que ele de fato girou.
     */
    public static final int TURNS_BEFORE_REROUTING = 3;

    /**
     * O ramal fechou e nenhuma pedra saiu — E45, 2026-09-16.
     *
     * <p>Chamado por quem fecha o ramal, uma vez por passagem. Ver
     * {@link #turnsWithoutAPickaxe} para por que a conta mora aqui.
     *
     * @return se esta foi a volta que encheu a conta
     */
    public boolean turnedWithoutAPickaxe() {
        return ++turnsWithoutAPickaxe >= TURNS_BEFORE_REROUTING;
    }

    /**
     * A picareta tirou um bloco: a mina está progredindo.
     *
     * <p>É o <b>único</b> jeito de zerar a conta, e é o ponto inteiro do
     * E45 — ver {@link #turnsWithoutAPickaxe}.
     */
    public void pickaxeTook() {
        turnsWithoutAPickaxe = 0;
    }

    /** Quantas voltas seguidas sem picareta a mina acumulou. */
    public int turnsWithoutAPickaxe() {
        return turnsWithoutAPickaxe;
    }

    /**
     * Gira a hélice sem descer, e recomeça os ramais nela — E45.
     *
     * <p><b>A saída da boca intransponível</b>, e a decisão do autor em
     * 2026-09-16: girar primeiro, porque é barato e reusa
     * {@link MineShaft#rerouted()}, que já existe e já é usada quando a
     * mina chega ao fundo. Se as quatro hélices falharem, o impedimento
     * é a boca, e aí {@link #mouthIsHopeless()} responde.
     *
     * <p>Zera a conta de voltas: o desenho é outro agora, e cobrar da
     * hélice nova o que a anterior não cavou faria a mina desistir da
     * boca sem ter tentado de verdade. Quem conta as hélices é
     * {@link #helicesTried}.
     */
    public void reroute() {
        shaft = shaft.rerouted();

        helicesTried++;

        turnsWithoutAPickaxe = 0;

        MineShaft heading = shaft;

        for (MineArm arm : arms) {
            arm.restartAt(heading);

            heading = heading.turned();
        }
    }

    /**
     * Se as quatro hélices já falharam e a boca é que está ruim — E45.
     *
     * <p>Quem pergunta é quem pode trocar a boca: a decisão de abandonar
     * o poço iniciado não é da mina, é de quem a plantou no mundo.
     */
    public boolean mouthIsHopeless() {
        return helicesTried >= HELICES_BEFORE_BLAMING_THE_MOUTH;
    }

    /** Quantas hélices esta mina já tentou neste nível. */
    public int helicesTried() {
        return helicesTried;
    }

    /**
     * Se o arco da boca já foi erguido alguma vez — 2026-09-11.
     *
     * <p>Quem põe o arco pergunta isto antes, e <b>não</b> pergunta ao
     * mundo: o mundo não sabe a diferença entre pedra que nunca subiu e
     * pedra que o jogador derrubou. Ver {@link #archRaised}.
     */
    public boolean archRaised() {
        return archRaised;
    }

    /**
     * Marca o arco como erguido, de uma vez por todas.
     *
     * <p>Chamado na passagem em que o arco sobe, e não na que o confere:
     * marcar sem ter tentado faria uma boca em chunk descarregado perder
     * o arco para sempre — é o mesmo motivo por que {@code furnish} sai
     * mudo quando o chunk não está carregado.
     */
    public void archIsUp() {
        this.archRaised = true;
    }

    /** A fronteira de cada ramal, na ordem, para o disco. */
    public int[] cuts() {
        int[] cuts = new int[arms.size()];

        for (int index = 0; index < arms.size(); index++) {
            cuts[index] = arms.get(index).cut();
        }

        return cuts;
    }
}
