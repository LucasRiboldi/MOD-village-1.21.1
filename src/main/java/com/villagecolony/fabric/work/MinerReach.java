package com.villagecolony.fabric.work;

import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * A que distância o mineiro alcança a pedra — 2026-08-27.
 *
 * <p><b>Uma conta só, e é esta.</b> Até hoje havia duas: o alcance media
 * com a posição real do aldeão, e o relatório media com
 * {@code getBlockPos()} — inteiro — e ainda truncava a raiz. Qualquer
 * distância entre 4,0 e 4,99 saía no log como <i>"4 blocks away"</i> e
 * estava <b>fora</b> de alcance.
 *
 * <p>A sessão das 22:19 passou dois mil e quatrocentos tiques dizendo
 * que o mineiro estava a quatro blocos da pedra que ele não alcançava, e
 * mandou procurar o defeito onde ele não estava. Instrumento que mente é
 * pior que instrumento nenhum.
 *
 * <p><b>Por que numa classe só dela.</b> {@code MinerWork} não carrega
 * fora do jogo — os estáticos dele pedem o registro de itens —, e isto
 * aqui é geometria: três subtrações e uma raiz, que se afirmam sem subir
 * servidor. Ver {@code MinerReachTest}.
 */
public final class MinerReach {

    /**
     * Quanto o braço do mineiro alcança, em blocos.
     *
     * <p>Medido em três dimensões, e o {@code dy} é o E30: quatro blocos
     * no plano mais quatro de altura são cinco e meia de distância real.
     * Enquanto a altura não contava, o mineiro batia na pedra de cima do
     * buraco e ela caía — o que fazia a mina descer sem ninguém dentro
     * dela.
     */
    public static final int REACH = 4;

    private MinerReach() {
    }

    /** A distância daqui até o centro daquele bloco. */
    public static double distanceTo(double x, double y, double z, BlockPos target) {
        double dx = x - (target.getX() + 0.5);
        double dy = y - (target.getY() + 0.5);
        double dz = z - (target.getZ() + 0.5);

        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Se daqui se alcança aquele bloco. */
    public static boolean isWithinReach(double x, double y, double z, BlockPos target) {
        return distanceTo(x, y, z, target) <= REACH;
    }

    /**
     * A perna de caminhada que a navegação do jogo cumpre sem se perder.
     *
     * <p>Oito blocos, e o número vem do que se viu: a mina desce vinte, e
     * um destino a vinte atravessando rocha devolve caminho parcial. Oito
     * é curto o bastante para o caminho ser contínuo e longo o bastante
     * para ele não reavaliar o destino a cada passo.
     */
    public static final int LEG = 8;

    /**
     * Para onde mandar o aldeão agora — a boca da mina, ou o destino.
     *
     * <p><b>A sessão da meia-noite mostrou onde ele estava</b>, e foi a
     * primeira vez que se soube: <i>"the miner is at 734, 66, 878"</i>.
     * Y 66 é a superfície. Ele estava vinte e um blocos em linha reta
     * <b>acima</b> da galeria, em cima do chão, mirando uma pedra no
     * fundo da mina.
     *
     * <p>A navegação do jogo recebe um destino a vinte blocos
     * atravessando rocha maciça, devolve caminho parcial, e ele
     * estaciona no ponto mais próximo que consegue — bem ali em cima. É
     * o sintoma que o MineColonies registrou na issue 4297 com as mesmas
     * palavras, e o remendo do jogador é o mesmo que o autor fez: cavar
     * até lá.
     *
     * <p><b>Não se pede à navegação um caminho que ela não sabe
     * traçar.</b> Pede-se a boca, que fica na superfície e a que se chega
     * andando; de dentro dela a escada é um corredor, e o resto é curto.
     * Chegando à boca, o destino passa a ser a pedra — sem esta segunda
     * metade ele trocaria um travamento por outro, parado na entrada
     * para sempre.
     *
     * <p>Não é a solução do MineColonies, que trocou a navegação inteira
     * por um A* próprio. É a que cabe aqui, e ataca exatamente o que se
     * viu: ele nunca entrava.
     *
     * @param mouth a boca da mina desta colônia, vazia quando não há
     *     mina — a pedra de superfície não tem descida a fazer
     */
    public static BlockPos legTowards(
            BlockPos villager, BlockPos destination, Optional<BlockPos> mouth) {

        if (mouth.isEmpty()) {
            return destination;
        }

        if (Math.sqrt(villager.getSquaredDistance(destination)) <= LEG) {
            return destination;
        }

        return Math.sqrt(villager.getSquaredDistance(mouth.get())) <= LEG
                ? destination
                : mouth.get();
    }
}
