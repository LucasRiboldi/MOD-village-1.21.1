package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

public final class FoundationPreparationGameTest implements FabricGameTest {
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "foundation_preparation")
    public void halfSupportedStreetBaseIsFilledWithBiomeGround(TestContext context) {
        BlockPos origin = new BlockPos(5, 1, 5);
        context.setBlockState(origin, Blocks.GRASS_BLOCK);
        context.setBlockState(origin.east(), Blocks.GRASS_BLOCK);
        context.setBlockState(origin.south(), Blocks.AIR);
        context.setBlockState(origin.south().east(), Blocks.AIR);
        context.setBlockState(origin.south().down(), Blocks.DIRT);
        context.setBlockState(origin.south().east().down(), Blocks.DIRT);

        context.assertTrue(FoundationPreparation.prepareIfQualified(context.getWorld(), project(context, origin)),
                "metade da base na altura da rua deve permitir a preparação");
        context.assertTrue(context.getWorld().getBlockState(context.getAbsolutePos(origin.south())).isOf(Blocks.GRASS_BLOCK),
                "a lacuna deve receber o solo do bioma");
        context.assertTrue(context.getWorld().getBlockState(context.getAbsolutePos(origin.south().east())).isOf(Blocks.GRASS_BLOCK),
                "a lacuna deve receber o solo do bioma");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "foundation_preparation")
    public void lessThanHalfSupportDoesNotCreateAPlatform(TestContext context) {
        BlockPos origin = new BlockPos(5, 1, 5);
        context.setBlockState(origin, Blocks.GRASS_BLOCK);
        context.setBlockState(origin.east(), Blocks.AIR);
        context.setBlockState(origin.south(), Blocks.AIR);
        context.setBlockState(origin.south().east(), Blocks.AIR);
        context.setBlockState(origin.east().down(), Blocks.DIRT);
        context.setBlockState(origin.south().down(), Blocks.DIRT);
        context.setBlockState(origin.south().east().down(), Blocks.DIRT);

        context.assertTrue(!FoundationPreparation.prepareIfQualified(context.getWorld(), project(context, origin)),
                "menos de metade de apoio não pode criar uma plataforma");
        context.assertTrue(context.getWorld().getBlockState(context.getAbsolutePos(origin.east())).isAir(),
                "a lacuna deve permanecer vazia abaixo do limiar");
        context.complete();
    }

    /**
     * Metade da base dois blocos abaixo da rua não é aterrada — decisão do
     * autor, 2026-09-30: a base fica na rua ou um acima, nunca abaixo. Por
     * algumas horas do mesmo dia o aterro foi de três camadas.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "foundation_preparation")
    public void aGapTwoBelowTheStreetIsNotFilled(TestContext context) {
        BlockPos origin = new BlockPos(5, 3, 5);
        context.setBlockState(origin, Blocks.GRASS_BLOCK);
        context.setBlockState(origin.east(), Blocks.GRASS_BLOCK);
        for (BlockPos low : List.of(origin.south(), origin.south().east())) {
            context.setBlockState(low, Blocks.AIR);
            context.setBlockState(low.down(), Blocks.AIR);
            context.setBlockState(low.down(2), Blocks.DIRT);
        }

        context.assertTrue(!FoundationPreparation.prepareIfQualified(context.getWorld(), project(context, origin)),
                "a lacuna de duas camadas foi preparada");
        context.assertTrue(context.getWorld().getBlockState(context.getAbsolutePos(origin.south().down())).isAir(),
                "a lacuna funda recebeu solo");
        context.complete();
    }

    /** O mesmo lote é recusado na escolha de lugar. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "foundation_preparation")
    public void aLotWithHalfItsBaseTwoBelowTheStreetIsRefused(TestContext context) {
        BlockPos origin = new BlockPos(5, 3, 5);
        context.setBlockState(origin, Blocks.GRASS_BLOCK);
        context.setBlockState(origin.east(), Blocks.GRASS_BLOCK);
        for (BlockPos low : List.of(origin.south(), origin.south().east())) {
            context.setBlockState(low, Blocks.AIR);
            context.setBlockState(low.down(), Blocks.AIR);
            context.setBlockState(low.down(2), Blocks.GRASS_BLOCK);
        }
        BlockPos at = context.getAbsolutePos(origin);

        context.assertTrue(LotLevel.flatGroundAt(context.getWorld(), UUID.randomUUID(),
                        at.getX(), at.getZ(), at.getY(), at.getY(), new ColonyPos(2, 2, 2)).isEmpty(),
                "o lote com metade da base dois blocos abaixo da rua foi aceito");
        context.complete();
    }

    /**
     * Chão um bloco acima da rua: o lote serve, e a casa assenta um acima —
     * decisão do autor, 2026-09-30.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "foundation_preparation")
    public void aLotOneAboveTheStreetIsAcceptedOneHigher(TestContext context) {
        BlockPos street = new BlockPos(5, 3, 5);
        for (BlockPos column : List.of(street, street.east(), street.south(), street.south().east())) {
            context.setBlockState(column, Blocks.GRASS_BLOCK);
            context.setBlockState(column.up(), Blocks.GRASS_BLOCK);
            context.setBlockState(column.up(2), Blocks.AIR);
            context.setBlockState(column.up(3), Blocks.AIR);
        }
        BlockPos at = context.getAbsolutePos(street);

        var floor = LotLevel.flatGroundAt(context.getWorld(), UUID.randomUUID(),
                at.getX(), at.getZ(), at.getY() + 1, at.getY(), new ColonyPos(2, 2, 2));

        context.assertTrue(floor.isPresent() && floor.get() == at.getY() + 2,
                "o lote um acima da rua devia assentar um acima: " + floor);
        context.complete();
    }

    /** Na frente da porta, dois abaixo do piso vira um degrau. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "foundation_preparation")
    public void aStepIsLaidWhereTheDoorWouldBeTwoAboveTheGround(TestContext context) {
        BlockPos floor = new BlockPos(5, 4, 5);
        BlockPos front = floor.north().down(3);
        context.setBlockState(front, Blocks.GRASS_BLOCK);
        context.setBlockState(front.up(), Blocks.AIR);
        context.setBlockState(front.up(2), Blocks.AIR);
        context.setBlockState(front.east(), Blocks.GRASS_BLOCK);
        context.setBlockState(front.east().up(), Blocks.AIR);
        context.setBlockState(front.east().up(2), Blocks.AIR);

        BlockPos at = context.getAbsolutePos(floor);
        BuildSiteScanner.Site site = new BuildSiteScanner.Site(
                MinecraftTypeAdapter.toColonyPos(at), net.minecraft.util.math.Direction.NORTH,
                new ColonyPos(2, 3, 2));

        DoorStep.placeIfNeeded(context.getWorld(), site);

        context.assertTrue(!context.getWorld().getBlockState(context.getAbsolutePos(front.up())).isAir(),
                "a frente da porta ficou dois abaixo do piso, sem degrau");
        context.assertTrue(context.getWorld().getBlockState(context.getAbsolutePos(front.up(2))).isAir(),
                "o degrau subiu até a altura do piso e fechou a porta");
        context.complete();
    }

    private static ConstructionProject project(TestContext context, BlockPos relativeOrigin) {
        Blueprint blueprint = Blueprint.of(ResourceId.vanilla("foundation_preparation"), List.of(
                new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("cobblestone")),
                new BlueprintBlock(new ColonyPos(1, 0, 0), ResourceId.vanilla("cobblestone")),
                new BlueprintBlock(new ColonyPos(0, 0, 1), ResourceId.vanilla("cobblestone")),
                new BlueprintBlock(new ColonyPos(1, 0, 1), ResourceId.vanilla("cobblestone"))
        )).withStreetLayer(0);
        ColonyPos origin = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(relativeOrigin));
        return ConstructionProject.plan(UUID.randomUUID(), blueprint, origin);
    }
}
