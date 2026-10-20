package com.villagecolony.fabric.content;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CraftingTableBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Bancada do mod que abre a mesa de trabalho 3×3 para o jogador — carpinteiro, e os ofícios sem estação
 * Vanilla (lenhador, construtor, mineiro). Decisão do autor, 2026-10-20.
 *
 * <p><b>Por que uma tela própria.</b> O {@link CraftingScreenHandler} do Vanilla só se mantém aberto
 * diante de {@code Blocks.CRAFTING_TABLE} ({@code canUse(context, player, Blocks.CRAFTING_TABLE)},
 * conferido por {@code javap} no 1.21.1): com o bloco do mod, a tela abria e fechava no mesmo tique —
 * era o que a 0.3.63 entregava como "o jogador usa a bancada". O {@link Handler} confere o bloco do mod.
 * O tipo de tela segue o Vanilla, então o cliente não precisa de registro nenhum.
 */
public class BenchCraftingBlock extends CraftingTableBlock {

    public static final MapCodec<BenchCraftingBlock> CODEC = createCodec(BenchCraftingBlock::new);

    public BenchCraftingBlock(Settings settings) {
        super(settings);
    }

    @Override
    public MapCodec<? extends CraftingTableBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected NamedScreenHandlerFactory createScreenHandlerFactory(BlockState state, World world, BlockPos pos) {
        return new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, player) -> new Handler(syncId, inventory, ScreenHandlerContext.create(world, pos), this),
                getName());
    }

    /** A mesa de trabalho Vanilla, aberta diante da bancada do mod. */
    static final class Handler extends CraftingScreenHandler {

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
