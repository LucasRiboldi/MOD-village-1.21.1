package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.Side;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A parede da porta na planta, e o caminho do mineiro pelos lances da escada — Regras 17 e 119. */
@DisplayName("BlueprintDoorSide")
class BlueprintDoorSideTest {

    private static final ResourceId HOUSE = new ResourceId("villagecolony", "houses/test_door_side");

    private static Blueprint withDoorAt(int x, int z) {
        return Blueprint.of(HOUSE, List.of(
                new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("cobblestone")),
                new BlueprintBlock(new ColonyPos(8, 0, 6), ResourceId.vanilla("cobblestone")),
                new BlueprintBlock(new ColonyPos(x, 1, z), ResourceId.vanilla("oak_door"))));
    }

    @Test
    @DisplayName("a porta fica na parede mais perto dela")
    void theDoorBelongsToTheNearestWall() {
        assertEquals(Optional.of(Side.NORTH), withDoorAt(4, 1).doorSide());
        assertEquals(Optional.of(Side.SOUTH), withDoorAt(4, 5).doorSide());
        assertEquals(Optional.of(Side.WEST), withDoorAt(1, 3).doorSide());
        assertEquals(Optional.of(Side.EAST), withDoorAt(7, 3).doorSide());
    }

    @Test
    @DisplayName("no empate, norte, sul, oeste e leste, nessa ordem")
    void aTieIsAlwaysResolvedTheSameWay() {
        assertEquals(Optional.of(Side.NORTH), withDoorAt(4, 3).doorSide());
        assertEquals(Optional.of(Side.NORTH), withDoorAt(0, 0).doorSide());
        assertEquals(Optional.of(Side.SOUTH), withDoorAt(0, 6).doorSide());
        assertEquals(Optional.of(Side.NORTH), withDoorAt(8, 0).doorSide());
        assertEquals(Optional.of(Side.SOUTH), withDoorAt(2, 4).doorSide());
        assertEquals(Optional.of(Side.EAST), withDoorAt(6, 3).doorSide());
    }

    @Test
    @DisplayName("sem porta, nenhum lado")
    void noDoorNoSide() {
        Blueprint blueprint = Blueprint.of(HOUSE, List.of(new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("oak_planks"))));

        assertEquals(Optional.empty(), blueprint.doorSide());
    }

    @Test
    @DisplayName("os pontos da escada só descem, em trechos curtos, até o salão do ramal")
    void theWaypointsWalkDownInShortLegs() {
        for (Side descent : Side.values()) {
            MineShaft shaft = MineShaft.from(new ColonyPos(100, 64, -200), descent);
            List<ColonyPos> path = shaft.waypoints();

            assertTrue(path.size() >= 6, "faltam pontos em " + descent + ": " + path);
            assertTrue(path.getFirst().y() < 64, "o primeiro lance já desceu em " + descent + ": " + path);
            assertTrue(path.getLast().y() < path.getFirst().y(), "o caminho não chegou embaixo em " + descent + ": " + path);

            for (int leg = 1; leg < path.size(); leg++) {
                ColonyPos from = path.get(leg - 1);
                ColonyPos to = path.get(leg);

                assertTrue(to.y() <= from.y(), "o caminho subiu em " + descent + ": " + path);
                assertTrue(Math.abs(to.x() - from.x()) + Math.abs(to.z() - from.z()) <= 2 * MineShaft.HELIX_SIDE * MineShaft.STAIR_LANES,
                        "trecho longo demais para a navegação em " + descent + ": " + from + " " + to);
            }
        }
    }

    @Test
    @DisplayName("os pontos da mesma escada são os mesmos, deslocados com a boca")
    void theWaypointsMoveWithTheMouth() {
        List<ColonyPos> here = MineShaft.from(new ColonyPos(0, 64, 0), Side.NORTH).waypoints();
        List<ColonyPos> there = MineShaft.from(new ColonyPos(10, 54, -20), Side.NORTH).waypoints();

        assertEquals(here.size(), there.size());

        for (int point = 0; point < here.size(); point++) {
            assertEquals(new ColonyPos(here.get(point).x() + 10, here.get(point).y() - 10, here.get(point).z() - 20), there.get(point));
        }
    }
}
