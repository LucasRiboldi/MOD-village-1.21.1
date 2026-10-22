package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import org.jspecify.annotations.Nullable;

/**
 * Os pontos de onde a escada de um nível tenta descer — decisão do autor de 2026-10-22 ("insistir").
 *
 * <p>Girar a hélice ({@link MineShaft#rerouted()}) só troca o rumo em volta da <b>mesma</b> entrada, num
 * quadrado de lado {@link MineShaft#HELIX_SIDE}. No deserto (0.3.86) as dez hélices bateram na mesma
 * bolsa de água em cinco segundos e a mina se mudou para uma boca que ninguém alcançava. Agora, a cada
 * volta completa ({@link #TURNS} rumos) sem passar do salão, a entrada anda {@link #SHIFT} blocos dentro
 * do nível, até {@link #SHIFTS} vezes. Não é gravado no save, como as hélices de {@link Mine}.
 */
public final class MineStairPoints {

    /** Os rumos de uma hélice: uma volta completa. */
    public static final int TURNS = 4;

    /** Quantos pontos novos a escada tenta antes de culpar a boca. */
    public static final int SHIFTS = 4;

    /** Quanto a entrada anda a cada deslocamento: duas hélices, para sair da bolsa da anterior. */
    public static final int SHIFT = MineShaft.HELIX_SIDE * 2;

    private int shifts;

    /** A entrada antes do primeiro deslocamento: os pontos novos são medidos dela, e não um do outro. */
    private @Nullable ColonyPos firstEntry;

    /**
     * A escada da partida {@code helicesTried}: o mesmo ponto girado, ou, a cada volta completa, um ponto novo.
     * A rampa só gira — ela já é a saída do fundo.
     */
    MineShaft next(MineShaft shaft, int helicesTried) {
        if (shaft.isRamp() || helicesTried % TURNS != 0 || shifts >= SHIFTS) {
            return shaft.rerouted();
        }

        if (firstEntry == null) {
            firstEntry = shaft.entry();
        }

        shifts++;

        return MineShaft.from(shiftedEntry(firstEntry, shifts), shaft.descent().clockwise());
    }

    int shifts() {
        return shifts;
    }

    /**
     * O ponto do deslocamento {@code shift} (1, 2, 3, 4): {@link #SHIFT} blocos para leste, oeste, sul e
     * norte da entrada original, na mesma altura — um lado por vez, para a bolsa que barrou um não barrar
     * o seguinte.
     */
    static ColonyPos shiftedEntry(ColonyPos origin, int shift) {
        Side side = switch ((shift - 1) % TURNS) {
            case 0 -> Side.EAST;
            case 1 -> Side.WEST;
            case 2 -> Side.SOUTH;
            default -> Side.NORTH;
        };
        int reach = SHIFT * ((shift - 1) / TURNS + 1);

        return new ColonyPos(origin.x() + side.offsetX() * reach, origin.y(), origin.z() + side.offsetZ() * reach);
    }
}
