package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.integration.BlockProtection;
import com.villagecolony.fabric.integration.VillageBiomes;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * As peças funcionais da obra: cama, porta e baú — Regras 21/32 resolvidas pelo autor em 2026-10-22 (C-02 do
 * estudo v2). A obra não termina sem elas: em vez de pular a peça sem apoio, a colônia dá apoio a ela — o chão
 * do bioma embaixo, onde ali há ar ou planta — e a põe. As demais peças mantêm as saídas de sempre (pular na
 * 5ª falha, largar as sem apoio, surgir a inalcançável), para não haver obra eterna; e a funcional também cai
 * nelas, como último recurso, só quando o apoio é impossível (algo que não se tira está embaixo), com
 * {@code VC_FUNCTIONAL_MISSING} no log.
 */
final class FunctionalPieces {

    private FunctionalPieces() {
    }

    static boolean isFunctional(BlockState state) {
        return state.isIn(BlockTags.BEDS) || state.isIn(BlockTags.DOORS)
                || state.getBlock() instanceof ChestBlock || state.isOf(Blocks.BARREL);
    }

    /** Pelo nome, para quem só tem a peça da planta (a obra que larga as sem apoio). */
    static boolean isFunctional(ResourceId piece) {
        String path = piece.path();

        return path.endsWith("_bed") || path.endsWith("_door") || path.endsWith("chest") || path.equals("barrel");
    }

    /**
     * Dá apoio à peça funcional em {@code target}: põe o chão do bioma embaixo dela, se ali há ar ou planta e
     * nada protegido.
     *
     * @return se agora a peça pode ser posta
     */
    static boolean support(ServerWorld world, BlockPos target, BlockState state) {
        if (state.canPlaceAt(world, target)) {
            return true;
        }

        BlockPos below = target.down();
        BlockState under = world.getBlockState(below);

        // Só sobre vazio: o que está embaixo e não é ar nem planta fica — Regra 3.
        if (!under.isReplaceable() || !under.getFluidState().isEmpty()
                || BlockProtection.isColonyBuilt(below) || BlockProtection.isVillageOriginal(world, below)) {
            return false;
        }

        world.setBlockState(below, VillageBiomes.foundationGroundAt(world, below).getDefaultState(), Block.NOTIFY_ALL);
        VillageColonyMod.LOGGER.info("The {} at {} had nothing to stand on — the ground under it was laid",
                state.getBlock().getName().getString(), target.toShortString());

        return state.canPlaceAt(world, target);
    }

    /** A peça funcional que ficou de fora por não haver como apoiá-la. */
    static void missing(ResourceId piece, BlockPos target, String why) {
        VillageColonyMod.LOGGER.warn("VC_FUNCTIONAL_MISSING version=1 piece={} at={} reason={}",
                piece, target.toShortString(), why);
    }
}
