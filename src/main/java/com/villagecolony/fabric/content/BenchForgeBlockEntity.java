package com.villagecolony.fabric.content;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.recipe.RecipeType;
import net.minecraft.screen.FurnaceScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * O conteúdo da forja do fundidor ({@link BenchForgeBlock}): o mesmo da fornalha Vanilla — receitas de
 * fundição, combustível, experiência —, com tipo de BlockEntity próprio. A tela é a da fornalha Vanilla,
 * e ela confere o inventário (este BlockEntity), não o bloco; por isso não precisa de tela própria.
 */
public class BenchForgeBlockEntity extends AbstractFurnaceBlockEntity {

    public BenchForgeBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.forgeEntity(), pos, state, RecipeType.SMELTING);
    }

    @Override
    protected Text getContainerName() {
        return Text.translatable("block.villagecolony.blacksmith_forge");
    }

    @Override
    protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
        return new FurnaceScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }
}
