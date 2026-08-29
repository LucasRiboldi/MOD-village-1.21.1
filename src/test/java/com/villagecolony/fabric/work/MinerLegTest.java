package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Por onde o mineiro entra na mina, e como ele desce — 2026-08-28 e 29.
 *
 * <p><b>A sessão da meia-noite mostrou onde ele estava</b>, e foi a
 * primeira vez que se soube:
 *
 * <pre>
 * the miner is at 734, 66, 878, 20,5 blocks away;
 * the stone at 735, 45, 878 is Pedra
 * </pre>
 *
 * <p><b>Y 66 é a superfície.</b> Ele estava vinte e um blocos em linha
 * reta <b>acima</b> da galeria, mirando uma pedra no fundo da mina. A
 * navegação do jogo recebe um destino a vinte blocos atravessando rocha
 * maciça, devolve caminho parcial, e ele estaciona no ponto mais próximo
 * que consegue — bem ali em cima. É o sintoma que o MineColonies
 * registrou na
 * <a href="https://github.com/ldtteam/minecolonies/issues/4297">issue
 * 4297</a>, e o remendo do jogador é o mesmo que o autor fez: cavar até
 * lá.
 *
 * <p><b>A primeira perna resolveu a entrada e criou o E35.</b> Ela tinha
 * duas pontas e nada no meio: longe, o destino era a boca; perto da
 * boca, o destino virava a pedra do fundo. A sessão de 2026-08-28 pegou
 * o segundo mineiro <b>oscilando na fronteira</b>:
 *
 * <pre>
 * 740, 65, 895  ->  8,77 da boca   FORA da perna  -> mandado à boca
 * 739, 65, 896  ->  7,55 da boca   DENTRO         -> mandado à pedra
 * 741, 63, 898  ->  9,00 da boca   FORA           -> mandado à boca
 * </pre>
 *
 * <p>Ele andava para a boca, cruzava os oito blocos, recebia um destino
 * que a navegação não cumpre, derivava, saía dos oito, e recomeçava.
 * Para sempre. <b>A descida tem vinte blocos e a perna tem oito: são
 * três passos, e o sistema só sabia dar dois.</b>
 *
 * <p>Agora quem dá o passo é a <b>ordem de cavar</b>: ela é um corredor
 * contínuo a partir da boca, e o passo seguinte é o ponto mais avançado
 * dela que ainda caiba numa perna.
 */
class MinerLegTest {

    private static final ColonyPos MOUTH = new ColonyPos(732, 63, 898);

    private static final BlockPos MOUTH_BLOCK = new BlockPos(732, 63, 898);

    private static final BlockPos DEEP = new BlockPos(735, 45, 878);

    /** A mina desta colônia, com a escada já aberta até certa posição. */
    private static Optional<Mine> mine(int cut) {
        return Optional.of(
                Mine.restore(UUID.randomUUID(), MineShaft.from(MOUTH, Side.NORTH), cut));
    }

    /** Longe da pedra e longe da boca: entra pela boca. */
    @Test
    void fromTheSurfaceHeAimsForTheMouth() {
        assertEquals(
                MOUTH_BLOCK,
                MinerReach.legTowards(new BlockPos(734, 66, 878), DEEP, mine(30)));
    }

    /**
     * <b>O E35.</b> Já na boca, o passo é escada abaixo — e não a pedra.
     *
     * <p>Este teste afirmava o contrário até 2026-08-29, e o que ele
     * travava era o defeito: <i>"já na boca, ele mira a pedra"</i>. Mirar
     * a pedra dali é mirar vinte blocos abaixo, do outro lado da rocha,
     * e a navegação não cumpre — ele derivava, saía do alcance da perna,
     * e o destino voltava a ser a boca.
     */
    @Test
    void atTheMouthTheLegIsAStepDownTheShaft() {
        BlockPos leg = MinerReach.legTowards(new BlockPos(731, 63, 898), DEEP, mine(30));

        assertNotEquals(
                DEEP, leg,
                "à boca ele continua sendo mandado à pedra do fundo, que é o E35");

        assertTrue(
                leg.getY() < MOUTH_BLOCK.getY(),
                "o passo não desce: " + leg.toShortString());

        assertTrue(
                Math.sqrt(new BlockPos(731, 63, 898).getSquaredDistance(leg)) <= MinerReach.LEG,
                "o passo saiu fora do alcance de uma perna: " + leg.toShortString());
    }

    /**
     * E de dentro da escada ele continua descendo, em vez de voltar.
     *
     * <p>É a metade que impede a troca de um travamento por outro: um
     * passo que devolvesse a boca desfaria a descida a cada tique.
     */
    @Test
    void fromInsideTheShaftTheLegKeepsGoingDown() {
        BlockPos onTheStairs = new BlockPos(732, 58, 893);

        BlockPos leg = MinerReach.legTowards(onTheStairs, DEEP, mine(60));

        assertTrue(
                leg.getY() < onTheStairs.getY(),
                "de dentro da escada ele foi mandado para cima: " + leg.toShortString());
    }

    /**
     * Perto da pedra, nem a boca nem a ordem interessam.
     *
     * <p>Dentro da galeria ele está a metros do alvo: o passo é o alvo.
     */
    @Test
    void insideTheGalleryHeKeepsAimingForTheStone() {
        assertEquals(
                DEEP,
                MinerReach.legTowards(new BlockPos(730, 45, 878), DEEP, mine(200)));
    }

    /**
     * Sem mina, não há perna: a pedra é o destino.
     *
     * <p>É a pedra de superfície, que o mineiro raspa quando a boca não
     * pôde nascer. Ali não há descida nenhuma a fazer.
     */
    @Test
    void withoutAMineTheStoneIsTheOnlyLeg() {
        assertEquals(
                DEEP,
                MinerReach.legTowards(new BlockPos(734, 66, 878), DEEP, Optional.empty()));
    }

    /**
     * Mina recém-aberta, sem nada cavado, ainda manda para a boca.
     *
     * <p>Não há ordem por onde andar, e a boca é a única resposta que
     * não inventa caminho.
     */
    @Test
    void anUntouchedMineStillSendsHimToTheMouth() {
        assertEquals(
                MOUTH_BLOCK,
                MinerReach.legTowards(new BlockPos(734, 66, 878), DEEP, mine(0)));
    }
}
