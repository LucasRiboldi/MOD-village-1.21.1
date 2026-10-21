package com.villagecolony.fabric.content;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ComposterBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameMode;

/**
 * As bancadas do mod funcionam para o jogador como a estação Vanilla do ofício — decisão do autor,
 * 2026-10-20.
 *
 * <p>O teste das telas tem controle: a tela Vanilla no mesmo bloco precisa <b>fechar</b>. Era o defeito da
 * 0.3.63 (a tela abria e fechava no mesmo tique), e sem o controle o teste passaria mesmo que a checagem
 * não medisse nada.
 */
public class BenchUseGameTest implements FabricGameTest {

    private static final BlockPos AT = new BlockPos(2, 1, 2);

    /** O jogador de teste ao lado do bloco: a tela confere a distância. */
    private static PlayerEntity playerBeside(TestContext context, BlockPos absolute) {
        PlayerEntity player = context.createMockPlayer(GameMode.SURVIVAL);
        player.setPosition(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 1.5);

        return player;
    }

    private static boolean screenStaysOpen(TestContext context, Block bench) {
        BlockPos absolute = context.getAbsolutePos(AT);
        context.getWorld().setBlockState(absolute, bench.getDefaultState());

        PlayerEntity player = playerBeside(context, absolute);
        NamedScreenHandlerFactory factory =
                context.getWorld().getBlockState(absolute).createScreenHandlerFactory(context.getWorld(), absolute);

        context.assertTrue(factory != null, "a bancada " + bench + " não abre tela nenhuma");

        ScreenHandler handler = factory.createMenu(0, player.getInventory(), player);

        return handler != null && handler.canUse(player);
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "bench_use_screens", tickLimit = 5)
    public void everyScreenBenchStaysOpenForThePlayer(TestContext context) {
        BlockPos absolute = context.getAbsolutePos(AT);

        // Controle: a mesa Vanilla diante do bloco do mod fecha — é o defeito que a tela própria corrige.
        context.getWorld().setBlockState(absolute, ModContent.CARPENTER_BENCH.getDefaultState());
        context.assertFalse(
                new CraftingScreenHandler(0, playerBeside(context, absolute).getInventory(),
                        ScreenHandlerContext.create(context.getWorld(), absolute))
                        .canUse(playerBeside(context, absolute)),
                "controle: a tela Vanilla devia fechar diante do bloco do mod");

        for (Block bench : new Block[] {
                ModContent.CARPENTER_BENCH, ModContent.LUMBERJACK_BENCH, ModContent.BUILDER_BENCH,
                ModContent.MINER_BENCH, ModContent.MASON_CUTTER, ModContent.SHEPHERD_BENCH}) {
            context.assertTrue(screenStaysOpen(context, bench), "a tela de " + bench + " fecha sozinha");
        }

        context.getWorld().setBlockState(absolute, Blocks.AIR.getDefaultState());
        context.complete();
    }

    /** A forja do fundidor funde como a fornalha Vanilla: minério e carvão viram lingote. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "bench_use_forge", tickLimit = 260)
    public void theForgeSmeltsLikeAFurnace(TestContext context) {
        BlockPos absolute = context.getAbsolutePos(AT);
        context.getWorld().setBlockState(absolute, ModContent.BLACKSMITH_FORGE.getDefaultState());

        context.assertTrue(context.getWorld().getBlockEntity(absolute) instanceof BenchForgeBlockEntity,
                "a forja não tem o conteúdo de fornalha");

        BenchForgeBlockEntity forge = (BenchForgeBlockEntity) context.getWorld().getBlockEntity(absolute);
        forge.setStack(0, new ItemStack(Items.RAW_IRON));
        forge.setStack(1, new ItemStack(Items.COAL));

        // 200 tiques de fundição na fornalha Vanilla.
        context.waitAndRun(220, () -> {
            try {
                context.assertTrue(forge.getStack(2).isOf(Items.IRON_INGOT),
                        "a forja não fundiu: saída = " + forge.getStack(2));
            } finally {
                forge.clear();
                context.getWorld().setBlockState(absolute, Blocks.AIR.getDefaultState());
            }

            context.complete();
        });
    }

    /** A bancada do fazendeiro composta como a composteira Vanilla. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "bench_use_composter", tickLimit = 5)
    public void theFarmerBenchComposts(TestContext context) {
        BlockPos absolute = context.getAbsolutePos(AT);
        context.getWorld().setBlockState(absolute, ModContent.FARMER_BENCH.getDefaultState());

        PlayerEntity player = playerBeside(context, absolute);
        // Torta de abóbora: chance 1,0 de subir o nível, sem sorteio no teste.
        ItemStack pie = new ItemStack(Items.PUMPKIN_PIE);
        context.getWorld().getBlockState(absolute).onUseWithItem(pie, context.getWorld(), player, Hand.MAIN_HAND,
                new BlockHitResult(absolute.toCenterPos(), Direction.UP, absolute, false));

        int level = context.getWorld().getBlockState(absolute).get(ComposterBlock.LEVEL);

        context.getWorld().setBlockState(absolute, Blocks.AIR.getDefaultState());
        context.assertTrue(level == 1, "a composteira do mod não compostou: nível " + level);
        context.complete();
    }

    /** Quebrar qualquer bancada devolve o bloco (antes não havia tabela de drop). */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "bench_use_drops", tickLimit = 5)
    public void everyBenchDropsItself(TestContext context) {
        BlockPos absolute = context.getAbsolutePos(AT);
        Box around = new Box(absolute).expand(2);

        for (Block bench : ModContent.benches().values()) {
            context.getWorld().setBlockState(absolute, bench.getDefaultState());
            context.getWorld().breakBlock(absolute, true);

            boolean dropped = !context.getWorld().getEntitiesByClass(ItemEntity.class, around,
                    item -> item.getStack().isOf(bench.asItem())).isEmpty();

            context.getWorld().getEntitiesByClass(ItemEntity.class, around, item -> true).forEach(ItemEntity::discard);
            context.assertTrue(dropped, "quebrar " + bench + " não devolveu o bloco");
        }

        context.complete();
    }
}
