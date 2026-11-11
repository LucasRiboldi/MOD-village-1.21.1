package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * O vão entre duas construções de bases diferentes para de mudar — Regras 178 e 242: o anel de cada casa e a rampa do vão
 * não disputam a mesma coluna. No jogo da 0.4.52 o anel cortava o que a rampa aterrava, 32 colunas por passada, sem fim.
 */
public class BuildingGapsGameTest implements FabricGameTest {

    private static final int SIZE = 20;

    /** Quantas passadas bastam para o vão assentar. */
    private static final int PASSES = 12;

    private static void forceChunks(ServerWorld world, BlockPos origin, boolean force) {
        for (int x = origin.getX() >> 4; x <= (origin.getX() + SIZE) >> 4; x++) {
            for (int z = origin.getZ() >> 4; z <= (origin.getZ() + SIZE) >> 4; z++) {
                world.setChunkForced(x, z, force);
            }
        }
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "building_gaps_settle", tickLimit = 20)
    public void theGapBetweenTwoBasesSettles(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 86, 1));
        ColonyFixture fixture = ColonyFixture.colonyAtWorld(origin.add(2, 1, 9));
        List<Building> houses = new ArrayList<>();

        forceChunks(world, origin, true);

        try {
            for (BlockPos at : BlockPos.iterate(origin.add(-3, -2, -3), origin.add(SIZE, -1, SIZE))) {
                world.setBlockState(at, Blocks.DIRT.getDefaultState(), 18);
            }

            for (BlockPos at : BlockPos.iterate(origin.add(-3, 0, -3), origin.add(SIZE, 0, SIZE))) {
                world.setBlockState(at, Blocks.GRASS_BLOCK.getDefaultState(), 18);
            }

            // Duas casas sem porta, uma de frente para a outra num vão de 8 blocos: a de baixo com base 1 acima do chão, a de
            // cima com base 5 acima. O piso de cada uma é a base da caixa.
            houses.add(house(fixture, origin.add(0, 1, 0), origin.add(4, 4, 4)));
            houses.add(house(fixture, origin.add(0, 5, 13), origin.add(4, 8, 17)));
            houses.forEach(VillageColonyMod.BUILDINGS::register);

            int last = -1;

            for (int pass = 0; pass < PASSES; pass++) {
                last = BuildingGaps.pass(world, fixture.colony());
            }

            context.assertTrue(last == 0, "Regras 178 e 242: o vão ainda muda depois de " + PASSES + " passadas: " + last + " colunas");

            // O meio do vão, fora dos anéis, subiu em rampa: a correção não desligou a rampa.
            int middle = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, origin.getX() + 2, origin.getZ() + 8) - 1;

            context.assertTrue(middle > origin.getY(), "Regra 242: o meio do vão não subiu em rampa: " + (middle - origin.getY()));
        } finally {
            houses.forEach(house -> VillageColonyMod.BUILDINGS.remove(house.id()));
            fixture.cleanUp();

            for (BlockPos at : BlockPos.iterate(origin.add(-3, -2, -3), origin.add(SIZE, 8, SIZE))) {
                world.setBlockState(at, Blocks.AIR.getDefaultState(), 18);
            }

            forceChunks(world, origin, false);
        }

        context.complete();
    }

    private static Building house(ColonyFixture fixture, BlockPos min, BlockPos max) {
        return new Building(UUID.randomUUID(), fixture.colony().id(), ResourceId.vanilla("village/desert/houses/desert_small_house_1"),
                MinecraftTypeAdapter.toColonyPos(min), MinecraftTypeAdapter.toColonyPos(max), true);
    }
}
