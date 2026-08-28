package com.villagecolony.fabric.work;

import net.minecraft.util.math.BlockPos;

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
}
