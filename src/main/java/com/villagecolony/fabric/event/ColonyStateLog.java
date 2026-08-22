package com.villagecolony.fabric.event;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.ColonyState;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Toda troca de estado de colônia, e quantas foram.
 *
 * <p><b>Existe para o E9</b> — decisão do autor em 2026-08-21. O erro
 * registrado diz que a marca {@code ABANDONED} é desmarcada no ciclo
 * seguinte, e ele está aberto desde 08-15 com a etiqueta "provável, não
 * investigado". Hoje {@code ABANDONED} não muda nada no comportamento
 * da colônia, o que esconde o sintoma: ninguém repara na marca oscilando
 * porque ela não custa nada.
 *
 * <p>A decisão foi não implementar a TASK-048 no escuro. Uma colônia
 * viva marcada por engano teria a construção silenciada sem que nada
 * avisasse, e "parar de crescer" só é seguro se a marca estiver certa.
 * O que vem antes é o dado.
 *
 * <p><b>O que ela acrescenta ao que já existia.</b> As duas linhas de
 * transição já saíam, uma por troca. Faltavam as duas coisas que
 * transformam linhas soltas em resposta:
 *
 * <ul>
 *   <li>o <b>motivo</b> nos dois sentidos — a linha do abandono já
 *       dizia o que a sonda viu, e a da volta não dizia nada. Sem ele
 *       não se distingue uma vila reconstruída de uma sonda que
 *       enxergou mal;
 *   <li>a <b>soma por colônia</b>, ao parar o servidor. Uma colônia que
 *       trocou de estado quarenta vezes numa sessão é o E9; uma que
 *       trocou uma vez é uma vila que acabou de verdade.
 * </ul>
 *
 * <p>Some quando o E9 fechar.
 */
public final class ColonyStateLog {

    /** Quantas trocas cada colônia teve nesta sessão. */
    private static final Map<UUID, Integer> FLIPS = new LinkedHashMap<>();

    /**
     * Acima disto a marca está oscilando, e não relatando.
     *
     * <p>Duas trocas são uma vila que acabou e foi refeita. A partir da
     * terceira, numa sessão só, é o E9.
     */
    private static final int OSCILLATING = 3;

    private ColonyStateLog() {
    }

    /**
     * Uma troca de estado, com o motivo, e o registro dela na soma.
     *
     * @param from o estado que a colônia tinha
     * @param to o estado que ela passa a ter
     * @param sawWhat o que a sonda enxergou, na mesma frase dos dois
     *     sentidos
     */
    public static void transition(UUID colony, ColonyState from, ColonyState to, String sawWhat) {
        FLIPS.merge(colony, 1, Integer::sum);

        if (to == ColonyState.ABANDONED) {
            VillageColonyMod.LOGGER.warn(
                    "Colony {} is now ABANDONED — was {}, probe found no village ({})",
                    colony,
                    from,
                    sawWhat);

            return;
        }

        VillageColonyMod.LOGGER.info(
                "Colony {} is inhabited again — was {}, now {} ({})", colony, from, to, sawWhat);
    }

    /**
     * A soma da sessão, ao parar o servidor.
     *
     * <p>Silêncio é a notícia boa: nenhuma colônia oscilou, e o E9 não
     * apareceu nesta sessão.
     */
    public static void report() {
        FLIPS.forEach(
                (colony, flips) -> {
                    if (flips < OSCILLATING) {
                        return;
                    }

                    VillageColonyMod.LOGGER.warn(
                            "E9 — colony {} changed state {} times this session. A village does"
                                    + " not end and restart that often: the mark is oscillating.",
                            colony,
                            flips);
                });
    }

    /** Esquece a soma. Chamado ao parar o servidor, depois do relatório. */
    public static void clearAll() {
        FLIPS.clear();
    }
}
