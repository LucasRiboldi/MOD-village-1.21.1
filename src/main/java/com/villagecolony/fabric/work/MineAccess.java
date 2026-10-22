package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.movement.DetourMoves;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.work.MinerWork.Job;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * O acesso cavado da vila até o nível da mina — decisão do autor de 2026-10-22 ("cavar um acesso
 * descendo").
 *
 * <p>No deserto a entrada da mina ficou no alto da duna (82,94,-434), e o mapa de alcance a deu como
 * inalcançável a sessão inteira; o mineiro chegava lá construindo caminho para fora da montanha, o que a
 * regra do mesmo dia proíbe ({@link MinerTerrain}). Quando o último {@link ReachMap} diz que a entrada não
 * se alcança a pé, o mineiro cava um túnel da célula alcançável mais perto da entrada do nível de agora até
 * dentro dele: só cavando, nunca pondo bloco ({@link MinerTerrain#digOnly}), e descendo antes de ir de lado
 * ({@link DetourMoves.Costs#MINER}). Depois de cavado, a descida passa pelo túnel e a escada segue dali.
 * Uma vez por mina; depois de {@link #MAX_FAILURES} túneis que não chegaram, desiste dela.
 */
final class MineAccess {

    static {
        ServerMemory.register(MineAccess.class, MineAccess::clearAll);
    }

    /** Túneis que não chegaram antes de desistir desta mina. */
    static final int MAX_FAILURES = 3;

    /** O acesso de uma mina: de onde parte, até qual nível, se já foi cavado e quantas vezes falhou. */
    private static final class Access {
        final Mine mine;
        final BlockPos start;
        final BlockPos level;
        boolean dug;
        int failures;

        Access(Mine mine, BlockPos start, BlockPos level) {
            this.mine = mine;
            this.start = start;
            this.level = level;
        }
    }

    private static final Map<UUID, Access> ACCESS = new HashMap<>();

    private MineAccess() {
    }

    /**
     * Para onde o mineiro fora da mina anda em vez da entrada no alto: o começo do túnel ainda por cavar, ou
     * o fim dele, já dentro do nível.
     *
     * @return vazio quando a entrada se alcança a pé, quando não há de onde partir, ou depois de desistir
     */
    static Optional<BlockPos> leg(ServerWorld world, UUID colonyId) {
        Optional<Access> access = accessOf(world, colonyId);

        if (access.isEmpty() || access.get().failures >= MAX_FAILURES) {
            return Optional.empty();
        }

        return Optional.of(access.get().dug ? access.get().level : access.get().start);
    }

    /** Se ele está no começo do túnel ainda por cavar. */
    static boolean atTheStart(ServerWorld world, UUID colonyId, BlockPos feet) {
        Optional<Access> access = accessOf(world, colonyId);

        return access.isPresent() && !access.get().dug && access.get().failures < MAX_FAILURES
                && feet.isWithinDistance(access.get().start, 2.5);
    }

    /**
     * Começa a cavar o túnel: um desvio só-cavar até dentro do nível.
     *
     * @return se o desvio começou
     */
    static boolean begin(ServerWorld world, VillagerEntity villager, Job job) {
        Access access = ACCESS.get(job.task.colonyId());

        if (access == null) {
            return false;
        }

        BlockPos level = access.level;
        Optional<DetourWalker> walker = DetourWalker.plan(
                world, villager.getUuid(), villager.getBlockPos(), level,
                at -> MineEntrance.insideTheLevel(at, level), Set.of(), false,
                MinerTerrain::digOnly, false, DetourMoves.Costs.MINER);

        if (walker.isEmpty()) {
            failed(job.task.colonyId(), "no dig-only way from " + villager.getBlockPos().toShortString()
                    + " down to the level at " + level.toShortString());

            return false;
        }

        job.detour = walker.get();
        job.accessDetour = true;
        job.stall.reset();
        job.lease.reset();
        job.descent.reset();
        job.stalled = 0;

        VillageColonyMod.LOGGER.info(
                "Miner {} digs the mine access from {} down to the level at {} — {} steps, only digging",
                villager.getUuid().toString().substring(0, 8), villager.getBlockPos().toShortString(),
                level.toShortString(), walker.get().steps());

        return true;
    }

    /** O túnel chegou ao nível: daqui em diante a descida passa por ele. */
    static void dug(UUID colonyId, BlockPos at) {
        Access access = ACCESS.get(colonyId);

        if (access != null) {
            access.dug = true;

            VillageColonyMod.LOGGER.info("Mine {} has a dug access from {} to the level at {} — the miners go in by it",
                    colonyId.toString().substring(0, 8), access.start.toShortString(), at.toShortString());
        }
    }

    /** O túnel não chegou. */
    static void failed(UUID colonyId, String why) {
        Access access = ACCESS.get(colonyId);

        if (access == null) {
            return;
        }

        access.failures++;

        VillageColonyMod.LOGGER.info("Mine {} access failed ({} of {}) — {}",
                colonyId.toString().substring(0, 8), access.failures, MAX_FAILURES, why);
    }

    /** O acesso desta mina, escolhido uma vez quando o mapa de alcance dá a entrada como inalcançável. */
    private static Optional<Access> accessOf(ServerWorld world, UUID colonyId) {
        Optional<Mine> mine = VillageColonyMod.MINES.of(colonyId);
        Access known = ACCESS.get(colonyId);

        if (mine.isEmpty()) {
            return Optional.empty();
        }

        if (known != null && known.mine == mine.get()) {
            return Optional.of(known);
        }

        if (!entranceUnreached(colonyId)) {
            return Optional.empty();
        }

        Optional<Colony> colony = VillageColonyMod.COLONIES.find(colonyId);
        List<BlockPos> route = MineEntrance.route(world, colonyId, mine.get());

        if (colony.isEmpty() || route.isEmpty()) {
            return Optional.empty();
        }

        BlockPos level = route.getLast();
        Optional<BlockPos> start = nearestReachable(ReachMap.reachableCells(world, colony.get()), level);

        if (start.isEmpty()) {
            return Optional.empty();
        }

        Access access = new Access(mine.get(), start.get(), level);

        ACCESS.put(colonyId, access);

        VillageColonyMod.LOGGER.info(
                "Mine {} entrance cannot be reached on foot — the miners will dig an access from {} down to the level at {}",
                colonyId.toString().substring(0, 8), start.get().toShortString(), level.toShortString());

        return Optional.of(access);
    }

    private static boolean entranceUnreached(UUID colonyId) {
        return ReachMap.of(colonyId)
                .map(result -> result.unreached().stream().anyMatch(place -> place.startsWith("mine entrance")))
                .orElse(false);
    }

    /**
     * A célula alcançável de onde o túnel sai: perto do nível na horizontal e acima dele, para o túnel
     * descer em vez de subir.
     */
    static Optional<BlockPos> nearestReachable(Set<BlockPos> reachable, BlockPos level) {
        BlockPos best = null;
        long bestScore = Long.MAX_VALUE;

        for (BlockPos cell : reachable) {
            int flat = Math.abs(cell.getX() - level.getX()) + Math.abs(cell.getZ() - level.getZ());
            int below = Math.max(0, level.getY() - cell.getY());
            long score = flat * 4L + below * 16L + Math.abs(cell.getY() - level.getY());

            if (score < bestScore) {
                best = cell;
                bestScore = score;
            }
        }

        return Optional.ofNullable(best);
    }

    static void clearAll() {
        ACCESS.clear();
    }
}
