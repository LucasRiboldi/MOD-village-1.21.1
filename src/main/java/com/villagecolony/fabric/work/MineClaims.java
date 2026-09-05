package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.Mine;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

/**
 * Quem está dentro da mina — 2026-08-28.
 *
 * <p><b>A sessão de 2026-08-26, 23:23:08.</b> A colônia tinha dois
 * mineiros, e os dois cavavam a mesma escada. Havia reserva, e ela era
 * da <b>tarefa</b>: o {@code ColonyCycle} abre uma tarefa por recurso
 * pedido, pedregulho e carvão são dois, e cada mineiro pegou a sua. Nada
 * na cadeia falava da mina.
 *
 * <p>E a mina é uma coisa só. O cursor da galeria mora no {@code Mine},
 * a ordem de cavar sai dele, e os dois recebiam <b>a mesma posição na
 * mesma passagem</b>: andavam para o mesmo bloco, um chegava, e os dois
 * escreviam {@code could not reach the stone} no mesmo tique. Pior que
 * trabalho perdido — esse aviso recua o cursor, e ele recuava duas vezes
 * por um bloco.
 *
 * <p>É o problema que o {@link TreeClaims} resolveu do lado do
 * lenhador, e a resposta é a mesma: <b>a coisa disputada é que tem
 * dono</b>. Ali é a árvore, aqui é a mina. A diferença é o que se
 * guarda: árvore é tronco e há muitas, mina é uma por colônia, então a
 * chave é a colônia e o valor é o mineiro.
 *
 * <p><b>E o valor virou quatro mineiros em 2026-09-04</b> — decisão do
 * autor: <i>"mineiros distintos escolhem caminhos de perfuração
 * distintos dentro das minas"</i>. A coisa disputada deixou de ser a
 * mina e passou a ser o <b>ramal</b>: a {@code Mine} tem
 * {@link com.villagecolony.core.construction.model.Mine#ARMS} frentes,
 * cada uma com o cursor dela, e duas frentes diferentes não se
 * atropelam. O raciocínio de cima continua inteiro — dois na
 * <b>mesma</b> frente ainda recuariam o cursor duas vezes por um bloco;
 * o que mudou é que agora há mais de uma frente para repartir.
 *
 * <p><b>A reserva não vaza, e é por construção.</b> Nem todo fim de
 * trabalho passa pelo mesmo lugar — morte, zumbificação, dispensa,
 * tarefa devolvida pelo guarda de travamento —, e uma mina trancada por
 * um aldeão que não existe mais é pior que o defeito que ela conserta.
 * Por isso {@link #retainOnly} roda a cada ciclo com os mineiros que
 * ainda têm trabalho aberto: quem sumiu perde a mina sem precisar
 * avisar.
 *
 * <p><b>O que ela não faz</b> é reservar a pedra de superfície. Aquela
 * busca é por mineiro — cada um tem o seu cursor de espiral —, e dois
 * raspando afloramentos diferentes não se atrapalham. O que é um só é a
 * escada.
 */
public final class MineClaims {

    /**
     * Os ramais de cada colônia, e o mineiro que está em cada um.
     *
     * <p>Um vetor por colônia, indexado pelo número do ramal, com
     * {@code null} onde não há ninguém. Vetor e não mapa porque o índice
     * <b>é</b> a identidade do ramal: ele diz para que lado a galeria
     * daquele mineiro anda.
     */
    private static final Map<UUID, UUID[]> DIGGERS = new HashMap<>();

    /**
     * Quem acabou de desistir de cada mina, e é recusado uma vez.
     *
     * <p><b>A sessão de 2026-08-29, 04:40.</b> Um mineiro ficou preso num
     * poço a dois blocos abaixo da passarela — oito leituras seguidas em
     * {@code 757, 42, 877}, sem andar um bloco em seis minutos, porque
     * aldeão não sobe dois. E ele <b>segurava a mina</b>: o guarda de
     * travamento devolvia a tarefa, ele a pegava de volta, e o segundo
     * mineiro passou a sessão em {@code waiting for the shaft}. A colônia
     * não recebeu uma pedra.
     *
     * <p>A reserva sozinha não responde isso. Ela impede dois na mesma
     * escada, e não dizia nada sobre <b>rodar a vez</b> quando o de
     * dentro não consegue trabalhar.
     */
    private static final Map<UUID, UUID> STOOD_ASIDE = new HashMap<>();

    private MineClaims() {
    }

    /**
     * Se este mineiro pode cavar a mina desta colônia agora.
     *
     * <p>Pergunta e reserva na mesma passagem: mina livre fica sendo
     * dele, e mina com dono só responde sim para o dono. É perguntado
     * uma vez por pedra, e não uma vez por mina — uma reserva que não se
     * renovasse expulsaria o dono na segunda pedra dele.
     */
    static OptionalInt claimArm(UUID colonyId, UUID workerId, int usable) {
        UUID[] taken = DIGGERS.computeIfAbsent(colonyId, id -> new UUID[Mine.ARMS]);

        for (int index = 0; index < taken.length; index++) {
            if (workerId.equals(taken[index])) {
                // Já é dele, e continua sendo: um mineiro que trocasse de
                // ramal a cada passagem deixaria quatro túneis pela metade
                // em vez de abrir um.
                return OptionalInt.of(index);
            }
        }

        if (workerId.equals(STOOD_ASIDE.get(colonyId))) {
            // Ele acabou de desistir desta mina: a vez é de outro, se
            // houver outro. Uma recusa e só — mineiro sozinho numa vila
            // não pode ser punido para sempre por um poço, e a passagem
            // seguinte devolve a mina a ele.
            STOOD_ASIDE.remove(colonyId);

            return OptionalInt.empty();
        }

        // <b>Só os ramais que já podem ser repartidos</b>: enquanto o
        // poço não está aberto, o único é o primeiro. Ver
        // Mine.branchesOpenNow.
        for (int index = 0; index < Math.min(usable, taken.length); index++) {
            if (taken[index] == null) {
                taken[index] = workerId;

                return OptionalInt.of(index);
            }
        }

        return OptionalInt.empty();
    }

    /**
     * Qual ramal já é deste mineiro, sem reservar nenhum.
     *
     * <p>Par do {@link #claimArm}, e existe pelo mesmo motivo que o
     * {@link #heldByOther}: aquele responde e toma na mesma passagem, e
     * quem só quer saber por onde o aldeão volta tiraria a última frente
     * livre de quem ia cavar nela.
     */
    static OptionalInt armAlreadyHeld(UUID colonyId, UUID workerId) {
        UUID[] taken = DIGGERS.get(colonyId);

        if (taken == null) {
            return OptionalInt.empty();
        }

        for (int index = 0; index < taken.length; index++) {
            if (workerId.equals(taken[index])) {
                return OptionalInt.of(index);
            }
        }

        return OptionalInt.empty();
    }

    /** Este mineiro larga o ramal em que estiver desta colônia. */
    static void releaseArm(UUID colonyId, UUID workerId) {
        UUID[] taken = DIGGERS.get(colonyId);

        if (taken == null) {
            return;
        }

        for (int index = 0; index < taken.length; index++) {
            if (workerId.equals(taken[index])) {
                taken[index] = null;
            }
        }
    }

    /**
     * Este mineiro desistiu da mina, e cede a vez.
     *
     * <p>Chamado pelo guarda de travamento, e não pelo fim normal do
     * trabalho: quem termina a pedra continua sendo o dono da escada.
     */
    static void stepAside(UUID colonyId, UUID workerId) {
        release(workerId);

        STOOD_ASIDE.put(colonyId, workerId);
    }

    /**
     * Se a mina desta colônia está com <b>outro</b> mineiro.
     *
     * <p>Pergunta sem reservar, que é o que {@link #claim} não sabe
     * fazer: claim responde e toma na mesma passagem, e quem só quer
     * saber se vale a pena tentar não pode pagar esse efeito.
     *
     * <p>Existe por causa do impasse de 2026-09-04. O orçamento de
     * buscas do tique é um para a colônia inteira, e quem chega primeiro
     * o gasta — inclusive quem vai ser recusado no portão da escada sem
     * varrer nada. O dono ficava sem passagem, e é a passagem dele que
     * solta a mina quando ele não acha pedra. Ver
     * {@code MinerWork.startNextStone}.
     */
    static boolean heldByOther(UUID colonyId, UUID workerId, int usable) {
        UUID[] taken = DIGGERS.get(colonyId);

        if (taken == null) {
            return false;
        }

        boolean full = true;

        // <b>Só os ramais que já podem ser repartidos contam</b> —
        // 2026-09-04, e a falta disto reabriu o impasse do mesmo dia por
        // uma passagem. Enquanto o poço é rocha, o único ramal
        // repartível é o primeiro: olhar os quatro compartimentos fazia o
        // vetor parecer com vaga, o barrado deixava de ser barrado, e ele
        // voltava a gastar a busca do tique antes de quem cava — para ser
        // recusado logo depois no portão. Ver Mine.branchesOpenNow.
        for (int index = 0; index < Math.min(usable, taken.length); index++) {
            if (workerId.equals(taken[index])) {
                return false;
            }

            full &= taken[index] != null;
        }

        // E quem já tem ramal fora da faixa repartível continua com ele.
        for (int index = Math.min(usable, taken.length); index < taken.length; index++) {
            if (workerId.equals(taken[index])) {
                return false;
            }
        }

        return full;
    }

    /**
     * Alguém que está na mina desta colônia e não é este mineiro.
     *
     * <p>É o que o relatório precisa dizer a quem ficou de fora: com
     * quatro ramais, quem espera espera porque <b>todos</b> estão
     * ocupados, e nomear um deles basta para o log ter onde começar.
     */
    public static Optional<UUID> otherDiggerIn(UUID colonyId, UUID workerId) {
        UUID[] taken = DIGGERS.get(colonyId);

        if (taken == null) {
            return Optional.empty();
        }

        for (UUID digger : taken) {
            if (digger != null && !digger.equals(workerId)) {
                return Optional.of(digger);
            }
        }

        return Optional.empty();
    }

    /**
     * Alguém que está em algum ramal desta colônia, se alguém está.
     *
     * <p>Sobreviveu à troca de 2026-09-04 porque a pergunta sobreviveu:
     * <i>a mina está em uso?</i>. Qual dos quatro ramais é quem responde
     * já não importa a quem pergunta isso.
     */
    public static Optional<UUID> diggerIn(UUID colonyId) {
        UUID[] taken = DIGGERS.get(colonyId);

        if (taken == null) {
            return Optional.empty();
        }

        return Arrays.stream(taken).filter(digger -> digger != null).findFirst();
    }

    /** Quantos mineiros estão nos ramais desta colônia. */
    public static int diggersIn(UUID colonyId) {
        UUID[] taken = DIGGERS.get(colonyId);

        if (taken == null) {
            return 0;
        }

        return (int) Arrays.stream(taken).filter(digger -> digger != null).count();
    }

    /** Este mineiro sai do ramal em que estiver, em qualquer colônia. */
    static void release(UUID workerId) {
        for (UUID[] taken : DIGGERS.values()) {
            for (int index = 0; index < taken.length; index++) {
                if (workerId.equals(taken[index])) {
                    taken[index] = null;
                }
            }
        }
    }

    /**
     * Só quem ainda tem trabalho aberto continua dono de mina.
     *
     * <p>A conferência que impede a reserva de vazar. Roda uma vez por
     * ciclo, com as chaves do registro de trabalhos do
     * {@link MinerWork}.
     */
    static void retainOnly(Set<UUID> working) {
        for (UUID[] taken : DIGGERS.values()) {
            for (int index = 0; index < taken.length; index++) {
                if (taken[index] != null && !working.contains(taken[index])) {
                    taken[index] = null;
                }
            }
        }

        STOOD_ASIDE.values().retainAll(working);
    }

    /** Esvazia o registro. Chamado ao parar o servidor. */
    public static void clearAll() {
        DIGGERS.clear();
        STOOD_ASIDE.clear();
    }
}
