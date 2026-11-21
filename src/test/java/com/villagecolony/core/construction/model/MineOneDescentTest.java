package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Regra 348: nos níveis do topo a mina é uma descida só — os ramais 1 a 3 ficam fechados, o ramal zero leva ao nível
 * seguinte, e a escada fechada antes do salão recomeça igual, sem girar nem mudar de ponto.
 */
class MineOneDescentTest {

    private static final ColonyPos MOUTH = new ColonyPos(100, 64, 200);

    private static Mine topLevel() {
        Mine mine = Mine.open(UUID.randomUUID(), MineShaft.from(MOUTH, Side.NORTH));

        mine.keepOneDescent(true);

        return mine;
    }

    @Test
    void onTheTopLevelTheOtherArmsStayShutAfterTheStairIsOpen() {
        Mine mine = topLevel();

        for (int index = 0; index < MineShaft.SHARED_BLOCKS + 1; index++) {
            mine.arm(0).nextPosition();
        }

        assertEquals(1, mine.branchesOpenNow(), "Regra 348: um braço abriu no nível do topo");
    }

    @Test
    void onTheTopLevelTheMineGoesDownWhenArmZeroIsDone() {
        Mine mine = topLevel();
        ColonyPos next = mine.shaft().deepened().entry();

        while (!mine.arm(0).reachedTheEndOfTheArm()) {
            mine.arm(0).nextPosition();
        }
        mine.arm(0).finish();

        assertEquals(Mine.LevelAdvance.DEEPENED, mine.advanceIfEveryOpenArmIsDone(),
                "Regra 348: o nível do topo esperou os braços que não abrem");
        assertEquals(next, mine.entry(), "o nível seguinte desce do salão do ramal zero");
    }

    @Test
    void belowTheTopTheFourArmsOpen() {
        Mine mine = topLevel();

        mine.keepOneDescent(false);

        for (int index = 0; index < MineShaft.SHARED_BLOCKS + 1; index++) {
            mine.arm(0).nextPosition();
        }

        assertEquals(Mine.ARMS, mine.branchesOpenNow());
    }

    @Test
    void theStairIsRetriedTheSameWayWithoutTurning() {
        Mine mine = topLevel();
        MineShaft before = mine.shaft();

        mine.arm(0).nextPosition();
        mine.retryTheStair();

        assertEquals(before, mine.shaft(), "Regra 348: a escada do arco girou ou mudou de ponto");
        assertEquals(0, mine.helicesTried(), "Regra 348: tentar de novo não conta hélice");
        assertEquals(0, mine.arm(0).cut(), "o ramal recomeça no primeiro degrau");
    }
}
