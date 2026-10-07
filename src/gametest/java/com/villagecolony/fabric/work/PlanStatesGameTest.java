package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.integration.StructureBlueprintReader;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.block.enums.StairShape;
import net.minecraft.state.property.Properties;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.Direction;

import java.util.Map;
import java.util.Optional;

/** Eixo, metade e formato saem na posição da planta (ADR-036 19). */
public class PlanStatesGameTest implements FabricGameTest {

    private static BlueprintBlock planned(String block, Map<String, String> states) {
        return new BlueprintBlock(new ColonyPos(1, 0, 1), ResourceId.vanilla(block), false, Optional.empty(), states);
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "plan_states", tickLimit = 20)
    public void thePlacedStateFollowsThePlan(TestContext context) {
        BlockState log = BlockShaping.withPlanStates(
                planned("oak_log", Map.of("axis", "x")), Blocks.OAK_LOG.getDefaultState());
        BlockState slab = BlockShaping.withPlanStates(
                planned("oak_slab", Map.of("type", "top")), Blocks.OAK_SLAB.getDefaultState());
        BlockState stairs = BlockShaping.withPlanStates(
                planned("sandstone_stairs", Map.of("half", "top", "shape", "outer_left")),
                Blocks.COBBLESTONE_STAIRS.getDefaultState());
        BlockState stone = BlockShaping.withPlanStates(
                planned("stone", Map.of("axis", "x")), Blocks.STONE.getDefaultState());

        context.assertTrue(log.get(Properties.AXIS) == Direction.Axis.X, "a tora deitada saiu em pé");
        context.assertTrue(slab.get(Properties.SLAB_TYPE) == SlabType.TOP, "a laje de cima saiu embaixo");
        context.assertTrue(stairs.get(Properties.BLOCK_HALF) == BlockHalf.TOP
                        && stairs.get(Properties.STAIR_SHAPE) == StairShape.OUTER_LEFT,
                "a escada de pedregulho no lugar da de arenito perdeu metade ou formato");
        context.assertTrue(stone == Blocks.STONE.getDefaultState(), "pedra não tem eixo e mudou de estado");
        context.complete();
    }

    /** O telhado da casa pequena de planície tem escada de canto: a leitura não pode perder o formato. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "plan_states", tickLimit = 20)
    public void aVanillaHouseKeepsItsStairShapesAndLogAxes(TestContext context) {
        Optional<Blueprint> house = StructureBlueprintReader.read(
                context.getWorld(), ResourceId.vanilla("village/plains/houses/plains_small_house_1"));

        context.assertTrue(house.isPresent(), "a casa do jogo não foi lida");
        context.assertTrue(house.get().blocks().stream()
                        .anyMatch(block -> "outer_left".equals(block.states().get("shape"))),
                "nenhuma escada de canto na planta: o formato se perdeu na leitura");
        context.assertTrue(house.get().blocks().stream()
                        .filter(block -> block.block().path().endsWith("_log"))
                        .allMatch(block -> block.states().containsKey("axis")),
                "tronco sem eixo na planta");
        context.complete();
    }
}
