package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.core.worker.service.ProfessionAssigner;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.event.VillageDetectionHandler;
import com.villagecolony.fabric.integration.VillageFoundation;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;

import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Prova a fundação física e profissional de uma vila recém-detectada. */
public class VillageFoundationGameTest implements FabricGameTest {

    /** Sem uma casa pronta a fundação aguarda: nunca semeia camas ou moradores ao relento. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE,
            batchId = "aaa_village_foundation", tickLimit = 120)
    public void aVillageWithoutItsBigHouseDoesNotReceiveOutdoorBeds(TestContext context) {
        BlockPos anchor = context.getAbsolutePos(new BlockPos(1, 1, 1))
                .add(1000000, 0, 1009000);
        Colony colony = Colony.create(
                UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(anchor));
        VillageColonyMod.COLONIES.register(colony);
        prepareFoundationTerrain(context.getWorld(), anchor);

        try {
            VillageFoundation.Result result = VillageFoundation.ensure(
                    context.getWorld(), colony, colony.center(), VillageColonyMod.WORKERS, false);

            context.assertFalse(result.changed(),
                    "sem BigHouseMOD a fundação criou cama ou morador: " + result);
            context.assertTrue(countBlocks(context.getWorld(), anchor, 20, Blocks.WHITE_BED) == 0,
                    "sem BigHouseMOD nasceram camas ao ar livre");
        } finally {
            cleanUp(context.getWorld(), colony, anchor);
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE,
            batchId = "aaa_village_foundation", tickLimit = 120)
    public void aNewVillageReceivesEveryProfessionHomeAndChest(TestContext context) {
        BlockPos anchor = context.getAbsolutePos(new BlockPos(1, 1, 1))
                .add(1000000, 0, 1000000);
        Colony colony = Colony.create(
                UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(anchor));
        VillageColonyMod.COLONIES.register(colony);
        prepareFoundationTerrain(context.getWorld(), anchor);

        try {
            VillageDetectionHandler.runFoundationNow(context.getWorld(), colony);

            Building house = VillageColonyMod.BUILDINGS.ofColony(colony.id()).stream()
                    .filter(building -> building.blueprint().equals(
                            StructureBlueprintReader.BIG_HOUSE_MOD))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("BigHouseMOD nao foi registrada"));

            List<Worker> crew = VillageColonyMod.WORKERS.ofColony(colony.id());
            context.assertTrue(
                    crew.size() >= ProfessionAssigner.FOUNDATION_ORDER.size(),
                    "a fundacao registrou " + crew.size() + " trabalhadores");

            Set<ColonyPos> assignedChests = new HashSet<>();
            for (ProfessionType role : ProfessionAssigner.FOUNDATION_ORDER) {
                Worker worker = workerWith(crew, role);
                VillagerEntity villager = villagerOf(context.getWorld(), worker.villagerId());

                context.assertTrue(
                        villager != null && villager.isAlive(),
                        role + " nao tem aldeao vivo");
                context.assertTrue(
                        hasBedHome(context.getWorld(), villager),
                        role + " nao recebeu cama vinculada");
                context.assertTrue(
                        VillageColonyMod.STORAGES.hasStorage(worker.villagerId()),
                        role + " nao recebeu bau proprio");

                assertStorageInsideHouse(context, house, worker.villagerId());
                ColonyPos chest = VillageColonyMod.STORAGES.of(worker.villagerId())
                        .orElseThrow(() -> new AssertionError("bau ausente"))
                        .chestPosition();
                context.assertTrue(
                        assignedChests.add(chest),
                        role + " compartilhou o bau " + chest);
            }

            context.assertTrue(
                    assignedChests.size() == ProfessionAssigner.FOUNDATION_ORDER.size(),
                    "a BigHouseMOD nao reservou sete baus distintos");

            context.assertTrue(
                    countBlocks(context, house, Blocks.WHITE_BED) == 14,
                    "BigHouseMOD deveria conter 7 camas completas");
            context.assertTrue(
                    countBlocks(context, house, Blocks.CHEST) == 7,
                    "BigHouseMOD deveria conter 7 baus");
            context.assertTrue(
                    countBlocks(context, house, Blocks.JIGSAW) == 0
                            && countBlocks(context, house, Blocks.STRUCTURE_BLOCK) == 0
                            && countBlocks(context, house, Blocks.STRUCTURE_VOID) == 0,
                    "BigHouseMOD nao pode colocar blocos de geracao no mundo");
        } finally {
            List<UUID> workerIds = VillageColonyMod.WORKERS.ofColony(colony.id())
                    .stream()
                    .map(Worker::villagerId)
                    .toList();

            for (UUID workerId : workerIds) {
                VillagerEntity worker = villagerOf(context.getWorld(), workerId);
                if (worker != null) {
                    worker.discard();
                }
            }

            ColonyFixture fixture = ColonyFixture.create().owning(colony);
            workerIds.forEach(fixture::owning);
            fixture.cleanUp();
        }

        context.complete();
    }

    /**
     * Regra 298: a fundação conta os adultos da vila primeiro e só faz nascer o que falta para os 8 ofícios — com 3 na
     * vila, nascem 5, cada um numa cama da BigHouseMOD com o bilhete tomado; a cama que sobra pode ir a um titular da
     * vila.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE,
            batchId = "aaa_village_foundation", tickLimit = 120)
    public void theFoundationOnlyFillsUpToEightAdults(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos anchor = context.getAbsolutePos(new BlockPos(1, 1, 1))
                .add(1000000, 0, 1003000);
        Colony colony = Colony.create(
                UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(anchor));
        VillageColonyMod.COLONIES.register(colony);
        prepareFoundationTerrain(world, anchor);
        spawnLocals(world, anchor, 3);

        try {
            VillageDetectionHandler.runFoundationNow(world, colony);

            Building house = houseOf(colony);
            List<BlockPos> beds = bedHeads(world, house);
            long taken = beds.stream().filter(bed -> world.getPointOfInterestStorage().getType(bed).isPresent()
                    && world.getPointOfInterestStorage().getFreeTickets(bed) == 0).count();

            context.assertTrue(beds.size() == 7,
                    "a BigHouseMOD deveria ter 7 camas, tem " + beds.size());
            context.assertTrue(VillageColonyMod.WORKERS.ofColony(colony.id()).size() == 8,
                    "os 3 adultos da vila e os nascidos deviam somar 8, somam "
                            + VillageColonyMod.WORKERS.ofColony(colony.id()).size());
            int residents = residents(world, house).size();

            context.assertTrue(residents >= 5 && residents <= beds.size() && taken == residents,
                    "deviam morar de 5 a 7 nas camas, cada um com a cama tomada: moram " + residents
                            + " com " + taken + " camas tomadas");
        } finally {
            cleanUp(world, colony, anchor);
        }

        context.complete();
    }

    /** Regra 298: a vila que já tem 9 adultos não ganha ninguém na fundação. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE,
            batchId = "aaa_village_foundation", tickLimit = 120)
    public void aVillageWithEightAdultsGetsNoNewborn(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos anchor = context.getAbsolutePos(new BlockPos(1, 1, 1))
                .add(1000000, 0, 1006000);
        Colony colony = Colony.create(
                UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(anchor));
        VillageColonyMod.COLONIES.register(colony);
        prepareFoundationTerrain(world, anchor);
        spawnLocals(world, anchor, 9);

        try {
            VillageDetectionHandler.runFoundationNow(world, colony);

            context.assertTrue(VillageColonyMod.WORKERS.ofColony(colony.id()).size() == 9,
                    "a vila de 9 adultos ganhou gente na fundação: "
                            + VillageColonyMod.WORKERS.ofColony(colony.id()).size() + " aldeões");
        } finally {
            cleanUp(world, colony, anchor);
        }

        context.complete();
    }

    /** Adultos da vila, longe da casa e uns ao lado dos outros. */
    private static void spawnLocals(ServerWorld world, BlockPos anchor, int count) {
        for (int index = 0; index < count; index++) {
            VillagerEntity local = net.minecraft.entity.EntityType.VILLAGER.create(world);
            local.refreshPositionAndAngles(anchor.add(-18 + index, 0, -18), 0.0F, 0.0F);
            world.spawnEntity(local);
        }
    }

    /** A vila nova nasce com um aldeão de cada profissão: o que falta para os 8 nasce adulto (Regra 259). */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE,
            batchId = "aaa_village_foundation", tickLimit = 120)
    public void aNewVillageGetsOneVillagerOfEveryProfession(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos anchor = context.getAbsolutePos(new BlockPos(1, 1, 1))
                .add(1000000, 0, 1009000);
        Colony colony = Colony.create(
                UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(anchor));
        VillageColonyMod.COLONIES.register(colony);
        prepareFoundationTerrain(world, anchor);

        try {
            VillageDetectionHandler.runFoundationNow(world, colony);

            java.util.Set<com.villagecolony.core.worker.model.ProfessionType> trades =
                    java.util.EnumSet.noneOf(com.villagecolony.core.worker.model.ProfessionType.class);

            VillageColonyMod.WORKERS.ofColony(colony.id())
                    .forEach(worker -> worker.profession().ifPresent(trades::add));

            // Os oito ofícios da primeira leva; o Zelador vem do adulto que passa deles (Regra 330).
            for (com.villagecolony.core.worker.model.ProfessionType type
                    : com.villagecolony.core.worker.service.ProfessionAssigner.GROWTH_ORDER) {
                context.assertTrue(trades.contains(type), "a vila nova nasceu sem " + type + ": " + trades
                        + " em " + VillageColonyMod.WORKERS.ofColony(colony.id()).size() + " aldeões");
            }
        } finally {
            cleanUp(world, colony, anchor);
        }

        context.complete();
    }

    /** N1, 2026-09-24: quem morre não é reposto pela passagem seguinte. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE,
            batchId = "aaa_village_foundation", tickLimit = 120)
    public void aDeadResidentIsNotReplacedByTheNextPass(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos anchor = context.getAbsolutePos(new BlockPos(1, 1, 1))
                .add(1000000, 0, 1006000);
        Colony colony = Colony.create(
                UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(anchor));
        VillageColonyMod.COLONIES.register(colony);
        prepareFoundationTerrain(world, anchor);

        try {
            VillageDetectionHandler.runFoundationNow(world, colony);

            Building house = houseOf(colony);
            List<VillagerEntity> before = residents(world, house);

            context.assertTrue(before.size() == 7,
                    "a fundacao deveria criar 7 moradores, criou " + before.size());

            VillagerEntity victim = before.get(0);
            BlockPos victimBed = victim.getBrain()
                    .getOptionalRegisteredMemory(MemoryModuleType.HOME)
                    .orElseThrow()
                    .pos();

            java.util.Set<UUID> alive = new java.util.HashSet<>();

            allVillagers(world, house).forEach(villager -> alive.add(villager.getUuid()));
            victim.kill();
            VillageDetectionHandler.runFoundationNow(world, colony);

            // Ninguém nasce no lugar do morto. A cama dele fica livre, ou vai para um aldeão que já morava na vila sem
            // cama (o fazendeiro, na casa grande da Regra 369, que tem baú ao lado de cada cama).
            List<VillagerEntity> after = allVillagers(world, house);

            context.assertTrue(after.size() == alive.size() - 1
                            && after.stream().allMatch(villager -> alive.contains(villager.getUuid())),
                    "a segunda passagem repos o morto: " + after.size() + " vivos, eram " + alive.size());
            context.assertTrue(
                    world.getPointOfInterestStorage().getFreeTickets(victimBed) == 1
                            || after.stream().anyMatch(villager -> villager.getBrain()
                                    .getOptionalRegisteredMemory(MemoryModuleType.HOME)
                                    .filter(home -> home.pos().equals(victimBed)).isPresent()),
                    "a cama do morto nao ficou livre nem com um morador da vila");
        } finally {
            cleanUp(world, colony, anchor);
        }

        context.complete();
    }

    private static Building houseOf(Colony colony) {
        return VillageColonyMod.BUILDINGS.ofColony(colony.id()).stream()
                .filter(building -> building.blueprint().equals(
                        StructureBlueprintReader.BIG_HOUSE_MOD))
                .findFirst()
                .orElseThrow(() -> new AssertionError("BigHouseMOD nao foi registrada"));
    }

    /** A cabeça de cada cama: é onde o Vanilla registra o ponto de interesse. */
    private static List<BlockPos> bedHeads(ServerWorld world, Building house) {
        List<BlockPos> feet = new java.util.ArrayList<>();

        for (int x = house.min().x(); x <= house.max().x(); x++) {
            for (int y = house.min().y(); y <= house.max().y(); y++) {
                for (int z = house.min().z(); z <= house.max().z(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    net.minecraft.block.BlockState state = world.getBlockState(pos);

                    if (state.getBlock() instanceof BedBlock
                            && state.get(net.minecraft.state.property.Properties.BED_PART)
                            == net.minecraft.block.enums.BedPart.HEAD) {
                        feet.add(pos);
                    }
                }
            }
        }

        return feet;
    }

    /** Todos os aldeões vivos em volta da casa, com cama ou sem. */
    private static List<VillagerEntity> allVillagers(ServerWorld world, Building house) {
        return world.getEntitiesByClass(VillagerEntity.class, new net.minecraft.util.math.Box(
                MinecraftTypeAdapter.toBlockPos(house.min()).toCenterPos(),
                MinecraftTypeAdapter.toBlockPos(house.max()).toCenterPos()).expand(40.0), VillagerEntity::isAlive);
    }

    /** Aldeões vivos cuja cama fica dentro da casa. */
    private static List<VillagerEntity> residents(ServerWorld world, Building house) {
        BlockPos min = MinecraftTypeAdapter.toBlockPos(house.min());
        BlockPos max = MinecraftTypeAdapter.toBlockPos(house.max());
        net.minecraft.util.math.Box area = new net.minecraft.util.math.Box(
                min.toCenterPos(), max.toCenterPos()).expand(40.0);

        return world.getEntitiesByClass(VillagerEntity.class, area, VillagerEntity::isAlive)
                .stream()
                .filter(villager -> villager.getBrain()
                        .getOptionalRegisteredMemory(MemoryModuleType.HOME)
                        .map(home -> house.contains(MinecraftTypeAdapter.toColonyPos(home.pos())))
                        .orElse(false))
                .toList();
    }

    private static void cleanUp(ServerWorld world, Colony colony, BlockPos anchor) {
        List<UUID> workerIds = VillageColonyMod.WORKERS.ofColony(colony.id())
                .stream()
                .map(Worker::villagerId)
                .toList();

        world.getEntitiesByClass(VillagerEntity.class,
                        net.minecraft.util.math.Box.of(anchor.toCenterPos(), 90.0, 40.0, 90.0),
                        villager -> true)
                .forEach(VillagerEntity::discard);

        ColonyFixture fixture = ColonyFixture.create().owning(colony);
        workerIds.forEach(fixture::owning);
        fixture.cleanUp();
    }

    private static void prepareFoundationTerrain(ServerWorld world, BlockPos anchor) {
        world.getChunk(anchor);

        for (int dx = -20; dx <= 20; dx++) {
            for (int dz = -20; dz <= 20; dz++) {
                BlockPos floor = anchor.add(dx, -1, dz);
                world.setBlockState(floor, net.minecraft.block.Blocks.STONE.getDefaultState());

                for (int dy = 0; dy <= 12; dy++) {
                    world.setBlockState(
                            anchor.add(dx, dy, dz),
                            net.minecraft.block.Blocks.AIR.getDefaultState());
                }
            }
        }
    }

    private static Worker workerWith(List<Worker> workers, ProfessionType role) {
        return workers.stream()
                .filter(worker -> worker.profession().filter(role::equals).isPresent())
                .findFirst()
                .orElseThrow(() -> new AssertionError("funcao ausente: " + role));
    }

    private static VillagerEntity villagerOf(ServerWorld world, UUID id) {
        return (VillagerEntity) world.getEntity(id);
    }

    private static boolean hasBedHome(ServerWorld world, VillagerEntity villager) {
        return villager.getBrain()
                .getOptionalRegisteredMemory(MemoryModuleType.HOME)
                .filter(home -> home.dimension().equals(world.getRegistryKey()))
                .map(GlobalPos::pos)
                .map(world::getBlockState)
                .map(state -> state.getBlock() instanceof BedBlock)
                .orElse(false);
    }

    private static int countBlocks(
            TestContext context, Building house, net.minecraft.block.Block target) {
        int count = 0;

        for (int x = house.min().x(); x <= house.max().x(); x++) {
            for (int y = house.min().y(); y <= house.max().y(); y++) {
                for (int z = house.min().z(); z <= house.max().z(); z++) {
                    if (context.getWorld().getBlockState(new BlockPos(x, y, z))
                            .isOf(target)) {
                        count++;
                    }
                }
            }
        }

        return count;
    }

    private static int countBlocks(ServerWorld world, BlockPos center, int radius, net.minecraft.block.Block target) {
        int count = 0;
        for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
            for (int y = center.getY(); y <= center.getY() + 4; y++) {
                for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                    if (world.getBlockState(new BlockPos(x, y, z)).isOf(target)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static void assertStorageInsideHouse(
            TestContext context, Building house, UUID workerId) {
        ColonyPos storage = VillageColonyMod.STORAGES.of(workerId)
                .orElseThrow(() -> new AssertionError("bau ausente"))
                .chestPosition();

        context.assertTrue(
                house.contains(storage),
                "o bau do aldeao " + workerId + " ficou fora da BigHouseMOD");
    }
}
