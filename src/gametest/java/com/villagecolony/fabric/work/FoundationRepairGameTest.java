package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.construction.service.ConstructionService;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/** A casa colocada pela fundacao nao vira canteiro profissional. */
public class FoundationRepairGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "foundation_repair")
    public void aPlacedFoundationDoesNotStartARepairProject(TestContext context) {
        ColonyPos origin = MinecraftTypeAdapter.toColonyPos(
                context.getAbsolutePos(new BlockPos(1, 4, 1)));
        Colony colony = Colony.create(UUID.randomUUID(), origin);

        try {
            VillageColonyMod.BUILDINGS.register(foundation(context, colony, origin));

            context.assertTrue(
                    BuildingRepairPlanner.open(context.getWorld(), colony).isEmpty(),
                    "a BigHouseMOD colocada pela fundacao abriu um canteiro de reparo");
            context.assertTrue(VillageColonyMod.CONSTRUCTIONS.openOf(colony.id()).isEmpty(),
                    "a BigHouseMOD virou obra profissional");
        } finally {
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colony.id());
            VillageColonyMod.BUILDINGS.removeOfColony(colony.id());
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "foundation_repair")
    public void aSavedRepairOfThePlacedFoundationIsDropped(TestContext context) {
        ColonyPos origin = MinecraftTypeAdapter.toColonyPos(
                context.getAbsolutePos(new BlockPos(1, 4, 1)));
        Colony colony = Colony.create(UUID.randomUUID(), origin);
        BlueprintBlock standing = HousePlans.blueprintOf(
                context.getWorld(), colony.id(), StructureBlueprintReader.BIG_HOUSE_MOD, origin)
                .orElseThrow(() -> new AssertionError("planta da BigHouseMOD ausente"))
                .blocks().stream()
                .filter(block -> MinecraftTypeAdapter.toBlock(block.block()).isPresent())
                .findFirst()
                .orElseThrow(() -> new AssertionError("planta sem blocos do Minecraft"));
        BlockPos standingAt = new BlockPos(
                origin.x() + standing.offset().x(),
                origin.y() + standing.offset().y(),
                origin.z() + standing.offset().z());
        BlockState before = context.getWorld().getBlockState(standingAt);
        Block expected = MinecraftTypeAdapter.toBlock(standing.block()).orElseThrow();

        try {
            context.getWorld().setBlockState(standingAt, expected.getDefaultState());
            VillageColonyMod.BUILDINGS.register(foundation(context, colony, origin));
            VillageColonyMod.CONSTRUCTIONS.registerPending(new ConstructionService.Pending(
                    UUID.randomUUID(), colony.id(), StructureBlueprintReader.BIG_HOUSE_MOD,
                    origin, ConstructionState.BUILDING));

            ConstructionPlanner.plan(context.getWorld(), colony);

            context.assertTrue(VillageColonyMod.CONSTRUCTIONS.pendingOf(colony.id()).isEmpty(),
                    "o reparo antigo da BigHouseMOD continuou pendente");
            context.assertTrue(VillageColonyMod.CONSTRUCTIONS.openOf(colony.id()).isEmpty(),
                    "o reparo antigo da BigHouseMOD voltou sobre a casa existente");
            context.assertTrue(context.getWorld().getBlockState(standingAt).isOf(expected),
                    "a retomada alterou a casa fundacional");
        } finally {
            context.getWorld().setBlockState(standingAt, before);
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colony.id());
            VillageColonyMod.BUILDINGS.removeOfColony(colony.id());
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "foundation_repair")
    public void anIncompleteProfessionHouseStillStartsRepair(TestContext context) {
        ColonyPos origin = MinecraftTypeAdapter.toColonyPos(
                context.getAbsolutePos(new BlockPos(1, 4, 1)));
        Colony colony = Colony.create(UUID.randomUUID(), origin);
        ColonyPos size = StructureBlueprintReader.read(
                context.getWorld(), StructureBlueprintReader.SMALL_HOUSE)
                .orElseThrow(() -> new AssertionError("planta da casa profissional ausente"))
                .size();

        try {
            VillageColonyMod.BUILDINGS.register(new Building(
                    UUID.randomUUID(), colony.id(), StructureBlueprintReader.SMALL_HOUSE,
                    origin, new ColonyPos(
                            origin.x() + size.x() - 1,
                            origin.y() + size.y() - 1,
                            origin.z() + size.z() - 1), true));

            context.assertTrue(BuildingRepairPlanner.open(context.getWorld(), colony).isPresent(),
                    "o reparo de uma casa profissional incompleta foi bloqueado");
        } finally {
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colony.id());
            VillageColonyMod.BUILDINGS.removeOfColony(colony.id());
        }

        context.complete();
    }

    private static Building foundation(TestContext context, Colony colony, ColonyPos origin) {
        ColonyPos size = StructureBlueprintReader.read(
                context.getWorld(), StructureBlueprintReader.BIG_HOUSE_MOD)
                .orElseThrow(() -> new AssertionError("planta da BigHouseMOD ausente"))
                .size();

        return new Building(
                UUID.randomUUID(), colony.id(), StructureBlueprintReader.BIG_HOUSE_MOD,
                origin, new ColonyPos(
                        origin.x() + size.x() - 1,
                        origin.y() + size.y() - 1,
                        origin.z() + size.z() - 1), true);
    }
}
