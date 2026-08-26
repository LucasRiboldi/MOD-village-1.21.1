package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BlockProtection;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * O que o trabalhador nunca pode quebrar — regra do autor, 2026-08-13.
 *
 * <p>O que este teste alcança e o que não alcança precisa ficar claro. O
 * mundo do gametest não tem vila gerada: estruturas nascem da geração de
 * terreno, e a bateria roda num mundo de teste vazio. Então a metade
 * "vila original" da regra é exercitada apenas no caminho em que não há
 * estrutura nenhuma — que é o caminho que roda a cada colheita, e o que
 * precisa ser barato e não travar.
 *
 * <p>A outra metade — bloco posto pelo jogador — é testável de verdade, e
 * está aqui.
 *
 * <p>Que a caixa da vila de fato protege é verificação de jogo, e está
 * registrada como tal no §8 do Project-State.
 */
public class BlockProtectionGameTest implements FabricGameTest {

    /** Fora de vila, e sem marca de mão: pode quebrar. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "protection_wild")
    public void aWildBlockMayBeBroken(TestContext context) {
        BlockPos pos = new BlockPos(2, 2, 2);

        context.setBlockState(pos, Blocks.OAK_LEAVES.getDefaultState());

        ServerWorld world = context.getWorld();
        BlockPos absolute = context.getAbsolutePos(pos);

        context.assertTrue(
                BlockProtection.mayBreak(world, absolute, world.getBlockState(absolute)),
                "folha de floresta, fora de vila, e a proteção recusou");

        context.assertTrue(
                !BlockProtection.isVillageOriginal(world, absolute),
                "não há vila gerada no mundo do teste, e a pergunta disse que sim");

        context.complete();
    }

    /**
     * O bloco de uma casa da colônia não se quebra — TASK-045, o E7.
     *
     * <p>{@code BuildingRegistry.isColonyInfrastructure} responde "este
     * bloco é da colônia?" desde 2026-08-14, e até esta data a porta única
     * do "posso quebrar isto?" não fazia a pergunta. Não causava dano
     * enquanto a única coisa que o mod quebrava era árvore — a regra da
     * copa já a separa de construção —, e passaria a causar na primeira
     * demolição de qualquer outra natureza.
     *
     * <p>A casa aqui é de pedra, e não de folha: sem a consulta ao
     * registro, nada nesta posição a protegeria.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "protection_building")
    public void aColonyBuildingIsProtected(TestContext context) {
        BlockPos pos = new BlockPos(2, 2, 2);

        context.setBlockState(pos, Blocks.STONE.getDefaultState());

        ServerWorld world = context.getWorld();
        BlockPos absolute = context.getAbsolutePos(pos);
        ColonyPos here = MinecraftTypeAdapter.toColonyPos(absolute);

        Colony colony = Colony.create(UUID.randomUUID(), here);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        try {
            context.assertTrue(
                    BlockProtection.mayBreak(world, absolute, world.getBlockState(absolute)),
                    "antes de existir casa aqui, a pedra devia ser quebrável");

            VillageColonyMod.BUILDINGS.register(new Building(
                    UUID.randomUUID(),
                    colony.id(),
                    new ResourceId("minecraft", "village/plains/houses/plains_small_house_1"),
                    here,
                    new ColonyPos(here.x() + 2, here.y() + 2, here.z() + 2)));

            context.assertTrue(
                    !BlockProtection.mayBreak(world, absolute, world.getBlockState(absolute)),
                    "a proteção deixou quebrar um bloco de uma casa da colônia");
        } finally {
            owned.cleanUp();
        }

        context.complete();
    }

    /** Folha pendurada à mão é bloco do jogador, e não se toca. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "protection_player")
    public void aBlockPlacedByHandIsProtected(TestContext context) {
        BlockPos pos = new BlockPos(2, 2, 2);

        context.setBlockState(
                pos, Blocks.OAK_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true));

        ServerWorld world = context.getWorld();
        BlockPos absolute = context.getAbsolutePos(pos);

        context.assertTrue(
                BlockProtection.isPlayerPlaced(world.getBlockState(absolute)),
                "a folha pendurada à mão não foi reconhecida como do jogador");

        context.assertTrue(
                !BlockProtection.mayBreak(world, absolute, world.getBlockState(absolute)),
                "a proteção deixou quebrar um bloco posto pelo jogador");

        context.complete();
    }
}
