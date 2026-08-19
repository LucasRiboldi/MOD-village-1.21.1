package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.Side;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A cabana da colônia — a Regra 17 e a Regra 20.
 *
 * <p>A planta é escrita em código, e não lida do jogo, justamente para
 * poder ser somada bloco a bloco fora dele. É o que estes casos fazem.
 */
@DisplayName("ColonyHut")
class ColonyHutTest {

    private static final ResourceId OAK = new ResourceId(ResourceId.VANILLA, "oak_planks");

    private static final ResourceId OAK_DOOR = new ResourceId(ResourceId.VANILLA, "oak_door");

    @Test
    @DisplayName("a porta fica na parede do lado que a colônia pediu")
    void theDoorwayFollowsTheSideItWasAskedFor() {
        assertEquals(new ColonyPos(2, 0, 0), doorOf(Side.NORTH), "porta ao norte");
        assertEquals(new ColonyPos(2, 0, 4), doorOf(Side.SOUTH), "porta ao sul");
        assertEquals(new ColonyPos(0, 0, 2), doorOf(Side.WEST), "porta a oeste");
        assertEquals(new ColonyPos(4, 0, 2), doorOf(Side.EAST), "porta a leste");
    }

    @Test
    @DisplayName("o vão da porta é aberto na parede, e não fica emparedado")
    void theDoorwayIsAHoleInTheWall() {
        for (Side side : Side.values()) {
            Blueprint plan = ColonyHut.blueprint(OAK, side);

            ColonyPos door = doorOf(side);

            // O bloco acima da porta é o resto do vão: se houvesse
            // parede ali, a porta de duas metades não caberia.
            ColonyPos above = new ColonyPos(door.x(), 1, door.z());

            assertFalse(
                    hasBlockAt(plan, above),
                    "o vão da porta ao " + side + " ficou emparedado em cima");
        }
    }

    @Test
    @DisplayName("a madeira da casa é a que a colônia mandou usar")
    void theWoodIsTheOneTheColonyAskedFor() {
        ResourceId spruce = new ResourceId(ResourceId.VANILLA, "spruce_planks");
        ResourceId spruceDoor = new ResourceId(ResourceId.VANILLA, "spruce_door");

        Blueprint plan = ColonyHut.blueprint(spruce, Side.NORTH);

        for (BlueprintBlock block : plan.blocks()) {
            assertTrue(
                    block.block().equals(spruce) || block.block().equals(spruceDoor),
                    "a cabana de pinheiro pediu " + block.block());
        }
    }

    @Test
    @DisplayName("mudar o lado não muda o tamanho da conta")
    void theBillOfMaterialsDoesNotChangeWithTheSide() {
        int north = ColonyHut.blueprint(OAK, Side.NORTH).blocks().size();

        for (Side side : Side.values()) {
            assertEquals(
                    north,
                    ColonyHut.blueprint(OAK, side).blocks().size(),
                    "a cabana ao " + side + " tem outro tamanho");
        }
    }

    private static ColonyPos doorOf(Side side) {
        List<ColonyPos> doors = ColonyHut.blueprint(OAK, side).blocks().stream()
                .filter(block -> block.block().path().endsWith("_door"))
                .map(BlueprintBlock::offset)
                .toList();

        assertEquals(1, doors.size(), "a cabana ao " + side + " tem " + doors.size() + " portas");

        return doors.get(0);
    }

    private static boolean hasBlockAt(Blueprint plan, ColonyPos offset) {
        return plan.blocks().stream().anyMatch(block -> block.offset().equals(offset));
    }
}
