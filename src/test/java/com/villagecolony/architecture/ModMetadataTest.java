package com.villagecolony.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guardas para o manifesto que vai dentro do jar publicado.
 */
class ModMetadataTest {

    private static final Path PUBLISHED_MOD_JSON =
            Path.of("src", "main", "resources", "fabric.mod.json");

    private static final Pattern STRING_PROPERTY =
            Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"");

    @Test
    void thePublishedManifestDoesNotUseWildcardDependencies() {
        Map<String, String> dependencies = dependenciesOf(PUBLISHED_MOD_JSON);

        Map<String, String> wildcards = new LinkedHashMap<>();
        dependencies.forEach((name, version) -> {
            if ("*".equals(version)) {
                wildcards.put(name, version);
            }
        });

        assertTrue(
                wildcards.isEmpty(),
                () -> PUBLISHED_MOD_JSON + " aceita qualquer versao em "
                        + wildcards.keySet()
                        + ". Use a matriz de gradle.properties ou um minimo documentado.");
    }

    @Test
    void theFabricApiRequirementComesFromTheVersionMatrix() {
        Map<String, String> dependencies = dependenciesOf(PUBLISHED_MOD_JSON);

        assertEquals(
                ">=${fabric_api_version}",
                dependencies.get("fabric-api"),
                "fabric-api deve vir de gradle.properties, nao de um curinga escrito a mao.");
    }

    private static Map<String, String> dependenciesOf(Path manifest) {
        String source = read(manifest);
        int key = source.indexOf("\"depends\"");

        if (key < 0) {
            return Map.of();
        }

        int start = source.indexOf('{', key);
        int end = matchingBrace(source, start);

        if (start < 0 || end < 0) {
            return Map.of();
        }

        Map<String, String> dependencies = new LinkedHashMap<>();
        Matcher matcher = STRING_PROPERTY.matcher(source.substring(start + 1, end));

        while (matcher.find()) {
            dependencies.put(matcher.group(1), matcher.group(2));
        }

        return dependencies;
    }

    private static int matchingBrace(String source, int start) {
        if (start < 0) {
            return -1;
        }

        int depth = 0;
        boolean quoted = false;
        boolean escaped = false;

        for (int index = start; index < source.length(); index++) {
            char current = source.charAt(index);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (current == '\\') {
                escaped = quoted;
                continue;
            }

            if (current == '"') {
                quoted = !quoted;
                continue;
            }

            if (quoted) {
                continue;
            }

            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;

                if (depth == 0) {
                    return index;
                }
            }
        }

        return -1;
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException cause) {
            throw new UncheckedIOException(cause);
        }
    }
}
