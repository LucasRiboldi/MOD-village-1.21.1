package com.villagecolony.gametest;

import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.storage.service.StorageRegistry;
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

    /** Sem cama, o baú nasce no chão livre perto do centro e fica registrado para ele. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "chest_spawner", tickLimit = 20)
    public void aWorkerWithoutABedGetsAChestNearTheCentre(TestContext context) {
        floor(context);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(1, 2, 1));
        StorageRegistry storages = new StorageRegistry();
        BlockPos centre = context.getAbsolutePos(new BlockPos(4, 2, 4));

        Optional<WorkerStorage> chest = ChestSpawner.ensureChest(
                context.getWorld(), villager, storages, centre, "LUMBERJACK");

        context.assertTrue(chest.isPresent(), "o lenhador sem cama ficou sem baú");
        BlockPos at = MinecraftTypeAdapter.toBlockPos(chest.get().chestPosition());
        context.assertTrue(context.getWorld().getBlockState(at).isOf(Blocks.CHEST), "não há baú no mundo");
        context.assertTrue(storages.hasStorage(villager.getUuid()), "o baú não foi registrado para ele");

        // Pedir de novo não faz outro baú.
        Optional<WorkerStorage> again = ChestSpawner.ensureChest(
                context.getWorld(), villager, storages, centre, "LUMBERJACK");
        context.assertTrue(again.isPresent() && again.get().chestPosition().equals(chest.get().chestPosition()),
                "um segundo baú nasceu para o mesmo trabalhador");
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

        Optional<WorkerStorage> chest = ChestSpawner.ensureChest(
                context.getWorld(), villager, new StorageRegistry(),
                context.getAbsolutePos(new BlockPos(7, 2, 7)), "MINER");

        context.assertTrue(chest.isPresent()
                        && MinecraftTypeAdapter.toBlockPos(chest.get().chestPosition())
                                .equals(context.getAbsolutePos(bed.east())),
                "o baú do mineiro devia nascer ao lado da cama, encostado na parede: "
                        + chest.map(WorkerStorage::chestPosition));
        context.complete();
    }

    /** Rua de terra batida e baú encostado não servem: um vira obstáculo, o outro baú duplo. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "chest_spawner_spots", tickLimit = 20)
    public void theCentreChestAvoidsTheRoadAndOtherChests(TestContext context) {
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 8; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.DIRT_PATH.getDefaultState());
            }
        }

        context.setBlockState(new BlockPos(6, 1, 4), Blocks.STONE.getDefaultState());
        context.setBlockState(new BlockPos(7, 1, 4), Blocks.STONE.getDefaultState());
        context.setBlockState(new BlockPos(7, 2, 4), Blocks.CHEST.getDefaultState());

        Optional<BlockPos> placed = ChestSpawner.placeNear(
                context.getWorld(), context.getAbsolutePos(new BlockPos(4, 2, 4)));

        // O raio (12) passa da arena: onde quer que nasça, não pode ser rua nem
        // colado noutro baú.
        placed.ifPresent(at -> {
            context.assertFalse(context.getWorld().getBlockState(at.down()).isOf(Blocks.DIRT_PATH),
                    "o baú nasceu sobre a rua em " + at.toShortString());
            for (Direction side : Direction.Type.HORIZONTAL) {
                context.assertFalse(context.getWorld().getBlockState(at.offset(side)).isOf(Blocks.CHEST),
                        "o baú nasceu colado noutro baú em " + at.toShortString());
            }
        });
        context.assertFalse(placed.isPresent() && placed.get().equals(context.getAbsolutePos(new BlockPos(6, 2, 4))),
                "o baú nasceu colado no baú que já estava lá");
        context.complete();
    }
}
