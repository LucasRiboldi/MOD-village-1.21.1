package com.villagecolony.fabric.content;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LoomBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.LoomScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A bancada do pastor: abre o tear Vanilla para o jogador — decisão do autor, 2026-10-20. Tela própria
 * pelo mesmo motivo do {@link BenchCraftingBlock}: o {@link LoomScreenHandler} só fica aberto diante de
 * {@code Blocks.LOOM}.
 */
public class BenchLoomBlock extends LoomBlock {

    public BenchLoomBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected NamedScreenHandlerFactory createScreenHandlerFactory(BlockState state, World world, BlockPos pos) {
        return new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, player) -> new Handler(syncId, inventory, ScreenHandlerContext.create(world, pos), this),
                getName());
    }

    /** O tear Vanilla, aberto diante da bancada do pastor. */
    static final class Handler extends LoomScreenHandler {

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
