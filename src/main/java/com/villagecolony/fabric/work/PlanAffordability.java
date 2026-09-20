package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.type.ResourceId;

import java.util.Comparator;
import java.util.List;

/**
 * A planta que a colônia consegue terminar vem primeiro — 2026-09-19.
 *
 * <p><b>A alternativa, se o viveiro não bastar.</b> A vila de deserto de
 * 19:29 parou a uma peça do fim esperando {@code fletching_table}, que é
 * de tábua, num bioma <b>sem floresta ao alcance</b>: o lenhador passou a
 * sessão inteira em {@code looking for a tree} e nenhuma árvore caiu. O
 * fazendeiro planta mudas desde 09-19, mas madeira leva tempo de jogo — e
 * até lá a colônia escolhe a casa do ferreiro de novo, e para de novo.
 *
 * <p><b>O que isto muda, e o que NÃO muda.</b> Não proíbe planta nenhuma:
 * a Regra 27 continua valendo e o catálogo inteiro continua disponível. O
 * que muda é a <b>ordem do sorteio</b> — entre duas plantas que cabem no
 * mesmo lote, a que pede menos do que falta vem primeiro.
 *
 * <p>É o mesmo princípio da Regra 25, que já manda a maior primeiro: a
 * escolha entre plantas equivalentes é do projeto, e aqui ela passa a
 * olhar o estoque além da pegada.
 *
 * <p><b>Por que ordenar em vez de filtrar.</b> Filtrar travaria a vila
 * quando <b>toda</b> planta pedisse madeira — e é o caso do deserto, cujas
 * casas todas têm porta. Ordenar degrada com elegância: sem madeira
 * nenhuma, a colônia ainda constrói, só começa pela casa que menos depende
 * dela. E quando a madeira chegar, a ordem volta a não importar.
 */
public final class PlanAffordability {

    /** Os materiais que a colônia pode não alcançar neste bioma. */
    private static final List<String> HARD_TO_GET = List.of("_planks", "_log", "_wood");

    private PlanAffordability() {
    }

    /**
     * Quantos blocos desta planta dependem do material escasso.
     *
     * <p>Conta bloco, e não tipo: uma casa com dezesseis vigas depende
     * mais de madeira que uma com uma porta, e é essa diferença que
     * decide qual começar.
     */
    public static int scarceBlocksIn(Blueprint plan) {
        int scarce = 0;

        for (BlueprintBlock block : plan.blocks()) {
            if (isScarce(block.block())) {
                scarce++;
            }
        }

        return scarce;
    }

    /** Se este material é de uma família que o bioma pode não ter. */
    static boolean isScarce(ResourceId material) {
        String path = material.path();

        for (String mark : HARD_TO_GET) {
            if (path.endsWith(mark)) {
                return true;
            }
        }

        return false;
    }

    /**
     * As plantas do sorteio, da mais barata para a mais cara em escasso.
     *
     * <p>Estável: plantas com a mesma conta ficam na ordem em que
     * chegaram, e a Regra 25 continua mandando dentro do empate.
     *
     * <p>Devolve a <b>mesma lista</b> quando nenhuma planta pede material
     * escasso, para o caso comum não pagar cópia nenhuma.
     */
    public static List<Blueprint> cheapestFirst(List<Blueprint> plans) {
        boolean anyScarce = plans.stream().anyMatch(plan -> scarceBlocksIn(plan) > 0);

        if (!anyScarce) {
            return plans;
        }

        return plans.stream()
                .sorted(Comparator.comparingInt(PlanAffordability::scarceBlocksIn))
                .toList();
    }
}
