package com.villagecolony.fabric.integration;

import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.coordination.VillageZone;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.UUID;

/**
 * Pedido do autor de 2026-10-08: o lenhador corta primeiro as árvores do centro da
 * vila, depois as intermediárias, depois as da borda. No playtest ele escolhia a
 * conhecida mais perto <b>dele</b>, e ia a 134 blocos com árvore na praça.
 */
public class VillageTreesZoneGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_trees_zone")
    public void theCentralTreeComesBeforeTheOneBesideTheLumberjack(TestContext context) {
        ServerWorld world = context.getWorld();
        UUID colonyId = UUID.randomUUID();
        // Quarenta acima do piso, fora do alcance das outras arenas.
        BlockPos centre = context.getAbsolutePos(new BlockPos(1, 42, 1));
        BlockPos edge = context.getAbsolutePos(new BlockPos(7, 42, 7));
        BlockPos lumberjack = context.getAbsolutePos(new BlockPos(7, 42, 6));
        VillageBounds box = new VillageBounds(
                centre.getX() - 7, centre.getY() - 5, centre.getZ() - 7,
                centre.getX() + 7, centre.getY() + 5, centre.getZ() + 7);

        try {
            world.setBlockState(centre, Blocks.OAK_LOG.getDefaultState());
            world.setBlockState(edge, Blocks.OAK_LOG.getDefaultState());
            VillageTrees.rememberTree(colonyId, centre);
            VillageTrees.rememberTree(colonyId, edge);

            Optional<BlockPos> chosen = VillageTrees.nearest(world, colonyId, lumberjack, log -> true,
                    log -> VillageZone.of(box, log.getX(), log.getZ(), 10).rank());

            context.assertTrue(chosen.map(centre::equals).orElse(false),
                    "a árvore do centro não veio antes da que está ao lado dele, na borda: " + chosen);

            // Controle: sem faixa, vale a mais perto dele, como antes.
            context.assertTrue(VillageTrees.nearest(world, colonyId, lumberjack, log -> true)
                            .map(edge::equals).orElse(false),
                    "sem faixa a mais perto dele deixou de valer");
        } finally {
            world.setBlockState(centre, Blocks.AIR.getDefaultState());
            world.setBlockState(edge, Blocks.AIR.getDefaultState());
        }

        context.complete();
    }
}
