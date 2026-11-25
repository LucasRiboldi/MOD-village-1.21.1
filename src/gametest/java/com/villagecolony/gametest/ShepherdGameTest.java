package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.coordination.WorkClock;
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
import net.minecraft.block.FenceBlock;
import net.minecraft.block.FenceGateBlock;
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

    /** A altura da arena da tosquia, acima das cercas das outras arenas. */
    private static final int HIGH = 24;

    /**
     * A lã sai da ovelha e entra no baú, e a ovelha continua viva.
     *
     * <p>A segunda metade é a que torna esta colheita diferente das
     * outras: a lã volta a crescer sozinha, e o rebanho fica. É a Regra 7
     * de graça — quem replanta aqui é o próprio jogo.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "shepherd_the_wool_leaves_the_sheep_and_the_sheep_stays",
            tickLimit = 200)
    public void theWoolLeavesTheSheepAndTheSheepStays(TestContext context) {
        // A hora é global: fora do expediente o pastor não trabalha (Regra 195).
        context.getWorld().setTimeOfDay(1_000);
        // Acima das outras arenas: a cerca de uma vizinha a até 16 blocos faria ele levar a ovelha (Regra 196).
        BlockPos chestAt = CHEST.up(HIGH);
        BlockPos sheepAt = PEN.up(HIGH);
        BlockPos standAt = STAND.up(HIGH);

        for (int x = 0; x <= 6; x++) {
            for (int z = 0; z <= 6; z++) {
                context.setBlockState(new BlockPos(x, 1 + HIGH, z), Blocks.DIRT.getDefaultState());
            }
        }

        context.setBlockState(chestAt, Blocks.CHEST.getDefaultState());

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(chestAt));

        Colony colony = Colony.create(UUID.randomUUID(), chest);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        SheepEntity sheep = context.spawnEntity(EntityType.SHEEP, sheepAt);
        sheep.setColor(DyeColor.WHITE);
        sheep.setSheared(false);
        sheep.setBreedingAge(0);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, standAt);
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
                    .read(context.getWorld(), context.getAbsolutePos(chestAt))
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
     * Regra 150, revista pelo playtest da 0.4.115: a ovelha a quatro blocos não perde lã;
     * depois de chegar junto dela, o pastor tosquia e entrega a lã física no baú.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "shepherd_shearing_requires_contact",
            tickLimit = 360)
    public void theShepherdOnlyShearsAfterReachingTheSheep(TestContext context) {
        // A tosquia é trabalho: a hora é deste teste, e não a que o lote anterior deixou — VF-011.
        context.getWorld().setTimeOfDay(1_000);

        // Cercado fechado e isolado em altura: depois da conferência do alcance, o pastor deve chegar junto da ovelha.
        int height = 96;
        BlockPos inside = new BlockPos(5, height, 5);
        BlockPos outside = new BlockPos(5, height, 9);
        BlockPos pastureChest = new BlockPos(0, height, 0);

        Arena.forceChunks(context, new BlockPos(-2, 0, -2), new BlockPos(12, 0, 12), true);
        for (int x = -2; x <= 12; x++) {
            for (int z = -2; z <= 12; z++) {
                context.setBlockState(new BlockPos(x, height - 1, z), Blocks.DIRT.getDefaultState());

                boolean ring = (x == 2 || x == 8) && z >= 2 && z <= 8 || (z == 2 || z == 8) && x >= 2 && x <= 8;

                if (ring) {
                    context.setBlockState(new BlockPos(x, height, z), Blocks.OAK_FENCE.getDefaultState()
                            .with(FenceBlock.NORTH, (x == 2 || x == 8) && z > 2)
                            .with(FenceBlock.SOUTH, (x == 2 || x == 8) && z < 8)
                            .with(FenceBlock.WEST, (z == 2 || z == 8) && x > 2)
                            .with(FenceBlock.EAST, (z == 2 || z == 8) && x < 8));
                }
            }
        }

        BlockPos gate = new BlockPos(5, height, 8);
        context.setBlockState(gate, Blocks.OAK_FENCE_GATE.getDefaultState());

        context.setBlockState(pastureChest, Blocks.CHEST.getDefaultState());

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(pastureChest));
        Colony colony = Colony.create(UUID.randomUUID(), chest);
        // A caixa é a arena: sem ela o pastor recolhia bicho de outro teste a até 48 blocos (Regra 283, KF-011).
        BlockPos low = context.getAbsolutePos(new BlockPos(0, height - 2, 0));
        BlockPos high = context.getAbsolutePos(new BlockPos(10, height + 4, 10));
        colony.measure(new com.villagecolony.core.colony.model.VillageBounds(Math.min(low.getX(), high.getX()), low.getY(),
                Math.min(low.getZ(), high.getZ()), Math.max(low.getX(), high.getX()), high.getY(), Math.max(low.getZ(), high.getZ())));

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        SheepEntity sheep = context.spawnEntity(EntityType.SHEEP, inside);
        sheep.setColor(DyeColor.WHITE);
        sheep.setSheared(false);
        sheep.setBreedingAge(0);
        sheep.setAiDisabled(true);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, outside);
        villager.setBreedingAge(0);
        villager.setAiDisabled(true);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.SHEPHERD);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));

        owned.owning(villager.getUuid());

        Task task = VillageColonyMod.TASKS.create(
                colony.id(), TaskType.COLLECT_WOOL, TaskPriority.PRODUCTION, ResourceType.WHITE_WOOL, 8);

        task.reserveFor(villager.getUuid());

        ShepherdWork.shortenSearchTo(8);

        ShepherdWork.run(context.getWorld(), colony);

        // Os testes rodam juntos num mundo só: o relógio e o raio são de todos. A cada tique, o expediente e o raio
        // voltam a ser os deste teste, até a conferência devolver o raio.
        boolean[] checked = {false};
        Runnable cleanup = () -> {
            checked[0] = true;
            owned.cleanUp();
            Arena.forceChunks(context, new BlockPos(-2, 0, -2), new BlockPos(12, 0, 12), false);
            ShepherdWork.restoreSearch();
        };
        // Diagnóstico da tosquia: hora, idade, posição do pastor, destino e tarefa, em recorte de 80 tiques.
        StringBuilder history = new StringBuilder();
        int[] tick = {0};
        String[] lastChange = {""};
        context.runAtEveryTick(() -> {
            if (checked[0]) {
                return;
            }
            if (!WorkClock.isWorkTime(context.getWorld().getTimeOfDay())) {
                context.getWorld().setTimeOfDay(1_000);
            }
            ShepherdWork.shortenSearchTo(8);
            // Cada troca de tarefa ou de condução por outro (socorro do curral, WalkOverride) entra no histórico.
            String change = task.state() + "/" + com.villagecolony.fabric.brain.WalkOverride.isHeld(villager.getUuid());
            if (!change.equals(lastChange[0])) {
                lastChange[0] = change;
                history.append(" | t").append(tick[0] + 1).append(" troca task/conduzido=").append(change)
                        .append(" p").append(villager.getBlockPos().subtract(context.getAbsolutePos(BlockPos.ORIGIN))
                                .toShortString());
            }
            if (++tick[0] % 80 == 0) {
                BlockPos origin = context.getAbsolutePos(BlockPos.ORIGIN);
                history.append(" | t").append(tick[0]).append(" d").append(context.getWorld().getTimeOfDay() % 24_000)
                        .append(" idade=").append(villager.age)
                        .append(" p").append(villager.getBlockPos().subtract(origin).toShortString())
                        .append(" n").append(villager.getNavigation().getCurrentPath() == null ? "idle"
                                : villager.getNavigation().getCurrentPath().getTarget().subtract(origin).toShortString())
                        .append(" alvo=").append(villager.getNavigation().getCurrentPath() == null ? "-"
                                : context.getWorld().getBlockState(villager.getNavigation().getCurrentPath().getTarget())
                                        .getBlock().getTranslationKey().replace("block.minecraft.", "")
                                        .replace("block.villagecolony.", "vc:"))
                        .append(" mem=").append(villager.getBrain().getOptionalRegisteredMemory(
                                net.minecraft.entity.ai.brain.MemoryModuleType.WALK_TARGET).isPresent() ? "walk" : "-")
                        .append(villager.getBrain().getOptionalRegisteredMemory(
                                net.minecraft.entity.ai.brain.MemoryModuleType.POTENTIAL_JOB_SITE).isPresent() ? "+jobsite" : "")
                        .append(" task=").append(task.state()).append('/').append(task.executor().map(
                                id -> id.equals(villager.getUuid()) ? "him" : "other").orElse("none"))
                        .append(" prof=").append(VillageColonyMod.WORKERS.find(villager.getUuid())
                                .flatMap(Worker::profession).map(Enum::name).orElse("none"));
            }
        });

        context.runAtTick(40, () -> {
            try {
                context.assertFalse(sheep.isSheared(), "N05: a ovelha foi tosquiada a quatro blocos" + history);
                context.assertFalse(context.getBlockState(gate).get(FenceGateBlock.OPEN),
                        "a porteira abriu antes de o pastor caminhar");
                context.assertTrue(ChestInventoryReader.read(context.getWorld(), context.getAbsolutePos(pastureChest))
                        .amountOfGroup(ResourceGroup.WOOL) == 0, "apareceu lã no baú sem contato");
                villager.setAiDisabled(false);
            } catch (RuntimeException | AssertionError failure) {
                cleanup.run();
                throw failure;
            }
        });

        context.runAtTick(320, () -> {
            checked[0] = true;
            try {
                context.assertTrue(villager.age > 100, "a arena não manteve o pastor ativo" + history);
                context.assertTrue(sheep.isSheared(), "o pastor não chegou junto da ovelha para tosquiar" + history);
                context.assertTrue(ChestInventoryReader.read(context.getWorld(), context.getAbsolutePos(pastureChest))
                        .amountOfGroup(ResourceGroup.WOOL) > 0, "a lã tosquiada não chegou ao baú");
            } finally {
                cleanup.run();
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
