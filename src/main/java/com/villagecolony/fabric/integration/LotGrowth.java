package com.villagecolony.fabric.integration;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.BiomeKeys;

/**
 * A vegetação que a casa fundadora tira do próprio lote — decisão do autor, 2026-10-20 (G1).
 *
 * <p>Na floresta, na cerejeira, no campo de cogumelos e na taiga nevada a bancada de simulação
 * mostrou a vila vista e a colônia adiada para sempre: todo lote tinha árvore, e o lote exigia ar.
 * Agora sai do lote o que o mundo plantou: tronco de árvore natural, folha natural, cogumelo
 * gigante, bambu e planta.
 *
 * <p><b>Tronco natural é o que sobe até folha natural</b> (não persistente — a única marca de
 * "posto à mão" que o mundo guarda, {@link BlockProtection#isPlayerPlaced}). A cabana de toras de
 * um jogador, coberta de tábua, não passa; tronco descascado também não, porque é trabalho de
 * alguém; e o tronco que nasce do chão de uma muda que a colônia plantou é do viveiro ou do bosque
 * dela, e fica ({@code ColonyTrees}).
 *
 * <p><b>Nas obras comuns, só nos biomas de árvores juntas</b> ({@link #clearsTreesAt}) — pedido
 * do autor, 2026-10-20: <i>"nos biomas com árvores muito juntas prever a limpeza da área de
 * construção"</i>. Na planície a regra antiga continua: lote com árvore é recusado e a colônia
 * procura outro, sem derrubar o que não planejou.
 */
final class LotGrowth {

    /** Quanto um tronco pode subir até a copa: o pinheiro gigante e a árvore da selva passam de 30. */
    private static final int TALLEST_TRUNK = 40;

    private LotGrowth() {
    }

    /** Bloco de vegetação que ocupa espaço: tronco, cogumelo gigante, bambu. Sem conferir se é natural. */
    static boolean isSolidGrowth(BlockState state) {
        return (state.isIn(BlockTags.LOGS) && !isStripped(state))
                || state.isOf(Blocks.MUSHROOM_STEM)
                || state.isOf(Blocks.RED_MUSHROOM_BLOCK)
                || state.isOf(Blocks.BROWN_MUSHROOM_BLOCK)
                || state.isOf(Blocks.BAMBOO)
                || state.isOf(Blocks.MANGROVE_ROOTS);
    }

    /**
     * Se este bioma é de árvores juntas, onde a obra comum limpa o próprio lote: florestas, taigas
     * e selvas pelas etiquetas do jogo (ADR-009), mais cerejeira e mangue, que não têm etiqueta.
     */
    static boolean clearsTreesAt(ServerWorld world, BlockPos pos) {
        return clearsTrees(world.getBiome(pos));
    }

    /** O mesmo, para um bioma — a auditoria de cobertura pergunta sem posição. */
    static boolean clearsTrees(net.minecraft.registry.entry.RegistryEntry<net.minecraft.world.biome.Biome> biome) {
        return biome.isIn(BiomeTags.IS_FOREST)
                || biome.isIn(BiomeTags.IS_TAIGA)
                || biome.isIn(BiomeTags.IS_JUNGLE)
                || biome.matchesKey(BiomeKeys.CHERRY_GROVE)
                || biome.matchesKey(BiomeKeys.MANGROVE_SWAMP);
    }

    /**
     * A régua barata da varredura: tronco, cogumelo, bambu ou folha natural, sem subir o tronco.
     * O volume da casa confere depois com {@link #isClearableOnSite}.
     */
    static boolean isLooseGrowth(BlockState state) {
        return isSolidGrowth(state) || (state.isIn(BlockTags.LEAVES) && !BlockProtection.isPlayerPlaced(state));
    }

    /** O mesmo de {@link #isClearable}, mais o que não é do mato: peça da vila gerada e obra da colônia. */
    static boolean isClearableOnSite(ServerWorld world, BlockPos pos, BlockState state) {
        return isClearable(world, pos, state)
                && !BlockProtection.isColonyBuilt(pos)
                && !BlockProtection.isVillageOriginal(world, pos);
    }

    /** Se este bloco é vegetação que a fundação pode tirar do lote. */
    static boolean isClearable(ServerWorld world, BlockPos pos, BlockState state) {
        if (LotGround.isNothing(state)) {
            return true;
        }

        if (state.isIn(BlockTags.LEAVES)) {
            return !BlockProtection.isPlayerPlaced(state);
        }

        if (state.isIn(BlockTags.LOGS)) {
            return !isStripped(state) && reachesNaturalLeaves(world, pos) && !growsFromNursery(world, pos);
        }

        return isSolidGrowth(state);
    }

    /**
     * A altura livre acima do chão de verdade, descontando tronco e cogumelo de pé nele.
     *
     * <p>{@code MOTION_BLOCKING_NO_LEAVES} já ignora folha; o tronco ele conta, e sem descontar o
     * lote debaixo de uma árvore teria o "chão" no alto da copa.
     */
    static int groundTop(ServerWorld world, int x, int z) {
        int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);

        while (top - 1 > world.getBottomY() && isSolidGrowth(world.getBlockState(new BlockPos(x, top - 1, z)))) {
            top--;
        }

        return top;
    }

    /** Se a coluna tem vegetação acima do chão — tronco, cogumelo ou copa. */
    static boolean hasGrowth(ServerWorld world, int x, int z) {
        int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);

        return top != groundTop(world, x, z) || world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z) != top;
    }

    /**
     * Tira a vegetação da coluna, do chão até {@code through}, e segue subindo enquanto houver
     * tronco ou copa, para não deixar árvore pendurada no ar. O tronco cai como item; o resto some.
     *
     * @return quantos blocos saíram
     */
    static int clearColumn(ServerWorld world, int x, int from, int through, int z) {
        int cleared = 0;

        for (int y = from; y <= through + TALLEST_TRUNK; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = world.getBlockState(pos);

            if (state.isAir()) {
                if (y > through) {
                    break;
                }

                continue;
            }

            if (!isClearableOnSite(world, pos, state)) {
                if (y > through) {
                    break;
                }

                continue;
            }

            if (state.isIn(BlockTags.LOGS)) {
                world.breakBlock(pos, true);
            } else {
                world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
            }

            cleared++;
        }

        return cleared;
    }

    /** O tronco sobe até a copa: folha natural em cima, ou do lado do último tronco (galho). */
    private static boolean reachesNaturalLeaves(ServerWorld world, BlockPos log) {
        BlockPos at = log;

        for (int step = 0; step < TALLEST_TRUNK && world.getBlockState(at.up()).isIn(BlockTags.LOGS); step++) {
            at = at.up();
        }

        if (isNaturalLeaf(world.getBlockState(at.up()))) {
            return true;
        }

        for (Direction side : Direction.Type.HORIZONTAL) {
            if (isNaturalLeaf(world.getBlockState(at.offset(side)))) {
                return true;
            }
        }

        return false;
    }

    /** O tronco nasce do chão de uma muda que a colônia plantou: é do viveiro ou do bosque (ColonyTrees). */
    private static boolean growsFromNursery(ServerWorld world, BlockPos log) {
        BlockPos at = log;

        for (int step = 0; step < TALLEST_TRUNK && world.getBlockState(at.down()).isIn(BlockTags.LOGS); step++) {
            at = at.down();
        }

        return com.villagecolony.fabric.world.ColonyTrees.isPlanted(at.down());
    }

    private static boolean isNaturalLeaf(BlockState state) {
        return state.isIn(BlockTags.LEAVES) && !BlockProtection.isPlayerPlaced(state);
    }

    private static boolean isStripped(BlockState state) {
        return Registries.BLOCK.getId(state.getBlock()).getPath().startsWith("stripped_");
    }
}
