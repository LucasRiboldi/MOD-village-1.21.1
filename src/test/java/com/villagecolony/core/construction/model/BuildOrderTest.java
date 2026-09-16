package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Em que ordem a casa sobe — 2026-09-16.
 *
 * <p>A ordem já era decisão do projeto, e não do arquivo do jogo: de
 * baixo para cima, mobília por último (Regra 32). O que entra agora é a
 * <b>porta</b>, por pedido do autor: <i>"deixar para colocar as portas
 * por último nas construções"</i>.
 *
 * <p><b>O motivo está no código que ele viu falhar.</b> O construtor
 * risca o que não tem apoio — <i>"skips ... nothing holds it"</i> — e a
 * porta é o caso clássico: ela precisa do batente dos dois lados e do
 * chão embaixo. Vindo no meio da parede, ela era tentada antes de a
 * parede existir, e riscada de vez: a casa terminava sem porta.
 *
 * <p>A decisão mora no Core porque é ordem, e ordem se afirma sem mundo.
 * Estava dentro do leitor de estrutura, que precisa de servidor para
 * rodar — e por isso nunca teve teste próprio.
 */
class BuildOrderTest {

    private static final ResourceId PLANKS = ResourceId.vanilla("oak_planks");
    private static final ResourceId DOOR = ResourceId.vanilla("oak_door");
    private static final ResourceId BED = ResourceId.vanilla("red_bed");

    private static BlueprintBlock at(int y, ResourceId block) {
        return new BlueprintBlock(new ColonyPos(0, y, 0), block);
    }

    private static List<BlueprintBlock> sorted(List<BlueprintBlock> blocks) {
        List<BlueprintBlock> copy = new ArrayList<>(blocks);
        copy.sort(BuildOrder.COMPARATOR);

        return copy;
    }

    /**
     * A porta vai depois da parede, mesmo estando embaixo dela.
     *
     * <p>É o caso que o autor viu: a porta fica na base da parede, então
     * a ordem de baixo para cima a colocava primeiro — antes de existir
     * batente para segurá-la.
     */
    @Test
    void theDoorComesAfterTheWallEvenBeingLower() {
        List<BlueprintBlock> order = sorted(List.of(
                at(0, DOOR),
                at(3, PLANKS)));

        assertEquals(
                PLANKS,
                order.get(0).block(),
                "a porta foi colocada antes da parede que a segura");

        assertEquals(DOOR, order.get(1).block());
    }

    /**
     * E antes da mobília, que continua sendo a última.
     *
     * <p>A Regra 32 não muda: a cama precisa da casa inteira para decidir
     * onde fica a cabeceira. A porta entra entre a estrutura e a mobília.
     */
    @Test
    void theDoorComesBeforeTheFurniture() {
        List<BlueprintBlock> order = sorted(List.of(
                new BlueprintBlock(new ColonyPos(0, 0, 0), BED, true),
                at(0, DOOR),
                at(1, PLANKS)));

        assertEquals(PLANKS, order.get(0).block(), "a estrutura não veio primeiro");
        assertEquals(DOOR, order.get(1).block(), "a porta não ficou entre estrutura e mobília");
        assertEquals(BED, order.get(2).block(), "a mobília deixou de ser a última");
    }

    /**
     * Dentro do mesmo grupo, continua de baixo para cima.
     *
     * <p>É o que garante que a parede tenha o que a sustente, e não pode
     * ter sido perdido ao acrescentar o grupo da porta.
     */
    @Test
    void withinAGroupItStillGoesBottomUp() {
        List<BlueprintBlock> order = sorted(List.of(
                at(5, PLANKS),
                at(1, PLANKS),
                at(3, PLANKS)));

        assertEquals(1, order.get(0).offset().y());
        assertEquals(3, order.get(1).offset().y());
        assertEquals(5, order.get(2).offset().y());
    }

    /**
     * A ordem é estável: duas leituras dão a mesma casa.
     *
     * <p>Obra com ordem instável é impossível de depurar — o projeto
     * registrou isso quando a mobília entrou.
     */
    @Test
    void theOrderIsStableBetweenReadings() {
        List<BlueprintBlock> blocks = List.of(
                new BlueprintBlock(new ColonyPos(2, 1, 3), PLANKS),
                new BlueprintBlock(new ColonyPos(1, 1, 3), PLANKS),
                new BlueprintBlock(new ColonyPos(1, 1, 2), PLANKS));

        assertEquals(
                sorted(blocks),
                sorted(blocks),
                "duas leituras da mesma planta deram casas diferentes");
    }

    /** Alçapão e portão contam como porta: dependem do mesmo apoio. */
    @Test
    void trapdoorsAndGatesCountAsDoors() {
        assertTrue(BuildOrder.isDoor(ResourceId.vanilla("oak_trapdoor")));
        assertTrue(BuildOrder.isDoor(ResourceId.vanilla("oak_fence_gate")));
        assertTrue(BuildOrder.isDoor(ResourceId.vanilla("iron_door")));
    }

    /** E o que não é porta não vira porta por ter nome parecido. */
    @Test
    void otherBlocksAreNotDoors() {
        assertTrue(!BuildOrder.isDoor(PLANKS));
        assertTrue(!BuildOrder.isDoor(ResourceId.vanilla("oak_stairs")));
    }
}
