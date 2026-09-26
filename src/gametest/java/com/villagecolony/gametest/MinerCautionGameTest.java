package com.villagecolony.gametest;

import com.villagecolony.fabric.work.MinerCaution;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

/**
 * O mineiro dá a volta na água — ADR-025, fase 1.
 *
 * <p>Na tabela do Vanilla água custa 8, não é proibida: a navegação atravessa
 * um lago quando a volta é longa. Na mina isso é o aldeão nadando para dentro
 * do veio que a picareta abriu. O pedido do autor foi "evitar entrar em água".
 */
public class MinerCautionGameTest implements FabricGameTest {

    /**
     * Um fosso de água de um bloco de largura entre o aldeão e o alvo, com
     * uma volta seca de sete blocos para cada lado. Sem a cautela, a
     * navegação atravessa (ver o controle abaixo).
     */
    private static void moatWithALongWayRound(TestContext context) {
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 8; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.STONE.getDefaultState());
            }
        }

        for (int z = 0; z <= 6; z++) {
            context.setBlockState(new BlockPos(4, 1, z), Blocks.WATER.getDefaultState());
        }
    }

    private static boolean crossesWater(Path path) {
        for (int i = 0; i < path.getLength(); i++) {
            if (path.getNode(i).type == PathNodeType.WATER) {
                return true;
            }
        }

        return false;
    }

    /** Espera cinco tiques, como o forense: o aldeão recém-nascido ainda não pousou. */
    private static void pathAcross(TestContext context, boolean cautious, java.util.function.Consumer<Path> check) {
        moatWithALongWayRound(context);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(1, 2, 1));

        if (cautious) {
            MinerCaution.keepOutOfWater(villager);
        }

        context.waitAndRun(5, () -> {
            check.accept(villager.getNavigation().findPathTo(context.getAbsolutePos(new BlockPos(7, 2, 1)), 0));
            context.complete();
        });
    }

    /** O controle: sem a cautela o caminho passa pela água. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_caution", tickLimit = 40)
    public void aVillagerSwimsAcrossWhenTheWayRoundIsLong(TestContext context) {
        pathAcross(context, false, path -> context.assertTrue(path != null && crossesWater(path),
                "o controle não atravessou a água; o teste da cautela não mediria nada"));
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_caution", tickLimit = 40)
    public void theMinerWalksRoundTheWater(TestContext context) {
        pathAcross(context, true, path -> {
            context.assertTrue(path != null, "sem caminho — a volta seca existe");
            context.assertTrue(path.reachesTarget(), "o caminho não chega ao alvo");
            context.assertFalse(crossesWater(path), "o mineiro atravessou a água");
        });
    }
}
