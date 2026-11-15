package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.TreeClearing;
import com.villagecolony.fabric.work.LeafPassage;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.UUID;

/**
 * Regra 290: quem tira árvore tira o tronco inteiro, também abaixo do piso; e o aldeão da colônia passa por folha
 * natural quebrando a que tranca o caminho — a folha posta (persistente) continua parede.
 */
public class TreeAndLeafGameTest implements FabricGameTest {

    /** A árvore com a base abaixo do piso do caminho sai inteira: nenhuma tora fica de toco. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "tree_and_leaf", tickLimit = 5)
    public void aPathColumnTakesTheWholeTrunk(TestContext context) {
        for (int x = 1; x <= 5; x++) {
            for (int z = 1; z <= 5; z++) {
                context.setBlockState(new BlockPos(x, 0, z), Blocks.DIRT);
            }
        }

        // Tronco de y 1 a y 5; o piso do caminho fica em y 2, então as toras de y 1 e y 2 estão abaixo da busca.
        for (int y = 1; y <= 5; y++) {
            context.setBlockState(new BlockPos(3, y, 3), Blocks.OAK_LOG);
        }

        for (int x = 2; x <= 4; x++) {
            for (int z = 2; z <= 4; z++) {
                if (x != 3 || z != 3) {
                    context.setBlockState(new BlockPos(x, 5, z), Blocks.OAK_LEAVES);
                }
            }
        }

        context.setBlockState(new BlockPos(3, 6, 3), Blocks.OAK_LEAVES);
        context.setBlockState(new BlockPos(6, 1, 6), Blocks.CHEST);
        BlockPos trunk = context.getAbsolutePos(new BlockPos(3, 0, 3));

        // O que a árvore dá vai para o baú, e não para o chão da área que o lote seguinte reaproveita.
        TreeClearing.openColumn(context.getWorld(), trunk.getX(), trunk.getZ(), trunk.getY() + 2,
                List.of(MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(6, 1, 6)))));
        clearDrops(context);

        for (int y = 1; y <= 5; y++) {
            context.assertFalse(context.getBlockState(new BlockPos(3, y, 3)).isIn(BlockTags.LOGS),
                    "sobrou tora do tronco em y=" + y);
        }

        // O baú sai vazio, sem espalhar o que guardou na área do lote seguinte.
        if (context.getWorld().getBlockEntity(context.getAbsolutePos(new BlockPos(6, 1, 6)))
                instanceof net.minecraft.inventory.Inventory chest) {
            chest.clear();
        }

        context.setBlockState(new BlockPos(6, 1, 6), Blocks.AIR);
        clearArena(context);
        context.complete();
    }

    /** Folha natural no corredor: o caminho passa por ela, e o aldeão a quebra ao chegar. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "tree_and_leaf", tickLimit = 40)
    public void aColonyVillagerBreaksTheNaturalLeavesInItsWay(TestContext context) {
        leafCorridor(context, Blocks.OAK_LEAVES.getDefaultState());
        Scene scene = scene(context);

        context.waitAndRun(5, () -> {
            try {
                scene.villager.getNavigation().stop();
                Path path = scene.villager.getNavigation().findPathTo(context.getAbsolutePos(new BlockPos(6, 2, 4)), 0);

                context.assertTrue(path != null && path.reachesTarget(),
                        "a folha natural fechou o corredor ao aldeão da colônia");

                int leaf = firstLeafNode(context, path);
                context.assertTrue(leaf > 0, "o caminho não passa pela folha");

                BlockPos before = path.getNodePos(leaf - 1);
                scene.villager.refreshPositionAndAngles(before.getX() + 0.5, before.getY(), before.getZ() + 0.5, 0, 0);
                path.setCurrentNodeIndex(leaf - 1);

                context.assertTrue(LeafPassage.clearAhead(context.getWorld(), scene.villager, path) > 0,
                        "o aldeão não quebrou a folha à frente");
                context.assertTrue(context.getBlockState(new BlockPos(4, 2, 4)).isAir()
                        && context.getBlockState(new BlockPos(4, 3, 4)).isAir(), "a folha à frente ficou no lugar");
            } finally {
                scene.cleanUp();
                clearDrops(context);
                clearArena(context);
            }

            context.complete();
        });
    }

    /** Folha posta (persistente) é construção: continua fechando o corredor. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "tree_and_leaf", tickLimit = 40)
    public void persistentLeavesStayAWall(TestContext context) {
        leafCorridor(context, Blocks.OAK_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true));
        Scene scene = scene(context);

        context.waitAndRun(5, () -> {
            try {
                scene.villager.getNavigation().stop();
                Path path = scene.villager.getNavigation().findPathTo(context.getAbsolutePos(new BlockPos(6, 2, 4)), 0);

                context.assertFalse(path != null && path.reachesTarget(), "o caminho atravessou folha posta");
            } finally {
                scene.cleanUp();
                clearDrops(context);
                clearArena(context);
            }

            context.complete();
        });
    }

    /** Corredor de pedra em z 4, de x 1 a x 6, piso em y 1, paredes até y 4, com a folha em x 4 (y 2 e 3); cabe nos 8 blocos da estrutura. */
    private static void leafCorridor(TestContext context, BlockState leaf) {
        for (int x = 0; x <= 7; x++) {
            for (int z = 2; z <= 6; z++) {
                for (int y = 1; y <= 5; y++) {
                    boolean inside = z == 4 && x >= 1 && x <= 6 && y >= 2 && y <= 4;
                    context.setBlockState(new BlockPos(x, y, z), inside ? Blocks.AIR.getDefaultState()
                            : Blocks.STONE.getDefaultState());
                }
            }
        }

        context.setBlockState(new BlockPos(4, 2, 4), leaf);
        context.setBlockState(new BlockPos(4, 3, 4), leaf);
        context.setBlockState(new BlockPos(4, 4, 4), Blocks.STONE.getDefaultState());
    }

    private static int firstLeafNode(TestContext context, Path path) {
        for (int index = 0; index < path.getLength(); index++) {
            if (context.getWorld().getBlockState(path.getNodePos(index)).isIn(BlockTags.LEAVES)
                    || context.getWorld().getBlockState(path.getNodePos(index).up()).isIn(BlockTags.LEAVES)) {
                return index;
            }
        }

        return -1;
    }

    /** O aldeão como trabalhador de uma colônia que não está registrada: o caminho só pede o trabalhador. */
    private record Scene(VillagerEntity villager, UUID colonyId) {
        void cleanUp() {
            VillageColonyMod.WORKERS.remove(villager.getUuid());
            villager.discard();
        }
    }

    /**
     * Sem colônia registrada de propósito: a colônia de teste roda a fundação no tique dela e deixa construção na área,
     * que o lote seguinte reaproveita (o {@code BuildSiteGameTest} recusava a roça por {@code TOO_CLOSE}).
     */
    private static Scene scene(TestContext context) {
        BlockPos from = context.getAbsolutePos(new BlockPos(1, 2, 4));
        UUID colonyId = UUID.randomUUID();
        VillagerEntity villager = EntityType.VILLAGER.create(context.getWorld());

        villager.refreshPositionAndAngles(from.getX() + 0.5, from.getY(), from.getZ() + 0.5, 0, 0);
        villager.setBreedingAge(0);
        context.getWorld().spawnEntity(villager);
        // Trabalhador sem ofício: o caminho só pede o registro, e um ofício agiria no chão e nas ruas da área.
        VillageColonyMod.WORKERS.register(villager.getUuid(), colonyId);

        return new Scene(villager, colonyId);
    }

    /** Os itens que a folha quebrada soltou saem com o teste: a área é reaproveitada pelo lote seguinte. */
    private static void clearDrops(TestContext context) {
        BlockPos min = context.getAbsolutePos(new BlockPos(0, 0, 0));
        BlockPos max = context.getAbsolutePos(new BlockPos(8, 8, 8));
        context.getWorld().getEntitiesByClass(ItemEntity.class, new Box(Vec3d.of(min), Vec3d.of(max)).expand(2),
                item -> true).forEach(ItemEntity::discard);
    }

    /**
     * Devolve a arena ao ar: a área é reaproveitada pelos lotes seguintes, e a caixa de pedra que ficava fazia o
     * {@code BuildSiteGameTest} achar o chão em cima dela, e não na rua dele.
     */
    private static void clearArena(TestContext context) {
        for (BlockPos at : BlockPos.iterate(0, 1, 0, 7, 7, 7)) {
            context.setBlockState(at.toImmutable(), Blocks.AIR);
        }
    }
}
