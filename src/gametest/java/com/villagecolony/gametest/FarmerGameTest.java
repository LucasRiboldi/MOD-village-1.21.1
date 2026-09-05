package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestInventoryReader;
import com.villagecolony.fabric.integration.CropPatch;
import com.villagecolony.fabric.work.FarmerWork;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.IntProperty;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.UUID;

/**
 * O fazendeiro colhe, replanta e guarda — 2026-08-27.
 *
 * <p><b>Das sete profissões, era a única sem trabalho.</b> A colônia lhe
 * dava enxada, baú e placa com o nome e nunca mais falava com ele: não
 * havia recurso de lavoura, nem produção, nem tarefa, nem meta. Este
 * arquivo afirma a ponta do mundo dessa corrente — o que
 * {@code FarmerChainTest} afirma do lado do núcleo.
 */
public class FarmerGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(1, 2, 1);

    private static final BlockPos FIELD = new BlockPos(4, 2, 4);

    private static final BlockPos STAND = new BlockPos(4, 2, 3);

    /** Trigo maduro sobre terra arada. */
    private static void ripeWheat(TestContext context, BlockPos at) {
        context.setBlockState(at.down(), Blocks.FARMLAND.getDefaultState());

        context.setBlockState(
                at,
                Blocks.WHEAT.getDefaultState()
                        .with((IntProperty) Blocks.WHEAT.getStateManager().getProperty("age"),
                                ((CropBlock) Blocks.WHEAT).getMaxAge()));
    }

    /** Uma colônia com um fazendeiro contratado, com baú e com tarefa. */
    private static VillagerEntity farmer(TestContext context) {
        Colony colony = Colony.create(
                UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST)));

        VillageColonyMod.COLONIES.register(colony);

        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, STAND);
        villager.setBreedingAge(0);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.FARMER);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(
                villager.getUuid(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST))));

        Task task = VillageColonyMod.TASKS.create(
                colony.id(), TaskType.COLLECT_FOOD, TaskPriority.PRODUCTION,
                ResourceType.WHEAT, 8);

        task.reserveFor(villager.getUuid());

        FarmerWork.shortenSearchTo(8);
        FarmerWork.run(context.getWorld(), colony);

        return villager;
    }

    /**
     * O trigo maduro vira trigo no baú do fazendeiro.
     *
     * <p>A afirmação inteira da profissão: buscar recurso e guardar no
     * seu baú, que é o que o autor pediu para todas elas.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "farmer_harvest",
            tickLimit = 200)
    public void ripeWheatEndsUpInTheFarmersChest(TestContext context) {
        ripeWheat(context, FIELD);

        farmer(context);

        context.runAtTick(120, () -> {
            int wheat = ChestInventoryReader
                    .read(context.getWorld(), context.getAbsolutePos(CHEST))
                    .amountOf(ResourceType.WHEAT);

            context.assertTrue(wheat > 0, "o baú do fazendeiro ficou sem trigo");

            FarmerWork.clearAll();

            context.complete();
        });
    }

    /**
     * O campo continua plantado — a Regra 7, onde ela nasceu.
     *
     * <p>Colher sem replantar deixaria a vila com um campo de terra
     * arada vazia e uma refeição só. A muda sai da própria colheita, e
     * por isso não custa estoque.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "farmer_harvest",
            tickLimit = 200)
    public void theFieldIsPlantedAgain(TestContext context) {
        ripeWheat(context, FIELD);

        farmer(context);

        context.runAtTick(120, () -> {
            context.assertTrue(
                    context.getBlockState(FIELD).getBlock() instanceof CropBlock,
                    "o campo ficou vazio depois da colheita: "
                            + context.getBlockState(FIELD).getBlock());

            context.assertFalse(
                    CropPatch.isRipe(context.getBlockState(FIELD)),
                    "a muda replantada já nasceu madura");

            FarmerWork.clearAll();

            context.complete();
        });
    }

    /**
     * Lavoura verde não é colhida.
     *
     * <p>Colher verde é trocar uma comida por nenhuma: o jogo devolve a
     * semente e mais nada. Quem sabe se está madura é o próprio bloco.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "farmer_harvest",
            tickLimit = 120)
    public void greenCropIsLeftAlone(TestContext context) {
        ServerWorld world = context.getWorld();

        context.setBlockState(FIELD.down(), Blocks.FARMLAND.getDefaultState());
        context.setBlockState(FIELD, Blocks.WHEAT.getDefaultState());

        context.assertFalse(
                CropPatch.isRipe(world.getBlockState(context.getAbsolutePos(FIELD))),
                "trigo recém-plantado foi dado como maduro");

        context.assertTrue(
                CropPatch.ripeNear(world, context.getAbsolutePos(CHEST), 8).isEmpty(),
                "a busca achou lavoura madura onde só há verde");

        context.complete();
    }

    /** A busca acha o que está maduro, e diz onde. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "farmer_harvest",
            tickLimit = 120)
    public void theSearchFindsTheRipeCrop(TestContext context) {
        ripeWheat(context, FIELD);

        Optional<BlockPos> found = CropPatch.ripeNear(
                context.getWorld(), context.getAbsolutePos(CHEST), 8);

        context.assertTrue(found.isPresent(), "a busca não achou o trigo maduro");

        context.assertTrue(
                found.get().equals(context.getAbsolutePos(FIELD)),
                "a busca achou outro lugar: " + found.get().toShortString());

        context.complete();
    }

    /**
     * <b>Tarefa devolvida à fila não é mais trabalhada</b> — 2026-09-05,
     * e a falta disto derrubou o servidor do autor na primeira sessão que
     * rodou o jar novo:
     *
     * <pre>
     * java.lang.IllegalStateException: Cannot release a task that is AVAILABLE
     *   at Task.release(Task.java:201)
     *   at FarmerWork.giveUp(FarmerWork.java:360)
     *   at FarmerWork.step(FarmerWork.java:211)
     * </pre>
     *
     * <p>O laço: o guarda de imobilidade dispara aos 300 tiques, a tarefa
     * volta para a fila, e o contador <b>não</b> zera — é o E36, e está
     * certo. Na passagem seguinte o fazendeiro pega outra lavoura,
     * continua sem andar, e o guarda dispara de novo sobre uma tarefa que
     * já é de ninguém. O {@code dropClosedJobs} limparia o trabalho, mas
     * roda uma vez por ciclo da colônia — 600 tiques — e cabem dois
     * disparos na janela.
     *
     * <p>Aqui a devolução é feita à mão, que é o mesmo estado sem esperar
     * os 300 tiques. O que se afirma é que o fazendeiro <b>para</b>: o
     * trigo fica no pé e o baú vazio, porque aquela tarefa deixou de ser
     * dele.
     *
     * <p>Rodado contra a correção desligada: ele colhe assim mesmo — e é
     * o mesmo trabalho fantasma que a exceção acima interrompe à força.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "farmer_released",
            tickLimit = 200)
    public void aTaskBackInTheQueueIsNoLongerWorked(TestContext context) {
        ripeWheat(context, FIELD);

        VillagerEntity villager = farmer(context);

        for (Task task : VillageColonyMod.TASKS.assignedTo(villager.getUuid())) {
            task.release();
        }

        context.runAtTick(120, () -> {
            int wheat = ChestInventoryReader
                    .read(context.getWorld(), context.getAbsolutePos(CHEST))
                    .amountOf(ResourceType.WHEAT);

            context.assertTrue(
                    wheat == 0,
                    "o fazendeiro colheu com a tarefa já de volta na fila — "
                            + wheat + " no baú");

            FarmerWork.clearAll();

            context.complete();
        });
    }
}
