package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A mina cresce por dentro — Regra 182 (autor, 2026-11-05): quando a escada acaba, abre um ramal novo a partir dos salões
 * que já existem, sem cortar as escadas, em vez de abrir outra boca.
 *
 * <p>Todo nível tem quatro salões de ramal, e só o do ramal zero vira o nível seguinte. Os outros três são os pontos de
 * onde a mina recomeça: {@code MineShaft.turned().deepened()}. Boca nova só quando {@value #FAILURES_FOR_A_NEW_MOUTH}
 * tentativas falharam, ou quando tudo o que resta está tomado por água.
 */
public final class MineBranches {

    static final int FAILURES_FOR_A_NEW_MOUTH = 50;

    /** A procura lê milhares de blocos: uma por este intervalo enquanto nada serve. */
    private static final long SEARCH_EVERY = 600;

    /** Mais aberto que isto já é mina ou caverna grande. */
    private static final double MAX_OPEN = 0.25;

    /** Dos cinco pontos olhados no salão de partida, quantos precisam estar cavados. */
    private static final int HALL_SAMPLES_DUG = 4;

    private static final int HALL_SAMPLE = 3;

    /** Só os níveis nesta faixa de altura podem ter escada no caminho. */
    private static final int LEVEL_RANGE = 3 * MineShaft.DESCENT;

    private enum Verdict { FREE, WATER, TAKEN, NOT_DUG }

    private static final class Progress {
        /** Os níveis conhecidos, e a escada da superfície até cada um. */
        final Map<MineShaft, List<ColonyPos>> levels = new LinkedHashMap<>();
        final Set<ColonyPos> used = new HashSet<>();
        int failures;
        boolean branchOpen;
        boolean dug;
        boolean onlyWaterLeft;
        long nextSearch = Long.MIN_VALUE;
    }

    private static final Map<UUID, Progress> PROGRESS = new HashMap<>();

    static {
        ServerMemory.register(MineBranches.class, PROGRESS::clear);
    }

    private MineBranches() {
    }

    /** A picareta tirou um bloco: o ramal de agora rendeu. */
    static void dug(UUID colonyId) {
        Progress progress = PROGRESS.get(colonyId);

        if (progress != null) {
            progress.dug = true;
        }
    }

    /** A boca nova só é permitida depois de esgotar os ramais, ou com tudo na água. */
    static boolean mayOpenNewMouth(UUID colonyId) {
        Progress progress = PROGRESS.get(colonyId);

        return progress != null && (progress.failures >= FAILURES_FOR_A_NEW_MOUTH || progress.onlyWaterLeft);
    }

    /** A conta de falhas chega ao teto de uma vez. Só a bateria pede isto, para exercitar a boca nova (Regra 182). */
    public static void allBranchesFailed(UUID colonyId) {
        PROGRESS.computeIfAbsent(colonyId, ignored -> new Progress()).failures = FAILURES_FOR_A_NEW_MOUTH;
    }

    /** A mina mudou de boca: a conta recomeça. */
    static void clear(UUID colonyId) {
        PROGRESS.remove(colonyId);
    }

    /** Abre um ramal novo; falso quando nenhum serve agora, e a tentativa conta como falha. */
    static boolean expand(ServerWorld world, UUID colonyId, Mine mine) {
        Progress progress = PROGRESS.computeIfAbsent(colonyId, ignored -> new Progress());
        long now = world.getTime();

        if (now < progress.nextSearch) {
            return false;
        }

        progress.nextSearch = now + SEARCH_EVERY;
        settleLastBranch(progress);
        remember(world, colonyId, mine, progress);

        int water = 0;
        int taken = 0;
        List<MineShaft> known = new ArrayList<>(progress.levels.keySet());

        for (int level = known.size() - 1; level >= 0; level--) {
            MineShaft heading = known.get(level);

            for (int turn = 0; turn < Mine.ARMS; turn++, heading = heading.turned()) {
                if (!heading.mayDeepen() || progress.used.contains(heading.deepened().entry())) {
                    continue;
                }

                MineShaft branch = heading.deepened();
                Verdict verdict = judge(world, branch, known);

                if (verdict == Verdict.FREE) {
                    open(world, colonyId, branch, progress.levels.getOrDefault(known.get(level), List.of()), progress);

                    return true;
                }

                water += verdict == Verdict.WATER ? 1 : 0;
                taken += verdict == Verdict.TAKEN ? 1 : 0;
            }
        }

        progress.failures++;
        progress.onlyWaterLeft = water > 0 && taken == 0;
        VillageColonyMod.LOGGER.info("VC_MINE_BRANCH colony={} no branch to open: {} in water, {} taken, failure {} of {}",
                colonyId.toString().substring(0, 8), water, taken, progress.failures, FAILURES_FOR_A_NEW_MOUTH);

        return false;
    }

    private static void settleLastBranch(Progress progress) {
        if (progress.branchOpen) {
            progress.failures = progress.dug ? 0 : progress.failures + 1;
            progress.branchOpen = false;
        }
    }

    private static void remember(ServerWorld world, UUID colonyId, Mine mine, Progress progress) {
        // Na rampa a escada de agora não diz os níveis: vale o que já se sabe deles.
        if (mine.shaft().isRamp()) {
            return;
        }

        for (MineShaft level : MineEntrance.levels(world, colonyId, mine)) {
            List<ColonyPos> chain = MineEntrance.chainUpTo(world, colonyId, mine, level.entry());

            if (!chain.isEmpty()) {
                progress.levels.putIfAbsent(level, chain);
                progress.used.add(level.entry());
            }
        }
    }

    private static void open(ServerWorld world, UUID colonyId, MineShaft branch, List<ColonyPos> parentChain,
                             Progress progress) {
        VillageColonyMod.MINES.removeOfColony(colonyId);

        Mine fresh = VillageColonyMod.MINES.open(colonyId, branch);

        MineEntrance.branched(colonyId, parentChain, branch.entry());
        MineFurnishing.lightMine(world, fresh);
        progress.used.add(branch.entry());
        progress.branchOpen = true;
        progress.dug = false;
        progress.onlyWaterLeft = false;
        progress.nextSearch = Long.MIN_VALUE;
        VillageColonyMod.LOGGER.info("VC_MINE_BRANCH colony={} opens a branch at {}, descent {}, gallery {}",
                colonyId.toString().substring(0, 8), MinecraftTypeAdapter.toBlockPos(branch.entry()).toShortString(),
                branch.descent(), branch.gallery());
    }

    /** Se este ramal pode ser cavado: salão de partida aberto, sem água, sem cortar o chão de escada nenhuma. */
    private static Verdict judge(ServerWorld world, MineShaft branch, List<MineShaft> known) {
        if (!hallIsDug(world, branch.entry())) {
            return Verdict.NOT_DUG;
        }

        Set<ColonyPos> stairs = stairsNear(branch, known);
        Set<ColonyPos> cells = branch.plannedCells();
        int open = 0;

        for (ColonyPos cell : cells) {
            BlockPos at = MinecraftTypeAdapter.toBlockPos(cell);

            if (!world.isChunkLoaded(at.getX() >> 4, at.getZ() >> 4)) {
                return Verdict.TAKEN;
            }

            if (!world.getFluidState(at).isEmpty()) {
                return Verdict.WATER;
            }

            // A célula debaixo de um degrau, e que não é degrau, é o chão dele.
            if (!stairs.contains(cell) && stairs.contains(new ColonyPos(cell.x(), cell.y() + 1, cell.z()))) {
                return Verdict.TAKEN;
            }

            open += MineRock.isOpenSpace(world, at, world.getBlockState(at)) ? 1 : 0;
        }

        return (double) open / cells.size() > MAX_OPEN ? Verdict.TAKEN : Verdict.FREE;
    }

    private static boolean hallIsDug(ServerWorld world, ColonyPos floor) {
        int[][] samples = {{0, 0}, {HALL_SAMPLE, 0}, {-HALL_SAMPLE, 0}, {0, HALL_SAMPLE}, {0, -HALL_SAMPLE}};
        int dug = 0;

        for (int[] sample : samples) {
            BlockPos feet = new BlockPos(floor.x() + sample[0], floor.y() + 1, floor.z() + sample[1]);

            if (world.isChunkLoaded(feet.getX() >> 4, feet.getZ() >> 4)
                    && MineRock.isOpenSpace(world, feet, world.getBlockState(feet))) {
                dug++;
            }
        }

        return dug >= HALL_SAMPLES_DUG;
    }

    /** Os degraus dos níveis conhecidos na faixa de altura deste ramal: caracol do nível e caracol do ramal zero. */
    private static Set<ColonyPos> stairsNear(MineShaft branch, List<MineShaft> known) {
        Set<ColonyPos> stairs = new HashSet<>();
        int armStairCells = MineShaft.ARM_STAIRS * MineShaft.STAIR_STEP_BLOCKS;

        for (MineShaft level : known) {
            if (Math.abs(level.entry().y() - branch.entry().y()) > LEVEL_RANGE) {
                continue;
            }

            for (int index = 0; index < MineShaft.CARVED; index++) {
                stairs.add(level.positionAt(index));
            }

            for (int index = 0; index < armStairCells; index++) {
                stairs.add(level.positionAt(MineShaft.SHARED_BLOCKS + index));
            }
        }

        return stairs;
    }
}
