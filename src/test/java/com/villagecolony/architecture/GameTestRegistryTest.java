package com.villagecolony.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Toda classe com {@code @GameTest} está no registro, e todo registro aponta
 * para uma classe que existe — ADR-035 §7.
 *
 * <p>O Fabric só roda as classes listadas em {@code fabric-gametest}. Uma
 * classe esquecida some da bateria em silêncio e a contagem continua verde: foi
 * o que aconteceu com {@code MineOverflowStorageGameTest} até 2026-10-06.
 */
class GameTestRegistryTest {

    private static final Path SOURCES = Path.of("src", "gametest", "java");

    private static final Path MANIFEST = Path.of("src", "gametest", "resources", "fabric.mod.json");

    private static final Pattern ANNOTATION = Pattern.compile("^\\s*@GameTest\\b", Pattern.MULTILINE);

    private static final Pattern ENTRY = Pattern.compile("\"(com\\.villagecolony\\.[A-Za-z0-9_.]+)\"");

    @Test
    void everyGameTestClassIsRegisteredAndEveryEntryExists() {
        Set<String> declared = declaredClasses();
        Set<String> registered = registeredClasses();

        Set<String> missing = new TreeSet<>(declared);
        missing.removeAll(registered);
        Set<String> dangling = new TreeSet<>(registered);
        dangling.removeAll(declared);

        assertTrue(missing.isEmpty(),
                () -> "Classes com @GameTest fora de " + MANIFEST + " (nunca rodam): " + missing);
        assertTrue(dangling.isEmpty(),
                () -> MANIFEST + " registra classes sem @GameTest ou inexistentes: " + dangling);
    }

    private static Set<String> declaredClasses() {
        Set<String> classes = new TreeSet<>();

        try (Stream<Path> files = Files.walk(SOURCES)) {
            files.filter(file -> file.toString().endsWith(".java"))
                    .filter(file -> ANNOTATION.matcher(read(file)).find())
                    .forEach(file -> classes.add(classNameOf(file)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return classes;
    }

    private static Set<String> registeredClasses() {
        String manifest = read(MANIFEST);
        int key = manifest.indexOf("\"fabric-gametest\"");
        int start = manifest.indexOf('[', key);
        int end = manifest.indexOf(']', start);

        Set<String> classes = new TreeSet<>();
        Matcher matcher = ENTRY.matcher(manifest.substring(start + 1, end));

        while (matcher.find()) {
            classes.add(matcher.group(1));
        }

        return classes;
    }

    private static String classNameOf(Path file) {
        String relative = SOURCES.relativize(file).toString().replace('\\', '/');

        return relative.substring(0, relative.length() - ".java".length()).replace('/', '.');
    }

    private static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
