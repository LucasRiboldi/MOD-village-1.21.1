package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/**
 * Regra 297: o chão em volta da obra nova é suavizado como o da vila gerada, numa janela de
 * {@link SiteSmoothing#WINDOW} blocos — o morro colado no lote vira encosta de degraus de 1, sem parede, e o caminho
 * calçado fica.
 */
public class SiteSurroundingsGameTest implements FabricGameTest {

    /** Até onde o teste confere: a borda da janela fica no chão natural e pode guardar o degrau dele. */
    private static final int CHECKED = SiteSmoothing.WINDOW - 2;

    /** Metade da arena, em volta da obra de 2 × 2. */
    private static final int HALF = SiteSmoothing.WINDOW + 2;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "site_surroundings", tickLimit = 20)
    public void theHillBesideTheBuildBecomesASlopeWithoutAWall(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(BlockPos.ORIGIN).add(4_400_000, 60, 4_400_000);
        int roadY = origin.getY();
        BlockPos path = origin.add(2, 0, 0);

        try {
            // Chão na altura da rua a oeste; a partir de 2 blocos a leste da obra, um morro de 4 (o curral da 0.4.88).
            for (int dx = -HALF; dx <= HALF + 1; dx++) {
                for (int dz = -HALF; dz <= HALF + 1; dz++) {
                    column(world, origin.add(dx, 0, dz), dx >= 3 ? roadY + 4 : roadY);
                }
            }

            world.setBlockState(path.withY(roadY), Blocks.DIRT_PATH.getDefaultState());

            Blueprint house = Blueprint.of(ResourceId.vanilla("surroundings_house"), List.of(
                    new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("cobblestone")),
                    new BlueprintBlock(new ColonyPos(1, 0, 1), ResourceId.vanilla("cobblestone")))).withStreetLayer(0);
            ConstructionProject project = ConstructionProject.plan(UUID.randomUUID(), house,
                    new ColonyPos(origin.getX(), origin.getY(), origin.getZ()));

            SiteSurroundings.level(world, project);

            context.assertTrue(world.getBlockState(path.withY(roadY)).isOf(Blocks.DIRT_PATH),
                    "o caminho calçado não pode ser cortado");
            context.assertTrue(topOf(world, origin.add(3, 0, 0), roadY) <= roadY + 2,
                    "o morro a 2 blocos da obra devia descer a no máximo rua+2, ficou "
                            + (topOf(world, origin.add(3, 0, 0), roadY) - roadY));

            for (int dx = -CHECKED; dx <= CHECKED; dx++) {
                for (int dz = -CHECKED; dz <= CHECKED; dz++) {
                    int here = topOf(world, origin.add(dx, 0, dz), roadY);
                    int east = topOf(world, origin.add(dx + 1, 0, dz), roadY);
                    int south = topOf(world, origin.add(dx, 0, dz + 1), roadY);

                    context.assertTrue(Math.abs(here - east) <= 1 && Math.abs(here - south) <= 1,
                            "parede em volta da obra em " + dx + "," + dz + ": " + (here - roadY) + " ao lado de "
                                    + (east - roadY) + " e " + (south - roadY));
                }
            }
        } finally {
            for (BlockPos at : BlockPos.iterate(origin.add(-HALF, -9, -HALF), origin.add(HALF + 1, 9, HALF + 1))) {
                world.setBlockState(at, Blocks.AIR.getDefaultState(), net.minecraft.block.Block.NOTIFY_LISTENERS);
            }
        }

        context.complete();
    }

    /** Uma coluna de pedra com grama em {@code top} e ar em cima. */
    private static void column(ServerWorld world, BlockPos at, int top) {
        world.getChunk(at);
        for (int y = at.getY() - 8; y <= at.getY() + 8; y++) {
            BlockPos here = new BlockPos(at.getX(), y, at.getZ());
            world.setBlockState(here, y < top ? Blocks.STONE.getDefaultState()
                    : y == top ? Blocks.GRASS_BLOCK.getDefaultState() : Blocks.AIR.getDefaultState(),
                    net.minecraft.block.Block.NOTIFY_LISTENERS);
        }
    }

    /** O topo da coluna: a obra de 2 × 2 em si fica na altura da rua. */
    private static int topOf(ServerWorld world, BlockPos at, int roadY) {
        for (int y = roadY + 8; y >= roadY - 8; y--) {
            if (!world.getBlockState(new BlockPos(at.getX(), y, at.getZ())).isAir()) {
                return y;
            }
        }

        return Integer.MIN_VALUE;
    }
}
