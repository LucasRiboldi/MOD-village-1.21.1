package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ResourceId;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A barreira de teste da Regra 28, e a única coisa que a implementa.
 *
 * <p><b>Regra 28, do autor em 2026-08-20, e ele a declarou provisória:</b>
 * enquanto o projeto não estiver formalmente acabado, a obra não espera
 * por peça que não esteja num baú da vila — hoje porta, baú, tronco
 * descascado, tocha e vidraça. O bloco é riscado, e a casa fica sem
 * ele.
 *
 * <p><b>A razão dela caducou em 2026-08-21.</b> A justificativa escrita
 * era "peças que dependem de cadeia que a colônia ainda não fecha". As
 * cadeias fecharam — o pastor tosquia, a mina traz carvão e ferro, a
 * fornalha funde, o fabricante descasca e monta. Hoje a colônia sabe
 * fazer <b>todas</b> elas.
 *
 * <p>Decisão do autor em 2026-08-21: a barreira <b>fica</b> até a
 * primeira sessão de jogo, porque nenhuma dessas cadeias rodou de
 * verdade e é ela que impede a sessão de morrer se um elo quebrar. Mas
 * ela deixa de ser silenciosa: cada peça riscada sai como {@code WARN}
 * dizendo qual cadeia deveria tê-la produzido, e a sessão termina com a
 * soma. Casa que sobe sem uma linha destas subiu com a cadeia inteira;
 * casa que sobe com elas diz, peça por peça, onde procurar.
 *
 * <p><b>Cama e lampião saíram da lista no mesmo dia</b>, e por outra
 * decisão: a Regra 21 morreu, e com ela a passagem que repunha mobília
 * depois. Riscar a cama deixaria a casa sem cama <b>para sempre</b>, e a
 * demanda de lã sumiria junto — a conta sai da obra aberta agora. Para
 * esses dois vale a Regra 27 pura: o construtor aguarda, e o
 * PatienceClock é quem impede a colônia de morrer esperando.
 *
 * <p><b>Para desligar:</b> apague esta classe e as três chamadas a ela
 * em {@link BuilderWork} e {@link
 * com.villagecolony.fabric.event.ServerLifecycleHandler}. A Regra 27
 * volta a valer sem exceção.
 */
public final class TestBarrier {

    /**
     * As cinco peças, e a cadeia que deveria ter posto cada uma no baú.
     *
     * <p>Ordenado: a chave é o sufixo do nome do bloco, e a primeira que
     * casar vence. {@code stripped_} é prefixo e está tratado à parte.
     */
    private static final Map<String, String> CHAINS = new LinkedHashMap<>();

    static {
        CHAINS.put("_door", "the manufacturer's planks");
        CHAINS.put("chest", "the manufacturer's planks");
        CHAINS.put("torch", "the miner's coal and the manufacturer's sticks");
        CHAINS.put("_pane", "the miner's sand and the smelter's glass");
    }

    /** Quantas vezes cada peça foi riscada nesta sessão. */
    private static final Map<String, Integer> SKIPPED = new LinkedHashMap<>();

    private TestBarrier() {
    }

    /**
     * Se este bloco é uma das sete peças que a barreira deixa passar.
     *
     * @return a cadeia que deveria tê-lo produzido, ou vazio se a
     *     barreira não cobre este bloco — e nesse caso vale a Regra 27,
     *     e o construtor aguarda. Cama e lampião caem aqui desde
     *     2026-08-21
     */
    public static Optional<String> chainFor(ResourceId block) {
        String name = block.path();

        if (name.startsWith("stripped_")) {
            return Optional.of("the manufacturer's stripping");
        }

        for (Map.Entry<String, String> chain : CHAINS.entrySet()) {
            if (name.endsWith(chain.getKey())) {
                return Optional.of(chain.getValue());
            }
        }

        return Optional.empty();
    }

    /**
     * Uma peça riscada, e o grito que a acompanha.
     *
     * <p>{@code WARN} de propósito: a linha existe para ser achada num
     * log de sessão de vinte minutos, e {@code INFO} some no meio do
     * relatório de ciclo.
     */
    public static void skip(UUID projectId, ResourceId block, String chain) {
        SKIPPED.merge(block.path(), 1, Integer::sum);

        VillageColonyMod.LOGGER.warn(
                "TEST BARRIER skipped {} in project {} — the colony should be able to make"
                        + " this by now ({}), so that chain did not deliver",
                block.path(),
                projectId,
                chain);
    }

    /**
     * A soma da sessão, uma linha só, ao parar o servidor.
     *
     * <p>Silêncio aqui é a notícia boa: nenhuma casa precisou da
     * barreira, e a Regra 28 pode sair.
     */
    public static void report() {
        if (SKIPPED.isEmpty()) {
            VillageColonyMod.LOGGER.info(
                    "TEST BARRIER covered for nothing this session — every piece came from the"
                            + " colony's own chests. Rule 28 can go.");

            return;
        }

        VillageColonyMod.LOGGER.warn("TEST BARRIER covered for {} this session:", total());

        SKIPPED.forEach(
                (block, count) ->
                        VillageColonyMod.LOGGER.warn(
                                "    {}x {} — {}",
                                count,
                                block,
                                chainFor(ResourceId.vanilla(block)).orElse("no chain")));
    }

    private static int total() {
        return SKIPPED.values().stream().mapToInt(Integer::intValue).sum();
    }

    /** Esquece a soma. Chamado ao parar o servidor, depois do relatório. */
    public static void clearAll() {
        SKIPPED.clear();
    }
}
