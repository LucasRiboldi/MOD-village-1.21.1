package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * O que construir, bloco a bloco, sem dizer onde.
 *
 * <p>TASK-030. Um projeto é uma lista de posições <b>relativas</b> e o
 * bloco de cada uma. Onde ele será erguido é decisão de outra classe: o
 * mesmo projeto serve para toda casa que a colônia levantar.
 *
 * <p>Puro, como manda a ADR-005: fala de bloco por {@link ResourceId} e
 * nunca por {@code BlockState}. Quem lê a estrutura Vanilla e converte é
 * {@code fabric.integration.StructureBlueprintReader} — Construction-
 * System.md §"Fonte das Construções" diz que toda construção do MVP vem
 * do próprio jogo, do mesmo jeito que a receita da tábua vem.
 *
 * <p><b>O ar faz parte do projeto?</b> Não. Um projeto guarda só o que
 * há de colocar, e não o vazio dentro da casa. O vazio é assunto da
 * preparação do terreno — Construction-System.md §PREPARING —, e
 * guardá-lo aqui multiplicaria por dez o tamanho de um projeto para
 * dizer "aqui não vai nada".
 *
 * <p>Imutável. Um projeto é lido uma vez e usado muitas: duas obras
 * podem estar em curso ao mesmo tempo, e nenhuma delas pode alterar o
 * que a outra está seguindo.
 */
public final class Blueprint {

    private final ResourceId id;

    private final List<BlueprintBlock> blocks;

    private final ColonyPos size;

    private Blueprint(ResourceId id, List<BlueprintBlock> blocks, ColonyPos size) {
        this.id = id;
        this.blocks = blocks;
        this.size = size;
    }

    /**
     * Cria um projeto a partir dos blocos dele.
     *
     * <p>O tamanho é calculado, e não recebido: dois números que
     * deveriam concordar acabam discordando, e aqui um deles é dedutível
     * do outro.
     *
     * @param blocks posições relativas e o bloco de cada uma; a ordem é
     *     preservada, e é ela que a obra vai seguir
     * @throws IllegalArgumentException se a lista vier vazia — um projeto
     *     sem bloco algum é uma obra que termina antes de começar, e
     *     deixá-lo passar esconderia uma leitura de estrutura que falhou
     */
    public static Blueprint of(ResourceId id, List<BlueprintBlock> blocks) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(blocks, "blocks");

        if (blocks.isEmpty()) {
            throw new IllegalArgumentException("Blueprint has no blocks: " + id);
        }

        return new Blueprint(id, List.copyOf(blocks), sizeOf(blocks));
    }

    public ResourceId id() {
        return id;
    }

    /** Os blocos, na ordem em que serão colocados. Somente leitura. */
    public List<BlueprintBlock> blocks() {
        return blocks;
    }

    /**
     * Quanto o projeto ocupa, em blocos, nos três eixos.
     *
     * <p>Serve para perguntar ao mundo se cabe, antes de decidir o
     * lugar. É a caixa que envolve tudo — o projeto pode não preencher
     * todo o volume dela.
     */
    public ColonyPos size() {
        return size;
    }

    public int blockCount() {
        return blocks.size();
    }

    /**
     * Quantos blocos de cada tipo a obra pede — TASK-032.
     *
     * <p>É a lista de compras da construção, e o que ela devolve são
     * nomes do jogo, não os três recursos que a colônia conta. Traduzir
     * para {@code ResourceType} é trabalho da fronteira: só ela sabe que
     * {@code oak_planks} é o item que está no baú, e só ela sabe o que
     * fazer com a porta e a vidraça, que a colônia não produz.
     *
     * <p>Ordem de primeira aparição, para que a lista saia estável no
     * log. Depurar obra com ordem instável é sofrido — Debugging-
     * Strategy.md.
     */
    public Map<ResourceId, Integer> materials() {
        Map<ResourceId, Integer> tally = new LinkedHashMap<>();

        for (BlueprintBlock block : blocks) {
            tally.merge(block.block(), 1, Integer::sum);
        }

        return Map.copyOf(tally);
    }

    /**
     * A caixa que envolve todos os blocos.
     *
     * <p>Conta a partir de zero: um projeto cujo bloco mais distante
     * está em x=4 tem cinco blocos de largura. E não assume que a origem
     * seja o menor canto — uma estrutura lida do jogo pode vir com
     * posições negativas, e medir do menor ao maior é o que continua
     * certo nos dois casos.
     */
    private static ColonyPos sizeOf(List<BlueprintBlock> blocks) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (BlueprintBlock block : blocks) {
            ColonyPos offset = block.offset();

            minX = Math.min(minX, offset.x());
            minY = Math.min(minY, offset.y());
            minZ = Math.min(minZ, offset.z());
            maxX = Math.max(maxX, offset.x());
            maxY = Math.max(maxY, offset.y());
            maxZ = Math.max(maxZ, offset.z());
        }

        return new ColonyPos(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1);
    }

    @Override
    public String toString() {
        return "Blueprint[id=" + id
                + ", blocks=" + blocks.size()
                + ", size=" + size
                + "]";
    }
}
