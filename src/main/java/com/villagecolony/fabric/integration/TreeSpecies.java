package com.villagecolony.fabric.integration;

import com.villagecolony.core.type.ResourceType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

import java.util.Optional;

/**
 * As árvores que o lenhador conhece.
 *
 * <p>Uma linha por espécie, e cada linha diz tudo o que a colheita
 * precisa saber: que tronco derrubar, que folha é copa dela, que muda
 * replantar e que recurso ela vira no estoque. Acrescentar uma árvore é
 * acrescentar uma linha — foi para isso que esta tabela existe.
 *
 * <p><b>Por que uma tabela e não a tag {@code minecraft:logs}.</b> A tag
 * inclui tronco descascado e bloco de madeira, que são material de
 * construção do jogador, não árvore. E ela não diz que muda replanta o
 * quê. A tabela custa oito linhas e não confunde a casa do jogador com
 * a floresta.
 *
 * <p><b>O que ainda não entrou, e por quê.</b> O Nether e o bambu ficam
 * de fora desta primeira volta, e não por esquecimento: caule carmesim e
 * distorcido não têm muda, têm fungo, e o fungo só vira árvore em nylium
 * e com farinha de osso — replantio com regra própria, que esta tabela
 * ainda não sabe expressar. Bambu não tem muda nenhuma: cresce da
 * própria base, e derrubá-lo inteiro impede que se reponha. Os dois
 * pedem um campo a mais aqui, e ele será acrescentado quando houver
 * colônia num bioma que os tenha.
 */
public enum TreeSpecies {

    OAK(Blocks.OAK_LOG, Blocks.OAK_LEAVES, Blocks.OAK_SAPLING, ResourceType.OAK_LOG,
            Blocks.OAK_PLANKS, ResourceType.OAK_PLANKS,
            Blocks.STRIPPED_OAK_LOG, ResourceType.STRIPPED_OAK_LOG),

    BIRCH(Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES, Blocks.BIRCH_SAPLING, ResourceType.BIRCH_LOG,
            Blocks.BIRCH_PLANKS, ResourceType.BIRCH_PLANKS,
            Blocks.STRIPPED_BIRCH_LOG, ResourceType.STRIPPED_BIRCH_LOG),

    SPRUCE(Blocks.SPRUCE_LOG, Blocks.SPRUCE_LEAVES, Blocks.SPRUCE_SAPLING,
            ResourceType.SPRUCE_LOG, Blocks.SPRUCE_PLANKS, ResourceType.SPRUCE_PLANKS,
            Blocks.STRIPPED_SPRUCE_LOG, ResourceType.STRIPPED_SPRUCE_LOG),

    JUNGLE(Blocks.JUNGLE_LOG, Blocks.JUNGLE_LEAVES, Blocks.JUNGLE_SAPLING,
            ResourceType.JUNGLE_LOG, Blocks.JUNGLE_PLANKS, ResourceType.JUNGLE_PLANKS,
            Blocks.STRIPPED_JUNGLE_LOG, ResourceType.STRIPPED_JUNGLE_LOG),

    ACACIA(Blocks.ACACIA_LOG, Blocks.ACACIA_LEAVES, Blocks.ACACIA_SAPLING,
            ResourceType.ACACIA_LOG, Blocks.ACACIA_PLANKS, ResourceType.ACACIA_PLANKS,
            Blocks.STRIPPED_ACACIA_LOG, ResourceType.STRIPPED_ACACIA_LOG),

    DARK_OAK(Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_LEAVES, Blocks.DARK_OAK_SAPLING,
            ResourceType.DARK_OAK_LOG, Blocks.DARK_OAK_PLANKS, ResourceType.DARK_OAK_PLANKS,
            Blocks.STRIPPED_DARK_OAK_LOG, ResourceType.STRIPPED_DARK_OAK_LOG),

    CHERRY(Blocks.CHERRY_LOG, Blocks.CHERRY_LEAVES, Blocks.CHERRY_SAPLING,
            ResourceType.CHERRY_LOG, Blocks.CHERRY_PLANKS, ResourceType.CHERRY_PLANKS,
            Blocks.STRIPPED_CHERRY_LOG, ResourceType.STRIPPED_CHERRY_LOG),

    /**
     * Mangue, o caso difícil do Overworld.
     *
     * <p>O que se replanta não é muda, é propágulo, e ele quer lama ou
     * água rasa. {@code canPlaceAt} responde por nós — a mesma pergunta
     * que o jogo faz ao jogador — então em chão errado a colheita
     * simplesmente não replanta, como já acontece com muda em pedra.
     */
    MANGROVE(Blocks.MANGROVE_LOG, Blocks.MANGROVE_LEAVES, Blocks.MANGROVE_PROPAGULE,
            ResourceType.MANGROVE_LOG, Blocks.MANGROVE_PLANKS, ResourceType.MANGROVE_PLANKS,
            Blocks.STRIPPED_MANGROVE_LOG, ResourceType.STRIPPED_MANGROVE_LOG);

    private final Block log;
    private final Block leaves;
    private final Block sapling;
    private final ResourceType resource;
    private final Block planks;
    private final ResourceType plankResource;

    private final Block strippedLog;

    private final ResourceType strippedResource;

    TreeSpecies(
            Block log,
            Block leaves,
            Block sapling,
            ResourceType resource,
            Block planks,
            ResourceType plankResource,
            Block strippedLog,
            ResourceType strippedResource) {

        this.log = log;
        this.leaves = leaves;
        this.sapling = sapling;
        this.resource = resource;
        this.planks = planks;
        this.plankResource = plankResource;
        this.strippedLog = strippedLog;
        this.strippedResource = strippedResource;
    }

    /**
     * A viga descascada desta espécie — 2026-09-10.
     *
     * <p>Entrou aqui, e não numa segunda lista, pelo motivo que o
     * {@code MinecraftTypeAdapter} já dá para o tronco e a tábua: duas
     * listas divergem no dia em que alguém lembra de uma e esquece a
     * outra. Acrescentar uma árvore aqui a faz contar sozinha no
     * estoque, e é isso que faltava à viga — a colônia não sabia que
     * lhe faltava porque a viga não era recurso.
     */
    public Block strippedLog() {
        return strippedLog;
    }

    /** O recurso da viga descascada desta espécie. */
    public ResourceType strippedResource() {
        return strippedResource;
    }

    public Block log() {
        return log;
    }

    public Block leaves() {
        return leaves;
    }

    public Block sapling() {
        return sapling;
    }

    public ResourceType resource() {
        return resource;
    }

    /**
     * A tábua desta espécie.
     *
     * <p>Entrou na Fase 9. O mod não usa este bloco para fabricar — quem
     * diz o que um tronco vira é a receita do próprio jogo —, mas
     * precisa dele para <b>contar</b> o resultado: sem isto, o fabricante
     * transformaria tronco em tábua que a colônia não enxerga, e o
     * estoque passaria a mentir.
     */
    public Block planks() {
        return planks;
    }

    /** O recurso que a tábua desta espécie vira no estoque. */
    public ResourceType plankResource() {
        return plankResource;
    }

    /** A espécie deste tronco, se for tronco de árvore conhecida. */
    public static Optional<TreeSpecies> ofLog(BlockState state) {
        if (state == null) {
            return Optional.empty();
        }

        for (TreeSpecies species : values()) {
            if (state.isOf(species.log)) {
                return Optional.of(species);
            }
        }

        return Optional.empty();
    }

    /** Se este bloco é tronco de alguma árvore conhecida. */
    public static boolean isLog(BlockState state) {
        return ofLog(state).isPresent();
    }
}
