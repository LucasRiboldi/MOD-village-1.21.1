package com.villagecolony.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A fronteira de conversão da ADR-005 §4, como teste.
 *
 * <p>A ADR diz que a conversão {@code BlockPos <-> ColonyPos} acontece
 * <b>apenas</b> na fronteira, em {@code fabric.adapter.MinecraftTypeAdapter}.
 * O Javadoc do {@code MinecraftTypeAdapter} repete a promessa em uma
 * frase: <i>"nenhuma conversão acontece fora daqui"</i>. Até agora nada
 * a verificava — e ela já era falsa: {@code MinerReach} tinha um
 * {@code at(ColonyPos)} privado que refazia o {@code toBlockPos} inteiro.
 *
 * <p><b>O que é conversão e o que não é.</b> Chamar
 * {@code MinecraftTypeAdapter.toBlockPos(p)} de dentro de
 * {@code fabric.work} ou {@code fabric.integration} <b>não</b> é violar
 * a regra — é usar a fronteira, que é para isso que ela existe. Vinte e
 * três arquivos fazem isso e todos estão certos. O que a regra proíbe é
 * uma <b>segunda implementação</b>: montar um dos tipos a partir dos
 * três acessores do outro, que é copiar o adaptador para dentro de casa.
 *
 * <p><b>Posição derivada também não é conversão.</b>
 * {@code new BlockPos(origin.x() + dx, origin.y() + dy, origin.z() + dz)}
 * é aritmética de deslocamento: não há {@code toBlockPos} que a
 * substitua, e proibi-la só faria o autor escrever a mesma conta de um
 * jeito pior. Por isso a regra exige os três acessores <b>puros</b> e do
 * <b>mesmo</b> receptor — o formato exato que o adaptador já resolve.
 *
 * <p>Lê o código-fonte, e não as classes compiladas, pela mesma razão de
 * {@link DependencyRuleTest}: a regra é sobre o que o autor escreve, e o
 * bytecode já apagou a diferença entre chamar o adaptador e refazê-lo.
 */
class ConversionBoundaryTest {

    private static final Path SOURCE_ROOT = Path.of("src", "main", "java");

    /** A fronteira. O único lugar onde a conversão pode existir. */
    private static final Path ADAPTER_PACKAGE =
            Path.of("com", "villagecolony", "fabric", "adapter");

    /**
     * {@code new BlockPos(p.x(), p.y(), p.z())} — o {@code toBlockPos}
     * refeito à mão.
     *
     * <p>O grupo 1 captura o receptor e a retrovisão {@code \1} exige que
     * os três acessores sejam do mesmo objeto. Sem isso,
     * {@code new BlockPos(a.x(), b.y(), c.z())} — que é composição de
     * três posições, e não conversão de uma — cairia aqui dentro.
     */
    private static final Pattern HAND_MADE_BLOCK_POS = Pattern.compile(
            "new\\s+BlockPos\\s*\\(\\s*([\\w.()\\[\\] ]+?)\\.x\\(\\)\\s*,"
                    + "\\s*\\1\\.y\\(\\)\\s*,\\s*\\1\\.z\\(\\)\\s*\\)");

    /** {@code new ColonyPos(p.getX(), p.getY(), p.getZ())} — o caminho de volta. */
    private static final Pattern HAND_MADE_COLONY_POS = Pattern.compile(
            "new\\s+ColonyPos\\s*\\(\\s*([\\w.()\\[\\] ]+?)\\.getX\\(\\)\\s*,"
                    + "\\s*\\1\\.getY\\(\\)\\s*,\\s*\\1\\.getZ\\(\\)\\s*\\)");

    /** Ninguém refaz a conversão fora da fronteira. */
    @Test
    void theConversionHappensOnlyAtTheBoundary() {
        List<String> offenders = new ArrayList<>();

        for (Path file : sourceFilesOutsideTheAdapter()) {
            String source = normalized(file);
            String relative = relative(file);

            record(offenders, relative, source, HAND_MADE_BLOCK_POS,
                    "refaz MinecraftTypeAdapter.toBlockPos");

            record(offenders, relative, source, HAND_MADE_COLONY_POS,
                    "refaz MinecraftTypeAdapter.toColonyPos");
        }

        assertTrue(
                offenders.isEmpty(),
                () -> message("a conversão acontece apenas em fabric.adapter", offenders));
    }

    /**
     * O detector ainda reconhece uma conversão quando vê uma.
     *
     * <p>Controle positivo, e ele não é zelo: a regra acima é uma busca
     * que passa quando não acha nada. No dia em que o adaptador for
     * reescrito de outra forma — {@code BlockPos.ofFloored}, um
     * construtor novo do jogo — os dois padrões param de casar, o teste
     * de cima fica verde para sempre, e ninguém percebe que ele deixou de
     * olhar. Aqui ele tem de achar, e é o próprio adaptador que prova.
     */
    @Test
    void theAdapterStillHoldsTheConversion() {
        Path adapter = SOURCE_ROOT.resolve(ADAPTER_PACKAGE).resolve("MinecraftTypeAdapter.java");

        assertTrue(Files.isRegularFile(adapter), () -> adapter + " não existe. A ADR-005 §4 nomeia"
                + " esta classe como a fronteira; se ela mudou de lugar, esta regra mudou junto.");

        String source = normalized(adapter);

        assertTrue(
                HAND_MADE_BLOCK_POS.matcher(source).find(),
                "O adaptador não contém mais o formato que este teste procura para toBlockPos."
                        + " O detector ficou cego: reveja HAND_MADE_BLOCK_POS.");

        assertTrue(
                HAND_MADE_COLONY_POS.matcher(source).find(),
                "O adaptador não contém mais o formato que este teste procura para toColonyPos."
                        + " O detector ficou cego: reveja HAND_MADE_COLONY_POS.");
    }

    /** Confere que a varredura de fato encontrou código para analisar. */
    @Test
    void theScanReachesTheSource() {
        List<Path> files = sourceFilesOutsideTheAdapter();

        assertTrue(
                files.size() > 10,
                "A varredura encontrou " + files.size() + " arquivos em " + SOURCE_ROOT.toAbsolutePath()
                        + ". Um teste que não lê nada passa sempre.");
    }

    // ----------------------------------------------------------------

    private static void record(
            List<String> offenders, String file, String source, Pattern pattern, String what) {

        Matcher matcher = pattern.matcher(source);

        while (matcher.find()) {
            offenders.add(file + " " + what + ": " + matcher.group());
        }
    }

    private static List<Path> sourceFilesOutsideTheAdapter() {
        if (!Files.isDirectory(SOURCE_ROOT)) {
            return List.of();
        }

        Path adapter = SOURCE_ROOT.resolve(ADAPTER_PACKAGE);

        try (Stream<Path> walk = Files.walk(SOURCE_ROOT)) {
            return walk
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.startsWith(adapter))
                    .toList();
        } catch (IOException cause) {
            throw new UncheckedIOException(cause);
        }
    }

    /**
     * O arquivo em uma linha só.
     *
     * <p>A chamada quebrada em duas linhas é conversão igual à que cabe
     * numa — e o autor quebra justamente as longas. Sem achatar, a regra
     * pegaria só as curtas, que é o mesmo que não pegar.
     */
    private static String normalized(Path file) {
        try {
            return Files.readString(file).replaceAll("\\s+", " ");
        } catch (IOException cause) {
            throw new UncheckedIOException(cause);
        }
    }

    private static String relative(Path file) {
        return SOURCE_ROOT.relativize(file).toString().replace('\\', '/');
    }

    private static String message(String rule, List<String> offenders) {
        return rule + ", e " + offenders.size() + " lugar(es) quebram isso:"
                + System.lineSeparator() + "  "
                + String.join(System.lineSeparator() + "  ", offenders)
                + System.lineSeparator()
                + "Use MinecraftTypeAdapter.toBlockPos / toColonyPos. Ver ADR-005 §4.";
    }
}
