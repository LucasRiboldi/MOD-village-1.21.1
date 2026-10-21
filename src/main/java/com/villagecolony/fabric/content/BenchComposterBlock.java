package com.villagecolony.fabric.content;

import net.minecraft.block.BlockState;
import net.minecraft.block.ComposterBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

/**
 * A bancada do fazendeiro: funciona como a composteira Vanilla (o jogador põe matéria orgânica e tira
 * farinha de osso; funis entram e saem) — decisão do autor, 2026-10-20. A composteira não tem tela, então
 * não há o problema do {@code canUse}. O que muda é a forma: o Vanilla é um caixote oco, aqui é um cubo
 * cheio, para casar com o modelo e as 6 faces.
 */
public class BenchComposterBlock extends ComposterBlock {

    public BenchComposterBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.fullCube();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.fullCube();
    }

    @Override
    protected VoxelShape getRaycastShape(BlockState state, BlockView world, BlockPos pos) {
        return VoxelShapes.fullCube();
    }
}
