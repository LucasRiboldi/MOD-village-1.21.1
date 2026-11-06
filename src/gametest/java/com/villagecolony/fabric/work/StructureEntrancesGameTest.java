package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.RoadGraph;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.data.save.ColonySavedData;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.MineGate;
import com.villagecolony.fabric.integration.MineMouth;
import com.villagecolony.fabric.integration.WorldPathTerrain;
import com.villagecolony.fabric.world.GeneratedVillages;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** A entrada de toda estrutura, sem porta posta no mundo — Regra 210, e a casa do mineiro na boca em uso — Regra 211. */
public class StructureEntrancesGameTest implements FabricGameTest {

    private static final int SIZE = 40;

    private static void platform(ServerWorld world, BlockPos origin) {
        chunks(world, origin, true);

        for (BlockPos at : BlockPos.iterate(origin, origin.add(SIZE, 0, SIZE))) {
            world.setBlockState(at, Blocks.GRASS_BLOCK.getDefaultState(), 18);
        }
    }

    private static void clean(ServerWorld world, BlockPos origin) {
        for (BlockPos at : BlockPos.iterate(origin, origin.add(SIZE, 9, SIZE))) {
            world.setBlockState(at, Blocks.AIR.getDefaultState(), 18);
        }

        chunks(world, origin, false);
    }

    private static void chunks(ServerWorld world, BlockPos origin, boolean forced) {
        for (int x = origin.getX() >> 4; x <= (origin.getX() + SIZE) >> 4; x++) {
            for (int z = origin.getZ() >> 4; z <= (origin.getZ() + SIZE) >> 4; z++) {
                world.setChunkForced(x, z, forced);
            }
        }
    }

    private static ColonyPos cell(BlockPos at) {
        return MinecraftTypeAdapter.toColonyPos(at);
    }

    /** Três paredes e um lado aberto: a entrada é o lado aberto, mesmo com o centro da vila atrás de uma parede. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "entrance_open_side", tickLimit = 20)
    public void aStructureWithoutDoorOpensOnTheSideThatLooksLikeOne(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 60, 1));

        platform(world, origin);

        try {
            BlockPos min = origin.add(10, 1, 10);
            BlockPos max = origin.add(14, 3, 14);

            for (BlockPos at : BlockPos.iterate(min, max)) {
                boolean wall = at.getX() == min.getX() || at.getX() == max.getX() || at.getZ() == min.getZ();

                if (wall) {
                    world.setBlockState(at, Blocks.STONE.getDefaultState(), 18);
                }
            }

            // O centro da vila fica ao norte, atrás da parede de pedra; o sul é o lado aberto.
            BlockPos hub = origin.add(12, 1, 0);
            WorldPathTerrain terrain = new WorldPathTerrain(world, UUID.randomUUID());
            Optional<ColonyPos> side = EntranceSides.of(world, min, max, hub, terrain);

            context.assertTrue(side.isPresent() && side.get().z() == max.getZ() + 1 && side.get().x() >= min.getX() + 1
                    && side.get().x() <= max.getX() - 1, "Regra 210: a entrada não saiu pelo lado aberto: " + side);
            context.assertTrue(world.getBlockState(origin.add(12, 1, 14)).isAir(), "Regra 210: uma porta foi posta no mundo");
        } finally {
            clean(world, origin);
        }

        context.complete();
    }

    /** Fechada dos quatro lados, a estrutura ainda ganha entrada, no lado do centro da vila. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "entrance_closed", tickLimit = 20)
    public void aClosedStructureStillGetsAnEntranceFacingTheVillage(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 72, 1));

        platform(world, origin);

        try {
            BlockPos min = origin.add(10, 1, 10);
            BlockPos max = origin.add(14, 3, 14);

            for (BlockPos at : BlockPos.iterate(min, max)) {
                world.setBlockState(at, Blocks.STONE.getDefaultState(), 18);
            }

            WorldPathTerrain terrain = new WorldPathTerrain(world, UUID.randomUUID());
            Optional<ColonyPos> side = EntranceSides.of(world, min, max, origin.add(12, 1, 0), terrain);

            context.assertTrue(side.isPresent() && side.get().z() == min.getZ() - 1,
                    "Regra 210: a estrutura fechada ficou sem entrada: " + side);
        } finally {
            clean(world, origin);
        }

        context.complete();
    }

    /** O portão é reconhecido no mundo em todo giro, com a boca e o rumo certos. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "entrance_gate_read", tickLimit = 20)
    public void theMineGateIsReadFromTheWorldInEveryRotation(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 84, 1));

        platform(world, origin);

        try {
            BlockPos mouth = origin.add(20, 1, 20);

            for (Direction descent : Direction.Type.HORIZONTAL) {
                MineMouth.furnish(world, mouth, descent, false);

                BlockPos peak = MineGate.at(mouth, descent, 4, 6, 2);

                context.assertTrue(MineGate.isPeak(world.getBlockState(peak)), "Regra 210: o topo do portão mudou: " + descent);

                Optional<MineGate.Standing> read = MineGate.standingAt(world, peak);

                context.assertTrue(read.equals(Optional.of(new MineGate.Standing(mouth, descent))),
                        "Regra 210: portão lido errado em " + descent + ": " + read);

                for (BlockPos at : BlockPos.iterate(origin.add(0, 1, 0), origin.add(SIZE, 9, SIZE))) {
                    world.setBlockState(at, Blocks.AIR.getDefaultState(), 18);
                }

                context.assertTrue(MineGate.standingAt(world, peak).isEmpty(), "Regra 210: portão derrubado continuou lido");
            }
        } finally {
            clean(world, origin);
        }

        context.complete();
    }

    /** O portão vira entrada do grafo, branca como a de uma porta, do lado de onde se chega. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "entrance_gate_graph", tickLimit = 20)
    public void theMineGateIsAnEntranceOfTheGraph(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 96, 1));

        platform(world, origin);

        ColonyFixture fixture = ColonyFixture.colonyAtWorld(origin.add(20, 1, 2));
        Colony colony = fixture.colony();

        try {
            Set<ColonyPos> paths = new HashSet<>();

            for (int x = 2; x <= 38; x++) {
                BlockPos at = origin.add(x, 0, 2);

                world.setBlockState(at, Blocks.DIRT_PATH.getDefaultState(), 18);
                paths.add(cell(at));
            }

            BlockPos mouth = origin.add(20, 1, 25);

            MineMouth.furnish(world, mouth, Direction.SOUTH, false);

            BlockPos peak = MineGate.at(mouth, Direction.SOUTH, 4, 6, 2);

            MineGates.look(world, colony.id(), peak, world.getBlockState(peak));
            RoadGraphRefresh.publish(world, colony, paths);

            BlockPos front = MineGate.approach(mouth, Direction.SOUTH, 1);
            ColonyPos entrance = new ColonyPos(front.getX(), origin.getY(), front.getZ());
            RoadGraph graph = ColonySavedData.get(world.getServer()).roadGraphs().graph(colony.id()).orElseThrow();

            context.assertTrue(graph.nodes().stream().anyMatch(node -> node.position().equals(entrance)
                    && node.roles().contains(RoadGraph.Role.ENTRANCE)),
                    "Regra 210: o portão da mina não virou entrada em " + entrance + ": " + graph.nodes());
        } finally {
            fixture.cleanUp();
            clean(world, origin);
        }

        context.complete();
    }

    /** Com a escada aberta na boca, a entrada recua para fora da folga dela: o caminho não pode aterrar a mina. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "entrance_gate_clear", tickLimit = 20)
    public void theGateEntranceStaysOutOfTheStairClearance(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 108, 1));

        platform(world, origin);

        ColonyFixture fixture = ColonyFixture.colonyAtWorld(origin.add(20, 1, 2));
        Colony colony = fixture.colony();

        try {
            BlockPos mouth = origin.add(20, 1, 25);

            VillageColonyMod.MINES.open(colony.id(), com.villagecolony.core.construction.model.MineShaft.from(cell(mouth),
                    com.villagecolony.core.type.Side.SOUTH));
            MineMouth.furnish(world, mouth, Direction.SOUTH, false);

            BlockPos peak = MineGate.at(mouth, Direction.SOUTH, 4, 6, 2);

            MineGates.look(world, colony.id(), peak, world.getBlockState(peak));

            WorldPathTerrain terrain = new WorldPathTerrain(world, colony.id());
            Set<ColonyPos> fronts = MineGates.fronts(world, colony.id(), terrain);

            context.assertTrue(fronts.size() == 1, "Regra 210: o portão ficou sem entrada: " + fronts);

            ColonyPos front = fronts.iterator().next();

            context.assertFalse(terrain.blocked(front.x(), front.z()), "Regra 210: a entrada caiu na folga da escada: " + front);
            context.assertTrue(front.z() < mouth.getZ() - WorldPathTerrain.MINE_CLEARANCE,
                    "Regra 210: a entrada não recuou para o lado de onde se chega: " + front);
        } finally {
            VillageColonyMod.MINES.removeOfColony(colony.id());
            fixture.cleanUp();
            clean(world, origin);
        }

        context.complete();
    }

    /** Toda estrutura da vila gerada conta; rua, ponta de rua e peça que só põe bicho não. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "entrance_pieces", tickLimit = 20)
    public void everyVillageStructureCountsAndStreetsDoNot(TestContext context) {
        for (String yes : java.util.List.of("minecraft:village/plains/houses/plains_small_house_1", "minecraft:village/plains/plains_lamp_1",
                "minecraft:village/plains/town_centers/plains_meeting_point_1", "minecraft:village/plains/houses/plains_accessory_1",
                "minecraft:village/desert/houses/desert_temple_1")) {
            context.assertTrue(GeneratedVillages.isStructure("Single[Left[" + yes + "]]"), "Regra 210: faltou " + yes);
        }

        for (String no : java.util.List.of("minecraft:village/plains/streets/corner_01", "minecraft:village/plains/terminators/terminator_01",
                "minecraft:village/plains/villagers/baby", "minecraft:village/common/animals/cows_1", "minecraft:village/common/iron_golem",
                "minecraft:village/plains/houses/plains_small_farm_1")) {
            context.assertFalse(GeneratedVillages.isStructure("Single[Left[" + no + "]]"), "Regra 210: não devia contar " + no);
        }

        context.assertFalse(GeneratedVillages.isStructure("Feature[minecraft:oak]"), "Regra 210: árvore não é estrutura");
        context.complete();
    }

    /** A boca mudou: a casa da boca antiga não serve, até uma por mineiro. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "entrance_miner_home", tickLimit = 20)
    public void theMinerHomeFollowsTheMouthInUse(TestContext context) {
        UUID colonyId = UUID.randomUUID();
        BlockPos oldMouth = new BlockPos(4_000_000, 70, 4_000_000);
        BlockPos newMouth = oldMouth.add(200, 0, 0);

        context.assertTrue(MineHomePlans.isOwedAt(colonyId, oldMouth), "Regra 211: a boca sem casa devia pedir uma");

        Building house = new Building(UUID.randomUUID(), colonyId, new ResourceId("villagecolony", "colony/plains/miner_house"),
                cell(oldMouth.add(14, 0, -24)), cell(oldMouth.add(18, 4, -20)), true);
        UUID[] miners = {UUID.randomUUID(), UUID.randomUUID()};

        try {
            VillageColonyMod.BUILDINGS.register(house);

            context.assertFalse(MineHomePlans.isOwedAt(colonyId, oldMouth), "Regra 211: a boca com casa pediu outra");
            context.assertFalse(MineHomePlans.isOwedAt(colonyId, newMouth), "Regra 211: um mineiro só devia ter uma casa");

            for (UUID miner : miners) {
                VillageColonyMod.WORKERS.register(miner, colonyId).assign(com.villagecolony.core.worker.model.ProfessionType.MINER);
            }

            context.assertTrue(MineHomePlans.isOwedAt(colonyId, newMouth), "Regra 211: a casa não acompanhou a boca nova");
            context.assertFalse(MineHomePlans.isOwedAt(colonyId, oldMouth), "Regra 211: a boca antiga pediu outra casa");
        } finally {
            VillageColonyMod.BUILDINGS.removeOfColony(colonyId);
            VillageColonyMod.WORKERS.ofColony(colonyId).forEach(worker -> VillageColonyMod.WORKERS.remove(worker.villagerId()));
        }

        context.complete();
    }
}
