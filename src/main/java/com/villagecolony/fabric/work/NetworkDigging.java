package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineArm;
import com.villagecolony.core.construction.model.MineNetwork;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.construction.service.MineNetworks;
import com.villagecolony.core.coordination.IdleReason;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.MineFlooding;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A próxima pedra da mina em rede — ADR-042.
 *
 * <p>A escadaria e o salão de cada nível continuam sendo cavados como antes, por um
 * mineiro só ({@link MineDigging}, o ramal zero até {@link MineShaft#SHARED_BLOCKS}).
 * Dali em diante o nível é um grafo de nós ({@link MineNetwork}): cada mineiro pega o
 * nó aberto mais perto dele, minera primeiro o veio que aparece na parede
 * ({@link NetworkVeins}), depois o corredor; com o corredor aberto, o piso é garantido
 * e as aberturas vedadas ({@link NetworkSafety}) e o nó fica pronto, abrindo os
 * vizinhos. Sem nó aberto nem em obra, o nível acabou e a mina desce.
 */
public final class NetworkDigging {

    static final String SUBJECT = "miner network";

    private NetworkDigging() {
    }

    /**
     * A mina já passou da escadaria e do salão deste nível: as galerias são em rede. O
     * ramal zero é quem cava a escadaria e o salão; só com ele além de
     * {@link MineShaft#SHARED_BLOCKS} o salão existe para os corredores saírem dele.
     */
    public static boolean galleryPhase(Mine mine) {
        return !mine.shaft().isRamp()
                && mine.branchesOpenNow() == Mine.ARMS
                && mine.arm(0).cut() >= MineShaft.SHARED_BLOCKS;
    }

    /** O mesmo, pela colônia. */
    static boolean galleryPhase(UUID colonyId) {
        return VillageColonyMod.MINES.of(colonyId).map(NetworkDigging::galleryPhase).orElse(false);
    }

    /** O nível em rede do salão de agora. */
    static MineNetwork levelOf(UUID colonyId, Mine mine) {
        return MineNetworks.forLevel(colonyId, mine.shaft().hallFloor());
    }

    /**
     * A próxima pedra deste mineiro na rede, ou vazio nesta passagem (piso por garantir,
     * nó que acabou de ficar pronto, nenhum nó aberto).
     */
    static Optional<BlockPos> nextTarget(ServerWorld world, UUID workerId, UUID colonyId, Mine mine) {
        MineNetwork network = levelOf(colonyId, mine);

        if (!network.shaftSecured() && NetworkSafety.secureShaft(world, colonyId, mine, network)) {
            network.shaftIsSecured();
            VillageColonyMod.LOGGER.info("Mine {} secured the stair and the hall of the level at {}",
                    colonyId.toString().substring(0, 8), network.hub());
        }

        // A reivindicação parte do salão do nível, não do corpo do mineiro — mina M, 2026-10-13.
        // O mineiro sai do nível para depositar e voltar; medir do corpo fazia-o pegar sempre o nó
        // mais raso, cavar uma ou duas pedras e subir (playtest 0.3.48: 336 "looking for stone"
        // contra 12 "digging", parado em "11 of 86" por 33 min). Do salão ele reclama a frente de
        // trabalho e cava vários antes de subir.
        Optional<MineNetwork.Node> claimed = network.claim(workerId, network.hub());

        if (claimed.isEmpty()) {
            IdleLog.recordAt(colonyId, SUBJECT, IdleReason.NO_TARGET,
                    "every node of the level is taken or done — " + network.census(), world.getTime());

            return Optional.empty();
        }

        MineNetwork.Node node = claimed.get();
        List<ColonyPos> corridor = network.corridorOf(node);

        // Nó com fluido não se abre (D6).
        BlockPos center = MinecraftTypeAdapter.toBlockPos(network.centerOf(node.gx(), node.gz())).up();

        if (world.isChunkLoaded(center) && !world.getFluidState(center).isEmpty()) {
            network.seal(node);
            VillageColonyMod.LOGGER.info("Mine {} seals the node ({}, {}) — water or lava at its center",
                    colonyId.toString().substring(0, 8), node.gx(), node.gz());

            return Optional.empty();
        }

        Optional<BlockPos> ore = NetworkVeins.next(world, workerId, network, node);

        if (ore.isPresent()) {
            IdleLog.clear(colonyId, SUBJECT);

            return ore;
        }

        for (ColonyPos cell : corridor) {
            BlockPos at = MinecraftTypeAdapter.toBlockPos(cell);

            if (!world.isChunkLoaded(at)) {
                return Optional.empty();
            }

            BlockState state = world.getBlockState(at);

            if (NetworkSafety.isOpen(state)) {
                continue;
            }

            // Célula recusada (sem lugar de pé, alta demais): pula para a próxima do corredor
            // — a frente do túnel anda, em vez de selar o nó inteiro por uma pedra (mina M,
            // 2026-10-13). Só quando TODAS as que faltam estão recusadas o nó falha.
            if (MineMarks.isOutOfReach(world, at)) {
                continue;
            }

            // Pedra que segura água: o nó é vedado em vez de inundar o corredor (D6).
            if (MineFlooding.holdsBackFluid(world, at)) {
                network.seal(node);
                VillageColonyMod.LOGGER.info("Mine {} seals the node ({}, {}) — {} holds back water or lava",
                        colonyId.toString().substring(0, 8), node.gx(), node.gz(), at.toShortString());

                return Optional.empty();
            }

            IdleLog.clear(colonyId, SUBJECT);

            return Optional.of(at);
        }

        // Todas as células que faltam estão recusadas: aí sim o nó falha (mina M).
        if (corridorHasClosedButAllRefused(world, corridor)) {
            if (network.failed(node)) {
                VillageColonyMod.LOGGER.info("Mine {} seals the node ({}, {}) — every stone left is out of reach",
                        colonyId.toString().substring(0, 8), node.gx(), node.gz());
            } else {
                network.release(workerId);
            }

            return Optional.empty();
        }

        // Corredor aberto: piso garantido e aberturas vedadas, e o nó fica pronto. O enchimento
        // surge (a mina não gasta recurso da vila, 2026-10-13), então uma passagem que não
        // terminou foi só o teto de PER_PASS deste tique — o próximo fecha o resto.
        Set<ColonyPos> cells = mineCells(network, mine);

        if (!NetworkSafety.secureCorridor(world, colonyId, corridor, cells)) {
            return Optional.empty();
        }

        network.done(node);
        VillageColonyMod.LOGGER.info("Mine {} finished the node ({}, {}) of the level at {} — {}",
                colonyId.toString().substring(0, 8), node.gx(), node.gz(), network.hub(), network.census());

        return Optional.empty();
    }

    /** Quantos anéis de caminhos laterais o fundo ganha antes de a mina voltar subindo (D8). */
    static final int BOTTOM_RINGS = MineNetwork.RADIUS;

    /**
     * A próxima pedra da rede; com o nível acabado, a mina desce um nível (D7). No fundo
     * o nível ganha anéis de caminhos laterais, e depois a mina volta subindo pela rampa
     * e abre outra boca na borda oposta — o percurso não acaba (D8).
     */
    static Optional<BlockPos> nextOrDescend(
            ServerWorld world, UUID workerId, UUID colonyId, Mine mine, BlockPos center) {

        Optional<BlockPos> next = nextTarget(world, workerId, colonyId, mine);
        MineNetwork network = levelOf(colonyId, mine);

        if (next.isPresent() || !network.exhausted()) {
            return next;
        }

        if (mine.shaft().mayDeepen()) {
            // Os ramais antigos não cavam mais na rede: terminam aqui, e a mina desce.
            mine.arms().forEach(MineArm::finish);

            if (mine.advanceIfEveryArmIsDone() == Mine.LevelAdvance.DEEPENED) {
                VillageColonyMod.LOGGER.info(
                        "Mine {} finished every node of the level and went one level deeper — {}",
                        colonyId, network.census());
            }

            return Optional.empty();
        }

        if (network.radius() < MineNetwork.RADIUS + BOTTOM_RINGS) {
            network.widen();
            VillageColonyMod.LOGGER.info("Mine {} is at the bottom and opens lateral paths — radius {} nodes",
                    colonyId.toString().substring(0, 8), network.radius());

            return Optional.empty();
        }

        if (MineTrouble.climbOutAtBottom(colonyId, mine, center)) {
            return Optional.empty();
        }

        if (MineBottomRetry.isDue(colonyId, world.getTime())) {
            if (MineTrouble.abandonAtBottom(world, colonyId, mine, center)) {
                MineBottomRetry.clear(colonyId);
            } else {
                MineBottomRetry.defer(colonyId, world.getTime());
            }
        }

        return Optional.empty();
    }

    /** Se o corredor ainda tem pedra fechada, mas toda a que falta está recusada (mina M). */
    static boolean corridorHasClosedButAllRefused(ServerWorld world, List<ColonyPos> corridor) {
        boolean anyClosed = false;

        for (ColonyPos cell : corridor) {
            BlockPos at = MinecraftTypeAdapter.toBlockPos(cell);

            if (!world.isChunkLoaded(at) || NetworkSafety.isOpen(world.getBlockState(at))) {
                continue;
            }

            anyClosed = true;

            if (!MineMarks.isOutOfReach(world, at)) {
                return false;
            }
        }

        return anyClosed;
    }

    /** O que é da mina neste nível: a escadaria, o salão e os corredores. */
    static Set<ColonyPos> mineCells(MineNetwork network, Mine mine) {
        Set<ColonyPos> cells = new HashSet<>(network.cells());

        for (int index = 0; index < MineShaft.SHARED_BLOCKS; index++) {
            cells.add(mine.shaft().positionAt(index));
        }

        return cells;
    }

    /** Uma pedra do nó foi cavada: as desistências dele recomeçam. */
    static void worked(UUID colonyId, UUID workerId) {
        MineNetworks.of(colonyId).ifPresent(network ->
                network.claimedBy(workerId).ifPresent(network::worked));
    }

    /**
     * O mineiro desistiu de uma pedra do nó dele.
     *
     * @return se o nó foi vedado agora
     */
    static boolean gaveUp(UUID colonyId, UUID workerId) {
        return MineNetworks.of(colonyId)
                .flatMap(network -> network.claimedBy(workerId).map(node -> {
                    boolean sealed = network.failed(node);

                    if (sealed) {
                        VillageColonyMod.LOGGER.info(
                                "Mine {} seals the node ({}, {}) after {} stones nobody could reach",
                                colonyId.toString().substring(0, 8), node.gx(), node.gz(),
                                MineNetwork.FAILURES_BEFORE_SEALING);
                    } else {
                        network.release(workerId);
                    }

                    return sealed;
                }))
                .orElse(false);
    }

    /** Larga o nó deste mineiro, em qualquer colônia. */
    static void release(UUID workerId) {
        for (var colony : VillageColonyMod.COLONIES.all()) {
            MineNetworks.of(colony.id()).ifPresent(network -> network.release(workerId));
        }
    }

    /** Só quem ainda trabalha segura nó. */
    static void retainOnly(Collection<UUID> working) {
        for (var colony : VillageColonyMod.COLONIES.all()) {
            MineNetworks.of(colony.id()).ifPresent(network -> {
                for (var worker : VillageColonyMod.WORKERS.ofColony(colony.id())) {
                    if (!working.contains(worker.villagerId())) {
                        network.release(worker.villagerId());
                    }
                }
            });
        }
    }
}
