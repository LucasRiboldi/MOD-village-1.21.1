package com.villagecolony.fabric.integration;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * As sobras da árvore derrubada — Regra 378 (autor 26-11, notas do jogo da 0.4.145, J5 🅱️: "descobrir por que esse tronco
 * de árvore gigante está aqui" e "o lenhador deve quebrar todas folhas da árvore que ele cortou"; emenda a decisão de
 * 2026-09-15 de só derrubar o tronco). Depois do último tronco: o tronco da mesma espécie a até {@value #REACH} blocos
 * da árvore que ficou no ar, sem ligação com o chão, sai; e a copa natural ligada à árvore, que ficou a mais de
 * {@value #LEAF_DISTANCE} de qualquer tronco de pé e ia apodrecer, sai também. O que cai é devolvido a quem colheu.
 */
public final class TreeLeftovers {

    /** Até onde da árvore derrubada um tronco solto conta como galho dela. */
    static final int REACH = 3;

    /** A distância de tronco em que a folha do Vanilla ainda se segura. */
    static final int LEAF_DISTANCE = 6;

    /** O maior galho solto lido de uma vez. */
    private static final int BRANCH_LOGS = 64;

    private TreeLeftovers() {
    }

    /** Tira os galhos soltos e a copa que ia apodrecer; devolve o que caiu. */
    public static List<ItemStack> clear(ServerWorld world, TreeHarvester.Plan plan) {
        List<ItemStack> drops = new ArrayList<>();

        if (plan.isEmpty()) {
            return drops;
        }

        List<BlockPos> felled = new ArrayList<>(plan.blocks());
        Set<BlockPos> seen = new HashSet<>(felled);

        for (BlockPos log : List.copyOf(felled)) {
            for (BlockPos at : BlockPos.iterate(log.add(-REACH, -REACH, -REACH), log.add(REACH, REACH, REACH))) {
                if (seen.contains(at) || !TreeShape.isBlock(world, at, plan.species().log())) {
                    continue;
                }

                List<BlockPos> branch = TreeShape.connectedLogs(world, plan.species(), at.toImmutable(), BRANCH_LOGS);

                seen.addAll(branch);

                if (!grounded(world, branch) && branch.stream().allMatch(pos -> TreeShape.mayHarvest(world, pos))) {
                    for (BlockPos pos : branch) {
                        drops.addAll(TreeHarvester.breakOne(world, plan, pos));
                    }
                    felled.addAll(branch);
                }
            }
        }

        for (BlockPos leaf : looseCanopy(world, plan, felled)) {
            drops.addAll(TreeHarvester.breakOne(world, plan, leaf));
        }

        return drops;
    }

    /** Se algum tronco do galho se apoia em chão: bloco abaixo que não é tronco, folha nem ar. */
    private static boolean grounded(ServerWorld world, List<BlockPos> branch) {
        for (BlockPos log : branch) {
            BlockState below = world.getBlockState(log.down());

            if (!below.isAir() && !below.isIn(BlockTags.LOGS) && !below.isIn(BlockTags.LEAVES)) {
                return true;
            }
        }

        return false;
    }

    /** A copa natural ligada à árvore que ficou longe de todo tronco de pé. */
    private static List<BlockPos> looseCanopy(ServerWorld world, TreeHarvester.Plan plan, List<BlockPos> felled) {
        List<BlockPos> loose = new ArrayList<>();
        List<BlockPos> standing = standingLogs(world, felled);
        Set<BlockPos> seen = new HashSet<>(felled);
        Deque<BlockPos> queue = new ArrayDeque<>(felled);

        while (!queue.isEmpty() && loose.size() < TreeHarvester.MAX_LEAVES * 2) {
            BlockPos current = queue.removeFirst();

            for (BlockPos next : TreeShape.around(current)) {
                if (!seen.add(next) || !TreeShape.isNaturalLeaf(world, next, plan.species())) {
                    continue;
                }

                queue.add(next);

                if (standing.stream().noneMatch(log -> chebyshev(log, next) <= LEAF_DISTANCE)) {
                    loose.add(next);
                }
            }
        }

        return loose;
    }

    private static int chebyshev(BlockPos a, BlockPos b) {
        return Math.max(Math.abs(a.getX() - b.getX()), Math.max(Math.abs(a.getY() - b.getY()), Math.abs(a.getZ() - b.getZ())));
    }

    /** Os troncos de pé em volta da árvore derrubada, lidos uma vez: a copa a até {@value #LEAF_DISTANCE} deles fica. */
    private static List<BlockPos> standingLogs(ServerWorld world, List<BlockPos> felled) {
        int reach = LEAF_DISTANCE * 2;
        BlockPos min = felled.getFirst();
        BlockPos max = felled.getFirst();

        for (BlockPos log : felled) {
            min = new BlockPos(Math.min(min.getX(), log.getX()), Math.min(min.getY(), log.getY()),
                    Math.min(min.getZ(), log.getZ()));
            max = new BlockPos(Math.max(max.getX(), log.getX()), Math.max(max.getY(), log.getY()),
                    Math.max(max.getZ(), log.getZ()));
        }

        List<BlockPos> standing = new ArrayList<>();

        for (BlockPos at : BlockPos.iterate(min.add(-reach, -reach, -reach), max.add(reach, reach, reach))) {
            if (world.isChunkLoaded(at) && world.getBlockState(at).isIn(BlockTags.LOGS)) {
                standing.add(at.toImmutable());
            }
        }

        return standing;
    }
}
