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
                    "a BigHouseMOD nao reservou oito baus distintos");

            context.assertTrue(
                    countBlocks(context, house, Blocks.WHITE_BED) == 16,
                    "BigHouseMOD deveria conter 8 camas completas");
            context.assertTrue(
                    countBlocks(context, house, Blocks.CHEST) == 8,
                    "BigHouseMOD deveria conter 8 baus");
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
