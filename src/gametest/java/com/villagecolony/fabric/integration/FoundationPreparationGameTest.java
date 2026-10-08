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
    public void aHouseRemovesOnlyGrassFromItsPlannedBase(TestContext context) {
        BlockPos origin = new BlockPos(5, 2, 5);
        context.setBlockState(origin, Blocks.GRASS_BLOCK);
        context.setBlockState(origin.east(), Blocks.SAND);
        context.setBlockState(origin.south(), Blocks.STONE);
        context.setBlockState(origin.south().east(), Blocks.DIRT);

        SitePreparation.clear(context.getWorld(), project(context, origin));

        context.assertTrue(context.getBlockState(origin).isOf(Blocks.DIRT),
                "a relva sob a base da casa deve virar terra");
        context.assertTrue(context.getBlockState(origin.east()).isOf(Blocks.SAND),
                "a preparação removeu areia da base");
        context.assertTrue(context.getBlockState(origin.south()).isOf(Blocks.STONE),
                "a preparação removeu pedra da base");
        context.assertTrue(context.getBlockState(origin.south().east()).isOf(Blocks.DIRT),
                "a preparação alterou terra da base");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "foundation_preparation")
    public void aFarmKeepsGrassInItsPlannedBase(TestContext context) {
        BlockPos origin = new BlockPos(5, 2, 5);
        context.setBlockState(origin, Blocks.GRASS_BLOCK);

        Blueprint farm = Blueprint.of(
                ResourceId.vanilla("village/plains/houses/plains_small_farm_1"),
                List.of(new BlueprintBlock(
                        new ColonyPos(0, 0, 0), ResourceId.vanilla("oak_planks"))))
                .withStreetLayer(0);
        ConstructionProject project = ConstructionProject.plan(
                UUID.randomUUID(), farm,
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(origin)));

        SitePreparation.clear(context.getWorld(), project);

        context.assertTrue(context.getBlockState(origin).isOf(Blocks.GRASS_BLOCK),
                "a preparação da plantação removeu a relva");
        context.complete();
    }

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

        // Pedido do autor, 2026-10-02: o degrau é escada de madeira, subindo para a casa.
        net.minecraft.block.BlockState step = context.getWorld().getBlockState(context.getAbsolutePos(front.up()));

        context.assertTrue(step.isOf(Blocks.OAK_STAIRS), "o degrau devia ser escada de madeira, é " + step);
        context.assertTrue(step.get(net.minecraft.block.StairsBlock.FACING) == net.minecraft.util.math.Direction.SOUTH,
                "a escada devia subir para a casa (sul), sobe para " + step.get(net.minecraft.block.StairsBlock.FACING));
        context.complete();
    }

    /** Três abaixo do piso: terra embaixo, escada em cima. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "foundation_preparation")
    public void aDeeperFrontGetsGroundUnderTheWoodenStair(TestContext context) {
        BlockPos floor = new BlockPos(5, 5, 5);
        BlockPos front = floor.north().down(4);
        context.setBlockState(front, Blocks.GRASS_BLOCK);
        context.setBlockState(front.east(), Blocks.GRASS_BLOCK);

        for (int up = 1; up <= 3; up++) {
            context.setBlockState(front.up(up), Blocks.AIR);
            context.setBlockState(front.east().up(up), Blocks.AIR);
        }

        BuildSiteScanner.Site site = new BuildSiteScanner.Site(
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(floor)),
                net.minecraft.util.math.Direction.NORTH, new ColonyPos(2, 3, 2));

        DoorStep.placeIfNeeded(context.getWorld(), site);

        net.minecraft.block.BlockState below = context.getWorld().getBlockState(context.getAbsolutePos(front.up()));
        net.minecraft.block.BlockState top = context.getWorld().getBlockState(context.getAbsolutePos(front.up(2)));

        context.assertTrue(!below.isAir() && !below.isOf(Blocks.OAK_STAIRS),
                "embaixo da escada devia ir solo, foi " + below);
        context.assertTrue(top.isOf(Blocks.OAK_STAIRS), "o degrau de cima devia ser escada de madeira, é " + top);
        context.assertTrue(context.getWorld().getBlockState(context.getAbsolutePos(front.up(3))).isAir(),
                "o degrau subiu até a altura do piso");
        context.complete();
    }

    /**
     * A obra um acima de chão plano não se qualifica, e a recusa diz por quê —
     * o lote do playtest de 2026-10-07: base em y70 sobre grama em y69, a obra
     * "AVAILABLE with nobody" por 37 minutos sem uma linha de causa.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "foundation_preparation")
    public void aBaseOneAboveFlatGroundIsRefusedWithItsReason(TestContext context) {
        BlockPos ground = new BlockPos(5, 1, 5);
        for (BlockPos column : List.of(ground, ground.east(), ground.south(), ground.south().east())) {
            context.setBlockState(column, Blocks.GRASS_BLOCK);
            context.setBlockState(column.up(), Blocks.AIR);
        }

        ConstructionProject oneAbove = project(context, ground.up());

        java.util.Optional<String> refusal = FoundationPreparation.refusal(context.getWorld(), oneAbove);
        context.assertTrue(refusal.isPresent() && refusal.get().contains("0 of 4"),
                "a base um acima do chão plano devia ser recusada por falta de apoio, veio " + refusal);
        context.assertFalse(FoundationPreparation.prepareIfQualified(context.getWorld(), oneAbove),
                "a preparação aceitou a base sem apoio");
        context.assertTrue(context.getBlockState(ground.up()).isAir(),
                "a preparação recusada aterrou a base");
        context.assertTrue(FoundationPreparation.refusal(context.getWorld(), project(context, ground)).isEmpty(),
                "a base na altura do chão foi recusada");
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
