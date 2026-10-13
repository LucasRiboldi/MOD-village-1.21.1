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
 */
public class NetworkDiggingGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "network_digging", tickLimit = 40)
    public void aNodeIsDugVeinFirstMadeSafeAndOpensItsNeighbours(TestContext context) {
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
        ((Inventory) world.getBlockEntity(chestAt)).setStack(0, new ItemStack(Items.COBBLESTONE, 64));

        // Um nível acima da arena: a boca a 42, o salão dez abaixo, e a galeria já liberada.
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

            context.assertTrue(NetworkDigging.galleryPhase(mine), "a mina com o salão cavado não está na fase da rede");

            Optional<BlockPos> first = NetworkDigging.nextTarget(world, miner, colony.id(), mine);
            MineNetwork network = MineNetworks.of(colony.id()).orElseThrow();
            MineNetwork.Node node = network.claimedBy(miner).orElseThrow();
            List<ColonyPos> corridor = network.corridorOf(node);

            context.assertTrue(first.map(MinecraftTypeAdapter::toColonyPos).map(corridor::contains).orElse(false),
                    "o primeiro alvo não é do corredor do nó: " + first);

            // Abre o primeiro passo do túnel e põe minério dentro da parede, a dois blocos,
            // sem encostar em ar: ao alcance do braço, ele vem antes do túnel (0.3.35: só o
            // exposto contava, e 1.411 blocos cavados deram 2 de ferro).
            for (ColonyPos cell : corridor.subList(0, MineNetwork.HEIGHT)) {
                world.setBlockState(MinecraftTypeAdapter.toBlockPos(cell), Blocks.AIR.getDefaultState());
            }

            BlockPos ore = MinecraftTypeAdapter.toBlockPos(corridor.getFirst()).add(-2 * node.dz(), 1, -2 * node.dx());
            world.setBlockState(ore, Blocks.IRON_ORE.getDefaultState());
            NetworkVeins.forget(miner);

            context.assertTrue(NetworkDigging.nextTarget(world, miner, colony.id(), mine).equals(Optional.of(ore)),
                    "o minério ao alcance do túnel não passou à frente");

            // O túnel inteiro aberto, um buraco no piso e uma queda ao lado do piso.
            world.setBlockState(ore, Blocks.STONE.getDefaultState());
            NetworkVeins.forget(miner);

            for (ColonyPos cell : corridor) {
                world.setBlockState(MinecraftTypeAdapter.toBlockPos(cell), Blocks.AIR.getDefaultState());
            }

            BlockPos hole = MinecraftTypeAdapter.toBlockPos(corridor.get(2 * MineNetwork.HEIGHT)).down();
            world.setBlockState(hole, Blocks.AIR.getDefaultState());

            BlockPos edge = MinecraftTypeAdapter.toBlockPos(corridor.get(MineNetwork.HEIGHT)).add(-node.dz(), 0, -node.dx());
            world.setBlockState(edge, Blocks.AIR.getDefaultState());
            world.setBlockState(edge.down(), Blocks.AIR.getDefaultState());
            world.setBlockState(edge.down(2), Blocks.AIR.getDefaultState());

            context.assertTrue(NetworkDigging.nextTarget(world, miner, colony.id(), mine).isEmpty(),
                    "com o túnel aberto não havia mais pedra a dar");
            context.assertTrue(NetworkSafety.isFirmFloor(world, hole), "o buraco do piso ficou aberto");
            context.assertTrue(!NetworkSafety.isOpen(world.getBlockState(edge)),
                    "a beirada da queda ao lado do piso ficou aberta");
            context.assertTrue(network.stateOf(node.gx(), node.gz()).orElseThrow() == MineNetwork.State.DONE,
                    "o nó não ficou pronto");
            context.assertTrue(network.stateOf(node.gx() * 2, node.gz() * 2).isPresent(),
                    "o nó pronto não abriu o vizinho à frente");
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

    /** O nível sem nó aberto faz a mina descer um nível — ADR-042 D7. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "network_descend", tickLimit = 20)
    public void anExhaustedLevelTakesTheMineOneLevelDeeper(TestContext context) {
        ServerWorld world = context.getWorld();
        Colony colony = Colony.create(UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(1, 2, 1))));
        ColonyFixture owned = ColonyFixture.create().owning(colony);
        VillageColonyMod.COLONIES.register(colony);

        ColonyPos entry = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(3, 42, 3)));
        int shared = MineShaft.SHARED_BLOCKS;
        Mine mine = Mine.restore(colony.id(), MineShaft.from(entry, Side.EAST), new int[] {shared, shared, shared, shared});
        VillageColonyMod.MINES.restore(mine);
        ColonyPos hub = mine.shaft().hallFloor();

        // O salão pronto e os quatro vizinhos vedados: nada mais a abrir neste nível.
        MineNetwork exhausted = MineNetwork.restore(hub, new int[] {
            1, MineNetwork.RADIUS, 5,
            0, 0, 0, 0, 2,
            1, 0, 0, 0, 3,
            -1, 0, 0, 0, 3,
            0, 1, 0, 0, 3,
            0, -1, 0, 0, 3});
        MineNetworks.restore(colony.id(), exhausted);

        try {
            context.assertTrue(exhausted.exhausted(), "o cenário não esgotou o nível");

            NetworkDigging.nextOrDescend(world, UUID.randomUUID(), colony.id(), mine,
                    MinecraftTypeAdapter.toBlockPos(colony.center()));

            context.assertTrue(mine.entry().y() < entry.y(),
                    "o nível esgotado não fez a mina descer: a entrada continua em " + mine.entry());
        } finally {
            VillageColonyMod.MINES.removeOfColony(colony.id());
            MineNetworks.forget(colony.id());
            owned.cleanUp();
        }

        context.complete();
    }
}
