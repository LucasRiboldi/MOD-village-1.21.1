package com.villagecolony.fabric.work;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T4 do diagnóstico da 0.3.103 (decisão B do autor, 2026-10-28): a pedra alta demais ou sem caminho sai antes de
 * virar alvo. Na mesma passagem o mineiro pula a recusada e pede a próxima, em vez de gastar a passagem inteira
 * para recusar uma pedra só — nas cinco vilas simuladas, 70 das 93 recusas vinham em cadeia, uma por passagem.
 */
class MinerPicksTest {

    @Test
    void theRefusedStonesAreSkippedAndTheFirstGoodOneIsChosenInTheSamePass() {
        Deque<String> stones = new ArrayDeque<>(List.of("alta", "sem caminho", "boa", "outra"));
        List<String> skipped = new ArrayList<>();

        Optional<String> chosen = MinerPicks.firstAccepted(
                () -> Optional.ofNullable(stones.poll()), stone -> stone.equals("boa") || stone.equals("outra"),
                skipped::add);

        assertEquals(Optional.of("boa"), chosen);
        assertEquals(List.of("alta", "sem caminho"), skipped);
    }

    @Test
    void aPassLooksAtMostAFewStonesSoTheTickStaysCheap() {
        List<String> skipped = new ArrayList<>();

        Optional<String> chosen = MinerPicks.firstAccepted(() -> Optional.of("alta" + skipped.size()), stone -> false,
                skipped::add);

        assertEquals(Optional.empty(), chosen);
        assertEquals(MinerPicks.PICKS_PER_SEARCH, skipped.size());
    }

    @Test
    void theSameStoneServedTwiceEndsThePass() {
        List<String> skipped = new ArrayList<>();

        Optional<String> chosen = MinerPicks.firstAccepted(() -> Optional.of("a mesma"), stone -> false, skipped::add);

        assertEquals(Optional.empty(), chosen);
        assertEquals(List.of("a mesma"), skipped);
    }

    @Test
    void tooHighAndNoPathLeaveBeforeChoosingButNoFootholdStillTriesTheDetour() {
        assertTrue(MinerPicks.skipsBeforeChoosing(MiningTarget.Verdict.no(MiningTarget.Rejection.TOO_HIGH)));
        assertTrue(MinerPicks.skipsBeforeChoosing(MiningTarget.Verdict.no(MiningTarget.Rejection.NO_PATH)));
        assertFalse(MinerPicks.skipsBeforeChoosing(
                MiningTarget.Verdict.no(MiningTarget.Rejection.NO_STANDING_POSITION)));
        assertFalse(MinerPicks.skipsBeforeChoosing(MiningTarget.Verdict.at(new BlockPos(0, 64, 0))));
    }

    @Test
    void noStoneLeftEndsThePass() {
        List<String> skipped = new ArrayList<>();

        assertEquals(Optional.empty(), MinerPicks.<String>firstAccepted(Optional::empty, stone -> true, skipped::add));
        assertEquals(List.of(), skipped);
    }
}
