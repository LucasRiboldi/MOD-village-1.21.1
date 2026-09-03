package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.Mine;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O alcance do mineiro, e a mentira que o relatório contava —
 * 2026-08-27.
 *
 * <p><b>A sessão das 22:19.</b> Os dois mineiros travaram, e o relatório
 * dizia a mesma coisa a cada trinta segundos:
 *
 * <pre>
 * digging Pedra at 728, 44, 878, 4 blocks away, 5/6 ticks, stall 2219/2400
 * </pre>
 *
 * <p>"Quatro blocos" e o alcance é quatro: parecia que ele estava no
 * lugar certo e não batia. Mas as duas frases mediam coisas diferentes —
 * o relatório usava {@code getBlockPos()}, que é inteiro, e ainda
 * truncava a raiz; o alcance usa a posição real do aldeão. **Qualquer
 * distância entre 4,0 e 4,99 aparecia como "4 blocks away" e estava
 * fora de alcance.**
 *
 * <p>Instrumento que mente é pior que instrumento nenhum: ele mandou
 * procurar o defeito onde ele não estava. A conta passou a ser uma só, e
 * é esta.
 */
class MinerReachTest {

    private static final BlockPos STONE = new BlockPos(728, 44, 878);

    /** Encostado na pedra: dentro de alcance, sem discussão. */
    @Test
    void besideTheStoneIsWithinReach() {
        assertTrue(MinerReach.isWithinReach(727.5, 44.0, 878.5, STONE));
    }

    /**
     * Quatro e meio está fora, e o relatório antigo dizia "quatro".
     *
     * <p>É o caso exato da sessão: longe o bastante para o alcance
     * recusar, perto o bastante para o log dizer que ele chegou.
     */
    @Test
    void fourAndAHalfIsOutOfReachEvenThoughItRoundsToFour() {
        double x = STONE.getX() + 0.5 - 4.5;

        assertFalse(MinerReach.isWithinReach(x, STONE.getY() + 0.5, STONE.getZ() + 0.5, STONE));

        assertEquals(
                4,
                (int) MinerReach.distanceTo(x, STONE.getY() + 0.5, STONE.getZ() + 0.5, STONE),
                "a distância truncada é a que o relatório mostrava");
    }

    /** A borda pertence a quem alcança: exatamente quatro ainda vale. */
    @Test
    void exactlyFourStillReaches() {
        double x = STONE.getX() + 0.5 - MinerReach.REACH;

        assertTrue(MinerReach.isWithinReach(x, STONE.getY() + 0.5, STONE.getZ() + 0.5, STONE));
    }

    /**
     * A altura conta, e é o E30.
     *
     * <p>Quatro blocos no plano mais quatro de altura são cinco e meia de
     * distância real. Enquanto o alcance ignorava o {@code dy}, o mineiro
     * batia na pedra de cima do buraco e ela caía.
     */
    @Test
    void heightCountsTowardTheDistance() {
        assertFalse(MinerReach.isWithinReach(
                STONE.getX() + 0.5 - 4, STONE.getY() + 0.5 + 4, STONE.getZ() + 0.5, STONE));
    }

    /**
     * A distância que o relatório mostra é a que o alcance usa.
     *
     * <p>A afirmação inteira deste ciclo: uma conta só. Enquanto forem
     * duas, o log continua podendo dizer "chegou" sobre quem não chegou.
     */
    @Test
    void theReportedDistanceIsTheOneTheReachUses() {
        double x = 724.2;
        double y = 44.0;
        double z = 878.7;

        double reported = MinerReach.distanceTo(x, y, z, STONE);

        assertEquals(
                reported <= MinerReach.REACH,
                MinerReach.isWithinReach(x, y, z, STONE),
                "o número mostrado e a decisão de alcance discordaram");
    }

    /**
     * A folga do caminhante cabe dentro do braço — 2026-08-29.
     *
     * <p><b>A sessão de 2026-08-28, 23:19, e o mineiro que estava
     * dentro da mina.</b> Pela primeira vez em nove sessões ele desceu:
     * y=44 é a galeria. E ficou lá, parado, seiscentos tiques:
     *
     * <pre>
     * digging Pedra at 760, 44, 878, 4,2 blocks away
     *   (out of reach, he is at 756, 44, 878, walking to 758, 44, 878),
     *   0/0 ticks, stall 2140/2400
     * </pre>
     *
     * <p><b>Ele estava a exatamente dois blocos do destino</b>, e não é
     * coincidência: dois é o {@code COMPLETION_RANGE} do
     * {@code GoToWorkTargetTask}. A navegação se deu por <b>chegada</b>
     * e parou; o mod continuou dizendo que estava fora de alcance; e ele
     * ficou moendo os últimos dois blocos até o guarda devolver a
     * tarefa. É o "rodando na escada e não desce" que o autor viu.
     *
     * <p><b>Duas contas certas que não compunham.</b> O
     * {@code approachTo} escolhe um lugar <b>dentro</b> do braço — 758
     * está a 2,0 da pedra, e o braço é 4. O caminhante para até dois
     * blocos <b>antes</b> desse lugar. Somadas, as duas dão 4,2: fora do
     * braço, para sempre, sem que nenhuma das duas esteja errada
     * sozinha.
     *
     * <p>A folga de dois foi escrita para o lenhador, e ali ela é certa:
     * o destino dele <b>é</b> a árvore, e parar dois antes é parar
     * dentro do braço. Para o mineiro o destino já é o lugar exato de
     * ficar de pé, e parar antes dele é não chegar.
     *
     * <p>Este teste usa os números da sessão e mede o <b>pior</b> caso:
     * a folga inteira, na direção contrária à pedra.
     */
    @Test
    void arrivingAtTheEdgeOfTheWalkStillReachesTheStone() {
        BlockPos stone = new BlockPos(760, 44, 878);
        BlockPos approach = new BlockPos(758, 44, 878);

        double worstX = approach.getX() + 0.5 - MinerReach.ARRIVAL;

        assertTrue(
                MinerReach.isWithinReach(worstX, approach.getY(), approach.getZ() + 0.5, stone),
                "parando a " + MinerReach.ARRIVAL + " bloco(s) do lugar escolhido ele fica a "
                        + MinerReach.distanceTo(
                                worstX, approach.getY(), approach.getZ() + 0.5, stone)
                        + " da pedra, e o braço é " + MinerReach.REACH);
    }

    /** A folga é menor que o braço, senão a soma nunca fecha. */
    @Test
    void theWalkersSlackIsSmallerThanTheArm() {
        assertTrue(
                MinerReach.ARRIVAL < MinerReach.REACH,
                "folga de " + MinerReach.ARRIVAL + " num braço de " + MinerReach.REACH);
    }

    /**
     * A folga de <b>um</b> ainda era demais — 2026-08-29, 04:26.
     *
     * <p><b>Segunda tentativa no mesmo número, e a sessão que a cobrou.</b>
     * Com a folga em dois ele parava dois blocos antes; baixada para um,
     * ele passou a parar um bloco antes — e continuou fora do braço:
     *
     * <pre>
     * he is at 757, 44, 878, 4,4 blocks away;
     * it was walking to 758, 44, 878;
     * the place to stand is 758, 44, 878;
     * </pre>
     *
     * <p>O lugar escolhido era bom — a frase não traz ressalva nenhuma —
     * e de cima dele ele alcançaria a 3,35. Um bloco de folga jogou a
     * conta para 4,27, e o braço é 4.
     *
     * <p><b>A lição é que folga nenhuma serve.</b> Enquanto ela existir,
     * a composição depende de sorte: o {@code approachTo} escolhe um
     * lugar <b>dentro</b> do braço, e qualquer sobra empurra para fora.
     * Com folga zero a garantia é por construção — ele fica onde foi
     * escolhido, e o lugar escolhido alcança.
     *
     * <p>Este caso é mais duro que o de 23:19 porque a pedra está uma
     * camada <b>acima</b>: o {@code dy} de 1,5 come folga que o plano não
     * come.
     */
    @Test
    void oneBlockOfSlackWasStillTooMuch() {
        BlockPos stone = new BlockPos(761, 45, 878);
        BlockPos approach = new BlockPos(758, 44, 878);

        double worstX = approach.getX() + 0.5 - MinerReach.ARRIVAL;

        assertTrue(
                MinerReach.isWithinReach(worstX, approach.getY(), approach.getZ() + 0.5, stone),
                "parando a " + MinerReach.ARRIVAL + " bloco(s) do lugar escolhido ele fica a "
                        + MinerReach.distanceTo(
                                worstX, approach.getY(), approach.getZ() + 0.5, stone)
                        + " da pedra, e o braço é " + MinerReach.REACH);
    }

    /**
     * A ordem de aproximação é a varredura antiga, sem a varredura —
     * 2026-09-03.
     *
     * <p>O {@code MinerWork.approachTo} varria o cubo inteiro para ficar
     * com o vizinho mais perto, e passou a devolver o primeiro que
     * servir de uma lista pronta. A troca só é segura se a lista
     * <b>for</b> a ordem em que o laço antigo teria aceitado cada
     * posição — e é isso que este teste afirma, refazendo o laço antigo
     * aqui.
     *
     * <p>Sem ele, um deslize na conta de distância ou na estabilidade da
     * ordenação escolheria um lugar de pé <i>parecido</i>. Parecido não
     * serve: com {@link MinerReach#ARRIVAL} em zero, o lugar escolhido
     * alcança a pedra <b>por construção</b>, e um bloco a mais joga a
     * conta para fora do braço — é a lição de 2026-08-29, 04:26.
     */
    @Test
    void theApproachOrderIsTheOldCubeScanInOrder() {
        List<Vec3i> asTheOldLoopWouldHaveTakenThem = new ArrayList<>();

        for (int dx = -MinerReach.REACH; dx <= MinerReach.REACH; dx++) {
            for (int dy = -MinerReach.REACH; dy <= MinerReach.REACH; dy++) {
                for (int dz = -MinerReach.REACH; dz <= MinerReach.REACH; dz++) {

                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }

                    BlockPos at = STONE.add(dx, dy, dz);

                    double distance = MinerReach.distanceTo(
                            at.getX() + 0.5, at.getY(), at.getZ() + 0.5, STONE);

                    if (distance > MinerReach.REACH) {
                        continue;
                    }

                    asTheOldLoopWouldHaveTakenThem.add(new Vec3i(dx, dy, dz));
                }
            }
        }

        asTheOldLoopWouldHaveTakenThem.sort(
                java.util.Comparator.comparingDouble(offset -> MinerReach.distanceTo(
                        STONE.getX() + offset.getX() + 0.5,
                        STONE.getY() + offset.getY(),
                        STONE.getZ() + offset.getZ() + 0.5,
                        STONE)));

        assertEquals(asTheOldLoopWouldHaveTakenThem, MinerReach.APPROACH_OFFSETS);
    }

    /**
     * A lista vem ordenada, e é isso que torna a saída antecipada
     * barata.
     *
     * <p>Se ela deixar de estar ordenada, o {@code approachTo} continua
     * respondendo — só que o <b>primeiro que serve</b> deixa de ser o
     * <b>mais perto</b>, e a garantia do {@link MinerReach#ARRIVAL} zero
     * cai em silêncio.
     */
    @Test
    void theApproachOrderRunsFromTheNearestOutwards() {
        List<Vec3i> offsets = MinerReach.APPROACH_OFFSETS;

        for (int i = 1; i < offsets.size(); i++) {
            double before = MinerReach.offsetDistance(
                    offsets.get(i - 1).getX(),
                    offsets.get(i - 1).getY(),
                    offsets.get(i - 1).getZ());

            double now = MinerReach.offsetDistance(
                    offsets.get(i).getX(), offsets.get(i).getY(), offsets.get(i).getZ());

            assertTrue(
                    before <= now,
                    "a posição " + i + " está a " + now + " e a anterior a " + before);
        }
    }

    /**
     * Nenhuma posição da lista está fora do braço, e a própria pedra não
     * está nela.
     *
     * <p>A pedra fora da lista é o que permite ao {@code approachTo}
     * usar <i>"devolveu a própria pedra"</i> como resposta de <b>não há
     * lugar nenhum</b> — que é a guarda de emparedada inteira, do
     * {@code nextCut} ao veio.
     */
    @Test
    void everyApproachIsWithinTheArmAndNoneIsTheStoneItself() {
        for (Vec3i offset : MinerReach.APPROACH_OFFSETS) {
            assertFalse(
                    offset.getX() == 0 && offset.getY() == 0 && offset.getZ() == 0,
                    "a própria pedra não é lugar de ficar de pé");

            BlockPos at = STONE.add(offset);

            assertTrue(
                    MinerReach.isWithinReach(
                            at.getX() + 0.5, at.getY(), at.getZ() + 0.5, STONE),
                    at.toShortString() + " está fora do braço de " + MinerReach.REACH);
        }
    }
}
