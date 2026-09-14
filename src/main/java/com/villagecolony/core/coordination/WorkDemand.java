package com.villagecolony.core.coordination;

import com.villagecolony.core.type.ResourceType;

import java.util.Map;
import java.util.EnumMap;

import java.util.Objects;

/**
 * O que a obra aberta de uma colônia ainda consome — 2026-08-21.
 *
 * <p><b>Por que virou um tipo.</b> {@code ColonyGoals.of} recebia a
 * demanda da obra solta, um inteiro por material, e cada material novo
 * era um parâmetro a mais: tábua em 08-13, pedra e lã em 08-20, vidro no
 * mesmo dia, carvão no seguinte. Nove parâmetros posicionais, quatro
 * deles do mesmo tipo — é a assinatura onde trocar dois de lugar não dá
 * erro de compilação e a colônia passa a pedir lã pela conta do vidro.
 *
 * <p>Aqui cada número tem nome, e a validação é uma só.
 *
 * @param planks quantas tábuas a obra ainda pede. Zero quando não há obra
 * @param stone qual pedra esta vila usa — pedregulho, ou arenito no
 *     deserto. Perguntar sempre por pedregulho daria zero no deserto
 * @param stoneAmount quanto dela a obra ainda pede
 * @param wool quanta lã as casas sem cama ainda pedem
 * @param glass quanto vidro a obra ainda consome, já com a vidraça
 *     decomposta pela receita
 * @param coal quanto carvão as tochas da obra vão custar, pela mesma
 *     decomposição
 * @param iron quantos lingotes os lampiões que faltam vão custar. Vem da
 *     mobília da Regra 21, e não da obra: o lampião entra na casa já
 *     terminada
 */
public record WorkDemand(
        int planks,
        ResourceType stone,
        int stoneAmount,
        int wool,
        int glass,
        int coal,
        int iron,
        Map<ResourceType, Integer> smelted,
        Map<ResourceType, Integer> surfaceGathered) {

    public WorkDemand {
        Objects.requireNonNull(stone, "stone");
        Objects.requireNonNull(smelted, "smelted");

        smelted = Map.copyOf(smelted);
        Objects.requireNonNull(surfaceGathered, "surfaceGathered");
        surfaceGathered = Map.copyOf(surfaceGathered);

        refuseNegative(planks, "plank");
        refuseNegative(stoneAmount, "stone");
        refuseNegative(wool, "wool");
        refuseNegative(glass, "glass");
        refuseNegative(coal, "coal");
        refuseNegative(iron, "iron");
    }

    public WorkDemand(
            int planks,
            ResourceType stone,
            int stoneAmount,
            int wool,
            int glass,
            int coal,
            int iron,
            Map<ResourceType, Integer> smelted) {
        this(planks, stone, stoneAmount, wool, glass, coal, iron, smelted, Map.of());
    }

    /** Nenhuma obra aberta: a colônia decide pela Regra 1 e nada mais. */
    public static WorkDemand none() {
        return new WorkDemand(0, ResourceType.COBBLESTONE, 0, 0, 0, 0, 0, Map.of(), Map.of());
    }

    /** Materiais que destravam a obra e devem preceder produção de reserva. */
    public Map<ResourceType, Integer> constructionMaterials() {
        Map<ResourceType, Integer> materials = new EnumMap<>(ResourceType.class);
        add(materials, stone, stoneAmount);
        add(materials, ResourceType.OAK_PLANKS, planks);
        add(materials, ResourceType.WHITE_WOOL, wool);
        add(materials, ResourceType.GLASS, glass);
        add(materials, ResourceType.COAL, coal);
        add(materials, ResourceType.IRON_INGOT, iron);
        smelted.forEach((type, amount) -> add(materials, type, amount));
        surfaceGathered.forEach((type, amount) -> add(materials, type, amount));
        return Map.copyOf(materials);
    }

    private static void add(Map<ResourceType, Integer> target, ResourceType type, int amount) {
        if (amount > 0) {
            target.merge(type, amount, Integer::sum);
        }
    }

    private static void refuseNegative(int amount, String what) {
        if (amount < 0) {
            throw new IllegalArgumentException("Negative " + what + " demand: " + amount);
        }
    }
}
