package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.MineMouths;
import com.villagecolony.fabric.integration.UnderPieceFill;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;
import java.util.UUID;

/**
 * Regra 356: o chão que o Zelador mexe não estraga a vila. O nivelamento não corta debaixo de construção nem tapa a boca
 * da mina, o vão sob a construção da colônia é aterrado e a frente da porta fica livre.
 */
public class FallingSandGuardGameTest implements FabricGameTest {

    /** Jogo da 0.4.136: a fazenda em y 70 ficou voando porque o nivelamento cortou a areia debaixo dela. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "sand_guard_building", tickLimit = 20)
    public void levellingNeverCutsUnderABuilding(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos column = context.getAbsolutePos(new BlockPos(2, 0, 2));
        UUID id = UUID.randomUUID();

        context.setBlockState(new BlockPos(2, 0, 2), Blocks.STONE.getDefaultState());
        for (int y = 1; y <= 3; y++) {
            context.setBlockState(new BlockPos(2, y, 2), Blocks.SAND.getDefaultState());
        }

        VillageColonyMod.BUILDINGS.register(new Building(id, UUID.randomUUID(), ResourceId.vanilla("farm"),
                MinecraftTypeAdapter.toColonyPos(column.add(-1, 6, -1)),
                MinecraftTypeAdapter.toColonyPos(column.add(1, 7, 1))));

        try {
            String refusal = GroundSmoothing.refusal(world, column.getX(), column.getZ(), column.getY() + 3,
                    column.getY() + 1);

            context.assertTrue("under_building".equals(refusal),
                    "Regra 356: a coluna debaixo da construção foi aceita para corte: " + refusal);
        } finally {
            VillageColonyMod.BUILDINGS.remove(id);
        }

        context.complete();
    }

    /** Jogo da 0.4.136: o buraco da escada é a coluna mais baixa da volta, e o nivelamento o tapava. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "sand_guard_mine", tickLimit = 20)
    public void levellingKeepsTheMineMouthOpen(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos column = context.getAbsolutePos(new BlockPos(2, 0, 2));
        UUID colony = UUID.randomUUID();

        context.setBlockState(new BlockPos(2, 0, 2), Blocks.STONE.getDefaultState());
        context.setBlockState(new BlockPos(2, 1, 2), Blocks.SAND.getDefaultState());
        MineMouths.remember(colony, column.add(2, 2, 0), Direction.NORTH);
        // A mina desceu por um ramal: a entrada do nível atual fica longe da boca, e só a boca guardada a protege.
        VillageColonyMod.MINES.open(colony, com.villagecolony.core.construction.model.MineShaft.from(
                MinecraftTypeAdapter.toColonyPos(column.add(100, -30, 100)), com.villagecolony.core.type.Side.NORTH));

        try {
            String near = GroundSmoothing.refusal(world, column.getX(), column.getZ(), column.getY() + 1,
                    column.getY() + 2);
            String far = GroundSmoothing.refusal(world, column.getX() + 40, column.getZ(), column.getY() + 1,
                    column.getY() + 2);

            context.assertTrue("mine_mouth".equals(near), "Regra 356: a folga da boca da mina foi aceita: " + near);
            context.assertFalse("mine_mouth".equals(far), "a folga da mina chegou a 40 blocos da boca");

            VillageColonyMod.MINES.removeOfColony(colony);
            String gone = GroundSmoothing.refusal(world, column.getX(), column.getZ(), column.getY() + 1,
                    column.getY() + 2);

            context.assertFalse("mine_mouth".equals(gone), "a boca de mina desfeita ainda protege a coluna");
        } finally {
            MineMouths.forget(colony);
            VillageColonyMod.MINES.removeOfColony(colony);
        }

        context.complete();
    }

    /** Jogo da 0.4.136: a areia na frente da porta do mineiro tirou a entrada da casa do grafo. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "sand_guard_door", tickLimit = 20)
    public void theKeeperClearsTheSandInFrontOfADoor(TestContext context) {
        ServerWorld world = context.getWorld();

        for (int x = 0; x <= 6; x++) {
            for (int z = 0; z <= 7; z++) {
                context.setBlockState(new BlockPos(x, 0, z), Blocks.SAND.getDefaultState());
            }
        }
        // A casa fechada: paredes nas filas z 1 e 3 e nos lados x 1 e 5, com o miolo em z 2.
        for (int y = 1; y <= 3; y++) {
            for (int x = 1; x <= 5; x++) {
                context.setBlockState(new BlockPos(x, y, 1), Blocks.SANDSTONE.getDefaultState());
                context.setBlockState(new BlockPos(x, y, 3), Blocks.SANDSTONE.getDefaultState());
            }
            context.setBlockState(new BlockPos(1, y, 2), Blocks.SANDSTONE.getDefaultState());
            context.setBlockState(new BlockPos(5, y, 2), Blocks.SANDSTONE.getDefaultState());
        }

        // Quem põe a porta olha para dentro (norte); a saída é o sul.
        BlockPos door = new BlockPos(3, 1, 3);
        context.setBlockState(door, Blocks.OAK_DOOR.getDefaultState().with(DoorBlock.FACING, Direction.NORTH)
                .with(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        context.setBlockState(door.up(), Blocks.OAK_DOOR.getDefaultState().with(DoorBlock.FACING, Direction.NORTH)
                .with(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        // A areia caída na frente, na altura da porta e da cabeça.
        context.setBlockState(new BlockPos(3, 1, 4), Blocks.SAND.getDefaultState());
        context.setBlockState(new BlockPos(3, 2, 4), Blocks.SAND.getDefaultState());
        // Ao lado da frente: não é passagem, e fica.
        context.setBlockState(new BlockPos(1, 1, 4), Blocks.SAND.getDefaultState());

        BlockPos min = context.getAbsolutePos(new BlockPos(1, 0, 1));
        BlockPos max = context.getAbsolutePos(new BlockPos(5, 4, 3));

        context.waitAndRun(2, () -> {
            BlockPos outside = context.getAbsolutePos(new BlockPos(3, 0, 4));

            // A entrada já fica na frente da porta tapada (Regra 360); aqui se mede a limpeza.
            context.assertTrue(context.getBlockState(new BlockPos(3, 1, 4)).isOf(Blocks.SAND),
                    "a areia não tapava a porta: o teste não mede nada");

            int cleared = DoorClearing.clear(world, min, max, 16);
            List<BlockPos> fronts = DoorFronts.of(world, min, max);

            context.assertTrue(cleared > 0 && context.getBlockState(new BlockPos(3, 1, 4)).isAir()
                    && context.getBlockState(new BlockPos(3, 2, 4)).isAir(),
                    "Regra 356: a areia da frente da porta ficou (" + cleared + " blocos tirados)");
            context.assertTrue(fronts.contains(outside),
                    "Regra 356: a entrada não voltou para a frente da porta: " + fronts);
            context.assertTrue(context.getBlockState(new BlockPos(1, 1, 4)).isOf(Blocks.SAND),
                    "a limpeza mexeu fora da frente da porta");
            context.complete();
        });
    }

    /** Jogo da 0.4.136: o vão debaixo da fazenda da colônia ficou aberto; a peça da vila já era aterrada (Regra 304). */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "sand_guard_under", tickLimit = 20)
    public void theGapUnderAColonyBuildingIsFilled(TestContext context) {
        ServerWorld world = context.getWorld();
        UUID id = UUID.randomUUID();

        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 4; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.STONE.getDefaultState());
            }
        }
        for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 3; z++) {
                context.setBlockState(new BlockPos(x, 5, z), Blocks.SANDSTONE.getDefaultState());
            }
        }

        Building farm = new Building(id, UUID.randomUUID(), ResourceId.vanilla("farm"),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(1, 5, 1))),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(3, 6, 3))));

        int filled = UnderPieceFill.fillBuilding(world, farm, 64);

        for (int y = 2; y <= 4; y++) {
            context.assertFalse(context.getBlockState(new BlockPos(2, y, 2)).isAir(),
                    "Regra 356: o vão sob a construção ficou com ar em y " + y + " (" + filled + " blocos)");
        }
        context.assertTrue(context.getBlockState(new BlockPos(0, 3, 0)).isAir(), "o vão fora da construção foi aterrado");

        context.complete();
    }
    /** Jogo da 0.4.136: a escada da boca tapada virava pátio de pedra quando o pátio era refeito. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "sand_guard_yard", tickLimit = 20)
    public void theYardNeverSealsTheStair(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos mouth = context.getAbsolutePos(new BlockPos(8, 4, 8));
        BlockPos cell = stairCellAt(mouth, Direction.NORTH, mouth.getY() - 1);

        context.assertTrue(cell != null, "nenhuma célula do caracol no nível do pátio: o teste não mede nada");
        world.setBlockState(cell, Blocks.SAND.getDefaultState());
        world.setBlockState(cell.down(), Blocks.STONE.getDefaultState());

        com.villagecolony.fabric.integration.MineYard.lay(world, mouth, Direction.NORTH, 3);

        context.assertFalse(world.getBlockState(cell).isOf(Blocks.STONE),
                "Regra 356: o pátio selou a escada da mina com pedra em " + cell.toShortString());
        context.complete();
    }

    /** Jogo da 0.4.136: a escada tapada sumia da corrente da mina, e o mineiro andava sem destino. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "sand_guard_stair", tickLimit = 20)
    public void theKeeperClearsTheStairOfADeeperMine(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos mouth = context.getAbsolutePos(new BlockPos(8, 4, 8));
        BlockPos cell = stairCellAt(mouth, Direction.NORTH, mouth.getY() - 1);
        BlockPos outside = mouth.add(6, 0, 6);

        context.assertTrue(cell != null, "nenhuma célula do caracol no nível do pátio: o teste não mede nada");
        world.setBlockState(cell, Blocks.SAND.getDefaultState());
        world.setBlockState(outside, Blocks.SAND.getDefaultState());

        int cleared = MineStairClearing.clear(world, mouth, Direction.NORTH, 16);

        context.assertTrue(cleared > 0 && world.getBlockState(cell).isAir(),
                "Regra 356: a areia na escada da mina ficou (" + cleared + " blocos)");
        context.assertTrue(world.getBlockState(outside).isOf(Blocks.SAND), "a limpeza mexeu fora da escada");
        context.complete();
    }

    private static BlockPos stairCellAt(BlockPos mouth, Direction descent, int y) {
        return MineMouths.stairCells(mouth, descent).stream().filter(cell -> cell.getY() == y).findFirst().orElse(null);
    }
}
