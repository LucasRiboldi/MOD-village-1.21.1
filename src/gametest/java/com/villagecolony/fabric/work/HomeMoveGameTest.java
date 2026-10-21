package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.GlobalPos;

import java.util.UUID;

/**
 * Quem não alcança o trabalho porque mora abaixo dele muda de casa — autor, 2026-10-21 — para uma cama
 * livre da vila na altura do trabalho. Sem cama livre lá, nada muda; na mesma altura, não conta.
 */
public class HomeMoveGameTest implements FabricGameTest {

    private static final BlockPos BED = new BlockPos(5, 5, 5);

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "home_move", tickLimit = 40)
    public void twoGiveUpsBelowTheWorkMoveTheWorkerToAFreeBedUpThere(TestContext context) {
        context.setBlockState(BED.down(), Blocks.STONE.getDefaultState());
        context.setBlockState(BED.down().south(), Blocks.STONE.getDefaultState());
        bed(context, BED);

        Colony colony = colonyOver(context);
        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(1, 2, 1));
        VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(1, 2, 2)))));
        BlockPos way = context.getAbsolutePos(BED.east());
        BlockPos head = context.getAbsolutePos(BED);

        context.waitAndRun(5, () -> {
            try {
                HomeMove.gaveUpOnTheWayTo(villager.getUuid(), way);
                HomeMove.pass(context.getWorld());
                context.assertFalse(isHome(villager, head), "mudou de casa na primeira desistência");

                HomeMove.gaveUpOnTheWayTo(villager.getUuid(), way);
                HomeMove.pass(context.getWorld());
                context.assertTrue(isHome(villager, head), "duas desistências abaixo do trabalho e não mudou de casa: "
                        + villager.getBrain().getOptionalRegisteredMemory(MemoryModuleType.HOME));
                context.assertTrue(VillageColonyMod.STORAGES.of(villager.getUuid()).isEmpty(),
                        "o baú da casa velha continuou com ele");
            } finally {
                cleanUp(colony, villager);
            }

            context.complete();
        });
    }

    /** Controle: na mesma altura do trabalho, a desistência não é por morar abaixo, e não conta. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "home_move", tickLimit = 40)
    public void givingUpAtTheSameHeightNeverMovesHome(TestContext context) {
        context.setBlockState(BED.down(), Blocks.STONE.getDefaultState());
        context.setBlockState(BED.down().south(), Blocks.STONE.getDefaultState());
        bed(context, BED);

        Colony colony = colonyOver(context);
        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(1, 2, 1));
        VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        BlockPos way = context.getAbsolutePos(new BlockPos(6, 2, 1));

        context.waitAndRun(5, () -> {
            try {
                for (int time = 0; time < HomeMove.STRIKES + 1; time++) {
                    HomeMove.gaveUpOnTheWayTo(villager.getUuid(), way);
                    HomeMove.pass(context.getWorld());
                }

                context.assertFalse(isHome(villager, context.getAbsolutePos(BED)),
                        "mudou de casa sem estar abaixo do trabalho");
            } finally {
                cleanUp(colony, villager);
            }

            context.complete();
        });
    }

    /** Controle: sem cama livre na altura do trabalho, não muda. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "home_move", tickLimit = 40)
    public void withoutAFreeBedUpThereNothingMoves(TestContext context) {
        Colony colony = colonyOver(context);
        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(1, 2, 1));
        VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());

        context.waitAndRun(5, () -> {
            try {
                context.assertFalse(HomeMove.move(context.getWorld(), villager, context.getAbsolutePos(BED.east())),
                        "mudou de casa sem cama livre na altura do trabalho");
            } finally {
                cleanUp(colony, villager);
            }

            context.complete();
        });
    }

    /** Sem cama livre em cima, troca de casa com o vizinho sem ofício de lá; o de ofício não troca. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "home_move", tickLimit = 40)
    public void withoutAFreeBedItSwapsHomesWithAnIdleNeighbourUpThere(TestContext context) {
        BlockPos low = new BlockPos(1, 2, 6);
        for (BlockPos head : new BlockPos[] {BED, low}) {
            context.setBlockState(head.down(), Blocks.STONE.getDefaultState());
            context.setBlockState(head.down().south(), Blocks.STONE.getDefaultState());
            bed(context, head);
        }

        Colony colony = colonyOver(context);
        VillagerEntity worker = context.spawnEntity(EntityType.VILLAGER, new BlockPos(1, 2, 1));
        VillagerEntity idle = context.spawnEntity(EntityType.VILLAGER, new BlockPos(3, 2, 1));
        VillageColonyMod.WORKERS.register(worker.getUuid(), colony.id())
                .assign(com.villagecolony.core.worker.model.ProfessionType.BUILDER);
        BlockPos up = context.getAbsolutePos(BED);
        BlockPos down = context.getAbsolutePos(low);

        context.waitAndRun(5, () -> {
            try {
                com.villagecolony.fabric.integration.VillageFoundation.giveHome(context.getWorld(), worker, down);
                com.villagecolony.fabric.integration.VillageFoundation.giveHome(context.getWorld(), idle, up);

                context.assertTrue(HomeMove.move(context.getWorld(), worker, up.east()),
                        "sem cama livre, não trocou de casa com o vizinho sem ofício");
                context.assertTrue(isHome(worker, up) && isHome(idle, down),
                        "a troca de casa não aconteceu: trabalhador em "
                                + worker.getBrain().getOptionalRegisteredMemory(MemoryModuleType.HOME)
                                + ", vizinho em " + idle.getBrain().getOptionalRegisteredMemory(MemoryModuleType.HOME));

                // Controle: com ofício, o vizinho de cima não troca.
                VillageColonyMod.WORKERS.register(idle.getUuid(), colony.id())
                        .assign(com.villagecolony.core.worker.model.ProfessionType.FARMER);
                context.assertFalse(HomeMove.move(context.getWorld(), idle, down.east().up(4)),
                        "controle: trocou de casa com um vizinho que tem ofício");
            } finally {
                VillageColonyMod.WORKERS.remove(idle.getUuid());
                idle.discard();
                cleanUp(colony, worker);
            }

            context.complete();
        });
    }

    private static Colony colonyOver(TestContext context) {
        BlockPos at = context.getAbsolutePos(BlockPos.ORIGIN);
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(at));
        colony.measure(new VillageBounds(at.getX(), at.getY(), at.getZ(), at.getX() + 8, at.getY() + 8, at.getZ() + 8));
        VillageColonyMod.COLONIES.register(colony);

        return colony;
    }

    private static void bed(TestContext context, BlockPos head) {
        context.setBlockState(head, Blocks.WHITE_BED.getDefaultState()
                .with(BedBlock.PART, BedPart.HEAD).with(BedBlock.FACING, Direction.NORTH));
        context.setBlockState(head.south(), Blocks.WHITE_BED.getDefaultState()
                .with(BedBlock.PART, BedPart.FOOT).with(BedBlock.FACING, Direction.NORTH));
    }

    private static boolean isHome(VillagerEntity villager, BlockPos head) {
        return villager.getBrain().getOptionalRegisteredMemory(MemoryModuleType.HOME)
                .map(GlobalPos::pos).filter(head::equals).isPresent();
    }

    private static void cleanUp(Colony colony, VillagerEntity villager) {
        VillageColonyMod.STORAGES.remove(villager.getUuid());
        VillageColonyMod.WORKERS.remove(villager.getUuid());
        VillageColonyMod.COLONIES.remove(colony.id());
        HomeMove.clearAll();
        villager.discard();
    }
}
