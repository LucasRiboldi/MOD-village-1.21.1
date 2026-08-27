package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * As ruas que uma colônia já mediu, e de que centro — 2026-08-27.
 *
 * <p><b>O que este registro existe para atravessar.</b> Achar as ruas
 * custa uma varredura do quadrado de raio 64 — 16.641 colunas, mil por
 * passagem, uma passagem por ciclo. Dezessete ciclos, oito minutos e
 * meio, e só então a colônia sabe onde procurar lote. Enquanto isso
 * morria ao fechar o mundo, toda sessão pagava a conta de novo, e as
 * curtas acabavam antes de chegar ao fim dela — o que é a família do
 * E14: a colônia dizendo "não há lote" com lote existindo.
 *
 * <p>Medido no save do autor: das 16.641 colunas, <b>698</b> eram
 * calçamento. É isso, e só isso, que vale gravar — a resposta cara de
 * uma pergunta cuja fonte (o mundo) continua ali para desmentir cada
 * coluna quando ela for visitada.
 *
 * <p><b>O centro vem junto porque a medida é relativa a ele.</b> Um
 * índice sem o centro de onde foi tirado não sabe dizer se ainda fala do
 * mesmo lugar, e a colônia anda — pouco, mas anda. Quem lê compara, e
 * descarta o que envelheceu.
 *
 * @param colonyId de quem são estas ruas
 * @param from o centro a partir do qual o raio foi varrido
 * @param columns as colunas calçadas, empacotadas por
 *     {@link #column(int, int)}, na ordem em que a varredura as achou —
 *     do centro para fora, que é a ordem em que interessa perguntar
 */
public record ColonyRoads(UUID colonyId, ColonyPos from, List<Long> columns) {

    public ColonyRoads {
        Objects.requireNonNull(colonyId, "colonyId");
        Objects.requireNonNull(from, "from");

        columns = List.copyOf(columns);
    }

    /**
     * Uma coluna do mundo em um número só: x no alto, z embaixo.
     *
     * <p>Mora aqui, e não em quem varre, porque a mesma conta é feita
     * dos dois lados do disco. Duas cópias que discordassem na ordem dos
     * bits devolveriam um índice embaralhado — ruas onde não há, e
     * silêncio onde há.
     *
     * <p>Y fica de fora de propósito: a altura da rua é lida do mundo na
     * hora, porque é ela que o jogador pode ter mudado.
     */
    public static long column(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    /** O x de uma coluna empacotada. */
    public static int xOf(long column) {
        return (int) (column >> 32);
    }

    /** O z de uma coluna empacotada. */
    public static int zOf(long column) {
        return (int) column;
    }
}
