package com.villagecolony.fabric.world;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.BiomeKeys;

import java.util.Optional;

/**
 * O bloco que a mina usa para tapar, escorar e fazer chão — pedido do autor, 2026-10-20: <i>"utilizar
 * a opção dele substituir os blocos necessários por arenito duro para poder caminhar, tapar buracos e
 * se locomover adequadamente"</i>.
 *
 * <p>Pacote à parte pelo mesmo motivo de {@link GeneratedVillages}: piso, selo de água, beirada, boca
 * e fuga moram em pacotes diferentes, e todos perguntam aqui sem criar ciclo.
 */
public final class MineFill {

    private MineFill() {
    }

    /**
     * O bloco de enchimento deste lugar: arenito no deserto e na praia, arenito vermelho nos ermos,
     * pedregulho no resto — a pedra que o bioma tem, e que não cai.
     */
    public static BlockState fillAt(ServerWorld world, BlockPos at) {
        return fillFor(world.getBiome(at));
    }

    /** O mesmo, para um bioma — a auditoria de cobertura pergunta sem posição. */
    public static BlockState fillFor(net.minecraft.registry.entry.RegistryEntry<net.minecraft.world.biome.Biome> biome) {
        if (biome.matchesKey(BiomeKeys.DESERT) || biome.isIn(BiomeTags.IS_BEACH)) {
            return Blocks.SANDSTONE.getDefaultState();
        }

        if (biome.isIn(BiomeTags.IS_BADLANDS)) {
            return Blocks.RED_SANDSTONE.getDefaultState();
        }

        return Blocks.COBBLESTONE.getDefaultState();
    }

    /**
     * O bloco que cai, compactado no mesmo material e firme: areia vira arenito, areia vermelha vira
     * arenito vermelho, cascalho vira pedregulho. Vazio para o que não é do mundo natural ou não
     * deve ser tocado: areia e cascalho suspeitos (arqueologia), pó de concreto (é do jogador).
     */
    public static Optional<Block> compacted(BlockState state) {
        if (!(state.getBlock() instanceof FallingBlock)) {
            return Optional.empty();
        }

        if (state.isOf(Blocks.SAND)) {
            return Optional.of(Blocks.SANDSTONE);
        }

        if (state.isOf(Blocks.RED_SAND)) {
            return Optional.of(Blocks.RED_SANDSTONE);
        }

        if (state.isOf(Blocks.GRAVEL)) {
            return Optional.of(Blocks.COBBLESTONE);
        }

        return Optional.empty();
    }
}
