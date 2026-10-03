package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/** O replantio do lenhador diante de uma obra da colônia. */
public final class TreeReplantingGameTest implements FabricGameTest {

    /**
     * Árvore cortada dentro de uma obra aberta não volta como rebento —
     * regra do autor, 2026-09-30.
     *
     * <p>No espaço escolhido para uma obra não nasce rebento de outra regra.
     * O replantio do lenhador era a segunda porta, ao lado do viveiro do
     * fazendeiro: ele só olhava se o lugar estava livre e se o chão aceitava.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "tree_replanting")
    public void aTreeCutInsideAnOpenBuildIsNotReplanted(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos base = context.getAbsolutePos(new BlockPos(3, 2, 3));

        world.setBlockState(base.down(), Blocks.GRASS_BLOCK.getDefaultState());
        world.setBlockState(base, Blocks.AIR.getDefaultState());

        UUID colony = UUID.randomUUID();
        ConstructionProject project = ConstructionProject.plan(
                colony,
                Blueprint.of(
                        ResourceId.vanilla("village/plains/houses/replant_lot"),
                        List.of(
                                new BlueprintBlock(new ColonyPos(-1, -1, -1), ResourceId.vanilla("oak_planks")),
                                new BlueprintBlock(new ColonyPos(1, 2, 1), ResourceId.vanilla("oak_planks")))),
                MinecraftTypeAdapter.toColonyPos(base));
        VillageColonyMod.CONSTRUCTIONS.register(project);

        try {
            TreeReplanting.replant(world, TreeSpecies.OAK, base);

            context.assertTrue(
                    world.getBlockState(base).isAir(),
                    "o lenhador replantou dentro de uma obra aberta: "
                            + world.getBlockState(base).getBlock());
        } finally {
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colony);
        }

        context.complete();
    }
}
