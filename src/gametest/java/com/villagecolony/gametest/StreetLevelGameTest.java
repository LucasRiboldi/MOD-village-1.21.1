package com.villagecolony.gametest;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import com.villagecolony.fabric.work.BuriedPieces;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/**
 * A casa na altura da rua — sessão de jogo de 2026-09-26: a
 * {@code plains_small_house_5} saiu sobre um monte de terra, piso em 67, porta
 * em 68, com a rua em 63-65. Pedido do autor: sem camadas de terra na base, chão
 * e porta na altura da rua.
 */
public class StreetLevelGameTest implements FabricGameTest {

    private static final List<String> PLAINS_HOUSES = List.of(
            "plains_small_house_1", "plains_small_house_2", "plains_small_house_3",
            "plains_small_house_4", "plains_small_house_5", "plains_small_house_6",
            "plains_small_house_7", "plains_small_house_8", "plains_medium_house_1",
            "plains_medium_house_2", "plains_big_house_1", "plains_shepherds_house_1",
            "plains_temple_3", "plains_temple_4", "plains_library_1", "plains_fisher_cottage_1");

    private static Blueprint read(TestContext context, String house) {
        return StructureBlueprintReader.read(context.getWorld(),
                ResourceId.vanilla("village/plains/houses/" + house)).orElseThrow();
    }

    /**
     * A camada da rua de cada casa, medida no NBT do jogo em 2026-09-26: uma
     * abaixo da porta mais baixa. O templo 4 tem a porta na camada 0 e não tem
     * camada da rua dentro da planta — ele fica como sempre foi.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "street_level", tickLimit = 20)
    public void eachPlainsHouseHasItsStreetOneBelowItsDoor(TestContext context) {
        java.util.Map<String, Integer> expected = java.util.Map.of(
                "plains_small_house_1", 0, "plains_small_house_4", 0,
                "plains_small_house_5", 1, "plains_medium_house_1", 1,
                "plains_shepherds_house_1", 0, "plains_big_house_1", 0);

        StringBuilder wrong = new StringBuilder();

        expected.forEach((house, layer) -> {
            Blueprint plan = read(context, house);

            if (plan.streetLayer() != layer) {
                wrong.append(house).append(" rua em ").append(plan.streetLayer())
                        .append(", devia ser ").append(layer).append("; ");
            }
        });

        context.assertTrue(wrong.length() == 0, wrong.toString());
        context.assertFalse(read(context, "plains_temple_4").hasStreetLayer(),
                "o templo 4 tem a porta na camada 0: não há camada da rua dentro dele");

        for (String house : PLAINS_HOUSES) {
            Blueprint plan = read(context, house);

            plan.blocks().stream()
                    .filter(block -> block.block().path().endsWith("_door"))
                    .mapToInt(block -> block.offset().y())
                    .min()
                    .ifPresent(door -> context.assertTrue(
                            !plan.hasStreetLayer() || door == plan.streetLayer() + 1,
                            house + ": porta em " + door + ", rua em " + plan.streetLayer()));
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "street_level", tickLimit = 20)
    public void theHouseOfTheSessionHasItsStreetOnLayerOne(TestContext context) {
        context.assertTrue(read(context, "plains_small_house_5").streetLayer() == 1,
                "a casa 5 devia ter a rua na camada 1 (a 0 é terra enterrada)");
        context.assertTrue(read(context, "plains_small_house_1").streetLayer() == 0,
                "a casa 1 devia ter a rua na camada 0 (ela é o piso)");
        context.assertFalse(StructureBlueprintReader.read(context.getWorld(),
                        StructureBlueprintReader.BIG_HOUSE_MOD).orElseThrow().hasStreetLayer(),
                "a BigHouseMOD não tem encaixe de rua e não pode mudar de altura");
        context.complete();
    }

    /**
     * A obra aberta num lote de grama: a porta fica um acima do chão, e nenhuma
     * terra — nem a fundação da camada 0, nem a do quintal na camada da rua —
     * sobra para construir.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "street_level_lot", tickLimit = 40)
    public void aHouseOpenedOnGrassHasItsDoorAtStreetHeightAndNoDirtToBuild(TestContext context) {
        Blueprint plan = read(context, "plains_small_house_5");

        BlockPos corner = context.getAbsolutePos(new BlockPos(0, 1, 0)).add(0, 0, 40);
        int ground = corner.getY() + 1;

        for (int x = 0; x < plan.size().x(); x++) {
            for (int z = 0; z < plan.size().z(); z++) {
                context.getWorld().setBlockState(corner.add(x, 0, z), Blocks.DIRT.getDefaultState());
                context.getWorld().setBlockState(corner.add(x, 1, z), Blocks.GRASS_BLOCK.getDefaultState());
            }
        }

        // O lote responde "piso um acima do chão".
        ColonyPos floor = new ColonyPos(corner.getX(), ground + 1, corner.getZ());
        ConstructionProject project = ConstructionProject.plan(UUID.randomUUID(), plan, plan.originFor(floor));

        int held = BuriedPieces.markHeldByTheGround(context.getWorld(), project);

        int doorY = plan.blocks().stream()
                .filter(block -> block.block().path().endsWith("_door"))
                .mapToInt(block -> project.worldPositionOf(block).y())
                .min().orElseThrow();

        context.assertTrue(doorY == ground + 1,
                "a porta devia ficar em " + (ground + 1) + " (um acima do chão), ficou em " + doorY);
        context.assertTrue(held > 0, "nenhuma peça foi reconhecida como chão");

        for (BlueprintBlock left : project.remaining()) {
            String name = left.block().path();

            context.assertFalse(left.offset().y() < plan.streetLayer(),
                    "sobrou fundação enterrada para construir: " + name + " em " + left.offset());
            context.assertFalse(left.offset().y() == plan.streetLayer()
                            && (name.equals("dirt") || name.equals("grass_block")),
                    "sobrou terra na altura da rua para construir em " + left.offset());
        }

        context.complete();
    }

    /** O piso na altura da rua toma o lugar da grama; um baú ou uma tábua, não. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "street_level", tickLimit = 20)
    public void theStreetFloorReplacesNaturalGroundOnly(TestContext context) {
        Blueprint plan = Blueprint.of(ResourceId.vanilla("test_street_floor"), List.of(
                new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("cobblestone")),
                new BlueprintBlock(new ColonyPos(0, 1, 0), ResourceId.vanilla("oak_planks"))))
                .withStreetLayer(0);

        BlockPos grass = context.getAbsolutePos(new BlockPos(1, 1, 1));
        BlockPos chest = context.getAbsolutePos(new BlockPos(3, 1, 1));
        BlockPos planks = context.getAbsolutePos(new BlockPos(5, 1, 1));

        context.getWorld().setBlockState(grass, Blocks.GRASS_BLOCK.getDefaultState());
        context.getWorld().setBlockState(chest, Blocks.CHEST.getDefaultState());
        context.getWorld().setBlockState(planks, Blocks.OAK_PLANKS.getDefaultState());

        BlueprintBlock floor = plan.blocks().get(0);
        BlueprintBlock above = plan.blocks().get(1);

        context.assertTrue(BuriedPieces.mayReplaceGround(context.getWorld(), plan, floor, grass),
                "o piso da rua não pôde tomar o lugar da grama");
        context.assertFalse(BuriedPieces.mayReplaceGround(context.getWorld(), plan, floor, chest),
                "o piso tomaria o lugar de um baú");
        context.assertFalse(BuriedPieces.mayReplaceGround(context.getWorld(), plan, floor, planks),
                "o piso tomaria o lugar de uma tábua posta por alguém");
        context.assertFalse(BuriedPieces.mayReplaceGround(context.getWorld(), plan, above, grass),
                "só a camada da rua troca terreno; a de cima não");
        context.complete();
    }
}
