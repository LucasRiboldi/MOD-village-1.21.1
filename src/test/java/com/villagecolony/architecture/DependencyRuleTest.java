package com.villagecolony.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A regra de dependência da ADR-006 §6, como teste.
 *
 * <p>Até aqui ela era conferida por {@code grep}, à mão. Isso não é
 * guarda-corpo: some no dia em que alguém esquecer de rodar, e a regra
 * já cobrou uma vez — foi ela que obrigou a mover {@code Capability} e
 * {@code ResourceType} para {@code core/type} na Fase 7.
 *
 * <p>Lê o código-fonte, e não as classes compiladas, de propósito. A
 * regra é sobre {@code import}, que é o que o autor escreve e o que a
 * ADR proíbe; o bytecode já perdeu a diferença entre um import e um
 * nome totalmente qualificado, e apagou os imports não usados que a
 * regra também quer barrar.
 */
class DependencyRuleTest {

    private static final Path SOURCE_ROOT = Path.of("src", "main", "java");

    private static final String BASE = "com.villagecolony.";

    /**
     * O pacote de tipos partilhados, que qualquer domínio pode importar.
     *
     * <p>Existe justamente para ser a saída legítima quando dois
     * domínios precisam falar do mesmo valor. Ver ADR-006 §5.
     */
    private static final String SHARED = "type";

    /** O core não conhece Minecraft. É o que o torna testável. */
    @Test
    void coreDoesNotImportMinecraft() {
        List<String> offenders = new ArrayList<>();

        forEachCoreImport((file, imported) -> {
            if (imported.startsWith("net.minecraft.")
                    || imported.startsWith("net.fabricmc.")) {

                offenders.add(file + " importa " + imported);
            }
        });

        assertTrue(offenders.isEmpty(), () -> message("core não importa Minecraft", offenders));
    }

    /** O core não conhece as camadas que dependem dele. */
    @Test
    void coreDoesNotImportOuterLayers() {
        List<String> offenders = new ArrayList<>();

        forEachCoreImport((file, imported) -> {
            if (imported.startsWith(BASE + "fabric.") || imported.startsWith(BASE + "data.")) {
                offenders.add(file + " importa " + imported);
            }
        });

        assertTrue(offenders.isEmpty(), () -> message("core não importa fabric nem data", offenders));
    }

    /**
     * Um domínio do core não importa outro diretamente.
     *
     * <p>A regra adicional da ADR-006 §6. A saída, quando dois domínios
     * precisam do mesmo valor, é {@code core/type} — e é por isso que
     * ele está isento aqui.
     */
    @Test
    void coreDomainsDoNotImportEachOther() {
        List<String> offenders = new ArrayList<>();

        forEachCoreImport((file, imported) -> {
            String owner = coreDomainOf(packageOfFile(file));
            String target = coreDomainOf(imported);

            if (owner == null || target == null) {
                return;
            }

            if (SHARED.equals(target) || owner.equals(target)) {
                return;
            }

            offenders.add(file + " (" + owner + ") importa " + imported);
        });

        assertTrue(
                offenders.isEmpty(),
                () -> message("nenhum domínio do core importa outro", offenders));
    }

    /** Confere que a varredura de fato encontrou código para analisar. */
    @Test
    void theScanReachesTheSource() {
        List<Path> files = coreFiles();

        assertTrue(
                files.size() > 10,
                "A varredura encontrou " + files.size() + " arquivos em " + SOURCE_ROOT.toAbsolutePath()
                        + ". Um teste que não lê nada passa sempre.");
    }

    // ----------------------------------------------------------------

    private interface ImportVisitor {
        void visit(String file, String imported);
    }

    private void forEachCoreImport(ImportVisitor visitor) {
        for (Path file : coreFiles()) {
            String relative = SOURCE_ROOT.relativize(file).toString().replace('\\', '/');

            for (String imported : importsOf(file)) {
                visitor.visit(relative, imported);
            }
        }
    }

    private static List<Path> coreFiles() {
        Path core = SOURCE_ROOT.resolve(Path.of("com", "villagecolony", "core"));

        if (!Files.isDirectory(core)) {
            return List.of();
        }

        try (Stream<Path> walk = Files.walk(core)) {
            return walk.filter(path -> path.toString().endsWith(".java")).toList();
        } catch (IOException cause) {
            throw new UncheckedIOException(cause);
        }
    }

    private static List<String> importsOf(Path file) {
        List<String> imports = new ArrayList<>();

        try {
            for (String line : Files.readAllLines(file)) {
                String trimmed = line.strip();

                if (trimmed.startsWith("package ") || trimmed.isEmpty()) {
                    continue;
                }

                if (!trimmed.startsWith("import ")) {
                    // Passou dos imports: o resto do arquivo não interessa.
                    if (trimmed.startsWith("//") || trimmed.startsWith("*")
                            || trimmed.startsWith("/*")) {
                        continue;
                    }

                    break;
                }

                imports.add(trimmed
                        .substring("import ".length())
                        .replaceFirst("^static ", "")
                        .replace(";", "")
                        .strip());
            }
        } catch (IOException cause) {
            throw new UncheckedIOException(cause);
        }

        return imports;
    }

    /** {@code core/worker/model/Worker.java} vira {@code ...core.worker.model.Worker}. */
    private static String packageOfFile(String relativePath) {
        return relativePath.replace(".java", "").replace('/', '.');
    }

    /**
     * O domínio de um nome do core, ou {@code null} se ele não for do
     * core.
     *
     * <p>{@code com.villagecolony.core.worker.model.Worker} → {@code worker}.
     */
    private static String coreDomainOf(String qualifiedName) {
        String corePrefix = BASE + "core.";

        if (!qualifiedName.startsWith(corePrefix)) {
            return null;
        }

        String rest = qualifiedName.substring(corePrefix.length());
        int dot = rest.indexOf('.');

        return dot < 0 ? null : rest.substring(0, dot);
    }

    private static String message(String rule, List<String> offenders) {
        return "ADR-006 §6 — " + rule + ". Violações:\n  " + String.join("\n  ", offenders);
    }
}
