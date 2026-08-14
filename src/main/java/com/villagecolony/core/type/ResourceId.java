package com.villagecolony.core.type;

import java.util.Objects;

/**
 * O nome de uma coisa do jogo, sem depender do jogo.
 *
 * <p>Substitui {@code Identifier} dentro do Core, conforme ADR-005. A
 * conversão para o tipo do Minecraft acontece só na fronteira, em
 * {@code fabric.adapter.MinecraftTypeAdapter}.
 *
 * <p>Estava previsto desde 2026-08-06 e ficou sem uso até a Fase 10:
 * até aqui o Core falava de recurso por {@link ResourceType}, que é uma
 * lista curta do que a colônia conta. Um projeto de construção fala de
 * porta, vidraça e degrau — coisas que a colônia não conta e precisa
 * saber nomear.
 *
 * <p>Não valida o que o jogo aceita como nome. Um id que não existe no
 * registro é descoberto na fronteira, ao converter, e não aqui: o Core
 * não tem como saber o que o jogo tem instalado.
 *
 * @param namespace de quem é o nome — {@code minecraft} para o Vanilla,
 *     {@code villagecolony} para o que for deste mod
 * @param path o nome dentro do namespace, como {@code oak_planks}
 */
public record ResourceId(String namespace, String path) {

    /** O namespace do jogo, que é a origem de tudo o que o MVP usa. */
    public static final String VANILLA = "minecraft";

    public ResourceId {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(path, "path");

        if (namespace.isBlank()) {
            throw new IllegalArgumentException("namespace must not be blank");
        }

        if (path.isBlank()) {
            throw new IllegalArgumentException("path must not be blank");
        }
    }

    /** Um nome do Vanilla, como {@code oak_planks}. */
    public static ResourceId vanilla(String path) {
        return new ResourceId(VANILLA, path);
    }

    /**
     * Lê a forma escrita, {@code namespace:path}.
     *
     * <p>Sem namespace vale {@link #VANILLA}, que é a mesma regra do
     * jogo — {@code oak_planks} e {@code minecraft:oak_planks} são o
     * mesmo bloco.
     *
     * @throws IllegalArgumentException se houver mais de um dois-pontos;
     *     um nome ambíguo é erro de quem escreveu, e adivinhar qual das
     *     partes é o quê esconderia isso
     */
    public static ResourceId parse(String text) {
        Objects.requireNonNull(text, "text");

        int separator = text.indexOf(':');

        if (separator < 0) {
            return vanilla(text);
        }

        if (text.indexOf(':', separator + 1) >= 0) {
            throw new IllegalArgumentException("Ambiguous resource id: " + text);
        }

        return new ResourceId(text.substring(0, separator), text.substring(separator + 1));
    }

    /** A forma escrita, do jeito que o jogo a mostra. */
    @Override
    public String toString() {
        return namespace + ":" + path;
    }
}
