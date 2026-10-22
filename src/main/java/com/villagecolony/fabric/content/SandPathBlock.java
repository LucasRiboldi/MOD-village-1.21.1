package com.villagecolony.fabric.content;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.event.GameEvent;

/**
 * O caminho de areia — autor, 2026-10-22: <i>"um bloco idêntico ao caminho de terra, com as mesmas
 * características do Vanilla, porém com a cor do arenito"</i>, o caminho exclusivo da vila do deserto.
 *
 * <p>Cópia do {@code DirtPathBlock} do Vanilla 1.21.1: quinze dezesseis avos de altura, transparência
 * lateral, e volta a ser o chão de baixo quando algo sólido o cobre — areia, aqui, e não terra. A cópia é
 * por composição e não por herança porque o codec do {@code DirtPathBlock} é tipado nele.
 */
public class SandPathBlock extends Block {

    public static final MapCodec<SandPathBlock> CODEC = createCodec(SandPathBlock::new);

    protected static final VoxelShape SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 15.0, 16.0);

    public SandPathBlock(Settings settings) {
        super(settings);
    }

    @Override
    public MapCodec<SandPathBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected boolean hasSidedTransparency(BlockState state) {
        return true;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return !getDefaultState().canPlaceAt(ctx.getWorld(), ctx.getBlockPos())
                ? Block.pushEntitiesUpBeforeBlockChange(getDefaultState(), Blocks.SAND.getDefaultState(), ctx.getWorld(), ctx.getBlockPos())
                : super.getPlacementState(ctx);
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
            WorldAccess world, BlockPos pos, BlockPos neighborPos) {

        if (direction == Direction.UP && !state.canPlaceAt(world, pos)) {
            world.scheduleBlockTick(pos, this, 1);
        }

        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        BlockState sand = Block.pushEntitiesUpBeforeBlockChange(state, Blocks.SAND.getDefaultState(), world, pos);

        world.setBlockState(pos, sand);
        world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(null, sand));
    }

    @Override
    protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockState above = world.getBlockState(pos.up());

        return !above.isSolid() || above.getBlock() instanceof FenceGateBlock;
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canPathfindThrough(BlockState state, NavigationType type) {
        return false;
    }
}
