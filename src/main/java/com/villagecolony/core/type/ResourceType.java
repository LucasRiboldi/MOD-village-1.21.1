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

    OAK_PLANKS(ResourceCategory.PROCESSED, ResourceGroup.PLANKS, Production.CRAFTED_WOOD),

    BIRCH_PLANKS(ResourceCategory.PROCESSED, ResourceGroup.PLANKS, Production.CRAFTED_WOOD),

    SPRUCE_PLANKS(ResourceCategory.PROCESSED, ResourceGroup.PLANKS, Production.CRAFTED_WOOD),

    JUNGLE_PLANKS(ResourceCategory.PROCESSED, ResourceGroup.PLANKS, Production.CRAFTED_WOOD),

    ACACIA_PLANKS(ResourceCategory.PROCESSED, ResourceGroup.PLANKS, Production.CRAFTED_WOOD),

    DARK_OAK_PLANKS(ResourceCategory.PROCESSED, ResourceGroup.PLANKS, Production.CRAFTED_WOOD),

    CHERRY_PLANKS(ResourceCategory.PROCESSED, ResourceGroup.PLANKS, Production.CRAFTED_WOOD),

    MANGROVE_PLANKS(ResourceCategory.PROCESSED, ResourceGroup.PLANKS, Production.CRAFTED_WOOD),

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
    SMOOTH_SANDSTONE(ResourceCategory.PROCESSED, ResourceGroup.NONE, Production.SMELTED),

    /**
     * A pedra assada, e o degrau do meio da cadeia do pedreiro —
     * 2026-09-09.
     *
     * <p>Sai da <b>fornalha</b>, como o arenito liso: pedregulho assado é
     * pedra. Entrou com a divisão do fabricante, porque o tijolo que o
     * pedreiro lavra é feito dela e não do pedregulho cru — é a receita
     * do jogo, e o mod não inventa receita.
     *
     * <p><b>Grupo {@code NONE} de propósito</b>, como todo processado
     * daqui. Pô-la no grupo da pedra faria pedra contar como pedregulho
     * na meta do mineiro, e a conta da parede é da paleta do bioma, por
     * nome — ver {@code ResourceSubstitution} e a discordância de
     * 2026-08-22.
     */
    STONE(ResourceCategory.PROCESSED, ResourceGroup.NONE, Production.SMELTED),

    /**
     * O que o pedreiro lavra, e a razão de ele não nascer decorativo —
     * 2026-09-09.
     *
     * <p>Profissão sem material declarado nunca recebe pedido: o aldeão
     * ganharia placa, baú e ferramenta e nunca trabalharia, que foi o
     * estado do fazendeiro até 2026-08-27. {@code MASON} nasceu com esta
     * linha no mesmo commit, e é ela que
     * {@code ProfessionResponsibilityTest.everyProfessionAnswersForSomeMaterial}
     * encontra.
     *
     * <p>A cadeia inteira é do jogo: o mineiro traz <b>pedregulho</b>, o
     * fundidor o assa em <b>pedra</b>, e o pedreiro lavra quatro delas
     * em quatro <b>tijolos</b>. A casa de planície do catálogo os usa.
     */
    STONE_BRICKS(ResourceCategory.PROCESSED, ResourceGroup.NONE, Production.CRAFTED_STONE);

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
