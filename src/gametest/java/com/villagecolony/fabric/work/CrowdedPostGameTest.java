package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/** Regra 130: cinco animais no mesmo poste pedem um curral; quatro ainda não. */
public class CrowdedPostGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "crowded_post", tickLimit = 40)
    public void fiveAnimalsOnOnePostAskForAPen(TestContext context) {
        BlockPos post = new BlockPos(4, 2, 4);
        context.setBlockState(post.down(), Blocks.GRASS_BLOCK);
        context.setBlockState(post, Blocks.OAK_FENCE);
        BlockPos absolute = context.getAbsolutePos(post);
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(absolute));
        LeashKnotEntity knot = LeashKnotEntity.getOrCreate(context.getWorld(), absolute);

        for (int i = 0; i < PenPlans.CROWDED_POST - 1; i++) {
            SheepEntity sheep = context.spawnEntity(EntityType.SHEEP, new BlockPos(2 + i, 2, 2));
            sheep.attachLeash(knot, true);
        }

        context.assertFalse(PenPlans.crowdedPost(context.getWorld(), colony), "quatro no poste ainda não pedem curral");

        SheepEntity fifth = context.spawnEntity(EntityType.SHEEP, new BlockPos(6, 2, 2));
        fifth.attachLeash(knot, true);

        context.assertTrue(PenPlans.crowdedPost(context.getWorld(), colony), "cinco no mesmo poste deviam pedir curral");
        context.killAllEntities();
        context.complete();
    }
}
