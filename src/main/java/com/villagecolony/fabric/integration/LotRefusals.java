package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Por que cada lote candidato foi recusado — 2026-09-11.
 *
 * <p><b>É o P0.1 do plano de correção ao pé da letra:</b> <i>"transforme
 * 'nothing to work on' em 'rejeitei 12 lotes por X, Y, Z'"</i>. A
 * varredura olha dezesseis mil colunas e devolve uma resposta binária;
 * quando ela diz não, ninguém sabe se foi por terreno, por ocupação ou
 * por proteção — e são consertos completamente diferentes.
 *
 * <p><b>Contagem, e não linha por candidato.</b> O {@code IdleLog}
 * registra só quando o motivo muda, e o javadoc dele explica por quê:
 * detalhe que muda todo ciclo faz a linha voltar sempre, que é a
 * enxurrada que ele existe para evitar. Uma linha por lote recusado
 * seria essa enxurrada elevada ao quadrado — a sessão de 2026-09-11 às
 * 02:03 já mostrou o que isso faz, com 4.389 linhas de areia em 6.117.
 * Então se conta, e se diz o resumo uma vez por relatório, junto do
 * {@link SweepLog}.
 *
 * <p><b>A pergunta que esta classe existe para responder</b> está em
 * {@code docs/research/terraplanagem-da-vila.md}: o autor decidiu que a
 * terraplanagem da vila só abre <i>com número</i>, e não com a inferência
 * de que o terreno é a causa de a vila não crescer. O número é o
 * {@link Reason#OFF_ROAD_LEVEL} contra os outros quatro.
 */
public final class LotRefusals {

    private LotRefusals() {
    }

    /**
     * Os cinco motivos pelos quais uma coluna candidata some.
     *
     * <p>Na ordem em que o {@code flatGroundAt} pergunta, que é da mais
     * barata para a mais cara.
     */
    public enum Reason {

        /** Não há chão na janela vertical da vila — morro alto ou buraco fundo. */
        NO_GROUND("no ground in the village's vertical window"),

        /** Uma cama ocupa o piso candidato e nunca é tratada como terreno. */
        BED("a bed occupies the candidate floor"),

        /** Há chão, e ele não é natural — calçamento, madeira, obra. */
        NOT_NATURAL_GROUND("the ground there is not natural soil"),

        /** Peça da vila gerada ou coisa do jogador. A Regra 3. */
        PROTECTED("village-original or player-placed, and Rule 3 protects it"),

        /** A coluna está na área espacial reservada de uma estrada. */
        ROAD("the ground is inside a reserved road area"),

        /**
         * O chão não está <b>no nível da rua</b> — a Regra 19.
         *
         * <p><b>E a pergunta é exata, não tolerante.</b> O
         * {@code flatGroundAt} compara {@code ground.getY() != roadY}:
         * uma única coluna um bloco acima ou abaixo reprova o lote
         * inteiro. É esta a contagem que decide se a terraplanagem vale
         * — ver o documento de pesquisa.
         */
        OFF_ROAD_LEVEL("the ground is not at street level"),

        /** A coluna tem coisa dentro do volume da casa. A Regra 22. */
        OCCUPIED("something stands inside the house's volume");

        private final String said;

        Reason(String said) {
            this.said = said;
        }

        @Override
        public String toString() {
            return said;
        }
    }

    /** O que cada colônia recusou nesta volta, e por quê. */
    private static final Map<UUID, Map<Reason, Integer>> COUNTED = new HashMap<>();

    /**
     * Teto de colônias guardadas, para o servidor que vive dias.
     *
     * <p>Mesma defesa do {@code MineMarks.MAX_REFUSED}, e pelo mesmo
     * motivo: vinte colônias carregadas é o mundo do autor, e um mapa que
     * cresce sem limite é vazamento com outro nome.
     */
    private static final int MAX_COLONIES = 256;

    /**
     * Quantas colunas sobreviveram a todas as recusas, por colônia.
     *
     * <p><b>O numerador que faltava</b> — 2026-09-17. Até aqui só o que
     * some era contado, e o playtest de 00:09 mostrou o preço: 174.912
     * recusas e nenhuma resposta para <i>"sobrou alguma coluna?"</i>.
     * Sem este número, "53% caem na reserva de estrada" não distingue
     * uma vila apertada de uma vila sem um palmo de chão livre — e as
     * duas pedem consertos diferentes.
     *
     * <p>Conta <b>colunas</b>, como as recusas, e não lotes: é a única
     * forma de os dois números se somarem na mesma unidade. Uma pegada
     * aprovada acrescenta todas as colunas dela de uma vez, porque
     * {@code flatGroundAt} só chega ao fim quando nenhuma reprovou.
     */
    private static final Map<UUID, Integer> ACCEPTED = new HashMap<>();

    /** Esta coluna não deu lote, e este foi o motivo. */
    public static void refused(UUID colonyId, Reason reason) {
        if (COUNTED.size() >= MAX_COLONIES && !COUNTED.containsKey(colonyId)) {
            COUNTED.clear();
        }

        COUNTED.computeIfAbsent(colonyId, id -> new EnumMap<>(Reason.class))
                .merge(reason, 1, Integer::sum);
    }

    /**
     * Uma pegada inteira passou: estas colunas sobreviveram a tudo.
     *
     * <p>Chamado de {@code LotLevel.flatGroundAt}, no único
     * ponto em que ele devolve piso — ver {@link #ACCEPTED}.
     *
     * @param columns quantas colunas a pegada tem
     */
    public static void accepted(UUID colonyId, int columns) {
        if (ACCEPTED.size() >= MAX_COLONIES && !ACCEPTED.containsKey(colonyId)) {
            ACCEPTED.clear();
        }

        ACCEPTED.merge(colonyId, columns, Integer::sum);
    }

    /**
     * Diz o que esta colônia recusou, e esquece.
     *
     * <p>Chamado de onde o {@link SweepLog} fecha o relatório da sessão,
     * porque as duas contagens respondem à mesma pergunta e lidas juntas
     * valem mais: <i>quantas passagens custou</i> e <i>o que foi
     * recusado no caminho</i>.
     *
     * <p>Silencioso quando não houve recusa nenhuma — colônia que achou
     * lote de primeira não tem o que reportar, e uma linha de zeros só
     * gastaria o log.
     */
    public static void report(UUID colonyId) {
        Map<Reason, Integer> counted = COUNTED.remove(colonyId);

        Integer survivors = ACCEPTED.remove(colonyId);

        int accepted = survivors == null ? 0 : survivors;

        if ((counted == null || counted.isEmpty()) && accepted == 0) {
            return;
        }

        StringBuilder text = new StringBuilder();

        int total = 0;

        if (counted != null) {
            for (Map.Entry<Reason, Integer> entry : counted.entrySet()) {
                if (text.length() > 0) {
                    text.append("; ");
                }

                text.append(entry.getValue()).append(" ").append(entry.getKey());

                total += entry.getValue();
            }
        }

        VillageColonyMod.LOGGER.info(
                "Colony {} lot refusals: {} candidates turned down — {}",
                colonyId,
                total,
                text);

        // <b>E a linha que responde se sobrou chão</b> — 2026-09-17.
        // Separada da de cima de propósito: aquela é o inventário do que
        // some, e esta é a única pergunta que decide o conserto. Ler as
        // duas juntas e fazer a conta de cabeça foi o que esta sessão
        // teve de fazer, e é o que ela deixa de exigir.
        //
        // Zero aceitas não é o mesmo que poucas: zero diz que a vila não
        // tem um palmo livre e o conserto é afrouxar alguma recusa;
        // poucas dizem que há chão e o problema é orçamento de varredura.
        VillageColonyMod.LOGGER.info(
                "Colony {} lot columns: {} survived every check, {} were turned down"
                        + " — {}% of what was looked at",
                colonyId,
                accepted,
                total,
                accepted + total == 0 ? 0 : Math.round(100.0 * accepted / (accepted + total)));

        // <b>E o que a Regra 3 protegeu</b> — P1.3, 2026-09-18. A linha
        // acima diz quantas foram, e esta diz de que são feitas: areia de
        // terreno e casa de verdade recusam pelo mesmo motivo e pedem
        // consertos opostos. Sai junto porque é aqui que quem investiga
        // já está olhando. Ver ProtectionSample.
        ProtectionSample.report();

        // E de que a Regra 22 e feita — P1.6. Ver VolumeSample.
        VolumeSample.report();
    }

    /** Quantas recusas deste motivo a colônia acumulou. Para a bateria. */
    public static int countOf(UUID colonyId, Reason reason) {
        return COUNTED.getOrDefault(colonyId, Map.of()).getOrDefault(reason, 0);
    }

    /** Quantas colunas sobreviveram a tudo nesta colônia. Para a bateria. */
    public static int acceptedIn(UUID colonyId) {
        return ACCEPTED.getOrDefault(colonyId, 0);
    }

    /** Esquece somente a telemetria de uma colônia, para testes isolados. */
    public static void clear(UUID colonyId) {
        COUNTED.remove(colonyId);
        ACCEPTED.remove(colonyId);
    }

    /** Esquece tudo. Os testes e o fim do servidor. */
    public static void clearAll() {
        COUNTED.clear();
        ACCEPTED.clear();
    }
}
