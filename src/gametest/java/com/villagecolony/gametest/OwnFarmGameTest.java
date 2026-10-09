package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestInventoryReader;
import com.villagecolony.fabric.integration.VillageFarms;
import com.villagecolony.fabric.work.FarmOwners;
import com.villagecolony.fabric.work.FarmerWork;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.state.property.IntProperty;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructureLiquidSettings;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockBox;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * O fazendeiro tem a sua roça e trabalha nela primeiro — Regra 52 (autor,
 * 2026-10-08).
 *
 * <p>O centro da colônia fica longe da roça e a varredura da vila é encurtada:
 * só a roça própria leva o fazendeiro até o trigo.
 */
public class OwnFarmGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(1, 2, 1);

    private static final BlockPos FIELD = new BlockPos(4, 2, 4);

    private static final BlockPos STAND = new BlockPos(4, 2, 3);

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "own_farm", tickLimit = 200)
    public void theFarmerHarvestsHisOwnFarmBeyondTheVillageSweep(TestContext context) {
        context.setBlockState(FIELD.down(), Blocks.FARMLAND.getDefaultState());
        context.setBlockState(FIELD, Blocks.WHEAT.getDefaultState()
                .with((IntProperty) Blocks.WHEAT.getStateManager().getProperty("age"),
                        ((CropBlock) Blocks.WHEAT).getMaxAge()));
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        BlockPos field = context.getAbsolutePos(FIELD);
        Colony colony = Colony.create(UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST).add(200, 0, 200)));

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        VillageColonyMod.BUILDINGS.register(new Building(UUID.randomUUID(), colony.id(),
                ResourceId.vanilla("village/plains/houses/plains_small_farm_1"),
                MinecraftTypeAdapter.toColonyPos(field.down()),
                MinecraftTypeAdapter.toColonyPos(field)));

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, STAND);
        villager.setBreedingAge(0);
        owned.owning(villager.getUuid());

        VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id()).assign(ProfessionType.FARMER);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST))));

        Task task = VillageColonyMod.TASKS.create(colony.id(), TaskType.COLLECT_FOOD,
                TaskPriority.PRODUCTION, ResourceType.WHEAT, 8);
        task.reserveFor(villager.getUuid());

        FarmerWork.shortenSearchTo(4);
        FarmerWork.run(context.getWorld(), colony);

        context.assertTrue(FarmOwners.farmOf(villager.getUuid()).isPresent(),
                "o único fazendeiro ficou sem a única roça");

        context.runAtTick(120, () -> {
            try {
                int wheat = ChestInventoryReader
                        .read(context.getWorld(), context.getAbsolutePos(CHEST))
                        .amountOf(ResourceType.WHEAT);

                context.assertTrue(wheat > 0,
                        "o trigo da roça dele ficou no pé: ele só procurou em volta do centro");
            } finally {
                VillageColonyMod.BUILDINGS.removeOfColony(colony.id());
                FarmOwners.forget(villager.getUuid());
                FarmerWork.clearAll();
                FarmerWork.restoreSearch();
                owned.cleanUp();
            }

            context.complete();
        });
    }

    /**
     * A peça de roça da vila gerada diz a planta dela — é como a roça da vila
     * entra na conta da colônia. O campo do jogo é protegido; a leitura passa
     * pelo codec.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "own_farm", tickLimit = 20)
    public void aVillageFarmPieceNamesItsPlan(TestContext context) {
        String farm = "minecraft:village/plains/houses/plains_small_farm_1";
        PoolStructurePiece piece = new PoolStructurePiece(
                context.getWorld().getStructureTemplateManager(),
                StructurePoolElement.ofLegacySingle(farm).apply(StructurePool.Projection.TERRAIN_MATCHING),
                BlockPos.ORIGIN, 0, BlockRotation.NONE, new BlockBox(0, 0, 0, 8, 2, 12),
                StructureLiquidSettings.APPLY_WATERLOGGING);

        context.assertTrue(VillageFarms.planOf(context.getWorld(), piece)
                        .filter(ResourceId.parse(farm)::equals).isPresent(),
                "a peça de roça não disse a planta: " + VillageFarms.planOf(context.getWorld(), piece));
        context.complete();
    }
}
