package com.villagecolony.core.construction.service;

import com.villagecolony.core.construction.model.Mine;

/**
 * A decisão pura por trás de {@code rerouteOrBlameTheMouth} — decisão 3B,
 * 2026-09-24.
 *
 * <p><b>Extração, não reescrita.</b> A regra já existia, inteira, dentro
 * de {@code MineTrouble.rerouteOrBlameTheMouth}: girar a hélice enquanto
 * houver hélice para tentar, e só culpar a boca depois que todas
 * falharem. Esta classe separa a <b>decisão</b> (pura, sobre o estado que
 * {@link Mine} já guarda) do <b>efeito</b> (achar a boca nova no mundo,
 * que continua em {@code fabric.work.MineDigging} — precisa de
 * {@code ServerWorld}).
 *
 * <p><b>Nunca toca {@code ServerWorld}, {@code BlockPos} ou
 * {@code MineMouth}.</b> Quem decide aqui não sabe onde a mina fica nem
 * se a boca nova existe — só que a mina <i>deveria</i> girar ou desistir
 * do poço atual, a partir da paciência que {@link Mine} já contava
 * sozinha.
 *
 * <p><b>{@code MineClaims} fica de fora, de propósito.</b> Quem está em
 * qual ramal agora é outro conceito — reserva de mineiro, mutável, sem
 * relação com a geometria da mina — e nunca foi parte desta decisão.
 */
public final class MineRecovery {

    /** O que fazer com uma mina que parou de progredir. */
    public enum Decision {
        /** Continua como está: a paciência ainda não estourou. */
        NO_ACTION,

        /** Troca a hélice da galeria e tenta de novo, na mesma boca. */
        REROUTE,

        /** As hélices se esgotaram: a boca é que está impedida. */
        EXHAUST_MOUTH
    }

    private MineRecovery() {
    }

    /**
     * A decisão para esta mina, a partir do que ela já sabe sobre si.
     *
     * <p>Não muda nada em {@code mine}: {@link Mine#turnedWithoutAPickaxe()}
     * já registrou a paciência antes desta chamada, e é o chamador quem
     * decide se aplica {@link Mine#reroute()} — este método só lê.
     */
    public static Decision recover(Mine mine) {
        if (mine.turnsWithoutAPickaxe() < Mine.TURNS_BEFORE_REROUTING) {
            return Decision.NO_ACTION;
        }

        return mine.mouthIsHopeless() ? Decision.EXHAUST_MOUTH : Decision.REROUTE;
    }
}
