package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.coordination.BedPlan;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.VillageFoundation;
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

import java.util.Optional;
import java.util.UUID;

/**
 * A cama mais perto do trabalho — Regra 74, autor de 2026-10-22. O trabalho é onde o aldeão trabalhou de
 * fato (o diário); a troca acontece quando os dois andam menos, e o baú acompanha a cama.
 */
public class BedChoiceGameTest implements FabricGameTest {

    // Um lote para cada teste: os pisos de 26 blocos de dois cenários vizinhos se sobrepunham.

    /** Alto no lote: perto do chão há restos acima da arena, e o mapa a pé partiria de cima deles. */
    private static final int Y = 40;

    /** As duas pontas do cenário: mais longe que o ganho mínimo da Regra 74. */
    private static final BlockPos WEST = new BlockPos(2, Y + 1, 2);
    private static final BlockPos EAST = new BlockPos(2 + BedPlan.MIN_GAIN + 6, Y + 1, 2);

    /** O mineiro dorme a leste e cava a oeste; o construtor, o contrário: trocam de cama. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "bed_choice_swap", tickLimit = 40)
    public void twoWorkersSwapBedsWhenBothWalkLess(TestContext context) {
        floorAndBeds(context, WEST, EAST);

        Colony colony = colonyOver(context);
        VillagerEntity miner = worker(context, colony, ProfessionType.MINER, new BlockPos(4, Y + 1, 4));
        VillagerEntity builder = worker(context, colony, ProfessionType.BUILDER, new BlockPos(6, Y + 1, 4));
        BlockPos west = context.getAbsolutePos(WEST);
        BlockPos east = context.getAbsolutePos(EAST);
        ColonyPos minerChest = VillageColonyMod.STORAGES.of(miner.getUuid()).orElseThrow().chestPosition();
        ColonyPos builderChest = VillageColonyMod.STORAGES.of(builder.getUuid()).orElseThrow().chestPosition();

        context.waitAndRun(5, () -> {
            try {
                VillageFoundation.giveHome(context.getWorld(), miner, east);
                VillageFoundation.giveHome(context.getWorld(), builder, west);
                worksAt(miner, west.east());
                worksAt(builder, east.west());

                int moved = BedChoice.reassign(context.getWorld(), colony, colony.bounds().orElseThrow());

                context.assertTrue(moved == 2, "deviam mudar os dois, mudaram " + moved);
                context.assertTrue(isHome(miner, west) && isHome(builder, east), "a troca não aconteceu: mineiro em "
                        + homeOf(miner) + ", construtor em " + homeOf(builder));
                context.assertTrue(VillageColonyMod.STORAGES.of(miner.getUuid()).map(WorkerStorage::chestPosition)
                                .equals(Optional.of(builderChest))
                                && VillageColonyMod.STORAGES.of(builder.getUuid()).map(WorkerStorage::chestPosition)
                                .equals(Optional.of(minerChest)),
                        "o baú não acompanhou a cama: mineiro com " + VillageColonyMod.STORAGES.of(miner.getUuid())
                                + ", construtor com " + VillageColonyMod.STORAGES.of(builder.getUuid()));
            } finally {
                cleanUp(colony, miner, builder);
                clearScenario(context);
            }

            context.complete();
        });
    }

    /** Uma cama livre ao lado do trabalho: o mineiro vai para ela, mas só quando já se sabe onde ele trabalha. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "bed_choice_free", tickLimit = 40)
    public void aWorkerTakesTheFreeBedByHisWorkOnlyOnceHisWorkIsKnown(TestContext context) {
        floorAndBeds(context, WEST, EAST);
        context.setBlockState(WEST.west(), Blocks.CHEST.getDefaultState());

        Colony colony = colonyOver(context);
        VillagerEntity miner = worker(context, colony, ProfessionType.MINER, new BlockPos(4, Y + 1, 4));
        BlockPos west = context.getAbsolutePos(WEST);
        BlockPos east = context.getAbsolutePos(EAST);

        context.waitAndRun(5, () -> {
            try {
                VillageFoundation.giveHome(context.getWorld(), miner, east);

                for (int i = 0; i < BedChoice.MIN_SAMPLES - 1; i++) {
                    BedChoice.worked(miner.getUuid(), "MINED", west.east());
                }

                context.assertTrue(BedChoice.reassign(context.getWorld(), colony, colony.bounds().orElseThrow()) == 0,
                        "mudou de cama sem ações bastantes para saber onde ele trabalha");

                BedChoice.worked(miner.getUuid(), "MINED", west.east());

                context.assertTrue(BedChoice.reassign(context.getWorld(), colony, colony.bounds().orElseThrow()) == 1,
                        "não foi para a cama livre ao lado do trabalho");
                context.assertTrue(isHome(miner, west), "a cama dele ficou em " + homeOf(miner));
                context.assertTrue(VillageColonyMod.STORAGES.of(miner.getUuid()).map(WorkerStorage::chestPosition)
                                .equals(Optional.of(MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(WEST.west())))),
                        "o baú ao lado da cama nova não ficou com ele: " + VillageColonyMod.STORAGES.of(miner.getUuid()));
            } finally {
                cleanUp(colony, miner);
                clearScenario(context);
            }

            context.complete();
        });
    }

    /**
     * A cama livre sem baú ao lado não é oferecida — R-043 do estudo v2: quem mudasse ficaria sem baú, e o
     * trabalhador sem baú perde o ofício para quem tem (VacancyEnforcer). Ele vai para a próxima cama que tem
     * baú, se ela ainda vale a mudança.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "bed_choice_no_chest", tickLimit = 40)
    public void aFreeBedWithoutAChestBesideItIsNotOffered(TestContext context) {
        // A do meio, com baú, fica a 7 da cama oeste: fora do raio de 6 em que o baú vale para ela.
        BlockPos middle = new BlockPos(8, Y + 1, 2);
        floorAndBeds(context, WEST, middle, EAST);
        context.setBlockState(middle.east(), Blocks.CHEST.getDefaultState());

        Colony colony = colonyOver(context);
        VillagerEntity miner = worker(context, colony, ProfessionType.MINER, new BlockPos(4, Y + 1, 4));
        BlockPos west = context.getAbsolutePos(WEST);
        BlockPos east = context.getAbsolutePos(EAST);
        BlockPos withChest = context.getAbsolutePos(middle);

        context.waitAndRun(5, () -> {
            try {
                VillageFoundation.giveHome(context.getWorld(), miner, east);
                worksAt(miner, west.east());

                context.assertTrue(BedChoice.reassign(context.getWorld(), colony, colony.bounds().orElseThrow()) == 1,
                        "não mudou para a cama com baú");
                context.assertTrue(isHome(miner, withChest), "devia ir para a cama com baú, e foi para " + homeOf(miner)
                        + (isHome(miner, west) ? " — a cama sem baú" : ""));
            } finally {
                cleanUp(colony, miner);
                clearScenario(context);
            }

            context.complete();
        });
    }

    /** Controle: a cama livre cercada por parede não se alcança a pé, e ninguém vai para ela. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "bed_choice_walled", tickLimit = 40)
    public void aFreeBedNobodyReachesOnFootIsNotOffered(TestContext context) {
        floorAndBeds(context, WEST, EAST);

        // Dois de espessura: o alcance aceita ficar a até dois blocos da cama.
        for (int x = WEST.getX() - 2; x <= WEST.getX() + 2; x++) {
            for (int z = WEST.getZ() - 2; z <= WEST.getZ() + 3; z++) {
                if (x != WEST.getX() || z < WEST.getZ() || z > WEST.getZ() + 1) {
                    for (int y = Y + 1; y <= Y + 3; y++) {
                        context.setBlockState(new BlockPos(x, y, z), Blocks.STONE.getDefaultState());
                    }
                }
            }
        }

        Colony colony = colonyOver(context);
        VillagerEntity miner = worker(context, colony, ProfessionType.MINER, new BlockPos(8, Y + 1, 4));
        BlockPos west = context.getAbsolutePos(WEST);
        BlockPos east = context.getAbsolutePos(EAST);

        context.waitAndRun(5, () -> {
            try {
                VillageFoundation.giveHome(context.getWorld(), miner, east);
                worksAt(miner, west.east(2));

                context.assertTrue(BedChoice.reassign(context.getWorld(), colony, colony.bounds().orElseThrow()) == 0,
                        "foi para uma cama cercada que ninguém alcança a pé");
            } finally {
                cleanUp(colony, miner);
                clearScenario(context);
            }

            context.complete();
        });
    }

    private static void floorAndBeds(TestContext context, BlockPos... heads) {
        for (int x = 0; x <= EAST.getX() + 2; x++) {
            for (int z = 0; z <= 6; z++) {
                context.setBlockState(new BlockPos(x, Y, z), Blocks.STONE.getDefaultState());
            }
        }

        for (BlockPos head : heads) {
            context.setBlockState(head, Blocks.WHITE_BED.getDefaultState()
                    .with(BedBlock.PART, BedPart.HEAD).with(BedBlock.FACING, Direction.NORTH));
            context.setBlockState(head.south(), Blocks.WHITE_BED.getDefaultState()
                    .with(BedBlock.PART, BedPart.FOOT).with(BedBlock.FACING, Direction.NORTH));
        }
    }

    private static Colony colonyOver(TestContext context) {
        BlockPos at = context.getAbsolutePos(new BlockPos(0, Y - 2, 0));
        BlockPos far = context.getAbsolutePos(new BlockPos(EAST.getX() + 2, Y + 6, 6));
        // O centro em cima do piso: é de lá que o mapa a pé (ReachMap) mede o que se alcança.
        Colony colony = Colony.create(UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(EAST.getX() / 2, Y + 1, 4))));
        colony.measure(new VillageBounds(Math.min(at.getX(), far.getX()), at.getY(), Math.min(at.getZ(), far.getZ()),
                Math.max(at.getX(), far.getX()), far.getY(), Math.max(at.getZ(), far.getZ())));
        VillageColonyMod.COLONIES.register(colony);

        return colony;
    }

    private static VillagerEntity worker(TestContext context, Colony colony, ProfessionType profession, BlockPos at) {
        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, at);
        VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id()).assign(profession);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(at.south()))));

        return villager;
    }

    private static void worksAt(VillagerEntity villager, BlockPos place) {
        for (int i = 0; i < BedChoice.MIN_SAMPLES; i++) {
            BedChoice.worked(villager.getUuid(), "PLACED", place);
        }
    }

    private static boolean isHome(VillagerEntity villager, BlockPos head) {
        return villager.getBrain().getOptionalRegisteredMemory(MemoryModuleType.HOME)
                .map(GlobalPos::pos).filter(head::equals).isPresent();
    }

    private static Object homeOf(VillagerEntity villager) {
        return villager.getBrain().getOptionalRegisteredMemory(MemoryModuleType.HOME).map(GlobalPos::pos);
    }

    private static void clearScenario(TestContext context) {
        for (int x = 0; x <= EAST.getX() + 2; x++) {
            for (int z = 0; z <= 6; z++) {
                for (int y = Y; y <= Y + 3; y++) {
                    context.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState());
                }
            }
        }
    }

    private static void cleanUp(Colony colony, VillagerEntity... villagers) {
        for (VillagerEntity villager : villagers) {
            VillageColonyMod.STORAGES.remove(villager.getUuid());
            VillageColonyMod.WORKERS.remove(villager.getUuid());
            BedChoice.forget(villager.getUuid());
            villager.discard();
        }

        VillageColonyMod.COLONIES.remove(colony.id());
    }
}
