package com.villagecolony.fabric.content;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.StonecutterBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.StonecutterScreenHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * O cortador do pedreiro: abre o cortador de pedra Vanilla para o jogador, num cubo inteiro — decisão do
 * autor, 2026-10-20. O Vanilla tem forma baixa (9/16); aqui a colisão é de cubo cheio, para casar com o
 * modelo e as 6 faces. Tela própria pelo mesmo motivo do {@link BenchCraftingBlock}: o
 * {@link StonecutterScreenHandler} só fica aberto diante de {@code Blocks.STONECUTTER}.
 */
public class BenchStonecutterBlock extends StonecutterBlock {

    public BenchStonecutterBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.fullCube();
    }

    @Override
    protected NamedScreenHandlerFactory createScreenHandlerFactory(BlockState state, World world, BlockPos pos) {
        return new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, player) -> new Handler(syncId, inventory, ScreenHandlerContext.create(world, pos), this),
                getName());
    }

    /** O cortador de pedra Vanilla, aberto diante do cortador do mod. */
    static final class Handler extends StonecutterScreenHandler {

        private final ScreenHandlerContext context;

        private final Block bench;

        Handler(int syncId, PlayerInventory inventory, ScreenHandlerContext context, Block bench) {
            super(syncId, inventory, context);
            this.context = context;
            this.bench = bench;
        }

        @Override
        public boolean canUse(PlayerEntity player) {
            return canUse(context, player, bench);
        }
    }
}
