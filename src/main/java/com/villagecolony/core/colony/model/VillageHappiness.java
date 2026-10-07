package com.villagecolony.core.colony.model;

/**
 * A felicidade da vila, medida na reunião (o ciclo de 30 s) — ADR-036 item 20.
 *
 * <p>Três medidas, cada uma soma +1, 0 ou −1:
 * <ul>
 *   <li><b>comida por adulto</b>, em pontos de comida do Vanilla guardados nos
 *       baús: {@value #FED} ou mais soma; menos de {@value #HUNGRY} tira;</li>
 *   <li><b>camas sobrando</b>: {@value #ROOMY} ou mais soma; nenhuma tira;</li>
 *   <li><b>obras concluídas</b> pela colônia: pelo menos uma soma;</li>
 *   <li><b>convívio</b>: todos os adultos saíram alimentados da reunião, soma
 *       (ADR-037 R1).</li>
 * </ul>
 * A comida conta o trigo: três valem um pão, quatro pontos (ADR-037 C7 b).
 * Soma {@value #HAPPY_AT} ou mais é feliz (mais filhos); {@value #UNHAPPY_AT} ou
 * menos é infeliz (nenhum filho novo).
 *
 * @param mood o humor
 * @param foodPerAdult pontos de comida por adulto
 * @param spareBeds camas além das dos adultos (negativo: falta cama)
 * @param finishedBuildings obras que a colônia concluiu
 * @param score a soma das três medidas
 */
public record VillageHappiness(Mood mood, int foodPerAdult, int spareBeds, int finishedBuildings, int score) {

    /** Doze pontos: o que um aldeão do Vanilla precisa para se reproduzir uma vez. */
    public static final int FED = 12;

    public static final int HUNGRY = 4;

    public static final int ROOMY = 2;

    public static final int HAPPY_AT = 2;

    public static final int UNHAPPY_AT = -1;

    /** Pontos de comida por trigo: três trigos, um pão de quatro pontos (C7 b). */
    public static int wheatPoints(int wheat) {
        return Math.max(0, wheat) * 4 / 3;
    }

    public enum Mood { HAPPY, CONTENT, UNHAPPY }

    /**
     * Mede a vila nesta reunião.
     *
     * @param foodPoints pontos de comida em todos os baús da colônia
     * @param adults aldeões adultos vivos da vila
     * @param beds camas observadas na vila
     * @param finishedBuildings obras concluídas pela colônia
     */
    public static VillageHappiness measure(int foodPoints, int adults, int beds, int finishedBuildings) {
        return measure(foodPoints, adults, beds, finishedBuildings, false);
    }

    /**
     * O mesmo, sabendo se a reunião alimentou todos os adultos.
     *
     * @param everyoneFed todo adulto tem ao menos {@link #FED} pontos no bolso
     */
    public static VillageHappiness measure(
            int foodPoints, int adults, int beds, int finishedBuildings, boolean everyoneFed) {
        int perAdult = Math.max(0, foodPoints) / Math.max(1, adults);
        int spare = beds - adults;
        int score = (perAdult >= FED ? 1 : perAdult < HUNGRY ? -1 : 0)
                + (spare >= ROOMY ? 1 : spare <= 0 ? -1 : 0)
                + (finishedBuildings > 0 ? 1 : 0)
                + (everyoneFed && adults > 0 ? 1 : 0);

        Mood mood = score >= HAPPY_AT ? Mood.HAPPY : score <= UNHAPPY_AT ? Mood.UNHAPPY : Mood.CONTENT;

        return new VillageHappiness(mood, perAdult, spare, finishedBuildings, score);
    }
}
