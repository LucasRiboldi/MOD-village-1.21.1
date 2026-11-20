package com.villagecolony.fabric.integration;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Regra 345, telemetria F1 a F3b: o mod percebe o conflito com o jogador. O jogador desfaz o mod ({@code VC_PLAYER_UNDO}),
 * o mod desfaz o jogador ({@code VC_MOD_OVERRIDE}) e, o mais forte, o jogador refaz o que tinha feito depois de o mod
 * desfazer ({@code VC_PLAYER_INSIST}): o ponto de atenção que vira item a corrigir.
 */
public class EditLedgerGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "edit_ledger", tickLimit = 20)
    public void thePlayerRedoingWhatTheModUndidIsAnInsistence(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos wall = context.getAbsolutePos(new BlockPos(2, 2, 2));

        try {
            EditLedger.clear();

            // O jogador abre uma janela na parede, o reparo a fecha, o jogador abre de novo.
            EditLedger.player(world, wall, Blocks.OAK_PLANKS.getDefaultState(), Blocks.AIR.getDefaultState());
            EditLedger.mod(world, wall, Blocks.OAK_PLANKS.getDefaultState(), "obra");
            List<String> events = EditLedger.player(world, wall, Blocks.OAK_PLANKS.getDefaultState(),
                    Blocks.AIR.getDefaultState());

            context.assertTrue(events.stream().anyMatch(event -> event.startsWith("VC_PLAYER_INSIST version=1")
                            && event.contains("system=obra") && event.contains("block=air") && event.contains("count=1")),
                    "Regra 345: o jogador refez a janela que a obra fechou e não saiu a reafirmação: " + events);
        } finally {
            EditLedger.clear();
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "edit_ledger", tickLimit = 20)
    public void eachSideOfTheConflictIsNamed(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos path = context.getAbsolutePos(new BlockPos(4, 2, 4));
        BlockPos fence = context.getAbsolutePos(new BlockPos(6, 2, 6));
        BlockPos stone = context.getAbsolutePos(new BlockPos(8, 2, 8));

        try {
            EditLedger.clear();

            // O Zelador calça, o jogador arranca: o jogador desfez o mod.
            EditLedger.mod(world, path, Blocks.DIRT_PATH.getDefaultState(), "calcada");
            List<String> undo = EditLedger.player(world, path, Blocks.DIRT_PATH.getDefaultState(),
                    Blocks.GRASS_BLOCK.getDefaultState());

            context.assertTrue(undo.stream().anyMatch(event -> event.startsWith("VC_PLAYER_UNDO version=1")
                    && event.contains("system=calcada")), "Regra 345: o jogador desfez a calçada e nada saiu: " + undo);

            // O jogador põe uma cerca, a rua passa por cima: o mod desfez o jogador.
            EditLedger.player(world, fence, Blocks.AIR.getDefaultState(), Blocks.OAK_FENCE.getDefaultState());
            List<String> override = EditLedger.mod(world, fence, Blocks.DIRT_PATH.getDefaultState(), "rua");

            context.assertTrue(override.stream().anyMatch(event -> event.startsWith("VC_MOD_OVERRIDE version=1")
                    && event.contains("system=rua")), "Regra 345: a rua passou por cima da cerca e nada saiu: " + override);

            // A pedra natural quebrada duas vezes, sem o mod no meio: nada.
            EditLedger.player(world, stone, Blocks.STONE.getDefaultState(), Blocks.AIR.getDefaultState());
            List<String> plain = EditLedger.player(world, stone, Blocks.COBBLESTONE.getDefaultState(),
                    Blocks.AIR.getDefaultState());

            context.assertTrue(plain.stream().noneMatch(event -> event.contains("INSIST") || event.contains("UNDO")),
                    "Regra 345: edição do jogador sem o mod no meio virou conflito: " + plain);
        } finally {
            EditLedger.clear();
        }

        context.complete();
    }
}
