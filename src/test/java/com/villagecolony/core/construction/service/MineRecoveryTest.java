package com.villagecolony.core.construction.service;

import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A decisão pura por trás de {@code rerouteOrBlameTheMouth} — decisão 3B,
 * 2026-09-24.
 *
 * <p><b>Não é uma reescrita do E45.</b> {@code MineDigging.rerouteOrBlameTheMouth}
 * já decidia isto corretamente; esta classe só separa a decisão (pura, em
 * {@code core}) do efeito (achar a boca nova, que precisa de
 * {@code ServerWorld} e mora em {@code fabric}). Ver
 * {@link com.villagecolony.core.construction.service.MineRecovery}.
 *
 * <p><b>Fora de escopo, de propósito.</b> {@code MineClaims} (quem está em
 * qual ramal agora) é outro conceito — mutex de reserva por mineiro, e
 * mutável por natureza —, não decisão sobre a geometria da mina. Esta
 * classe nunca soube quem está cavando.
 */
class MineRecoveryTest {

    private static Mine freshMine() {
        MineShaft shaft = MineShaft.from(new ColonyPos(0, 60, 0), Side.NORTH);

        return Mine.open(UUID.randomUUID(), shaft);
    }

    @Test
    void freshMineNeedsNoRecovery() {
        Mine mine = freshMine();

        assertEquals(MineRecovery.Decision.NO_ACTION, MineRecovery.recover(mine));
    }

    @Test
    void turningLessThanTheThresholdStillWaits() {
        Mine mine = freshMine();

        for (int i = 0; i < Mine.TURNS_BEFORE_REROUTING - 1; i++) {
            mine.turnedWithoutAPickaxe();
        }

        assertEquals(MineRecovery.Decision.NO_ACTION, MineRecovery.recover(mine));
    }

    @Test
    void reachingTheThresholdReroutesTheHelix() {
        Mine mine = freshMine();

        for (int i = 0; i < Mine.TURNS_BEFORE_REROUTING; i++) {
            mine.turnedWithoutAPickaxe();
        }

        assertEquals(MineRecovery.Decision.REROUTE, MineRecovery.recover(mine));
    }

    @Test
    void everyHelixFailingBlamesTheMouth() {
        Mine mine = freshMine();

        // As HELICES_BEFORE_BLAMING_THE_MOUTH voltas giram a hélice de
        // verdade — é o reroute() do chamador aplicando a decisão
        // anterior, até helicesTried atingir o teto. A rodada de voltas
        // seguinte fica sem reroute(): é o instante em que
        // mouthIsHopeless() já é verdade, e é isso que
        // MineRecovery.recover precisa enxergar.
        for (int helix = 0; helix < Mine.HELICES_BEFORE_BLAMING_THE_MOUTH; helix++) {
            for (int i = 0; i < Mine.TURNS_BEFORE_REROUTING; i++) {
                mine.turnedWithoutAPickaxe();
            }

            mine.reroute();
        }

        for (int i = 0; i < Mine.TURNS_BEFORE_REROUTING; i++) {
            mine.turnedWithoutAPickaxe();
        }

        assertEquals(MineRecovery.Decision.EXHAUST_MOUTH, MineRecovery.recover(mine));
    }

    /** Uma pedra que sai do mundo zera a paciência: a mina não estava presa. */
    @Test
    void progressResetsThePatienceAndCancelsRecovery() {
        Mine mine = freshMine();

        for (int i = 0; i < Mine.TURNS_BEFORE_REROUTING; i++) {
            mine.turnedWithoutAPickaxe();
        }

        mine.pickaxeTook();

        assertEquals(MineRecovery.Decision.NO_ACTION, MineRecovery.recover(mine));
    }

    /** A decisão nunca muda o estado da mina — só olha e responde. */
    @Test
    void recoveryNeverMutatesTheMine() {
        Mine mine = freshMine();

        for (int i = 0; i < Mine.TURNS_BEFORE_REROUTING; i++) {
            mine.turnedWithoutAPickaxe();
        }

        MineShaft before = mine.shaft();
        int turnsBefore = mine.turnsWithoutAPickaxe();
        int helicesBefore = mine.helicesTried();

        MineRecovery.recover(mine);

        assertEquals(before, mine.shaft());
        assertEquals(turnsBefore, mine.turnsWithoutAPickaxe());
        assertEquals(helicesBefore, mine.helicesTried());
    }
}
