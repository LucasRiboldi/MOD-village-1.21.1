package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.MineMouth;
import com.villagecolony.fabric.integration.WorldPathTerrain;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Set;
import java.util.UUID;

/**
 * Regra 386 (nota do jogo da 0.4.148, K5 🅱️: "o caminho do arco da mina até a casa do mineiro tem que ter um caminho que
 * una os dois e esse caminho deve ser unido até o grafo mais perto existente"): a leitura das entradas traz o caminho
 * entre a porta da casa do mineiro e a entrada da mina, e as duas pontas buscam a linha de mais longe.
 */
public class MineHomeWayGameTest implements FabricGameTest {

    private static final int SIZE = 40;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_home_way", tickLimit = 20)
    public void theMinerHomeAndTheMineAreJoined(TestContext context) {
        ServerWorld world = context.getWorld();
        // Longe da bateria: a plataforma de 40 e a mina invadiam a arena do pastor ao lado (ShepherdRoundGameTest).
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 96, 1)).add(2_750_000, 0, 2_750_000);

        chunks(world, origin, true);

        for (BlockPos at : BlockPos.iterate(origin, origin.add(SIZE, 0, SIZE))) {
            world.setBlockState(at, Blocks.GRASS_BLOCK.getDefaultState(), 18);
        }

        ColonyFixture fixture = ColonyFixture.colonyAtWorld(origin.add(20, 1, 2));
        Colony colony = fixture.colony();
        // A casa do mineiro: uma caixa de 5 por 5 com a porta no lado sul, virada para a mina.
        BlockPos door = origin.add(7, 1, 24);
        Building house = new Building(UUID.randomUUID(), colony.id(),
                new ResourceId("villagecolony", "colony/plains/" + MineHomePlans.MODEL),
                MinecraftTypeAdapter.toColonyPos(origin.add(5, 1, 20)), MinecraftTypeAdapter.toColonyPos(origin.add(9, 4, 24)),
                true);

        try {
            world.setBlockState(door, Blocks.OAK_DOOR.getDefaultState().with(DoorBlock.FACING, Direction.NORTH), 18);
            world.setBlockState(door.up(), Blocks.OAK_DOOR.getDefaultState().with(DoorBlock.FACING, Direction.NORTH)
                    .with(DoorBlock.HALF, DoubleBlockHalf.UPPER), 18);
            VillageColonyMod.BUILDINGS.register(house);

            BlockPos mouth = origin.add(20, 1, 30);

            VillageColonyMod.MINES.open(colony.id(), com.villagecolony.core.construction.model.MineShaft.from(
                    MinecraftTypeAdapter.toColonyPos(mouth), com.villagecolony.core.type.Side.SOUTH));
            MineMouth.furnish(world, mouth, Direction.SOUTH, false);

            WorldPathTerrain terrain = new WorldPathTerrain(world, colony.id());
            StructureFronts.Reading reading = StructureFronts.read(world, colony, terrain, Set.of());
            Set<ColonyPos> mine = MineGates.fronts(world, colony, terrain);
            ColonyPos gate = mine.stream().findFirst().orElseThrow();

            context.assertTrue(reading.mineWay().contains(gate),
                    "Regra 386: o caminho da casa do mineiro não chega à entrada da mina " + gate + ": " + reading.mineWay());
            context.assertTrue(reading.planned().containsAll(reading.mineWay()),
                    "Regra 386: o caminho da mina não entrou no grafo como planejado");
            context.assertTrue(reading.far().contains(gate), "Regra 386: a entrada da mina não busca a linha de longe");
            context.assertTrue(reading.far().stream().anyMatch(at -> Math.abs(at.x() - door.getX()) <= 1
                    && Math.abs(at.z() - door.getZ()) <= 2 && !at.equals(gate)),
                    "Regra 386: a porta da casa do mineiro não busca a linha de longe: " + reading.far());
        } finally {
            VillageColonyMod.MINES.removeOfColony(colony.id());
            VillageColonyMod.BUILDINGS.removeOfColony(colony.id());
            fixture.cleanUp();

            for (BlockPos at : BlockPos.iterate(origin, origin.add(SIZE, 9, SIZE))) {
                world.setBlockState(at, Blocks.AIR.getDefaultState(), 18);
            }

            chunks(world, origin, false);
        }

        context.complete();
    }

    private static void chunks(ServerWorld world, BlockPos origin, boolean forced) {
        for (int x = origin.getX() >> 4; x <= (origin.getX() + SIZE) >> 4; x++) {
            for (int z = origin.getZ() >> 4; z <= (origin.getZ() + SIZE) >> 4; z++) {
                world.setChunkForced(x, z, forced);
            }
        }
    }
}
