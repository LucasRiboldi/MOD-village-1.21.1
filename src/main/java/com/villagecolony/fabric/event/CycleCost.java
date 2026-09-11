package com.villagecolony.fabric.event;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Onde o ciclo da colônia gasta o tempo dele — P2.1, 2026-09-11.
 *
 * <p><b>Medir antes de otimizar</b>, que é a ordem que o plano de
 * correção dá e a razão de esta classe vir sozinha, sem nenhuma
 * otimização junto. O ciclo mediu <b>112 ms</b> contra o orçamento de 50,
 * e o log tinha uma linha só — {@code Colony cycle took 112 ms} — que diz
 * <i>que</i> custou caro e não diz <b>onde</b>. Com seis colônias e
 * setenta e oito trabalhadores, "onde" tem pelo menos seis respostas
 * possíveis, e escolher uma por palpite é o que o §11 existe para
 * impedir.
 *
 * <p><b>As fases são as que existem, e não as que o plano listou.</b> Ele
 * pedia <i>planner, chest scan, task assign, workers, persistence,
 * other</i>. Persistência não entra: o {@code ColonySavedData.sync} roda
 * no {@code SERVER_STOPPING} e em lugar nenhum do ciclo, então uma fase
 * para ela reportaria zero constante — um número que parece medida e não
 * é. Entraram no lugar as duas do topo que o plano não previa, a detecção
 * em volta do jogador e o ciclo de vida das colônias, que rodam antes de
 * qualquer colônia ser vista.
 *
 * <p><b>O que sobra é {@code other}</b>, e ele é informação: é o ciclo
 * menos tudo o que se sabe nomear. Crescer ali quer dizer que falta uma
 * fase nesta lista, e não que o custo sumiu.
 *
 * <p>O relógio só é lido nas fronteiras — seis ou sete vezes por ciclo, a
 * vinte e poucos nanossegundos cada. Medir aqui custa menos que uma
 * leitura de bloco.
 *
 * <p>Uma thread só, a do servidor: é ela que roda o tique, e por isso não
 * há sincronização aqui.
 */
final class CycleCost {

    /** Os pedaços do ciclo que sabem dizer o próprio nome. */
    enum Phase {

        /** Procurar vila em volta dos jogadores e dos centros conhecidos. */
        DETECT("detect"),

        /** Nascer, crescer e abandonar colônia. */
        LIFECYCLE("lifecycle"),

        /** Ler os baús e medir o espaço que sobra neles. */
        CHESTS("chests"),

        /** Decidir obra, rua e o que a construção pede. */
        PLANNER("planner"),

        /** Casar tarefa com trabalhador. */
        ASSIGN("assign"),

        /** As sete profissões andando. */
        WORKERS("workers");

        private final String label;

        Phase(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }
    }

    private static final long NANOS_PER_MILLI = 1_000_000L;

    private static final Map<Phase, Long> NANOS = new EnumMap<>(Phase.class);

    private CycleCost() {
    }

    /** Começa a contar um ciclo novo, esquecendo o anterior. */
    static void startOver() {
        NANOS.clear();
    }

    /**
     * Cobra a esta fase o tempo decorrido desde {@code mark}, e devolve o
     * agora — para servir de marca da fase seguinte.
     *
     * <p>O bastão passa de fase em fase assim: nenhum intervalo fica sem
     * dono, e nenhum é contado duas vezes. Somar em vez de sobrescrever
     * importa porque a detecção acontece em dois momentos do mesmo ciclo,
     * e porque cada colônia cobra as fases dela.
     */
    static long since(Phase phase, long mark) {
        long now = System.nanoTime();

        NANOS.merge(phase, now - mark, Long::sum);

        return now;
    }

    /**
     * A repartição do ciclo, da fase mais cara para a mais barata.
     *
     * <p>Todas as fases aparecem, inclusive as zeradas: <i>"o planejador
     * custou zero"</i> é uma resposta, e omiti-la deixaria quem lê sem
     * saber se a fase é barata ou se ninguém a mediu.
     *
     * @param totalNanos o ciclo inteiro, para o {@code other} sair da
     *     subtração
     */
    static String breakdown(long totalNanos) {
        List<Map.Entry<Phase, Long>> phases = new ArrayList<>();

        long named = 0;

        for (Phase phase : Phase.values()) {
            long nanos = NANOS.getOrDefault(phase, 0L);

            named += nanos;

            phases.add(Map.entry(phase, nanos));
        }

        phases.sort(Map.Entry.<Phase, Long>comparingByValue().reversed());

        StringBuilder line = new StringBuilder();

        for (Map.Entry<Phase, Long> phase : phases) {
            if (line.length() > 0) {
                line.append(", ");
            }

            line.append(phase.getKey().label())
                    .append(' ')
                    .append(phase.getValue() / NANOS_PER_MILLI)
                    .append(" ms");
        }

        // Nunca negativo: o total vem de fora e uma fase que o ultrapasse
        // por arredondamento sairia como "other -1 ms", que é medida
        // impossível e faria duvidar do resto da linha.
        long other = Math.max(0L, totalNanos - named);

        line.append(", other ").append(other / NANOS_PER_MILLI).append(" ms");

        // <b>E quando o corte machuca, ele fala.</b> Cortar em zero e
        // calar seria trocar um número impossível por um plausível — o
        // defeito-que-parece-número do V5, que o P0.2 desta mesma data
        // custou um bloqueador inteiro para reaprender. Fase somando mais
        // que o ciclo só acontece por bastão mal passado: um trecho
        // cobrado a duas fases. Quem lê precisa saber que os números
        // acima estão inflados antes de sair otimizando o primeiro deles.
        long overlap = named - totalNanos;

        if (overlap > NANOS_PER_MILLI) {
            line.append(" (phases overlap by ")
                    .append(overlap / NANOS_PER_MILLI)
                    .append(" ms — the numbers above are inflated)");
        }

        return line.toString();
    }
}
