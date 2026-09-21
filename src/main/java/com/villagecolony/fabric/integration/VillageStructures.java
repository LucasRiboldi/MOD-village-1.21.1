package com.villagecolony.fabric.integration;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ResourceId;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * O que o construtor <b>pode</b> levantar, por tipo de vila — a Regra 27.
 *
 * <p><b>A regra é imutável, e o autor a disse assim:</b> as estruturas
 * que o construtor de cada bioma pode construir são as da pasta de
 * estruturas do jogo, e nenhuma outra. O mod não inventa casa.
 *
 * <p>Isso desfaz uma decisão anterior. A Regra 13 tinha criado a cabana
 * do mod — escrita em código, cinco por cinco — porque a casa do jogo
 * era impossível de levantar com o que a colônia produzia. A resposta
 * agora é outra: se a casa do catálogo pede pedra, a colônia aprende a
 * minerar. Foi o que 2026-08-20 fez.
 *
 * <p>A lista sai de {@code data/villagecolony/catalog/vanilla_structures.json},
 * que é o índice da pasta e entrou no repositório em 08-19 — só os
 * nomes, nenhum byte de arquivo da Mojang. Os arquivos em si o jogo já
 * traz, e {@link StructureBlueprintReader} os lê por id.
 *
 * <p>Desde 2026-09-20 a lista não é mais o catálogo inteiro: cada bioma
 * tem uma whitelist explícita de moradias, oficinas, agricultura e uma
 * peça decorativa. Os nomes abaixo são os ids reais do jogo; os pedidos
 * abreviados foram corrigidos, por exemplo {@code desert_fletcher_house_1}
 * e {@code snowy_fisher_cottage}.
 *
 * <p><b>As variantes zumbi ficam de fora</b>, e não por gosto: são as
 * mesmas casas em ruína, com teia e tocha apagada. Uma colônia que as
 * levantasse estaria construindo a própria decadência.
 *
 * <p><b>O catálogo inteiro vale desde 2026-09-09</b>, por decisão do
 * autor: <i>"permitir que o construtor possa construir casa grande e
 * média, se houver espaço para elas"</i>. Até essa data uma linha em
 * {@code load} descartava tudo o que não terminasse em
 * {@code _small_house_1} — a barreira provisória de 08-20, que existia
 * para uma sessão de depuração ser comparável com a anterior. Ela
 * cumpriu o prazo: as cadeias fecharam, e a vila de uma casa só era o
 * que restava dela.
 */
public final class VillageStructures {

    /** Onde o índice da pasta mora dentro do jar. */
    private static final String CATALOG =
            "/data/villagecolony/catalog/vanilla_structures.json";

    /** Moradias e infraestrutura da pasta houses, lidas uma vez por sessão. */
    private static final Map<String, List<ResourceId>> HOUSES = new HashMap<>();

    /** Todas as peças permitidas, incluindo lâmpadas fora de houses. */
    private static final Map<String, List<ResourceId>> BUILDABLE = new HashMap<>();

    /** As ruas, pela mesma porta e no mesmo catálogo — 2026-08-21. */
    private static final Map<String, List<ResourceId>> STREETS = new HashMap<>();

    /** As roças, pela mesma porta — 2026-09-05. */
    private static final Map<String, List<ResourceId>> FARMS = new HashMap<>();

    /**
     * Contrato fechado das construções do mod. A ordem é deliberada: ela
     * estabiliza a escolha das variantes quando duas plantas têm a mesma
     * pegada.
     */
    private static final Map<String, List<String>> ALLOWED = Map.of(
            "plains", List.of(
                    "village/plains/houses/plains_small_house_1",
                    "village/plains/houses/plains_small_house_2",
                    "village/plains/houses/plains_small_house_3",
                    "village/plains/houses/plains_small_house_4",
                    "village/plains/houses/plains_small_house_5",
                    "village/plains/houses/plains_small_house_6",
                    "village/plains/houses/plains_small_house_7",
                    "village/plains/houses/plains_small_house_8",
                    "village/plains/houses/plains_medium_house_1",
                    "village/plains/houses/plains_medium_house_2",
                    "village/plains/houses/plains_big_house_1",
                    "village/plains/houses/plains_armorer_house_1",
                    "village/plains/houses/plains_butcher_shop_1",
                    "village/plains/houses/plains_butcher_shop_2",
                    "village/plains/houses/plains_cartographer_1",
                    "village/plains/houses/plains_fisher_cottage_1",
                    "village/plains/houses/plains_fletcher_house_1",
                    "village/plains/houses/plains_library_1",
                    "village/plains/houses/plains_library_2",
                    "village/plains/houses/plains_masons_house_1",
                    "village/plains/houses/plains_shepherds_house_1",
                    "village/plains/houses/plains_tannery_1",
                    "village/plains/houses/plains_temple_3",
                    "village/plains/houses/plains_temple_4",
                    "village/plains/houses/plains_tool_smith_1",
                    "village/plains/houses/plains_weaponsmith_1",
                    "village/plains/houses/plains_small_farm_1",
                    "village/plains/houses/plains_large_farm_1",
                    "village/plains/houses/plains_animal_pen_1",
                    "village/plains/houses/plains_animal_pen_2",
                    "village/plains/houses/plains_animal_pen_3",
                    "village/plains/houses/plains_stable_1",
                    "village/plains/houses/plains_stable_2",
                    "village/plains/plains_lamp_1"),
            "desert", List.of(
                    "village/desert/houses/desert_small_house_1",
                    "village/desert/houses/desert_small_house_2",
                    "village/desert/houses/desert_small_house_3",
                    "village/desert/houses/desert_small_house_4",
                    "village/desert/houses/desert_small_house_5",
                    "village/desert/houses/desert_small_house_6",
                    "village/desert/houses/desert_small_house_7",
                    "village/desert/houses/desert_small_house_8",
                    "village/desert/houses/desert_medium_house_1",
                    "village/desert/houses/desert_medium_house_2",
                    "village/desert/houses/desert_armorer_1",
                    "village/desert/houses/desert_butcher_shop_1",
                    "village/desert/houses/desert_cartographer_house_1",
                    "village/desert/houses/desert_fisher_1",
                    "village/desert/houses/desert_fletcher_house_1",
                    "village/desert/houses/desert_library_1",
                    "village/desert/houses/desert_mason_1",
                    "village/desert/houses/desert_shepherd_house_1",
                    "village/desert/houses/desert_tannery_1",
                    "village/desert/houses/desert_temple_1",
                    "village/desert/houses/desert_temple_2",
                    "village/desert/houses/desert_tool_smith_1",
                    "village/desert/houses/desert_weaponsmith_1",
                    "village/desert/houses/desert_farm_1",
                    "village/desert/houses/desert_farm_2",
                    "village/desert/houses/desert_large_farm_1",
                    "village/desert/houses/desert_animal_pen_1",
                    "village/desert/houses/desert_animal_pen_2",
                    "village/desert/desert_lamp_1"),
            "savanna", List.of(
                    "village/savanna/houses/savanna_small_house_1",
                    "village/savanna/houses/savanna_small_house_2",
                    "village/savanna/houses/savanna_small_house_3",
                    "village/savanna/houses/savanna_small_house_4",
                    "village/savanna/houses/savanna_small_house_5",
                    "village/savanna/houses/savanna_small_house_6",
                    "village/savanna/houses/savanna_small_house_7",
                    "village/savanna/houses/savanna_small_house_8",
                    "village/savanna/houses/savanna_medium_house_1",
                    "village/savanna/houses/savanna_medium_house_2",
                    "village/savanna/houses/savanna_armorer_1",
                    "village/savanna/houses/savanna_butchers_shop_1",
                    "village/savanna/houses/savanna_cartographer_1",
                    "village/savanna/houses/savanna_fisher_cottage_1",
                    "village/savanna/houses/savanna_fletcher_house_1",
                    "village/savanna/houses/savanna_library_1",
                    "village/savanna/houses/savanna_mason_1",
                    "village/savanna/houses/savanna_shepherd_1",
                    "village/savanna/houses/savanna_tannery_1",
                    "village/savanna/houses/savanna_temple_1",
                    "village/savanna/houses/savanna_temple_2",
                    "village/savanna/houses/savanna_tool_smith_1",
                    "village/savanna/houses/savanna_weaponsmith_1",
                    "village/savanna/houses/savanna_small_farm",
                    "village/savanna/houses/savanna_large_farm_1",
                    "village/savanna/houses/savanna_animal_pen_1",
                    "village/savanna/houses/savanna_animal_pen_2",
                    "village/savanna/savanna_lamp_post_01"),
            "taiga", List.of(
                    "village/taiga/houses/taiga_small_house_1",
                    "village/taiga/houses/taiga_small_house_2",
                    "village/taiga/houses/taiga_small_house_3",
                    "village/taiga/houses/taiga_small_house_4",
                    "village/taiga/houses/taiga_small_house_5",
                    "village/taiga/houses/taiga_medium_house_1",
                    "village/taiga/houses/taiga_medium_house_2",
                    "village/taiga/houses/taiga_medium_house_3",
                    "village/taiga/houses/taiga_medium_house_4",
                    "village/taiga/houses/taiga_armorer_house_1",
                    "village/taiga/houses/taiga_armorer_2",
                    "village/taiga/houses/taiga_butcher_shop_1",
                    "village/taiga/houses/taiga_cartographer_house_1",
                    "village/taiga/houses/taiga_fisher_cottage_1",
                    "village/taiga/houses/taiga_fletcher_house_1",
                    "village/taiga/houses/taiga_library_1",
                    "village/taiga/houses/taiga_masons_house_1",
                    "village/taiga/houses/taiga_shepherds_house_1",
                    "village/taiga/houses/taiga_tannery_1",
                    "village/taiga/houses/taiga_temple_1",
                    "village/taiga/houses/taiga_tool_smith_1",
                    "village/taiga/houses/taiga_weaponsmith_1",
                    "village/taiga/houses/taiga_small_farm_1",
                    "village/taiga/houses/taiga_large_farm_1",
                    "village/taiga/houses/taiga_animal_pen_1",
                    "village/taiga/taiga_lamp_post_1"),
            "snowy", List.of(
                    "village/snowy/houses/snowy_small_house_1",
                    "village/snowy/houses/snowy_small_house_2",
                    "village/snowy/houses/snowy_small_house_3",
                    "village/snowy/houses/snowy_small_house_4",
                    "village/snowy/houses/snowy_small_house_5",
                    "village/snowy/houses/snowy_small_house_6",
                    "village/snowy/houses/snowy_small_house_7",
                    "village/snowy/houses/snowy_small_house_8",
                    "village/snowy/houses/snowy_medium_house_1",
                    "village/snowy/houses/snowy_medium_house_2",
                    "village/snowy/houses/snowy_armorer_house_1",
                    "village/snowy/houses/snowy_butchers_shop_1",
                    "village/snowy/houses/snowy_cartographer_house_1",
                    "village/snowy/houses/snowy_fisher_cottage",
                    "village/snowy/houses/snowy_fletcher_house_1",
                    "village/snowy/houses/snowy_library_1",
                    "village/snowy/houses/snowy_masons_house_1",
                    "village/snowy/houses/snowy_shepherds_house_1",
                    "village/snowy/houses/snowy_tannery_1",
                    "village/snowy/houses/snowy_temple_1",
                    "village/snowy/houses/snowy_tool_smith_1",
                    "village/snowy/houses/snowy_weapon_smith_1",
                    "village/snowy/houses/snowy_farm_1",
                    "village/snowy/houses/snowy_animal_pen_1",
                    "village/snowy/houses/snowy_animal_pen_2",
                    "village/snowy/snowy_lamp_post_01"));

    private VillageStructures() {
    }

    /**
     * As casas que uma vila deste estilo pode ter.
     *
     * <p>Vazio quer dizer catálogo ausente ou estilo desconhecido, e o
     * chamador precisa tratar: sem lista não há o que construir, e
     * inventar uma casa para preencher o silêncio é exatamente o que
     * esta regra proíbe.
     */
    public static synchronized List<ResourceId> housesFor(String style) {
        return HOUSES.computeIfAbsent(style, found -> buildableFor(found).stream()
                .filter(id -> id.path().startsWith("village/" + found + "/houses/"))
                .toList());
    }

    /** Todas as peças autorizadas, inclusive lâmpadas fora de {@code houses}. */
    public static synchronized List<ResourceId> buildableFor(String style) {
        return BUILDABLE.computeIfAbsent(style, VillageStructures::loadAllowed).stream()
                .filter(VillageStructures::isProfessionBuildable)
                .toList();
    }

    /**
     * Se a planta pode ser escolhida pelas profissoes.
     *
     * <p>A {@code BigHouseMOD} pertence a fundacao da vila e e colocada
     * uma unica vez por {@link BigHouseFoundation}; ela nunca e uma obra
     * aberta pelo catalogo de construtor. O namespace e a barreira
     * explicita contra ela voltar a aparecer numa lista profissional.
     */
    public static boolean isProfessionBuildable(ResourceId id) {
        return id != null && ResourceId.VANILLA.equals(id.namespace());
    }

    /**
     * As ruas que uma vila deste estilo tem — 2026-08-21.
     *
     * <p>Servem a uma pergunta só, e ela é sobre <b>material</b>: de que
     * bloco o jogo pavimenta a rua deste bioma. A colônia precisa saber
     * para reconhecer beira de rua e para estender a que existe.
     *
     * <p>Nunca teve a restrição da casa pequena, e não passou a precisar
     * quando ela caiu em 2026-09-09: rua não é casa, e o nome da peça de
     * rua nunca seguiu a convenção da casa.
     */
    public static synchronized List<ResourceId> streetsFor(String style) {
        return STREETS.computeIfAbsent(style, found -> load(found, "streets"));
    }

    /**
     * As roças que uma vila deste estilo tem — decisão do autor,
     * 2026-09-05.
     *
     * <p><b>A frase dele:</b> <i>"precisam construir o espaço de
     * plantação padrão e idêntico aos que já vêm na vila do
     * Minecraft"</i>. E a resposta é a Regra 27 outra vez: a roça padrão
     * já está no catálogo do jogo, ao lado das casas —
     * {@code plains_small_farm_1}, {@code plains_large_farm_1}, e as
     * equivalentes dos outros quatro estilos. Nenhum {@code .nbt} novo,
     * nenhum byte da Mojang.
     *
     * <p>Moram na pasta {@code houses} porque é lá que o gerador de vilas
     * as põe — para o Vanilla, a roça é uma das peças que um lote pode
     * receber. É por isso que a busca é por <b>nome</b> e não por pasta.
     *
     * <p>A busca por nome também a livrava da restrição da casa pequena,
     * que valeu até 2026-09-09: filtrar a pasta {@code houses} pelo
     * sufixo da casa apagaria a roça do catálogo inteiro.
     */
    public static synchronized List<ResourceId> farmsFor(String style) {
        return FARMS.computeIfAbsent(style, found -> housesFor(found).stream()
                .filter(id -> id.path().contains("farm"))
                .toList());
    }

    private static List<ResourceId> loadAllowed(String style) {
        Set<String> catalog = readCatalog();
        List<String> requested = ALLOWED.getOrDefault(style, List.of());
        List<ResourceId> found = new ArrayList<>();

        for (String path : requested) {
            if (catalog.contains(path)) {
                found.add(ResourceId.vanilla(path));
            } else {
                VillageColonyMod.LOGGER.warn(
                        "Allowed village structure is missing from the Vanilla catalog: {}", path);
            }
        }

        VillageColonyMod.LOGGER.info(
                "Village style {} has {} explicitly allowed buildable structures",
                style, found.size());
        return List.copyOf(found);
    }

    private static List<ResourceId> load(String style, String kind) {
        String folder = "village/" + style + "/" + kind + "/";

        List<ResourceId> found = readCatalog().stream()
                .filter(path -> path.startsWith(folder) && !path.contains("zombie"))
                .map(ResourceId::vanilla)
                .toList();

        VillageColonyMod.LOGGER.info(
                "Village style {} has {} {} in the game catalog",
                style,
                found.size(),
                kind);

        return List.copyOf(found);
    }

    private static Set<String> readCatalog() {
        try (InputStream stream = VillageStructures.class.getResourceAsStream(CATALOG)) {
            if (stream == null) {
                VillageColonyMod.LOGGER.warn(
                        "The structure catalog is missing from the jar — no structure to build");
                return Set.of();
            }

            JsonObject root = JsonParser
                    .parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            Set<String> paths = new HashSet<>();

            for (JsonElement entry : root.getAsJsonArray("structures")) {
                paths.add(entry.getAsString());
            }

            return Set.copyOf(paths);
        } catch (Exception broken) {
            VillageColonyMod.LOGGER.warn(
                    "Could not read the structure catalog — no structure to build", broken);
            return Set.of();
        }
    }

    /** Esquece o que foi lido. Chamado ao parar o servidor. */
    public static synchronized void clearAll() {
        HOUSES.clear();
        BUILDABLE.clear();
        STREETS.clear();
        FARMS.clear();
    }
}
