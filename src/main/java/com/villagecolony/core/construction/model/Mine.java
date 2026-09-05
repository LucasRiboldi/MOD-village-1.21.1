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
        return arms.get(0).cut() >= MineShaft.CARVED ? ARMS : 1;
    }

    /** Se os quatro ramais deste nível acabaram. */
    public boolean everyArmIsDone() {
        return arms.stream().allMatch(MineArm::isDone);
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
    public boolean deepenIfEveryArmIsDone() {
        if (!everyArmIsDone()) {
            return false;
        }

        if (!shaft.mayDeepen()) {
            // Chegou ao fundo. Os ramais reabrem no mesmo nível: é pior
            // recavar do que deixar a colônia sem mineiro para sempre, e
            // o findTheFrontier passa por cima do que já é ar.
            MineShaft heading = shaft;

            for (MineArm arm : arms) {
                arm.restartAt(heading);

                heading = heading.turned();
            }

            return false;
        }

        shaft = shaft.deepened();

        MineShaft heading = shaft;

        for (MineArm arm : arms) {
            arm.restartAt(heading);

            heading = heading.turned();
        }

        return true;
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
