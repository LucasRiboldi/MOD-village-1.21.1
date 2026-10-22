package com.villagecolony.core.coordination;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** O vocabulário único de bloqueio — Regra 112, com as frases reais dos diários de 22-10. */
class BlockCodeTest {

    @Test
    void theRealReasonsOfTheDiaryGetTheirCode() {
        Map<String, BlockCode> seen = Map.ofEntries(
                Map.entry("no material", BlockCode.NO_RESOURCE),
                Map.entry("could not reach the crop", BlockCode.NO_PATH),
                Map.entry("it got no closer than N blocks in N ticks of work time", BlockCode.NO_PATH),
                Map.entry("unable to reach N, N, N", BlockCode.NO_PATH),
                Map.entry("it has not moved a block in N ticks of work time", BlockCode.NO_PROGRESS),
                Map.entry("made no progress for N work ticks", BlockCode.NO_PROGRESS),
                Map.entry("frozen twice on the same spot", BlockCode.NO_PROGRESS),
                Map.entry("TOO_HIGH", BlockCode.RESOURCE_INACCESSIBLE),
                Map.entry("NO_STANDING_POSITION", BlockCode.SITE_BLOCKED),
                Map.entry("the next piece has no physical support at 1, 2, 3", BlockCode.UNSUPPORTED_PIECE),
                Map.entry("the chest has no room left", BlockCode.CHEST_FULL),
                Map.entry("the worker has no chest", BlockCode.NO_STORAGE),
                Map.entry("village-original block (oak_stairs)", BlockCode.PROTECTED_BLOCK),
                Map.entry("this game does not have what it asks for", BlockCode.NO_RECIPE),
                Map.entry("no pickaxe in the chest", BlockCode.TOOL_MISSING),
                Map.entry("the stone is reserved by another miner", BlockCode.TARGET_RESERVED),
                Map.entry("chunk not loaded", BlockCode.WORLD_UNAVAILABLE),
                Map.entry("found no lot inside the annexed strip", BlockCode.NO_VALID_SITE));

        seen.forEach((reason, code) -> assertEquals(code, BlockCode.classify(reason), reason));
    }

    @Test
    void theMoreSpecificPhraseWins() {
        assertEquals(BlockCode.SITE_BLOCKED, BlockCode.classify("no room to walk"),
                "'no room to walk' não é baú cheio, embora contenha 'no room'");
        assertEquals(BlockCode.UNSUPPORTED_PIECE, BlockCode.classify("nothing holds it — no room"),
                "a falta de apoio vem antes do baú");
    }

    @Test
    void anUnknownReasonIsUnclassifiedAndCaseDoesNotMatter() {
        assertEquals(BlockCode.UNCLASSIFIED, BlockCode.classify("a reason nobody wrote yet"));
        assertEquals(BlockCode.UNCLASSIFIED, BlockCode.classify(""));
        assertEquals(BlockCode.NO_RESOURCE, BlockCode.classify("NO MATERIAL"));
    }
}
