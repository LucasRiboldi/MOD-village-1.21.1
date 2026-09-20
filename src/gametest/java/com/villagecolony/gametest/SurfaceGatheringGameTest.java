package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.DirtPatch;
import com.villagecolony.fabric.integration.ChestInventoryReader;
import com.villagecolony.fabric.integration.FarthestVillageSector;
import com.villagecolony.fabric.integration.GrassPatch;
import com.villagecolony.fabric.integration.RingSweep;
import com.villagecolony.fabric.integration.SandPatch;
import com.villagecolony.fabric.integration.WorkerEquipment;
import com.villagecolony.fabric.work.SurfaceGatheringWork;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/** Prova a coleta de grass_block fora da vila com a ferramenta do fundidor. */
public class SurfaceGatheringGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(2, 2, 2);

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "surface_resource_catalogue",
            tickLimit = 20)
    public void dirtIsARecognizedSurfaceResource(TestContext context) {
        context.assertTrue(
                MinecraftTypeAdapter.toResourceType(Items.DIRT).isPresent(),
                "dirt precisa entrar no catálogo para a obra gerar uma tarefa de coleta ao fundidor");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "surface_grass_gathering", tickLimit = 100)
    public void smelterGathersGrassOutsideTheProtectedVillageRadius(TestContext context) {
        var world = context.getWorld();
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        BlockPos center = context.getAbsolutePos(CHEST);
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(center);
        Colony colony = Colony.create(UUID.randomUUID(), chest);
        VillageColonyMod.COLONIES.register(colony);
        ColonyFixture owned = ColonyFixture.create().owning(colony);

        var sector = FarthestVillageSector.farthestLoadedSector(world, center, colony.id());
        BlockPos grass = center.offset(sector, FarthestVillageSector.PROTECTED_RADIUS + 1);
        world.setBlockState(grass.down(), Blocks.DIRT.getDefaultState());
        world.setBlockState(grass, Blocks.GRASS_BLOCK.getDefaultState());
        world.setBlockState(grass.up(), Blocks.AIR.getDefaultState());
        context.assertTrue(
                GrassPatch.in(world, grass, center.getY(), center, sector).filter(grass::equals).isPresent(),
                "o cenário precisa expor grass_block elegível no setor carregado");
        BlockPos stand = grass.offset(sector.rotateYClockwise(), 2);
        world.setBlockState(stand.down(), Blocks.DIRT.getDefaultState());
        VillagerEntity villager = EntityType.VILLAGER.create(world);
        villager.setBreedingAge(0);
        villager.refreshPositionAndAngles(
                stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0.0f, 0.0f);
        context.assertTrue(world.spawnEntity(villager), "não foi possível criar o fundidor no setor de coleta");
        context.runAtTick(1, () -> {
            try {
                context.assertTrue(world.getEntity(villager.getUuid()) == villager,
                        "fundidor criado no setor não está registrado no ServerWorld");
                Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
                worker.assign(ProfessionType.SMELTER);
                VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));
                owned.owning(villager.getUuid());
                WorkerEquipment.equip(world, List.of(worker));
                context.assertTrue(
                        RingSweep.around(villager.getUuid(), grass, 48,
                                column -> GrassPatch.in(world, column, center.getY(), center, sector))
                                .filter(grass::equals).isPresent(),
                        "o RingSweep não encontrou a coluna que GrassPatch aceitou diretamente");

                Task task = VillageColonyMod.TASKS.create(
                        colony.id(), TaskType.COLLECT_SURFACE_RESOURCE,
                        TaskPriority.PRODUCTION, ResourceType.GRASS_BLOCK, 1);
                task.reserveFor(villager.getUuid());
                int opened = SurfaceGatheringWork.run(world, colony);
                context.assertTrue(opened == 1, "o coletor não abriu a tarefa reservada: " + task.state());

                for (int tick = 0; tick < 80 && !world.getBlockState(grass).isAir(); tick++) {
                    SurfaceGatheringWork.tick(world);
                }

                context.assertTrue(
                        world.getBlockState(grass).isAir(),
                        "o fundidor não removeu o grass_block do setor externo escolhido");
                context.assertTrue(
                        villager.getEquippedStack(EquipmentSlot.MAINHAND).isOf(Items.IRON_SHOVEL),
                        "o fundidor não estava com a pá de ferro do mod");
                context.assertTrue(
                        ChestInventoryReader.read(world, center).amountOf(ResourceType.GRASS_BLOCK) == 1,
                        "o grass_block não chegou ao baú pessoal do fundidor");
                context.assertTrue(
                        task.state() == TaskState.COMPLETED,
                        "a tarefa não foi concluída após recolher o bloco: " + task.state());
                context.complete();
            } finally {
                owned.cleanUp();
            }
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "surface_sand_gathering", tickLimit = 100)
    public void smelterGathersSandOnlyOutsideTheProtectedVillageRadius(TestContext context) {
        var world = context.getWorld();
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        BlockPos center = context.getAbsolutePos(CHEST);
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(center);
        Colony colony = Colony.create(UUID.randomUUID(), chest);
        VillageColonyMod.COLONIES.register(colony);
        ColonyFixture owned = ColonyFixture.create().owning(colony);

        var sector = FarthestVillageSector.farthestLoadedSector(world, center, colony.id());
        BlockPos near = center.offset(sector, FarthestVillageSector.PROTECTED_RADIUS - 1);
        world.setBlockState(near.down(), Blocks.SANDSTONE.getDefaultState());
        world.setBlockState(near, Blocks.SAND.getDefaultState());
        world.setBlockState(near.up(), Blocks.AIR.getDefaultState());

        BlockPos sand = center.offset(sector, FarthestVillageSector.PROTECTED_RADIUS + 1);
        world.setBlockState(sand.down(), Blocks.SANDSTONE.getDefaultState());
        world.setBlockState(sand, Blocks.SAND.getDefaultState());
        world.setBlockState(sand.up(), Blocks.AIR.getDefaultState());
        context.assertTrue(
                SandPatch.in(world, sand, center.getY()).filter(sand::equals).isPresent(),
                "o cenário precisa expor areia elegível no setor externo");
        BlockPos stand = sand.offset(sector.rotateYClockwise(), 2);
        world.setBlockState(stand.down(), Blocks.SANDSTONE.getDefaultState());
        VillagerEntity villager = EntityType.VILLAGER.create(world);
        villager.setBreedingAge(0);
        villager.refreshPositionAndAngles(
                stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0.0f, 0.0f);
        context.assertTrue(world.spawnEntity(villager), "não foi possível criar o fundidor no setor de coleta");
        context.runAtTick(1, () -> {
            try {
                Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
                worker.assign(ProfessionType.SMELTER);
                VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));
                owned.owning(villager.getUuid());
                WorkerEquipment.equip(world, List.of(worker));

                Task task = VillageColonyMod.TASKS.create(
                        colony.id(), TaskType.COLLECT_SURFACE_RESOURCE,
                        TaskPriority.PRODUCTION, ResourceType.SAND, 1);
                task.reserveFor(villager.getUuid());
                int opened = SurfaceGatheringWork.run(world, colony);
                context.assertTrue(opened == 1, "o coletor não abriu a tarefa de areia reservada: " + task.state());

                for (int tick = 0; tick < 80 && !world.getBlockState(sand).isAir(); tick++) {
                    SurfaceGatheringWork.tick(world);
                }

                context.assertTrue(world.getBlockState(near).isOf(Blocks.SAND),
                        "a coleta de areia invadiu o raio protegido da vila");
                context.assertTrue(world.getBlockState(sand).isAir(),
                        "o fundidor não removeu a areia do setor externo escolhido");
                context.assertTrue(
                        villager.getEquippedStack(EquipmentSlot.MAINHAND).isOf(Items.IRON_SHOVEL),
                        "o fundidor não estava com a pá de ferro do mod");
                context.assertTrue(
                        ChestInventoryReader.read(world, center).amountOf(ResourceType.SAND) == 1,
                        "a areia não chegou ao baú pessoal do fundidor");
                context.assertTrue(
                        task.state() == TaskState.COMPLETED,
                        "a tarefa de areia não foi concluída: " + task.state());
                context.complete();
            } finally {
                owned.cleanUp();
            }
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "surface_dirt_gathering", tickLimit = 100)
    public void smelterGathersDirtOutsideTheProtectedVillageRadius(TestContext context) {
        var world = context.getWorld();
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        BlockPos center = context.getAbsolutePos(CHEST);
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(center);
        Colony colony = Colony.create(UUID.randomUUID(), chest);
        VillageColonyMod.COLONIES.register(colony);
        ColonyFixture owned = ColonyFixture.create().owning(colony);

        var sector = FarthestVillageSector.farthestLoadedSector(world, center, colony.id());
        BlockPos dirt = center.offset(sector, FarthestVillageSector.PROTECTED_RADIUS + 1);
        world.setBlockState(dirt.down(), Blocks.DIRT.getDefaultState());
        world.setBlockState(dirt, Blocks.DIRT.getDefaultState());
        world.setBlockState(dirt.up(), Blocks.AIR.getDefaultState());
        context.assertTrue(
                DirtPatch.in(world, dirt, center.getY(), center, sector).filter(dirt::equals).isPresent(),
                "o cenário precisa expor terra elegível no setor carregado");
        BlockPos stand = dirt.offset(sector.rotateYClockwise(), 2);
        world.setBlockState(stand.down(), Blocks.DIRT.getDefaultState());
        VillagerEntity villager = EntityType.VILLAGER.create(world);
        villager.setBreedingAge(0);
        villager.refreshPositionAndAngles(
                stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0.0f, 0.0f);
        context.assertTrue(world.spawnEntity(villager), "não foi possível criar o fundidor no setor de coleta");
        context.runAtTick(1, () -> {
            try {
                context.assertTrue(world.getEntity(villager.getUuid()) == villager,
                        "fundidor criado no setor não está registrado no ServerWorld");
                Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
                worker.assign(ProfessionType.SMELTER);
                VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));
                owned.owning(villager.getUuid());
                WorkerEquipment.equip(world, List.of(worker));
                context.assertTrue(
                        RingSweep.around(villager.getUuid(), dirt, 48,
                                column -> DirtPatch.in(world, column, center.getY(), center, sector))
                                .filter(dirt::equals).isPresent(),
                        "o RingSweep não encontrou a coluna que DirtPatch aceitou diretamente");

                Task task = VillageColonyMod.TASKS.create(
                        colony.id(), TaskType.COLLECT_SURFACE_RESOURCE,
                        TaskPriority.PRODUCTION, ResourceType.DIRT, 1);
                task.reserveFor(villager.getUuid());
                int opened = SurfaceGatheringWork.run(world, colony);
                context.assertTrue(opened == 1, "o coletor não abriu a tarefa reservada: " + task.state());

                for (int tick = 0; tick < 80 && !world.getBlockState(dirt).isAir(); tick++) {
                    SurfaceGatheringWork.tick(world);
                }

                context.assertTrue(
                        world.getBlockState(dirt).isAir(),
                        "o fundidor não removeu a terra do setor externo escolhido");
                context.assertTrue(
                        villager.getEquippedStack(EquipmentSlot.MAINHAND).isOf(Items.IRON_SHOVEL),
                        "o fundidor não estava com a pá de ferro do mod");
                context.assertTrue(
                        ChestInventoryReader.read(world, center).amountOf(ResourceType.DIRT) == 1,
                        "a terra não chegou ao baú pessoal do fundidor");
                context.assertTrue(
                        task.state() == TaskState.COMPLETED,
                        "a tarefa não foi concluída após recolher o bloco: " + task.state());
                context.complete();
            } finally {
                owned.cleanUp();
            }
        });
    }
}
