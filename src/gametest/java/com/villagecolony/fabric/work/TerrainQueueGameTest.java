package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.Optional;
import java.util.UUID;

/**
 * O terreno da vila arrumado pelos aldeões — Regra 323: a fila por quadrado de 16×16 pega primeiro o buraco e o bloco
 * isolados, que o alisamento de degrau 1 deixava, e quem não tem ofício também arruma quando não há o que carregar.
 */
public class TerrainQueueGameTest implements FabricGameTest {

    private static final int FLOOR = 2;

    private static final int SIDE = 24;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "terrain_queue_hole", tickLimit = 20)
    public void theMasonFillsAnIsolatedHoleBeforeTheBigSteps(TestContext context) {
        Colony colony = flatVillage(context);
        BlockPos hole = new BlockPos(20, FLOOR, 20);

        // Um planalto de 3 blocos: degraus que pedem mais colunas do que cabem numa passada.
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z < SIDE; z++) {
                context.setBlockState(new BlockPos(x, FLOOR, z), Blocks.DIRT);
                context.setBlockState(new BlockPos(x, FLOOR + 1, z), Blocks.DIRT);
                context.setBlockState(new BlockPos(x, FLOOR + 2, z), Blocks.DIRT);
                context.setBlockState(new BlockPos(x, FLOOR + 3, z), Blocks.GRASS_BLOCK);
            }
        }

        context.setBlockState(hole, Blocks.AIR);
        VillagerEntity mason = context.spawnEntity(EntityType.VILLAGER, new BlockPos(14, FLOOR + 1, 4));

        try {
            context.assertTrue(VillageHelpers.helps(context.getWorld(), colony, Optional.of(ProfessionType.MASON), mason) == 0,
                    "Regra 330: o pedreiro ainda nivela");
            GroundKeeper.day(context.getWorld(), colony, mason);

            context.assertTrue(top(context, 20, 20) == context.getAbsolutePos(hole).getY(),
                    "o buraco isolado ficou atrás dos degraus do planalto: topo em " + top(context, 20, 20));
        } finally {
            mason.discard();
            GradeWork.clearAll();
            GroundSmoothing.clearAll();
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "terrain_queue_no_trade", tickLimit = 20)
    public void theVillagerWithoutATradeCutsAnIsolatedBlock(TestContext context) {
        Colony colony = flatVillage(context);
        BlockPos bump = new BlockPos(6, FLOOR + 1, 18);

        context.setBlockState(bump, Blocks.DIRT);
        VillagerEntity idle = context.spawnEntity(EntityType.VILLAGER, new BlockPos(4, FLOOR + 1, 4));

        try {
            context.assertTrue(VillageHelpers.helps(context.getWorld(), colony, Optional.empty(), idle) == 0,
                    "Regra 330: quem não tem ofício ainda nivela");
            int done = GroundKeeper.day(context.getWorld(), colony, idle);

            context.assertTrue(done > 0 && top(context, 6, 18) == context.getAbsolutePos(bump).getY() - 1,
                    "o Zelador deixou o bloco isolado: fez " + done + ", topo em " + top(context, 6, 18));
        } finally {
            idle.discard();
            GradeWork.clearAll();
            GroundSmoothing.clearAll();
        }

        context.complete();
    }

    /** Chão plano de grama de {@value #SIDE}×{@value #SIDE}, com pedra embaixo, e a caixa da vila sobre ele. */
    private static Colony flatVillage(TestContext context) {
        for (int x = 0; x < SIDE; x++) {
            for (int z = 0; z < SIDE; z++) {
                context.setBlockState(new BlockPos(x, 0, z), Blocks.STONE);
                context.setBlockState(new BlockPos(x, 1, z), Blocks.DIRT);
                context.setBlockState(new BlockPos(x, FLOOR, z), Blocks.GRASS_BLOCK);

                for (int y = FLOOR + 1; y <= FLOOR + 8; y++) {
                    context.setBlockState(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }

        BlockPos origin = context.getAbsolutePos(new BlockPos(0, FLOOR, 0));
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(origin));

        colony.measure(new VillageBounds(origin.getX(), origin.getY(), origin.getZ(), origin.getX() + SIDE - 1,
                origin.getY() + 5, origin.getZ() + SIDE - 1));

        return colony;
    }

    private static int top(TestContext context, int x, int z) {
        BlockPos at = context.getAbsolutePos(new BlockPos(x, 0, z));

        return context.getWorld().getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ()) - 1;
    }
}
