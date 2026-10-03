package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.event.VillageDetectionHandler;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Optional;

/**
 * Camas abaixo da superfície não são vila — ADR-003 Emenda 6, 2026-09-30.
 *
 * <p>O save do autor tinha 67 camas de Trial Chamber no subsolo, duas
 * embaixo de vilas. O gatilho da cama de chunk carregado varria em volta
 * delas, e os mineiros que cavam ali embaixo validariam o aglomerado. Aqui a
 * "superfície" é um bloco 40 acima das camas: a busca procura entre 16
 * abaixo e 24 acima dela, e as camas ficam de fora.
 *
 * <p>Longe da arena, como {@code aVillageWithoutASafeBigHouseLotIsNotAdopted}:
 * a caixa de uma colônia de outro teste ditaria outra janela.
 */
public final class BedWindowGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE,
            batchId = "colony_detection", tickLimit = 120)
    public void bedsFarBelowTheSurfaceAreNotAVillage(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos anchor = context.getAbsolutePos(new BlockPos(1, 2, 1))
                .add(1_200_000, 0, 1_300_000);

        for (int i = 0; i < VillageDetector.MIN_BEDS; i++) {
            BlockPos head = anchor.add(i * 2, 0, 0);

            world.setBlockState(head, Blocks.WHITE_BED.getDefaultState()
                    .with(BedBlock.PART, BedPart.HEAD).with(BedBlock.FACING, Direction.NORTH));
            world.setBlockState(head.offset(Direction.SOUTH), Blocks.WHITE_BED.getDefaultState()
                    .with(BedBlock.PART, BedPart.FOOT).with(BedBlock.FACING, Direction.NORTH));
        }

        for (int i = 0; i < VillageDetector.MIN_VILLAGERS; i++) {
            VillagerEntity villager = EntityType.VILLAGER.create(world);
            if (villager == null) {
                throw new AssertionError("nao foi possivel criar aldeao");
            }
            villager.refreshPositionAndAngles(
                    anchor.getX() + i + 0.5, anchor.getY(), anchor.getZ() + 4.5, 0.0F, 0.0F);
            villager.setBreedingAge(0);
            world.spawnEntity(villager);
        }

        // A superfície 40 blocos acima: as camas ficam "no subsolo".
        world.setBlockState(anchor.up(40), Blocks.STONE.getDefaultState());

        try {
            VillageDetectionHandler.runCycleNow(world, anchor);

            context.assertFalse(colonyAt(anchor).isPresent(),
                    "camas 40 blocos abaixo da superfície viraram vila");
        } finally {
            colonyAt(anchor).ifPresent(BedWindowGameTest::forget);
        }

        context.complete();
    }

    private static Optional<Colony> colonyAt(BlockPos anchor) {
        return VillageColonyMod.COLONIES.findNearest(
                MinecraftTypeAdapter.toColonyPos(anchor), VillageDetector.DUPLICATE_DISTANCE);
    }

    private static void forget(Colony colony) {
        ColonyFixture owned = ColonyFixture.create().owning(colony);

        for (Worker worker : VillageColonyMod.WORKERS.ofColony(colony.id())) {
            owned.owning(worker.villagerId());
        }

        owned.cleanUp();
    }
}
