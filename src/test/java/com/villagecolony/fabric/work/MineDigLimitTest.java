package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.movement.Cell;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regra 348: nos dois primeiros níveis o mineiro só cava as células do desenho da mina — a descida cavada e o desvio
 * não abrem escada nem túnel fora dele; abaixo deles a rocha é livre.
 */
class MineDigLimitTest {

    private static final ColonyPos MOUTH = new ColonyPos(0, 70, 0);

    private static final MineShaft FIRST = MineShaft.from(MOUTH, Side.EAST);

    @Test
    void onTheFirstLevelOnlyThePlannedCellsAreCut() {
        Mine mine = Mine.open(UUID.randomUUID(), FIRST);
        MineDigLimit limit = MineDigLimit.of(List.of(FIRST), mine);
        ColonyPos firstStep = FIRST.positionAt(0);
        ColonyPos beside = new ColonyPos(MOUTH.x() + 20, MOUTH.y() - 3, MOUTH.z() + 20);
        ColonyPos deep = new ColonyPos(MOUTH.x(), 0, MOUTH.z());

        assertTrue(limit.mayCut(firstStep), "o primeiro degrau sob o arco não se cava");
        assertFalse(limit.mayCut(beside), "Regra 348: uma escada nova fora do arco se cava");
        assertFalse(limit.mayCut(deep), "Regra 348: com um nível só, nada abaixo é livre ainda");
        assertEquals(Cell.FIRM, limit.over(pos -> Cell.ROCK).at(beside), "o desvio cava fora do desenho");
        assertEquals(Cell.ROCK, limit.over(pos -> Cell.ROCK).at(firstStep));
    }

    @Test
    void belowTheTwoTopLevelsTheRockIsFree() {
        MineShaft second = FIRST.deepened();
        MineShaft third = second.deepened();
        Mine mine = Mine.open(UUID.randomUUID(), third);
        MineDigLimit limit = MineDigLimit.of(List.of(FIRST, second, third), mine);
        int bottom = second.plannedCells().stream().mapToInt(ColonyPos::y).min().orElseThrow();
        ColonyPos above = new ColonyPos(MOUTH.x() + 40, bottom, MOUTH.z() + 40);
        ColonyPos below = new ColonyPos(MOUTH.x() + 40, bottom - 1, MOUTH.z() + 40);

        assertFalse(limit.mayCut(above), "Regra 348: um túnel abriu no fundo do segundo nível");
        assertTrue(limit.mayCut(below), "abaixo dos dois níveis do topo a mina cresce em braços");
    }
}
