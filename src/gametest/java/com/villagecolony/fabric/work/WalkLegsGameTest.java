package com.villagecolony.fabric.work;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * A7, pedido do autor de 2026-10-08: árvore longe se alcança por pernas de 24
 * blocos na superfície; perto, vai direto.
 */
public class WalkLegsGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "walk_legs")
    public void aFarTreeIsReachedByLegsAndANearOneDirectly(TestContext context) {
        BlockPos from = context.getAbsolutePos(new BlockPos(2, 1, 2));
        BlockPos far = from.add(100, 0, 0);
        BlockPos near = from.add(20, 0, 0);

        Optional<BlockPos> leg = WalkLegs.towards(context.getWorld(), from, far);

        context.assertTrue(leg.isPresent(), "a 100 blocos não saiu perna nenhuma");

        int forward = leg.get().getX() - from.getX();

        context.assertTrue(forward >= WalkLegs.LEG - WalkLegs.GRID - 2 && forward <= WalkLegs.LEG + WalkLegs.GRID + 2,
                "a perna não ficou a ~24 blocos na direção da árvore: " + forward);
        java.util.Set<BlockPos> legs = new java.util.HashSet<>();

        for (int step = 0; step < WalkLegs.GRID; step++) {
            WalkLegs.towards(context.getWorld(), from.add(step, 0, 0), far).ifPresent(legs::add);
        }

        context.assertTrue(legs.size() <= 2,
                "em oito passos a perna mudou " + legs.size() + " vezes — a navegação refaria o caminho a cada passo");
        context.assertTrue(WalkLegs.towards(context.getWorld(), from, near).isEmpty(),
                "a 20 blocos ele não foi direto");
        context.complete();
    }
}
