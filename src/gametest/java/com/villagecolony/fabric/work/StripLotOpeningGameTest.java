package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.coordination.VillageExpansion;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.StripLot;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import com.villagecolony.fabric.integration.BuildSiteScanner;
import com.villagecolony.fabric.integration.SweepPersistence;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * O planejador abre a obra no lote da faixa e calça o caminho — A5, decisão do autor de 2026-10-22. É a
 * ligação de ponta a ponta: faixa pendente, lote no fundo dela, obra registrada, caminho calçado e a
 * faixa deixando de estar pendente.
 */
public class StripLotOpeningGameTest implements FabricGameTest {

    private static final int Y = 40;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "strip_lot_opening")
    public void thePlannerOpensTheStripLotAndPavesItsPath(TestContext context) {
        ServerWorld world = context.getWorld();

        for (int x = 0; x <= 10; x++) {
            for (int z = 0; z <= 21; z++) {
                context.setBlockState(new BlockPos(x, Y, z), Blocks.DIRT.getDefaultState());
            }

            context.setBlockState(new BlockPos(x, Y, 5), Blocks.DIRT_PATH.getDefaultState());
        }

        for (int x = 0; x <= 8; x++) {
            context.setBlockState(new BlockPos(x, Y + 1, 7), Blocks.STONE.getDefaultState());
            context.setBlockState(new BlockPos(x, Y + 2, 7), Blocks.STONE.getDefaultState());
        }

        BlockPos low = context.getAbsolutePos(new BlockPos(0, Y - 2, 0));
        BlockPos high = context.getAbsolutePos(new BlockPos(10, Y + 8, 5));
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(
                context.getAbsolutePos(new BlockPos(5, Y + 1, 5))));

        colony.measure(new VillageBounds(low.getX(), low.getY(), low.getZ(), high.getX(), high.getY(), high.getZ()));
        VillageColonyMod.COLONIES.register(colony);
        VillageExpansion.annexed(colony.id(), Side.SOUTH);

        List<BlueprintBlock> floor = new ArrayList<>();

        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                floor.add(new BlueprintBlock(new ColonyPos(x, 0, z), ResourceId.vanilla("cobblestone")));
            }
        }

        Blueprint plan = Blueprint.of(ResourceId.vanilla("test_strip_hut"), floor);

        try {
            // Uma casa achada perto de outra rua não encerra a faixa (0.3.87: encerrava, e o lote
            // dentro dela nunca mais foi tentado na sessão).
            com.villagecolony.fabric.integration.RoadExtension.lotFound(colony.id());
            context.assertTrue(VillageExpansion.hasPendingStrip(colony.id()),
                    "um lote achado em outro lugar encerrou a faixa anexada");

            Optional<ConstructionProject> opened = Optional.empty();

            for (int pass = 0; pass < 20 && opened.isEmpty(); pass++) {
                opened = RoadGrowthPlanning.lotInTheStrip(world, colony, plan, List.of(plan), 1);
            }

            context.assertTrue(opened.isPresent(), "a faixa pendente não abriu obra");
            context.assertTrue(VillageColonyMod.CONSTRUCTIONS.openOf(colony.id()).isPresent(), "a obra não ficou registrada");
            context.assertFalse(VillageExpansion.hasPendingStrip(colony.id()), "a faixa continuou pendente com a obra aberta");

            int stripStart = context.getAbsolutePos(new BlockPos(0, Y, 6)).getZ();

            context.assertTrue(opened.get().origin().z() > stripStart,
                    "a obra não nasceu dentro da faixa: " + opened.get().origin());

            BlockPos throughTheGap = context.getAbsolutePos(new BlockPos(9, Y, 7));
            BlockPos gapOther = context.getAbsolutePos(new BlockPos(10, Y, 7));

            context.assertTrue(com.villagecolony.fabric.integration.PathProposals.isWanted(colony.id(), throughTheGap.getX(), throughTheGap.getZ())
                            || com.villagecolony.fabric.integration.PathProposals.isWanted(colony.id(), gapOther.getX(), gapOther.getZ()),
                    "Regra 214: o caminho pela volta da duna não foi pedido ao grafo");
        } finally {
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colony.id());
            VillageExpansion.lotFound(colony.id());
            VillageColonyMod.COLONIES.remove(colony.id());
            StripLot.clearAll();
        }

        context.complete();
    }
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "house_center_door")
    public void aNewHouseFacesTheCenterWithAnAccessibleStreet(TestContext context) {
        assertHouseDirection(context, true, Side.NORTH);
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "house_center_door")
    public void aNewHouseKeepsTheOriginalAccessWhenTheCenterSideIsBlocked(TestContext context) {
        assertHouseDirection(context, false, Side.SOUTH);
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "house_center_door")
    public void theSmallHouseRecessedDoorCanReachTheStreet(TestContext context) {
        ServerWorld world = context.getWorld();
        Blueprint house = StructureBlueprintReader.read(world, StructureBlueprintReader.PLAINS_SMALL_HOUSE)
                .orElseThrow();
        Blueprint facingNorth = house.rotated(house.doorSide().orElseThrow().turnsTo(Side.NORTH));
        ColonyPos door = facingNorth.blocks().stream()
                .filter(block -> block.block().path().endsWith("_door"))
                .findFirst().orElseThrow().offset();
        context.assertTrue(door.z() > 0, "a planta precisa ter uma porta recuada");

        BlockPos origin = new BlockPos(2, Y + 1, 2);
        for (int z = 0; z < door.z(); z++) {
            context.setBlockState(origin.add(door.x(), -1, z), Blocks.DIRT);
        }
        BlockPos road = origin.add(door.x(), -1, -1);
        context.setBlockState(road, Blocks.DIRT_PATH);

        ColonyPos lot = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(origin));
        Colony colony = Colony.create(UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(road.north())));
        BlockPos absoluteRoad = context.getAbsolutePos(road);
        SweepPersistence.restore(new ColonyRoads(colony.id(), colony.center(),
                List.of(ColonyRoads.column(absoluteRoad.getX(), absoluteRoad.getZ()))));

        context.assertTrue(PlanPlacement.hasDirectRoadAccess(world, colony.id(), lot, facingNorth, Side.NORTH),
                "a passagem da porta recuada deveria alcançar a rua");
        context.complete();
    }

    private static void assertHouseDirection(TestContext context, boolean northOpen, Side expected) {
        ServerWorld world = context.getWorld();
        for (int x = 0; x <= 6; x++) {
            for (int z = 0; z <= 6; z++) {
                context.setBlockState(new BlockPos(x, Y, z), Blocks.DIRT);
            }
        }

        BlockPos north = new BlockPos(3, Y, 1);
        BlockPos south = new BlockPos(3, Y, 5);
        context.setBlockState(north, Blocks.DIRT_PATH);
        context.setBlockState(south, Blocks.DIRT_PATH);
        if (!northOpen) {
            context.setBlockState(north.up(), Blocks.STONE);
        }

        Colony colony = Colony.create(UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(3, Y + 1, 0))));
        List<Long> roadColumns = List.of(north, south).stream()
                .map(context::getAbsolutePos)
                .map(pos -> ColonyRoads.column(pos.getX(), pos.getZ()))
                .toList();
        SweepPersistence.restore(new ColonyRoads(colony.id(), colony.center(), roadColumns));

        Blueprint plan = Blueprint.of(ResourceId.vanilla("test_center_house"), List.of(
                new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("oak_planks")),
                new BlueprintBlock(new ColonyPos(2, 0, 2), ResourceId.vanilla("oak_planks")),
                new BlueprintBlock(new ColonyPos(1, 1, 2), ResourceId.vanilla("oak_door"),
                        true, Optional.of(Side.SOUTH)),
                new BlueprintBlock(new ColonyPos(1, 2, 2), ResourceId.vanilla("oak_door"),
                        true, Optional.of(Side.SOUTH))));
        ColonyPos lot = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(2, Y + 1, 2)));
        BuildSiteScanner.Site site = new BuildSiteScanner.Site(lot, Direction.SOUTH, plan.size());

        try {
            Optional<ConstructionProject> project = SiteOpening.open(world, colony, site, List.of(plan), plan, 1);
            context.assertTrue(project.isPresent(), "a moradia não abriu no lote aprovado");
            context.assertTrue(project.orElseThrow().blueprint().doorSide().filter(expected::equals).isPresent(),
                    "a porta da moradia deveria apontar para " + expected);
        } finally {
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colony.id());
        }

        context.complete();
    }
}
