package com.villagecolony.fabric.integration;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.lang.reflect.Method;
import java.util.UUID;

/** Regressoes para a escolha do lote da BigHouseMOD. */
public class BigHouseFoundationGameTest implements FabricGameTest {

    /** Uma fundacao nao pode ser apoiada sobre o telhado de uma construcao. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "big_house_foundation")
    public void theFoundationDoesNotStartOnTopOfAnExistingBuilding(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(3, 2, 3));
        Vec3i size = new Vec3i(3, 4, 3);

        // O lote fica imediatamente acima de uma construcao existente. A
        // versao defeituosa ignorava esta camada e aceitava o lote.
        for (int dx = 0; dx < size.getX(); dx++) {
            for (int dz = 0; dz < size.getZ(); dz++) {
                world.setBlockState(
                        origin.add(dx, -1, dz), Blocks.OAK_PLANKS.getDefaultState());
            }
        }

        Colony colony = Colony.create(
                UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(origin));

        context.assertFalse(
                invokesSafe(world, colony, origin, size),
                "a BigHouseMOD foi aceita sobre o topo de uma construcao existente");
        context.complete();
    }

    private static boolean invokesSafe(
            ServerWorld world, Colony colony, BlockPos origin, Vec3i size) {
        try {
            Method safe = BigHouseFoundation.class.getDeclaredMethod(
                    "safe",
                    ServerWorld.class,
                    Colony.class,
                    int.class,
                    int.class,
                    int.class,
                    Vec3i.class);
            safe.setAccessible(true);
            return (boolean) safe.invoke(
                    null,
                    world,
                    colony,
                    origin.getX(),
                    origin.getY(),
                    origin.getZ(),
                    size);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("nao foi possivel testar a regra do lote", exception);
        }
    }
}
