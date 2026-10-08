package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Pedido do autor de 2026-10-08: o mineiro guarda a entrada da mina, vai até ela e
 * só depois começa a tarefa. A mina do save tinha descido três níveis; a "boca" que
 * o mineiro usava era a entrada do nível de agora, trinta blocos dentro da rocha.
 *
 * <p>O cenário: uma mina aberta na superfície do piso de teste e descida dois
 * níveis (os níveis enterrados ganham rocha alta em cima da coluna, para o mapa de
 * alturas dizer que não são superfície).
 */
public class MineEntranceGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_entrance")
    public void theMinerGoesToTheSurfaceEntranceFirstAndThenDownLevelByLevel(TestContext context) {
        ServerWorld world = context.getWorld();
        UUID colonyId = UUID.randomUUID();
        // Quarenta acima do piso: a entrada de superfície tem só ar em cima.
        ColonyPos mouth = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(3, 42, 3)));
        MineShaft surface = MineShaft.from(mouth, Side.NORTH);
        MineShaft middle = surface.deepened();
        MineShaft now = middle.deepened();
        BlockPos middleAt = MinecraftTypeAdapter.toBlockPos(middle.entry());
        BlockPos nowAt = MinecraftTypeAdapter.toBlockPos(now.entry());

        try {
            world.setBlockState(MinecraftTypeAdapter.toBlockPos(mouth).down(), Blocks.STONE.getDefaultState());
            world.setBlockState(middleAt.up(16), Blocks.STONE.getDefaultState());
            world.setBlockState(nowAt.up(26), Blocks.STONE.getDefaultState());

            VillageColonyMod.MINES.restore(Mine.restore(colonyId, now, 0));
            MineEntrance.forget(colonyId);

            context.assertTrue(MineEntrance.surfaceOf(world, colonyId, VillageColonyMod.MINES.of(colonyId)
                            .orElseThrow()).equals(mouth),
                    "a entrada de superfície não foi deduzida subindo os níveis");

            List<BlockPos> route = MineEntrance.route(world, colonyId,
                    VillageColonyMod.MINES.of(colonyId).orElseThrow());

            context.assertTrue(route.size() == 3 && route.get(1).equals(middleAt) && route.get(2).equals(nowAt),
                    "a rota não liga a superfície ao nível de agora: " + route);

            BlockPos stone = nowAt.down();
            BlockPos onTheSurface = MinecraftTypeAdapter.toBlockPos(mouth).add(10, 0, 10);

            Optional<BlockPos> first = MineEntrance.legDown(world, colonyId, onTheSurface, stone);

            context.assertTrue(first.map(MinecraftTypeAdapter.toBlockPos(mouth)::equals).orElse(false),
                    "na superfície ele não foi primeiro à boca: " + first);

            Optional<BlockPos> second = MineEntrance.legDown(
                    world, colonyId, MinecraftTypeAdapter.toBlockPos(mouth), stone);

            context.assertTrue(second.map(middleAt::equals).orElse(false),
                    "na boca ele não desceu para o nível seguinte: " + second);

            // Playtest de 08-10: ele para dois abaixo e três ao lado da entrada (ela é o
            // alto do primeiro degrau). Isso é ter chegado: segue para o nível seguinte.
            Optional<BlockPos> third = MineEntrance.legDown(world, colonyId, middleAt.add(0, -2, 3), stone);

            context.assertTrue(third.map(nowAt::equals).orElse(false),
                    "dois abaixo e três ao lado da entrada ele não seguiu para o nível seguinte: " + third);

            context.assertTrue(MineEntrance.legDown(world, colonyId, nowAt, stone).isEmpty(),
                    "no nível da pedra a descida ainda mandava nele");
        } finally {
            world.setBlockState(MinecraftTypeAdapter.toBlockPos(mouth).down(), Blocks.AIR.getDefaultState());
            world.setBlockState(middleAt.up(16), Blocks.AIR.getDefaultState());
            world.setBlockState(nowAt.up(26), Blocks.AIR.getDefaultState());
            VillageColonyMod.MINES.removeOfColony(colonyId);
            MineEntrance.forget(colonyId);
        }

        context.complete();
    }
}
