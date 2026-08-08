package com.villagecolony.gametest;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ChestInventoryReader;
import com.villagecolony.fabric.integration.TreeHarvester;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.TreeScanner;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.Schedule;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

/**
 * O lenhador derrubando — Fase 8.
 *
 * <p>Primeiro código do mod que escreve no mundo, e por isso o primeiro
 * em que um defeito estraga o save de quem joga. Estes testes correm num
 * mundo descartável: é exatamente o que o item A do §8 comprou.
 *
 * <p>Cada regra decidida pelo autor em 2026-08-08 tem um teste, e os
 * negativos importam mais que os positivos: o que o lenhador **não**
 * pode quebrar é o que protege a construção do jogador.
 */
public class LumberjackGameTest implements FabricGameTest {

    /** Uma árvore de carvalho simples: quatro troncos sobre terra. */
    private static BlockPos plantTree(TestContext context, BlockPos base) {
        context.setBlockState(base.down(), Blocks.DIRT.getDefaultState());

        for (int y = 0; y < 4; y++) {
            context.setBlockState(base.up(y), Blocks.OAK_LOG.getDefaultState());
        }

        return base;
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_fell")
    public void fellingTakesTheWholeTrunk(TestContext context) {
        BlockPos base = new BlockPos(2, 2, 2);

        plantTree(context, base);

        int felled = TreeHarvester.fell(context.getWorld(), context.getAbsolutePos(base)).logs();

        context.assertTrue(felled == 4, "esperava 4 troncos derrubados, foram " + felled);

        for (int y = 1; y < 4; y++) {
            context.expectBlock(Blocks.AIR, base.up(y));
        }

        context.complete();
    }

    /**
     * A muda entra no lugar da base.
     *
     * <p>É o que impede a floresta ao redor da vila de sumir com o tempo,
     * e a única parte da regra que se autocorrige sem o jogador notar.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_replant")
    public void fellingReplantsASapling(TestContext context) {
        BlockPos base = new BlockPos(2, 2, 2);

        plantTree(context, base);

        TreeHarvester.fell(context.getWorld(), context.getAbsolutePos(base));

        context.expectBlock(Blocks.OAK_SAPLING, base);

        context.complete();
    }

    /**
     * A folha em cima da muda sai da frente.
     *
     * <p>Pedido do autor em 2026-08-08. A copa da árvore derrubada fica
     * de pé, e a folha logo acima da base é justamente o que impede a
     * muda de virar árvore: ela ficaria plantada para sempre debaixo da
     * copa da antecessora, e a floresta não se reporia.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_clearance")
    public void replantingOpensTheColumnAbove(TestContext context) {
        BlockPos base = new BlockPos(2, 2, 2);

        plantTree(context, base);
        context.setBlockState(base.up(4), Blocks.OAK_LEAVES.getDefaultState());
        context.setBlockState(base.up(5), Blocks.OAK_LEAVES.getDefaultState());

        TreeHarvester.fell(context.getWorld(), context.getAbsolutePos(base));

        context.expectBlock(Blocks.OAK_SAPLING, base);
        context.expectBlock(Blocks.AIR, base.up(4));
        context.expectBlock(Blocks.AIR, base.up(5));

        context.complete();
    }

    /**
     * A limpeza para no que não é folha.
     *
     * <p>Uma varanda do jogador acima da árvore encerra a limpeza ali. A
     * muda não vai crescer, e isso é problema dela — não licença para
     * abrir buraco em construção alheia.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_ceiling")
    public void theClearanceStopsAtWhatIsNotALeaf(TestContext context) {
        BlockPos base = new BlockPos(2, 2, 2);
        BlockPos floor = base.up(5);
        BlockPos leafAbove = base.up(6);

        plantTree(context, base);
        context.setBlockState(floor, Blocks.OAK_PLANKS.getDefaultState());
        context.setBlockState(leafAbove, Blocks.OAK_LEAVES.getDefaultState());

        TreeHarvester.fell(context.getWorld(), context.getAbsolutePos(base));

        context.expectBlock(Blocks.OAK_PLANKS, floor);
        context.expectBlock(Blocks.OAK_LEAVES, leafAbove);

        context.complete();
    }

    /**
     * Árvore que não cabe no baú fica de pé.
     *
     * <p>O tronco sai do mundo sem drop. Derrubar sem ter onde guardar
     * não seria colher: seria destruir a árvore e não ficar com nada.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_full_chest")
    public void aFullChestLeavesTheTreeStanding(TestContext context) {
        BlockPos base = new BlockPos(2, 2, 2);
        BlockPos chest = new BlockPos(5, 2, 2);

        plantTree(context, base);
        context.setBlockState(chest, Blocks.CHEST.getDefaultState());

        ServerWorld world = context.getWorld();
        ColonyPos chestPos = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(chest));

        int room = ChestDepositor.freeSpaceFor(world, chestPos, Items.OAK_LOG);

        context.assertTrue(room > 0, "baú vazio devia ter espaço, tinha " + room);

        // Enche o baú até não caber mais nenhum tronco.
        int leftOver = ChestDepositor.deposit(world, chestPos, Items.OAK_LOG, room);

        context.assertTrue(leftOver == 0, "não coube o que o próprio baú disse caber");

        context.assertTrue(
                ChestDepositor.freeSpaceFor(world, chestPos, Items.OAK_LOG) == 0,
                "o baú devia estar cheio");

        context.expectBlock(Blocks.OAK_LOG, base);

        context.complete();
    }

    /**
     * A copa da árvore derrubada vem junto.
     *
     * <p>Regra nova de 2026-08-08: o lenhador recolhe tudo o que a
     * árvore dropa, e muda, maçã e graveto vêm da folha. A folha ligada
     * ao tronco que caiu é copa dele.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_canopy")
    public void thecanopyComesDownWithTheTrunk(TestContext context) {
        BlockPos base = new BlockPos(2, 2, 2);
        BlockPos canopy = base.up(3).east();

        plantTree(context, base);
        context.setBlockState(canopy, Blocks.OAK_LEAVES.getDefaultState());

        TreeHarvester.Harvest harvest =
                TreeHarvester.fell(context.getWorld(), context.getAbsolutePos(base));

        context.expectBlock(Blocks.AIR, canopy);

        context.assertTrue(
                harvest.leaves() == 1,
                "esperava 1 folha colhida, foram " + harvest.leaves());

        context.complete();
    }

    /**
     * A folha da árvore de trás fica onde está.
     *
     * <p>Copas encostadas ligariam uma árvore à vizinha, e derrubar uma
     * levaria a copa de meia floresta. A folha longe do tronco que caiu
     * não é copa dele — é o que impede a colheita de virar desmatamento.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_far_leaves")
    public void leavesFarFromTheTrunkStay(TestContext context) {
        BlockPos base = new BlockPos(2, 2, 2);
        BlockPos faraway = base.up(2).east(8);

        plantTree(context, base);
        context.setBlockState(faraway, Blocks.OAK_LEAVES.getDefaultState());

        TreeHarvester.fell(context.getWorld(), context.getAbsolutePos(base));

        context.expectBlock(Blocks.OAK_LEAVES, faraway);

        context.complete();
    }

    /**
     * A folha de outra espécie não é copa desta árvore.
     *
     * <p>Uma parede de folha de bétula encostada num carvalho é parede.
     * A colheita é por espécie, do tronco à muda.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_leaves")
    public void leavesOfAnotherSpeciesStay(TestContext context) {
        BlockPos base = new BlockPos(2, 2, 2);
        BlockPos foreign = base.up(2).east();

        plantTree(context, base);
        context.setBlockState(foreign, Blocks.BIRCH_LEAVES.getDefaultState());

        TreeHarvester.fell(context.getWorld(), context.getAbsolutePos(base));

        context.expectBlock(Blocks.BIRCH_LEAVES, foreign);

        context.complete();
    }

    /**
     * Qualquer árvore da tabela, e a muda é da própria espécie.
     *
     * <p>Pedido do autor: o lenhador corta todo tipo de árvore. Bétula
     * aqui vale pelas oito — o caminho é o mesmo para todas, e o que se
     * verifica é que a espécie percorre a colheita inteira, do tronco à
     * muda replantada.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_birch_tree")
    public void anyTreeInTheTableIsFelledAndReplanted(TestContext context) {
        BlockPos base = new BlockPos(2, 2, 2);

        context.setBlockState(base.down(), Blocks.DIRT.getDefaultState());

        for (int y = 0; y < 4; y++) {
            context.setBlockState(base.up(y), Blocks.BIRCH_LOG.getDefaultState());
        }

        TreeHarvester.Harvest harvest =
                TreeHarvester.fell(context.getWorld(), context.getAbsolutePos(base));

        context.assertTrue(
                harvest.logs() == 4, "esperava 4 troncos de bétula, foram " + harvest.logs());

        context.expectBlock(Blocks.BIRCH_SAPLING, base);

        context.complete();
    }

    /**
     * O que a árvore dropa volta como item, e não fica no chão.
     *
     * <p>É o que o baú recebe. Item no chão despawna, cai n'água e é
     * roubado por mob, e a contagem da colônia passaria a mentir.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_drops")
    public void theHarvestComesBackAsItems(TestContext context) {
        BlockPos base = new BlockPos(2, 2, 2);

        plantTree(context, base);

        TreeHarvester.Harvest harvest =
                TreeHarvester.fell(context.getWorld(), context.getAbsolutePos(base));

        int logs = 0;

        for (ItemStack stack : harvest.drops()) {
            if (stack.isOf(Items.OAK_LOG)) {
                logs += stack.getCount();
            }
        }

        context.assertTrue(
                logs == harvest.logs(),
                "derrubou " + harvest.logs() + " troncos e devolveu " + logs + " itens");

        context.complete();
    }

    /**
     * Madeira que não é carvalho fica de pé.
     *
     * <p>A regra é {@code oak_log} e nada mais. Uma casa de bétula
     * encostada numa árvore não pode virar estoque da colônia.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_birch")
    public void fellingIgnoresOtherWoods(TestContext context) {
        BlockPos base = new BlockPos(2, 2, 2);
        BlockPos birch = base.up(1).east();

        plantTree(context, base);
        context.setBlockState(birch, Blocks.BIRCH_LOG.getDefaultState());

        TreeHarvester.fell(context.getWorld(), context.getAbsolutePos(base));

        context.expectBlock(Blocks.BIRCH_LOG, birch);

        context.complete();
    }

    /** A madeira derrubada entra no baú, e a colônia a conta. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_deposit")
    public void theWoodGoesIntoTheChest(TestContext context) {
        BlockPos base = new BlockPos(2, 2, 2);
        BlockPos chest = new BlockPos(5, 2, 2);

        plantTree(context, base);
        context.setBlockState(chest, Blocks.CHEST.getDefaultState());

        ServerWorld world = context.getWorld();

        int felled = TreeHarvester.fell(world, context.getAbsolutePos(base)).logs();

        ColonyPos chestPos =
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(chest));

        int leftOver = ChestDepositor.deposit(world, chestPos, Items.OAK_LOG, felled);

        context.assertTrue(leftOver == 0, "sobrou madeira sem lugar: " + leftOver);

        int stored = ChestInventoryReader
                .read(world, context.getAbsolutePos(chest))
                .amountOf(ResourceType.OAK_LOG);

        context.assertTrue(
                stored == felled,
                "derrubou " + felled + " e o baú guardou " + stored);

        context.complete();
    }

    /**
     * O aldeão anda até onde a colônia mandou.
     *
     * <p>É o bloqueio da Fase 8, e o motivo de existir a task no Brain: a
     * versão que chamava {@code startMovingTo} direto passava por todos os
     * outros testes desta classe e mesmo assim o lenhador nunca chegava à
     * árvore em jogo. Só um teste que **tique o mundo** com um aldeão
     * dentro pega isso — os de derrubada não tocam no cérebro dele.
     *
     * <p>O relógio é posto no horário de trabalho de propósito: fora
     * dele, a task deve mesmo ficar quieta, e o teste passaria por
     * engano.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_walk", tickLimit = 300)
    public void theVillagerWalksToWhereTheColonyAsked(TestContext context) {
        BlockPos start = new BlockPos(1, 2, 1);
        BlockPos target = new BlockPos(7, 2, 5);

        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 6; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.DIRT.getDefaultState());
            }
        }

        context.getWorld().setTimeOfDay(Schedule.WORK_TIME);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, start);
        BlockPos absoluteTarget = context.getAbsolutePos(target);

        WorkTargets.set(villager.getUuid(), absoluteTarget);

        double startDistance = villager.getBlockPos().getSquaredDistance(absoluteTarget);

        context.runAtTick(200, () -> {
            double now = villager.getBlockPos().getSquaredDistance(absoluteTarget);

            context.assertTrue(
                    villager.getBrain().getOptionalRegisteredMemory(MemoryModuleType.WALK_TARGET)
                            .isPresent(),
                    "o Brain do aldeão não recebeu destino nenhum");

            context.assertTrue(
                    now < startDistance,
                    "o aldeão não se aproximou: saiu a " + startDistance + " e está a " + now);

            WorkTargets.clear(villager.getUuid());

            context.complete();
        });
    }

    /**
     * A busca acha a árvore que está ao alcance.
     *
     * <p>O raio de 64 é o do jogo; aqui a árvore está a poucos blocos, e
     * o que se verifica é que a varredura por colunas a encontra.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_search")
    public void theSearchFindsATreeNearby(TestContext context) {
        BlockPos base = new BlockPos(4, 2, 4);
        BlockPos center = new BlockPos(1, 2, 1);

        plantTree(context, base);

        boolean found = TreeScanner.findNearestLog(
                        context.getWorld(), context.getAbsolutePos(center), 16)
                .isPresent();

        context.assertTrue(found, "a árvore ao lado não foi encontrada");

        context.complete();
    }
}
