package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BuildSiteScanner;
import com.villagecolony.fabric.work.ConstructionPlanner;
import com.villagecolony.fabric.work.HousePlans;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Casa quando falta cama — E48, 2026-09-24, decisão do autor.
 *
 * <p>O cenário é o de {@code FarmPlanGameTest.theNextTurnAfterAHouseIsNonResidential}
 * — uma casa de pé, e a vez do rodízio é de outro tipo —, com uma única
 * diferença: a colônia contou menos camas do que adultos. Lá a passagem
 * abre infraestrutura; aqui tem de abrir casa. Juntos, os dois provam que a
 * regra das camas é que decide, e não outra coisa do cenário.
 */
public class HouseRotationGameTest implements FabricGameTest {

    private static final int SCAN_RADIUS = 16;

    private static final int ADULTS = 8;

    private static final int BEDS = 6;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "house_rotation_beds")
    public void missingBedsOpenAHouseEvenWhenTheTurnIsAnotherType(TestContext context) {
        BlockPos center = new BlockPos(16, 1, 16);

        for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
            for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
                context.setBlockState(center.add(dx, 0, dz), Blocks.GRASS_BLOCK.getDefaultState());
            }
        }

        context.setBlockState(center, Blocks.DIRT_PATH.getDefaultState());

        UUID colonyId = UUID.randomUUID();
        BlockPos absoluteRoad = context.getAbsolutePos(center);
        BuildSiteScanner.restore(new ColonyRoads(
                colonyId,
                MinecraftTypeAdapter.toColonyPos(absoluteRoad),
                List.of(ColonyRoads.column(absoluteRoad.getX(), absoluteRoad.getZ()))));

        ColonyPos colonyCenter = MinecraftTypeAdapter.toColonyPos(absoluteRoad);
        Colony colony = Colony.create(colonyId, colonyCenter);

        // A detecção de vila contou seis camas para oito adultos.
        colony.observe(colonyCenter, BEDS);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        try {
            UUID builderId = UUID.randomUUID();

            VillageColonyMod.WORKERS.register(builderId, colony.id())
                    .assign(ProfessionType.BUILDER);
            owned.owning(builderId);

            for (int villager = 1; villager < ADULTS; villager++) {
                UUID id = UUID.randomUUID();

                VillageColonyMod.WORKERS.register(id, colony.id());
                owned.owning(id);
            }

            // Uma casa de pé no mundo — o reparo cíclico compara a planta com
            // os blocos, e uma casa só no registro seria adotada como reparo.
            ResourceId houseId =
                    ResourceId.vanilla("village/plains/houses/plains_small_house_1");
            ColonyPos built = MinecraftTypeAdapter.toColonyPos(
                    context.getAbsolutePos(center.add(8, 0, 8)));

            Blueprint house = HousePlans.blueprintOf(
                    context.getWorld(), colony.id(), houseId, built).orElse(null);

            context.assertTrue(house != null, "o catálogo não devolveu a casa de referência");

            for (BlueprintBlock block : house.blocks()) {
                MinecraftTypeAdapter.toBlock(block.block()).ifPresent(expected ->
                        context.getWorld().setBlockState(
                                new BlockPos(
                                        built.x() + block.offset().x(),
                                        built.y() + block.offset().y(),
                                        built.z() + block.offset().z()),
                                expected.getDefaultState()));
            }

            ColonyPos size = house.size();

            VillageColonyMod.BUILDINGS.register(new Building(
                    colony.id(),
                    colony.id(),
                    houseId,
                    built,
                    new ColonyPos(
                            built.x() + size.x() - 1,
                            built.y() + size.y() - 1,
                            built.z() + size.z() - 1)));

            Optional<ConstructionProject> opened =
                    ConstructionPlanner.plan(context.getWorld(), colony);

            context.assertTrue(
                    opened.isPresent(),
                    "a colônia com gente sem cama não abriu obra nenhuma");

            context.assertTrue(
                    HousePlans.isDwelling(opened.get().blueprint().id()),
                    "oito adultos e seis camas, e a colônia abriu "
                            + opened.get().blueprint().id() + " em vez de uma casa");
        } finally {
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colony.id());
            VillageColonyMod.BUILDINGS.removeOfColony(colony.id());
            owned.cleanUp();
        }

        context.complete();
    }
}
