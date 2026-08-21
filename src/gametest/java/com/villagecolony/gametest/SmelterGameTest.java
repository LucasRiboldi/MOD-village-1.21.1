package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ChestInventoryReader;
import com.villagecolony.fabric.work.SmelterWork;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * O fundidor — 2026-08-20.
 *
 * <p>A exceção honesta da Regra 10, resolvida: a vidraça pedia vidro,
 * vidro pedia fundir, e a colônia não fundia. Ficou escrito em 08-18 que
 * "enquanto não houver forno, o vidro é material que o jogador guarda no
 * baú".
 */
public class SmelterGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(2, 2, 2);

    private static final BlockPos STAND = new BlockPos(3, 2, 3);

    /**
     * A areia do baú vira vidro no mesmo baú.
     *
     * <p>Duas metades, como em toda transformação deste mod: a areia sai
     * e o vidro entra. Uma que saísse sem a outra entrar seria a colônia
     * destruindo material do jogador.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "smelter",
            tickLimit = 200)
    public void theSandInTheChestBecomesGlass(TestContext context) {
        ServerWorld world = context.getWorld();

        context.setBlockState(new BlockPos(3, 1, 3), Blocks.DIRT.getDefaultState());
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        ChestDepositor.deposit(world, chest, Items.SAND, 4);

        Colony colony = Colony.create(UUID.randomUUID(), chest);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, STAND);
        villager.setBreedingAge(0);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.SMELTER);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));

        owned.owning(villager.getUuid());

        Task task = VillageColonyMod.TASKS.create(
                colony.id(),
                TaskType.SMELT_MATERIAL,
                TaskPriority.PRODUCTION,
                ResourceType.GLASS,
                4);

        task.reserveFor(villager.getUuid());

        SmelterWork.run(world, colony);

        context.runAtTick(150, () -> {
            int glass = ChestInventoryReader
                    .read(world, context.getAbsolutePos(CHEST))
                    .amountOf(ResourceType.GLASS);

            int sand = ChestInventoryReader
                    .read(world, context.getAbsolutePos(CHEST))
                    .amountOf(ResourceType.SAND);

            context.assertTrue(glass > 0, "a areia não virou vidro nenhum");

            context.assertTrue(
                    sand < 4,
                    "o vidro apareceu e a areia continua inteira — matéria do nada");

            owned.cleanUp();

            SmelterWork.forget(villager.getUuid());

            context.complete();
        });
    }

    /**
     * O ferro cru vira lingote na mesma fornalha — 2026-08-21.
     *
     * <p>O fundidor só conhecia areia. Uma tarefa de fundir ferro o faria
     * tirar a <b>areia</b> do baú, porque o cru estava escrito no código
     * e não vinha da tarefa — e a areia da vidraça queimaria para não dar
     * lingote nenhum.
     *
     * <p>Agora o cru sai do que a tarefa pede, e é a única tabela desse
     * tipo no mod: duas linhas, as duas que a colônia consome. O que a
     * fornalha <b>devolve</b> continua sendo pergunta ao livro do jogo.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "smelter_iron",
            tickLimit = 200)
    public void theRawIronInTheChestBecomesAnIngot(TestContext context) {
        ServerWorld world = context.getWorld();

        context.setBlockState(new BlockPos(3, 1, 3), Blocks.DIRT.getDefaultState());
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        ChestDepositor.deposit(world, chest, Items.RAW_IRON, 4);

        // A areia entra junto, e é o ponto: se o fundidor voltar a olhar
        // só para o grupo da areia, ele a queima e o ferro fica intacto.
        ChestDepositor.deposit(world, chest, Items.SAND, 4);

        Colony colony = Colony.create(UUID.randomUUID(), chest);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, STAND);
        villager.setBreedingAge(0);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.SMELTER);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));

        owned.owning(villager.getUuid());

        Task task = VillageColonyMod.TASKS.create(
                colony.id(),
                TaskType.SMELT_MATERIAL,
                TaskPriority.PRODUCTION,
                ResourceType.IRON_INGOT,
                4);

        task.reserveFor(villager.getUuid());

        SmelterWork.run(world, colony);

        context.runAtTick(150, () -> {
            var inChest = ChestInventoryReader.read(world, context.getAbsolutePos(CHEST));

            context.assertTrue(
                    inChest.amountOf(ResourceType.IRON_INGOT) > 0,
                    "o ferro cru não virou lingote nenhum");

            context.assertTrue(
                    inChest.amountOf(ResourceType.RAW_IRON) < 4,
                    "o lingote apareceu e o cru continua inteiro — matéria do nada");

            context.assertTrue(
                    inChest.amountOf(ResourceType.SAND) == 4,
                    "o fundidor queimou a areia numa tarefa de ferro");

            owned.cleanUp();

            SmelterWork.forget(villager.getUuid());

            context.complete();
        });
    }
}
