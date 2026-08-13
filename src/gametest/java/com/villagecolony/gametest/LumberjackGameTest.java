package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.work.LumberjackWork;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BlockBreakTime;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ChestInventoryReader;
import com.villagecolony.fabric.integration.TreeHarvester;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.TreeScanner;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
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

import java.util.List;
import java.util.UUID;

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

    /**
     * Uma árvore de carvalho simples: quatro troncos sobre terra, e uma
     * folha da copa encostada no de cima.
     *
     * <p>A copa não é enfeite do teste. Desde 2026-08-12 tronco sem folha
     * viva não é árvore, é construção — e uma árvore de teste sem copa
     * provaria a regra errada: passaria por não ser tocada, e não por ser
     * colhida direito.
     *
     * <p>Ao lado do tronco, e não acima dele: a coluna acima da base é
     * onde a muda cresce, e três testes desta classe medem justamente o
     * que acontece nela.
     */
    private static BlockPos plantTree(TestContext context, BlockPos base) {
        context.setBlockState(base.down(), Blocks.DIRT.getDefaultState());

        for (int y = 0; y < 4; y++) {
            context.setBlockState(base.up(y), Blocks.OAK_LOG.getDefaultState());
        }

        context.setBlockState(base.up(3).north(), Blocks.OAK_LEAVES.getDefaultState());

        return base;
    }

    /** Um tronco pelado: o que uma casa de vila tem, e uma árvore não. */
    private static void raiseLogs(TestContext context, BlockPos base, int height) {
        context.setBlockState(base.down(), Blocks.DIRT.getDefaultState());

        for (int y = 0; y < height; y++) {
            context.setBlockState(base.up(y), Blocks.OAK_LOG.getDefaultState());
        }
    }

    /** Folha pendurada à mão: a mesma folha, com a marca do jogador. */
    private static void hangLeaf(TestContext context, BlockPos pos) {
        context.setBlockState(
                pos, Blocks.OAK_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true));
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

        // Duas: a que o teste pendurou aqui, e a que a árvore já tinha.
        context.assertTrue(
                harvest.leaves() == 2,
                "esperava 2 folhas colhidas, foram " + harvest.leaves());

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

    // ----------------------------------------------------------------
    // A copa é o que separa árvore de construção — 2026-08-12
    // ----------------------------------------------------------------

    /**
     * Tronco sem copa viva não é árvore.
     *
     * <p>Casa de planície é feita de tronco de carvalho, e o carvalho da
     * casa é exatamente o mesmo da floresta: espécie, bloco e drop. A
     * copa é a única diferença que o mundo registra entre uma coisa e a
     * outra, e é nela que a regra se apoia.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_no_canopy")
    public void aTrunkWithoutACanopyIsNotATree(TestContext context) {
        BlockPos base = new BlockPos(2, 2, 2);

        raiseLogs(context, base, 4);

        TreeHarvester.Harvest harvest =
                TreeHarvester.fell(context.getWorld(), context.getAbsolutePos(base));

        context.assertTrue(
                harvest.isEmpty(),
                "colheu " + harvest.logs() + " troncos de uma construção");

        for (int y = 0; y < 4; y++) {
            context.expectBlock(Blocks.OAK_LOG, base.up(y));
        }

        context.complete();
    }

    /**
     * Folha pendurada à mão não faz de um pilar uma árvore.
     *
     * <p>Sem isto a regra seria fácil de burlar sem querer: uma folha
     * decorativa no canto da casa devolveria a casa inteira à colheita. A
     * marca {@code persistent} do Vanilla é o que separa a folha que
     * nasceu ali da que alguém colocou.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_hung_leaves")
    public void leavesHungByHandAreNotACanopy(TestContext context) {
        BlockPos base = new BlockPos(2, 2, 2);
        BlockPos hung = base.up(3).north();

        raiseLogs(context, base, 4);
        hangLeaf(context, hung);

        TreeHarvester.Harvest harvest =
                TreeHarvester.fell(context.getWorld(), context.getAbsolutePos(base));

        context.assertTrue(
                harvest.isEmpty(),
                "a folha do jogador virou copa: colheu " + harvest.logs() + " troncos");

        context.expectBlock(Blocks.OAK_LOG, base);
        context.expectBlock(Blocks.OAK_LEAVES, hung);

        context.complete();
    }

    /**
     * A construção grande também é olhada.
     *
     * <p>Era o furo da primeira versão da regra: a copa só era procurada
     * quando o tronco cabia no teto de 24, e uma parede de vinte e cinco
     * troncos passava sem nunca ser olhada — justamente a construção que
     * mais dói perder.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_big_wall")
    public void aWallBiggerThanTheCeilingIsNotATree(TestContext context) {
        BlockPos corner = new BlockPos(1, 2, 2);

        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                context.setBlockState(corner.add(x, y, 0), Blocks.OAK_LOG.getDefaultState());
            }
        }

        TreeHarvester.Harvest harvest =
                TreeHarvester.fell(context.getWorld(), context.getAbsolutePos(corner));

        context.assertTrue(
                harvest.isEmpty(),
                "colheu " + harvest.logs() + " troncos de uma parede de 25");

        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                context.expectBlock(Blocks.OAK_LOG, corner.add(x, y, 0));
            }
        }

        context.complete();
    }

    /**
     * A limpeza da coluna para na folha que o jogador pendurou.
     *
     * <p>Mesma regra do telhado de tábua, e pelo mesmo motivo: acima da
     * muda pode estar a decoração de alguém. A folha que nasceu ali sai
     * da frente; a que foi pendurada, não.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_hung_ceiling")
    public void theClearanceStopsAtALeafHungByHand(TestContext context) {
        BlockPos base = new BlockPos(2, 2, 2);
        BlockPos hung = base.up(5);

        plantTree(context, base);
        hangLeaf(context, hung);

        TreeHarvester.fell(context.getWorld(), context.getAbsolutePos(base));

        context.expectBlock(Blocks.OAK_SAPLING, base);
        context.expectBlock(Blocks.OAK_LEAVES, hung);

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

        // Copa de bétula: a espécie percorre a colheita inteira, e sem
        // folha da própria espécie isto seria uma parede de bétula.
        context.setBlockState(base.up(3).north(), Blocks.BIRCH_LEAVES.getDefaultState());

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

    // ----------------------------------------------------------------
    // Regra 2 — colher no tempo de um jogador com ferramenta de ferro
    // ----------------------------------------------------------------

    /**
     * O tronco leva meio segundo, que é o que um jogador leva.
     *
     * <p>Dez ticks: dureza 2, machado de ferro com velocidade 6, divisor
     * 30 do Vanilla. O número não está escrito no código — sai da
     * fórmula do jogo — e é aqui que se prova que sai certo.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_break_time")
    public void aLogTakesHalfASecondWithAnIronAxe(TestContext context) {
        BlockPos base = new BlockPos(2, 2, 2);

        plantTree(context, base);

        BlockPos absolute = context.getAbsolutePos(base);

        int ticks = BlockBreakTime.ticksFor(
                context.getWorld(),
                absolute,
                context.getWorld().getBlockState(absolute),
                Items.IRON_AXE);

        context.assertTrue(ticks == 10, "esperava 10 ticks por tronco, deu " + ticks);

        context.complete();
    }

    /**
     * A árvore não cai mais dentro de um tick.
     *
     * <p>É a Regra 2 pelo lado que interessa: nenhum bloco custa zero, e
     * a colheita inteira é uma soma de esperas. Antes de 2026-08-08 uma
     * árvore de seis troncos e oitenta folhas desaparecia no mesmo
     * instante — visível, errado, e um pico de custo dentro de um tick.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_break_cost")
    public void everyBlockOfTheHarvestCostsAtLeastOneTick(TestContext context) {
        BlockPos base = new BlockPos(2, 2, 2);

        plantTree(context, base);
        context.setBlockState(base.up(4), Blocks.OAK_LEAVES.getDefaultState());

        ServerWorld world = context.getWorld();

        TreeHarvester.Plan plan = TreeHarvester.plan(world, context.getAbsolutePos(base));

        context.assertTrue(plan.blocks().size() >= 5, "o plano não pegou a árvore inteira");

        for (BlockPos pos : plan.blocks()) {
            int ticks = BlockBreakTime.ticksFor(
                    world, pos, world.getBlockState(pos), Items.IRON_AXE);

            context.assertTrue(ticks >= 1, "bloco de graça em " + pos.toShortString());
        }

        context.complete();
    }

    /**
     * Um bloco de cada vez deixa o resto de pé.
     *
     * <p>É o que permite a colheita durar: quebrar o primeiro tronco não
     * pode levar os outros junto, senão a Regra 2 seria só uma espera
     * antes de a árvore sumir inteira.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_one_by_one")
    public void breakingOneBlockLeavesTheRestStanding(TestContext context) {
        BlockPos base = new BlockPos(2, 2, 2);

        plantTree(context, base);

        ServerWorld world = context.getWorld();

        TreeHarvester.Plan plan = TreeHarvester.plan(world, context.getAbsolutePos(base));

        TreeHarvester.breakOne(world, plan, plan.blocks().get(0));

        int standing = 0;

        for (int y = 0; y < 4; y++) {
            if (world.getBlockState(context.getAbsolutePos(base.up(y)))
                    .isOf(Blocks.OAK_LOG)) {

                standing++;
            }
        }

        context.assertTrue(standing == 3, "esperava 3 troncos de pé, ficaram " + standing);

        context.complete();
    }

    /**
     * O bloco que o jogador trocou não é quebrado.
     *
     * <p>Entre planejar e chegar num bloco passam-se dezenas de ticks, e
     * nesse meio-tempo o jogador pode ter posto uma tábua ali. Quebrar o
     * que estiver na posição sem perguntar seria quebrar a construção
     * dele — e o risco só existe porque a colheita passou a demorar.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_changed_world")
    public void aBlockReplacedMidHarvestIsLeftAlone(TestContext context) {
        BlockPos base = new BlockPos(2, 2, 2);
        BlockPos top = base.up(3);

        plantTree(context, base);

        ServerWorld world = context.getWorld();

        TreeHarvester.Plan plan = TreeHarvester.plan(world, context.getAbsolutePos(base));

        // O jogador troca o tronco de cima por tábua depois do plano.
        context.setBlockState(top, Blocks.OAK_PLANKS.getDefaultState());

        List<ItemStack> drops =
                TreeHarvester.breakOne(world, plan, context.getAbsolutePos(top));

        context.assertTrue(drops.isEmpty(), "colheu o que não era da árvore");
        context.expectBlock(Blocks.OAK_PLANKS, top);

        context.complete();
    }

    // ----------------------------------------------------------------
    // Regra 1 — colher até os baús encherem
    // ----------------------------------------------------------------

    /**
     * O espaço é do grupo, e não de um item.
     *
     * <p>Um baú com meia pilha de bétula tem espaço para madeira mesmo
     * que o próximo tronco seja de carvalho. Perguntar por um item só
     * faria a colônia enxergar menos espaço do que tem, e parar de colher
     * antes da hora.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_group_space")
    public void freeSpaceCountsAnyWood(TestContext context) {
        BlockPos chest = new BlockPos(2, 2, 2);

        context.setBlockState(chest, Blocks.CHEST.getDefaultState());

        ServerWorld world = context.getWorld();
        ColonyPos chestPos = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(chest));

        int empty = ChestDepositor.freeSpaceForGroup(world, chestPos, ResourceGroup.WOOD);

        context.assertTrue(empty > 0, "baú vazio devia ter espaço, tinha " + empty);

        // Meia pilha de bétula num slot: o espaço cai, mas o que sobrou
        // naquele slot continua contando como espaço de madeira.
        ChestDepositor.deposit(world, chestPos, Items.BIRCH_LOG, 32);

        int afterBirch = ChestDepositor.freeSpaceForGroup(world, chestPos, ResourceGroup.WOOD);

        context.assertTrue(
                afterBirch == empty - 32,
                "esperava " + (empty - 32) + " de espaço, deu " + afterBirch);

        context.complete();
    }

    /**
     * O que não é recurso da colônia não vira espaço.
     *
     * <p>Um slot ocupado por item do jogador não é espaço da colônia. Se
     * contasse, a meta seria maior que o baú e o lenhador colheria
     * madeira que não caberia — que é exatamente a perda que a Regra 1
     * veio evitar.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_foreign_space")
    public void anotherPlayersItemIsNotFreeSpace(TestContext context) {
        BlockPos chest = new BlockPos(2, 2, 2);

        context.setBlockState(chest, Blocks.CHEST.getDefaultState());

        ServerWorld world = context.getWorld();
        ColonyPos chestPos = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(chest));

        int empty = ChestDepositor.freeSpaceForGroup(world, chestPos, ResourceGroup.WOOD);

        ChestDepositor.deposit(world, chestPos, Items.DIAMOND, 1);

        int afterDiamond = ChestDepositor.freeSpaceForGroup(world, chestPos, ResourceGroup.WOOD);

        context.assertTrue(
                afterDiamond == empty - 64,
                "o slot do jogador continuou contando como espaço: " + afterDiamond);

        context.complete();
    }

    /**
     * As duas regras juntas, num mundo que tica.
     *
     * <p>É o teste que os outros desta seção não substituem. Eles
     * verificam as peças — quanto tempo um bloco pede, um bloco por vez,
     * quanto cabe no baú — e todas podiam estar certas com o lenhador
     * parado, que foi exatamente o que aconteceu no bloqueio da Fase 8:
     * a versão que chamava {@code startMovingTo} passou por todos os
     * testes de derrubada e mesmo assim o aldeão nunca chegava à árvore.
     *
     * <p>Aqui a colônia tem tarefa, o lenhador tem baú, o aldeão está ao
     * pé da árvore e o servidor tica. O que se prova é o ritmo: aos
     * cinco ticks nenhum tronco caiu — porque o tronco pede dez —, e mais
     * adiante a árvore está no baú.
     *
     * <p>O relógio é posto no horário de trabalho de propósito: fora
     * dele o aldeão dorme, e o teste passaria por engano.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_continuous",
            tickLimit = 400)
    public void theHarvestTakesTimeAndEndsInTheChest(TestContext context) {
        BlockPos base = new BlockPos(4, 2, 4);
        BlockPos chest = new BlockPos(2, 2, 2);
        BlockPos stand = new BlockPos(3, 2, 4);

        clearColonyState();

        plantTree(context, base);
        context.setBlockState(chest, Blocks.CHEST.getDefaultState());
        context.getWorld().setTimeOfDay(Schedule.WORK_TIME);

        ServerWorld world = context.getWorld();

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, stand);
        villager.setBreedingAge(0);

        Colony colony = Colony.create(
                UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(base)));

        VillageColonyMod.COLONIES.register(colony);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.LUMBERJACK);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(
                villager.getUuid(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(chest))));

        Task task = VillageColonyMod.TASKS.create(
                colony.id(),
                TaskType.COLLECT_WOOD,
                TaskPriority.PRODUCTION,
                ResourceType.OAK_LOG,
                64);

        task.reserveFor(villager.getUuid());

        // Despacho: abre o trabalho. Daqui em diante quem age é o tick do
        // servidor, que é o que este teste quer exercitar.
        LumberjackWork.run(world, colony);

        context.runAtTick(5, () -> {
            int standing = logsStanding(context, base);

            context.assertTrue(
                    standing == 4,
                    "aos 5 ticks o tronco não podia ter caído — de pé: " + standing);
        });

        context.runAtTick(320, () -> {
            int stored = ChestInventoryReader
                    .read(world, context.getAbsolutePos(chest))
                    .amountOf(ResourceType.OAK_LOG);

            context.assertTrue(stored > 0, "a madeira não chegou ao baú");

            clearColonyState();
            LumberjackWork.clearAll();
            WorkTargets.clear(villager.getUuid());

            context.complete();
        });
    }

    /**
     * Dois lenhadores, duas árvores.
     *
     * <p>A busca parte sempre do centro da colônia e é determinística:
     * sem reserva, os dois recebem a mesma árvore, um só a derruba e o
     * outro fica ao lado de um toco. É o defeito que nasceu no instante
     * em que a colônia passou a abrir uma tarefa por trabalhador.
     *
     * <p>Dois ticks porque a varredura por árvore tem orçamento de uma
     * por tick no servidor inteiro — é o teto que impede o trabalho
     * contínuo de virar varredura contínua.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_two_workers",
            tickLimit = 200)
    public void twoLumberjacksDoNotShareATree(TestContext context) {
        BlockPos first = new BlockPos(2, 2, 2);
        BlockPos second = new BlockPos(6, 2, 6);

        clearColonyState();

        plantTree(context, first);
        plantTree(context, second);
        context.getWorld().setTimeOfDay(Schedule.WORK_TIME);

        ServerWorld world = context.getWorld();

        Colony colony = Colony.create(
                UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(first)));

        VillageColonyMod.COLONIES.register(colony);

        for (int i = 0; i < 2; i++) {
            BlockPos chest = new BlockPos(1, 2, 4 + i);

            context.setBlockState(chest, Blocks.CHEST.getDefaultState());

            VillagerEntity villager = context.spawnEntity(
                    EntityType.VILLAGER, new BlockPos(4, 2, 4));
            villager.setBreedingAge(0);

            Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
            worker.assign(ProfessionType.LUMBERJACK);

            VillageColonyMod.STORAGES.register(WorkerStorage.of(
                    villager.getUuid(),
                    MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(chest))));

            Task task = VillageColonyMod.TASKS.create(
                    colony.id(),
                    TaskType.COLLECT_WOOD,
                    TaskPriority.PRODUCTION,
                    ResourceType.OAK_LOG,
                    32);

            task.reserveFor(villager.getUuid());
        }

        LumberjackWork.run(world, colony);

        context.runAtTick(4, () -> {
            List<BlockPos> working = LumberjackWork.blocksInProgress();

            context.assertTrue(
                    working.size() == 2,
                    "esperava dois lenhadores com árvore, foram " + working.size());

            context.assertTrue(
                    !working.get(0).equals(working.get(1)),
                    "os dois pegaram o mesmo tronco: " + working.get(0).toShortString());

            // E são de árvores diferentes, não dois troncos da mesma.
            int firstColumn = 0;

            for (BlockPos pos : working) {
                if (pos.getX() == context.getAbsolutePos(first).getX()
                        && pos.getZ() == context.getAbsolutePos(first).getZ()) {

                    firstColumn++;
                }
            }

            context.assertTrue(
                    firstColumn == 1,
                    "os dois foram para a mesma árvore: " + firstColumn + " na primeira");

            clearColonyState();
            LumberjackWork.clearAll();

            context.complete();
        });
    }

    /** Quantos troncos da árvore ainda estão de pé. */
    private static int logsStanding(TestContext context, BlockPos base) {
        int standing = 0;

        for (int y = 0; y < 4; y++) {
            if (context.getWorld().getBlockState(context.getAbsolutePos(base.up(y)))
                    .isOf(Blocks.OAK_LOG)) {

                standing++;
            }
        }

        return standing;
    }

    /**
     * Zera o estado do mod entre testes.
     *
     * <p>Os registros são estáticos e vivem no servidor, que é um só para
     * a bateria inteira. Sem isto, a colônia de um teste apareceria na do
     * seguinte, e a ordem de execução — que não é garantida — mudaria o
     * resultado.
     */
    private static void clearColonyState() {
        VillageColonyMod.COLONIES.clear();
        VillageColonyMod.WORKERS.clear();
        VillageColonyMod.STORAGES.clear();
        VillageColonyMod.TASKS.clear();
        LumberjackWork.clearAll();
        TreeScanner.clearAll();
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

    /**
     * A busca continua de onde parou, e depois volta ao centro.
     *
     * <p>O teto de mil e vinte e quatro colunas acaba no anel dezesseis.
     * Até 2026-08-12 toda busca recomeçava do centro, então o raio de 64
     * era decorativo: a colônia de {@code 1109,730} passou uma sessão
     * inteira sem achar árvore porque a floresta dela começa depois do
     * décimo sexto bloco, e a busca morria no mesmo lugar todo ciclo.
     *
     * <p>O que este teste exige é a consequência, não o cursor: uma
     * árvore plantada perto **depois** da primeira busca não é vista na
     * segunda — sinal de que ela está longe, continuando — e volta a ser
     * vista quando a varredura completa a volta e o cursor zera.
     *
     * <p>O filtro por coluna existe para que árvore de outra estrutura de
     * teste, dentro do raio, não responda pela nossa.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_search_cursor")
    public void theSearchMovesOutwardAndComesBack(TestContext context) {
        BlockPos base = new BlockPos(4, 2, 4);
        BlockPos center = context.getAbsolutePos(new BlockPos(2, 2, 2));
        BlockPos trunk = context.getAbsolutePos(base);

        TreeScanner.clearAll();

        java.util.function.Predicate<BlockPos> onlyOurs =
                pos -> pos.getX() == trunk.getX() && pos.getZ() == trunk.getZ();

        ServerWorld world = context.getWorld();

        context.assertTrue(
                TreeScanner.findNearestLog(world, center, 24, onlyOurs).isEmpty(),
                "achou a nossa árvore antes de ela existir");

        plantTree(context, base);

        context.assertTrue(
                TreeScanner.findNearestLog(world, center, 24, onlyOurs).isEmpty(),
                "a busca recomeçou do centro em vez de continuar de onde parou");

        boolean found = false;

        for (int attempt = 0; attempt < 10 && !found; attempt++) {
            found = TreeScanner.findNearestLog(world, center, 24, onlyOurs).isPresent();
        }

        context.assertTrue(found, "a busca deu a volta e não voltou a olhar perto");

        TreeScanner.clearAll();

        context.complete();
    }
}
