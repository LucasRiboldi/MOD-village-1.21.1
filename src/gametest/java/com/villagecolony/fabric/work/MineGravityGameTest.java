package com.villagecolony.fabric.work;

import com.villagecolony.fabric.world.MineFill;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

/**
 * A mina abre caminho em areia sem a areia voltar — pedido do autor, 2026-10-20: o bloco que cai acima
 * do alvo é compactado em arenito antes de o alvo sair, e o enchimento do deserto é arenito.
 */
public class MineGravityGameTest implements FabricGameTest {

    /** Coluna de quatro areias sobre o alvo de areia, apoiada em pedra. */
    private static BlockPos sandColumn(TestContext context, int x) {
        ServerWorld world = context.getWorld();
        BlockPos target = context.getAbsolutePos(new BlockPos(x, 2, 2));

        world.setBlockState(target.down(), Blocks.STONE.getDefaultState());

        for (int dy = 0; dy <= 4; dy++) {
            world.setBlockState(target.up(dy), Blocks.SAND.getDefaultState());
        }

        return target;
    }

    private static boolean nothingFalling(ServerWorld world, BlockPos at) {
        return world.getEntitiesByType(EntityType.FALLING_BLOCK, new Box(at).expand(3), entity -> true).isEmpty();
    }

    private static void clear(TestContext context, BlockPos target) {
        for (BlockPos pos : BlockPos.iterate(target.add(0, -1, 0), target.add(0, 6, 0))) {
            context.getWorld().setBlockState(pos, Blocks.AIR.getDefaultState());
        }
    }

    /** Escorado: o alvo sai, o teto vira arenito, e nada cai no lugar minerado. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_gravity", tickLimit = 40)
    public void aSandCeilingIsShoredBeforeTheBlockUnderItLeaves(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos target = sandColumn(context, 1);

        context.assertTrue(MineSettling.shoreUp(world, target), "o teto de areia não foi escorado");
        world.removeBlock(target, false);

        context.waitAndRun(10, () -> {
            try {
                context.assertTrue(world.getBlockState(target).isAir(),
                        "a areia voltou para o lugar minerado: " + world.getBlockState(target).getBlock());
                context.assertTrue(world.getBlockState(target.up()).isOf(Blocks.SANDSTONE),
                        "o teto não virou arenito: " + world.getBlockState(target.up()).getBlock());
                context.assertTrue(world.getBlockState(target.up(2)).isOf(Blocks.SAND),
                        "a coluna de cima não ficou apoiada");
                context.assertTrue(nothingFalling(world, target), "ainda há bloco caindo");
            } finally {
                clear(context, target);
            }

            context.complete();
        });
    }

    /** Controle: sem escorar, a areia de cima ocupa o lugar minerado — o retrabalho que o escoramento evita. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_gravity", tickLimit = 60)
    public void withoutShoringTheSandRefillsTheMinedCell(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos target = sandColumn(context, 4);

        world.removeBlock(target, false);

        context.waitAndRun(40, () -> {
            try {
                context.assertTrue(world.getBlockState(target).isOf(Blocks.SAND),
                        "controle: a areia não caiu no lugar minerado — o cenário não mede nada");
            } finally {
                clear(context, target);
            }

            context.complete();
        });
    }

    /** Areia suspeita é de arqueologia: não se compacta. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_gravity")
    public void suspiciousSandIsNeverCompacted(TestContext context) {
        context.assertTrue(MineFill.compacted(Blocks.SUSPICIOUS_SAND.getDefaultState()).isEmpty(),
                "a areia suspeita virou arenito");
        context.assertTrue(MineFill.compacted(Blocks.WHITE_CONCRETE_POWDER.getDefaultState()).isEmpty(),
                "o pó de concreto do jogador virou outra coisa");
        context.assertTrue(MineFill.compacted(Blocks.GRAVEL.getDefaultState()).filter(Blocks.COBBLESTONE::equals).isPresent(),
                "o cascalho não vira pedregulho");
        context.complete();
    }

    /** Uma coluna de pedra na arena, com o que for pedido acima do alvo, em {@code (x, 2, 4)}. */
    private static BlockPos column(TestContext context, int x, net.minecraft.block.BlockState... above) {
        ServerWorld world = context.getWorld();
        BlockPos target = context.getAbsolutePos(new BlockPos(x, 2, 4));

        world.setBlockState(target.down(), Blocks.STONE.getDefaultState());
        world.setBlockState(target, Blocks.STONE.getDefaultState());

        for (int dy = 0; dy < above.length; dy++) {
            world.setBlockState(target.up(dy + 1), above[dy], net.minecraft.block.Block.FORCE_STATE);
        }

        return target;
    }

    /**
     * O mineiro não quebra o bloco com cascalho pendurado acima, através do ar — jogo da 0.3.108: o
     * cascalho de y 56–57 caiu pelo vão de y 55 sobre o mineiro que acabara de cavar y 54, e o matou; o
     * escoramento só olha o bloco logo acima. Água e lava acima, pelo vão, também seguram o bloco.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_gravity")
    public void aBlockUnderLooseGravelWaterOrLavaIsNotBroken(TestContext context) {
        ServerWorld world = context.getWorld();
        var air = Blocks.AIR.getDefaultState();
        BlockPos hanging = column(context, 1, air, Blocks.GRAVEL.getDefaultState(), Blocks.GRAVEL.getDefaultState());
        BlockPos water = column(context, 3, air, air, Blocks.WATER.getDefaultState());
        BlockPos lava = column(context, 5, Blocks.LAVA.getDefaultState());
        BlockPos roofed = column(context, 7, Blocks.STONE.getDefaultState(), Blocks.GRAVEL.getDefaultState());
        BlockPos shored = column(context, 9, Blocks.GRAVEL.getDefaultState(), Blocks.GRAVEL.getDefaultState());
        BlockPos open = column(context, 11, air, air, air);

        try {
            context.assertTrue(MineSettling.looseAbove(world, hanging), "o cascalho pendurado acima do vão passou");
            context.assertTrue(MineSettling.looseAbove(world, water), "a água acima do vão passou");
            context.assertTrue(MineSettling.looseAbove(world, lava), "a lava logo acima passou");
            context.assertFalse(MineSettling.looseAbove(world, roofed), "a pedra firme acima segura o cascalho");
            context.assertFalse(MineSettling.looseAbove(world, shored),
                    "o cascalho logo acima é escorado antes de o alvo sair, como pediu o autor em 2026-10-20");
            context.assertFalse(MineSettling.looseAbove(world, open), "ar acima não segura nada");
        } finally {
            for (BlockPos target : new BlockPos[] {hanging, water, lava, roofed, shored, open}) {
                for (BlockPos pos : BlockPos.iterate(target.down(), target.up(4))) {
                    world.setBlockState(pos, Blocks.AIR.getDefaultState(), net.minecraft.block.Block.FORCE_STATE);
                }
            }
        }

        context.complete();
    }

    /** O enchimento da mina é a pedra do bioma: arenito no deserto, pedregulho na planície. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_gravity")
    public void theDesertFillsWithSandstone(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos plains = context.getAbsolutePos(new BlockPos(1, 1, 1)).add(1_000_000, 0, 1_600_000);
        BlockPos desert = plains.add(0, 0, 48);

        for (BlockPos corner : new BlockPos[] {desert.add(-8, 0, -8), desert.add(8, 0, 8), desert.add(-8, 0, 8),
                desert.add(8, 0, -8), plains}) {
            world.getChunk(corner);
        }

        world.getServer().getCommandManager().executeWithPrefix(world.getServer().getCommandSource().withSilent(),
                "fillbiome " + (desert.getX() - 8) + " " + (desert.getY() - 4) + " " + (desert.getZ() - 8) + " "
                        + (desert.getX() + 8) + " " + (desert.getY() + 4) + " " + (desert.getZ() + 8) + " minecraft:desert");

        context.assertTrue(MineFill.fillAt(world, desert).isOf(Blocks.SANDSTONE),
                "o enchimento do deserto é " + MineFill.fillAt(world, desert).getBlock());
        context.assertTrue(MineFill.fillAt(world, plains).isOf(Blocks.COBBLESTONE),
                "controle: o enchimento da planície é " + MineFill.fillAt(world, plains).getBlock());
        context.complete();
    }
}
