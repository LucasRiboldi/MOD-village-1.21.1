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
 */
public final class VillageStructures {

    /**
     * <b>Barreira de teste, e ela sai inteira quando o autor mandar.</b>
     *
     * <p>Regra do autor de 2026-08-20, e ela é explicitamente
     * provisória: <i>"enquanto este projeto não estiver formalmente
     * acabado, a única estrutura que o construtor pode construir é a
     * casa pequena do seu bioma"</i>.
     *
     * <p>Vinte e oito a trinta e seis casas por bioma é variedade demais
     * para depurar: cada uma pede materiais diferentes, e uma sessão que
     * falha não diz se falhou pela regra nova ou pela casa sorteada. Uma
     * casa por bioma torna toda sessão comparável com a anterior.
     *
     * <p><b>Para desligar:</b> apague este campo e a linha que o usa em
     * {@link #load}. A lista volta a ser a pasta inteira, que é a
     * Regra 27, e nada mais precisa mudar.
     */
    private static final String ONLY_WHILE_TESTING = "_small_house_1";

    /** Onde o índice da pasta mora dentro do jar. */
    private static final String CATALOG =
            "/data/villagecolony/catalog/vanilla_structures.json";

    /** Lido uma vez por sessão. São mil e cento e oitenta nomes. */
    private static final Map<String, List<ResourceId>> HOUSES = new HashMap<>();

    /** As ruas, pela mesma porta e no mesmo catálogo — 2026-08-21. */
    private static final Map<String, List<ResourceId>> STREETS = new HashMap<>();

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
        return HOUSES.computeIfAbsent(style, found -> load(found, "houses", true));
    }

    /**
     * As ruas que uma vila deste estilo tem — 2026-08-21.
     *
     * <p>Servem a uma pergunta só, e ela é sobre <b>material</b>: de que
     * bloco o jogo pavimenta a rua deste bioma. A colônia precisa saber
     * para reconhecer beira de rua e para estender a que existe.
     *
     * <p>Sem a barreira de teste: a Regra 28 limita quantas <b>casas</b>
     * a colônia tenta construir, e uma rua não é casa — restringir a
     * lista aqui só esconderia estilos cujo nome de peça não bate com a
     * convenção da casa pequena.
     */
    public static synchronized List<ResourceId> streetsFor(String style) {
        return STREETS.computeIfAbsent(style, found -> load(found, "streets", false));
    }

    private static List<ResourceId> load(String style, String kind, boolean onlyWhileTesting) {
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

                // A barreira de teste. Some com esta linha.
                if (onlyWhileTesting && !path.endsWith(ONLY_WHILE_TESTING)) {
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
    }
}
