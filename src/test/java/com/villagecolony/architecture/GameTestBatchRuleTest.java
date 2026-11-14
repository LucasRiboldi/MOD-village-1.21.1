package com.villagecolony.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GameTest que mexe no que é do mundo inteiro roda no próprio lote — VF-011 e VF-016 da varredura de 12-11.
 *
 * <p>Os testes de um lote rodam juntos no mesmo mundo. A hora ({@code setTimeOfDay}), a memória estática
 * ({@code clearAll()}, {@code WORKERS.clear}, {@code COLONIES.clear}) e as constantes que o teste encurta
 * ({@code shorten…To}) valem para o lote todo: o {@code clearAll} de um apagava o estado do vizinho no meio.
 * Quem faz qualquer um deles no corpo do método não divide o {@code batchId} com outro teste.
 *
 * <p>Lê só o corpo do método anotado; o que um auxiliar faz por ele não é visto.
 */
class GameTestBatchRuleTest {

    private static final Path GAMETEST_ROOT = Path.of("src", "gametest", "java");

    private static final Pattern TEST = Pattern.compile("@GameTest\\(([^)]*)\\)\\s*public void (\\w+)\\(");

    private static final Pattern BATCH = Pattern.compile("batchId\\s*=\\s*\"([^\"]+)\"");

    private static final Pattern GLOBAL = Pattern.compile(
            "setTimeOfDay|\\.clearAll\\(\\)|shorten\\w+To\\(|WORKERS\\.clear|COLONIES\\.clear");

    private record Case(String name, boolean global) {
    }

    @Test
    void aTestThatTouchesTheWholeWorldRunsInItsOwnBatch() throws IOException {
        Map<String, List<Case>> batches = new LinkedHashMap<>();
        int scanned = 0;

        try (Stream<Path> files = Files.walk(GAMETEST_ROOT)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                String text = Files.readString(file, StandardCharsets.UTF_8);
                String owner = file.getFileName().toString().replace(".java", "");
                Matcher test = TEST.matcher(text);
                List<int[]> spans = new ArrayList<>();
                List<String[]> heads = new ArrayList<>();

                while (test.find()) {
                    spans.add(new int[] {test.end(), 0});
                    Matcher batch = BATCH.matcher(test.group(1));
                    heads.add(new String[] {batch.find() ? batch.group(1) : "defaultBatch", test.group(2)});
                }

                for (int index = 0; index < spans.size(); index++) {
                    int end = index + 1 < spans.size() ? spans.get(index + 1)[0] : text.length();
                    String body = text.substring(spans.get(index)[0], end);
                    String[] head = heads.get(index);

                    batches.computeIfAbsent(head[0], key -> new ArrayList<>())
                            .add(new Case(owner + "." + head[1], GLOBAL.matcher(body).find()));
                    scanned++;
                }
            }
        }

        List<String> shared = new ArrayList<>();

        batches.forEach((batch, cases) -> {
            if (cases.size() > 1) {
                cases.stream().filter(Case::global)
                        .forEach(c -> shared.add(c.name() + " divide o lote \"" + batch + "\" com " + (cases.size() - 1)));
            }
        });

        assertTrue(scanned > 1000, "a varredura achou só " + scanned + " GameTests — o caminho mudou?");
        assertEquals(List.of(), shared, "GameTest que mexe na hora ou na memória do mundo dividindo lote");
    }
}
