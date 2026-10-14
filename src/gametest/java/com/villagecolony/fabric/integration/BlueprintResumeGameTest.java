package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A obra que volta do save volta no giro em que foi levantada — playtest 0.3.31: a
 * casa com 8 peças por pôr voltou girada pela rua nova, com 292 por pôr, limpou 27
 * blocos dela mesma e pulou 120 "no caminho".
 */
public class BlueprintResumeGameTest implements FabricGameTest {

    private static final ResourceId HOUSE = ResourceId.vanilla("village/plains/houses/plains_small_house_1");

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "blueprint_resume", tickLimit = 40)
    public void aResumedBuildKeepsTheTurnItWasBuiltIn(TestContext context) {
        ServerWorld world = context.getWorld();
        UUID colonyId = UUID.randomUUID();
        ColonyPos origin = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(2, 2, 2)));
        Optional<Blueprint> byRoad = BlueprintPlacement.blueprintOf(world, colonyId, HOUSE, origin);

        context.assertTrue(byRoad.isPresent(), "a planta da casa não foi lida do jogo");

        // Levantada num giro diferente do que a rua de agora daria.
        Blueprint built = byRoad.get().rotated(1);
        List<BlockPos> placed = new ArrayList<>();

        try {
            for (BlueprintBlock block : built.blocks()) {
                Optional<Block> type = MinecraftTypeAdapter.toBlock(block.block());
                BlockPos where = new BlockPos(origin.x() + block.offset().x(), origin.y() + block.offset().y(),
                        origin.z() + block.offset().z());

                if (type.isPresent() && !type.get().getDefaultState().isAir()) {
                    world.setBlockState(where, type.get().getDefaultState(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
                    placed.add(where);
                }
            }

            Blueprint resumed = BlueprintPlacement.resumedBlueprintOf(world, colonyId, HOUSE, origin).orElseThrow();
            int standing = BlueprintPlacement.standing(world, resumed, origin);
            int asBuilt = BlueprintPlacement.standing(world, built, origin);

            context.assertTrue(standing == asBuilt && asBuilt > BlueprintPlacement.standing(world, byRoad.get(), origin),
                    "a obra voltou num giro com " + standing + " peças de pé, e a casa levantada tem " + asBuilt);
        } finally {
            for (BlockPos where : placed) {
                world.setBlockState(where, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
            }
        }

        context.complete();
    }
}
