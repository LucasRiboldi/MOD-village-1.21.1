package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.storage.service.StorageRegistry;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestSpawner;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.GlobalPos;

import java.util.Optional;
import java.util.UUID;

/**
 * Todo trabalhador de profissão tem baú — decisão do autor, 2026-09-26. Na
 * sessão longa daquele dia, lenhador, mineiro e pedreiro passaram 4h45 sem baú
 * e, portanto, sem tarefa.
 */
public class ChestSpawnerGameTest implements FabricGameTest {

    private static void floor(TestContext context) {
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 8; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.STONE.getDefaultState());
            }
        }
    }

    /** Sem cama dentro de uma estrutura, o trabalhador não recebe um baú no centro da vila. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "chest_spawner", tickLimit = 20)
    public void aWorkerWithoutABedDoesNotGetACentreChest(TestContext context) {
        floor(context);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(1, 2, 1));
        StorageRegistry storages = new StorageRegistry();

        Optional<WorkerStorage> chest = ChestSpawner.ensureChest(
                context.getWorld(), villager, storages, "LUMBERJACK");

        context.assertTrue(chest.isEmpty(), "um baú nasceu no centro sem cama em estrutura");
        context.assertFalse(storages.hasStorage(villager.getUuid()), "o baú sem estrutura foi registrado");

        // Pedir de novo também não cria um baú solto.
        Optional<WorkerStorage> again = ChestSpawner.ensureChest(
                context.getWorld(), villager, storages, "LUMBERJACK");
        context.assertTrue(again.isEmpty(), "uma segunda tentativa criou um baú no centro");
        context.complete();
    }

    /** Com cama, o baú vai ao lado dela e encostado numa parede — a regra (b). */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "chest_spawner", tickLimit = 20)
    public void aWorkerWithABedGetsItsChestBesideTheBed(TestContext context) {
        floor(context);

        BlockPos bed = new BlockPos(3, 2, 4);
        context.setBlockState(bed, Blocks.RED_BED.getDefaultState()
                .with(Properties.BED_PART, BedPart.FOOT).with(Properties.HORIZONTAL_FACING, Direction.NORTH));
        context.setBlockState(bed.north(), Blocks.RED_BED.getDefaultState()
                .with(Properties.BED_PART, BedPart.HEAD).with(Properties.HORIZONTAL_FACING, Direction.NORTH));
        context.setBlockState(bed.east(2), Blocks.STONE.getDefaultState());

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(1, 2, 1));
        villager.getBrain().remember(MemoryModuleType.HOME,
                GlobalPos.create(context.getWorld().getRegistryKey(), context.getAbsolutePos(bed)));

        UUID colony = UUID.randomUUID();
        BlockPos houseMin = context.getAbsolutePos(new BlockPos(2, 1, 3));
        BlockPos houseMax = context.getAbsolutePos(new BlockPos(5, 4, 5));
        VillageColonyMod.BUILDINGS.register(new Building(
                UUID.randomUUID(), colony, ResourceId.vanilla("test/worker_house"),
                MinecraftTypeAdapter.toColonyPos(houseMin), MinecraftTypeAdapter.toColonyPos(houseMax)));
        try {
            Optional<WorkerStorage> chest = ChestSpawner.ensureChest(
                    context.getWorld(), villager, new StorageRegistry(), "MINER");

            context.assertTrue(chest.isPresent()
                            && MinecraftTypeAdapter.toBlockPos(chest.get().chestPosition())
                                    .equals(context.getAbsolutePos(bed.east())),
                    "o baú do mineiro devia nascer dentro da casa, ao lado da cama e encostado na parede: "
                            + chest.map(WorkerStorage::chestPosition));
        } finally {
            VillageColonyMod.BUILDINGS.removeOfColony(colony);
        }
        context.complete();
    }

    /** Cama ao ar livre não autoriza um baú: ele pertence ao quarto, não ao centro da vila. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "chest_spawner_spots", tickLimit = 20)
    public void aWorkerWithAnOutdoorBedDoesNotGetAChest(TestContext context) {
        floor(context);

        BlockPos bed = new BlockPos(3, 2, 4);
        context.setBlockState(bed, Blocks.RED_BED.getDefaultState()
                .with(Properties.BED_PART, BedPart.FOOT).with(Properties.HORIZONTAL_FACING, Direction.NORTH));
        context.setBlockState(bed.north(), Blocks.RED_BED.getDefaultState()
                .with(Properties.BED_PART, BedPart.HEAD).with(Properties.HORIZONTAL_FACING, Direction.NORTH));
        context.setBlockState(bed.east(2), Blocks.STONE.getDefaultState());

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(1, 2, 1));
        villager.getBrain().remember(MemoryModuleType.HOME,
                GlobalPos.create(context.getWorld().getRegistryKey(), context.getAbsolutePos(bed)));

        StorageRegistry storages = new StorageRegistry();
        Optional<WorkerStorage> chest = ChestSpawner.ensureChest(
                context.getWorld(), villager, storages, "SHEPHERD");

        context.assertTrue(chest.isEmpty(), "a cama fora de uma estrutura recebeu um baú");
        context.assertFalse(storages.hasStorage(villager.getUuid()), "o baú externo foi registrado");
        context.complete();
    }
}
