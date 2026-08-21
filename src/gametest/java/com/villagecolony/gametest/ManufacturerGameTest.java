package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.event.VillageDetectionHandler;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ChestInventoryReader;
import com.villagecolony.fabric.integration.ColonySupply;
import com.villagecolony.fabric.work.ManufacturerWork;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.Schedule;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * O fabricante trabalhando — Fase 9.
 *
 * <p>A primeira profissão que consome. Estes testes olham tanto o que ela
 * produz quanto o que ela **não** faz com o que estava no baú.
 */
public class ManufacturerGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(2, 2, 2);
    private static final BlockPos STAND = new BlockPos(3, 2, 2);

    /**
     * O baú de outro trabalhador da mesma colônia.
     *
     * <p>A dois blocos do baú do fabricante de propósito: encostados, o
     * Minecraft os juntaria num baú duplo, e a prova de que o material veio
     * de outro lugar morreria junto.
     */
    private static final BlockPos OTHER_CHEST = new BlockPos(4, 2, 2);

    /** Colônia, fabricante, baú e tarefa reservada. */
    private static Fixture setUp(TestContext context, int logs) {
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        context.getWorld().setTimeOfDay(Schedule.WORK_TIME);

        ServerWorld world = context.getWorld();
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        if (logs > 0) {
            ChestDepositor.deposit(world, chest, Items.OAK_LOG, logs);
        }

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, STAND);
        villager.setBreedingAge(0);

        Colony colony = Colony.create(
                UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST)));

        VillageColonyMod.COLONIES.register(colony);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.MANUFACTURER);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));

        Task task = VillageColonyMod.TASKS.create(
                colony.id(),
                TaskType.CRAFT_MATERIAL,
                TaskPriority.PRODUCTION,
                ResourceType.OAK_PLANKS,
                16);

        task.reserveFor(villager.getUuid());

        ManufacturerWork.run(world, colony);

        return new Fixture(
                colony,
                task,
                chest,
                ColonyFixture.create().owning(colony).owning(villager.getUuid()));
    }

    private record Fixture(Colony colony, Task task, ColonyPos chest, ColonyFixture owned) {
    }

    private static int planksIn(TestContext context, ColonyPos chest) {
        return ChestInventoryReader
                .read(context.getWorld(), MinecraftTypeAdapter.toBlockPos(chest))
                .amountOfGroup(ResourceGroup.PLANKS);
    }

    private static int logsIn(TestContext context, ColonyPos chest) {
        return ChestInventoryReader
                .read(context.getWorld(), MinecraftTypeAdapter.toBlockPos(chest))
                .amountOfGroup(ResourceGroup.WOOD);
    }

    /** Tronco vira tábua, e a tábua volta para o mesmo baú. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "craft_planks",
            tickLimit = 300)
    public void logsBecomePlanksInTheChest(TestContext context) {
        Fixture fixture = setUp(context, 4);

        context.runAtTick(90, () -> {
            int planks = planksIn(context, fixture.chest);
            int logs = logsIn(context, fixture.chest);

            context.assertTrue(planks > 0, "o baú não recebeu tábua nenhuma");

            context.assertTrue(
                    logs < 4,
                    "nenhum tronco foi consumido: ainda são " + logs);

            fixture.owned.cleanUp();

            context.complete();
        });
    }

    /**
     * Nada sai do baú antes de a peça ficar pronta.
     *
     * <p>É a regra que o lenhador não tem e esta profissão precisa: o
     * tronco é retirado, transformado e devolvido no mesmo tick. Durante
     * a espera existe um contador, e não um tronco na mão de um aldeão
     * que pode morrer — ou de um servidor que vai ser desligado. Ver o E3
     * do §17 para o que acontece quando algo sai do mundo antes de ter
     * para onde ir.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "craft_atomic",
            tickLimit = 300)
    public void nothingLeavesTheChestBeforeThePieceIsDone(TestContext context) {
        Fixture fixture = setUp(context, 4);

        context.runAtTick(8, () -> {
            context.assertTrue(
                    logsIn(context, fixture.chest) == 4,
                    "o tronco saiu do baú antes de virar tábua");

            context.assertTrue(
                    planksIn(context, fixture.chest) == 0,
                    "apareceu tábua antes do tempo da peça");
        });

        context.runAtTick(90, () -> {
            context.assertTrue(
                    planksIn(context, fixture.chest) > 0,
                    "e no fim nenhuma tábua apareceu — o teste não provou nada");

            fixture.owned.cleanUp();

            context.complete();
        });
    }

    /**
     * O tronco está no baú do lenhador, e a tábua sai assim mesmo.
     *
     * <p>É o mundo que a sessão de 2026-08-14 mostrou, e que nenhum teste
     * desta classe modelava: quem colhe deposita no <b>próprio</b> baú, e o
     * fabricante tem um baú só dele — vazio. Os outros testes desta classe
     * põem o tronco no baú do fabricante, que é um estado que o jogo nunca
     * produz sozinho.
     *
     * <p>Com o tronco do lado de lá, o fabricante encerrava a tarefa a cada
     * ciclo dizendo "no logs left in the chest" — dezessete vezes em
     * dezesseis minutos, zero tábuas, com 134 troncos guardados na colônia.
     * A meta da Regra 5 se mede na colônia inteira; o executor media um baú
     * só, e os dois discordavam sobre onde estava o estoque.
     *
     * <p>A tábua volta para o baú de onde o tronco saiu, que é o que
     * preserva a regra do mesmo baú no mesmo tick.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "craft_colony_wide",
            tickLimit = 300)
    public void logsInAnotherWorkersChestStillBecomePlanks(TestContext context) {
        Fixture fixture = setUp(context, 0);

        context.setBlockState(OTHER_CHEST, Blocks.CHEST.getDefaultState());

        ColonyPos lumberjackChest =
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(OTHER_CHEST));

        ChestDepositor.deposit(context.getWorld(), lumberjackChest, Items.OAK_LOG, 4);

        UUID lumberjack = UUID.randomUUID();

        VillageColonyMod.WORKERS.register(lumberjack, fixture.colony.id())
                .assign(ProfessionType.LUMBERJACK);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(lumberjack, lumberjackChest));

        fixture.owned.owning(lumberjack);

        context.runAtTick(90, () -> {
            int planks = planksIn(context, lumberjackChest);
            int logs = logsIn(context, lumberjackChest);

            context.assertTrue(
                    planks > 0,
                    "o fabricante não fez tábua nenhuma com o tronco do lenhador");

            context.assertTrue(
                    logs < 4,
                    "nenhum tronco foi consumido do baú do lenhador: ainda são " + logs);

            fixture.owned.cleanUp();

            context.complete();
        });
    }

    /** Sem tronco no baú, a tarefa se encerra em vez de girar. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "craft_no_logs",
            tickLimit = 300)
    public void withoutLogsTheTaskEnds(TestContext context) {
        Fixture fixture = setUp(context, 0);

        context.runAtTick(60, () -> {
            context.assertTrue(
                    fixture.task.state() == TaskState.COMPLETED,
                    "a tarefa devia ter se encerrado, está em " + fixture.task.state());

            fixture.owned.cleanUp();

            context.complete();
        });
    }

    /**
     * O laço inteiro da Fase 9, sem tarefa posta à mão.
     *
     * <p>Os outros testes desta classe entregam a tarefa pronta ao
     * fabricante. Este não: põe tronco no baú e roda o ciclo da colônia,
     * que é quem decide se há o que fabricar. É a torneira da Regra 5 —
     * a meta é metade do que os baús comportam em tábua — e é o caminho
     * que o jogo percorre.
     *
     * <p>A ordem importa e foi aprendida na Fase 8: a torneira só foi
     * aberta depois de existir quem executasse. Tarefa sem executor fica
     * reservada para sempre.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "craft_from_cycle",
            tickLimit = 300)
    public void theCycleOpensTheCraftingTaskByItself(TestContext context) {
        BlockPos stand = new BlockPos(3, 2, 2);

        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        context.getWorld().setTimeOfDay(Schedule.WORK_TIME);

        ServerWorld world = context.getWorld();
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        ChestDepositor.deposit(world, chest, Items.OAK_LOG, 8);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, stand);
        villager.setBreedingAge(0);

        Colony colony = Colony.create(
                UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST)));

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create()
                .owning(colony)
                .owning(villager.getUuid());

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.MANUFACTURER);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));

        VillageDetectionHandler.runCycleNow(world, context.getAbsolutePos(stand));

        int crafting = 0;

        for (Task task : VillageColonyMod.TASKS.ofColony(colony.id())) {
            if (task.type() == TaskType.CRAFT_MATERIAL) {
                crafting++;
            }
        }

        context.assertTrue(
                crafting == 1,
                "o ciclo devia ter aberto uma tarefa de fabricação, abriu " + crafting);

        context.runAtTick(90, () -> {
            context.assertTrue(
                    planksIn(context, chest) > 0,
                    "a tarefa que o ciclo abriu não virou tábua nenhuma");

            owned.cleanUp();

            context.complete();
        });
    }

    /**
     * A tocha sai de carvão e tábua — 2026-08-21.
     *
     * <p><b>O degrau que faltava.</b> Até aqui a colônia só montava o que
     * pudesse montar com <b>todos</b> os ingredientes já no baú. A tocha
     * pede carvão e graveto: o carvão a mina passou a dar, e o graveto
     * cai das folhas por sorteio — quando não caía, a colônia ficava com
     * carvão, tábua e nenhuma tocha, e as três da casa de planície
     * continuavam por conta do jogador.
     *
     * <p>Aqui o baú tem carvão e tábua, e <b>nenhum graveto</b>. Se a
     * tocha aparecer, foi porque a colônia fez o graveto primeiro.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "craft_depth",
            tickLimit = 100)
    public void theColonyMakesTheStickTheTorchNeeds(TestContext context) {
        ServerWorld world = context.getWorld();

        context.setBlockState(new BlockPos(2, 1, 2), Blocks.DIRT.getDefaultState());
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        ChestDepositor.deposit(world, chest, Items.COAL, 4);
        ChestDepositor.deposit(world, chest, Items.OAK_PLANKS, 8);

        Colony colony = Colony.create(UUID.randomUUID(), chest);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, STAND);
        villager.setBreedingAge(0);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.MANUFACTURER);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));

        owned.owning(villager.getUuid());

        context.assertTrue(
                ChestInventoryReader.read(world, context.getAbsolutePos(CHEST))
                        .amountOf(ResourceType.OAK_PLANKS) == 8,
                "o cenário do teste não montou: a tábua não entrou no baú");

        // A pergunta antes da retirada precisa concordar com ela. Uma que
        // dissesse "não" aqui poria a obra a esperar por peça que a
        // colônia sabe montar.
        context.assertTrue(
                ColonySupply.canProvide(world, colony.id(), chest, Items.TORCH),
                "a colônia disse que não sabe fazer tocha, com carvão e tábua no baú");

        context.assertTrue(
                ColonySupply.take(world, colony.id(), chest, Items.TORCH),
                "a tocha não saiu de carvão e tábua — faltou o degrau do graveto");

        int planksLeft = ChestInventoryReader.read(world, context.getAbsolutePos(CHEST))
                .amountOf(ResourceType.OAK_PLANKS);

        context.assertTrue(
                planksLeft < 8,
                "a tocha apareceu e a tábua continua inteira — matéria do nada");

        owned.cleanUp();

        context.complete();
    }

    /**
     * Sem tábua não há graveto, e sem graveto não há tocha.
     *
     * <p>A outra ponta: o degrau novo não pode virar uma colônia que diz
     * saber fazer tudo. Com carvão e mais nada, a resposta é não — e ela
     * precisa ser a mesma dos dois lados, senão a obra acorda e volta a
     * dormir todo ciclo.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "craft_depth",
            tickLimit = 100)
    public void coalAloneIsNotATorch(TestContext context) {
        ServerWorld world = context.getWorld();

        context.setBlockState(new BlockPos(2, 1, 2), Blocks.DIRT.getDefaultState());
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        ChestDepositor.deposit(world, chest, Items.COAL, 4);

        Colony colony = Colony.create(UUID.randomUUID(), chest);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, STAND);
        villager.setBreedingAge(0);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.MANUFACTURER);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));

        owned.owning(villager.getUuid());

        context.assertTrue(
                !ColonySupply.canProvide(world, colony.id(), chest, Items.TORCH),
                "a colônia disse saber fazer tocha só com carvão");

        context.assertTrue(
                !ColonySupply.take(world, colony.id(), chest, Items.TORCH),
                "saiu uma tocha de carvão e nada mais");

        owned.cleanUp();

        context.complete();
    }
}
