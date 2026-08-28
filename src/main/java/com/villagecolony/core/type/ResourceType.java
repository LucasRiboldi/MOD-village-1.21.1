package com.villagecolony.core.type;

import java.util.Objects;

/**
 * Um recurso que a colônia sabe contar.
 *
 * <p>Três no MVP, conforme TASK-017. Não é a lista de tudo que existe no
 * Minecraft: é a lista do que a colônia acompanha. Um item fora dela
 * continua no baú, apenas não é contado.
 *
 * <p>Tipo próprio do Core: nenhuma classe daqui conhece {@code Item}. A
 * conversão a partir do item Vanilla mora em
 * {@code fabric.adapter.MinecraftTypeAdapter}. Ver ADR-005.
 *
 * <p>O modelo de Resource-System.md §"Modelo de Recurso" também prevê
 * {@code quantity} e {@code locations}. Eles não estão aqui de
 * propósito: isto é o tipo do recurso, que é fixo, e não o estoque, que
 * muda. A quantidade vive em {@link ResourceTally}.
 */
public enum ResourceType {

    OAK_LOG(ResourceCategory.NATURAL, ResourceGroup.WOOD, Production.HARVESTED),

    BIRCH_LOG(ResourceCategory.NATURAL, ResourceGroup.WOOD, Production.HARVESTED),

    SPRUCE_LOG(ResourceCategory.NATURAL, ResourceGroup.WOOD, Production.HARVESTED),

    JUNGLE_LOG(ResourceCategory.NATURAL, ResourceGroup.WOOD, Production.HARVESTED),

    ACACIA_LOG(ResourceCategory.NATURAL, ResourceGroup.WOOD, Production.HARVESTED),

    DARK_OAK_LOG(ResourceCategory.NATURAL, ResourceGroup.WOOD, Production.HARVESTED),

    CHERRY_LOG(ResourceCategory.NATURAL, ResourceGroup.WOOD, Production.HARVESTED),

    MANGROVE_LOG(ResourceCategory.NATURAL, ResourceGroup.WOOD, Production.HARVESTED),

    OAK_PLANKS(ResourceCategory.PROCESSED, ResourceGroup.PLANKS, Production.CRAFTED),

    BIRCH_PLANKS(ResourceCategory.PROCESSED, ResourceGroup.PLANKS, Production.CRAFTED),

    SPRUCE_PLANKS(ResourceCategory.PROCESSED, ResourceGroup.PLANKS, Production.CRAFTED),

    JUNGLE_PLANKS(ResourceCategory.PROCESSED, ResourceGroup.PLANKS, Production.CRAFTED),

    ACACIA_PLANKS(ResourceCategory.PROCESSED, ResourceGroup.PLANKS, Production.CRAFTED),

    DARK_OAK_PLANKS(ResourceCategory.PROCESSED, ResourceGroup.PLANKS, Production.CRAFTED),

    CHERRY_PLANKS(ResourceCategory.PROCESSED, ResourceGroup.PLANKS, Production.CRAFTED),

    MANGROVE_PLANKS(ResourceCategory.PROCESSED, ResourceGroup.PLANKS, Production.CRAFTED),

    COBBLESTONE(ResourceCategory.NATURAL, ResourceGroup.STONE, Production.MINED),

    /** A pedra da vila de deserto, e a parede dela — 2026-08-20. */
    SANDSTONE(ResourceCategory.NATURAL, ResourceGroup.STONE, Production.MINED),

    /** O que o fundidor recebe para dar vidro. */
    SAND(ResourceCategory.NATURAL, ResourceGroup.SAND, Production.MINED),

    /** O que o fundidor devolve. A vidraça sai daqui. */
    GLASS(ResourceCategory.PROCESSED, ResourceGroup.NONE, Production.SMELTED),

    /** O que o pastor traz. A cama sai daqui. */
    WHITE_WOOL(ResourceCategory.NATURAL, ResourceGroup.WOOL, Production.SHEARED),

    /**
     * A lavoura — 2026-08-27.
     *
     * <p>Das sete profissões, o fazendeiro era a única sem trabalho: a
     * colônia lhe dava enxada, baú e placa com o nome, e nunca mais
     * falava com ele. Faltava a corrente inteira, e ela começa aqui —
     * sem um recurso de lavoura não há meta, sem meta não há tarefa, e
     * sem tarefa ele fica parado como o mineiro das 21:06.
     *
     * <p>As quatro do jogo que se plantam em terra arada e se replantam
     * com a própria colheita. Ver {@code FarmerWork}.
     */
    WHEAT(ResourceCategory.NATURAL, ResourceGroup.CROPS, Production.FARMED),

    CARROT(ResourceCategory.NATURAL, ResourceGroup.CROPS, Production.FARMED),

    POTATO(ResourceCategory.NATURAL, ResourceGroup.CROPS, Production.FARMED),

    BEETROOT(ResourceCategory.NATURAL, ResourceGroup.CROPS, Production.FARMED),

    /** O que o mineiro acha na galeria. A tocha sai daqui — 2026-08-21. */
    COAL(ResourceCategory.NATURAL, ResourceGroup.COAL, Production.MINED),

    /** O minério de ferro, como sai da pedra: cru, e ainda não serve. */
    RAW_IRON(ResourceCategory.NATURAL, ResourceGroup.IRON, Production.MINED),

    /** O que o fundidor devolve do ferro cru. O lampião sai daqui. */
    IRON_INGOT(ResourceCategory.PROCESSED, ResourceGroup.NONE, Production.SMELTED),

    /**
     * A parede da vila de deserto — 2026-08-22.
     *
     * <p>Sai da <b>fornalha</b>, e não da bancada: arenito liso é arenito
     * assado. Entrou porque a casa de deserto do catálogo é feita dele —
     * sessenta blocos — e a colônia só sabia cavar o arenito cru.
     */
    SMOOTH_SANDSTONE(ResourceCategory.PROCESSED, ResourceGroup.NONE, Production.SMELTED);

    private final ResourceCategory category;
    private final ResourceGroup group;
    private final Production production;

    ResourceType(ResourceCategory category, ResourceGroup group, Production production) {
        this.category = Objects.requireNonNull(category);
        this.group = Objects.requireNonNull(group);
        this.production = Objects.requireNonNull(production);
    }

    public ResourceCategory category() {
        return category;
    }

    /**
     * O grupo que soma com este recurso para efeito de meta.
     *
     * <p>Oito madeiras, uma meta. Ver {@link ResourceGroup}.
     */
    public ResourceGroup group() {
        return group;
    }

    /**
     * De onde este recurso vem, e por isso quem o produz.
     *
     * <p>Declarado, e não deduzido: era uma exceção nominal em
     * {@code ColonyCycle.typeFor} até 2026-08-22, e a ADR-009 pede o
     * contrário. Ver {@link Production}.
     */
    public Production production() {
        return production;
    }
}
