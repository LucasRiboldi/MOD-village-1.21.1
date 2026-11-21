package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.movement.Cell;
import com.villagecolony.core.movement.Terrain;
import com.villagecolony.core.type.ColonyPos;
import net.minecraft.server.world.ServerWorld;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Uma descida por arco — Regra 348 (autor, 2026-11-20, jogo da 0.4.111): nos {@value #ONE_DESCENT_LEVELS} primeiros
 * níveis, o mineiro só cava as células do desenho da mina — a escada que sai dos três blocos sob o arco, o salão e o
 * ramal que leva ao nível seguinte. A descida cavada (Regra 336) e o desvio não abrem escada nem túnel fora dele. Abaixo
 * desses níveis a rocha volta a ser livre: é lá que a mina cresce em braços.
 */
final class MineDigLimit {

    /** Os níveis do topo que são uma descida só, sem braço e sem escada nova. */
    static final int ONE_DESCENT_LEVELS = 2;

    private final Set<ColonyPos> planned;

    /** Abaixo desta altura (exclusiva) a rocha é livre; {@link Integer#MIN_VALUE} quando nada é livre ainda. */
    private final int freeBelow;

    MineDigLimit(Set<ColonyPos> planned, int freeBelow) {
        this.planned = Set.copyOf(planned);
        this.freeBelow = freeBelow;
    }

    /** A trava da mina desta colônia; sem mina, nada é travado. */
    static MineDigLimit of(ServerWorld world, UUID colonyId) {
        return VillageColonyMod.MINES.of(colonyId)
                .map(mine -> of(MineEntrance.levels(world, colonyId, mine), mine))
                .orElse(new MineDigLimit(Set.of(), Integer.MAX_VALUE));
    }

    /**
     * A trava dos níveis {@code levels} (da superfície até o de agora, o último): as células planejadas deles e os
     * quatro ramais do nível de agora; livre só abaixo do fundo dos {@value #ONE_DESCENT_LEVELS} primeiros.
     */
    static MineDigLimit of(List<MineShaft> levels, Mine mine) {
        Set<ColonyPos> planned = new HashSet<>(mine.plannedCells());
        int topBottom = Integer.MAX_VALUE;

        for (int level = 0; level < levels.size(); level++) {
            Set<ColonyPos> cells = levels.get(level).plannedCells();

            planned.addAll(cells);

            if (level < ONE_DESCENT_LEVELS) {
                topBottom = Math.min(topBottom, cells.stream().mapToInt(ColonyPos::y).min().orElse(topBottom));
            }
        }

        return new MineDigLimit(planned, levels.size() < ONE_DESCENT_LEVELS ? Integer.MIN_VALUE : topBottom);
    }

    /** Se a rocha desta célula pode sair. */
    boolean mayCut(ColonyPos cell) {
        return cell.y() < freeBelow || planned.contains(cell);
    }

    /** O terreno de {@code base} em que a rocha fora do desenho, nos níveis do topo, não se cava. */
    Terrain over(Terrain base) {
        return pos -> {
            Cell cell = base.at(pos);

            return cell.diggable() && !mayCut(pos) ? Cell.FIRM : cell;
        };
    }
}
