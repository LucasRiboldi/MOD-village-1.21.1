package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

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

        context.assertTrue(
                fixture.villager.isDead(),
                "o aldeão não morreu, e o resto do teste não valeria nada");

        assertForgotten(context, fixture, "morrer");

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

        assertForgotten(context, fixture, "ser zumbificado");

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
            clearColonyState();

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

    /**
     * Zera o estado do mod entre testes.
     *
     * <p>Os registros são estáticos e vivem no servidor, que é um só para
     * a bateria inteira.
     */
    private static void clearColonyState() {
        VillageColonyMod.COLONIES.clear();
        VillageColonyMod.WORKERS.clear();
        VillageColonyMod.STORAGES.clear();
        VillageColonyMod.TASKS.clear();
    }
}
