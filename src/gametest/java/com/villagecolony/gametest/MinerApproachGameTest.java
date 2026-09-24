package com.villagecolony.gametest;

import com.villagecolony.fabric.work.MinerApproach;
import com.villagecolony.fabric.work.MinerWork;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

/**
 * Onde o mineiro fica de pé depende de onde ELE está — sessão de jogo de
 * 2026-09-19.
 *
 * <p><b>O que a sessão mediu.</b> Um mineiro passou a sessão inteira em
 * {@code -841, 44, 1374} — vinte e uma das vinte e cinco leituras na
 * mesma posição — com toda aproximação escolhida em {@code y=46}:
 *
 * <pre>
 * gave up the stone at -836, 46, 1374 — it has not moved a block in 300
 *   ticks of work time. the miner is at -841, 44, 1374, 2 blocks below it
 *   and unable to climb; it was walking to -838, 46, 1374
 * </pre>
 *
 * <p>Dezoito das trinta e duas desistências dizem <i>"2 blocks below it
 * and unable to climb"</i>, e a sessão terminou com <b>103 pedras
 * quebradas e zero entregues</b> num pedido de 66.
 *
 * <p><b>A conta do E40 estava certa, e mesmo assim o laço fechava.</b> O
 * {@code approachTo} de três mãos filtra por {@code CLIMB}, mas responde
 * para a posição em que o aldeão estava <b>na hora de escolher o
 * alvo</b>. Quem cai num buraco depois disso fica com um destino dois
 * acima da cabeça — e aldeão sobe um. A navegação não cumpre, o guarda de
 * imobilidade devolve a tarefa, o ciclo reabre a mesma pedra, e a
 * pergunta é feita de novo de onde ele está: no buraco.
 *
 * <p>Este teste fixa a <b>propriedade</b> que faltava: a resposta muda
 * quando quem pergunta muda de altura. É de cenário mínimo de propósito —
 * a bateria já tem um teste instável, e o que aqui se afirma é geometria,
 * não corrida de relógio.
 */
public class MinerApproachGameTest implements FabricGameTest {

    /** A pedra a ser batida, no alto de um degrau de dois. */
    private static final BlockPos STONE = new BlockPos(3, 4, 3);

    /** O piso alto, de onde a pedra está ao alcance da mão. */
    private static final BlockPos LEDGE = new BlockPos(3, 3, 2);

    /** E o buraco ao lado, dois abaixo do piso alto. */
    private static final BlockPos PIT = new BlockPos(1, 1, 3);

    /** Uma boca alta devolvida pela perna quando o aldeão caiu abaixo dela. */
    private static final BlockPos HIGH_LEG = new BlockPos(5, 4, 3);

    /** O patamar baixo de onde ele precisa retomar a subida. */
    private static final BlockPos LOW_WORKER = new BlockPos(3, 1, 3);

    /**
     * De baixo do buraco, a aproximação escolhida tem de ser alcançável.
     *
     * <p>É a asserção que a sessão de 09-19 pagou: com a resposta presa à
     * posição antiga, o mineiro recebia {@code y=46} estando em
     * {@code y=44} e não saía mais do lugar.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_approach")
    public void theApproachIsClimbableFromWhereHeActuallyStands(TestContext context) {
        ServerWorld world = context.getWorld();

        floor(context);

        // O degrau alto com a pedra em cima, e o poço ao lado dele.
        context.setBlockState(LEDGE.down(), Blocks.STONE.getDefaultState());
        context.setBlockState(LEDGE, Blocks.AIR.getDefaultState());
        context.setBlockState(LEDGE.up(), Blocks.AIR.getDefaultState());
        context.setBlockState(STONE, Blocks.STONE.getDefaultState());

        context.setBlockState(PIT, Blocks.AIR.getDefaultState());
        context.setBlockState(PIT.up(), Blocks.AIR.getDefaultState());

        BlockPos stone = context.getAbsolutePos(STONE);
        BlockPos ledge = context.getAbsolutePos(LEDGE);
        BlockPos pit = context.getAbsolutePos(PIT);

        BlockPos fromLedge = MinerApproach.approachTo(world, stone, ledge);
        BlockPos fromPit = MinerApproach.approachTo(world, stone, pit);

        // <b>A propriedade, e não uma coordenada.</b> Fixar a posição
        // exata amarraria o teste à ordem do APPROACH_OFFSETS, que é
        // detalhe de busca; o que a sessão provou é que a resposta
        // precisa ser alcançável de quem pergunta.
        context.assertTrue(
                fromLedge.getY() - ledge.getY() <= MinerWork.CLIMB,
                "do piso alto a aproximação saiu em y=" + fromLedge.getY()
                        + ", e ele está em y=" + ledge.getY()
                        + " — nem do lugar bom a conta fecha");

        context.assertTrue(
                fromPit.getY() - pit.getY() <= MinerWork.CLIMB,
                "do buraco a aproximação saiu em y=" + fromPit.getY()
                        + ", e ele está em y=" + pit.getY() + ": "
                        + (fromPit.getY() - pit.getY()) + " acima da cabeça, e aldeão sobe "
                        + MinerWork.CLIMB + ". É o laço de 09-19 — 103 pedras quebradas,"
                        + " zero entregues");

        context.complete();
    }

    /**
     * A perna até a boca também precisa respeitar o degrau, não só o
     * {@code approach} da pedra.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_approach")
    public void theMouthLegStopsAtTheNextClimbableLanding(TestContext context) {
        floor(context);

        BlockPos highLeg = context.getAbsolutePos(HIGH_LEG);
        BlockPos worker = context.getAbsolutePos(LOW_WORKER);

        context.setBlockState(highLeg, Blocks.STONE.getDefaultState());

        BlockPos landing = MinerApproach.climbableWalkTarget(
                context.getWorld(), worker, highLeg);

        context.assertTrue(
                landing.getY() - worker.getY() <= MinerWork.CLIMB,
                "a perna mandou o aldeão de " + worker + " para " + landing
                        + ", " + (landing.getY() - worker.getY())
                        + " blocos acima; a boca precisa ser vencida por patamares");
        context.assertTrue(
                com.villagecolony.fabric.work.BuilderApproach.standable(
                        context.getWorld(), landing),
                "o patamar devolvido não é pisável: " + landing);

        context.complete();
    }

    private static void floor(TestContext context) {
        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 7; z++) {
                context.setBlockState(new BlockPos(x, 0, z), Blocks.DIRT.getDefaultState());
            }
        }
    }
}
