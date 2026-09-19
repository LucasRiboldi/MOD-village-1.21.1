package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.type.ResourceId;

import java.util.List;
import java.util.Map;

/**
 * A areia da planta do deserto vira arenito — 2026-09-19.
 *
 * <p><b>Decisão do autor, e é uma emenda à Regra 27.</b> A regra manda o
 * construtor <i>aguardar a existência do específico tipo de bloco que ele
 * precisa</i>, e ela é imutável desde 2026-08-20 — o autor já a emendou
 * duas vezes por escrito (pedra em 08-26, espécie de madeira em 09-05), e
 * esta é a terceira. Ela não se emenda sozinha e não se emenda aqui: o que
 * está escrito aqui é o que foi decidido, não o que o código achou melhor.
 *
 * <p><b>O que a planta realmente pede.</b> Medido no relatório de
 * cobertura: as casas do deserto pedem {@code sand} <b>junto com nove
 * variantes de arenito</b> — {@code sandstone}, {@code cut_sandstone},
 * {@code smooth_sandstone}, {@code chiseled_sandstone}, mais lajes,
 * escadas e muro. A areia não é o material da casa; ela é o punhado que o
 * Vanilla deixa no piso e na moldura.
 *
 * <p><b>Por que trocar.</b> Areia cai. Uma casa entregue com areia na
 * parede ou no teto desaba sozinha no primeiro vizinho quebrado, e o que
 * sobra não é a casa que a planta descreve — é um monte. O arenito é o
 * mesmo material cozido: mesma cor, mesma família, e a colônia já o
 * produz, porque a {@code VillagePalette} define o arenito como <b>a pedra
 * do deserto</b>.
 *
 * <p><b>Por que a troca é na PLANTA, e não na hora de assentar.</b> Esta
 * foi a decisão de projeto que quase saiu errada. Trocar só no
 * {@code BuilderWork} faria a obra <b>pedir areia</b> ao baú e
 * <b>assentar arenito</b> na parede, porque a demanda da colônia sai de
 * {@code ConstructionProject.remainingMaterials()}, que conta o bloco da
 * planta. Conta e parede discordando é exatamente o defeito de
 * <b>2026-08-22</b>: a colônia declarava a meta cumprida, o mineiro não ia
 * cavar, e a obra dormia esperando o que ninguém buscaria.
 *
 * <p>Trocando na planta — no único ponto em que ela é montada,
 * {@code StructureBlueprintReader} — as três pontas voltam a falar do
 * mesmo bloco: o que a colônia estoca, o que o construtor espera e o que
 * encosta na parede.
 *
 * <p><b>E a areia vermelha vai junto</b>, para arenito vermelho, porque a
 * razão é a mesma e deixá-la de fora seria consertar meio deserto.
 *
 * <p>Não mexe com a areia do <b>mundo</b>: duna, praia e o chão que o
 * mineiro raspa continuam areia. Isto governa só o que a colônia
 * <b>assenta</b>.
 */
public final class DesertSand {

    /** O estilo em que a troca vale, como o catálogo o nomeia. */
    public static final String DESERT = "desert";

    /** O que vira o quê. Areia comum e vermelha, cada uma no seu arenito. */
    private static final Map<ResourceId, ResourceId> BAKED = Map.of(
            ResourceId.vanilla("sand"), ResourceId.vanilla("sandstone"),
            ResourceId.vanilla("red_sand"), ResourceId.vanilla("red_sandstone"));

    private DesertSand() {
    }

    /**
     * A planta com a areia já trocada, se for do deserto.
     *
     * <p>Devolve a <b>mesma lista</b> quando não há o que trocar, para que
     * o caminho comum — toda vila que não é de deserto — não pague cópia
     * nenhuma.
     */
    public static List<BlueprintBlock> baked(ResourceId plan, List<BlueprintBlock> blocks) {
        if (!DESERT.equals(styleOf(plan))) {
            return blocks;
        }

        boolean anySand = blocks.stream().anyMatch(block -> BAKED.containsKey(block.block()));

        if (!anySand) {
            return blocks;
        }

        return blocks.stream()
                .map(block -> {
                    ResourceId baked = BAKED.get(block.block());

                    if (baked == null) {
                        return block;
                    }

                    return new BlueprintBlock(block.offset(), baked, block.furniture());
                })
                .toList();
    }

    /** Se este bloco da planta seria trocado neste estilo. Para a bateria. */
    public static boolean isSwapped(ResourceId block, String style) {
        return DESERT.equals(style) && BAKED.containsKey(block);
    }

    /**
     * O estilo que esta planta declara no próprio nome.
     *
     * <p><b>Por que sai da planta, e não do bioma debaixo da obra.</b> A
     * vila de deserto que nasce na borda tem chão de deserto num canto e
     * savana no outro; perguntar ao bioma faria a mesma casa trocar de
     * regra conforme o bloco, e metade dela sairia de areia. A planta é
     * uma só e já diz o que é — {@code village/desert/houses/…} —, então a
     * casa inteira segue uma regra só.
     *
     * <p>É a ADR-009 aplicada: quem sabe é o jogo. O caminho da estrutura
     * é do catálogo do próprio Minecraft, e não uma tabela escrita aqui.
     */
    public static String styleOf(ResourceId plan) {
        String path = plan.path();

        int village = path.indexOf("village/");

        if (village < 0) {
            return "";
        }

        String after = path.substring(village + "village/".length());

        int slash = after.indexOf('/');

        return slash < 0 ? after : after.substring(0, slash);
    }
}
