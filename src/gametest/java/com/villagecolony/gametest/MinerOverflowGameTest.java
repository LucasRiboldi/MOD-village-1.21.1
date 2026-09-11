package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.type.Side;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestInventoryReader;
import com.villagecolony.fabric.work.HousePlans;
import com.villagecolony.fabric.work.MineDigging;
import com.villagecolony.fabric.work.MinerWork;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * O que o mineiro cava e não cabe — o E3, a metade que faltava.
 *
 * <p><b>O lenhador parou de destruir em 2026-09-04; o mineiro não.</b>
 * Naquele dia o baú de um lenhador encheu e vinte e quatro troncos
 * viraram nada antes do conserto: o bloco já tinha saído do mundo e não
 * havia para onde mandá-lo. A correção deu a ele
 * {@code ColonyChests.ownFirst} — o baú do dono primeiro, o resto da
 * colônia como transbordo —, e o {@code TODO.md} passou a registrar o
 * E3 como <i>metade fechada</i>: <i>"o mineiro continua sem teto de
 * inventário"</i>.
 *
 * <p>Era literal. O {@code MinerHaul} depositava no baú da boca da mina
 * e depois no baú do mineiro, e o que sobrasse virava uma linha de WARN
 * contando a perda. Colônia com seis baús vazios a vinte blocos não
 * mudava nada: a pergunta nunca chegava a eles.
 *
 * <p>Este teste é a prova da segunda metade, e é de integração de
 * propósito — o defeito só existe com baú cheio de verdade, bloco saindo
 * do mundo de verdade e um segundo baú registrado na colônia. Um
 * unitário sobre o {@code MinerHaul} mediria a chamada, e a chamada
 * nunca foi a dúvida.
 */
public class MinerOverflowGameTest implements FabricGameTest {

    /** O baú do mineiro, que este teste enche até não caber um item. */
    private static final BlockPos CHEST = new BlockPos(2, 2, 2);

    /** O baú de outro trabalhador da colônia, vazio. É o transbordo. */
    private static final BlockPos SPARE = new BlockPos(2, 2, 5);

    private static final BlockPos STAND = new BlockPos(4, 2, 4);

    private static final BlockPos ROCK = new BlockPos(4, 2, 3);

    /** Onde o dono do baú de transbordo fica, longe da pedra. */
    private static final BlockPos NEIGHBOUR = new BlockPos(6, 2, 6);

    /**
     * Raio curto: a bateria roda arenas vizinhas no mesmo mundo, e um
     * mineiro de raio 48 comeria a pedra do teste do lado.
     */
    private static final int NEARBY = 2;

    private static void ground(TestContext context) {
        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 7; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.DIRT.getDefaultState());
            }
        }
    }

    /**
     * Enche o baú até o último compartimento.
     *
     * <p>Terra, e não pedra: o teste afirma sobre o grupo da pedra no
     * fim, e encher com o próprio material da prova confundiria a
     * afirmação com a montagem.
     */
    private static void fillToTheBrim(TestContext context, BlockPos chest) {
        ChestBlockEntity entity = (ChestBlockEntity) context.getWorld()
                .getBlockEntity(context.getAbsolutePos(chest));

        for (int slot = 0; slot < entity.size(); slot++) {
            entity.setStack(slot, new ItemStack(Items.DIRT, 64));
        }

        entity.markDirty();
    }

    /**
     * O baú do mineiro está cheio, e a pedra vai para outro da colônia.
     *
     * <p>A afirmação central é sobre o <b>baú de transbordo</b>: antes de
     * 2026-09-11 ele terminava vazio, e a pedra não estava em lugar
     * nenhum do mundo.
     *
     * <p>Duas asserções de montagem seguram o cenário, porque sem elas
     * este teste passaria por motivo errado. Que o baú do mineiro
     * continua cheio até o fim — se coubesse pedra nele, o transbordo
     * nunca seria exercitado. E que a pedra saiu do mundo — sem isso um
     * mineiro que não cavou nada e um que cavou e perdeu tudo dariam o
     * mesmo baú vazio.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_overflow",
            tickLimit = 400)
    public void theHaulThatDoesNotFitGoesToAnotherColonyChest(TestContext context) {
        ServerWorld world = context.getWorld();

        ground(context);

        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        context.setBlockState(SPARE, Blocks.CHEST.getDefaultState());

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));
        ColonyPos spare = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(SPARE));

        fillToTheBrim(context, CHEST);

        // A pedra que ESTA vila quer, perguntada à paleta — escrever
        // "stone" à mão amarraria o teste ao bioma da arena.
        Block rock = MinecraftTypeAdapter
                .toBlock(HousePlans.paletteOf(world, chest).stone())
                .orElseThrow();

        context.setBlockState(ROCK, rock.getDefaultState());

        Colony colony = Colony.create(UUID.randomUUID(), chest);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        Worker miner = TestWorkers.createEquippedWorker(
                context, colony.id(), ProfessionType.MINER, STAND);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(miner.villagerId(), chest));

        owned.owning(miner.villagerId());

        // O dono do transbordo. Fundidor, e sem tarefa de fundir: ele
        // existe para que o baú dele conte como baú da colônia, que é a
        // única porta pela qual ColonyChests enxerga um baú.
        Worker neighbour = TestWorkers.createWorker(
                context, colony.id(), ProfessionType.SMELTER, NEIGHBOUR);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(neighbour.villagerId(), spare));

        owned.owning(neighbour.villagerId());

        Task task = VillageColonyMod.TASKS.create(
                colony.id(),
                TaskType.COLLECT_STONE,
                TaskPriority.PRODUCTION,
                ResourceType.COBBLESTONE,
                16);

        task.reserveFor(miner.villagerId());

        // A boca posta à mão: o lado da descida sai do identificador da
        // colônia, que é sorteado, e um teste não pode depender de sorte
        // para saber onde a escada passa. Boca no STAND e descida ao
        // norte põem MineShaft.positionAt(0) exatamente no ROCK.
        ColonyPos mouth = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(STAND));

        VillageColonyMod.MINES.restore(
                Mine.restore(colony.id(), MineShaft.from(mouth, Side.NORTH), 0));

        MineDigging.shortenMineDistanceTo(NEARBY);

        MinerWork.run(world, colony);

        context.runAtTick(320, () -> {
            int inSpare = ChestInventoryReader
                    .read(world, context.getAbsolutePos(SPARE))
                    .amountOfGroup(ResourceGroup.STONE);

            int inOwn = ChestInventoryReader
                    .read(world, context.getAbsolutePos(CHEST))
                    .amountOfGroup(ResourceGroup.STONE);

            boolean rockIsGone = !context.getBlockState(ROCK).isOf(rock);

            try {
                // Montagem: sem pedra fora do mundo não há transbordo a
                // medir, e o baú vazio não distinguiria as duas causas.
                context.assertTrue(
                        rockIsGone,
                        "o mineiro não cavou nada — o teste não chegou a medir transbordo");

                // Montagem: o baú do mineiro tem de continuar sem espaço.
                context.assertTrue(
                        inOwn == 0,
                        "coube pedra no baú do mineiro — o cenário não força o transbordo");

                // A prova.
                context.assertTrue(
                        inSpare > 0,
                        "a pedra que não coube no baú do mineiro se perdeu, "
                                + "com baú vazio registrado na colônia");
            } finally {
                owned.cleanUp();

                MineDigging.restoreMineDistance();
            }

            context.complete();
        });
    }
}
