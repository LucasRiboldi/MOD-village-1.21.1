package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.work.LumberjackWork;
import com.villagecolony.fabric.work.TreeChoice;
import com.villagecolony.fabric.work.TreeMarks;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BlockBreakTime;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ChestInventoryReader;
import com.villagecolony.fabric.integration.ColonyChests;
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

    /**
     * Árvore alta demais para o teto de colheita continua sendo árvore.
     *
     * <p>O defeito de 2026-08-19, visto em jogo: quatro recusas seguidas
     * de {@code 24 logs without a living canopy}, sempre com o mesmo
     * número — e 24 é o teto de colheita, não uma medida de árvore. A
     * copa era procurada a partir do grupo de troncos <b>já cortado no
     * teto</b>; num abeto gigante os 24 primeiros troncos são a base, e
     * a copa fica muito acima deles. A árvore virava "não é árvore", e a
     * recusa é permanente.
     *
     * <p>O teto é encurtado aqui porque a arena tem oito blocos de
     * altura e um abeto gigante não cabe nela. A geometria é a mesma: um
     * tronco mais alto que o teto, com a copa em cima.
     *
     * <p>Rodado contra a correção desligada: o plano volta vazio e a
     * afirmação falha.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_tall_tree")
    public void aTreeTallerThanTheHarvestCeilingIsStillATree(TestContext context) {
        BlockPos base = new BlockPos(3, 2, 3);

        // Seis troncos e a copa no alto, com o teto em quatro.
        context.setBlockState(base.down(), Blocks.DIRT.getDefaultState());

        for (int y = 0; y < 6; y++) {
            context.setBlockState(base.up(y), Blocks.OAK_LOG.getDefaultState());
        }

        context.setBlockState(base.up(5).north(), Blocks.OAK_LEAVES.getDefaultState());
        context.setBlockState(base.up(6), Blocks.OAK_LEAVES.getDefaultState());

        TreeHarvester.shortenHarvestCeilingTo(4);

        try {
            TreeHarvester.Plan plan = TreeHarvester.plan(
                    context.getWorld(), context.getAbsolutePos(base));

            context.assertTrue(
                    !plan.isEmpty(),
                    "o tronco tem copa viva no alto e foi recusado como 'não é árvore'"
                            + " — o teto de colheita não pode decidir isso");
        } finally {
            TreeHarvester.restoreHarvestCeiling();
        }

        context.complete();
    }

    /**
     * E o tronco pelado continua não sendo árvore.
     *
     * <p>A outra metade: a correção acima não pode ter afrouxado a
     * regra que protege a construção do jogador. Um pilar de troncos
     * sem copa é casa, e a Regra 3 manda não tocar.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_tall_tree")
    public void aTallBareTrunkIsStillNotATree(TestContext context) {
        BlockPos base = new BlockPos(5, 2, 5);

        raiseLogs(context, base, 6);

        TreeHarvester.shortenHarvestCeilingTo(4);

        try {
            TreeHarvester.Plan plan = TreeHarvester.plan(
                    context.getWorld(), context.getAbsolutePos(base));

            context.assertTrue(
                    plan.isEmpty(),
                    "um pilar de troncos sem copa virou árvore — a Regra 3 caiu junto");
        } finally {
            TreeHarvester.restoreHarvestCeiling();
        }

        context.complete();
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

        ColonyFixture owned = ColonyFixture.create()
                .owning(colony)
                .owning(villager.getUuid());

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

            try {
                context.assertTrue(stored > 0, "a madeira não chegou ao baú");
            } finally {
                owned.cleanUp();
            }

            context.complete();
        });
    }

    /**
     * O guarda de travamento, enfim coberto — o E1 do grupo E.
     *
     * <p>Ele existe desde a TASK-050 e nunca teve teste: 2.400 ticks são
     * dois minutos de relógio contra uma bateria que roda em vinte e
     * cinco segundos. O autor autorizou encurtar o limite em 2026-08-15,
     * e é isso que este teste faz.
     *
     * <p>Prova as duas coisas que o guarda faz, e a segunda entrou com a
     * Regra 9: a tarefa volta para a fila, <b>e</b> a árvore é esquecida.
     * Sem a segunda, a busca reescolhe a mesma árvore no ciclo seguinte —
     * ela é a mais próxima, e a busca é determinística — e o lenhador
     * seguinte trava no mesmo lugar.
     *
     * <p>A árvore fica numa torre sem escada, alcançável pela busca e não
     * pelos pés.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_stall_guard",
            tickLimit = 400)
    public void theStallGuardReturnsTheTaskAndForgetsTheTree(TestContext context) {
        BlockPos marooned = new BlockPos(4, 6, 4);
        BlockPos chest = new BlockPos(2, 2, 2);
        BlockPos stand = new BlockPos(3, 2, 6);

        for (int x = 1; x <= 8; x++) {
            for (int z = 1; z <= 8; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.STONE.getDefaultState());
            }
        }

        // Uma coluna isolada com a árvore em cima: a busca a enxerga, os
        // pés não chegam nela.
        for (int y = 2; y <= 5; y++) {
            context.setBlockState(new BlockPos(4, y, 4), Blocks.STONE.getDefaultState());
        }

        plantTree(context, marooned);

        context.setBlockState(chest, Blocks.CHEST.getDefaultState());
        context.getWorld().setTimeOfDay(Schedule.WORK_TIME);

        ServerWorld world = context.getWorld();

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, stand);
        villager.setBreedingAge(0);

        Colony colony = Colony.create(
                UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(marooned)));

        // A colônia deste teste NÃO entra em COLONIES, e isso é o
        // conserto do E20 — medido em 2026-08-26.
        //
        // O guarda sempre funcionou. A instrumentação de uma rodada
        // vermelha mostrou a tarefa voltando à fila no tique 61 e ficando
        // lá até o 240, com o job encerrado; no 260 ela aparecia RESERVED
        // de novo, com job novo. Quem a reservava era o ciclo longo:
        // `onServerTick` tem contador estático do processo, e a cada 600
        // ticks ele varre TODAS as colônias registradas — a fase em que
        // cada teste pega esse contador é arbitrária, e é daí que vinha a
        // alternância de uma rodada em quatro.
        //
        // A árvore marcada não era o alvo: a marca vale 6.000 ticks e
        // ainda estava de pé. O despacho achava OUTRA árvore — o raio de
        // busca alcança a arena dos testes vizinhos, que rodam
        // concorrentes.
        //
        // Nada aqui precisa do registro: `run` recebe a colônia na mão, e
        // `LumberjackWork` não tem uma única referência a COLONIES — o
        // `tick` percorre o próprio mapa de trabalhos. Registrar só punha
        // um segundo ator mexendo no que o teste mede.
        //
        // Ver Project-State.md §20.

        ColonyFixture owned = ColonyFixture.create()
                .owning(colony)
                .owning(villager.getUuid());

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

        // Sessenta ticks de horário de trabalho em vez de 2.400.
        TreeChoice.shortenStallLimitTo(60);

        LumberjackWork.run(world, colony);

        context.runAtTick(300, () -> {
            TreeChoice.restoreStallLimit();

            try {
                context.assertTrue(
                        task.state() == TaskState.AVAILABLE,
                        "o guarda não devolveu a tarefa à fila — ela está em " + task.state());

                context.assertTrue(
                        task.executor().isEmpty(),
                        "a tarefa voltou à fila e continua com dono");

                // A segunda metade da Regra 9, que o javadoc acima
                // prometia desde sempre e ninguém conferia — 2026-08-26.
                //
                // Sem ela, a tarefa volta para a fila e a busca reescolhe
                // esta mesma árvore: ela é a mais próxima e a busca é
                // determinística. Trocaria de trabalhador, e não de
                // problema. Quem prova o filtro é
                // theTreeMarkedOutOfReachIsSkipped, que marca à mão; o
                // que faltava provar é que o guarda MARCA.
                context.assertTrue(
                        TreeMarks.isOutOfReach(world, context.getAbsolutePos(marooned)),
                        "o guarda devolveu a tarefa e não esqueceu a árvore de "
                                + marooned.toShortString()
                                + " — o substituto vai travar no mesmo lugar");
            } finally {
                TreeMarks.forgetUnreachable();

                owned.cleanUp();
            }

            context.complete();
        });
    }

    /**
     * A Regra 9: árvore fora de alcance sai da escolha.
     *
     * <p>O autor decidiu a leitura estreita em 2026-08-15 — só
     * navegação, o lenhador não põe nem tira bloco para chegar. Então a
     * árvore a que ele não chega deixa de ser alvo por um tempo, e ele
     * vai atrás de outra.
     *
     * <p>É o G2: em jogo, dois lenhadores passaram dezesseis minutos a
     * sete e nove blocos de uma árvore sem nunca chegar. A busca é
     * determinística a partir do centro, então a mais próxima era
     * escolhida de novo a cada ciclo — soltar a tarefa sem esquecer a
     * árvore trocava de trabalhador, não de problema.
     *
     * <p>Este teste marca a árvore de perto e prova o <b>filtro</b>: o
     * lenhador pula a marcada e derruba a outra. Quem marca em jogo é
     * {@code giveUp}, e essa metade a bateria não alcança — são 2.400
     * ticks contra vinte e cinco segundos, o E1 do grupo E.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_unreachable",
            tickLimit = 400)
    public void theTreeMarkedOutOfReachIsSkipped(TestContext context) {
        // As duas árvores ao alcance do braço, e o lenhador entre elas.
        // Sem caminhada de propósito: o que este teste mede é a escolha
        // da busca, e uma caminhada de nove blocos mediria a navegação
        // junto — foi assim que a primeira versão dele ficou instável.
        BlockPos near = new BlockPos(4, 2, 4);
        BlockPos far = new BlockPos(6, 2, 4);
        BlockPos chest = new BlockPos(2, 2, 2);
        BlockPos stand = new BlockPos(5, 2, 5);

        for (int x = 1; x <= 8; x++) {
            for (int z = 1; z <= 8; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.STONE.getDefaultState());
            }
        }

        plantTree(context, near);
        plantTree(context, far);

        context.setBlockState(chest, Blocks.CHEST.getDefaultState());
        context.getWorld().setTimeOfDay(Schedule.WORK_TIME);

        ServerWorld world = context.getWorld();

        // A de perto está fora de alcance, e a busca tem de pular por
        // cima dela em vez de escolhê-la por ser a mais próxima.
        TreeMarks.markUnreachable(world, context.getAbsolutePos(near));

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, stand);
        villager.setBreedingAge(0);

        Colony colony = Colony.create(
                UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(near)));

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create()
                .owning(colony)
                .owning(villager.getUuid());

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

        LumberjackWork.run(world, colony);

        context.runAtTick(360, () -> {
            try {
                context.assertTrue(
                        logsStanding(context, near) == 4,
                        "a árvore marcada fora de alcance foi derrubada assim mesmo");

                context.assertTrue(
                        logsStanding(context, far) < 4,
                        "o lenhador pulou a marcada e não foi atrás de nenhuma outra");
            } finally {
                // As áreas de teste são reaproveitadas, e a marca dura 6.000
                // ticks: deixá-la aqui faria o teste seguinte encontrar uma
                // árvore boa marcada como fora de alcance.
                TreeMarks.forgetUnreachable();

                owned.cleanUp();
            }

            context.complete();
        });
    }

    /**
     * A regra do autor: o lenhador sempre planta onde cortou.
     *
     * <p>{@code fellingReplantsASapling} já provava a muda, mas pela
     * porta de {@code TreeHarvester.fell} — a que derruba tudo num tick.
     * Em jogo quem derruba é o trabalhador, tick a tick, e o replantio
     * dele mora noutro lugar: em {@code startNextTree}, quando ele vai
     * procurar a árvore seguinte.
     *
     * <p>É a mesma distância entre teste e jogo que custou o E14 — lá o
     * teste criava a tarefa à mão e a colônia nunca a criava. A pergunta
     * não é "o replantio funciona?", é <em>"quem planta esta muda, em
     * jogo?"</em>.
     *
     * <p>Aqui a árvore desce pelas mãos do lenhador, e a muda tem de
     * estar no lugar da base ao fim.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_worker_replant",
            tickLimit = 400)
    public void theLumberjackPlantsWhereHeCut(TestContext context) {
        BlockPos base = new BlockPos(4, 2, 4);
        BlockPos chest = new BlockPos(2, 2, 2);
        BlockPos stand = new BlockPos(3, 2, 4);

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

        ColonyFixture owned = ColonyFixture.create()
                .owning(colony)
                .owning(villager.getUuid());

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

        LumberjackWork.run(world, colony);

        context.runAtTick(320, () -> {
            try {
                context.expectBlock(Blocks.OAK_SAPLING, base);
            } finally {
                owned.cleanUp();
            }

            context.complete();
        });
    }

    /**
     * E planta mesmo quando o trabalho acaba junto com a árvore.
     *
     * <p>O replantio é preguiçoso: acontece quando o lenhador procura a
     * próxima árvore, e não quando o último tronco cai. Quem perdesse o
     * trabalho entre uma coisa e outra deixava o toco pelado.
     *
     * <p>Aqui a tarefa é cancelada com a árvore já no chão. O trabalho
     * morre no tick seguinte sem nunca chegar a {@code startNextTree} —
     * e a muda tem de entrar mesmo assim, que é o que
     * {@code closePlan} passou a garantir.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_replant_on_close",
            tickLimit = 400)
    public void theSaplingGoesInEvenWhenTheTaskDiesWithTheTree(TestContext context) {
        BlockPos base = new BlockPos(4, 2, 4);
        BlockPos chest = new BlockPos(2, 2, 2);
        BlockPos stand = new BlockPos(3, 2, 4);

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

        ColonyFixture owned = ColonyFixture.create()
                .owning(colony)
                .owning(villager.getUuid());

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

        LumberjackWork.run(world, colony);

        // A janela é de um tick: o último tronco cai num tick e
        // startNextTree replanta no seguinte. Um `runAtTick` fixo cai
        // depois dela — foi assim que a primeira versão deste teste
        // passou com a correção revertida, que é o E2 de novo.
        //
        // Então observa-se todo tick e cancela-se no primeiro em que a
        // árvore está no chão. Aí o trabalho morre sem nunca chegar a
        // startNextTree, que é o caso que closePlan cobre.
        boolean[] cancelled = {false};
        int[] cancelledAt = {0};

        for (int tick = 1; tick <= 320; tick++) {
            int now = tick;

            context.runAtTick(tick, () -> {
                if (cancelled[0] || logsStanding(context, base) > 0) {
                    return;
                }

                task.cancel();

                cancelled[0] = true;
                cancelledAt[0] = now;
            });
        }

        context.runAtTick(340, () -> {
            try {
                context.assertTrue(
                        cancelled[0],
                        "a árvore não chegou ao chão em 320 ticks — o teste não chegou a exercitar"
                                + " o que pretende");

                context.assertTrue(
                        logsStanding(context, base) == 0,
                        "a árvore precisava estar no chão");

                context.expectBlock(Blocks.OAK_SAPLING, base);
            } finally {
                owned.cleanUp();
            }

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

        plantTree(context, first);
        plantTree(context, second);
        context.getWorld().setTimeOfDay(Schedule.WORK_TIME);

        ServerWorld world = context.getWorld();

        Colony colony = Colony.create(
                UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(first)));

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        for (int i = 0; i < 2; i++) {
            BlockPos chest = new BlockPos(1, 2, 4 + i);

            context.setBlockState(chest, Blocks.CHEST.getDefaultState());

            VillagerEntity villager = context.spawnEntity(
                    EntityType.VILLAGER, new BlockPos(4, 2, 4));
            villager.setBreedingAge(0);

            owned.owning(villager.getUuid());

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
            List<BlockPos> working = LumberjackWork.blocksInProgress(colony.id());

            try {
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
            } finally {
                owned.cleanUp();
            }

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
     * Construção perto não prende o lenhador.
     *
     * <p>É o defeito que a regra da copa criou e que o log de 2026-08-13
     * mostrou: a vila de {@code 1109,730} passou dezesseis minutos em
     * horário de trabalho, com dois lenhadores, sem derrubar nada — e com
     * floresta ao alcance.
     *
     * <p>A causa é a busca ser determinística a partir do centro. O
     * tronco mais próximo era construção; a regra da copa devolvia plano
     * vazio; a busca recomeçava do centro no ciclo seguinte e achava o
     * mesmo tronco. Para sempre, sem uma linha de log dizendo o quê.
     *
     * <p>O teste põe a construção mais perto que a árvore, que é
     * exatamente a ordem que trava. Sem a correção, nada é derrubado.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_stuck_on_build",
            tickLimit = 400)
    public void aBuildingNearTheCenterDoesNotTrapTheSearch(TestContext context) {
        BlockPos center = new BlockPos(1, 2, 1);
        BlockPos pillar = new BlockPos(2, 2, 2);
        BlockPos tree = new BlockPos(6, 2, 6);

        // O pilar é construção: tronco sem copa. Fica entre o centro e a
        // árvore, e a busca chega nele primeiro.
        raiseLogs(context, pillar, 3);

        plantTree(context, tree);

        BlockPos chest = new BlockPos(1, 2, 3);

        context.setBlockState(chest, Blocks.CHEST.getDefaultState());
        context.getWorld().setTimeOfDay(Schedule.WORK_TIME);

        ServerWorld world = context.getWorld();

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(5, 2, 5));
        villager.setBreedingAge(0);

        Colony colony = Colony.create(
                UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center)));

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create()
                .owning(colony)
                .owning(villager.getUuid());

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

        LumberjackWork.run(world, colony);

        context.runAtTick(30, () -> {
            List<BlockPos> working = LumberjackWork.blocksInProgress(colony.id());

            try {
                context.assertTrue(
                        !working.isEmpty(),
                        "o lenhador ficou preso na construção e nunca achou a árvore");

                BlockPos absoluteTree = context.getAbsolutePos(tree);

                context.assertTrue(
                        working.get(0).getX() == absoluteTree.getX()
                                && working.get(0).getZ() == absoluteTree.getZ(),
                        "o lenhador foi para " + working.get(0).toShortString()
                                + ", que não é a árvore");

                // E a construção continua de pé, que é a outra metade.
                for (int y = 0; y < 3; y++) {
                    context.expectBlock(Blocks.OAK_LOG, pillar.up(y));
                }
            } finally {
                owned.cleanUp();
            }

            context.complete();
        });
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

    /**
     * Baú assoreado transborda para a colônia — não destrói a colheita.
     *
     * <p>O mundo que a sessão de 2026-09-04 mostrou, e que nenhum teste
     * desta classe modelava. A folha derruba vara, maçã e muda; nenhuma
     * delas é de um {@code ResourceGroup}, e nada no mod as retira de baú
     * nenhum — a muda replantada vem do mundo, não do baú. Cada uma
     * ocupa um slot para sempre, e o espaço do baú do lenhador só desce.
     *
     * <p>Ao chegar a zero o lenhador morre em definitivo. A sessão
     * mediu: cinquenta e nove ciclos seguidos de {@code filled the chest
     * — 0 logs collected, 0 more would fit}, vinte e quatro troncos
     * destruídos no meio da colheita, e a obra parada vinte ciclos
     * esperando a madeira que ele não entregava mais.
     *
     * <p>A regra do conserto é a que a retirada já segue desde
     * 2026-08-14: o baú do próprio primeiro, qualquer baú da colônia
     * depois. Transbordar não muda o assoreamento de lugar porque o
     * tronco tem consumidor em todo baú — o fabricante retira de todos —,
     * e a vara não tem em nenhum.
     *
     * <p>O assoreamento aqui é de vara, que é o que a árvore dá. Assorear
     * com diamante contaria outra história — a do item do jogador, que a
     * Regra 1 trata de propósito de outro jeito, e que
     * {@code anotherPlayersItemIsNotFreeSpace} já fixa.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_silted_chest",
            tickLimit = 400)
    public void theSiltedChestSpillsIntoTheColonyInsteadOfDestroying(TestContext context) {
        BlockPos base = new BlockPos(4, 2, 4);
        BlockPos own = new BlockPos(2, 2, 2);
        BlockPos spare = new BlockPos(2, 2, 6);
        BlockPos stand = new BlockPos(3, 2, 4);

        plantTree(context, base);
        context.setBlockState(own, Blocks.CHEST.getDefaultState());
        context.setBlockState(spare, Blocks.CHEST.getDefaultState());
        context.getWorld().setTimeOfDay(Schedule.WORK_TIME);

        ServerWorld world = context.getWorld();

        ColonyPos ownChest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(own));
        ColonyPos spareChest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(spare));

        // Vinte e sete slots de vara: o assoreamento da sessão, no ponto
        // em que ele mata o lenhador.
        ChestDepositor.deposit(world, ownChest, Items.STICK, 27 * 64);

        context.assertTrue(
                ChestDepositor.freeSpaceForGroup(world, ownChest, ResourceGroup.WOOD) == 0,
                "o baú do lenhador devia estar sem espaço de madeira para o teste valer");

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, stand);
        villager.setBreedingAge(0);

        Colony colony = Colony.create(
                UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(base)));

        VillageColonyMod.COLONIES.register(colony);

        UUID neighbour = UUID.randomUUID();

        ColonyFixture owned = ColonyFixture.create()
                .owning(colony)
                .owning(villager.getUuid())
                .owning(neighbour);

        VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id())
                .assign(ProfessionType.LUMBERJACK);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), ownChest));

        // O vizinho existe para ter baú, e o baú é o ponto: a colônia tem
        // espaço, só não no baú deste lenhador.
        VillageColonyMod.WORKERS.register(neighbour, colony.id())
                .assign(ProfessionType.MANUFACTURER);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(neighbour, spareChest));

        Task task = VillageColonyMod.TASKS.create(
                colony.id(),
                TaskType.COLLECT_WOOD,
                TaskPriority.PRODUCTION,
                ResourceType.OAK_LOG,
                64);

        task.reserveFor(villager.getUuid());

        LumberjackWork.run(world, colony);

        context.runAtTick(320, () -> {
            int spilled = ChestInventoryReader
                    .read(world, context.getAbsolutePos(spare))
                    .amountOf(ResourceType.OAK_LOG);

            int standing = logsStanding(context, base);

            try {
                context.assertTrue(
                        standing < 4,
                        "o lenhador não derrubou nada: de pé ainda são " + standing);

                context.assertTrue(
                        spilled > 0,
                        "a madeira não transbordou para o baú vizinho — foi destruída");

                context.assertTrue(
                        spilled == 4 - standing,
                        "tronco derrubado que não chegou a baú nenhum: caíram "
                                + (4 - standing) + ", guardados " + spilled);
            } finally {
                owned.cleanUp();
            }

            context.complete();
        });
    }

    /**
     * O contrato do transbordo, sem mundo ticando.
     *
     * <p>O teste de cima prova o comportamento; este prova a conta, que é
     * onde um erro de um item se esconde. Baú cheio devolve o que não
     * coube ao próximo da lista, e só o que não couber em nenhum volta
     * como sobra — que é o número que o chamador usa para decidir se
     * houve perda.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lumber_spill_contract")
    public void theColonyChestsTakeWhatTheOwnChestCannot(TestContext context) {
        BlockPos full = new BlockPos(2, 2, 2);
        BlockPos free = new BlockPos(2, 2, 4);

        context.setBlockState(full, Blocks.CHEST.getDefaultState());
        context.setBlockState(free, Blocks.CHEST.getDefaultState());

        ServerWorld world = context.getWorld();

        ColonyPos fullChest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(full));
        ColonyPos freeChest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(free));

        ChestDepositor.deposit(world, fullChest, Items.STICK, 27 * 64);

        int leftOver = ColonyChests.deposit(
                world, List.of(fullChest, freeChest), Items.OAK_LOG, 10);

        context.assertTrue(leftOver == 0, "sobrou " + leftOver + " com baú livre na lista");

        int stored = ChestInventoryReader
                .read(world, context.getAbsolutePos(free))
                .amountOf(ResourceType.OAK_LOG);

        context.assertTrue(stored == 10, "esperava 10 no baú livre, deu " + stored);

        // Sem baú livre na lista, a sobra é o total: nada some em silêncio.
        int allLost = ColonyChests.deposit(world, List.of(fullChest), Items.OAK_LOG, 7);

        context.assertTrue(allLost == 7, "esperava sobra de 7, deu " + allLost);

        context.complete();
    }
}
