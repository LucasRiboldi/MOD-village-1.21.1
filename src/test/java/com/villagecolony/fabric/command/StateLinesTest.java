package com.villagecolony.fabric.command;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** O retrato da vila em linhas para o chat — o mesmo registro do arquivo de análise (autor, 2026-10-28). */
class StateLinesTest {

    private static final String STATE = """
            {"schema":1,"day":12,"daytime":6000,"mspt":18.4,
             "people":{"villagers":18,"employed":12,"beds":17,"stranded":1,"trades":{"MASON":2,"NONE":6},
                       "badges":{"DUTY":5,"STUCK":1},"stuck":["eed83c53:MINER"]},
             "stock":{"chests":50,"fill":83,"needsStorage":true,"WOOD":413,"STONE":1202},
             "build":{"buildings":21,"lotsOk":143,"open":"savanna_small_house_5","state":"BUILDING","left":160,"deferred":2},
             "tasks":{"open":{"BUILD":1},"held":1},
             "mine":{"entryY":40,"levels":2,"cut":900},
             "village":{"roads":778,"benches":["MINER"],"animals":{"sheep":6}}}
            """;

    @Test
    void everySubjectHasItsLine() {
        List<String> lines = StateLines.of(JsonParser.parseString(STATE).getAsJsonObject());
        String all = String.join("\n", lines);

        assertTrue(all.contains("Dia 12 (12:00)"), all);
        assertTrue(all.contains("Aldeões 18 (12 com ofício)"), all);
        assertTrue(all.contains("1 travados — travados: [\"eed83c53:MINER\"]"), all);
        assertTrue(all.contains("83% cheios — pede armazém"), all);
        assertTrue(all.contains("wood 413"), all);
        assertTrue(all.contains("savanna_small_house_5 (BUILDING, faltam 160, de lado 2)"), all);
        assertTrue(all.contains("Mina: nível em y=40, 2 nível(is)"), all);
        assertTrue(all.contains("bancadas: [MINER]"), all);
    }

    @Test
    void anEmptyStateStillPrintsWithoutFailing() {
        List<String> lines = StateLines.of(JsonParser.parseString("{}").getAsJsonObject());

        assertTrue(lines.stream().anyMatch(line -> line.equals("Mina: nenhuma")));
    }

    @Test
    void theClockStartsAtSixInTheMorning() {
        assertEquals("06:00", StateLines.clock(0));
        assertEquals("18:30", StateLines.clock(12_500));
    }
}
