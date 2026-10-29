package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestInventoryReader;
import com.villagecolony.fabric.work.ShepherdWork;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * O pastor — 2026-08-20.
 *
 * <p>Ele fecha o laço da vila: casa, cama, aldeão novo, trabalhador,
 * casa. A Regra 21 pôs a cama em toda casa e registrou o elo que
 * faltava — lã pede tosquia, e ninguém tosquiava.
 */
public class ShepherdGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(2, 2, 2);

    private static final BlockPos STAND = new BlockPos(3, 2, 3);

    /** A ovelha, ao alcance do braço de quem está no STAND. */
    private static final BlockPos PEN = new BlockPos(4, 2, 3);

    /**
     * A lã sai da ovelha e entra no baú, e a ovelha continua viva.
     *
     * <p>A segunda metade é a que torna esta colheita diferente das
     * outras: a lã volta a crescer sozinha, e o rebanho fica. É a Regra 7
     * de graça — quem replanta aqui é o próprio jogo.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "shepherd",
            tickLimit = 200)
    public void theWoolLeavesTheSheepAndTheSheepStays(TestContext context) {
        ground(context);

        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        Colony colony = Colony.create(UUID.randomUUID(), chest);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        SheepEntity sheep = context.spawnEntity(EntityType.SHEEP, PEN);
        sheep.setColor(DyeColor.WHITE);
        sheep.setSheared(false);
        sheep.setBreedingAge(0);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, STAND);
        villager.setBreedingAge(0);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.SHEPHERD);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));

        owned.owning(villager.getUuid());

        Task task = VillageColonyMod.TASKS.create(
                colony.id(),
                TaskType.COLLECT_WOOL,
                TaskPriority.PRODUCTION,
                ResourceType.WHITE_WOOL,
                8);

        task.reserveFor(villager.getUuid());

        // Raio curto: a bateria roda arenas vizinhas no mesmo mundo, e um
        // pastor de raio 32 tosquiaria o rebanho do teste do lado.
        ShepherdWork.shortenSearchTo(4);

        ShepherdWork.run(context.getWorld(), colony);

        context.runAtTick(150, () -> {
            int wool = ChestInventoryReader
                    .read(context.getWorld(), context.getAbsolutePos(CHEST))
                    .amountOfGroup(ResourceGroup.WOOL);

            try {
                context.assertTrue(wool > 0, "a lã não chegou ao baú");

                context.assertTrue(sheep.isAlive(), "o pastor matou a ovelha em vez de tosquiá-la");

                context.assertTrue(sheep.isSheared(), "a lã apareceu e a ovelha continua lanuda");
            } finally {
                owned.cleanUp();

                ShepherdWork.restoreSearch();
            }

            context.complete();
        });
    }

    /**
     * O guarda do pastor não conta fora do expediente — 2026-09-03.
     *
     * <p><b>Ele era a única profissão que andava sem esta conferência.</b>
     * Mineiro, lenhador e fazendeiro gateiam o contador por
     * {@code WorkHours}; construtor e fabricante saem cedo do tique
     * inteiro quando não é hora. O pastor contava a noite toda.
     *
     * <p>É o defeito que o mineiro teve em 2026-08-26: o contador foi de
     * 886 a 2086 com o relatório dizendo {@code off hours}, e metade do
     * orçamento de dois minutos queimou com o aldeão dormindo. Fora da
     * hora a {@code GoToWorkTargetTask} nem começa — o aldeão está
     * <b>proibido</b> de andar até a ovelha, e o guarda existe para punir
     * quem anda sem chegar.
     *
     * <p><b>Por que criança e não noite.</b> {@code WorkHours} responde não
     * para bebê sem depender do relógio. Mexer na hora do mundo é global e
     * vaza para os testes vizinhos do mesmo lote.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "shepherd_off_hours",
            tickLimit = 100)
    public void theStallGuardDoesNotCountOutsideWorkHours(TestContext context) {
        ground(context);

        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        Colony colony = Colony.create(UUID.randomUUID(), chest);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        // A ovelha longe do braço, para o pastor ficar no ramo que conta.
        SheepEntity sheep = context.spawnEntity(EntityType.SHEEP, new BlockPos(6, 2, 6));
        sheep.setColor(DyeColor.WHITE);
        sheep.setSheared(false);
        sheep.setBreedingAge(0);

        // Criança: WorkHours diz não sem que o relógio do mundo mude.
        VillagerEntity child = context.spawnEntity(EntityType.VILLAGER, STAND);
        child.setBreedingAge(-24_000);

        Worker worker = VillageColonyMod.WORKERS.register(child.getUuid(), colony.id());
        worker.assign(ProfessionType.SHEPHERD);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(child.getUuid(), chest));

        owned.owning(child.getUuid());

        Task task = VillageColonyMod.TASKS.create(
                colony.id(),
                TaskType.COLLECT_WOOL,
                TaskPriority.PRODUCTION,
                ResourceType.WHITE_WOOL,
                8);

        task.reserveFor(child.getUuid());

        ShepherdWork.shortenSearchTo(8);

        ShepherdWork.run(context.getWorld(), colony);

        context.runAtTick(60, () -> {
            int stalled = ShepherdWork.stallOf(child.getUuid());

            try {
                context.assertTrue(
                        stalled == 0,
                        "o guarda contou " + stalled + " tiques fora do expediente, e fora"
                                + " dele o aldeão está proibido de andar até a ovelha");
            } finally {
                owned.cleanUp();

                ShepherdWork.restoreSearch();
            }

            context.complete();
        });
    }

    /**
     * O pastor tosquia por cima da cerca e não entra no curral — T2 do diagnóstico da 0.3.103 (autor, 2026-10-28:
     * 45 pastores presos no curral). A ovelha fica num cercado fechado, a 4 blocos do pastor do lado de fora: o
     * braço não chega, o olho chega (4,5 blocos, como o fazendeiro).
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "shepherd",
            tickLimit = 200)
    public void theShepherdShearsOverTheFenceWithoutGoingIn(TestContext context) {
        // Cercado de 7 x 7 fechado, a ovelha no meio: de fora, o pé fica a 4 blocos dela e o braço (3) não chega.
        BlockPos inside = new BlockPos(5, 2, 5);
        BlockPos outside = new BlockPos(5, 2, 9);
        BlockPos pastureChest = new BlockPos(0, 2, 0);

        for (int x = 0; x <= 10; x++) {
            for (int z = 0; z <= 10; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.DIRT.getDefaultState());

                boolean ring = (x == 2 || x == 8) && z >= 2 && z <= 8 || (z == 2 || z == 8) && x >= 2 && x <= 8;

                if (ring) {
                    context.setBlockState(new BlockPos(x, 2, z), Blocks.OAK_FENCE.getDefaultState());
                }
            }
        }

        context.setBlockState(pastureChest, Blocks.CHEST.getDefaultState());

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(pastureChest));
        Colony colony = Colony.create(UUID.randomUUID(), chest);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        SheepEntity sheep = context.spawnEntity(EntityType.SHEEP, inside);
        sheep.setColor(DyeColor.WHITE);
        sheep.setSheared(false);
        sheep.setBreedingAge(0);
        sheep.setAiDisabled(true);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, outside);
        villager.setBreedingAge(0);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.SHEPHERD);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));

        owned.owning(villager.getUuid());

        Task task = VillageColonyMod.TASKS.create(
                colony.id(), TaskType.COLLECT_WOOL, TaskPriority.PRODUCTION, ResourceType.WHITE_WOOL, 8);

        task.reserveFor(villager.getUuid());

        ShepherdWork.shortenSearchTo(8);

        ShepherdWork.run(context.getWorld(), colony);

        context.runAtTick(150, () -> {
            try {
                context.assertTrue(sheep.isSheared(), "a ovelha do cercado continua lanuda: o pastor não tosquiou de fora");
                BlockPos at = context.getRelativePos(villager.getBlockPos());
                boolean inThePen = at.getX() > 2 && at.getX() < 8 && at.getZ() > 2 && at.getZ() < 8;

                context.assertTrue(!inThePen, "o pastor entrou no curral: está em " + at.toShortString());
            } finally {
                owned.cleanUp();

                ShepherdWork.restoreSearch();
            }

            context.complete();
        });
    }

    private static void ground(TestContext context) {
        for (int x = 0; x <= 6; x++) {
            for (int z = 0; z <= 6; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.DIRT.getDefaultState());
            }
        }
    }
}
