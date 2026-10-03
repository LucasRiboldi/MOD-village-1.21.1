package com.villagecolony.core.type;

import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Toda classe que guarda memória de servidor se inscreve no {@link ServerMemory}
 * — item 3 da avaliação técnica, 2026-09-24.
 *
 * <p>Percorre as classes compiladas do mod. Quem declara {@code static void
 * clearAll()} é carregado, e o teste confere que o bloco estático dele se
 * inscreveu. Foi assim que o {@code BiomeConstructionSupply} escapou antes: o
 * {@code clearAll()} existia e ninguém o chamava.
 */
class ServerMemoryRegistrationTest {

    @BeforeAll
    static void bootMinecraft() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
    }

    @Test
    void everyClassWithServerMemoryRegistersItself() throws IOException, ClassNotFoundException {
        Path classes = Path.of("build", "classes", "java", "main");
        assertTrue(Files.isDirectory(classes), "classes do mod não encontradas em " + classes.toAbsolutePath());

        List<String> missing = new ArrayList<>();
        int checked = 0;

        try (Stream<Path> files = Files.walk(classes)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".class") && !f.toString().contains("$")).toList()) {
                String name = classes.relativize(file).toString()
                        .replace(java.io.File.separatorChar, '.').replace(".class", "");

                // Classe de mixin não se carrega direto: ela só existe aplicada
                // no alvo, e o carregador do Fabric recusa a carga.
                if (name.contains(".mixin.")) {
                    continue;
                }
                Class<?> type = Class.forName(name, false, getClass().getClassLoader());

                if (!declaresClearAll(type)) {
                    continue;
                }

                checked++;
                Class.forName(name, true, getClass().getClassLoader());

                if (!ServerMemory.registered().contains(name)) {
                    missing.add(name);
                }
            }
        }

        assertTrue(checked >= 50, "o teste achou só " + checked + " classes com clearAll — o caminho mudou?");
        assertEquals(List.of(), missing, "classes com clearAll() que não se inscrevem no ServerMemory");
    }

    private static boolean declaresClearAll(Class<?> type) {
        try {
            Method method = type.getDeclaredMethod("clearAll");
            return Modifier.isStatic(method.getModifiers()) && method.getParameterCount() == 0;
        } catch (NoSuchMethodException | NoClassDefFoundError e) {
            return false;
        }
    }
}
