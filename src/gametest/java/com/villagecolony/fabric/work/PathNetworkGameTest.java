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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** A rede de caminhos no mundo — Regras 123 e 124. */
public class PathNetworkGameTest implements FabricGameTest {

    /** O diário mede blocos realmente assentados, sem contar de novo o piso pronto — ADR-043. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "path_paving_journal", tickLimit = 20)
    public void theJournalCountsOnlyTheSingleBlockActuallyChanged(TestContext context) throws IOException {
        ServerWorld world = context.getWorld();
        BlockPos at = context.getAbsolutePos(BlockPos.ORIGIN).add(3_200_200, 30, 3_200_200);
        UUID colonyId = UUID.randomUUID();
        UUID workerId = UUID.randomUUID();
        Path previousJournal = ActionJournal.file();
        Path journal = Files.createTempFile("village-path-paving-", ".jsonl");
        ActionJournal.flush();
        ActionJournal.writeTo(journal);
        try {
            world.getChunk(at);
            world.setBlockState(at.down(), Blocks.STONE.getDefaultState());
            world.setBlockState(at, Blocks.GRASS_BLOCK.getDefaultState());
            PathStep step = new PathStep(at.getX(), at.getY(), at.getZ(), Side.EAST, 0, 0);
            context.assertTrue(PathPaving.layAndRecord(world, colonyId, workerId, step), "primeiro passo recusado");
            context.assertTrue(world.getBlockState(at).isOf(Blocks.DIRT_PATH), "nenhum bloco foi assentado");
            context.assertTrue(PathPaving.layAndRecord(world, colonyId, workerId, step), "piso pronto recusado");
            ActionJournal.flush();
            List<String> actions = Files.readAllLines(journal).stream()
                    .filter(line -> line.contains(workerId.toString().substring(0, 8))).toList();
            context.assertTrue(actions.size() == 1 && actions.getFirst().contains("\"n\":1"),
                    "um bloco alterado deve produzir uma ação de quantidade 1: " + actions);
        } finally {
            ActionJournal.flush();
            ActionJournal.writeTo(previousJournal);
            Files.deleteIfExists(journal);
            world.setBlockState(at, Blocks.AIR.getDefaultState());
            world.setBlockState(at.down(), Blocks.AIR.getDefaultState());
        }
        context.complete();
    }

    /** A cota da entrada vem do apoio da porta, não do monte em sua frente — ADR-043. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "path_entry_height", tickLimit = 20)
    public void aMoundInFrontDoesNotRaiseTheDoorPath(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos base = context.getAbsolutePos(BlockPos.ORIGIN).add(3_200_000, 30, 3_200_000);
        UUID colonyId = UUID.randomUUID();
        try {
            for (BlockPos at : BlockPos.iterate(base.add(-2, -1, -3), base.add(12, 0, 3))) {
                world.getChunk(at);
                world.setBlockState(at, Blocks.GRASS_BLOCK.getDefaultState());
            }
            world.setBlockState(base.up(), Blocks.GRASS_BLOCK.getDefaultState());
            BlockPos road = base.east(10);
            world.setBlockState(road, Blocks.DIRT_PATH.getDefaultState());
            PathLinks.Entrance entrance = new PathLinks.Entrance(base.up(), Side.EAST);
            List<PathStep> steps = PathLinks.trace(world, colonyId, entrance,
                    Set.of(ColonyRoads.column(road.getX(), road.getZ())), road, false).orElseThrow();
            context.assertTrue(steps.getFirst().floorY() == base.getY(),
                    "o monte elevou o piso da entrada: " + steps.getFirst());
            context.assertTrue(PathPaving.layStep(world, colonyId, steps.getFirst()),
                    "o corte de um bloco na frente da porta deveria caber");
            context.assertTrue(world.getBlockState(base).isOf(Blocks.DIRT_PATH) && world.getBlockState(base.up()).isAir(),
                    "o piso deve ficar abaixo da metade inferior da porta, com passagem livre");
        } finally {
            for (BlockPos at : BlockPos.iterate(base.add(-2, -1, -3), base.add(12, 4, 3))) {
                world.setBlockState(at, Blocks.AIR.getDefaultState());
            }
            PathLinks.clearAll();
        }
        context.complete();
    }

    /** Índice e retorno de sucesso exigem apoio físico antes da pavimentação — ADR-043. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "path_entry_support", tickLimit = 20)
    public void anUnsupportedFloorIsRejectedWithoutChangingTheWorld(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos base = context.getAbsolutePos(BlockPos.ORIGIN).add(3_200_100, 30, 3_200_100);
        UUID colonyId = UUID.randomUUID();
        world.getChunk(base);
        try {
            world.setBlockState(base.down(), Blocks.AIR.getDefaultState());
            world.setBlockState(base, Blocks.GRASS_BLOCK.getDefaultState());
            PathStep step = new PathStep(base.getX(), base.getY(), base.getZ(), Side.EAST, 0, 0);
            context.assertFalse(PathPaving.layStep(world, colonyId, step),
                    "pavimentação sem apoio não pode retornar sucesso");
            context.assertTrue(world.getBlockState(base).isOf(Blocks.GRASS_BLOCK),
                    "pavimentação recusada alterou o piso");
            context.assertFalse(PathLinks.roadsOf(colonyId).contains(ColonyRoads.column(base.getX(), base.getZ())),
                    "pavimentação recusada entrou no índice");
        } finally {
            world.setBlockState(base, Blocks.AIR.getDefaultState());
            PathLinks.clearAll();
        }
        context.complete();
    }

    /**
     * O caminho da porta até a rede, rente ao chão (autor, 2026-10-28): o buraco de um bloco é coberto no
     * nível do chão em volta, o poço fundo ao lado nunca é aterrado (o caminho estreita ou desvia), e
     * nenhum bloco nasce acima da superfície — na 0.3.98 o aterro de até 4 levantou paredes de terra.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "path_network", tickLimit = 40)
    public void aPathFromTheDoorStaysFlushWithTheGroundAndJoinsTheNetwork(TestContext context) {
        ServerWorld world = context.getWorld();
        UUID colonyId = UUID.randomUUID();
        // Longe de tudo e acima do chão plano do mundo de teste: o cenário é só esta laje.
        BlockPos base = context.getAbsolutePos(BlockPos.ORIGIN).add(3_000_000, 30, 3_000_000);
        int y = base.getY();
        BlockPos pit = base.add(12, 0, 1);

        try {
            for (int x = -2; x <= 22; x++) {
                for (int z = -6; z <= 6; z++) {
                    BlockPos at = base.add(x, 0, z);
                    world.getChunk(at);
                    BlockPos surface = x == 8 ? at.down() : at;
                    world.setBlockState(surface.down(), Blocks.STONE.getDefaultState());
                    world.setBlockState(surface, Blocks.GRASS_BLOCK.getDefaultState());
                }
            }

            world.setBlockState(pit, Blocks.AIR.getDefaultState());
            world.setBlockState(pit.down(), Blocks.AIR.getDefaultState());
            world.setBlockState(pit.down(2), Blocks.AIR.getDefaultState());
            world.setBlockState(pit.down(3), Blocks.GRASS_BLOCK.getDefaultState());

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
                context.assertTrue(step.floorY() <= y, "piso acima da superfície em " + step);
                context.assertTrue(PathPaving.layStep(world, colonyId, step), "o passo não coube em " + step);
                context.assertTrue(world.getBlockState(new BlockPos(step.x(), step.floorY(), step.z())).isOf(Blocks.DIRT_PATH),
                        "o eixo não virou caminho em " + step);
                lastY = step.floorY();
            }

            for (BlockPos at : BlockPos.iterate(base.add(-2, 1, -6), base.add(22, 4, 6))) {
                context.assertTrue(world.getBlockState(at).isAir(),
                        "nasceu bloco acima da superfície em " + at.toShortString() + ": " + world.getBlockState(at));
            }

            for (int down = 0; down <= 2; down++) {
                context.assertTrue(world.getBlockState(pit.down(down)).isAir(),
                        "o poço fundo ao lado do caminho foi aterrado em " + pit.down(down).toShortString());
            }
        } finally {
            for (BlockPos at : BlockPos.iterate(base.add(-3, -4, -7), base.add(23, 4, 7))) {
                world.setBlockState(at, Blocks.AIR.getDefaultState());
            }

            PathLinks.clearAll();
        }

        context.complete();
    }

    /**
     * O caminho da porta até a rua tem 1 bloco de largura — autor, 2026-10-28, depois do jogo da 0.3.104 ("1 de
     * largura, todas as casas"): o passo assenta só o eixo, e as colunas de lado ficam como estão.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "path_network_width", tickLimit = 20)
    public void aDoorPathIsOneBlockWide(TestContext context) {
        ServerWorld world = context.getWorld();
        UUID colonyId = UUID.randomUUID();
        BlockPos base = context.getAbsolutePos(BlockPos.ORIGIN).add(3_000_100, 30, 3_000_100);

        try {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    BlockPos at = base.add(x, 0, z);
                    world.getChunk(at);
                    world.setBlockState(at.down(), Blocks.STONE.getDefaultState());
                    world.setBlockState(at, Blocks.GRASS_BLOCK.getDefaultState());
                }
            }

            PathStep step = new PathStep(base.getX(), base.getY(), base.getZ(), Side.EAST, 0, 0);

            context.assertTrue(PathPaving.layStep(world, colonyId, step), "o passo não coube em " + step);
            context.assertTrue(world.getBlockState(base).isOf(Blocks.DIRT_PATH), "o eixo não virou caminho");

            for (int offset : new int[] {-1, 1}) {
                BlockPos side = new BlockPos(step.sideX(offset), base.getY(), step.sideZ(offset));

                context.assertTrue(world.getBlockState(side).isOf(Blocks.GRASS_BLOCK),
                        "a coluna de lado virou " + world.getBlockState(side) + ": o caminho da porta é de 1");
            }
        } finally {
            for (BlockPos at : BlockPos.iterate(base.add(-3, -1, -3), base.add(3, 3, 3))) {
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
