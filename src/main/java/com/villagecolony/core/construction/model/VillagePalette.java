package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ResourceId;

import java.util.Objects;

/**
 * O que o bioma de uma vila dá, e como a pasta dele se chama.
 *
 * <p>Decidido em 2026-08-20. A Regra 20 já dizia que cada vila constrói
 * no estilo do seu bioma, mas o estilo era <b>uma coisa só</b>: a
 * espécie da madeira. Isso bastava enquanto a colônia só sabia derrubar
 * árvore, e deixava o deserto de fora — a vila nascia, contratava, e não
 * construía nunca, porque não há árvore ali.
 *
 * <pre>
 * estilo      pasta do catálogo     pedra        vidro
 * carvalho    village/plains/       cobblestone  glass
 * pinheiro    village/taiga/        cobblestone  glass
 * acácia      village/savanna/      cobblestone  glass
 * nevado      village/snowy/        cobblestone  glass
 * arenito     village/desert/       sandstone    glass
 * </pre>
 *
 * <p><b>Encolheu em 2026-08-21, com a cabana.</b> Ela carregava também a
 * <b>parede</b> e a <b>porta</b> de cada estilo, e havia um só leitor
 * para as duas: {@code ColonyHut}, a casa que o mod desenhava em código.
 * A Regra 27 aposentou essa casa, e a Regra 28 tornou a paleta de
 * construção inútil — o que levanta parede hoje é o arquivo de
 * estrutura do jogo, que traz os próprios blocos.
 *
 * <p>O que ficou é o que ainda tem quem pergunte: o <b>estilo</b>, que é
 * a pasta do catálogo e o elo com a Regra 27; a <b>pedra</b>, que o
 * mineiro procura e que no deserto é arenito; e o <b>vidro</b>, que o
 * fundidor faz.
 *
 * <p>Mora em {@code core} e não conhece Minecraft: é uma tabela de
 * decisões do autor, e tabela de decisão se afirma sem subir servidor.
 */
public record VillagePalette(String style, ResourceId stone, ResourceId glass) {

    private static ResourceId vanilla(String path) {
        return new ResourceId(ResourceId.VANILLA, path);
    }

    /** O pedregulho, que é a pedra de toda vila que não é de deserto. */
    public static final ResourceId COBBLESTONE = vanilla("cobblestone");

    /** O arenito, que é a pedra do deserto. */
    public static final ResourceId SANDSTONE = vanilla("sandstone");

    /** A areia, de onde o fundidor tira o vidro. */
    public static final ResourceId SAND = vanilla("sand");

    /**
     * O calçamento de quase toda vila do jogo.
     *
     * <p>Está aqui como <b>último recurso</b>, e não como decisão: quem
     * diz de que bloco é a rua de cada estilo é o catálogo do jogo — ver
     * {@code VillageRoad}. Este nome só entra em cena se o catálogo não
     * puder ser lido, e a alternativa seria a colônia deixar de
     * reconhecer rua nenhuma e parar de crescer em silêncio.
     */
    public static final ResourceId DIRT_PATH = vanilla("dirt_path");

    public static final ResourceId GLASS = vanilla("glass");

    public VillagePalette {
        Objects.requireNonNull(style, "style");
        Objects.requireNonNull(stone, "stone");
        Objects.requireNonNull(glass, "glass");
    }

    /**
     * A paleta de uma vila de madeira, qualquer que seja a espécie.
     *
     * <p>A espécie deixou de entrar na conta em 2026-08-21: ela servia
     * para a parede e para a porta da cabana, e a cabana saiu. O que a
     * madeira ainda decide é <b>se o mod atende este bioma</b>, e essa
     * pergunta é de {@code VillageBiomes}, não daqui.
     */
    public static VillagePalette ofWood(String style) {
        return new VillagePalette(style, COBBLESTONE, GLASS);
    }

    /**
     * A paleta do deserto: a pedra é arenito.
     *
     * <p>É o que faz a vila de deserto finalmente construir — o mineiro
     * tira arenito da duna ao lado, e a casa sobe sem depender de uma
     * árvore que não existe.
     */
    public static VillagePalette ofSandstone() {
        return new VillagePalette("desert", SANDSTONE, GLASS);
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
}
