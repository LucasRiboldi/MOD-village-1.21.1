package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ColonyPos;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * O que a varredura de lote fez nesta sessão, por colônia — 2026-08-27.
 *
 * <p><b>A pergunta que ela existe para responder.</b> Na sessão das
 * 19:11 a colônia passou 26 ciclos dizendo <i>"still sweeping"</i> e não
 * terminou uma volta. Dezessete passagens bastariam para as 16.641
 * colunas do raio de 64, e houve vinte e seis ciclos. Duas explicações
 * cabem no mesmo silêncio:
 *
 * <ol>
 *   <li>a varredura <b>reinicia</b> — o centro anda mais que o
 *       {@code CENTER_DRIFT} e o cursor é jogado fora, e ela recomeça do
 *       centro sem nunca chegar ao fim;
 *   <li>a varredura <b>não é chamada</b> — o planejador desiste antes do
 *       {@code BuildSiteScanner}, e o ciclo passa sem gastar passagem.
 * </ol>
 *
 * <p>O {@code IdleLog} não as separa, e por um motivo bom: ele registra
 * transições, e um ciclo em que o planejador nem roda não tem transição
 * nenhuma. O silêncio das duas é igual.
 *
 * <p><b>Os dois números que fecham a conta</b> são {@link Tally#asked()}
 * — quantas vezes o planejador chegou a perguntar — e
 * {@link Tally#passes()} — quantas passagens de fato correram. A
 * diferença é {@link Tally#bailed()}, e é ela que aponta o culpado:
 *
 * <pre>
 * 26 perguntas, 26 passagens, 26 reinícios   a varredura reinicia
 * 26 perguntas,  8 passagens,  1 reinício    o planejador desiste antes
 * </pre>
 *
 * <p>Molde do {@link com.villagecolony.fabric.event.ColonyStateLog}:
 * soma em memória, uma linha por colônia ao parar o servidor, e some
 * quando a pergunta estiver respondida.
 */
public final class SweepLog {

    /**
     * A partir daqui, reiniciar sem terminar nenhuma volta é o defeito.
     *
     * <p>Um reinício é normal — a primeira varredura da sessão começa do
     * centro por definição. Do segundo em diante, sem nenhuma volta
     * completa no meio, a varredura está rodando em falso.
     */
    private static final int RESTARTS_WITHOUT_A_ROUND = 2;

    /**
     * Abaixo disto não há do que acusar ninguém.
     *
     * <p>Uma colônia que rodou o planejador uma vez e desistiu não está
     * travada — ela mal começou. Sem este piso a primeira bateria de
     * testes de jogo abriu cinquenta avisos, um por vila de teste, e
     * aviso que sai sempre não avisa nada.
     */
    private static final int RUNS_BEFORE_JUDGING = 3;

    /** A soma de cada colônia, na ordem em que elas apareceram. */
    private static final Map<UUID, Tally> TALLIES = new LinkedHashMap<>();

    private SweepLog() {
    }

    /**
     * O que uma colônia fez de varredura nesta sessão.
     *
     * @param asked quantas vezes o planejador chegou a pedir um lote
     * @param passes quantas passagens de varredura correram
     * @param columns quantas colunas foram olhadas ao todo
     * @param restarts quantas vezes a varredura começou do anel zero
     * @param drifts quantos desses reinícios foram por deriva do centro
     * @param farthestDrift a maior deriva vista, em blocos
     * @param rounds quantas voltas completaram o raio inteiro
     * @param indexed quantas respostas saíram do índice, sem varrer
     */
    public record Tally(
            int asked,
            int passes,
            int columns,
            int restarts,
            int drifts,
            int farthestDrift,
            int rounds,
            int indexed) {

        private static final Tally EMPTY = new Tally(0, 0, 0, 0, 0, 0, 0, 0);

        /**
         * O que o planejador perguntou e nunca virou trabalho.
         *
         * <p>Nem passagem de varredura, nem resposta do índice: o ciclo
         * desistiu antes. É este número que absolve a varredura.
         *
         * <p><b>Só vale quando quem move o scanner é o planejador</b>, e
         * pode ficar negativo quando não é — os testes de jogo chamam o
         * {@code BuildSiteScanner} direto, sem passar por
         * {@code ConstructionPlanner.plan}, e aí há passagem sem
         * pergunta. Quem lê pergunta antes ao
         * {@link #gaveUpBeforeSweeping()}, que é onde a diferença tem
         * sentido; o relatório não imprime este número fora dali.
         */
        public int bailed() {
            return asked - passes - indexed;
        }

        /**
         * Se houve ciclo que desistiu antes de chegar à varredura.
         *
         * <p>Falso também quando o scanner foi movido de fora do
         * planejador: ali a conta não fecha por construção, e afirmar
         * qualquer coisa sobre ela seria inventar.
         */
        public boolean gaveUpBeforeSweeping() {
            return bailed() > 0;
        }

        /**
         * Se a varredura está rodando em falso.
         *
         * <p>Reiniciar depois de terminar o raio é o que ela deve fazer —
         * a vila muda, e o lote de ontem pode existir amanhã. Reiniciar
         * duas vezes <b>sem nunca terminar</b> é outra coisa.
         */
        public boolean restarting() {
            return rounds == 0 && restarts >= RESTARTS_WITHOUT_A_ROUND;
        }
    }

    private static void change(UUID colonyId, UnaryOperator<Tally> how) {
        TALLIES.put(colonyId, how.apply(TALLIES.getOrDefault(colonyId, Tally.EMPTY)));
    }

    /** O planejador chegou a pedir um lote a esta colônia. */
    public static void asked(UUID colonyId) {
        change(colonyId, was -> new Tally(
                was.asked() + 1, was.passes(), was.columns(), was.restarts(),
                was.drifts(), was.farthestDrift(), was.rounds(), was.indexed()));
    }

    /** Uma passagem de varredura correu, olhando estas colunas. */
    public static void pass(UUID colonyId, int columns) {
        change(colonyId, was -> new Tally(
                was.asked(), was.passes() + 1, was.columns() + columns, was.restarts(),
                was.drifts(), was.farthestDrift(), was.rounds(), was.indexed()));
    }

    /** A varredura começou do anel zero. */
    public static void restarted(UUID colonyId) {
        change(colonyId, was -> new Tally(
                was.asked(), was.passes(), was.columns(), was.restarts() + 1,
                was.drifts(), was.farthestDrift(), was.rounds(), was.indexed()));
    }

    /**
     * O centro andou o bastante para a medida anterior deixar de valer.
     *
     * <p>Fala na hora, e não só na soma: pelo javadoc do
     * {@code CENTER_DRIFT} isto deveria ser raro — três movimentos em
     * treze minutos na sessão de 08-25. Se a linha aparecer a cada
     * trinta segundos, a enxurrada <b>é</b> o achado.
     */
    public static void drifted(UUID colonyId, ColonyPos was, ColonyPos now) {
        long dx = (long) was.x() - now.x();
        long dz = (long) was.z() - now.z();

        int blocks = (int) Math.round(Math.sqrt(dx * dx + dz * dz));

        change(colonyId, before -> new Tally(
                before.asked(), before.passes(), before.columns(), before.restarts(),
                before.drifts() + 1, Math.max(before.farthestDrift(), blocks),
                before.rounds(), before.indexed()));

        VillageColonyMod.LOGGER.info(
                "Colony {} — the sweep starts over: the center moved {} blocks, from {} to {}",
                colonyId,
                blocks,
                was,
                now);
    }

    /** Uma volta terminou o raio inteiro. */
    public static void completed(UUID colonyId) {
        change(colonyId, was -> new Tally(
                was.asked(), was.passes(), was.columns(), was.restarts(),
                was.drifts(), was.farthestDrift(), was.rounds() + 1, was.indexed()));
    }

    /** O índice respondeu, e nenhuma coluna do quadrado foi olhada. */
    public static void indexed(UUID colonyId) {
        change(colonyId, was -> new Tally(
                was.asked(), was.passes(), was.columns(), was.restarts(),
                was.drifts(), was.farthestDrift(), was.rounds(), was.indexed() + 1));
    }

    /** A soma desta colônia, se ela chegou a ser perguntada. */
    public static Optional<Tally> tallyOf(UUID colonyId) {
        return Optional.ofNullable(TALLIES.get(colonyId));
    }

    /**
     * A soma da sessão, ao parar o servidor.
     *
     * <p>Uma linha por colônia, e ela é o instrumento: os números estão
     * todos ali, na ordem em que a conta se faz.
     */
    public static void report() {
        TALLIES.forEach((colonyId, tally) -> {
            // Só o que foi medido. A diferença entre perguntas e
            // passagens sai na linha seguinte, e só onde ela tem sentido.
            VillageColonyMod.LOGGER.info(
                    "Colony {} sweep: {} planner runs, {} passes over {} columns, {} answered"
                            + " by the index — {} restarts ({} by drift, farthest {} blocks),"
                            + " {} complete rounds",
                    colonyId,
                    tally.asked(),
                    tally.passes(),
                    tally.columns(),
                    tally.indexed(),
                    tally.restarts(),
                    tally.drifts(),
                    tally.farthestDrift(),
                    tally.rounds());

            if (tally.restarting()) {
                VillageColonyMod.LOGGER.warn(
                        "Colony {} restarted its sweep {} times without ever finishing the"
                                + " radius ({} by center drift). It is not advancing — it is"
                                + " starting over.",
                        colonyId,
                        tally.restarts(),
                        tally.drifts());

                return;
            }

            if (!tally.gaveUpBeforeSweeping()) {
                return;
            }

            VillageColonyMod.LOGGER.info(
                    "Colony {} — {} of {} planner runs gave up before reaching the sweep",
                    colonyId,
                    tally.bailed(),
                    tally.asked());

            if (tally.asked() >= RUNS_BEFORE_JUDGING
                    && tally.rounds() == 0
                    && tally.bailed() > tally.passes()) {
                VillageColonyMod.LOGGER.warn(
                        "Colony {} never finished a sweep, and the sweep is not why: most"
                                + " cycles gave up before reaching it. Look at what the planner"
                                + " refused, not at the sweep.",
                        colonyId);
            }
        });
    }

    /** Esquece a soma. Chamado ao parar o servidor, depois do relatório. */
    public static void clearAll() {
        TALLIES.clear();
    }
}
