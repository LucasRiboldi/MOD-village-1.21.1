package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.VillagePalette;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * De que bloco é a rua desta vila — 2026-08-21.
 *
 * <p><b>Um defeito que estava calado desde 08-14.</b> O
 * {@code BuildSiteScanner} reconhecia rua por um nome escrito no código:
 * {@code minecraft:dirt_path}. Perguntando ao jogo, a resposta é outra:
 *
 * <pre>
 * planície, taiga, savana, nevada   dirt_path
 * deserto                           smooth_sandstone
 * </pre>
 *
 * <p>Ou seja: <b>a vila de deserto nunca teve beira de rua</b>. Ela
 * nascia, contratava, contava recurso, recebia pedra de arenito do
 * mineiro — e nunca achava lote, porque nenhuma coluna dela era
 * {@code dirt_path}. A varredura terminava com "no free lot beside a
 * road", que é a frase certa para a pergunta errada.
 *
 * <p><b>Quem responde é o catálogo</b>, pelo mesmo caminho da Regra 27: a
 * peça de rua do estilo é lida, e o bloco mais frequente dela é a rua. É
 * medida, não tabela — um datapack que troque o calçamento do deserto
 * continua sendo entendido, e nenhum nome de bloco precisa ser escrito
 * aqui.
 *
 * <p>Lido uma vez por sessão, como o resto do catálogo.
 */
public final class VillageRoad {

    /**
     * Quantas peças de rua se leem antes de desistir de um estilo.
     *
     * <p>Duas, e não quatro. A medida acontece dentro de um tique, e ler
     * estrutura do jogo não é barato: cinco estilos vezes quatro peças
     * são vinte leituras num tique só, na primeira vez que alguém
     * pergunta. Duas bastam — a peça de rua é feita de calçamento, e o
     * que a segunda corrige é a decoração solta da primeira.
     */
    private static final int TRIES = 2;

    /** O calçamento de cada estilo, medido do catálogo. */
    private static final Map<String, Optional<ResourceId>> PAVING = new HashMap<>();

    /** Todo bloco que o jogo usa como rua de vila, em qualquer estilo. */
    private static Set<Block> everyPaving;

    private VillageRoad() {
    }

    /**
     * O bloco com que esta vila calça a rua.
     *
     * <p>Vazio quando o estilo não tem peça de rua legível — datapack
     * incompleto, ou estilo que o jogo não conhece. O chamador não
     * inventa um: calçar com o bloco errado é pior que não calçar.
     */
    public static synchronized Optional<ResourceId> pavingFor(ServerWorld world, String style) {
        return PAVING.computeIfAbsent(style, found -> measure(world, found));
    }

    /**
     * Se este bloco é rua de vila — de qualquer estilo.
     *
     * <p>Sem perguntar o estilo de propósito. Quem chama é a busca de
     * lote, que olha coluna por coluna e não tem o bioma em mãos; e a
     * confusão que isso poderia causar não existe: nem {@code dirt_path}
     * nem {@code smooth_sandstone} nascem sozinhos no mundo. Onde há um
     * deles, alguém calçou.
     */
    public static synchronized boolean isPaving(ServerWorld world, BlockState state) {
        if (everyPaving == null) {
            everyPaving = measureEvery(world);
        }

        return everyPaving.contains(state.getBlock());
    }

    /** Esquece o que foi medido. Chamado ao parar o servidor. */
    public static synchronized void clearAll() {
        PAVING.clear();
        everyPaving = null;
    }

    /**
     * O bloco mais frequente das peças de rua deste estilo.
     *
     * <p>Mais de uma peça é lida porque as pequenas mentem: a esquina do
     * deserto tem vinte e sete blocos de calçamento e nada mais, mas a
     * reta tem um arbusto morto junto — e uma peça que fosse só decoração
     * daria a resposta errada. A maioria entre várias não se engana.
     */
    private static Optional<ResourceId> measure(ServerWorld world, String style) {
        Map<ResourceId, Integer> seen = new HashMap<>();

        int read = 0;

        for (ResourceId street : VillageStructures.streetsFor(style)) {
            if (read >= TRIES) {
                break;
            }

            Optional<Blueprint> plan = StructureBlueprintReader.read(world, street);

            if (plan.isEmpty()) {
                continue;
            }

            read++;

            plan.get().materials().forEach((block, count) -> seen.merge(block, count, Integer::sum));
        }

        Optional<ResourceId> paving = seen.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);

        VillageColonyMod.LOGGER.info(
                "Village style {} paves its streets with {}", style, paving.orElse(null));

        return paving;
    }

    /**
     * O calçamento de todos os estilos que o catálogo conhece.
     *
     * <p>Os cinco do jogo estão escritos aqui, e é a única lista de
     * nomes desta classe. Não são materiais — são <b>biomas</b>, e o
     * bloco de cada um continua saindo do catálogo. Um estilo que o
     * catálogo não tenha simplesmente não contribui.
     */
    private static Set<Block> measureEvery(ServerWorld world) {
        Set<Block> blocks = new HashSet<>();

        for (String style : List.of("plains", "desert", "savanna", "taiga", "snowy")) {
            pavingFor(world, style)
                    .flatMap(MinecraftTypeAdapter::toBlock)
                    .ifPresent(blocks::add);
        }

        if (blocks.isEmpty()) {
            // Catálogo ausente. Sem isto a colônia deixaria de reconhecer
            // qualquer rua, e pararia de crescer em silêncio — pior que a
            // suposição que este ramo faz.
            MinecraftTypeAdapter.toBlock(VillagePalette.DIRT_PATH).ifPresent(blocks::add);

            VillageColonyMod.LOGGER.warn(
                    "No village street could be read — falling back to {}",
                    VillagePalette.DIRT_PATH);
        }

        return Set.copyOf(blocks);
    }
}
