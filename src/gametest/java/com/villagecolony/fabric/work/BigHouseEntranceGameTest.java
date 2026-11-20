package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.RoadGraph;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.data.save.ColonySavedData;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.WorldPathTerrain;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;

import java.util.List;
import java.util.UUID;
import java.util.OptionalInt;

/** A entrada da BigHouseMOD de verdade, posta do molde do mod num chão plano (Regra 335). */
public class BigHouseEntranceGameTest implements FabricGameTest {

    private static final int FLOOR = 20;

    /** A entrada é a porta do térreo, no chão em volta, e o grafo lê esse chão, e não o beiral acima dela. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "big_house_entrance", tickLimit = 20)
    public void theBigHouseEntranceIsOnTheGround(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos ground = context.getAbsolutePos(new BlockPos(0, 150, 0));
        BlockPos low = ground.add(-4, 0, -4);

        for (BlockPos at : BlockPos.iterate(low, low.add(FLOOR, 0, FLOOR))) {
            world.setBlockState(at, Blocks.GRASS_BLOCK.getDefaultState(), 18);
            world.setBlockState(at.down(), Blocks.DIRT.getDefaultState(), 18);
        }

        StructureTemplate template = world.getStructureTemplateManager()
                .getTemplate(Identifier.of("villagecolony", "houses/big_house_mod")).orElseThrow();
        BlockPos origin = ground.up();
        Vec3i size = template.getSize();
        ColonyFixture fixture = ColonyFixture.colonyAtWorld(origin.add(size.getX() / 2, 0, size.getZ() / 2));

        try {
            template.place(world, origin, origin, new StructurePlacementData(), Random.create(1), 2);

            BlockPos max = origin.add(size.getX() - 1, size.getY() - 1, size.getZ() - 1);
            List<DoorFronts.Entrance> entrances = DoorFronts.entrances(world, origin, max);

            context.assertTrue(entrances.size() == 1, "a casa devia ter uma entrada: " + entrances.size());

            BlockPos front = entrances.getFirst().front();

            context.assertTrue(front.getY() == ground.getY(),
                    "Regra 335: a entrada não ficou no chão em volta: y " + front.getY() + ", chão " + ground.getY());

            OptionalInt read = new WorldPathTerrain(world, fixture.colony().id()).groundY(front.getX(), front.getZ(), front.getY());

            context.assertTrue(read.isPresent() && read.getAsInt() == ground.getY(),
                    "Regra 335: o grafo leu o chão da entrada em " + read + ", e não em " + ground.getY());
        } finally {
            fixture.cleanUp();
            for (BlockPos at : BlockPos.iterate(low.down(), low.add(FLOOR, 12, FLOOR))) {
                world.setBlockState(at, Blocks.AIR.getDefaultState(), 18);
            }
        }

        context.complete();
    }

    /**
     * A casa posta pela fundação, registrada como construção, com a rua do jogo da 0.4.93 à frente da porta: caminho, uma
     * falha, um obstáculo, caminho e a porta (`---.#---E`). A entrada liga à rede.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "big_house_entrance_graph", tickLimit = 20)
    public void theBigHouseEntranceJoinsTheGraph(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos ground = context.getAbsolutePos(new BlockPos(0, 170, 0));
        BlockPos low = ground.add(-20, 0, -12);
        int size = 32;

        for (BlockPos at : BlockPos.iterate(low, low.add(size, 0, size))) {
            world.setBlockState(at, Blocks.GRASS_BLOCK.getDefaultState(), 18);
            world.setBlockState(at.down(), Blocks.DIRT.getDefaultState(), 18);
        }

        StructureTemplate template = world.getStructureTemplateManager()
                .getTemplate(Identifier.of("villagecolony", "houses/big_house_mod")).orElseThrow();
        BlockPos origin = ground.up();
        Vec3i box = template.getSize();
        ColonyFixture fixture = ColonyFixture.colonyAtWorld(ground.add(-14, 1, 5));
        Colony colony = fixture.colony();

        try {
            template.place(world, origin, origin, new StructurePlacementData(), Random.create(1), 2);
            colony.measure(new VillageBounds(low.getX(), ground.getY() - 4, low.getZ(), low.getX() + size,
                    ground.getY() + 12, low.getZ() + size));
            VillageColonyMod.BUILDINGS.register(new Building(UUID.randomUUID(), colony.id(),
                    com.villagecolony.fabric.integration.StructureBlueprintReader.BIG_HOUSE_MOD,
                    MinecraftTypeAdapter.toColonyPos(origin), new ColonyPos(origin.getX() + box.getX() - 1,
                    origin.getY() + box.getY() - 1, origin.getZ() + box.getZ() - 1), true));

            // A porta do molde fica em x 1, z 5; a frente, em x 0, no chão.
            int doorZ = origin.getZ() + 5;
            int frontX = origin.getX() - 1;

            // A rua principal, de norte a sul, a 8 blocos da porta.
            for (int z = low.getZ() + 2; z <= low.getZ() + size - 2; z++) {
                world.setBlockState(new BlockPos(frontX - 8, ground.getY(), z), Blocks.DIRT_PATH.getDefaultState(), 18);
            }

            // `---.#---E`: caminho, falha, obstáculo, caminho e a frente da porta.
            for (int x = frontX - 7; x <= frontX - 6; x++) {
                world.setBlockState(new BlockPos(x, ground.getY(), doorZ), Blocks.DIRT_PATH.getDefaultState(), 18);
            }
            world.setBlockState(new BlockPos(frontX - 4, ground.getY() + 1, doorZ), Blocks.COBBLESTONE_WALL.getDefaultState(), 18);
            for (int x = frontX - 3; x <= frontX; x++) {
                world.setBlockState(new BlockPos(x, ground.getY(), doorZ), Blocks.DIRT_PATH.getDefaultState(), 18);
            }

            RoadGraphRefresh.scanNow(world, colony);

            RoadGraph graph = ColonySavedData.get(world.getServer()).roadGraphs().graph(colony.id()).orElseThrow();
            ColonyPos front = new ColonyPos(frontX, ground.getY(), doorZ);
            boolean linked = graph.edges().stream().anyMatch(edge -> edge.from().equals(front) || edge.to().equals(front)
                    || edge.columns().contains(front));

            context.assertTrue(linked, "Regra 335: a entrada da BigHouseMOD ficou solta; nós "
                    + graph.nodes().stream().filter(node -> Math.abs(node.position().x() - frontX) <= 1
                    && Math.abs(node.position().z() - doorZ) <= 1).toList());
        } finally {
            fixture.cleanUp();
            for (BlockPos at : BlockPos.iterate(low.down(), low.add(size, 12, size))) {
                world.setBlockState(at, Blocks.AIR.getDefaultState(), 18);
            }
        }

        context.complete();
    }

    /** Regra 335: a porta da sacada, com o chão 3 abaixo do piso dela, não vira entrada; a do térreo vira. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "upper_floor_door", tickLimit = 20)
    public void anUpperFloorDoorIsNeverAnEntrance(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos ground = context.getAbsolutePos(new BlockPos(0, 190, 0));
        BlockPos low = ground.add(-6, 0, -2);

        for (BlockPos at : BlockPos.iterate(low, low.add(14, 0, 12))) {
            world.setBlockState(at, Blocks.GRASS_BLOCK.getDefaultState(), 18);
            world.setBlockState(at.down(), Blocks.DIRT.getDefaultState(), 18);
        }

        BlockPos min = ground.up();
        BlockPos max = min.add(6, 6, 6);
        net.minecraft.block.BlockState door = Blocks.OAK_DOOR.getDefaultState()
                .with(net.minecraft.block.DoorBlock.FACING, net.minecraft.util.math.Direction.EAST);
        BlockPos balconyDoor = min.add(0, 3, 3);
        BlockPos groundDoor = min.add(0, 0, 5);

        try {
            // A sacada de tábua para oeste, 3 acima do chão, sem escada.
            for (int x = -3; x <= -1; x++) {
                world.setBlockState(balconyDoor.add(x, -1, 0), Blocks.OAK_PLANKS.getDefaultState(), 18);
            }

            world.setBlockState(balconyDoor, door, 18);
            world.setBlockState(balconyDoor.up(), door.with(net.minecraft.block.DoorBlock.HALF,
                    net.minecraft.block.enums.DoubleBlockHalf.UPPER), 18);
            world.setBlockState(groundDoor, door, 18);
            world.setBlockState(groundDoor.up(), door.with(net.minecraft.block.DoorBlock.HALF,
                    net.minecraft.block.enums.DoubleBlockHalf.UPPER), 18);

            context.assertFalse(GroundFloorDoors.isGroundFloor(world, balconyDoor, net.minecraft.util.math.Direction.WEST),
                    "Regra 335: a porta da sacada contou como térreo");
            context.assertTrue(GroundFloorDoors.isGroundFloor(world, groundDoor, net.minecraft.util.math.Direction.WEST),
                    "a porta do térreo não contou como térreo");

            List<DoorFronts.Entrance> entrances = DoorFronts.entrances(world, min, max);

            context.assertTrue(entrances.size() == 1 && entrances.getFirst().front().getY() == ground.getY(),
                    "Regra 335: o grafo buscou a porta do andar de cima: " + entrances.stream().map(DoorFronts.Entrance::front).toList());
        } finally {
            for (BlockPos at : BlockPos.iterate(low.down(), low.add(14, 8, 12))) {
                world.setBlockState(at, Blocks.AIR.getDefaultState(), 18);
            }
        }

        context.complete();
    }
}
