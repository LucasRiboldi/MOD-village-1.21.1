package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.PathStep;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.integration.LotSpacing;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** A rede de caminhos no mundo — Regras 123 e 124. */
public class PathNetworkGameTest implements FabricGameTest {

    /**
     * Da porta até a rede por cima de uma vala: o caminho tem 3 de largura, a vala é aterrada, o degrau é
     * de no máximo um, e ele para ao encostar na rede.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "path_network", tickLimit = 40)
    public void aPathFromTheDoorFillsTheDitchAndJoinsTheNetwork(TestContext context) {
        ServerWorld world = context.getWorld();
        UUID colonyId = UUID.randomUUID();
        // Longe de tudo e acima do chão plano do mundo de teste: o cenário é só esta laje.
        BlockPos base = context.getAbsolutePos(BlockPos.ORIGIN).add(3_000_000, 30, 3_000_000);
        int y = base.getY();

        try {
            for (int x = -2; x <= 22; x++) {
                for (int z = -6; z <= 6; z++) {
                    BlockPos at = base.add(x, 0, z);
                    world.getChunk(at);
                    world.setBlockState(x == 8 ? at.down(3) : at, Blocks.GRASS_BLOCK.getDefaultState());
                }
            }

            Set<Long> roads = new HashSet<>();

            for (int z = -6; z <= 6; z++) {
                roads.add(ColonyRoads.column(base.getX() + 16, base.getZ() + z));
            }

            PathLinks.Entrance door = new PathLinks.Entrance(base.add(1, 1, 0), Side.EAST);
            List<PathStep> steps = PathLinks.trace(world, colonyId, door, roads, base.add(100, 0, 0), false).orElseThrow();

            context.assertTrue(roads.contains(ColonyRoads.column(steps.getLast().x(), steps.getLast().z())),
                    "o caminho devia parar na rede: " + steps.getLast());

            int lastY = steps.getFirst().floorY();

            for (PathStep step : steps) {
                context.assertTrue(Math.abs(step.floorY() - lastY) <= 1, "degrau maior que um em " + step);
                context.assertTrue(PathPaving.layStep(world, colonyId, step), "o passo não coube em " + step);
                lastY = step.floorY();

                for (int offset = -1; offset <= 1; offset++) {
                    BlockPos floor = new BlockPos(step.sideX(offset), step.floorY(), step.sideZ(offset));

                    context.assertTrue(world.getBlockState(floor).isOf(Blocks.DIRT_PATH),
                            "a seção não virou caminho em " + floor.toShortString());
                    context.assertTrue(world.getBlockState(floor.up()).isAir() && world.getBlockState(floor.up(2)).isAir(),
                            "sem altura livre em cima de " + floor.toShortString());
                }
            }

            // O passo sobre a vala: o piso dele tem chão debaixo, até o fundo da vala.
            PathStep overTheDitch = steps.stream().filter(step -> step.x() == base.getX() + 8).findFirst()
                    .orElseThrow();

            for (int offset = -1; offset <= 1; offset++) {
                for (int below = y - 2; below < overTheDitch.floorY(); below++) {
                    BlockPos under = new BlockPos(overTheDitch.sideX(offset), below, overTheDitch.sideZ(offset));

                    context.assertTrue(!world.getBlockState(under).isAir(),
                            "a vala não foi aterrada debaixo do caminho em " + under.toShortString() + ": " + overTheDitch);
                }
            }
        } finally {
            for (BlockPos at : BlockPos.iterate(base.add(-3, -4, -7), base.add(23, 3, 7))) {
                world.setBlockState(at, Blocks.AIR.getDefaultState());
            }

            PathLinks.clearAll();
        }

        context.complete();
    }

    /** Construção nova nasce a no mínimo três blocos de outra; o caminho pode encostar. */
    /**
     * A porta recuada da casa Vanilla (a varanda fica dentro da caixa) também é achada, e a frente é o
     * primeiro chão fora da caixa pelo lado aberto — no playtest 0.3.97 a rede não traçou nada por isso.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "path_network", tickLimit = 20)
    public void aRecessedDoorIsFoundAndLeadsOutOfTheBox(TestContext context) {
        BlockPos min = context.getAbsolutePos(new BlockPos(1, 2, 1));
        BlockPos max = context.getAbsolutePos(new BlockPos(7, 5, 7));
        BlockPos door = context.getAbsolutePos(new BlockPos(4, 2, 3));
        ServerWorld world = context.getWorld();

        world.setBlockState(door, Blocks.OAK_DOOR.getDefaultState()
                .with(net.minecraft.block.DoorBlock.FACING, net.minecraft.util.math.Direction.NORTH));
        world.setBlockState(door.up(), Blocks.OAK_DOOR.getDefaultState()
                .with(net.minecraft.block.DoorBlock.FACING, net.minecraft.util.math.Direction.NORTH)
                .with(net.minecraft.block.DoorBlock.HALF, net.minecraft.block.enums.DoubleBlockHalf.UPPER));
        world.setBlockState(door.south(2), Blocks.STONE.getDefaultState());

        Building house = new Building(UUID.randomUUID(), UUID.randomUUID(), ResourceId.vanilla("test_porch_house"),
                new ColonyPos(min.getX(), min.getY(), min.getZ()), new ColonyPos(max.getX(), max.getY(), max.getZ()));

        var entrance = PathLinks.entranceOf(world, house);

        context.assertTrue(entrance.isPresent(), "a porta recuada não foi achada");
        context.assertTrue(entrance.get().front().equals(new BlockPos(door.getX(), door.getY(), min.getZ() - 1)),
                "a frente devia ser o chão logo fora da caixa, ao norte; veio " + entrance.get().front());
        context.assertTrue(entrance.get().out() == Side.NORTH, "o rumo devia ser o norte, veio " + entrance.get().out());
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lot_spacing", tickLimit = 20)
    public void aNewLotKeepsThreeBlocksFromAnyConstruction(TestContext context) {
        UUID colonyId = UUID.randomUUID();
        BlockPos base = context.getAbsolutePos(BlockPos.ORIGIN).add(3_100_000, 0, 3_100_000);
        Building house = new Building(UUID.randomUUID(), colonyId, ResourceId.vanilla("test_house"),
                new ColonyPos(base.getX(), 0, base.getZ()), new ColonyPos(base.getX() + 4, 5, base.getZ() + 4));
        ColonyPos lot = new ColonyPos(3, 4, 3);

        VillageColonyMod.BUILDINGS.register(house);

        try {
            context.assertTrue(LotSpacing.tooClose(colonyId, base.getX() + 6, base.getZ(), lot),
                    "um bloco de folga devia ser pouco");
            context.assertTrue(LotSpacing.tooClose(colonyId, base.getX() + 7, base.getZ(), lot),
                    "dois blocos de folga devia ser pouco");
            context.assertFalse(LotSpacing.tooClose(colonyId, base.getX() + 8, base.getZ(), lot),
                    "três blocos de folga deviam bastar");
        } finally {
            VillageColonyMod.BUILDINGS.remove(house.id());
        }

        context.complete();
    }
}
