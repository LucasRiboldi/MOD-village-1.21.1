package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** O nível da mina em rede — ADR-042. */
class MineNetworkTest {

    private static final ColonyPos HUB = new ColonyPos(100, 30, 200);

    @Test
    void aNewLevelOpensTheFourNeighboursOfTheHall() {
        MineNetwork network = MineNetwork.open(HUB);

        assertEquals(4, network.census().get(MineNetwork.State.OPEN));
        assertEquals(1, network.census().get(MineNetwork.State.DONE));
        assertFalse(network.exhausted());
    }

    @Test
    void eachMinerHoldsADifferentNodeTheNearestToHim() {
        MineNetwork network = MineNetwork.open(HUB);
        UUID one = UUID.randomUUID();
        UUID two = UUID.randomUUID();

        MineNetwork.Node east = network.claim(one, new ColonyPos(HUB.x() + 10, HUB.y(), HUB.z())).orElseThrow();
        MineNetwork.Node other = network.claim(two, new ColonyPos(HUB.x() + 10, HUB.y(), HUB.z())).orElseThrow();

        assertEquals(1, east.gx(), "o mais perto de quem está a leste é o nó a leste");
        assertTrue(other.gx() != 1 || other.gz() != 0, "dois mineiros no mesmo nó");
        assertEquals(east, network.claim(one, HUB).orElseThrow(), "quem segura um nó continua nele");
    }

    @Test
    void theTunnelRunsFromTheParentToTheNodeOneWideAndTwoHigh() {
        MineNetwork network = MineNetwork.open(HUB);
        MineNetwork.Node east = new MineNetwork.Node(1, 0, 0, 0);
        List<ColonyPos> cells = network.corridorOf(east);
        int far = MineNetwork.SPACING;

        assertEquals(MineNetwork.SPACING * MineNetwork.WIDTH * MineNetwork.HEIGHT, cells.size());
        assertEquals(new ColonyPos(HUB.x() + 1, HUB.y(), HUB.z()), cells.getFirst(),
                "o piso de andar ao lado do salão sai primeiro");
        assertTrue(cells.contains(new ColonyPos(HUB.x() + far, HUB.y() + 1, HUB.z())), "vai até o centro do nó");
        assertTrue(cells.stream().allMatch(c -> c.y() >= HUB.y() && c.y() < HUB.y() + MineNetwork.HEIGHT),
                "o túnel é plano: nada abaixo do piso");
        assertTrue(cells.stream().allMatch(c -> c.z() == HUB.z()), "um bloco de largura");
    }

    @Test
    void aFinishedNodeOpensItsNeighboursUpToTheRadius() {
        MineNetwork network = MineNetwork.open(HUB);
        UUID miner = UUID.randomUUID();

        // Abre a linha leste até a borda.
        for (int gx = 1; gx <= MineNetwork.RADIUS; gx++) {
            MineNetwork.Node node = network.claim(miner, network.centerOf(gx, 0)).orElseThrow();
            assertEquals(gx, node.gx());
            network.done(node);
        }

        assertTrue(network.stateOf(MineNetwork.RADIUS, 1).isPresent(), "o vizinho de lado abriu");
        assertTrue(network.stateOf(MineNetwork.RADIUS + 1, 0).isEmpty(), "passou do raio");
    }

    @Test
    void theLevelIsExhaustedWhenEveryNodeIsDoneOrSealed() {
        MineNetwork network = MineNetwork.open(HUB);
        UUID miner = UUID.randomUUID();
        int guard = 0;

        while (!network.exhausted() && guard++ < 1000) {
            MineNetwork.Node node = network.claim(miner, HUB).orElseThrow();

            if (node.gx() == 2) {
                network.seal(node);
            } else {
                network.done(node);
            }
        }

        assertTrue(network.exhausted());
        assertTrue(network.census().get(MineNetwork.State.DONE) > 20, "o nível cobriu uma zona grande");
    }

    @Test
    void threeFailuresSealANode() {
        MineNetwork network = MineNetwork.open(HUB);
        MineNetwork.Node node = network.claim(UUID.randomUUID(), HUB).orElseThrow();

        assertFalse(network.failed(node));
        assertFalse(network.failed(node));
        assertTrue(network.failed(node));
        assertEquals(MineNetwork.State.SEALED, network.stateOf(node.gx(), node.gz()).orElseThrow());
    }

    @Test
    void theRouteGoesOnlyThroughFinishedNodes() {
        MineNetwork network = MineNetwork.open(HUB);
        UUID miner = UUID.randomUUID();
        MineNetwork.Node one = network.claim(miner, network.centerOf(1, 0)).orElseThrow();
        network.done(one);
        MineNetwork.Node two = network.claim(miner, network.centerOf(2, 0)).orElseThrow();
        network.done(two);
        MineNetwork.Node three = network.claim(miner, network.centerOf(3, 0)).orElseThrow();

        List<ColonyPos> route = network.routeTo(HUB, three);

        assertEquals(List.of(network.centerOf(0, 0), network.centerOf(1, 0), network.centerOf(2, 0)), route);
        assertTrue(network.routeTo(HUB, new MineNetwork.Node(0, 2, 0, 1)).isEmpty(),
                "sem pai pronto, não há rota");
    }

    @Test
    void theLevelSurvivesTheSave() {
        MineNetwork network = MineNetwork.open(HUB);
        UUID miner = UUID.randomUUID();
        network.done(network.claim(miner, network.centerOf(1, 0)).orElseThrow());
        network.claim(miner, HUB);
        network.shaftIsSecured();
        network.widen();

        MineNetwork back = MineNetwork.restore(HUB, network.toArray());

        assertTrue(back.shaftSecured());
        assertEquals(network.radius(), back.radius());
        assertEquals(MineNetwork.State.DONE, back.stateOf(1, 0).orElseThrow());
        assertEquals(0, back.census().get(MineNetwork.State.DIGGING), "o nó em obra volta aberto");
        assertEquals(MineNetwork.open(HUB).census(), MineNetwork.restore(HUB, new int[] {1, 2}).census(),
                "vetor estragado volta como nível novo");
    }

    @Test
    void theBottomWidensTheLevelWithANewRing() {
        MineNetwork network = MineNetwork.open(HUB);
        UUID miner = UUID.randomUUID();

        while (!network.exhausted()) {
            network.done(network.claim(miner, HUB).orElseThrow());
        }

        network.widen();

        assertFalse(network.exhausted(), "o anel novo abriu caminhos laterais");
        assertTrue(network.stateOf(MineNetwork.RADIUS + 1, 0).isPresent());
    }

    @Test
    void theCellsOfTheLevelAreTheCorridorsInWorkOrDone() {
        MineNetwork network = MineNetwork.open(HUB);
        MineNetwork.Node node = network.claim(UUID.randomUUID(), HUB).orElseThrow();
        Set<ColonyPos> cells = network.cells();

        assertEquals(Set.copyOf(network.corridorOf(node)), cells);
    }
}
