package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O índice de ruas, e a distância até a mais próxima — E46, 2026-09-16.
 *
 * <p>É esta conta que responde se uma obra ainda é da vila. Ela mora no
 * core, e não em quem varre, justamente para ser afirmável sem servidor.
 */
class ColonyRoadsTest {

    private static final UUID COLONY = UUID.randomUUID();

    private static final ColonyPos CENTRE = new ColonyPos(0, 64, 0);

    private static ColonyRoads roadsAt(long... columns) {
        return new ColonyRoads(
                COLONY,
                CENTRE,
                java.util.Arrays.stream(columns).boxed().toList());
    }

    /** Ida e volta do empacotamento, inclusive com coordenada negativa. */
    @Test
    void aColumnSurvivesBeingPackedAndUnpacked() {
        long column = ColonyRoads.column(2456, -2936);

        assertEquals(2456, ColonyRoads.xOf(column));
        assertEquals(-2936, ColonyRoads.zOf(column));
    }

    /**
     * Sem rua nenhuma a resposta é <b>vazio</b>, e não "infinitamente
     * longe".
     *
     * <p>A diferença é o defeito que o E46 veio tirar: não saber onde
     * estão as ruas não é o mesmo que não haver nenhuma, e quem confunde
     * as duas coisas larga obra por ignorância.
     */
    @Test
    void anEmptyIndexAnswersNothingRatherThanFarAway() {
        assertTrue(roadsAt().blocksToTheNearestRoad(new ColonyPos(10, 64, 10)).isEmpty());
    }

    /** A obra encostada na rua está a um bloco dela. */
    @Test
    void theWorkBesideTheRoadIsOneBlockAway() {
        ColonyRoads roads = roadsAt(ColonyRoads.column(100, 100));

        assertEquals(
                OptionalInt.of(1),
                roads.blocksToTheNearestRoad(new ColonyPos(101, 64, 100)));
    }

    /** Entre várias, vence a mais próxima — e não a primeira da lista. */
    @Test
    void theNearestRoadWinsAndNotTheFirstOne() {
        ColonyRoads roads = roadsAt(
                ColonyRoads.column(500, 500),
                ColonyRoads.column(10, 10),
                ColonyRoads.column(300, 300));

        assertEquals(
                OptionalInt.of(2),
                roads.blocksToTheNearestRoad(new ColonyPos(12, 64, 10)));
    }

    /**
     * A régua é o quadrado, como no resto do projeto.
     *
     * <p>Na diagonal, {@code (3,3)} dista <b>3</b>, e não 4,24. É a mesma
     * conta dos anéis do {@code BuildSiteScanner} e do
     * {@code withinTheFarmersReach} — ver o C1 do E46, que registra o
     * círculo como o forasteiro entre três réguas.
     */
    @Test
    void theRulerIsTheSquareAndNotTheStraightLine() {
        ColonyRoads roads = roadsAt(ColonyRoads.column(0, 0));

        assertEquals(
                OptionalInt.of(3),
                roads.blocksToTheNearestRoad(new ColonyPos(3, 64, 3)));
    }

    /**
     * A altura não conta.
     *
     * <p>Mesma razão do alcance da obra: uma vila em encosta não fica
     * mais longe de si mesma, e o y da rua é lido do mundo na hora.
     */
    @Test
    void heightDoesNotCount() {
        ColonyRoads roads = roadsAt(ColonyRoads.column(100, 100));

        assertEquals(
                OptionalInt.of(0),
                roads.blocksToTheNearestRoad(new ColonyPos(100, 200, 100)));
    }

    // --- rebasedTo: o índice sobrevive ao centro que anda ---

    /**
     * <b>Rua não deixa de existir porque o centro se mudou.</b>
     *
     * <p>O caso do playtest de 2026-09-17: a colônia foi de 37 para 69
     * camas — a sonda descobriu quase o dobro da vila — e o centro andou
     * 40 blocos para o meio verdadeiro. O movimento estava <b>certo</b>;
     * errado era jogar o índice fora junto com o cursor.
     */
    @Test
    void theIndexSurvivesTheCentreMoving() {
        ColonyRoads roads = roadsAt(
                ColonyRoads.column(10, 10),
                ColonyRoads.column(20, 20));

        Optional<ColonyRoads> kept =
                roads.rebasedTo(new ColonyPos(15, 64, 15), 64);

        assertTrue(kept.isPresent(), "perdeu o índice inteiro com as duas ruas dentro do raio");

        assertEquals(2, kept.get().columns().size());

        assertEquals(
                new ColonyPos(15, 64, 15),
                kept.get().from(),
                "o índice tinha de passar a dizer que foi medido do centro novo");
    }

    /**
     * A coluna que ficou fora do raio novo sai.
     *
     * <p>Guardá-la seria servir lote de fora do alcance, que é o E46.
     */
    @Test
    void whatFellOutsideTheNewRadiusIsDropped() {
        ColonyRoads roads = roadsAt(
                ColonyRoads.column(0, 0),
                ColonyRoads.column(500, 500));

        Optional<ColonyRoads> kept = roads.rebasedTo(new ColonyPos(0, 64, 0), 64);

        assertTrue(kept.isPresent());

        assertEquals(1, kept.get().columns().size(), "a coluna a 500 blocos tinha de sair");
    }

    /**
     * Nenhuma rua sobrevivendo devolve vazio, e não um índice vazio.
     *
     * <p>Vazio quer dizer "varra de novo", que é a resposta certa quando
     * o centro se mudou para longe de tudo o que se conhecia. Um índice
     * com zero colunas mentiria dizendo que já se olhou.
     */
    @Test
    void anIndexWithNothingLeftIsEmptyAndNotHollow() {
        ColonyRoads roads = roadsAt(ColonyRoads.column(0, 0));

        assertTrue(roads.rebasedTo(new ColonyPos(500, 64, 500), 64).isEmpty());
    }
}
