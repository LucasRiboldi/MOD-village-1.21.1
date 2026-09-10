package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.type.Side;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkHours;
import com.villagecolony.fabric.work.MineDigging;
import com.villagecolony.fabric.work.MineLease;
import com.villagecolony.fabric.work.MineMarks;
import com.villagecolony.fabric.work.MinerReport;
import com.villagecolony.fabric.work.MinerWork;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.Schedule;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * O prazo de aproximação do mineiro, em jogo — E44, 2026-09-10.
 *
 * <p><b>Em arquivo próprio de propósito.</b> O {@code MinerGameTest} está
 * com 4.845 linhas e o portão acusa isso a cada entrega; somar o E44 lá
 * seria piorar o número que já é queixa. O lote é outro, o cenário é
 * outro, e nada aqui reaproveita as ajudas de lá.
 *
 * <p><b>O que a sessão das 08:33 mediu, e que nenhum guarda pegava:</b>
 *
 * <pre>
 * 08:33     4c4171a4 mira 2442,44,-1424 — out of reach, 9,9 blocos
 * 08:34:13  desiste: "walked for 2400 ticks of work time without arriving",
 *           com "19 blocks below it and unable to climb"
 * 08:34:43  d5f6de43 assume o ramal e recebe A MESMA pedra
 * 08:36:17  desiste com a mesma frase
 * </pre>
 *
 * <p>Dois mineiros, dois minutos de expediente cada, zero pedra. O
 * {@code WorkStall} não falou porque ele <b>andava</b>; o guarda de
 * travamento falou tarde porque ele só sabe contar até 2.400.
 */
public class MineLeaseGameTest implements FabricGameTest {

    /** O baú da colônia, e a âncora de tudo o mais. */
    private static final BlockPos CHEST = new BlockPos(2, 2, 2);

    /** A boca da mina, fundo o bastante para a galeria caber embaixo. */
    private static final BlockPos MOUTH = new BlockPos(2, 1, 5);

    /**
     * O poleiro do mineiro, seis blocos acima da pedra.
     *
     * <p>É o mesmo do teste do mineiro congelado, e pelo mesmo motivo: de
     * lá a pedra está a 5,5 blocos, fora do braço de 4, e nenhuma forma
     * de arena precisa ser inventada para garantir isso.
     */
    private static final BlockPos PERCH = new BlockPos(3, 7, 5);

    /** E o passo dele: um bloco ao norte, e de volta. */
    private static final BlockPos STEP = new BlockPos(3, 7, 4);

    /**
     * A distância da mina à vila que a arena precisa aceitar.
     *
     * <p>Sem isto o mineiro não vai à mina — ele cai na varredura de
     * pedra <b>exposta</b>, que tem 48 blocos de raio, sai da arena e
     * <b>raspa o cenário do teste vizinho</b>. Foi o que a primeira
     * versão deste teste fez em 2026-09-10: o alvo veio de outra arena, e
     * a afirmação final falhou apontando uma posição que não existe aqui.
     */
    private static final int NEARBY = 2;

    /**
     * O prazo encurtado para esta bateria — ver MineLease.shortenTo.
     *
     * <p>Quarenta tiques em vez de quatrocentos. Continua muito abaixo
     * dos outros dois guardas (300 e 2.400), que é o que faz o teste
     * medir o prazo e não outro; e tira o teste de cima dos vizinhos.
     */
    private static final int LEASE = 40;

    /**
     * A folga entre o prazo vencer e a afirmação.
     *
     * <p>O guarda roda no tique do servidor, a devolução da tarefa passa
     * pelo {@code giveUp} e a bateria observa de fora: vinte tiques
     * cobrem a diferença sem chegar perto do guarda seguinte, que é
     * 2.400.
     */
    private static final int SLACK = 20;

    /**
     * O mineiro que anda sem se aproximar larga a pedra dentro do prazo.
     *
     * <p><b>E o teste tem de provar QUAL guarda falou</b>, senão ele mede
     * outra coisa — que é justamente a queixa que derrubou a primeira
     * tentativa do E42 em 09-09. São três guardas, e dois deles também
     * devolvem a tarefa:
     *
     * <ul>
     *   <li>o de imobilidade fala aos 300 tiques parado no mesmo bloco;
     *   <li>o de travamento, aos 2.400 andando;
     *   <li>o prazo, aos 400 sem encurtar a distância.
     * </ul>
     *
     * <p>Por isso o mineiro <b>anda</b> aqui: um passo por tique, ida e
     * volta entre dois blocos. O contador de imobilidade zera todo tique
     * e não tem como falar; o de travamento não chega a 2.400 dentro do
     * limite deste teste; e o que sobra é o prazo. As afirmações conferem
     * os três contadores no instante da devolução, e não só o relógio.
     *
     * <p><b>Por que a bateria move o aldeão em vez de deixar a navegação
     * mover.</b> O caso é <i>anda e não chega</i>, e uma arena não
     * reproduz isso por geometria de forma confiável: alvo inalcançável
     * costuma virar aldeão <b>parado</b>, e aldeão parado é o outro
     * guarda — o teste passaria medindo o guarda errado. O passo forçado
     * é a definição do caso, e não um atalho: em jogo, quem o dá é a
     * perna do {@code MinerReach.legTowards} sendo reposta a cada tique.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_lease",
            tickLimit = LEASE + 4 * SLACK)
    public void theMinerWhoWalksWithoutClosingInLosesTheStone(TestContext context) {
        // O relógio é do mundo inteiro e a bateria o faz andar: sem
        // fixá-lo, onde este teste cai no dia depende de quantos tiques a
        // bateria gastou antes dele — e fora do expediente nenhum dos
        // três guardas conta.
        context.getWorld().setTimeOfDay(Schedule.WORK_TIME);

        ServerWorld world = context.getWorld();

        floor(context);

        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        // A pedra que ele vai mirar, e o chão dos dois blocos por onde
        // ele anda — sem eles o aldeão cai enquanto a bateria o repõe.
        context.setBlockState(MOUTH.east(), Blocks.STONE.getDefaultState());
        context.setBlockState(PERCH.down(), Blocks.DIRT.getDefaultState());
        context.setBlockState(STEP.down(), Blocks.DIRT.getDefaultState());

        MineDigging.shortenMineDistanceTo(NEARBY);

        MineLease.shortenTo(LEASE);

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        Colony colony = Colony.create(UUID.randomUUID(), chest);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, PERCH);
        villager.setBreedingAge(0);

        UUID minerId = villager.getUuid();

        Worker worker = VillageColonyMod.WORKERS.register(minerId, colony.id());
        worker.assign(ProfessionType.MINER);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(minerId, chest));

        owned.owning(minerId);

        Task task = VillageColonyMod.TASKS.create(
                colony.id(),
                TaskType.COLLECT_STONE,
                TaskPriority.PRODUCTION,
                ResourceType.COBBLESTONE,
                16);

        task.reserveFor(minerId);

        ColonyPos mouth = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(MOUTH));

        VillageColonyMod.MINES.restore(
                Mine.restore(colony.id(), MineShaft.from(mouth, Side.EAST), 0));

        MinerWork.run(world, colony);

        // O que se afirma é o EVENTO, e não o estado num tique escolhido
        // — KF-001, 2026-09-09. Estado de tarefa é global e a fronteira do
        // ciclo da colônia pode reatribuí-la dentro da janela; o instante
        // em que ela voltou para a fila, não.
        int[] passes = { 0 };
        int[] releasedAt = { -1 };
        int[] stillnessAtRelease = { -1 };
        int[] stallAtRelease = { -1 };
        int[] adriftAtRelease = { -1 };

        BlockPos[] stone = { null };

        BlockPos absolutePerch = context.getAbsolutePos(PERCH);
        BlockPos absoluteStep = context.getAbsolutePos(STEP);

        context.runAtEveryTick(() -> {
            passes[0]++;

            // <b>Um passo por tique.</b> Os dois blocos ficam a 5,5 e
            // 5,6 da pedra: os dois fora do braço de 4, e a diferença
            // entre eles menor que a margem do prazo. Andar assim é
            // exatamente "não se aproximou".
            BlockPos step = passes[0] % 2 == 0 ? absolutePerch : absoluteStep;

            villager.refreshPositionAndAngles(
                    step.getX() + 0.5,
                    step.getY(),
                    step.getZ() + 0.5,
                    villager.getYaw(),
                    villager.getPitch());

            if (stone[0] == null) {
                MinerWork.targetOf(minerId).ifPresent(at -> stone[0] = at);
            }

            if (releasedAt[0] >= 0) {
                return;
            }

            if (task.state() != TaskState.RESERVED && task.state() != TaskState.EXECUTING) {
                releasedAt[0] = passes[0];
                stillnessAtRelease[0] = MinerWork.stillnessOf(minerId);
                stallAtRelease[0] = MinerWork.stallOf(minerId);
                adriftAtRelease[0] = MinerWork.adriftOf(minerId);
            }
        });

        context.runAtTick(LEASE + 3 * SLACK, () -> {
            // A mensagem de uma asserção de gametest não aparece no log
            // da bateria — o console imprime só o nome do teste que
            // falhou. Ver KF-001: numa falha rara, isto é a diferença
            // entre diagnosticar e adivinhar.
            if (releasedAt[0] < 0) {
                VillageColonyMod.LOGGER.warn(
                        "E44 — o mineiro que anda sem se aproximar não largou a pedra."
                                + " task={}, expediente={}, relatório={}",
                        task.state(),
                        WorkHours.isWorkTime(world, villager),
                        MinerReport.report(world, colony).orElse("(sem relatório)"));
            }

            try {
                context.assertTrue(
                        WorkHours.isWorkTime(world, villager),
                        "a arena não está em horário de expediente, e fora dele nenhum dos"
                                + " três guardas conta — este teste não mede o que promete");

                context.assertTrue(
                        stone[0] != null,
                        "o mineiro nunca chegou a mirar uma pedra, e sem alvo não há prazo"
                                + " para vencer — o cenário não montou");

                // <b>E a pedra tem de ser DESTA arena.</b> A varredura de
                // pedra exposta tem 48 blocos de raio e alcança o cenário
                // do vizinho: sem a distância de mina encurtada, a
                // primeira versão deste teste mirou uma pedra de outro
                // teste e afirmou coisas verdadeiras sobre ela. Esta
                // linha é o que teria dito isso na hora.
                context.assertTrue(
                        stone[0].isWithinDistance(context.getAbsolutePos(CHEST), 16),
                        "o alvo saiu em " + stone[0].toShortString() + ", longe desta arena:"
                                + " o mineiro foi buscar pedra no cenário do teste vizinho,"
                                + " e o que este teste mediria não seria o daqui");

                context.assertTrue(
                        releasedAt[0] >= 0,
                        "o mineiro andou " + passes[0] + " tiques sem se aproximar e a tarefa"
                                + " nunca voltou para a fila. Ela só voltaria no tique "
                                + MinerWork.STALL_LIMIT + ", que é o preço que a sessão das"
                                + " 08:33 pagou duas vezes. O relatório: "
                                + MinerReport.report(world, colony).orElse("(sem relatório)"));

                context.assertTrue(
                        releasedAt[0] <= LEASE + SLACK,
                        "a tarefa voltou no tique " + releasedAt[0] + ", e o prazo é de "
                                + LEASE + " — quem falou não foi o prazo");

                context.assertTrue(
                        stillnessAtRelease[0] < MinerWork.STILL_LIMIT,
                        "o contador de imobilidade marcava " + stillnessAtRelease[0]
                                + " de " + MinerWork.STILL_LIMIT + " quando a tarefa voltou:"
                                + " quem devolveu foi o guarda de imobilidade, e este teste"
                                + " promete medir o prazo. O aldeão devia estar ANDANDO");

                context.assertTrue(
                        stallAtRelease[0] < MinerWork.STALL_LIMIT,
                        "o contador de travamento marcava " + stallAtRelease[0]
                                + " de " + MinerWork.STALL_LIMIT + ": quem devolveu foi ele,"
                                + " e ele é justamente o que custa caro demais");

                context.assertTrue(
                        adriftAtRelease[0] == 0,
                        "o prazo devia ter sido zerado ao largar a pedra, e marcava "
                                + adriftAtRelease[0]);

                // <b>E a pedra fica de fora para a colônia inteira.</b> É
                // o que impede o revezamento: o segundo mineiro assume o
                // ramal trinta segundos depois e não pode herdar a
                // armadilha do primeiro.
                context.assertTrue(
                        MineMarks.isOutOfReach(world, stone[0]),
                        "a pedra de " + stone[0].toShortString() + " não ficou marcada, e sem"
                                + " marca o próximo mineiro recebe a mesma");

                // <b>E a pergunta do cursor mora no teste vizinho</b>, de
                // propósito: {@code theGalleryStepsPastTheStoneNobodyCouldReach}
                // monta rocha maciça e uma galeria de verdade, que é o
                // que ela exige. Repeti-la aqui, numa arena de um bloco
                // de pedra, foi o que a primeira versão deste teste fez
                // — e ela falhou por não ter galeria para andar, não por
                // haver defeito. Um teste que precisa de outro cenário é
                // outro teste.
            } finally {
                // <b>Limpeza pontual, e não clearAll</b> — 2026-09-10, e
                // custou uma rodada. Os lotes da bateria rodam ao mesmo
                // tempo: {@code MineMarks.clearAll()} apaga o mapa
                // INTEIRO, inclusive as marcas que o teste vizinho
                // acabou de pôr. Foi assim que este teste derrubou
                // {@code theGalleryStepsPastTheStoneNobodyCouldReach} e
                // {@code theSkippedStoneStaysBehindOnceTheGalleryMovedPast},
                // que passavam. A marca desta pedra é a única que é
                // minha, e {@code dug} tira exatamente ela.
                if (stone[0] != null) {
                    MineMarks.dug(stone[0]);
                }

                MineLease.restoreLimit();

                MineDigging.restoreMineDistance();

                owned.cleanUp();
            }

            context.complete();
        });
    }

    /** Chão de terra sob a arena, para o aldeão não cair. */
    private static void floor(TestContext context) {
        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 7; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.DIRT.getDefaultState());
            }
        }
    }
}
