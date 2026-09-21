package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.construction.service.ConstructionService;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.work.HousePlans;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

/** Regressoes para a escolha do lote da BigHouseMOD. */
public class BigHouseFoundationGameTest implements FabricGameTest {

    /** Uma fundacao nao pode ser apoiada sobre o telhado de uma construcao. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "big_house_foundation")
    public void theFoundationDoesNotStartOnTopOfAnExistingBuilding(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(3, 2, 3));
        Vec3i size = new Vec3i(3, 4, 3);

        // O lote fica imediatamente acima de uma construcao existente. A
        // versao defeituosa ignorava esta camada e aceitava o lote.
        for (int dx = 0; dx < size.getX(); dx++) {
            for (int dz = 0; dz < size.getZ(); dz++) {
                world.setBlockState(
                        origin.add(dx, -1, dz), Blocks.OAK_PLANKS.getDefaultState());
            }
        }

        Colony colony = Colony.create(
                UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(origin));

        context.assertFalse(
                invokesSafe(world, colony, origin, size),
                "a BigHouseMOD foi aceita sobre o topo de uma construcao existente");
        context.complete();
    }

    /** Um projeto pendente do save já reserva a área antes de {@code resume}. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "big_house_foundation")
    public void aPendingProjectStillReservesItsSavedBox(TestContext context) {
        UUID colonyId = UUID.randomUUID();
        ColonyPos origin = MinecraftTypeAdapter.toColonyPos(
                context.getAbsolutePos(new BlockPos(3, 4, 3)));
        ResourceId blueprintId = ResourceId.vanilla(
                "village/plains/houses/plains_small_house_1");
        UUID projectId = UUID.randomUUID();

        Optional<com.villagecolony.core.construction.model.Blueprint> blueprint =
                HousePlans.blueprintOf(context.getWorld(), colonyId, blueprintId, origin);

        context.assertTrue(blueprint.isPresent(), "a planta de teste nao foi encontrada");

        ColonyPos max = new ColonyPos(
                origin.x() + blueprint.get().size().x() - 1,
                origin.y() + blueprint.get().size().y() - 1,
                origin.z() + blueprint.get().size().z() - 1);

        try {
            VillageColonyMod.CONSTRUCTIONS.registerPending(new ConstructionService.Pending(
                    projectId,
                    colonyId,
                    blueprintId,
                    origin,
                    ConstructionState.BUILDING));

            context.assertTrue(
                    BuildSiteScanner.overlapsConstructionSite(
                            context.getWorld(), colonyId, origin, max, null),
                    "o projeto pendente nao protegeu sua caixa antes de resume");
        } finally {
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colonyId);
            BuildSiteScanner.clearAll();
        }

        context.complete();
    }

    /** Uma BigHouseMOD pendente nao pode ser duplicada na deteccao da vila. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "big_house_foundation")
    public void aPendingBigHouseIsNotPlacedAgain(TestContext context) {
        UUID colonyId = UUID.randomUUID();
        ColonyPos origin = MinecraftTypeAdapter.toColonyPos(
                context.getAbsolutePos(new BlockPos(3, 4, 3)));

        try {
            VillageColonyMod.CONSTRUCTIONS.registerPending(new ConstructionService.Pending(
                    UUID.randomUUID(),
                    colonyId,
                    StructureBlueprintReader.BIG_HOUSE_MOD,
                    origin,
                    ConstructionState.BUILDING));

            BigHouseFoundation.Result result = BigHouseFoundation.ensure(
                    context.getWorld(),
                    Colony.create(colonyId, origin));

            context.assertFalse(result.placed(), "a fundacao pendente foi colocada novamente");
            context.assertTrue(result.building().isEmpty(),
                    "a fundacao pendente foi registrada como uma casa nova");
        } finally {
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colonyId);
            VillageColonyMod.BUILDINGS.removeOfColony(colonyId);
        }

        context.complete();
    }

    private static boolean invokesSafe(
            ServerWorld world, Colony colony, BlockPos origin, Vec3i size) {
        try {
            Method safe = BigHouseFoundation.class.getDeclaredMethod(
                    "safe",
                    ServerWorld.class,
                    Colony.class,
                    int.class,
                    int.class,
                    int.class,
                    Vec3i.class);
            safe.setAccessible(true);
            return (boolean) safe.invoke(
                    null,
                    world,
                    colony,
                    origin.getX(),
                    origin.getY(),
                    origin.getZ(),
                    size);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("nao foi possivel testar a regra do lote", exception);
        }
    }
}
