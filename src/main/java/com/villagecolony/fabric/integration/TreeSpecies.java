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

    OAK(Blocks.OAK_LOG, Blocks.OAK_LEAVES, Blocks.OAK_SAPLING, ResourceType.OAK_LOG),

    BIRCH(Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES, Blocks.BIRCH_SAPLING, ResourceType.BIRCH_LOG),

    SPRUCE(Blocks.SPRUCE_LOG, Blocks.SPRUCE_LEAVES, Blocks.SPRUCE_SAPLING,
            ResourceType.SPRUCE_LOG),

    JUNGLE(Blocks.JUNGLE_LOG, Blocks.JUNGLE_LEAVES, Blocks.JUNGLE_SAPLING,
            ResourceType.JUNGLE_LOG),

    ACACIA(Blocks.ACACIA_LOG, Blocks.ACACIA_LEAVES, Blocks.ACACIA_SAPLING,
            ResourceType.ACACIA_LOG),

    DARK_OAK(Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_LEAVES, Blocks.DARK_OAK_SAPLING,
            ResourceType.DARK_OAK_LOG),

    CHERRY(Blocks.CHERRY_LOG, Blocks.CHERRY_LEAVES, Blocks.CHERRY_SAPLING,
            ResourceType.CHERRY_LOG),

    /**
     * Mangue, o caso difícil do Overworld.
     *
     * <p>O que se replanta não é muda, é propágulo, e ele quer lama ou
     * água rasa. {@code canPlaceAt} responde por nós — a mesma pergunta
     * que o jogo faz ao jogador — então em chão errado a colheita
     * simplesmente não replanta, como já acontece com muda em pedra.
     */
    MANGROVE(Blocks.MANGROVE_LOG, Blocks.MANGROVE_LEAVES, Blocks.MANGROVE_PROPAGULE,
            ResourceType.MANGROVE_LOG);

    private final Block log;
    private final Block leaves;
    private final Block sapling;
    private final ResourceType resource;

    TreeSpecies(Block log, Block leaves, Block sapling, ResourceType resource) {
        this.log = log;
        this.leaves = leaves;
        this.sapling = sapling;
        this.resource = resource;
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
