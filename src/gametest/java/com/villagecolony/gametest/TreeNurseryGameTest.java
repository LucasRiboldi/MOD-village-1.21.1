package com.villagecolony.gametest;

import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.work.FarmerNursery;
import com.villagecolony.fabric.work.TreeNursery;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * O viveiro do fazendeiro — 2026-09-19, habilidade nova pedida pelo autor.
 *
 * <p>O que estes cenários trancam: a espécie sai do bioma, o plantio
 * acontece de verdade no mundo, e o lugar ocupado não vira viveiro.
 */
public class TreeNurseryGameTest {

    /**
     * A terra enraizada e o rebento entram no mundo.
     *
     * <p>É a habilidade inteira num cenário: chão do bioma, planta, e os
     * dois blocos ficam de pé.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "tree_nursery",
            tickLimit = 100)
    public void theRootedDirtAndTheSaplingBothLand(TestContext context) {
        ServerWorld world = context.getWorld();

        BlockPos ground = context.getAbsolutePos(new BlockPos(1, 1, 1));

        world.setBlockState(ground, Blocks.SAND.getDefaultState());

        if (!TreeNursery.plant(world, ground, Blocks.OAK_SAPLING)) {
            throw new AssertionError("o viveiro nao nasceu em chao de bioma livre");
        }

        if (!world.getBlockState(ground).isOf(Blocks.ROOTED_DIRT)) {
            throw new AssertionError(
                    "o chao nao virou terra enraizada: "
                            + world.getBlockState(ground).getBlock());
        }

        if (!world.getBlockState(ground.up()).isOf(Blocks.OAK_SAPLING)) {
            throw new AssertionError(
                    "o rebento nao foi plantado: "
                            + world.getBlockState(ground.up()).getBlock());
        }

        context.complete();
    }

    /**
     * Lugar com coisa em cima não vira viveiro.
     *
     * <p>Sem esta metade o fazendeiro plantaria dentro de casa, e a
     * árvore crescendo levantaria o telhado.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "tree_nursery",
            tickLimit = 100)
    public void theOccupiedSpotIsNeverANursery(TestContext context) {
        ServerWorld world = context.getWorld();

        BlockPos ground = context.getAbsolutePos(new BlockPos(2, 1, 2));

        world.setBlockState(ground, Blocks.SAND.getDefaultState());

        world.setBlockState(ground.up(), Blocks.STONE.getDefaultState());

        if (TreeNursery.isSpotForANursery(world, ground)) {
            throw new AssertionError("lugar com bloco em cima foi aceito como viveiro");
        }

        if (TreeNursery.plant(world, ground, Blocks.OAK_SAPLING)) {
            throw new AssertionError("plantou por cima de um bloco que ja estava ali");
        }

        // E o chão não foi mexido: a recusa não pode deixar estrago.
        if (!world.getBlockState(ground).isOf(Blocks.SAND)) {
            throw new AssertionError(
                    "a recusa trocou o chao mesmo assim: "
                            + world.getBlockState(ground).getBlock());
        }

        context.complete();
    }

    /**
     * Cada bioma planta a sua espécie.
     *
     * <p>É o pedido do autor — <i>"cada bioma planta o tipo de rebento
     * que precisa para criar seus itens, portas e afins"</i> —, e a
     * espécie sai da mesma tabela que decide a tábua da obra.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "tree_nursery",
            tickLimit = 100)
    public void eachWoodHasItsOwnSapling(TestContext context) {
        if (!TreeNursery.saplingOf(ResourceId.vanilla("acacia_planks"))
                .filter(block -> block == Blocks.ACACIA_SAPLING)
                .isPresent()) {

            throw new AssertionError("a tabua de acacia nao achou o rebento de acacia");
        }

        if (!TreeNursery.saplingOf(ResourceId.vanilla("jungle_planks"))
                .filter(block -> block == Blocks.JUNGLE_SAPLING)
                .isPresent()) {

            throw new AssertionError("a tabua de selva nao achou o rebento de selva");
        }

        // E o que não tem rebento de verdade fica de fora: o bambu é
        // broto, não rebento, e plantá-lo daria um canteiro que nunca
        // vira árvore.
        if (TreeNursery.saplingOf(ResourceId.vanilla("bamboo_planks")).isPresent()) {
            throw new AssertionError(
                    "o bambu passou por rebento — a convencao de nome foi aceita sem o"
                            + " jogo confirmar");
        }

        context.complete();
    }

    /**
     * O ritmo segura o plantio seguinte.
     *
     * <p>Sem freio o fazendeiro planta uma árvore por passagem: ele chega
     * ao viveiro toda vez que varre o raio sem achar lavoura, o que numa
     * vila sem roça é sempre.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "tree_nursery",
            tickLimit = 100)
    public void theRhythmHoldsTheNextPlanting(TestContext context) {
        ServerWorld world = context.getWorld();

        UUID colony = UUID.randomUUID();

        FarmerNursery.clearAll();

        long now = world.getTime();

        if (!FarmerNursery.isTime(colony, now)) {
            throw new AssertionError("a primeira arvore foi barrada pelo ritmo");
        }

        // A colônia acabou de plantar: a passagem seguinte espera.
        FarmerNursery.remember(colony, now);

        if (FarmerNursery.isTime(colony, now + 1)) {
            throw new AssertionError(
                    "plantou de novo no tique seguinte — sem freio a borda inteira"
                            + " vira viveiro, porque o fazendeiro chega aqui toda"
                            + " passagem em que nao acha lavoura");
        }

        // E passado o intervalo ele planta de novo: um freio que nunca
        // solta seria uma arvore so, para sempre.
        if (!FarmerNursery.isTime(colony, now + FarmerNursery.BETWEEN_PLANTINGS)) {
            throw new AssertionError(
                    "passado o intervalo inteiro o ritmo continuou segurando — a vila"
                            + " planta uma arvore e nunca mais");
        }

        FarmerNursery.clearAll();

        context.complete();
    }

    /** A vila mantém dez viveiros, e não uma quantidade sem teto. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "tree_nursery",
            tickLimit = 100)
    public void theNurseryStopsAtTenTrees(TestContext context) {
        ServerWorld world = context.getWorld();
        UUID colony = UUID.randomUUID();
        BlockPos centre = context.getAbsolutePos(new BlockPos(8, 1, 8));

        FarmerNursery.clearAll();

        for (int dx = -29; dx <= 29; dx++) {
            for (int dz = -29; dz <= 29; dz++) {
                int distance = dx * dx + dz * dz;

                if (distance >= 20 * 20 && distance <= 29 * 29) {
                    world.setBlockState(
                            centre.add(dx, 0, dz), Blocks.SAND.getDefaultState());
                }
            }
        }

        for (int planted = 0; planted < 10; planted++) {
            long now = world.getTime();
            FarmerNursery.remember(
                    colony, now - FarmerNursery.BETWEEN_PLANTINGS);

            if (!FarmerNursery.plantIfItIsTime(world, colony, centre)) {
                throw new AssertionError(
                        "o viveiro parou antes de plantar as dez arvores: " + planted);
            }
        }

        long now = world.getTime();
        FarmerNursery.remember(colony, now - FarmerNursery.BETWEEN_PLANTINGS);

        if (FarmerNursery.plantIfItIsTime(world, colony, centre)) {
            throw new AssertionError("o viveiro plantou uma decima primeira arvore");
        }

        int rootedDirt = 0;
        for (int dx = -30; dx <= 30; dx++) {
            for (int dz = -30; dz <= 30; dz++) {
                if (world.getBlockState(centre.add(dx, 0, dz))
                        .isOf(Blocks.ROOTED_DIRT)) {
                    rootedDirt++;
                }
            }
        }

        if (rootedDirt != 10) {
            throw new AssertionError(
                    "a vila deveria ter dez bases de viveiro, mas tem " + rootedDirt);
        }

        FarmerNursery.clearAll();
        context.complete();
    }
}
