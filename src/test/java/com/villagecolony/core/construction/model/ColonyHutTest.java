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
            if (block.furniture()) {
                // Cama, baú e lampião não têm espécie de madeira — ver a
                // Regra 21. O que esta afirmação cobre é a estrutura.
                continue;
            }

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

    @Test
    @DisplayName("toda casa nasce com cama, baú e lampião dentro")
    void everyHouseHasABedAChestAndALantern() {
        Blueprint plan = ColonyHut.blueprint(OAK, Side.NORTH);

        for (ResourceId piece : List.of(ColonyHut.BED, ColonyHut.CHEST, ColonyHut.LANTERN)) {
            assertTrue(
                    plan.blocks().stream().anyMatch(block -> block.block().equals(piece)),
                    "a casa saiu sem " + piece.path());
        }
    }

    @Test
    @DisplayName("a mobília não segura a obra, e a estrutura sim")
    void furnitureIsMarkedAndStructureIsNot() {
        Blueprint plan = ColonyHut.blueprint(OAK, Side.NORTH);

        for (BlueprintBlock block : plan.blocks()) {
            boolean isFurniture = block.block().equals(ColonyHut.BED)
                    || block.block().equals(ColonyHut.CHEST)
                    || block.block().equals(ColonyHut.LANTERN);

            assertEquals(
                    isFurniture,
                    block.furniture(),
                    block.block().path() + " em " + block.offset() + " está do lado errado"
                            + " da Regra 21");
        }
    }

    @Test
    @DisplayName("a mobília fica no miolo, sem encostar em parede")
    void theFurnitureSitsInsideTheWalls() {
        for (Side side : Side.values()) {
            for (BlueprintBlock block : ColonyHut.blueprint(OAK, side).blocks()) {
                if (!block.furniture()) {
                    continue;
                }

                ColonyPos at = block.offset();

                assertTrue(
                        at.x() > 0 && at.x() < ColonyHut.SIDE - 1
                                && at.z() > 0 && at.z() < ColonyHut.SIDE - 1,
                        "a peça " + block.block().path() + " ficou na parede, em " + at);
            }
        }
    }

    /**
     * A cabeceira da cama precisa caber, e ela vai um bloco ao norte do
     * pé — é o que {@code BuilderWork.placeSecondHalf} faz.
     */
    @Test
    @DisplayName("a cabeceira da cama cabe dentro da casa")
    void theBedHeadFitsIndoors() {
        ColonyPos foot = ColonyHut.blueprint(OAK, Side.NORTH).blocks().stream()
                .filter(block -> block.block().equals(ColonyHut.BED))
                .map(BlueprintBlock::offset)
                .findFirst()
                .orElseThrow();

        assertTrue(
                foot.z() - 1 > 0,
                "a cabeceira da cama cairia dentro da parede: o pé está em " + foot);
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
