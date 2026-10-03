package com.villagecolony.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O teto de 500 linhas por arquivo, como função de aptidão — 2026-09-30.
 *
 * <p>A regra está no CLAUDE.md desde o começo e era conferida à mão; em
 * 2026-09-30 havia oito arquivos acima dela. Eles ficam congelados em
 * {@code architecture/oversized-files.txt}: não podem crescer, e arquivo que
 * não está na lista nasce e continua dentro do teto. Quando um congelado
 * desce a 500, a mensagem pede para tirá-lo da lista — a lista só encolhe.
 */
class FileSizeRuleTest {

    private static final int CEILING = 500;

    private static final Path SOURCE_ROOT = Path.of("src", "main", "java");

    @Test
    void noFileGrowsPastTheCeiling() throws IOException {
        Map<String, Integer> frozen = frozen();
        List<String> broken = new ArrayList<>();
        List<String> healed = new ArrayList<>();
        int scanned = 0;

        try (Stream<Path> files = Files.walk(SOURCE_ROOT)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                scanned++;
                String name = SOURCE_ROOT.relativize(file).toString().replace('\\', '/');
                int lines = lineCount(file);
                Integer allowed = frozen.get(name);

                if (allowed == null && lines > CEILING) {
                    broken.add(name + " tem " + lines + " linhas (teto " + CEILING + ")");
                } else if (allowed != null && lines > allowed) {
                    broken.add(name + " cresceu de " + allowed + " para " + lines
                            + " linhas — congelado acima do teto, não pode crescer");
                } else if (allowed != null && lines <= CEILING) {
                    healed.add(name + " voltou a " + lines + " linhas: tire-o de oversized-files.txt");
                }
            }
        }

        assertTrue(scanned > 300, "a varredura achou só " + scanned + " arquivos — o caminho mudou?");
        assertEquals(List.of(), broken, "arquivos acima do teto");
        assertEquals(List.of(), healed, "a lista congelada precisa encolher");
    }

    private static int lineCount(Path file) throws IOException {
        try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
            return (int) lines.count();
        }
    }

    private static Map<String, Integer> frozen() throws IOException {
        Map<String, Integer> frozen = new HashMap<>();

        try (InputStream in = Objects.requireNonNull(
                FileSizeRuleTest.class.getResourceAsStream("/architecture/oversized-files.txt"),
                "architecture/oversized-files.txt")) {

            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.trim().split(" ");
                frozen.put(parts[0], Integer.parseInt(parts[1]));
            }
        }

        return frozen;
    }
}
