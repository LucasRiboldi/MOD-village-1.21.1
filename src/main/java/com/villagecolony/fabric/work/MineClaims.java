package com.villagecolony.fabric.work;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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

    /** A mina de cada colônia, e o mineiro que está dentro dela. */
    private static final Map<UUID, UUID> DIGGERS = new HashMap<>();

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
    static boolean claim(UUID colonyId, UUID workerId) {
        UUID digger = DIGGERS.get(colonyId);

        if (digger != null) {
            return digger.equals(workerId);
        }

        if (workerId.equals(STOOD_ASIDE.get(colonyId))) {
            // Ele acabou de desistir desta mina: a vez é de outro, se
            // houver outro. Uma recusa e só — mineiro sozinho numa vila
            // não pode ser punido para sempre por um poço, e a passagem
            // seguinte devolve a mina a ele.
            STOOD_ASIDE.remove(colonyId);

            return false;
        }

        DIGGERS.put(colonyId, workerId);

        return true;
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

    /** Quem está na mina desta colônia, se alguém está. */
    public static Optional<UUID> diggerIn(UUID colonyId) {
        return Optional.ofNullable(DIGGERS.get(colonyId));
    }

    /** Este mineiro sai da mina em que estiver. */
    static void release(UUID workerId) {
        DIGGERS.values().removeIf(workerId::equals);
    }

    /**
     * Só quem ainda tem trabalho aberto continua dono de mina.
     *
     * <p>A conferência que impede a reserva de vazar. Roda uma vez por
     * ciclo, com as chaves do registro de trabalhos do
     * {@link MinerWork}.
     */
    static void retainOnly(Set<UUID> working) {
        DIGGERS.values().retainAll(working);
        STOOD_ASIDE.values().retainAll(working);
    }

    /** Esvazia o registro. Chamado ao parar o servidor. */
    public static void clearAll() {
        DIGGERS.clear();
        STOOD_ASIDE.clear();
    }
}
