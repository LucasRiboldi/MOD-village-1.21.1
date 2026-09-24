package com.villagecolony.core.worker.service;

import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.ProfessionType;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Por que cada vaga foi ou não preenchida — 2026-09-18.
 *
 * <p><b>A pergunta que o playtest do deserto abriu.</b> A obra parou 28
 * vezes esperando {@code cut_sandstone}, que é do <b>pedreiro</b>, e a
 * vila não tinha pedreiro nenhum: 109 linhas de fundidor, 64 de mineiro,
 * <b>uma</b> de pedreiro. E o pedreiro é o <b>terceiro</b> da
 * {@link ProfessionAssigner#PRODUCER_ORDER}, antes do fundidor — a ordem
 * foi desviada e nada no log diz por quê.
 *
 * <p><b>São quatro silêncios diferentes, e eles pedem consertos
 * opostos:</b>
 *
 * <pre>
 * FILLED      a vaga existia e alguém a ocupou
 * AT_TARGET   a profissão já tem o que a população paga
 * SHUNNED     havia vaga e o candidato está de castigo nela
 * NO_VACANCY  a colônia inteira está lotada
 * </pre>
 *
 * <p>Do lado de fora os três últimos são o mesmo nada: a vila simplesmente
 * não tem pedreiro. Distinguir {@code AT_TARGET} de {@code SHUNNED} é a
 * diferença entre <i>"a conta da população não abre a vaga"</i> — e aí o
 * conserto é a conta — e <i>"a vaga abre e ninguém a aceita"</i>, que é o
 * castigo prendendo a colônia.
 *
 * <p><b>É o mesmo movimento que resolveu o P1.0 e o P1.3</b>, e os dois
 * custaram dias antes de alguém instrumentar: o {@code LotRefusals} separou
 * <i>"não há chão"</i> de <i>"faltou varredura"</i>, e o
 * {@code ProtectionSample} decidiu o P1.3 numa leitura dizendo de que
 * blocos a recusa era feita.
 *
 * <p>Vive em {@code core} porque a decisão é de {@code core} — o
 * {@code ProfessionAssigner} não conhece Minecraft, e esta conta não
 * precisa conhecer.
 */
public final class HiringLog {

    static {
        ServerMemory.register(HiringLog.class, HiringLog::clearAll);
    }

    /** O que aconteceu com uma profissão nesta passagem de contratação. */
    public enum Outcome {

        /** A vaga existia e alguém a ocupou. */
        FILLED("filled"),

        /** A profissão já tem o número que a população adulta paga. */
        AT_TARGET("at target"),

        /** Havia vaga, e o candidato está de castigo nela. */
        SHUNNED("shunned by the candidate"),

        /**
         * Havia vaga, e o candidato acabou de largar um ofício — está
         * entre ofícios. Ver {@code Worker.BETWEEN_TRADES_CYCLES}.
         *
         * <p>Separado do {@code SHUNNED} de propósito: aquele é <i>"ele
         * não quer ESTA vaga"</i> e este é <i>"ele não quer vaga
         * nenhuma agora"</i>. Somados, escondem justamente o rodízio que
         * a sessão de 09-19 mediu.
         */
        BETWEEN_TRADES("just left a trade"),

        /** Nenhuma profissão tem vaga: a colônia está lotada. */
        NO_VACANCY("no vacancy anywhere");

        private final String said;

        Outcome(String said) {
            this.said = said;
        }

        @Override
        public String toString() {
            return said;
        }
    }

    /**
     * Quantas colônias guardar antes de esquecer tudo.
     *
     * <p>Mesmo teto e mesma razão do {@code LotRefusals}: um save com
     * muitas vilas não pode fazer o diagnóstico crescer sem fim.
     */
    private static final int MAX_COLONIES = 64;

    private static final Map<UUID, Map<ProfessionType, Map<Outcome, Integer>>> COUNTED =
            new HashMap<>();

    private HiringLog() {
    }

    /** Registra o que aconteceu com esta profissão. */
    public static void record(UUID colonyId, ProfessionType profession, Outcome outcome) {
        if (COUNTED.size() >= MAX_COLONIES && !COUNTED.containsKey(colonyId)) {
            COUNTED.clear();
        }

        COUNTED.computeIfAbsent(colonyId, id -> new EnumMap<>(ProfessionType.class))
                .computeIfAbsent(profession, type -> new EnumMap<>(Outcome.class))
                .merge(outcome, 1, Integer::sum);
    }

    /**
     * A linha do relatório desta colônia, ou vazio se não houve nada.
     *
     * <p>Só as profissões que <b>não</b> foram preenchidas entram: quem
     * conseguiu gente não é o assunto, e listá-la afogaria a que falta.
     */
    public static String report(UUID colonyId) {
        Map<ProfessionType, Map<Outcome, Integer>> byProfession = COUNTED.get(colonyId);

        if (byProfession == null || byProfession.isEmpty()) {
            return "";
        }

        StringBuilder said = new StringBuilder();

        for (ProfessionType type : ProfessionAssigner.PRODUCER_ORDER) {
            Map<Outcome, Integer> outcomes = byProfession.get(type);

            // <b>Preenchida ALGUMA VEZ não é preenchida agora</b> —
            // 2026-09-19. A primeira versão saía do relatório para sempre
            // depois de um único FILLED, e o contador é acumulativo: numa
            // sessão de 61 passagens, o mineiro contratado na primeira
            // sumia das outras sessenta. A linha ficou dizendo
            // "MASON/SMELTER/CARPENTER at target" sem citar MINER e
            // LUMBERJACK, e eu li isso como "a vaga do pedreiro não
            // abre" — quando o certo era que ela estava preenchida.
            //
            // Agora compara: se houve mais no-alvo do que contratações, a
            // profissão passou a maior parte do tempo sem vaga e isso é o
            // que o relatório existe para mostrar.
            if (outcomes == null) {
                continue;
            }

            int filled = outcomes.getOrDefault(Outcome.FILLED, 0);

            int denied = outcomes.values().stream().mapToInt(Integer::intValue).sum() - filled;

            if (denied == 0) {
                continue;
            }

            for (Map.Entry<Outcome, Integer> entry : outcomes.entrySet()) {
                if (said.length() > 0) {
                    said.append("; ");
                }

                said.append(type)
                        .append(' ')
                        .append(entry.getKey())
                        .append(" x")
                        .append(entry.getValue());
            }
        }

        return said.toString();
    }

    /** Quantas vezes esta profissão bateu neste desfecho. Para a bateria. */
    public static int countOf(UUID colonyId, ProfessionType profession, Outcome outcome) {
        return COUNTED.getOrDefault(colonyId, Map.of())
                .getOrDefault(profession, Map.of())
                .getOrDefault(outcome, 0);
    }

    /** Esquece o que foi contado. Chamado ao parar o servidor. */
    public static void clearAll() {
        COUNTED.clear();
    }
}
