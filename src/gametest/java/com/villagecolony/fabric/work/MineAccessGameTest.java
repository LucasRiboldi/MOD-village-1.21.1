package com.villagecolony.fabric.work;

import com.villagecolony.core.movement.DetourMoves;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * O acesso cavado da vila até o nível da mina — decisão do autor de 2026-10-22 ("cavar um acesso
 * descendo"). Cenários quarenta blocos acima do piso, longe das outras arenas.
 */
public class MineAccessGameTest implements FabricGameTest {

    private static final int Y = 40;

    /** Da plataforma até a sala do nível dentro da pedra: só cavando, e descendo. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_access")
    public void theAccessIsDugDownOnlyDigging(TestContext context) {
        ServerWorld world = context.getWorld();

        // Maciço de pedra x 0..7, y 33..42, z 0..4; plataforma de terra em cima, na ponta x 0..1.
        for (int x = 0; x <= 7; x++) {
            for (int y = Y - 7; y <= Y + 2; y++) {
                for (int z = 0; z <= 4; z++) {
                    context.setBlockState(new BlockPos(x, y, z), Blocks.STONE.getDefaultState());
                }
            }
        }

        for (int x = 0; x <= 1; x++) {
            for (int y = Y + 1; y <= Y + 2; y++) {
                context.setBlockState(new BlockPos(x, y, 2), Blocks.AIR.getDefaultState());
            }
        }

        // A sala do nível, cinco abaixo e seis ao lado.
        for (int x = 6; x <= 7; x++) {
            for (int y = Y - 4; y <= Y - 3; y++) {
                context.setBlockState(new BlockPos(x, y, 2), Blocks.AIR.getDefaultState());
            }
        }

        BlockPos start = context.getAbsolutePos(new BlockPos(0, Y + 1, 2));
        BlockPos level = context.getAbsolutePos(new BlockPos(7, Y - 4, 2));

        Optional<DetourWalker> walker = DetourWalker.plan(
                world, UUID.randomUUID(), start, level, at -> MineEntrance.insideTheLevel(at, level), Set.of(),
                false, MinerTerrain::digOnly, false, DetourMoves.Costs.MINER);

        context.assertTrue(walker.isPresent(), "não houve túnel só-cavar até o nível");

        ColonyPos room = MinecraftTypeAdapter.toColonyPos(level);

        context.assertTrue(MinerTerrain.of(world).at(room) == com.villagecolony.core.movement.Cell.OPEN
                        && MinerTerrain.digOnly(world).at(room) == com.villagecolony.core.movement.Cell.PASSAGE,
                "o acesso só-cavar devia recusar bloco até dentro da montanha");

        ColonyPos from = MinecraftTypeAdapter.toColonyPos(start);
        int highest = from.y();

        for (ColonyPos step : walker.get().cells()) {
            Optional<DetourMoves.Actions> actions = DetourMoves.actions(
                    MinerTerrain.digOnly(world), from, step, Set.of(), DetourMoves.Costs.MINER);

            context.assertTrue(actions.isPresent() && actions.get().place().isEmpty(),
                    "o acesso pôs bloco em " + step);
            highest = Math.max(highest, step.y());
            from = step;
        }

        context.assertTrue(highest == MinecraftTypeAdapter.toColonyPos(start).y(),
                "o acesso subiu acima de onde partiu: " + highest);

        context.complete();
    }

    /** O túnel sai da célula alcançável mais perto do nível, e acima dele. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_access")
    public void theAccessStartsNearTheLevelAndAboveIt(TestContext context) {
        BlockPos level = new BlockPos(100, 64, 100);
        BlockPos above = new BlockPos(104, 70, 100);
        BlockPos below = new BlockPos(101, 60, 100);
        BlockPos far = new BlockPos(130, 64, 100);

        Optional<BlockPos> chosen = MineAccess.nearestReachable(Set.of(above, below, far), level);

        context.assertTrue(chosen.equals(Optional.of(above)),
                "devia partir de cima e perto do nível, e escolheu " + chosen);

        context.complete();
    }
}
