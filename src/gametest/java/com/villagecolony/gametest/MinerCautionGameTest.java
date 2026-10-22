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
 * Debaixo da terra o mineiro dá a volta na água; ao ar livre ele nada — ADR-025 fase 1, Regra 71.
 *
 * <p>Na tabela do Vanilla água custa 8, não é proibida: a navegação atravessa
 * um lago quando a volta é longa. Na mina isso é o aldeão nadando para dentro
 * do veio que a picareta abriu. Na margem de um lago cercada por paredão, nadar
 * é a única saída.
 */
public class MinerCautionGameTest implements FabricGameTest {

    /** Alto no lote: perto do chão do lote há restos acima da arena, e o "ao ar livre" não se mediria. */
    private static final int Y = 40;

    /** O cenário vai de z 0 a este; o fosso fecha tudo menos a última fileira. */
    private static final int DEPTH = 12;

    /**
     * Um fosso de água de um bloco de largura entre o aldeão e o alvo, com
     * uma volta seca de onze blocos para cada lado. Sem a cautela, a
     * navegação atravessa (ver o controle abaixo).
     */
    private static void moatWithALongWayRound(TestContext context, boolean underground) {
        bed(context);

        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= DEPTH; z++) {
                context.setBlockState(new BlockPos(x, Y, z), Blocks.STONE.getDefaultState());

                if (underground) {
                    context.setBlockState(new BlockPos(x, Y + 3, z), Blocks.STONE.getDefaultState());
                }
            }
        }

        for (int z = 0; z < DEPTH; z++) {
            context.setBlockState(new BlockPos(4, Y, z), Blocks.WATER.getDefaultState());
        }
    }

    /** Um piso de pedra embaixo do cenário: no alto, a água sem fundo escorreria. */
    private static void bed(TestContext context) {
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= DEPTH; z++) {
                context.setBlockState(new BlockPos(x, Y - 1, z), Blocks.STONE.getDefaultState());
            }
        }
    }

    /** Desfaz o cenário: alto no lote, nada o limparia, e o teto sobraria sobre a arena do próximo. */
    private static void clear(TestContext context) {
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= DEPTH; z++) {
                for (int y = Y - 1; y <= Y + 3; y++) {
                    context.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState());
                }
            }
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
    private static void pathAcross(
            TestContext context, boolean cautious, boolean underground, java.util.function.Consumer<Path> check) {
        moatWithALongWayRound(context, underground);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(1, Y + 1, 1));

        if (cautious) {
            MinerCaution.keepOutOfWater(villager);
        }

        context.waitAndRun(5, () -> {
            Path path = villager.getNavigation().findPathTo(context.getAbsolutePos(new BlockPos(7, Y + 1, 1)), 0);

            villager.discard();
            clear(context);
            check.accept(path);
            context.complete();
        });
    }

    /** O controle: sem a cautela o caminho passa pela água. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_caution", tickLimit = 40)
    public void aVillagerSwimsAcrossWhenTheWayRoundIsLong(TestContext context) {
        pathAcross(context, false, true, path -> context.assertTrue(path != null && crossesWater(path),
                "o controle não atravessou a água; o teste da cautela não mediria nada"));
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_caution", tickLimit = 40)
    public void underTheGroundTheMinerWalksRoundTheWater(TestContext context) {
        pathAcross(context, true, true, path -> {
            context.assertTrue(path != null, "sem caminho — a volta seca existe");
            context.assertTrue(path.reachesTarget(), "o caminho não chega ao alvo");
            context.assertFalse(crossesWater(path), "o mineiro atravessou a água");
        });
    }

    /** Ao ar livre a água volta ao custo do Vanilla, e a volta de onze blocos perde para o fosso de um. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_caution", tickLimit = 40)
    public void underTheSkyTheMinerSwims(TestContext context) {
        pathAcross(context, true, false, path -> context.assertTrue(path != null && crossesWater(path),
                "ao ar livre o mineiro não atravessou o fosso"));
    }

    /**
     * A margem do deserto (0.3.88): dois blocos de areia entre o paredão e o lago, e a vila do outro
     * lado da água. A pé não há caminho; o mineiro ficou doze minutos parado ali.
     */
    private static void beach(TestContext context, boolean roofed, boolean swimsOff) {
        bed(context);

        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= DEPTH; z++) {
                boolean lake = x >= 3 && x <= 5;
                boolean wall = x == 0 || x <= 2 && (z < 3 || z > 5);

                context.setBlockState(new BlockPos(x, Y, z),
                        lake ? Blocks.WATER.getDefaultState() : Blocks.SAND.getDefaultState());

                for (int y = Y + 1; y <= Y + 3; y++) {
                    if (wall || roofed && y == Y + 3) {
                        context.setBlockState(new BlockPos(x, y, z), Blocks.SANDSTONE.getDefaultState());
                    }
                }
            }
        }

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(1, Y + 1, 4));

        context.waitAndRun(5, () -> {
            MinerCaution.keepOutOfWater(villager);

            Path path = villager.getNavigation().findPathTo(context.getAbsolutePos(new BlockPos(7, Y + 1, 4)), 0);
            boolean off = path != null && path.reachesTarget();

            villager.discard();
            clear(context);

            context.assertTrue(off == swimsOff, swimsOff
                    ? "ao ar livre o mineiro não sai da margem nadando"
                    : "debaixo da terra o mineiro saiu da margem pela água — ou a margem não está fechada");
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_caution", tickLimit = 40)
    public void theMinerSwimsOffABeachWalledIn(TestContext context) {
        beach(context, false, true);
    }

    /** O controle: com teto a margem só se deixa pela água, e a água está proibida. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_caution", tickLimit = 40)
    public void underARoofTheBeachHasNoWayOff(TestContext context) {
        beach(context, true, false);
    }
}
