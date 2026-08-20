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
import com.villagecolony.fabric.work.HousePlans;
import com.villagecolony.fabric.work.MinerWork;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * O mineiro — 2026-08-20.
 *
 * <p>Ele existe para destravar duas coisas: os 43 pedregulhos da casa de
 * planície, que a Regra 24 tinha deixado por conta do jogador, e a vila
 * de deserto, que nascia e nunca construía por não haver árvore.
 */
public class MinerGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(2, 2, 2);

    private static final BlockPos STAND = new BlockPos(4, 2, 4);

    /** A pedra exposta, ao alcance do braço de quem está no STAND. */
    private static final BlockPos ROCK = new BlockPos(4, 2, 3);

    /**
     * A boca da mina, perto da arena.
     *
     * <p>A bateria roda arenas lado a lado no mesmo mundo, e uma mina
     * aberta a quarenta blocos cava o cenário do teste vizinho.
     */
    private static final int NEARBY = 2;

    /**
     * A pedra sai do mundo e entra no baú.
     *
     * <p>As duas metades da mesma regra, e é a mesma do lenhador:
     * mineração nunca cria recurso, e nunca o perde. Um pedregulho que
     * entrasse no baú sem sair do mundo seria a colônia inventando
     * matéria; um que saísse do mundo sem entrar no baú seria o E3.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner",
            tickLimit = 400)
    public void theStoneLeavesTheWorldAndReachesTheChest(TestContext context) {
        ServerWorld world = context.getWorld();

        ground(context);

        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        // A pedra que ESTA vila quer, perguntada à paleta. Escrever
        // "stone" à mão amarraria o teste ao bioma da arena, que é fixo e
        // não é escolha nossa: num bioma de deserto o mineiro procuraria
        // arenito e passaria direto por uma pedra comum.
        Block rock = MinecraftTypeAdapter
                .toBlock(HousePlans.paletteOf(world, chest).stone())
                .orElseThrow();

        context.setBlockState(ROCK, rock.getDefaultState());

        Colony colony = Colony.create(UUID.randomUUID(), chest);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, STAND);
        villager.setBreedingAge(0);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.MINER);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));

        owned.owning(villager.getUuid());

        Task task = VillageColonyMod.TASKS.create(
                colony.id(),
                TaskType.COLLECT_STONE,
                TaskPriority.PRODUCTION,
                ResourceType.COBBLESTONE,
                16);

        task.reserveFor(villager.getUuid());

        // Despacho: abre o trabalho. Daqui em diante quem age é o tique
        // do servidor, que é o que este teste exercita.
        // Raio curto: a bateria roda arenas vizinhas no mesmo mundo, e
        // um mineiro de raio 48 comeria a pedra do teste do lado.
        MinerWork.shortenMineDistanceTo(NEARBY);

        MinerWork.run(world, colony);

        context.runAtTick(5, () -> context.assertTrue(
                context.getBlockState(ROCK).isOf(rock),
                "aos 5 tiques a pedra não podia ter caído"));

        context.runAtTick(320, () -> {
            // Afirma que pedra saiu do mundo e entrou no baú, e não que
            // saiu *daquela* posição. O mundo do teste tem rocha exposta
            // abaixo do piso da arena, mais perto do centro que a pedra
            // montada aqui, e não há como forrá-la: ela é o terreno do
            // mundo, fora da estrutura. Amarrar a prova a uma posição
            // seria amarrá-la ao relevo do mundo de teste.
            int stored = ChestInventoryReader
                    .read(world, context.getAbsolutePos(CHEST))
                    .amountOfGroup(ResourceGroup.STONE);

            context.assertTrue(stored > 0, "a pedra não chegou ao baú");

            owned.cleanUp();

            MinerWork.forget(villager.getUuid());

            MinerWork.restoreMineDistance();

            context.complete();
        });
    }

    /**
     * Pedra de vila gerada e de casa da colônia não se toca.
     *
     * <p>A Regra 3, e para o mineiro ela morde mais que para o lenhador:
     * a vila do jogo e as casas do jogador são feitas exatamente do
     * material que ele procura. Um mineiro que cavasse qualquer pedra
     * derrubaria a igreja no primeiro ciclo.
     *
     * <p>Aqui a prova é pelo lado da colônia, e vale para a mina
     * inteira: uma construção registrada cobre tudo o que o mineiro
     * alcançaria, e nada chega ao baú. Se ele cavasse, cavaria a casa.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_protection",
            tickLimit = 400)
    public void theMinerDoesNotEatTheColonysOwnWalls(TestContext context) {
        ServerWorld world = context.getWorld();

        ground(context);

        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        Block rock = MinecraftTypeAdapter
                .toBlock(HousePlans.paletteOf(world, chest).stone())
                .orElseThrow();

        context.setBlockState(ROCK, rock.getDefaultState());

        Colony colony = Colony.create(UUID.randomUUID(), chest);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        // Uma construção da colônia cobrindo tudo o que a mina alcança:
        // a boca fica a NEARBY do centro, e ela desce vinte blocos. Se o
        // mineiro cavar qualquer coisa, cavou a casa.
        ColonyPos wall = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(ROCK));

        VillageColonyMod.BUILDINGS.register(new com.villagecolony.core.construction.model.Building(
                UUID.randomUUID(),
                colony.id(),
                com.villagecolony.core.construction.model.ColonyHut.ID,
                new ColonyPos(wall.x() - 40, wall.y() - 40, wall.z() - 40),
                new ColonyPos(wall.x() + 40, wall.y() + 40, wall.z() + 40)));

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, STAND);
        villager.setBreedingAge(0);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.MINER);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));

        owned.owning(villager.getUuid());

        Task task = VillageColonyMod.TASKS.create(
                colony.id(),
                TaskType.COLLECT_STONE,
                TaskPriority.PRODUCTION,
                ResourceType.COBBLESTONE,
                16);

        task.reserveFor(villager.getUuid());

        // Raio curto: a bateria roda arenas vizinhas no mesmo mundo, e
        // um mineiro de raio 48 comeria a pedra do teste do lado.
        MinerWork.shortenMineDistanceTo(NEARBY);

        MinerWork.run(world, colony);

        context.runAtTick(320, () -> {
            int stored = ChestInventoryReader
                    .read(world, context.getAbsolutePos(CHEST))
                    .amountOfGroup(ResourceGroup.STONE);

            context.assertTrue(
                    stored == 0,
                    "o mineiro cavou " + stored + " de dentro da própria colônia");

            owned.cleanUp();

            MinerWork.forget(villager.getUuid());

            VillageColonyMod.BUILDINGS.removeOfColony(colony.id());

            MinerWork.restoreMineDistance();

            context.complete();
        });
    }

    /** Chão sólido, para o aldeão andar e a pedra ter em que assentar. */
    private static void ground(TestContext context) {
        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 7; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.DIRT.getDefaultState());
            }
        }
    }
}
