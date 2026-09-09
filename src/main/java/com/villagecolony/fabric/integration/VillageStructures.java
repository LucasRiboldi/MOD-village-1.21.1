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
import java.util.List;
import java.util.Map;

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
 * <pre>
 * plains    36 casas      savanna   31 casas
 * taiga     27 casas      snowy     30 casas
 * desert    28 casas
 * </pre>
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

    /** Lido uma vez por sessão. São mil e cento e oitenta nomes. */
    private static final Map<String, List<ResourceId>> HOUSES = new HashMap<>();

    /** As ruas, pela mesma porta e no mesmo catálogo — 2026-08-21. */
    private static final Map<String, List<ResourceId>> STREETS = new HashMap<>();

    /** As roças, pela mesma porta — 2026-09-05. */
    private static final Map<String, List<ResourceId>> FARMS = new HashMap<>();

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
        return HOUSES.computeIfAbsent(style, found -> load(found, "houses"));
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
        return FARMS.computeIfAbsent(style, found -> load(found, "houses").stream()
                .filter(id -> id.path().contains("farm"))
                .toList());
    }

    private static List<ResourceId> load(String style, String kind) {
        String folder = "village/" + style + "/" + kind + "/";

        List<ResourceId> found = new ArrayList<>();

        try (InputStream stream = VillageStructures.class.getResourceAsStream(CATALOG)) {
            if (stream == null) {
                VillageColonyMod.LOGGER.warn(
                        "The structure catalog is missing from the jar — no house to build");

                return List.of();
            }

            JsonObject root = JsonParser
                    .parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();

            for (JsonElement entry : root.getAsJsonArray("structures")) {
                String path = entry.getAsString();

                if (!path.startsWith(folder) || path.contains("zombie")) {
                    continue;
                }

                found.add(ResourceId.vanilla(path));
            }
        } catch (Exception broken) {
            // Catálogo corrompido é o mod sem casa nenhuma, e isso precisa
            // aparecer: silêncio aqui viraria "a colônia não constrói" sem
            // motivo no log, que é o §11 outra vez.
            VillageColonyMod.LOGGER.warn(
                    "Could not read the structure catalog — no house to build", broken);

            return List.of();
        }

        VillageColonyMod.LOGGER.info(
                "Village style {} has {} {} in the game catalog",
                style,
                found.size(),
                kind);

        return List.copyOf(found);
    }

    /** Esquece o que foi lido. Chamado ao parar o servidor. */
    public static synchronized void clearAll() {
        HOUSES.clear();
        STREETS.clear();
        FARMS.clear();
    }
}
