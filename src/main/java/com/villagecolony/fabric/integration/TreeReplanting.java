package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Depois do corte: abrir o céu sobre a base e replantar a muda da mesma
 * espécie — separado de {@link TreeHarvester} em 2026-09-24, quando ele
 * passou de 500 linhas. Os comentários vieram junto sem mudança.
 */
final class TreeReplanting {

    private TreeReplanting() {
    }

    /**
     * Abre a coluna acima da muda.
     *
     * <p>A copa desta árvore já saiu com a colheita. O que pode ter
     * sobrado ali é folha de outra árvore — a vizinha cuja copa passa por
     * cima desta base — e é ela que impediria a muda de crescer.
     *
     * <p>Para no primeiro bloco que não seja folha nem ar. Um telhado,
     * uma ponte ou uma varanda do jogador acima da árvore encerra a
     * limpeza ali: a muda não vai crescer, e isso é problema dela, não
     * licença para abrir buraco em construção alheia.
     *
     * <p>Folha pendurada à mão encerra do mesmo jeito. Ela é construção
     * como qualquer outra — ver {@link TreeShape#isNaturalLeaf} —, e a única
     * diferença é que aqui vale a folha de qualquer espécie: a copa que
     * cobre esta base pode ser da árvore vizinha.
     *
     * <p>É o único lugar da colheita que quebra bloco que não é desta
     * árvore, e por isso o único que pergunta a
     * {@link BlockProtection}: a copa que passa por cima da muda pode ser
     * de uma árvore que o jogo gerou junto com a vila. A árvore desta
     * colheita não passa por lá, pela exceção do autor.
     */
    static void clearAbove(ServerWorld world, BlockPos base) {
        for (int height = 1; height <= TreeHarvester.SAPLING_CLEARANCE; height++) {
            BlockPos above = base.up(height);
            BlockState state = TreeShape.stateAt(world, above);

            if (state == null) {
                return;
            }

            if (state.isAir()) {
                continue;
            }

            if (!TreeShape.isAnyNaturalLeaf(state)) {
                return;
            }

            if (!BlockProtection.mayBreak(world, above, state)) {
                return;
            }

            world.removeBlock(above, false);
        }
    }

    /**
     * Muda da própria espécie no lugar da base, se o chão aceitar.
     *
     * <p>{@code canPlaceAt} é quem responde — a mesma pergunta que o
     * jogo faz quando o jogador tenta plantar. Repetir a regra aqui
     * seria inventar uma segunda verdade sobre o que é chão bom, e ela
     * envelheceria no primeiro bioma novo.
     */
    static void replant(ServerWorld world, TreeSpecies species, BlockPos base) {
        BlockState here = TreeShape.stateAt(world, base);

        if (here == null) {
            VillageColonyMod.LOGGER.info(
                    "No {} sapling at {} — the chunk went out from under it",
                    species,
                    base.toShortString());

            return;
        }

        if (!here.isAir()) {
            VillageColonyMod.LOGGER.info(
                    "No {} sapling at {} — {} is in the way",
                    species,
                    base.toShortString(),
                    here.getBlock());

            return;
        }

        BlockState sapling = species.sapling().getDefaultState();

        if (!sapling.canPlaceAt(world, base)) {
            VillageColonyMod.LOGGER.info(
                    "No {} sapling at {} — the ground will not take one",
                    species,
                    base.toShortString());

            return;
        }

        world.setBlockState(base, sapling);

        VillageColonyMod.LOGGER.info(
                "Planted a {} sapling at {}", species, base.toShortString());
    }
}
