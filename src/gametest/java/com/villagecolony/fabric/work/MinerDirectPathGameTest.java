package com.villagecolony.fabric.work;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

/**
 * O mineiro anda direto quando a navegação do jogo chega ao lugar de pé da pedra, e só
 * então — playtest 0.3.32 (pedra alcançável a pé, perna mandando para a boca).
 */
public class MinerDirectPathGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_direct_path", tickLimit = 40)
    public void theMinerWalksStraightWhenTheGameFindsTheWay(TestContext context) {
        for (int x = 0; x <= 9; x++) {
            for (int z = 0; z <= 2; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.STONE.getDefaultState());
            }
        }

        // Em coordenada absoluta: o spawnEntity do contexto desloca a entidade para fora do piso.
        BlockPos from = context.getAbsolutePos(new BlockPos(1, 2, 1));
        VillagerEntity villager = EntityType.VILLAGER.create(context.getWorld());
        villager.refreshPositionAndAngles(from.getX() + 0.5, from.getY(), from.getZ() + 0.5, 0, 0);
        context.getWorld().spawnEntity(villager);
        BlockPos open = context.getAbsolutePos(new BlockPos(8, 2, 1));

        context.waitAndRun(5, () -> {
            net.minecraft.entity.ai.pathing.Path probe = villager.getNavigation().findPathTo(open, 0);
            context.assertTrue(MinerDirectPath.reaches(context.getWorld(), villager, open),
                    "com o chão aberto até a pedra, a navegação não chegou: aldeão em "
                            + villager.getBlockPos().toShortString() + ", alvo " + open.toShortString()
                            + ", caminho " + (probe == null ? "nulo" : probe.getEnd() + " chega=" + probe.reachesTarget()));

            // Parede de rocha de ponta a ponta: nenhum caminho; vale a perna de sempre.
            for (int z = -3; z <= 5; z++) {
                for (int y = 2; y <= 5; y++) {
                    context.setBlockState(new BlockPos(5, y, z), Blocks.BEDROCK.getDefaultState());
                }
            }

            MinerDirectPath.forget(villager.getUuid());

            context.assertFalse(MinerDirectPath.reaches(context.getWorld(), villager, open),
                    "com a parede no meio, o caminho direto ainda foi aceito");

            for (int z = -3; z <= 5; z++) {
                for (int y = 2; y <= 5; y++) {
                    context.setBlockState(new BlockPos(5, y, z), Blocks.AIR.getDefaultState());
                }
            }

            MinerDirectPath.clearAll();
            villager.discard();
            context.complete();
        });
    }
}
