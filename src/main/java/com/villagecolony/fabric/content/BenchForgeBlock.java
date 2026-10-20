package com.villagecolony.fabric.content;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * A forja do fundidor: uma fornalha de verdade para o jogador — funde, guarda itens, queima combustível
 * e derruba o conteúdo ao ser quebrada, como a Vanilla. Decisão do autor, 2026-10-20 (revê a de 15-10,
 * em que ela abria a mesa de trabalho para evitar o BlockEntity).
 *
 * <p>A fundição do aldeão continua sendo do fluxo do mod ({@code SmelterWork}); esta fornalha é a do
 * jogador. O {@link BenchForgeBlockEntity} é o {@code FurnaceBlockEntity} do Vanilla com tipo próprio —
 * o Vanilla valida o bloco do tipo e não aceita um bloco do mod no {@code BlockEntityType.FURNACE}.
 */
public class BenchForgeBlock extends AbstractFurnaceBlock {

    public static final MapCodec<BenchForgeBlock> CODEC = createCodec(BenchForgeBlock::new);

    public BenchForgeBlock(Settings settings) {
        super(settings);
    }

    @Override
    public MapCodec<BenchForgeBlock> getCodec() {
        return CODEC;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new BenchForgeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(world, type, ModContent.forgeEntity());
    }

    @Override
    protected void openScreen(World world, BlockPos pos, PlayerEntity player) {
        if (world.getBlockEntity(pos) instanceof BenchForgeBlockEntity forge) {
            player.openHandledScreen(forge);
            player.incrementStat(Stats.INTERACT_WITH_FURNACE);
        }
    }
}
