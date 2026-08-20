package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ResourceId;

import java.util.Objects;
import java.util.Optional;

/**
 * De que uma vila é feita, e o que o bioma dela dá.
 *
 * <p>Decidido em 2026-08-20. A Regra 20 já dizia que cada vila constrói
 * no estilo do seu bioma, mas o estilo era <b>uma coisa só</b>: a
 * espécie da madeira. Isso bastava enquanto a colônia só sabia derrubar
 * árvore, e deixava o deserto de fora — a vila nascia, contratava, e não
 * construía nunca, porque não há árvore ali.
 *
 * <p>A paleta é a Regra 20 dita por inteiro: <b>parede, porta, e as três
 * matérias que os trabalhadores novos produzem</b>. Cada bioma responde
 * com o que ele tem, e a colônia constrói com isso.
 *
 * <pre>
 * estilo      parede            porta        pedra        vidro   lã
 * carvalho    oak_planks        oak_door     cobblestone  glass   white_wool
 * pinheiro    spruce_planks     spruce_door  cobblestone  glass   white_wool
 * acácia      acacia_planks     acacia_door  cobblestone  glass   white_wool
 * arenito     sandstone         —            sandstone    glass   white_wool
 * </pre>
 *
 * <p><b>O deserto não tem porta, e isso é decisão.</b> A porta sai de
 * tábua, tábua sai de tronco, e o deserto não tem tronco. Uma casa que
 * exigisse porta ali ficaria em {@code WAITING_RESOURCES} até o jogador
 * trazer uma — que é exatamente o travamento que a Regra 13 corrigiu. A
 * cabana do deserto tem o vão, e quem quiser pendura a porta.
 *
 * <p>Mora em {@code core} e não conhece Minecraft: é uma tabela de
 * decisões do autor, e tabela de decisão se afirma sem subir servidor.
 */
public record VillagePalette(
        String style,
        ResourceId wall,
        Optional<ResourceId> door,
        ResourceId stone,
        ResourceId glass,
        ResourceId wool) {

    private static ResourceId vanilla(String path) {
        return new ResourceId(ResourceId.VANILLA, path);
    }

    /** O pedregulho, que é a pedra de toda vila que não é de deserto. */
    public static final ResourceId COBBLESTONE = vanilla("cobblestone");

    /** O arenito, que é a pedra <b>e</b> a parede do deserto. */
    public static final ResourceId SANDSTONE = vanilla("sandstone");

    /** A areia, de onde o fundidor tira o vidro. */
    public static final ResourceId SAND = vanilla("sand");

    public static final ResourceId GLASS = vanilla("glass");

    public static final ResourceId WOOL = vanilla("white_wool");

    public VillagePalette {
        Objects.requireNonNull(style, "style");
        Objects.requireNonNull(wall, "wall");
        Objects.requireNonNull(door, "door");
        Objects.requireNonNull(stone, "stone");
        Objects.requireNonNull(glass, "glass");
        Objects.requireNonNull(wool, "wool");
    }

    /**
     * A paleta de uma vila de madeira, qualquer que seja a espécie.
     *
     * <p>A porta sai da própria espécie: {@code spruce_planks} vira
     * {@code spruce_door}. É convenção do jogo e não do mod, e vale para
     * as nove madeiras que existem — o que a torna mais confiável que
     * uma tabela escrita aqui, que envelheceria na próxima madeira que o
     * jogo acrescentasse.
     */
    public static VillagePalette ofWood(String style, ResourceId planks) {
        Objects.requireNonNull(planks, "planks");

        ResourceId door = new ResourceId(
                planks.namespace(), planks.path().replace("_planks", "_door"));

        return new VillagePalette(style, planks, Optional.of(door), COBBLESTONE, GLASS, WOOL);
    }

    /**
     * A paleta do deserto: arenito, e sem porta.
     *
     * <p>A pedra <b>é</b> a parede aqui, e é o que faz a vila de deserto
     * finalmente construir: o mineiro tira arenito da duna ao lado, e a
     * casa sobe sem depender de uma árvore que não existe.
     */
    public static VillagePalette ofSandstone() {
        return new VillagePalette(
                "desert", SANDSTONE, Optional.empty(), SANDSTONE, GLASS, WOOL);
    }

    /**
     * O nome da pasta deste estilo no catálogo de estruturas.
     *
     * <p>É o elo com a Regra 27: as estruturas que o construtor pode
     * levantar são as de {@code village/<estilo>/houses/}, e nenhuma
     * outra. O estilo mora aqui porque já é ele quem responde "que tipo
     * de vila é esta".
     */
    public String style() {
        return style;
    }

    /** Se a colônia deste estilo sabe fazer porta. */
    public boolean hasDoor() {
        return door.isPresent();
    }
}
