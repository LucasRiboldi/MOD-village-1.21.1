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
import com.villagecolony.fabric.work.LumberjackWork;
import com.villagecolony.fabric.work.TreeChoice;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.Schedule;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * Procurar árvore não é estar travado — sessão de jogo de 2026-09-26.
 *
 * <p>Na vila de planície sem árvore natural ao alcance, o lenhador passou a
 * sessão em "looking for a tree" e o guarda de imobilidade devolvia a tarefa a
 * cada 300 tiques ("has not moved a block in 300 work ticks while looking for a
 * tree"), até ele largar o ofício. Procurar é ficar parado; quem espera a muda
 * crescer não está preso. Os guardas passam a contar só com árvore escolhida.
 */
public class LumberjackSearchGameTest implements FabricGameTest {

    // Lote só dele: o "lumber_search" antigo planta uma árvore na arena ao lado,
    // e o raio de busca (64) a achava — com árvore escolhida o guarda conta, e
    // é certo que conte.
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_search_no_tree",
            tickLimit = 500)
    public void aLumberjackWithNoTreeInReachKeepsItsTask(TestContext context) {
        BlockPos stand = new BlockPos(3, 2, 3);
        BlockPos chest = new BlockPos(1, 2, 1);

        // Chão e um poço de pedra em volta dele: ele não sai do bloco, que é o
        // que o guarda de imobilidade mede.
        for (int x = 0; x <= 6; x++) {
            for (int z = 0; z <= 6; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.STONE.getDefaultState());
            }
        }

        for (int y = 2; y <= 3; y++) {
            for (BlockPos wall : new BlockPos[] {stand.east(), stand.west(), stand.north(), stand.south()}) {
                context.setBlockState(wall.withY(y), Blocks.STONE.getDefaultState());
            }
        }

        context.setBlockState(chest, Blocks.CHEST.getDefaultState());
        context.getWorld().setTimeOfDay(Schedule.WORK_TIME);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, stand);
        villager.setBreedingAge(0);

        // Fora de COLONIES, como o teste do guarda de travamento (E20): o ciclo
        // da colônia não mexe nesta tarefa.
        Colony colony = Colony.create(
                UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(stand)));

        ColonyFixture owned = ColonyFixture.create().owning(colony).owning(villager.getUuid());

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.LUMBERJACK);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(
                villager.getUuid(), MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(chest))));

        Task task = VillageColonyMod.TASKS.create(
                colony.id(), TaskType.COLLECT_WOOD, TaskPriority.PRODUCTION, ResourceType.OAK_LOG, 64);

        task.reserveFor(villager.getUuid());

        TreeChoice.shortenStallLimitTo(60);
        LumberjackWork.shortenSearchRadiusTo(6);

        LumberjackWork.run(context.getWorld(), colony);

        context.runAtTick(420, () -> {
            TreeChoice.restoreStallLimit();
            LumberjackWork.restoreSearchRadius();

            try {
                context.assertTrue(task.executor().isPresent()
                                && task.executor().get().equals(villager.getUuid()),
                        "o lenhador perdeu a tarefa procurando árvore — estado " + task.state()
                                + "; os guardas contaram a procura como travamento");
            } finally {
                owned.cleanUp();
            }

            context.complete();
        });
    }
}
