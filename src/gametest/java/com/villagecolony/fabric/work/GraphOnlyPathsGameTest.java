package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.construction.model.PathStep;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.PathProposals;
import com.villagecolony.fabric.integration.WorldPathTerrain;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Em vila nova, só o grafo traça o caminho (Regra 214), e o pedreiro ocioso ajuda a calçar (Regra 215). */
public class GraphOnlyPathsGameTest implements FabricGameTest {

    private static final int SIZE = 40;

    private static void platform(ServerWorld world, BlockPos origin) {
        for (BlockPos at : BlockPos.iterate(origin, origin.add(SIZE, 0, SIZE))) {
            world.getChunk(at);
            world.setBlockState(at, Blocks.GRASS_BLOCK.getDefaultState(), 18);
            world.setBlockState(at.down(), Blocks.STONE.getDefaultState(), 18);
        }
    }

    private static void clean(ServerWorld world, BlockPos origin) {
        for (BlockPos at : BlockPos.iterate(origin.down(), origin.add(SIZE, 5, SIZE))) {
            world.setBlockState(at, Blocks.AIR.getDefaultState(), 18);
        }
    }

    private static ColonyPos cell(BlockPos at) {
        return MinecraftTypeAdapter.toColonyPos(at);
    }

    /** O construtor está ausente: o pedreiro sem tarefa pega a calçada. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "graph_only_mason", tickLimit = 20)
    public void anIdleMasonPavesThePath(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 60, 1));

        platform(world, origin);

        ColonyFixture fixture = ColonyFixture.colonyAtWorld(origin.add(20, 1, 20));
        Colony colony = fixture.colony();
        long before = world.getTimeOfDay();

        try {
            world.setTimeOfDay(2_000);

            var mason = fixture.equippedWorker(context, ProfessionType.MASON, BlockPos.ORIGIN.up());
            BlockPos floor = origin.add(6, 0, 5);

            // A arena pode vir girada: a posição vem do mundo.
            world.getEntity(mason.villagerId()).refreshPositionAndAngles(floor.getX() - 0.5, floor.getY() + 1, floor.getZ() + 0.5, 0, 0);

            PathLinks.offerRepair(colony.id(), List.of(new PathStep(floor.getX(), floor.getY(), floor.getZ(), Side.EAST, 0, 0)));
            PathPaving.pass(world, colony);

            context.assertTrue(world.getBlockState(floor).isOf(Blocks.DIRT_PATH), "Regra 215: o pedreiro ocioso não calçou; vez=" + PathPaving.isPaving(mason.villagerId()) + " plano=" + PathLinks.planOf(colony.id()).isPresent() + " livre=" + world.getEntity(mason.villagerId()));
            context.assertTrue(mason.profession().filter(ProfessionType.MASON::equals).isPresent(), "o trabalhador não é pedreiro");
        } finally {
            world.setTimeOfDay(before);
            PathLinks.drop(colony.id());
            fixture.cleanUp();
            clean(world, origin);
        }

        context.complete();
    }

    /** Quem não tem ofício também calça: é o trabalho dele na vila — Regra 220. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "graph_only_none", tickLimit = 20)
    public void anAdultWithoutATradePavesThePath(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 84, 1));

        platform(world, origin);

        ColonyFixture fixture = ColonyFixture.colonyAtWorld(origin.add(20, 1, 20));
        Colony colony = fixture.colony();
        long before = world.getTimeOfDay();

        try {
            world.setTimeOfDay(2_000);

            VillagerEntity adult = context.spawnEntity(EntityType.VILLAGER, BlockPos.ORIGIN.up());

            adult.setBreedingAge(0);
            VillageColonyMod.WORKERS.register(adult.getUuid(), colony.id());
            fixture.owning(adult.getUuid());

            BlockPos floor = origin.add(6, 0, 5);

            adult.refreshPositionAndAngles(floor.getX() - 0.5, floor.getY() + 1, floor.getZ() + 0.5, 0.0F, 0.0F);
            PathLinks.offerRepair(colony.id(), List.of(new PathStep(floor.getX(), floor.getY(), floor.getZ(), Side.EAST, 0, 0)));
            PathPaving.pass(world, colony);

            context.assertTrue(world.getBlockState(floor).isOf(Blocks.DIRT_PATH), "Regra 220: quem não tem ofício não calçou");
        } finally {
            world.setTimeOfDay(before);
            PathLinks.drop(colony.id());
            fixture.cleanUp();
            clean(world, origin);
        }

        context.complete();
    }

    /** O carpinteiro continua de fora: a ajuda é só do pedreiro. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "graph_only_carpenter", tickLimit = 20)
    public void aCarpenterDoesNotPave(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 72, 1));

        platform(world, origin);

        ColonyFixture fixture = ColonyFixture.colonyAtWorld(origin.add(20, 1, 20));
        Colony colony = fixture.colony();
        long before = world.getTimeOfDay();

        try {
            world.setTimeOfDay(2_000);
            var carpenter = fixture.equippedWorker(context, ProfessionType.CARPENTER, BlockPos.ORIGIN.up());
            BlockPos floor = origin.add(6, 0, 5);

            world.getEntity(carpenter.villagerId()).refreshPositionAndAngles(floor.getX() - 0.5, floor.getY() + 1, floor.getZ() + 0.5, 0, 0);

            PathLinks.offerRepair(colony.id(), List.of(new PathStep(floor.getX(), floor.getY(), floor.getZ(), Side.EAST, 0, 0)));
            PathPaving.pass(world, colony);

            context.assertFalse(world.getBlockState(floor).isOf(Blocks.DIRT_PATH), "Regra 215: o carpinteiro calçou");
        } finally {
            world.setTimeOfDay(before);
            PathLinks.drop(colony.id());
            fixture.cleanUp();
            clean(world, origin);
        }

        context.complete();
    }

    /** Sem portão de pé (Regra 3), a mina ainda é entrada: a borda da folga da escada, do lado do centro da vila. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "graph_only_mouth", tickLimit = 20)
    public void aMineWithoutGateIsStillAnEntrance(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 96, 1));

        platform(world, origin);

        ColonyFixture fixture = ColonyFixture.colonyAtWorld(origin.add(20, 1, 2));
        Colony colony = fixture.colony();

        try {
            BlockPos mouth = origin.add(20, 1, 25);

            VillageColonyMod.MINES.open(colony.id(), MineShaft.from(cell(mouth), Side.SOUTH));

            WorldPathTerrain terrain = new WorldPathTerrain(world, colony.id());
            Set<ColonyPos> fronts = MineGates.fronts(world, colony, terrain);

            context.assertTrue(fronts.size() == 1, "Regra 214: a mina sem portão ficou sem entrada: " + fronts);

            ColonyPos front = fronts.iterator().next();

            context.assertFalse(terrain.blocked(front.x(), front.z()), "a entrada caiu na folga da escada: " + front);
            context.assertTrue(front.z() < mouth.getZ(), "a entrada não ficou do lado do centro da vila: " + front);
        } finally {
            VillageColonyMod.MINES.removeOfColony(colony.id());
            fixture.cleanUp();
            clean(world, origin);
        }

        context.complete();
    }

    /** A falha no meio da rua é pedida ao grafo: ninguém calça fora dele (Regra 214). */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "graph_only_gap", tickLimit = 20)
    public void aGapIsAskedOfTheGraphAndNotPaved(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 108, 1));

        platform(world, origin);

        Colony colony = Colony.create(UUID.randomUUID(), cell(origin.add(5, 1, 5)));

        colony.measure(new com.villagecolony.core.colony.model.VillageBounds(origin.getX(), origin.getY() - 2, origin.getZ(),
                origin.getX() + 12, origin.getY() + 4, origin.getZ() + 12));
        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture fixture = ColonyFixture.create().owning(colony);
        long before = world.getTimeOfDay();

        try {
            world.setTimeOfDay(2_000);

            var builder = fixture.equippedWorker(context, ProfessionType.BUILDER, BlockPos.ORIGIN.up());
            BlockPos gap = origin.add(6, 0, 6);

            world.setBlockState(gap.west(), Blocks.DIRT_PATH.getDefaultState(), 18);
            world.setBlockState(gap.east(), Blocks.DIRT_PATH.getDefaultState(), 18);
            world.getEntity(builder.villagerId()).refreshPositionAndAngles(gap.getX() + 0.5, gap.getY() + 1, gap.getZ() + 0.5, 0, 0);

            context.assertTrue(BuilderPaving.isGap(world, gap), "a falha não foi vista");

            for (int pass = 0; pass < 3; pass++) {
                BuilderPaving.pass(world, colony);
            }

            context.assertTrue(PathProposals.isWanted(colony.id(), gap.getX(), gap.getZ()), "Regra 214: a falha não foi pedida ao grafo");
            context.assertFalse(world.getBlockState(gap).isOf(Blocks.DIRT_PATH), "Regra 214: o construtor calçou fora do grafo");
        } finally {
            world.setTimeOfDay(before);
            PathProposals.drain(colony.id());
            fixture.cleanUp();
            clean(world, origin);
        }

        context.complete();
    }

    /** Uma ajuda de cada vez: quem está calçando não está livre para outra, e continua livre para a própria. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "graph_only_idle", tickLimit = 20)
    public void aWorkerOnAHelpIsNotFreeForAnother(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 120, 1));

        platform(world, origin);

        ColonyFixture fixture = ColonyFixture.colonyAtWorld(origin.add(20, 1, 20));
        Colony colony = fixture.colony();
        long before = world.getTimeOfDay();

        try {
            world.setTimeOfDay(2_000);

            var mason = fixture.equippedWorker(context, ProfessionType.MASON, BlockPos.ORIGIN.up());
            var entity = (net.minecraft.entity.passive.VillagerEntity) world.getEntity(mason.villagerId());
            BlockPos far = origin.add(30, 0, 30);

            entity.refreshPositionAndAngles(origin.getX() + 0.5, origin.getY() + 1, origin.getZ() + 0.5, 0, 0);

            context.assertTrue(IdleWorkers.isFree(world, entity, null), "o pedreiro sem tarefa devia estar livre");

            PathLinks.offerRepair(colony.id(), List.of(new PathStep(far.getX(), far.getY(), far.getZ(), Side.EAST, 0, 0)));
            PathPaving.pass(world, colony);

            context.assertTrue(PathPaving.isPaving(mason.villagerId()), "ele não pegou a calçada");
            context.assertFalse(IdleWorkers.isFree(world, entity, null), "quem calça ficou livre para outra ajuda");
            context.assertTrue(IdleWorkers.isFree(world, entity, IdleWorkers.Help.PATH), "quem calça perdeu a própria ajuda");
        } finally {
            world.setTimeOfDay(before);
            PathLinks.drop(colony.id());
            PathPaving.clearAll();
            fixture.cleanUp();
            clean(world, origin);
        }

        context.complete();
    }

    /** Quem pergunta pela rede ouve o grafo: a entrada solta e o caminho fora da linha não são rede (Regra 216). */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "graph_only_network", tickLimit = 20)
    public void theNetworkIsTheGraphLines(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(1, 132, 1));

        platform(world, origin);

        ColonyFixture fixture = ColonyFixture.colonyAtWorld(origin.add(20, 1, 2));
        Colony colony = fixture.colony();

        try {
            Set<ColonyPos> paths = new java.util.HashSet<>();

            for (int x = 2; x <= 38; x++) {
                BlockPos at = origin.add(x, 0, 2);

                world.setBlockState(at, Blocks.DIRT_PATH.getDefaultState(), 18);
                paths.add(cell(at));
                com.villagecolony.fabric.integration.RoadIndex.remember(colony.id(), at);
            }

            RoadGraphRefresh.publish(world, colony, paths);

            var network = com.villagecolony.fabric.integration.RoadNetwork.of(colony.id()).orElseThrow();
            long line = com.villagecolony.core.construction.model.ColonyRoads.column(origin.getX() + 20, origin.getZ() + 2);

            context.assertTrue(network.columns().contains(line), "Regra 216: a linha do grafo não virou rede");
        } finally {
            com.villagecolony.fabric.integration.RoadNetwork.publish(colony.id(), colony.center(), List.of());
            fixture.cleanUp();
            clean(world, origin);
        }

        context.complete();
    }
}
