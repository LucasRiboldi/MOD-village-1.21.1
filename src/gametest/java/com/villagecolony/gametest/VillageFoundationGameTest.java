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
                    "a BigHouseMOD nao reservou seis baus distintos");

            context.assertTrue(
                    countBlocks(context, house, Blocks.WHITE_BED) == 12,
                    "BigHouseMOD deveria conter 6 camas completas");
            context.assertTrue(
                    countBlocks(context, house, Blocks.CHEST) == 6,
                    "BigHouseMOD deveria conter 6 baus");
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
     * N1, 2026-09-24: cada cama da BigHouseMOD ganha o seu morador, mesmo
     * numa vila que já tinha adultos, e a cama sai com o bilhete tomado.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE,
            batchId = "aaa_village_foundation", tickLimit = 120)
    public void everyBedOfTheHouseGetsItsOwnNewborn(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos anchor = context.getAbsolutePos(new BlockPos(1, 1, 1))
                .add(1000000, 0, 1003000);
        Colony colony = Colony.create(
                UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(anchor));
        VillageColonyMod.COLONIES.register(colony);
        prepareFoundationTerrain(world, anchor);

        for (int index = 0; index < 3; index++) {
            VillagerEntity local = net.minecraft.entity.EntityType.VILLAGER.create(world);
            local.refreshPositionAndAngles(anchor.add(-18 + index, 0, -18), 0.0F, 0.0F);
            world.spawnEntity(local);
        }

        try {
            VillageDetectionHandler.runFoundationNow(world, colony);

            Building house = houseOf(colony);
            List<BlockPos> beds = bedHeads(world, house);

            context.assertTrue(beds.size() == 6,
                    "a BigHouseMOD deveria ter 6 camas, tem " + beds.size());
            context.assertTrue(residents(world, house).size() == beds.size(),
                    "nasceram " + residents(world, house).size() + " moradores para "
                            + beds.size() + " camas");
            context.assertTrue(VillageColonyMod.WORKERS.ofColony(colony.id()).size() >= 9,
                    "os 3 adultos da vila mais os 6 moradores deveriam somar 9");

            for (BlockPos bed : beds) {
                // Sem ponto de interesse, zero bilhete livre é o valor
                // padrão, e o caso não mediria nada — foi assim que a
                // primeira versão, olhando o pé da cama, passou vazia.
                context.assertTrue(
                        world.getPointOfInterestStorage().getType(bed).isPresent(),
                        "a cama " + bed.toShortString() + " nao e ponto de interesse");
                context.assertTrue(
                        world.getPointOfInterestStorage().getFreeTickets(bed) == 0,
                        "a cama " + bed.toShortString() + " ficou sem bilhete tomado");
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

            context.assertTrue(before.size() == 6,
                    "a fundacao deveria criar 6 moradores, criou " + before.size());

            VillagerEntity victim = before.get(0);
            BlockPos victimBed = victim.getBrain()
                    .getOptionalRegisteredMemory(MemoryModuleType.HOME)
                    .orElseThrow()
                    .pos();

            victim.kill();
            VillageDetectionHandler.runFoundationNow(world, colony);

            context.assertTrue(residents(world, house).size() == 5,
                    "a segunda passagem repos o morto: " + residents(world, house).size()
                            + " moradores vivos");
            context.assertTrue(
                    world.getPointOfInterestStorage().getFreeTickets(victimBed) == 1,
                    "a cama do morto nao voltou a ficar livre para a procriacao");
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
