package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.MaeBuildQueue;
import com.villagecolony.fabric.integration.MaeRescue;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A obra pedida pelo jogador não desiste da vez por estar parada (Regra 213), e a volta sem lote da casa soma para o
 * socorro do MAE, a da roça não (Regras 222 e 226).
 */
public class MaePatienceGameTest implements FabricGameTest {

    /** Parada muito além de qualquer paciência do relógio. */
    private static final long LONG_STALL = 10_000_000L;

    private static ConstructionProject building(TestContext context, Colony colony, BlockPos at) {
        Blueprint blueprint = Blueprint.of(ResourceId.parse("minecraft:village/plains/houses/plains_small_house_1"),
                List.of(new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("oak_planks"))));
        ConstructionProject project = ConstructionProject.plan(colony.id(), blueprint,
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(at)));

        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);
        VillageColonyMod.CONSTRUCTIONS.register(project);

        return project;
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_patience", tickLimit = 20)
    public void theRequestedBuildDoesNotGiveUpWhenStalled(TestContext context) {
        ServerWorld world = context.getWorld();
        Colony plain = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(1, 2, 1))));
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(5, 2, 5))));
        ConstructionProject ordinary = building(context, plain, new BlockPos(2, 2, 2));
        ConstructionProject requested = building(context, colony, new BlockPos(6, 2, 6));
        BlockPos origin = MinecraftTypeAdapter.toBlockPos(requested.origin());

        VillageColonyMod.COLONIES.register(plain);
        VillageColonyMod.COLONIES.register(colony);
        MaeBuildQueue.requestForTest(world, colony.id(), requested.blueprint().id(), origin);

        try {
            WaitingWork.stalledFor(world, ordinary, LONG_STALL);
            WaitingWork.stalledFor(world, requested, LONG_STALL);

            context.assertTrue(WaitingWork.giveUpIfStalled(world, plain, ordinary),
                    "a obra comum parada não desistiu: a cena não mede nada");
            context.assertFalse(WaitingWork.giveUpIfStalled(world, colony, requested),
                    "Regra 213: a obra pedida desistiu por estar parada");
            context.assertTrue(VillageColonyMod.CONSTRUCTIONS.find(requested.id()).filter(found -> found.state().isOpen()).isPresent(),
                    "Regra 213: a obra pedida saiu da fila");
        } finally {
            MaeBuildQueue.ofColony(world, colony.id()).forEach(request -> MaeBuildQueue.remove(world, request.id()));
            for (Colony each : List.of(plain, colony)) {
                VillageColonyMod.CONSTRUCTIONS.removeOfColony(each.id());
                VillageColonyMod.COLONIES.remove(each.id());
            }
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mae_patience_rescue", tickLimit = 20)
    public void onlyTheHouseWithoutALotCountsForTheRescue(TestContext context) {
        ServerWorld world = context.getWorld();
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(1, 2, 1))));
        List<BlueprintBlock> plank = List.of(new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("oak_planks")));
        Blueprint house = Blueprint.of(ResourceId.parse("minecraft:village/plains/houses/plains_small_house_1"), plank);
        Blueprint farm = Blueprint.of(ResourceId.parse("minecraft:village/plains/houses/plains_small_farm_1"), plank);
        ConstructionProject opened = ConstructionProject.plan(colony.id(), house, colony.center());

        MaeRescue.foundALot(colony.id());

        try {
            LotSeeking.afterTheRoad(world, colony, farm, List.of(farm), Optional.empty());
            context.assertTrue(MaeRescue.sweptOf(colony.id()) == 0, "Regra 226: a roça sem lote contou para o socorro");

            LotSeeking.afterTheRoad(world, colony, house, List.of(house), Optional.of(opened));
            context.assertTrue(MaeRescue.sweptOf(colony.id()) == 0, "Regra 222: a rua abriu lote e a volta contou para o socorro");

            LotSeeking.afterTheRoad(world, colony, house, List.of(house), Optional.empty());
            context.assertTrue(MaeRescue.sweptOf(colony.id()) == 1, "Regras 222 e 226: a casa sem lote não contou para o socorro");
        } finally {
            MaeRescue.foundALot(colony.id());
        }

        context.complete();
    }
}
