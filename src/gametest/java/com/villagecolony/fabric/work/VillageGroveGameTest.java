package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/**
 * Pedido do autor de 2026-10-08: o lenhador planta seis mudas perto da vila, onde
 * não atrapalham obra futura — fora da caixa da vila, longe da rua (o lote novo
 * nasce ao lado dela) e afastadas entre si.
 *
 * <p>Sessenta acima do piso: o patamar de grama tem 25 de lado, mais que a arena,
 * e lá em cima não há cenário de ninguém.
 */
public class VillageGroveGameTest implements FabricGameTest {

    private static final int Y = 60;

    /**
     * A rua, do lado leste — por onde a busca começa quando não há lenhador: seis
     * além da borda da caixa. Do lado norte ela não era testada (a primeira versão
     * sobreviveu à mutação).
     */
    private static final int PATH_X = 11;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_grove", tickLimit = 100)
    public void threeSaplingsGrowNearTheVillageAwayFromTheRoad(TestContext context) {
        ServerWorld world = context.getWorld();

        for (int x = -9; x <= 15; x++) {
            for (int z = -9; z <= 15; z++) {
                world.setBlockState(context.getAbsolutePos(new BlockPos(x, Y, z)),
                        x == PATH_X ? Blocks.DIRT_PATH.getDefaultState() : Blocks.GRASS_BLOCK.getDefaultState());
            }
        }

        BlockPos low = context.getAbsolutePos(new BlockPos(1, Y - 5, 1));
        BlockPos high = context.getAbsolutePos(new BlockPos(5, Y + 5, 5));
        VillageBounds box = new VillageBounds(low.getX(), low.getY(), low.getZ(), high.getX(), high.getY(), high.getZ());
        Colony colony = Colony.create(UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(3, Y + 1, 3))));

        colony.measure(box);
        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);
        int pathX = context.getAbsolutePos(new BlockPos(PATH_X, Y, 0)).getX();

        try {
            int planted = VillageGrove.tendNow(world, colony);
            List<BlockPos> grove = VillageGrove.of(colony.id());

            context.assertTrue(planted == 6 && grove.size() == 6,
                    "o bosque não ficou com seis mudas: " + planted + " " + grove);

            for (BlockPos spot : grove) {
                context.assertTrue(!box.containsColumn(spot.getX(), spot.getZ(), VillageGrove.NEAR - 1),
                        "uma muda nasceu dentro da vila: " + spot);
                context.assertTrue(Math.abs(spot.getX() - pathX) >= VillageGrove.ROAD_CLEARANCE,
                        "uma muda nasceu ao lado da rua, onde os lotes nascem: " + spot);
                // A farinha de osso pode já ter feito a muda virar árvore.
                context.assertTrue(world.getBlockState(spot.up()).isIn(net.minecraft.registry.tag.BlockTags.SAPLINGS)
                                || world.getBlockState(spot.up()).isIn(net.minecraft.registry.tag.BlockTags.LOGS),
                        "não há muda nem árvore em " + spot);

                for (BlockPos other : grove) {
                    context.assertTrue(other.equals(spot)
                                    || other.getSquaredDistance(spot) >= VillageGrove.SPACING * VillageGrove.SPACING,
                            "duas mudas coladas: " + spot + " e " + other);
                }
            }

            context.assertTrue(VillageGrove.tendNow(world, colony) == 0,
                    "com o bosque cheio o lenhador plantou mais");
        } finally {
            for (int x = -9; x <= 15; x++) {
                for (int z = -9; z <= 15; z++) {
                    for (int up = 1; up <= 10; up++) {
                        world.setBlockState(context.getAbsolutePos(new BlockPos(x, Y + up, z)), Blocks.AIR.getDefaultState());
                    }
                    world.setBlockState(context.getAbsolutePos(new BlockPos(x, Y, z)), Blocks.AIR.getDefaultState());
                }
            }

            owned.cleanUp();
            VillageGrove.clearAll();
        }

        context.complete();
    }
}
