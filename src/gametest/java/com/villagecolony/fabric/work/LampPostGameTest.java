package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.LampPostPlan;
import com.villagecolony.core.construction.model.RoadGraph;
import com.villagecolony.core.construction.model.RoadSettling;
import com.villagecolony.core.construction.model.VillagePalette;
import com.villagecolony.core.coordination.IdleReason;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.data.save.ColonySavedData;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.content.ModContent;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.VillageBiomes;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * O poste de luz — Regra 261 (autor, 2026-11-12): só com linha fixa do grafo, no bloco ao lado dela e na linha do solo,
 * com 2 blocos livres de estrutura em volta, céu aberto e 30 blocos de outro poste; erguido pelo construtor, com a tocha da
 * colônia no topo.
 *
 * <p>Longe da arena, como {@code ColonyTorchesGameTest}: a tocha e o poste de um teste vizinho contariam nos 30 blocos.
 */
public class LampPostGameTest implements FabricGameTest {

    private static final int SIZE = 70;

    /** A rua: de x = 0 a x = 64, em z = 20; o centro da vila na ponta oeste. */
    private static final int LINE_Z = 20;

    private static final int LINE_END = 64;

    private static BlockPos originOf(TestContext context, int which) {
        return context.getAbsolutePos(BlockPos.ORIGIN).add(4_400_000 + which * 1_000, 40, 4_400_000);
    }

    private static void chunks(ServerWorld world, BlockPos origin, boolean forced) {
        for (int x = (origin.getX() - 16) >> 4; x <= (origin.getX() + SIZE) >> 4; x++) {
            for (int z = (origin.getZ() - 16) >> 4; z <= (origin.getZ() + SIZE) >> 4; z++) {
                world.setChunkForced(x, z, forced);
            }
        }
    }

    private static void floor(ServerWorld world, BlockPos origin) {
        chunks(world, origin, true);

        for (BlockPos at : BlockPos.iterate(origin.down(), origin.add(SIZE, 0, SIZE))) {
            world.setBlockState(at, at.getY() < origin.getY() ? Blocks.DIRT.getDefaultState() : Blocks.GRASS_BLOCK.getDefaultState(), 18);
        }
    }

    private static void clean(ServerWorld world, BlockPos origin) {
        for (BlockPos at : BlockPos.iterate(origin.down(), origin.add(SIZE, 8, SIZE))) {
            world.setBlockState(at, Blocks.AIR.getDefaultState(), 18);
        }

        chunks(world, origin, false);
    }

    /** A rua calçada e o grafo dela, na linha há {@code age} dias em {@code today}. */
    private static void road(ServerWorld world, Colony colony, BlockPos origin, long today, long age) {
        Set<ColonyPos> line = new HashSet<>();
        Map<Long, Long> since = new HashMap<>();

        for (int x = 0; x <= LINE_END; x++) {
            BlockPos at = origin.add(x, 0, LINE_Z);

            world.setBlockState(at, Blocks.DIRT_PATH.getDefaultState(), 18);
            line.add(MinecraftTypeAdapter.toColonyPos(at));
            since.put(ColonyRoads.column(at.getX(), at.getZ()), today - age);
        }

        var store = ColonySavedData.get(world.getServer()).roadGraphs();
        ColonyPos hub = MinecraftTypeAdapter.toColonyPos(origin.add(0, 0, LINE_Z));
        ColonyPos door = MinecraftTypeAdapter.toColonyPos(origin.add(LINE_END, 0, LINE_Z));

        store.update(colony.id(), RoadGraph.from(line, hub, Set.of(door)));
        store.settle(colony.id(), since);
    }

    private static void forget(ServerWorld world, Colony colony, BlockPos origin) {
        var store = ColonySavedData.get(world.getServer()).roadGraphs();

        store.update(colony.id(), RoadGraph.from(Set.of(), MinecraftTypeAdapter.toColonyPos(origin), Set.of()));
        store.settle(colony.id(), Map.of());
    }

    private static ColonyFixture colonyAt(BlockPos origin) {
        return ColonyFixture.colonyAtWorld(origin.add(0, 1, LINE_Z));
    }

    /** Sem linha fixa não há poste; com ela, o poste fica no bloco ao lado da linha, na altura dela. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lamp_post", tickLimit = 20)
    public void aPostWaitsForAFixedLineAndStandsBesideIt(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = originOf(context, 0);

        floor(world, origin);

        ColonyFixture fixture = colonyAt(origin);
        Colony colony = fixture.colony();
        long today = 40;

        try {
            road(world, colony, origin, today, RoadSettling.DAYS - 1);

            context.assertTrue(LampPostSites.find(world, colony, today).isEmpty(),
                    "Regra 261: poste antes de a linha ficar fixa");

            road(world, colony, origin, today, RoadSettling.DAYS);

            Optional<BlockPos> site = LampPostSites.find(world, colony, today);

            context.assertTrue(site.isPresent(), "Regra 261: linha fixa sem poste");

            BlockPos base = site.get();

            context.assertTrue(base.getY() == origin.getY(), "Regra 261: a base não ficou na linha do solo: " + base);
            context.assertTrue(Math.abs(base.getZ() - (origin.getZ() + LINE_Z)) == 1
                            && base.getX() >= origin.getX() && base.getX() <= origin.getX() + LINE_END,
                    "Regra 261: a base não ficou no bloco ao lado da linha: " + base);
            context.assertTrue(world.getBlockState(base).isOf(Blocks.GRASS_BLOCK), "Regra 261: a base caiu fora do solo: " + base);

            context.assertTrue(LampPostWork.mayOpenAfter(IdleReason.NO_TARGET) && LampPostWork.mayOpenAfter(IdleReason.VILLAGE_COMPLETE),
                    "Regra 261: a vaga ociosa não abre poste");
            context.assertFalse(LampPostWork.mayOpenAfter(IdleReason.SWEEP_INCOMPLETE),
                    "Regra 261: o poste tomou a vaga com a busca de lote em curso");
        } finally {
            forget(world, colony, origin);
            fixture.cleanUp();
            clean(world, origin);
        }

        context.complete();
    }

    /** Estrutura a 2 blocos ou bloco acima da base tiram o lugar; o poste vai para o primeiro livre. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lamp_post", tickLimit = 20)
    public void aPostKeepsTwoBlocksClearAndTheSkyOpen(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = originOf(context, 1);

        floor(world, origin);

        ColonyFixture fixture = colonyAt(origin);
        Colony colony = fixture.colony();
        long today = 40;
        UUID house = UUID.randomUUID();

        try {
            road(world, colony, origin, today, RoadSettling.DAYS);

            // Ao norte, uma casa de x 0 a 6 e z 15 a 17: os dois blocos em volta vão até x 8 e z 19.
            VillageColonyMod.BUILDINGS.register(new Building(house, colony.id(), ResourceId.vanilla("village/plains/houses/test_house"),
                    MinecraftTypeAdapter.toColonyPos(origin.add(0, 1, 15)), MinecraftTypeAdapter.toColonyPos(origin.add(6, 5, 17)), true));

            // Ao sul, um teto de pedra a 6 blocos sobre a coluna da base, de x 0 a 10.
            for (int x = 0; x <= 10; x++) {
                world.setBlockState(origin.add(x, 6, LINE_Z + 1), Blocks.STONE.getDefaultState(), 18);
            }

            Optional<BlockPos> site = LampPostSites.find(world, colony, today);

            context.assertTrue(site.isPresent(), "Regra 261: nenhum lugar livre na rua");
            context.assertTrue(site.get().equals(origin.add(9, 0, LINE_Z - 1)),
                    "Regra 261: o poste não respeitou os 2 blocos da casa ou o céu aberto: " + site.get().subtract(origin));
        } finally {
            forget(world, colony, origin);
            fixture.cleanUp();
            clean(world, origin);
        }

        context.complete();
    }

    /** A 30 blocos ou mais de outro poste: a tocha da colônia de pé e o poste da vila contam. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lamp_post", tickLimit = 20)
    public void aPostStandsThirtyBlocksFromAnotherPost(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = originOf(context, 2);

        floor(world, origin);

        ColonyFixture fixture = colonyAt(origin);
        Colony colony = fixture.colony();
        long today = 40;
        UUID post = UUID.randomUUID();

        try {
            road(world, colony, origin, today, RoadSettling.DAYS);

            // Uma tocha da colônia já de pé ao lado da ponta oeste da rua.
            world.setBlockState(origin.add(0, 1, LINE_Z - 1), ModContent.COLONY_TORCH.getDefaultState(), 3);

            Optional<BlockPos> first = LampPostSites.find(world, colony, today);

            context.assertTrue(first.isPresent() && first.get().equals(origin.add(30, 0, LINE_Z - 1)),
                    "Regra 261: o poste não ficou a 30 blocos da tocha: " + first.map(at -> at.subtract(origin)));

            // O poste da vila já de pé ali conta, mesmo sem a tocha no mundo.
            BlockPos at = first.orElseThrow();
            Blueprint plan = LampPostPlan.blueprint(VillagePalette.COBBLESTONE, ResourceId.vanilla("oak_fence"));

            VillageColonyMod.BUILDINGS.register(new Building(post, colony.id(), plan.id(),
                    MinecraftTypeAdapter.toColonyPos(at), MinecraftTypeAdapter.toColonyPos(at.up(3)), true));

            Optional<BlockPos> second = LampPostSites.find(world, colony, today);

            context.assertTrue(second.isPresent() && second.get().equals(origin.add(60, 0, LINE_Z - 1)),
                    "Regra 261: o segundo poste não ficou a 30 blocos do primeiro: " + second.map(found -> found.subtract(origin)));
        } finally {
            forget(world, colony, origin);
            fixture.cleanUp();
            clean(world, origin);
        }

        context.complete();
    }

    /** O construtor ergue o poste com o material dos baús: pedra do bioma, duas cercas e a tocha da colônia no topo. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lamp_post_build", tickLimit = 400)
    public void theBuilderRaisesThePostWithTheColonyTorchOnTop(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = originOf(context, 3);

        floor(world, origin);
        world.setTimeOfDay(net.minecraft.entity.ai.brain.Schedule.WORK_TIME);

        ColonyFixture fixture = colonyAt(origin);
        Colony colony = fixture.colony();
        long today = 40;

        road(world, colony, origin, today, RoadSettling.DAYS);

        BlockPos chestAt = origin.add(2, 1, LINE_Z + 2);

        world.setBlockState(chestAt, Blocks.CHEST.getDefaultState(), 3);

        VillagerEntity villager = ColonyFixture.spawnAtWorld(context, EntityType.VILLAGER, origin.add(1, 1, LINE_Z));

        villager.setBreedingAge(0);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());

        worker.assign(ProfessionType.BUILDER);

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(chestAt);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));
        fixture.owning(villager.getUuid());

        Optional<ConstructionProject> opened = LampPostWork.openNext(world, colony, 1, today);

        if (opened.isEmpty()) {
            forget(world, colony, origin);
            fixture.cleanUp();
            clean(world, origin);
            throw new net.minecraft.test.GameTestException("Regra 261: a vaga ociosa não abriu a obra do poste");
        }

        ConstructionProject project = opened.get();
        BlockPos base = MinecraftTypeAdapter.toBlockPos(project.origin());
        ColonyPos where = MinecraftTypeAdapter.toColonyPos(base);
        ResourceId stone = VillageBiomes.paletteAt(world, colony.center()).map(VillagePalette::stone).orElse(VillagePalette.COBBLESTONE);
        ResourceId fence = LampPostPlan.fenceOf(VillageBiomes.woodAt(world, colony.center()).orElse(ResourceId.vanilla("oak_planks")));

        context.assertTrue(project.blueprint().id().equals(LampPostPlan.idOf(stone, fence)),
                "Regra 261: o poste não é da pedra e da madeira do bioma: " + project.blueprint().id());

        for (BlueprintBlock block : project.blueprint().blocks()) {
            Item item = MinecraftTypeAdapter.toBlock(block.block()).orElseThrow().asItem();

            ChestDepositor.deposit(world, chest, item, 1);
        }

        for (Task task : VillageColonyMod.TASKS.ofColony(colony.id())) {
            if (task.type() == TaskType.BUILD && task.isOpen()) {
                task.reserveFor(villager.getUuid());
            }
        }

        BuilderWork.run(world, colony);

        context.runAtTick(300, () -> {
            try {
                context.assertTrue(world.getBlockState(base).isOf(MinecraftTypeAdapter.toBlock(stone).orElseThrow()),
                        "Regra 261: a base de pedra não foi posta na linha do solo: " + world.getBlockState(base));
                context.assertTrue(world.getBlockState(base.up()).isOf(MinecraftTypeAdapter.toBlock(fence).orElseThrow())
                                && world.getBlockState(base.up(2)).isOf(MinecraftTypeAdapter.toBlock(fence).orElseThrow()),
                        "Regra 261: as duas cercas não subiram: " + world.getBlockState(base.up()) + ", " + world.getBlockState(base.up(2)));
                context.assertTrue(world.getBlockState(base.up(3)).isOf(ModContent.COLONY_TORCH),
                        "Regra 261: a tocha da colônia não ficou no topo: " + world.getBlockState(base.up(3)));
                context.assertTrue(VillageColonyMod.BUILDINGS.ofColony(colony.id()).stream().anyMatch(building -> building.finished()
                                && LampPostPlan.isLampPost(building.blueprint()) && building.min().equals(where)),
                        "Regra 261: o poste pronto não virou construção da vila");
            } finally {
                forget(world, colony, origin);
                fixture.cleanUp();
                clean(world, origin);
            }

            context.complete();
        });
    }
}
