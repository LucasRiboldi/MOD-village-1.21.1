package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.work.BuilderWork;
import com.villagecolony.fabric.work.WaitingWork;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.Schedule;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/**
 * O trabalhador que a colônia perde — item A do §8.
 *
 * <p>{@code VillagerLifecycleHandler} é o único lugar do mod que reage a
 * um aldeão deixando de existir, e até aqui nada o exercitava: os
 * registros são estáticos, o evento vem do Fabric, e um teste unitário
 * não alcança nem um nem outro.
 *
 * <p>O que se perde quando ninguém repara é sempre a mesma coisa: a vaga
 * de profissão continua ocupada por um morto, o baú dele fica reservado
 * para sempre, e a tarefa que ele tinha nunca volta para a fila. Nenhuma
 * das três aparece como erro — a colônia só vai ficando menos capaz.
 *
 * <p>Os dois caminhos são testados porque são eventos diferentes do
 * Fabric: morrer dispara {@code AFTER_DEATH}, e ser mordido por um zumbi
 * não — vira {@code MOB_CONVERSION}, que é o caso mais comum em jogo.
 */
public class WorkerLossGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "worker_death")
    public void aDeadWorkerFreesProfessionStorageAndTask(TestContext context) {
        Fixture fixture = Fixture.build(context);

        ServerWorld world = context.getWorld();

        fixture.villager.damage(world.getDamageSources().genericKill(), Float.MAX_VALUE);

        try {
            context.assertTrue(
                    fixture.villager.isDead(),
                    "o aldeão não morreu, e o resto do teste não valeria nada");

            assertForgotten(context, fixture, "morrer");
        } finally {
            cleanUp(fixture);
        }

        context.complete();
    }

    /**
     * Zumbificação não passa por morte.
     *
     * <p>É a diferença que o handler existe para cobrir: o aldeão mordido
     * é convertido, não morto, então {@code AFTER_DEATH} nunca dispara. Um
     * teste que só matasse o aldeão deixaria passar o caminho que o jogo
     * percorre quase toda noite.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "worker_conversion")
    public void aZombifiedWorkerFreesTheSameThings(TestContext context) {
        Fixture fixture = Fixture.build(context);

        fixture.villager.convertTo(EntityType.ZOMBIE_VILLAGER, true);

        try {
            assertForgotten(context, fixture, "ser zumbificado");
        } finally {
            cleanUp(fixture);
        }

        context.complete();
    }

    /**
     * A obra que fecha depois de o construtor morrer — o crash de
     * 2026-08-25.
     *
     * <p>Numa sessão de jogo um zumbi matou o construtor às 21:59:18. A
     * tarefa dele voltou para a fila, como manda o item A do §8 — e o
     * <b>trabalho</b> dele ficou no registro, porque esta classe avisava
     * cinco dos seis trabalhos e o construtor era o que faltava. Sete
     * minutos depois a obra desistiu, o trabalho órfão viu o projeto
     * fechado e mandou devolver à fila uma tarefa que já estava nela.
     * {@code Task.release} recusa, e o servidor caiu.
     *
     * <p>São duas correções, e este teste passa com qualquer uma das
     * duas — é de propósito: o que ele afirma é que a sequência não
     * derruba nada. Qual das duas está de pé é o
     * {@code TaskTest.onlyAReservedOrExecutingTaskIsHeld} que diz.
     *
     * <p>Rodado contra as duas desligadas: {@code BuilderWork.tick}
     * levanta {@code IllegalStateException: Cannot release a task that
     * is AVAILABLE}, que é a linha do log daquela noite.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "worker_death_builder")
    public void theProjectGivenUpAfterItsBuilderDiedDoesNotCrashTheServer(TestContext context) {
        ServerWorld world = context.getWorld();

        BlockPos chest = new BlockPos(2, 2, 2);
        BlockPos stand = new BlockPos(1, 2, 1);
        BlockPos site = new BlockPos(4, 2, 2);

        context.setBlockState(chest, Blocks.CHEST.getDefaultState());
        world.setTimeOfDay(Schedule.WORK_TIME);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, stand);
        villager.setBreedingAge(0);

        ColonyPos chestPos = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(chest));

        Colony colony = Colony.create(UUID.randomUUID(), chestPos);

        VillageColonyMod.COLONIES.register(colony);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.BUILDER);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chestPos));

        ConstructionProject project = ConstructionProject.plan(
                colony.id(),
                Blueprint.of(
                        ResourceId.vanilla("village/plains/houses/test_wall"),
                        List.of(new BlueprintBlock(
                                new ColonyPos(0, 0, 0),
                                MinecraftTypeAdapter.toResourceId(Blocks.OAK_PLANKS)))),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(site)));

        VillageColonyMod.CONSTRUCTIONS.register(project);

        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);

        Task task = VillageColonyMod.TASKS.create(
                colony.id(), TaskType.BUILD, TaskPriority.PRODUCTION, ResourceType.OAK_PLANKS, 1);

        task.reserveFor(villager.getUuid());

        // O trabalho nasce aqui, e é ele que sobrevive ao dono.
        BuilderWork.run(world, colony);

        villager.damage(world.getDamageSources().genericKill(), Float.MAX_VALUE);

        try {
            context.assertTrue(
                    villager.isDead(),
                    "o construtor não morreu, e o resto do teste não valeria nada");

            // A obra desistindo, pelo mesmo caminho que o PatienceClock usa.
            WaitingWork.giveUp(colony, project);

            // A linha que derrubava o servidor.
            BuilderWork.tick(world);

            context.assertTrue(
                    task.state() == TaskState.AVAILABLE,
                    "a tarefa do construtor morto ficou em " + task.state()
                            + " — depois da obra fechar ela tem de continuar na fila");
        } finally {
            ColonyFixture.create()
                    .owning(colony)
                    .owning(villager.getUuid())
                    .cleanUp();
        }

        context.complete();
    }

    /** As três coisas que o trabalhador leva embora se ninguém reparar. */
    private static void assertForgotten(TestContext context, Fixture fixture, String what) {
        UUID villagerId = fixture.villager.getUuid();

        context.assertTrue(
                VillageColonyMod.WORKERS.find(villagerId).isEmpty(),
                "depois de " + what + " o trabalhador continua no registro,"
                        + " e a vaga de profissão dele continua ocupada");

        context.assertTrue(
                VillageColonyMod.STORAGES.of(villagerId).isEmpty(),
                "depois de " + what + " o baú continua reservado para ele");

        context.assertTrue(
                fixture.task.state() == TaskState.AVAILABLE,
                "depois de " + what + " a tarefa dele ficou em " + fixture.task.state()
                        + " — ninguém mais pode pegá-la");
    }

    /**
     * Uma colônia com um lenhador que tem baú e tarefa.
     *
     * <p>Os três de uma vez porque é assim que o defeito aparece: o
     * trabalhador some e leva consigo uma vaga, um baú e uma tarefa.
     */
    private record Fixture(VillagerEntity villager, Colony colony, Task task) {

        static Fixture build(TestContext context) {
            BlockPos chest = new BlockPos(2, 2, 2);
            BlockPos stand = new BlockPos(1, 2, 1);

            context.setBlockState(chest, Blocks.CHEST.getDefaultState());

            VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, stand);
            villager.setBreedingAge(0);

            Colony colony = Colony.create(
                    UUID.randomUUID(),
                    MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(stand)));

            VillageColonyMod.COLONIES.register(colony);

            Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
            worker.assign(ProfessionType.LUMBERJACK);

            ColonyPos chestPos =
                    MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(chest));

            VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chestPos));

            Task task = VillageColonyMod.TASKS.create(
                    colony.id(),
                    TaskType.COLLECT_WOOD,
                    TaskPriority.PRODUCTION,
                    ResourceType.OAK_LOG,
                    32);

            task.reserveFor(villager.getUuid());

            return new Fixture(villager, colony, task);
        }
    }

    /** Tira do registro o que este teste criou, e nada mais. */
    private static void cleanUp(Fixture fixture) {
        ColonyFixture.create()
                .owning(fixture.colony())
                .owning(fixture.villager().getUuid())
                .cleanUp();
    }
}
