package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Regra 357 (ADR-063, fase 3): o Zelador executa o plano de alturas, e um aldeão anda a pé, com degrau de no máximo 1, do
 * alto de um paredão de 3 até a porta da casa embaixo.
 */
public class GradeWorkGameTest implements FabricGameTest {

    private static final int SIDE = 24;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "grade_work_access", tickLimit = 60)
    public void afterThePlanEveryDoorIsReachedOnFoot(TestContext context) {
        ServerWorld world = context.getWorld();
        UUID colonyId = UUID.randomUUID();
        UUID houseId = UUID.randomUUID();
        // Longe da arena: a barreira que a cerca seria o topo das colunas.
        BlockPos base = context.getAbsolutePos(BlockPos.ORIGIN).add(2_700_000, 20, 2_700_000);

        force(world, base, true);

        for (int x = 0; x < SIDE; x++) {
            for (int z = 0; z < SIDE; z++) {
                world.setBlockState(base.add(x, 0, z), Blocks.STONE.getDefaultState());
                world.setBlockState(base.add(x, 1, z), Blocks.GRASS_BLOCK.getDefaultState());
                for (int y = 2; y <= 4 && x >= 14; y++) {
                    world.setBlockState(base.add(x, y, z), y == 4 ? Blocks.GRASS_BLOCK.getDefaultState()
                            : Blocks.DIRT.getDefaultState());
                }
            }
        }

        for (int y = 2; y <= 4; y++) {
            for (int x = 4; x <= 8; x++) {
                world.setBlockState(base.add(x, y, 4), Blocks.SANDSTONE.getDefaultState());
                world.setBlockState(base.add(x, y, 6), Blocks.SANDSTONE.getDefaultState());
            }
            world.setBlockState(base.add(4, y, 5), Blocks.SANDSTONE.getDefaultState());
            world.setBlockState(base.add(8, y, 5), Blocks.SANDSTONE.getDefaultState());
        }
        world.setBlockState(base.add(6, 2, 6), Blocks.OAK_DOOR.getDefaultState().with(DoorBlock.FACING, Direction.NORTH)
                .with(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        world.setBlockState(base.add(6, 3, 6), Blocks.OAK_DOOR.getDefaultState().with(DoorBlock.FACING, Direction.NORTH)
                .with(DoorBlock.HALF, DoubleBlockHalf.UPPER));

        BlockPos min = base.add(2, 0, 2);
        BlockPos max = base.add(21, 4, 21);
        Colony colony = Colony.create(colonyId, MinecraftTypeAdapter.toColonyPos(base.add(12, 1, 12)));
        colony.restoreBounds(new VillageBounds(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ()));
        VillageColonyMod.BUILDINGS.register(new Building(houseId, colonyId, ResourceId.vanilla("house"),
                MinecraftTypeAdapter.toColonyPos(base.add(4, 1, 4)), MinecraftTypeAdapter.toColonyPos(base.add(8, 4, 6))));

        context.waitAndRun(2, () -> {
            try {
                BlockPos top = base.add(20, 0, 12);
                BlockPos door = base.add(6, 0, 7);

                context.assertFalse(walks(world, base, top, door), "o paredão já dava passagem: o teste não mede nada");

                int done = 0;

                for (int round = 0; round < 40; round++) {
                    done += GradeWork.apply(world, colony, 64);
                }

                context.assertTrue(walks(world, base, top, door),
                        "Regra 357: depois do plano a porta não é alcançada a pé (" + done + " colunas mexidas)");
            } catch (RuntimeException failed) {
                VillageColonyMod.LOGGER.error("VC_TEST_FAIL GradeWorkGameTest: {}", failed.getMessage());
                throw failed;
            } finally {
                VillageColonyMod.BUILDINGS.remove(houseId);
                GradeWork.forget(colonyId);
                force(world, base, false);
            }

            context.complete();
        });
    }

    /** Se dá para ir a pé de uma coluna à outra, com degrau de no máximo 1 e fora da pegada da casa. */
    private static boolean walks(ServerWorld world, BlockPos base, BlockPos from, BlockPos to) {
        Set<Long> seen = new HashSet<>();
        Deque<int[]> open = new ArrayDeque<>();

        open.add(new int[] {from.getX(), from.getZ()});
        seen.add(BlockPos.asLong(from.getX(), 0, from.getZ()));

        while (!open.isEmpty()) {
            int[] at = open.poll();

            if (at[0] == to.getX() && at[1] == to.getZ()) {
                return true;
            }

            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, at[0], at[1]);

            for (Direction side : Direction.Type.HORIZONTAL) {
                int x = at[0] + side.getOffsetX();
                int z = at[1] + side.getOffsetZ();
                boolean house = x >= base.getX() + 4 && x <= base.getX() + 8 && z >= base.getZ() + 4 && z <= base.getZ() + 6;

                if (x < base.getX() || z < base.getZ() || x >= base.getX() + SIDE || z >= base.getZ() + SIDE || house
                        || Math.abs(world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - y) > 1
                        || !seen.add(BlockPos.asLong(x, 0, z))) {
                    continue;
                }

                open.add(new int[] {x, z});
            }
        }

        return false;
    }

    private static void force(ServerWorld world, BlockPos base, boolean forced) {
        for (int cx = (base.getX() >> 4) - 1; cx <= ((base.getX() + SIDE) >> 4) + 1; cx++) {
            for (int cz = (base.getZ() >> 4) - 1; cz <= ((base.getZ() + SIDE) >> 4) + 1; cz++) {
                world.setChunkForced(cx, cz, forced);
            }
        }
    }
}
