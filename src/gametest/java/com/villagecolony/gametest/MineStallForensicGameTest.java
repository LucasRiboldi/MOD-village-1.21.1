package com.villagecolony.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

/**
 * A mina de 25-09, reconstruída do save, bloco por bloco — experimento forense
 * da ADR-025.
 *
 * <p>O mineiro travou em todas as sessões na pedra 553, 39, 158, parado em
 * 549, 41, 158 e mandado ficar de pé em 552, 40, 158: três blocos adiante, na
 * sala da mina, um degrau abaixo. O {@code approachTo} só devolve lugar que
 * passa no {@code standable}, então às 09:15 a célula era ar. A pergunta é se a
 * navegação Vanilla acha caminho até lá.
 *
 * <p>O recorte é x 545..556, y 37..47, z 153..162 do mundo do autor
 * ({@code saves/Novo mundo/region/r.1.0.mca}). Rocha e minério viram pedra; ar,
 * tocha de parede, líquen e os quatro {@code dirt_path} (que ocupam células já
 * cavadas) viram ar, como estavam quando o mineiro escolheu o lugar de pisar.
 */
public class MineStallForensicGameTest implements FabricGameTest {

    private static final String OPEN_CELLS = "0,0,0;0,0,1;0,1,0;0,4,3;0,4,4;0,4,5;0,4,6;0,5,3;0,5,4;0,5,5;0,5,6;0,6,2;0,6,3;0,6,4;0,6,5;0,7,2;0,7,3;0,7,4;0,8,2;0,8,3;0,10,3;0,10,4;1,0,0;1,0,1;1,4,3;1,4,4;1,4,5;1,4,6;1,5,3;1,5,4;1,5,5;1,5,6;1,6,2;1,6,3;1,6,4;1,6,5;1,7,2;1,7,3;1,7,4;2,4,3;2,4,4;2,4,5;2,4,6;2,5,3;2,5,4;2,5,5;2,5,6;3,4,3;3,4,4;3,4,5;3,4,6;3,5,3;3,5,4;3,5,5;3,5,6;3,6,4;3,6,5;4,4,3;4,4,4;4,4,5;4,4,6;4,5,3;4,5,4;4,5,5;4,5,6;4,6,3;4,6,4;4,6,5;5,4,3;5,4,4;5,4,5;5,4,6;5,5,3;5,5,4;5,5,5;5,5,6;5,6,3;5,6,4;5,6,5;5,7,3;5,7,4;5,7,5;5,8,3;5,8,4;5,9,3;5,10,5;6,4,3;6,4,4;6,4,5;6,4,6;6,5,3;6,5,4;6,5,5;6,5,6;6,6,3;6,6,4;6,6,5;6,8,3;6,9,3;6,10,3;6,10,6;7,3,4;7,3,5;7,4,3;7,4,4;7,4,5;7,4,6;7,5,3;7,5,4;7,5,5;7,5,6;7,6,3;7,6,4;7,6,5;7,9,3;7,10,3;8,4,5;8,6,3;8,10,3";

    private static final BlockPos CORNER = new BlockPos(2, 2, 2);

    private static final BlockPos MINER = CORNER.add(549 - 545, 41 - 37, 158 - 153);

    private static final BlockPos STAND = CORNER.add(552 - 545, 40 - 37, 158 - 153);

    private static void buildTheMine(TestContext context) {
        for (int x = 0; x < 12; x++) {
            for (int y = 0; y < 11; y++) {
                for (int z = 0; z < 10; z++) {
                    context.setBlockState(CORNER.add(x, y, z), Blocks.STONE.getDefaultState());
                }
            }
        }

        for (String cell : OPEN_CELLS.split(";")) {
            String[] xyz = cell.split(",");
            context.setBlockState(
                    CORNER.add(Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]), Integer.parseInt(xyz[2])),
                    Blocks.AIR.getDefaultState());
        }
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_stall_forensic", tickLimit = 200)
    public void theVanillaNavigationReachesThePlaceToStand(TestContext context) {
        buildTheMine(context);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, MINER);
        BlockPos target = context.getAbsolutePos(STAND);

        context.waitAndRun(5, () -> {
            Path path = villager.getNavigation().findPathTo(target, 0);

            context.assertTrue(path != null, "a navegação não achou caminho nenhum até o lugar de pisar");
            context.assertTrue(path.reachesTarget(),
                    "a navegação achou caminho, mas ele não chega ao lugar de pisar — termina em "
                            + path.getTarget() + " (alvo " + target + ")");
            context.complete();
        });
    }
}
