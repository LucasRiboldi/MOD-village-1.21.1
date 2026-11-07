package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.RoadGraph;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.data.save.ColonySavedData;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/** Branco só onde se entra, colado na porta, e sem entrada não há ramal — Regra 223. */
public class EntrancesAtTheDoorGameTest implements FabricGameTest {

    private static final int SIZE = 40;

    /** Uma arena plana com vila, longe das outras arenas de teste. */
    private static final class Arena {
        final TestContext context;
        final ServerWorld world;
        final BlockPos origin;
        final ColonyFixture fixture;
        final Colony colony;
        final Set<ColonyPos> paths = new HashSet<>();
        final Set<UUID> buildings = new HashSet<>();

        Arena(TestContext context, int y) {
            this.context = context;
            world = context.getWorld();
            origin = context.getAbsolutePos(new BlockPos(1, y, 1));
            chunks(true);

            for (BlockPos at : BlockPos.iterate(origin, origin.add(SIZE, 0, SIZE))) {
                world.setBlockState(at, Blocks.GRASS_BLOCK.getDefaultState(), 18);
            }

            fixture = ColonyFixture.colonyAtWorld(origin.add(20, 1, 2));
            colony = fixture.colony();
        }

        void path(int fromX, int toX, int fromZ, int toZ) {
            for (BlockPos at : BlockPos.iterate(origin.add(fromX, 0, fromZ), origin.add(toX, 0, toZ))) {
                world.setBlockState(at, Blocks.DIRT_PATH.getDefaultState(), 18);
                paths.add(cell(at));
            }
        }

        ColonyPos cell(BlockPos at) {
            return MinecraftTypeAdapter.toColonyPos(at);
        }

        ColonyPos cell(int x, int z) {
            return cell(origin.add(x, 0, z));
        }

        void building(String path, BlockPos min, BlockPos max) {
            Building building = new Building(UUID.randomUUID(), colony.id(), ResourceId.vanilla(path), cell(min), cell(max), true);

            buildings.add(building.id());
            VillageColonyMod.BUILDINGS.register(building);
        }

        /** Uma porta de frente para o norte. */
        void door(BlockPos at) {
            var lower = Blocks.OAK_DOOR.getDefaultState().with(DoorBlock.FACING, Direction.SOUTH);

            world.setBlockState(at, lower.with(DoorBlock.HALF, DoubleBlockHalf.LOWER), 18);
            world.setBlockState(at.up(), lower.with(DoorBlock.HALF, DoubleBlockHalf.UPPER), 18);
        }

        RoadGraph publish() {
            RoadGraphRefresh.publish(world, colony, paths);

            return ColonySavedData.get(world.getServer()).roadGraphs().graph(colony.id()).orElseThrow();
        }

        void end() {
            buildings.forEach(VillageColonyMod.BUILDINGS::remove);
            fixture.cleanUp();
            ShepherdPens.clearAll();

            for (BlockPos at : BlockPos.iterate(origin, origin.add(SIZE, 9, SIZE))) {
                world.setBlockState(at, Blocks.AIR.getDefaultState(), 18);
            }

            chunks(false);
        }

        private void chunks(boolean forced) {
            for (int x = origin.getX() >> 4; x <= (origin.getX() + SIZE) >> 4; x++) {
                for (int z = origin.getZ() >> 4; z <= (origin.getZ() + SIZE) >> 4; z++) {
                    world.setChunkForced(x, z, forced);
                }
            }
        }
    }

    private static void run(TestContext context, int y, Consumer<Arena> scenario) {
        Arena arena = new Arena(context, y);

        try {
            scenario.accept(arena);
        } finally {
            arena.end();
        }

        context.complete();
    }

    private static boolean isEntrance(RoadGraph graph, ColonyPos at) {
        return graph.nodes().stream().anyMatch(node -> node.position().equals(at) && node.roles().contains(RoadGraph.Role.ENTRANCE));
    }

    private static boolean isLinked(RoadGraph graph, ColonyPos at) {
        return graph.edges().stream().anyMatch(edge -> edge.from().equals(at) || edge.to().equals(at));
    }

    private static boolean onALine(RoadGraph graph, ColonyPos at) {
        return graph.edges().stream().anyMatch(edge -> edge.columns().contains(at));
    }

    /** A caixa da casa inclui a frente da porta: o branco fica na porta, e uma linha nova sai da caixa até a rua. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "door_entrance_box", tickLimit = 20)
    public void aDoorDeepInTheBoxHasItsMarkerThereAndALineToTheRoad(TestContext context) {
        run(context, 60, arena -> {
            arena.path(2, 38, 2, 2);
            arena.building("village/plains/houses/test_house", arena.origin.add(10, 1, 5), arena.origin.add(18, 5, 12));
            arena.door(arena.origin.add(14, 1, 8));

            RoadGraph graph = arena.publish();
            ColonyPos front = arena.cell(14, 7);

            context.assertTrue(isEntrance(graph, front), "Regra 223: o branco não ficou na frente da porta: " + graph.nodes());
            context.assertFalse(isEntrance(graph, arena.cell(14, 4)), "Regra 223: o branco ainda fica fora da caixa: " + graph.nodes());
            context.assertTrue(isLinked(graph, front), "Regra 223: a porta ficou sem linha até a rua: " + graph.edges());
            context.assertTrue(onALine(graph, arena.cell(14, 5)), "Regra 223: a linha não saiu pela frente da porta: " + graph.edges());
        });
    }

    /** O caminho que já leva da porta à rua vale: há rua logo em frente à porta, e a linha não cruza em reta até ela. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "door_entrance_path", tickLimit = 20)
    public void theLineFollowsThePathThatAlreadyLeavesTheDoor(TestContext context) {
        run(context, 70, arena -> {
            arena.path(2, 38, 16, 16);
            arena.path(13, 13, 7, 16);
            arena.path(2, 38, 3, 3);
            arena.path(38, 38, 3, 16);
            arena.building("village/plains/houses/test_house", arena.origin.add(10, 1, 5), arena.origin.add(18, 5, 12));
            arena.door(arena.origin.add(14, 1, 8));

            RoadGraph graph = arena.publish();

            context.assertTrue(isEntrance(graph, arena.cell(14, 7)), "Regra 223: o branco não ficou na frente da porta: " + graph.nodes());
            context.assertTrue(onALine(graph, arena.cell(13, 10)), "Regra 223: a linha largou o caminho: " + graph.edges());
            context.assertFalse(onALine(graph, arena.cell(14, 5)), "Regra 223: a linha cruzou em reta: " + graph.edges());
        });
    }

    /** O poste não tem entrada: o ramal dele sai inteiro, mesmo além dos 3 blocos da Regra 186, e o rosa do encontro também. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "door_entrance_lamp", tickLimit = 20)
    public void aLampHasNoEntranceAndItsSpurLeavesTheGround(TestContext context) {
        run(context, 80, arena -> {
            arena.path(2, 38, 2, 2);
            arena.path(20, 20, 3, 13);
            arena.building("village/plains/plains_lamp_1", arena.origin.add(19, 1, 14), arena.origin.add(21, 5, 16));

            RoadGraph graph = arena.publish();

            context.assertFalse(isEntrance(graph, arena.cell(20, 13)), "Regra 223: o poste ganhou branco: " + graph.nodes());
            context.assertFalse(onALine(graph, arena.cell(20, 8)), "Regra 223: o ramal do poste continua na rede: " + graph.edges());
            context.assertFalse(graph.nodes().stream().anyMatch(node -> node.position().equals(arena.cell(20, 2))
                    && node.roles().contains(RoadGraph.Role.JUNCTION)), "Regra 223: o rosa do ramal ficou: " + graph.nodes());

            for (int pass = 0; pass < 2; pass++) {
                PathTrim.pass(arena.world, arena.colony);
            }

            for (int z = 3; z <= 13; z++) {
                context.assertFalse(arena.world.getBlockState(arena.origin.add(20, 0, z)).isOf(Blocks.DIRT_PATH),
                        "Regra 223: o ramal ficou no chão em z=" + z);
            }

            context.assertTrue(arena.world.getBlockState(arena.origin.add(20, 0, 2)).isOf(Blocks.DIRT_PATH), "Regra 223: a rua saiu junto");
        });
    }

    /** O degrau em frente à porta leva o branco, mas não vira caminho: o construtor não recebe a coluna. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "door_entrance_step", tickLimit = 20)
    public void theStepInFrontOfTheDoorIsNotOfferedForPaving(TestContext context) {
        run(context, 120, arena -> {
            PathLinks.clearAll();
            arena.path(2, 38, 2, 2);
            arena.building("village/plains/houses/test_house", arena.origin.add(10, 1, 5), arena.origin.add(18, 5, 12));
            arena.world.setBlockState(arena.origin.add(14, 1, 8), Blocks.STONE_BRICKS.getDefaultState(), 18);
            arena.world.setBlockState(arena.origin.add(14, 1, 7), Blocks.OAK_STAIRS.getDefaultState(), 18);
            arena.door(arena.origin.add(14, 2, 8));

            RoadGraph graph = arena.publish();
            ColonyPos step = arena.cell(arena.origin.add(14, 1, 7));

            context.assertTrue(isEntrance(graph, step), "Regra 223: o branco não ficou no degrau: " + graph.nodes());

            var plan = PathLinks.planOf(arena.colony.id());

            context.assertTrue(plan.isPresent(), "o resto do ramal devia ser calçado");
            context.assertFalse(plan.get().steps.stream().anyMatch(at -> at.x() == step.x() && at.z() == step.z()),
                    "Regra 223: o degrau foi oferecido para calçar: " + plan.get().steps);
            PathLinks.clearAll();
        });
    }

    /** A rua ao lado da plantação basta: a quina deixa de ser entrada, e fica assim nas releituras seguintes. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "door_entrance_farm_road", tickLimit = 20)
    public void aFarmBesideTheRoadHasNoCornerEntrance(TestContext context) {
        run(context, 90, arena -> {
            arena.path(2, 38, 4, 4);
            arena.building("village/plains/test_farm", arena.origin.add(18, 1, 6), arena.origin.add(24, 2, 12));

            RoadGraph graph = arena.publish();

            for (int read = 0; read < RoadShape.CORNER_AFTER_READS + 2; read++) {
                graph = arena.publish();
            }

            context.assertFalse(isEntrance(graph, arena.cell(17, 5)), "Regra 223: a quina ao lado da rua virou entrada: " + graph.nodes());
        });
    }

    /** Sem rua perto, a quina continua sendo a entrada da plantação (Regra 184), e o ramal dela não a desfaz. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "door_entrance_farm_far", tickLimit = 20)
    public void aFarmFarFromTheRoadKeepsItsCornerEntrance(TestContext context) {
        run(context, 100, arena -> {
            arena.path(2, 38, 2, 2);
            arena.building("village/plains/test_farm", arena.origin.add(18, 1, 20), arena.origin.add(24, 2, 26));

            RoadGraph graph = arena.publish();

            for (int read = 0; read < RoadShape.CORNER_AFTER_READS + 3; read++) {
                graph = arena.publish();
            }

            context.assertTrue(isEntrance(graph, arena.cell(20 - 3, 19)) || isEntrance(graph, arena.cell(25, 19)),
                    "Regra 184: a plantação longe da rua perdeu a quina: " + graph.nodes());
        });
    }

    /** A porta dentro de um curral fechado não dá marcador: o caminho abriria a cerca. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "door_entrance_pen", tickLimit = 20)
    public void nothingInsideAClosedPenIsAnEntrance(TestContext context) {
        run(context, 110, arena -> {
            BlockPos corner = arena.origin.add(10, 1, 10);

            for (int x = 0; x <= 8; x++) {
                for (int z = 0; z <= 8; z++) {
                    if (x == 0 || x == 8 || z == 0 || z == 8) {
                        arena.world.setBlockState(corner.add(x, 0, z), Blocks.OAK_FENCE.getDefaultState(), 3);
                    }
                }
            }

            arena.world.setBlockState(corner.add(4, 0, 0), Blocks.OAK_FENCE_GATE.getDefaultState()
                    .with(net.minecraft.block.FenceGateBlock.FACING, Direction.NORTH), 3);
            arena.colony.measure(new VillageBounds(arena.origin.getX(), arena.origin.getY(), arena.origin.getZ(),
                    arena.origin.getX() + SIZE, arena.origin.getY() + 9, arena.origin.getZ() + SIZE));
            arena.building("village/plains/houses/test_house", corner.add(3, 0, 4), corner.add(5, 2, 6));
            arena.door(corner.add(4, 0, 6));

            context.assertTrue(ShepherdPens.pensOf(arena.world, arena.colony).size() == 1, "o curral da fixture não fechou");

            WorldPathTerrain terrain = new WorldPathTerrain(arena.world, arena.colony.id());
            Set<ColonyPos> fronts = StructureFronts.read(arena.world, arena.colony, terrain, arena.paths).fronts();

            context.assertFalse(fronts.contains(arena.cell(14, 15)), "Regra 223: entrada dentro do curral: " + fronts);
            context.assertTrue(fronts.contains(arena.cell(14, 9)), "Regra 178: o portão do curral continua entrada: " + fronts);
            context.assertTrue(List.copyOf(fronts).size() == 1, "só o portão: " + fronts);
        });
    }
}
