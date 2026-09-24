package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
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

    /**
     * A quantos blocos daqui está a rua mais próxima — E46, 2026-09-16.
     *
     * <p>Em quadrado, que é a régua do resto do projeto: o
     * {@code BuildSiteScanner} varre em anéis, o
     * {@code ConstructionDemand.withinTheFarmersReach} mede assim, e o
     * {@code VillageDetector} também. Ver o C1 do E46 — o círculo era o
     * forasteiro entre três réguas.
     *
     * <p>Horizontal, pelo mesmo motivo que o alcance da obra: a altura
     * da rua é lida do mundo, e uma vila em encosta não fica mais longe
     * de si mesma por isso.
     *
     * <p>Índice vazio devolve vazio, e não "infinitamente longe": não
     * saber onde estão as ruas não é o mesmo que não haver nenhuma, e
     * quem pergunta precisa distinguir os dois casos para não largar uma
     * obra por ignorância.
     *
     * @return a distância em quadrado até a rua mais próxima, ou vazio
     *     se esta colônia ainda não tem índice de ruas
     */
    public OptionalInt blocksToTheNearestRoad(ColonyPos at) {
        Objects.requireNonNull(at, "at");

        int nearest = Integer.MAX_VALUE;

        for (long column : columns) {
            int distance = Math.max(
                    Math.abs(xOf(column) - at.x()),
                    Math.abs(zOf(column) - at.z()));

            if (distance < nearest) {
                nearest = distance;
            }
        }

        return nearest == Integer.MAX_VALUE ? OptionalInt.empty() : OptionalInt.of(nearest);
    }

    /**
     * O mesmo índice, medido de um centro novo — 2026-09-17.
     *
     * <p><b>Rua não deixa de existir porque o centro se mudou.</b> Até
     * aqui, deriva de centro descartava o índice inteiro junto com o
     * cursor da varredura, e o preço está medido: no playtest de 09-17 a
     * colônia respondeu <b>16 de 32</b> consultas pelo índice — as de
     * antes do movimento — e depois voltou a varrer o quadrado do zero,
     * sem nunca mais fechar uma volta.
     *
     * <p><b>Por que o cursor cai e o índice não.</b> O cursor é
     * {@code (anel, coluna)} <b>relativo</b> ao centro: mover o centro o
     * torna sem sentido, e a aritmética mostra que transladá-lo não
     * recupera nada — com deriva de 40 e anel visto de 27, o anel seguro
     * no centro novo é zero. Já o índice guarda <b>colunas absolutas do
     * mundo</b>, e elas continuam sendo rua.
     *
     * <p>O que muda é o alcance: coluna que ficou fora do raio do centro
     * novo sai, porque servir lote de fora do raio é o E46. Em quadrado,
     * que é a régua do scanner.
     *
     * @param centre o centro de agora
     * @param radius o raio da vila
     * @return o índice reancorado, ou vazio se nenhuma rua sobreviveu —
     *     e aí varrer de novo é mesmo a resposta certa
     */
    public Optional<ColonyRoads> rebasedTo(ColonyPos centre, int radius) {
        Objects.requireNonNull(centre, "centre");

        List<Long> kept = new ArrayList<>();

        for (long column : columns) {
            int square = Math.max(
                    Math.abs(xOf(column) - centre.x()),
                    Math.abs(zOf(column) - centre.z()));

            if (square <= radius) {
                kept.add(column);
            }
        }

        return kept.isEmpty()
                ? Optional.empty()
                : Optional.of(new ColonyRoads(colonyId, centre, kept));
    }
}
