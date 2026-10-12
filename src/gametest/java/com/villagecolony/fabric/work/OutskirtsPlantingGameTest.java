package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * O lenhador sem tarefa planta muda fora da borda da vila, a até 10 blocos dela — regra
 * do autor, 2026-10-12.
 */
public class OutskirtsPlantingGameTest implements FabricGameTest {

    private static final int SIDE = 30;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "outskirts_planting", tickLimit = 20)
    public void anIdleLumberjackPlantsOutsideTheVillage(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos min = context.getAbsolutePos(new BlockPos(0, 1, 0));
        BlockPos max = context.getAbsolutePos(new BlockPos(SIDE - 1, 1, SIDE - 1));
        BlockPos boxMin = context.getAbsolutePos(new BlockPos(10, 2, 10));
        BlockPos boxMax = context.getAbsolutePos(new BlockPos(19, 6, 19));
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(boxMin));
        ColonyFixture owned = ColonyFixture.create().owning(colony);
        UUID lumberjack = UUID.randomUUID();

        colony.measure(new VillageBounds(boxMin.getX(), boxMin.getY(), boxMin.getZ(),
                boxMax.getX(), boxMax.getY(), boxMax.getZ()));
        VillageColonyMod.COLONIES.register(colony);
        VillageColonyMod.WORKERS.register(lumberjack, colony.id()).assign(ProfessionType.LUMBERJACK);
        owned.owning(lumberjack);

        try {
            for (BlockPos at : BlockPos.iterate(min, max)) {
                world.setBlockState(at, Blocks.GRASS_BLOCK.getDefaultState());
            }

            int planted = 0;

            for (int round = 0; round < 5; round++) {
                planted += OutskirtsPlanting.plantForIdleLumberjacks(world, colony);
            }

            context.assertTrue(planted > 0, "o lenhador ocioso não plantou muda nenhuma");

            for (BlockPos at : BlockPos.iterate(min.up(), max.up())) {
                if (!world.getBlockState(at).isIn(BlockTags.SAPLINGS)) {
                    continue;
                }

                boolean inside = at.getX() >= boxMin.getX() && at.getX() <= boxMax.getX()
                        && at.getZ() >= boxMin.getZ() && at.getZ() <= boxMax.getZ();
                int out = Math.max(Math.max(boxMin.getX() - at.getX(), at.getX() - boxMax.getX()),
                        Math.max(boxMin.getZ() - at.getZ(), at.getZ() - boxMax.getZ()));

                context.assertTrue(!inside, "muda plantada dentro da vila em " + at.toShortString());
                context.assertTrue(out <= OutskirtsPlanting.BAND, "muda a " + out + " blocos da borda");
            }
        } finally {
            for (BlockPos at : BlockPos.iterate(min, max.up())) {
                world.setBlockState(at, Blocks.AIR.getDefaultState());
            }

            owned.cleanUp();
        }

        context.complete();
    }
}
