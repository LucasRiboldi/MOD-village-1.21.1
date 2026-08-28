package com.villagecolony.fabric.work;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Por onde o mineiro entra na mina — 2026-08-28.
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
 * reta <b>acima</b> da galeria, em cima do chão, mirando uma pedra no
 * fundo da mina. A navegação do jogo recebe um destino a vinte blocos
 * atravessando rocha maciça, devolve caminho parcial, e ele estaciona no
 * ponto mais próximo que consegue — que é bem ali em cima.
 *
 * <p>É o sintoma que o MineColonies registrou na
 * <a href="https://github.com/ldtteam/minecolonies/issues/4297">issue
 * 4297</a> com as mesmas palavras — <i>"o mineiro fica parado na
 * superfície acima do alvo"</i> —, e o remendo do jogador é o mesmo que
 * o autor fez: cavar até lá.
 *
 * <p><b>A perna que faltava.</b> Não se pede à navegação um caminho que
 * ela não sabe traçar: pede-se a <b>boca da mina</b>, que fica na
 * superfície e a que se chega andando. De dentro dela a escada é um
 * corredor, e o resto do caminho é curto.
 */
class MinerLegTest {

    private static final BlockPos MOUTH = new BlockPos(732, 63, 898);

    private static final BlockPos DEEP = new BlockPos(735, 45, 878);

    /** Longe da pedra e longe da boca: entra pela boca. */
    @Test
    void fromTheSurfaceHeAimsForTheMouth() {
        assertEquals(
                MOUTH,
                MinerReach.legTowards(new BlockPos(734, 66, 878), DEEP, Optional.of(MOUTH)));
    }

    /**
     * Já na boca, ele mira a pedra.
     *
     * <p>Sem isto ele ficaria parado na entrada para sempre, trocando um
     * travamento por outro.
     */
    @Test
    void atTheMouthHeAimsForTheStone() {
        assertEquals(
                DEEP,
                MinerReach.legTowards(new BlockPos(731, 63, 898), DEEP, Optional.of(MOUTH)));
    }

    /**
     * Perto da pedra, a boca não interessa.
     *
     * <p>Dentro da galeria ele está a metros do alvo e a dezenas da
     * boca; mandá-lo voltar seria desfazer a descida a cada passo.
     */
    @Test
    void insideTheGalleryHeKeepsAimingForTheStone() {
        assertEquals(
                DEEP,
                MinerReach.legTowards(new BlockPos(730, 45, 878), DEEP, Optional.of(MOUTH)));
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
}
