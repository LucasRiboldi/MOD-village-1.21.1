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
            world.setBlockState(nowAt.add(0, 26, -22), Blocks.STONE.getDefaultState());

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

            // Playtest de 08-10, 15:29: três abaixo e quatro ao lado da boca, na escada,
            // ele era mandado de volta à boca. Ele já desceu: a seguinte é o nível 1.
            Optional<BlockPos> onTheStairs = MineEntrance.legDown(
                    world, colonyId, MinecraftTypeAdapter.toBlockPos(mouth).add(0, -3, 4), stone);

            context.assertTrue(onTheStairs.map(middleAt::equals).orElse(false),
                    "logo abaixo da boca, na escada, ele não desceu: " + onTheStairs);

            // Na superfície em cima da mina (08-10 de manhã): não é escada — vai à boca.
            Optional<BlockPos> above = MineEntrance.legDown(world, colonyId, nowAt.up(28), stone);

            context.assertTrue(above.map(MinecraftTypeAdapter.toBlockPos(mouth)::equals).orElse(false),
                    "em cima da mina, na superfície, ele não foi à boca: " + above);

            // Playtest 0.3.31: a pedra no fim do ramal, 22 blocos da entrada do nível, e
            // o mineiro na superfície em cima dela. Ele ia à entrada enterrada embaixo
            // dele e desistia; vai à boca.
            BlockPos endOfTheBranch = nowAt.add(0, -1, -22);
            Optional<BlockPos> farStone = MineEntrance.legDown(
                    world, colonyId, endOfTheBranch.up(29), endOfTheBranch);

            context.assertTrue(farStone.map(MinecraftTypeAdapter.toBlockPos(mouth)::equals).orElse(false),
                    "com a pedra no fim do ramal, na superfície ele não foi à boca: " + farStone);

            context.assertTrue(MineEntrance.legDown(world, colonyId, nowAt, stone).isEmpty(),
                    "no nível da pedra a descida ainda mandava nele");
            context.assertTrue(MineEntrance.legDown(world, colonyId, nowAt.add(0, 0, -18), endOfTheBranch).isEmpty(),
                    "no meio do ramal, no nível da pedra, a descida ainda mandava nele");
        } finally {
            world.setBlockState(MinecraftTypeAdapter.toBlockPos(mouth).down(), Blocks.AIR.getDefaultState());
            world.setBlockState(middleAt.up(16), Blocks.AIR.getDefaultState());
            world.setBlockState(nowAt.up(26), Blocks.AIR.getDefaultState());
            world.setBlockState(nowAt.add(0, 26, -22), Blocks.AIR.getDefaultState());
            VillageColonyMod.MINES.removeOfColony(colonyId);
            MineEntrance.forget(colonyId);
        }

        context.complete();
    }

    /**
     * A escada girada no nível de agora (ADR-037 M1) não troca a boca guardada —
     * RC2 do roteiro de 10-10. O giro muda o rumo e mantém a entrada, e a cadeia
     * calculada pelo rumo novo saía de outro lugar.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_entrance_turned")
    public void aTurnedStairKeepsTheRememberedMouth(TestContext context) {
        ServerWorld world = context.getWorld();
        UUID colonyId = UUID.randomUUID();
        ColonyPos mouth = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(3, 42, 3)));
        MineShaft turned = MineShaft.from(mouth, Side.NORTH).deepened().rerouted();

        try {
            MineEntrance.forget(colonyId);
            MineEntrance.opened(colonyId, mouth);
            VillageColonyMod.MINES.restore(Mine.restore(colonyId, turned, 0));

            List<BlockPos> route = MineEntrance.route(world, colonyId,
                    VillageColonyMod.MINES.of(colonyId).orElseThrow());

            context.assertTrue(route.equals(List.of(MinecraftTypeAdapter.toBlockPos(mouth),
                            MinecraftTypeAdapter.toBlockPos(turned.entry()))),
                    "depois do giro da escada a rota não sai mais da boca guardada: " + route);
        } finally {
            VillageColonyMod.MINES.removeOfColony(colonyId);
            MineEntrance.forget(colonyId);
        }

        context.complete();
    }

    /**
     * Sem boca guardada (servidor reiniciado), a dedução segue a escada cavada, e
     * não o rumo de agora — RC2 do roteiro de 10-10. No save do autor a cadeia pelo
     * rumo de agora subia por pedra maciça e o mineiro ia e voltava até um patamar
     * que nunca foi cavado.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_entrance_dug_stair")
    public void withoutAMemoryTheDeductionFollowsTheDugStair(TestContext context) {
        ServerWorld world = context.getWorld();
        UUID colonyId = UUID.randomUUID();
        ColonyPos mouth = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(3, 72, 3)));
        MineShaft surface = MineShaft.from(mouth, Side.NORTH);
        MineShaft turned = surface.deepened().rerouted();
        BlockPos below = MinecraftTypeAdapter.toBlockPos(turned.entry());
        BlockPos from = below.add(-12, -3, -12);
        BlockPos to = new BlockPos(below.getX() + 12, mouth.y() - 1, below.getZ() + 12);

        try {
            // Rocha em volta dos quatro rumos possíveis, e só a escada do rumo de cima cavada.
            for (BlockPos at : BlockPos.iterate(from, to)) {
                world.setBlockState(at, Blocks.STONE.getDefaultState());
            }

            for (int index = 0; index < MineShaft.CARVED; index++) {
                world.setBlockState(MinecraftTypeAdapter.toBlockPos(surface.positionAt(index)),
                        Blocks.AIR.getDefaultState());
            }

            VillageColonyMod.MINES.restore(Mine.restore(colonyId, turned, 0));
            MineEntrance.forget(colonyId);

            List<BlockPos> route = MineEntrance.route(world, colonyId,
                    VillageColonyMod.MINES.of(colonyId).orElseThrow());

            context.assertTrue(route.equals(List.of(MinecraftTypeAdapter.toBlockPos(mouth), below)),
                    "a dedução não seguiu a escada cavada até a boca " + mouth + ": " + route);
        } finally {
            for (BlockPos at : BlockPos.iterate(from, to.up(3))) {
                world.setBlockState(at, Blocks.AIR.getDefaultState());
            }

            VillageColonyMod.MINES.removeOfColony(colonyId);
            MineEntrance.forget(colonyId);
        }

        context.complete();
    }
}
