package com.villagecolony.core.coordination;

/**
 * Até onde a colheita vai, e ela cresce com a vila — N11, 2026-09-24.
 *
 * <p><b>Decisão do autor:</b> <i>"crescer junto com a vila"</i>. O
 * lenhador procurava árvore a 64 blocos e o mineiro procurava areia a 48
 * desde o primeiro dia, numa vila de três casas: o desmatamento e as
 * crateras apareciam longe, onde o jogador não esperava que uma aldeia
 * pequena chegasse. Agora a vila jovem trabalha no próprio entorno e vai
 * mais longe à medida que ganha camas — a mesma medida que o Vanilla usa
 * para dizer o tamanho de uma vila.
 *
 * <p><b>A régua:</b> {@value #BASE} blocos mais {@value #PER_BED} por cama,
 * sem passar do teto de cada ofício (o raio que ele tinha antes).
 *
 * <p><b>Zero camas não encolhe nada.</b> A contagem nasce em zero e só é
 * preenchida quando a detecção observa a vila; até lá, zero quer dizer
 * "ainda não medido", e não "vila sem casa". É a mesma guarda do E48.
 */
public final class GatheringReach {

    /** O alcance da vila recém-fundada. */
    public static final int BASE = 24;

    /** Quanto cada cama acrescenta ao alcance. */
    public static final int PER_BED = 2;

    private GatheringReach() {
    }

    /**
     * Até onde o ofício coleta numa vila com tantas camas, sem passar do teto dele.
     *
     * @param beds as camas que a detecção observou nesta vila
     * @param ceiling o alcance máximo do ofício
     */
    public static int radius(int beds, int ceiling) {
        if (ceiling <= 0) {
            throw new IllegalArgumentException("ceiling must be positive: " + ceiling);
        }

        if (beds <= 0) {
            return ceiling;
        }

        return Math.min(ceiling, BASE + PER_BED * beds);
    }
}
