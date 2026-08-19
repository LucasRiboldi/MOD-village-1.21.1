package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Girar a planta — a Regra 17 aplicada à casa lida do jogo.
 *
 * <p>A cabana do mod é quadrada e resolvia a porta mudando duas
 * coordenadas. A casa do jogo não: a porta está onde o arquivo a pôs, e
 * virá-la para a rua exige girar tudo.
 */
@DisplayName("Blueprint.rotated")
class BlueprintRotationTest {

    private static final ResourceId ID = new ResourceId("villagecolony", "test");

    private static final ResourceId PLANKS = new ResourceId(ResourceId.VANILLA, "oak_planks");

    private static final ResourceId DOOR = new ResourceId(ResourceId.VANILLA, "oak_door");

    @Test
    @DisplayName("meia volta leva a parede do norte para o sul")
    void halfATurnMovesTheNorthWallToTheSouth() {
        Blueprint plan = wall();

        assertEquals(new ColonyPos(1, 0, 0), doorOf(plan), "antes de girar");
        assertEquals(new ColonyPos(1, 0, 2), doorOf(plan.rotated(2)), "meia volta");
    }

    @Test
    @DisplayName("um quarto de volta troca os eixos")
    void aQuarterTurnSwapsTheAxes() {
        Blueprint turned = wall().rotated(1);

        // A porta estava no meio da parede do norte (z=0) e vai para o
        // meio da parede do leste.
        assertEquals(new ColonyPos(2, 0, 1), doorOf(turned));
    }

    @Test
    @DisplayName("quatro quartos de volta devolvem a planta original")
    void fourQuarterTurnsComeBackHome() {
        Blueprint plan = wall();

        assertEquals(doorOf(plan), doorOf(plan.rotated(4)));
        assertEquals(plan.size(), plan.rotated(4).size());
    }

    @Test
    @DisplayName("nada nasce em coordenada negativa")
    void nothingLandsOutsideTheBox() {
        for (int turns = 0; turns < 4; turns++) {
            Blueprint turned = oblong().rotated(turns);

            for (BlueprintBlock block : turned.blocks()) {
                ColonyPos at = block.offset();

                assertTrue(
                        at.x() >= 0 && at.y() >= 0 && at.z() >= 0,
                        "girando " + turns + " a peça foi para " + at);
            }
        }
    }

    @Test
    @DisplayName("a caixa acompanha o giro: 5x3 vira 3x5")
    void theBoxTurnsWithTheBlocks() {
        Blueprint plan = oblong();

        assertEquals(5, plan.size().x());
        assertEquals(3, plan.size().z());

        Blueprint turned = plan.rotated(1);

        assertEquals(3, turned.size().x(), "a largura devia virar profundidade");
        assertEquals(5, turned.size().z(), "a profundidade devia virar largura");
    }

    @Test
    @DisplayName("girar não muda a lista de compras nem a ordem da obra")
    void theBillAndTheOrderSurviveTheTurn() {
        Blueprint plan = oblong();
        Blueprint turned = plan.rotated(3);

        assertEquals(plan.materials(), turned.materials(), "a lista de compras mudou");
        assertEquals(plan.blockCount(), turned.blockCount(), "o número de blocos mudou");

        for (int i = 0; i < plan.blocks().size(); i++) {
            assertEquals(
                    plan.blocks().get(i).block(),
                    turned.blocks().get(i).block(),
                    "a ordem da obra mudou na posição " + i);
        }
    }

    @Test
    @DisplayName("a marca de mobília sobrevive ao giro")
    void theFurnitureMarkSurvivesTheTurn() {
        Blueprint plan = Blueprint.of(ID, List.of(
                new BlueprintBlock(new ColonyPos(0, 0, 0), PLANKS),
                BlueprintBlock.furniture(new ColonyPos(1, 0, 1), DOOR)));

        assertTrue(
                plan.rotated(1).blocks().stream().anyMatch(BlueprintBlock::furniture),
                "a mobília deixou de ser mobília depois de girar");
    }

    /** Três por três, com a porta no meio da parede do norte. */
    private static Blueprint wall() {
        return Blueprint.of(ID, List.of(
                new BlueprintBlock(new ColonyPos(0, 0, 0), PLANKS),
                new BlueprintBlock(new ColonyPos(1, 0, 0), DOOR),
                new BlueprintBlock(new ColonyPos(2, 0, 0), PLANKS),
                new BlueprintBlock(new ColonyPos(0, 0, 2), PLANKS),
                new BlueprintBlock(new ColonyPos(2, 0, 2), PLANKS)));
    }

    /** Cinco por três, para o giro ter o que trocar. */
    private static Blueprint oblong() {
        return Blueprint.of(ID, List.of(
                new BlueprintBlock(new ColonyPos(0, 0, 0), PLANKS),
                new BlueprintBlock(new ColonyPos(4, 0, 0), DOOR),
                new BlueprintBlock(new ColonyPos(0, 1, 2), PLANKS),
                new BlueprintBlock(new ColonyPos(4, 1, 2), PLANKS)));
    }

    private static ColonyPos doorOf(Blueprint plan) {
        return plan.blocks().stream()
                .filter(block -> block.block().equals(DOOR))
                .map(BlueprintBlock::offset)
                .findFirst()
                .orElseThrow();
    }
}
