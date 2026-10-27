package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineNetwork;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.construction.service.MineNetworks;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A mina em rede de ponta a ponta — ADR-042: o corredor do nó sai do salão, o veio da
 * parede passa à frente, o corredor aberto ganha piso e vedação com o pedregulho dos baús,
 * e o nó pronto abre os vizinhos.
 *
 * <p>Desde a Regra 119 a mina não entra mais na rede (NetworkDigging.galleryPhase é falso): os testes
 * da fase de rede e da descida pela rede saíram; os de enchimento e de recusa seguem medindo as peças
 * que continuam no código.
 */
public class NetworkDiggingGameTest implements FabricGameTest {

    /**
     * O enchimento da própria mina não sai dos baús: a construção interna da mina surge quando
     * o mineiro precisa, sem consumir recurso da vila (decisão do autor, 2026-10-13). Sem
     * pedregulho em baú nenhum, o túnel aberto fica seguro numa passagem e o nó fica pronto.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "network_fill", tickLimit = 20)
    public void withoutFillInChestsTheMineFillsItselfAndFinishesTheNode(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos chestAt = context.getAbsolutePos(new BlockPos(1, 2, 1));
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(chestAt);
        Colony colony = Colony.create(UUID.randomUUID(), chest);
        ColonyFixture owned = ColonyFixture.create().owning(colony);
        UUID miner = UUID.randomUUID();

        VillageColonyMod.COLONIES.register(colony);
        VillageColonyMod.WORKERS.register(miner, colony.id());
        owned.owning(miner);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(miner, chest));
        // Um baú vazio de propósito: nem o baú do mineiro nem os da vila têm enchimento.
        world.setBlockState(chestAt, Blocks.CHEST.getDefaultState());

        ColonyPos entry = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(3, 42, 3)));
        int shared = MineShaft.SHARED_BLOCKS;
        Mine mine = Mine.restore(colony.id(), MineShaft.from(entry, Side.EAST), new int[] {shared, shared, shared, shared});
        VillageColonyMod.MINES.restore(mine);
        BlockPos hub = MinecraftTypeAdapter.toBlockPos(mine.shaft().hallFloor());
        BlockPos from = hub.add(-10, -1, -10);
        BlockPos to = hub.add(10, 3, 10);

        try {
            for (BlockPos at : BlockPos.iterate(from, to)) {
                world.setBlockState(at, Blocks.STONE.getDefaultState());
            }

            NetworkDigging.nextTarget(world, miner, colony.id(), mine);
            MineNetwork network = MineNetworks.of(colony.id()).orElseThrow();
            MineNetwork.Node node = network.claimedBy(miner).orElseThrow();
            List<ColonyPos> corridor = network.corridorOf(node);

            // O túnel inteiro aberto e um buraco no piso: falta enchimento, e não há nenhum nos baús.
            for (ColonyPos cell : corridor) {
                world.setBlockState(MinecraftTypeAdapter.toBlockPos(cell), Blocks.AIR.getDefaultState());
            }

            BlockPos hole = MinecraftTypeAdapter.toBlockPos(corridor.get(2 * MineNetwork.HEIGHT)).down();
            world.setBlockState(hole, Blocks.AIR.getDefaultState());
            NetworkVeins.forget(miner);

            Optional<BlockPos> next = NetworkDigging.nextTarget(world, miner, colony.id(), mine);

            context.assertTrue(next.isEmpty(),
                    "o enchimento da mina devia surgir; em vez disso pediu um alvo: " + next);
            context.assertTrue(NetworkSafety.isFirmFloor(world, hole),
                    "o buraco do piso não foi fechado sem enchimento nos baús");
            context.assertTrue(network.stateOf(node.gx(), node.gz()).orElseThrow() == MineNetwork.State.DONE,
                    "o nó não ficou pronto com o enchimento surgido pela própria mina");
        } finally {
            for (BlockPos at : BlockPos.iterate(from, to)) {
                world.setBlockState(at, Blocks.AIR.getDefaultState());
            }

            world.setBlockState(chestAt, Blocks.AIR.getDefaultState());
            VillageColonyMod.MINES.removeOfColony(colony.id());
            MineNetworks.forget(colony.id());
            owned.cleanUp();
        }

        context.complete();
    }

    /**
     * A frente do túnel anda: uma célula recusada é pulada, não sela o nó; só com todas as que
     * faltam recusadas é que o nó falha — mina M, 2026-10-13.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "network_skip", tickLimit = 20)
    public void aRefusedCellIsSkippedAndTheNodeFailsOnlyWhenAllAreRefused(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos chestAt = context.getAbsolutePos(new BlockPos(1, 2, 1));
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(chestAt);
        Colony colony = Colony.create(UUID.randomUUID(), chest);
        ColonyFixture owned = ColonyFixture.create().owning(colony);
        UUID miner = UUID.randomUUID();

        VillageColonyMod.COLONIES.register(colony);
        VillageColonyMod.WORKERS.register(miner, colony.id());
        owned.owning(miner);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(miner, chest));
        world.setBlockState(chestAt, Blocks.CHEST.getDefaultState());

        ColonyPos entry = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(3, 42, 3)));
        int shared = MineShaft.SHARED_BLOCKS;
        Mine mine = Mine.restore(colony.id(), MineShaft.from(entry, Side.EAST), new int[] {shared, shared, shared, shared});
        VillageColonyMod.MINES.restore(mine);
        BlockPos hub = MinecraftTypeAdapter.toBlockPos(mine.shaft().hallFloor());
        BlockPos from = hub.add(-10, -1, -10);
        BlockPos to = hub.add(10, 3, 10);

        try {
            for (BlockPos at : BlockPos.iterate(from, to)) {
                world.setBlockState(at, Blocks.STONE.getDefaultState());
            }

            NetworkDigging.nextTarget(world, miner, colony.id(), mine);
            MineNetwork network = MineNetworks.of(colony.id()).orElseThrow();
            MineNetwork.Node node = network.claimedBy(miner).orElseThrow();
            List<ColonyPos> corridor = network.corridorOf(node);
            NetworkVeins.forget(miner);

            BlockPos firstCell = MinecraftTypeAdapter.toBlockPos(corridor.getFirst());

            // A primeira pedra fechada do corredor é recusada: o alvo tem de pular para outra.
            MineMarks.refuse(world, firstCell);

            Optional<BlockPos> target = NetworkDigging.nextTarget(world, miner, colony.id(), mine);

            context.assertTrue(target.isPresent() && !target.get().equals(firstCell),
                    "a célula recusada não foi pulada: " + target);
            context.assertTrue(network.stateOf(node.gx(), node.gz()).orElseThrow() != MineNetwork.State.SEALED,
                    "o nó foi selado por uma pedra recusada");

            // Agora todas as que faltam recusadas: três passagens falham o nó (FAILURES_BEFORE_SEALING).
            for (ColonyPos cell : corridor) {
                MineMarks.refuse(world, MinecraftTypeAdapter.toBlockPos(cell));
            }

            for (int pass = 0; pass < MineNetwork.FAILURES_BEFORE_SEALING; pass++) {
                NetworkVeins.forget(miner);
                network.claim(miner, mine.shaft().hallFloor());
                NetworkDigging.nextTarget(world, miner, colony.id(), mine);
            }

            context.assertTrue(network.stateOf(node.gx(), node.gz()).orElseThrow() == MineNetwork.State.SEALED,
                    "com toda pedra recusada o nó devia ter sido selado: "
                            + network.stateOf(node.gx(), node.gz()));
        } finally {
            for (BlockPos at : BlockPos.iterate(from, to)) {
                world.setBlockState(at, Blocks.AIR.getDefaultState());
            }

            world.setBlockState(chestAt, Blocks.AIR.getDefaultState());
            VillageColonyMod.MINES.removeOfColony(colony.id());
            MineNetworks.forget(colony.id());
            owned.cleanUp();
        }

        context.complete();
    }

}
