package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.ProfessionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Os modelos de estrutura próprios da colônia — pedido do autor, 2026-10-02.
 *
 * <p><i>"Criar uma pasta no projeto onde ficarão salvos os modelos de
 * estruturas particulares deste mod; dentro dela, modelo de casa de cada um dos
 * ofícios, além de modelos extras; estes modelos serão os prioritários em
 * relação à versão vanilla, quando existirem."</i>
 *
 * <p><b>A pasta</b> é {@code src/main/resources/data/villagecolony/structure/colony/},
 * e cada modelo é um arquivo {@code .nbt} de estrutura do jogo (o mesmo formato
 * que o Bloco de Estrutura grava). O {@code CATALOGO.md} dentro dela lista tudo
 * o que a colônia pode construir e se já há modelo próprio.
 *
 * <ul>
 *   <li><b>Casa de ofício</b> — {@code colony/<estilo>/trade_<oficio>.nbt}, ou
 *       {@code colony/trade_<oficio>.nbt} para todos os estilos. Entra na lista
 *       de obras à frente das casas de ofício do jogo (Regra 49).</li>
 *   <li><b>Troca de uma estrutura do jogo</b> —
 *       {@code colony/override/<caminho do jogo>.nbt}, por exemplo
 *       {@code colony/override/village/plains/houses/plains_small_house_1.nbt}.
 *       Os blocos vêm do modelo; o nome continua o do jogo, e por isso o tipo,
 *       o ofício e o rodízio não mudam.</li>
 * </ul>
 *
 * <p><b>Sem modelo, nada muda.</b> A pasta vazia deixa o mod exatamente como
 * era: só o que existe no jar entra.
 */
public final class ColonyModels {

    static {
        ServerMemory.register(ColonyModels.class, ColonyModels::clearAll);
    }

    /** A pasta dos modelos, dentro de {@code data/villagecolony/structure/}. */
    public static final String FOLDER = "colony";

    private static final String OVERRIDE = FOLDER + "/override/";

    private static final String TRADE = "trade_";

    private static final Map<String, Boolean> EXISTS = new HashMap<>();

    /** Modelos de mentira, só para o teste: caminho do modelo → estrutura real. */
    private static final Map<String, ResourceId> PRETEND = new HashMap<>();

    private static final Set<String> ANNOUNCED = new HashSet<>();

    private ColonyModels() {
    }

    /**
     * O modelo da colônia que substitui esta estrutura do jogo, se houver.
     *
     * @return o id do modelo a ler; vazio sem modelo, ou para estrutura que não é do jogo
     */
    public static Optional<ResourceId> overrideOf(ResourceId vanilla) {
        if (!ResourceId.VANILLA.equals(vanilla.namespace())) {
            return Optional.empty();
        }

        Optional<ResourceId> model = find(OVERRIDE + vanilla.path());

        model.filter(found -> ANNOUNCED.add(vanilla.path())).ifPresent(found ->
                VillageColonyMod.LOGGER.info("The colony builds its own model of {} — {}", vanilla, found));

        return model;
    }

    /**
     * A estrutura cujos blocos se leem para {@code structure}: o modelo da
     * colônia que a substitui, quando existe, ou ela mesma. O nome da planta
     * continua o pedido, para o tipo, o ofício e o rodízio não mudarem.
     */
    public static ResourceId sourceFor(ResourceId structure) {
        ResourceId source = overrideOf(structure).orElse(structure);

        return isColonyModel(source) ? templateOf(source) : source;
    }

    /**
     * As casas de ofício da colônia para este estilo, na ordem dos ofícios: a
     * do estilo primeiro, a de todos os estilos se a do estilo faltar.
     */
    public static List<ResourceId> tradeHousesFor(String style) {
        List<ResourceId> found = new ArrayList<>();

        for (ProfessionType profession : ProfessionType.values()) {
            String name = TRADE + profession.name().toLowerCase(Locale.ROOT);

            find(FOLDER + "/" + style + "/" + name)
                    .or(() -> find(FOLDER + "/" + name))
                    .ifPresent(found::add);
        }

        return List.copyOf(found);
    }

    /** De que ofício é esta casa da colônia; vazio para o que não é casa de ofício dela. */
    public static Optional<ProfessionType> professionOf(ResourceId id) {
        if (!isColonyModel(id)) {
            return Optional.empty();
        }

        String path = id.path();
        int at = path.lastIndexOf(TRADE);

        if (at < 0) {
            return Optional.empty();
        }

        try {
            return Optional.of(ProfessionType.valueOf(path.substring(at + TRADE.length()).toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException unknown) {
            return Optional.empty();
        }
    }

    /** Se este id é um modelo da pasta da colônia. */
    public static boolean isColonyModel(ResourceId id) {
        return id != null && "villagecolony".equals(id.namespace()) && id.path().startsWith(FOLDER + "/");
    }

    /**
     * A estrutura que de fato se lê para este modelo: a do jar, ou a de
     * mentira que o teste pôs no lugar.
     */
    public static ResourceId templateOf(ResourceId model) {
        return PRETEND.getOrDefault(model.path(), model);
    }

    private static Optional<ResourceId> find(String path) {
        if (PRETEND.containsKey(path)) {
            return Optional.of(new ResourceId("villagecolony", path));
        }

        boolean exists = EXISTS.computeIfAbsent(path, missing ->
                ColonyModels.class.getResource("/data/villagecolony/structure/" + missing + ".nbt") != null);

        return exists ? Optional.of(new ResourceId("villagecolony", path)) : Optional.empty();
    }

    /**
     * Finge, para o teste, que existe o modelo {@code path} com os blocos de
     * {@code template}. Quem chama desfaz com {@link #stopPretending} na mesma
     * chamada: a bateria divide um mundo só.
     */
    public static void pretend(String path, ResourceId template) {
        PRETEND.put(path, template);
    }

    public static void stopPretending() {
        PRETEND.clear();
    }

    static void clearAll() {
        EXISTS.clear();
        PRETEND.clear();
        ANNOUNCED.clear();
    }
}
