package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ResourceId;

import java.util.Comparator;

/**
 * Em que ordem a casa sobe — 2026-09-16.
 *
 * <p>A ordem é do <b>projeto</b>, e não do arquivo do jogo: o arquivo não
 * promete ordem nenhuma, e um construtor que seguisse a dele poria o
 * telhado antes da parede.
 *
 * <p><b>Três grupos, nesta ordem:</b>
 *
 * <ol>
 *   <li><b>Estrutura</b> — parede, piso, teto. De baixo para cima, para
 *       que cada bloco tenha o que o sustente;
 *   <li><b>Porta</b> — pedido do autor, 2026-09-16: <i>"deixar para
 *       colocar as portas por último nas construções"</i>. Ver
 *       {@link #isDoor};
 *   <li><b>Mobília</b> — a Regra 32, de 2026-08-29. A cama e a tocha
 *       dependem do que está <b>ao lado</b>, e só a casa inteira responde
 *       isso.
 * </ol>
 *
 * <p><b>Por que a porta ganhou grupo próprio.</b> Ela fica na base da
 * parede, então a ordem de baixo para cima a colocava <b>primeiro</b> —
 * antes de existir batente para segurá-la. E o construtor <b>risca</b> o
 * que não tem apoio, com a linha <i>"skips ... nothing holds it"</i>: a
 * porta não era adiada, era perdida, e a casa terminava sem ela.
 *
 * <p>É o mesmo defeito de ordem que a mobília teve, e a correção é a
 * mesma — só que a porta é estrutura, não mobília: ela precisa vir antes
 * da cama, senão a cabeceira seria decidida contra um vão de porta que
 * ainda não está lá.
 *
 * <p>Mora no Core porque é <b>decisão</b>, e decisão se afirma sem mundo.
 * Estava dentro do leitor de estrutura, que precisa de servidor — e por
 * isso a ordem nunca teve teste próprio.
 */
public final class BuildOrder {

    /**
     * A ordem de assentamento.
     *
     * <p>Dentro do mesmo grupo e da mesma altura, por x e depois por z:
     * duas leituras do mesmo arquivo têm de dar exatamente a mesma casa,
     * porque obra com ordem instável é impossível de depurar.
     */
    public static final Comparator<BlueprintBlock> COMPARATOR = Comparator
            .comparingInt(BuildOrder::groupOf)
            .thenComparingInt(block -> block.offset().y())
            .thenComparingInt(block -> block.offset().x())
            .thenComparingInt(block -> block.offset().z());

    private BuildOrder() {
    }

    /** Estrutura, porta, mobília — nesta ordem. */
    private static int groupOf(BlueprintBlock block) {
        if (block.furniture()) {
            return 2;
        }

        return isDoor(block.block()) ? 1 : 0;
    }

    /**
     * Se este bloco é porta, alçapão ou portão.
     *
     * <p>Os três dependem de apoio que a estrutura em volta fornece, e os
     * três são riscados quando tentados cedo demais.
     *
     * <p>Pelo nome, e não por uma lista fechada: o jogo tem porta de nove
     * madeiras, de ferro e de bambu, e cada versão acrescenta mais. Uma
     * lista fixa envelheceria calada — e o preço seria a casa sem porta,
     * que é justamente o que se está consertando.
     */
    public static boolean isDoor(ResourceId block) {
        String path = block.path();

        return path.endsWith("_door")
                || path.endsWith("_trapdoor")
                || path.endsWith("_fence_gate");
    }
}
