package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Uma varredura de lote pela metade — 2026-08-27.
 *
 * <p><b>A medição que pediu isto.</b> Sessão das 20:22, colônia
 * {@code 56c5b68d}: <i>14 passes over 14336 columns, 1 restarts (0 by
 * drift), 0 complete rounds</i>. A varredura andou perfeitamente — uma
 * passagem por ciclo, zero deriva de centro — e precisava de dezessete.
 * Faltaram três ciclos, noventa segundos, e as catorze passagens foram
 * para o lixo.
 *
 * <p>O {@link ColonyRoads} não salva esse caso: ele só nasce de uma
 * volta <b>completa</b>, e a sessão que não completa nenhuma grava
 * zero. Uma vila grande podia nunca guardar nada, sessão após sessão,
 * repetindo sempre os mesmos primeiros anéis.
 *
 * <p><b>Por que o que já foi achado vem junto.</b> Retomar no anel 40
 * sem as ruas dos anéis 0 a 39 faria a volta terminar com meia lista e
 * chamá-la de índice completo — um índice que mente sobre ter visto tudo
 * é pior que índice nenhum, porque a colônia passa a perguntar só a ele
 * e nunca mais varre. O cursor sozinho não é meio conserto: é um
 * defeito novo.
 *
 * <p><b>O que ele aceita de envelhecimento.</b> Os anéis já varridos não
 * são reolhados, e entre uma sessão e outra o jogador pode ter mudado o
 * mundo ali. É o mesmo trato que a varredura já fazia dentro de uma
 * sessão — dezessete ciclos são oito minutos e meio —, agora esticado
 * para além do fechar do mundo. Quando a volta completa, a seguinte
 * recomeça do centro, e é aí que o mundo novo entra.
 *
 * @param colonyId de quem é esta varredura
 * @param from o centro de onde estes anéis foram medidos. Centro que
 *     andou demais invalida a medida, e quem lê descarta
 * @param ring o anel em que o orçamento acabou
 * @param column a posição, na casca desse anel, da primeira coluna que
 *     <b>não</b> chegou a ser olhada
 * @param found as colunas calçadas que os anéis já varridos deram. Vazia
 *     é honesto aqui, ao contrário do índice pronto: os anéis de dentro
 *     podem simplesmente não ter rua
 */
public record ColonySweepCursor(
        UUID colonyId, ColonyPos from, int ring, int column, List<Long> found) {

    public ColonySweepCursor {
        Objects.requireNonNull(colonyId, "colonyId");
        Objects.requireNonNull(from, "from");

        if (ring < 0) {
            throw new IllegalArgumentException("Ring before the center: " + ring);
        }

        if (column < 0) {
            throw new IllegalArgumentException("Column before the ring: " + column);
        }

        found = List.copyOf(found);
    }
}
