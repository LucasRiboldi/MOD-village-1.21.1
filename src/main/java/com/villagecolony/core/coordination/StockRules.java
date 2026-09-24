package com.villagecolony.core.coordination;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.resource.model.ResourceTally;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.type.Production;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Quanto converter e quanto guardar — separado de {@link ColonyGoals} em
 * 2026-09-24, quando ele passou de 500 linhas.
 *
 * <p>O {@code ColonyGoals} diz quanto de cada recurso a vila quer ter; estas
 * são as regras de estoque que ele usa para chegar lá: quantas toras virar
 * tábua, quanto minério bruto guardar antes de fundir, e quando um baú está
 * cheio a ponto de pedir ajuda. Os comentários vieram junto sem mudança.
 */
public final class StockRules {

    private StockRules() {
    }

    /**
     * Quantas tábuas saem de um tronco.
     *
     * <p>Convenção do jogo, e não conta desta classe: é o que a receita
     * de tábua rende, e o Core não pode perguntar ao livro de receitas
     * (ADR-005). Está aqui porque a reserva de tronco precisa comparar
     * duas medidas da mesma madeira — o que está em tora e o que já
     * virou tábua —, e sem um câmbio entre elas a comparação não existe.
     *
     * <p>Se alguma versão do jogo mudar o rendimento, o que sai errado é
     * a proporção da reserva, e não a colônia: o pior caso é ela guardar
     * tronco a mais.
     */
    public static final int PLANKS_PER_LOG = 4;

    /**
     * Quantas toras ainda podem virar tábua sem furar a reserva.
     *
     * <p><b>A regra do autor</b>, 2026-09-05: <i>"converter somente
     * aproximadamente metade do estoque de troncos em tábuas e preservar
     * o restante como troncos"</i>. Vinte toras pedem dez conversões, e
     * as outras dez ficam — os {@code stripped_oak_log} da casa de
     * planície saem de tora, e não de tábua.
     *
     * <p><b>Mora aqui, e é chamada de dois lugares</b> — a meta, logo
     * abaixo, e o fabricante, que executa. Os dois têm de dizer o mesmo:
     * uma meta que parasse de pedir com um executor que continuasse
     * moendo seria a reserva existindo só no papel, que foi o defeito
     * original com outro nome. É o mesmo argumento que
     * {@code ColonySupply.canProvide} escreve para o par dele.
     *
     * <p>Zero quando a colônia já tem tábua bastante: {@code storedPlanks}
     * é convertido de volta a toras equivalentes, e o que se compara são
     * duas medidas da mesma madeira.
     *
     * @param logs quantas toras a colônia guarda, de qualquer espécie
     * @param storedPlanks quantas tábuas ela guarda, de qualquer espécie
     */
    public static int logsToConvert(int logs, int storedPlanks) {
        return Math.max(0, (logs - storedPlanks / PLANKS_PER_LOG) / 2);
    }

    /**
     * Tudo o que sai de fornalha, pela produção declarada.
     *
     * <p>Pela {@link Production}, e não por uma lista de nomes — ADR-009.
     * Material novo que saia de fornalha entra sozinho no "um pouco de
     * cada", sem uma linha de código a mais.
     */
    static List<ResourceType> everythingTheFurnaceMakes() {
        List<ResourceType> made = new ArrayList<>();

        for (ResourceType type : ResourceType.values()) {
            if (type.production() != Production.SMELTED) {
                continue;
            }

            // <b>O vidro e o ferro ficam de fora</b>, e a bateria cobrou
            // isso: os dois já têm meta própria, e ela puxa o cru por
            // trás — a de vidro abre meta de <b>areia</b> pelo que falta,
            // a de ferro abre meta de minério. Pôr piso neles fazia a
            // colônia pedir areia sem obra nenhuma querendo vidraça, e
            // cinco testes de {@code ColonyGoalsTest} disseram isso na
            // primeira tentativa.
            //
            // O pedido do autor é sobre <b>bloco de construção</b> —
            // <i>"um pouco de cada para ter todos tipos de blocos"</i> —,
            // e vidro e ferro não são blocos de parede: são peça de
            // janela e de ferramenta, com cadeia própria.
            if (type == ResourceType.GLASS || type == ResourceType.IRON_INGOT) {
                continue;
            }

            made.add(type);
        }

        return made;
    }

    /**
     * Quanto do cru fica sem ser assado, para o pedreiro lavrar.
     *
     * <p><b>Decisão do autor por simetria com a madeira</b>, 2026-09-19.
     * A regra de 09-05 manda <i>"converter somente aproximadamente
     * metade do estoque de troncos em tábuas e preservar o restante como
     * troncos"</i>, e a pedra não tinha equivalente.
     *
     * <p><b>O que isto conserta, medido na sessão de 17:15.</b> A obra
     * parou <b>39 vezes</b> esperando {@code cut_sandstone}, e o baú da
     * colônia tinha <b>139 arenitos LISOS e zero arenito cru</b>. O
     * fundidor assou o estoque inteiro; o arenito cortado sai do
     * <b>cru</b>, e não sobrou nenhum. É o mesmo defeito que a reserva de
     * tronco corrigiu do lado da madeira, com outro material.
     *
     * <p>Metade, como a madeira: a proporção não se move, e o ponto de
     * equilíbrio é o que o autor chamou de metade.
     */
    public static int rawToKeep(int raw, int processed) {
        // <b>A reserva é metade do TOTAL, e não metade da diferença</b> —
        // e esta linha saiu errada na primeira versão, com o próprio
        // teste a cobrando. Com {@code (raw - processed) / 2}, um
        // estoque empatado — 40 crus e 40 processados — reservava ZERO e
        // liberava os 40 para a fornalha, que é o oposto de equilibrar.
        //
        // Contando o total, o ponto de equilíbrio fica parado: 40 e 40
        // somam 80, metade é 40, e é exatamente o que já está cru. Não
        // se assa mais nada, que é o que a regra quer dizer.
        int both = raw + processed;

        return Math.max(0, Math.min(raw, both / 2));
    }

    /**
     * Quanto do cru ainda pode ir à fornalha sem furar a reserva.
     *
     * <p>Zero quando o processado já empata o cru: aí a colônia tem
     * tanto de um quanto do outro, e assar mais desequilibraria para o
     * lado que já está servido.
     */
    public static int rawThatMayBeSmelted(int raw, int processed) {
        return Math.max(0, raw - rawToKeep(raw, processed));
    }

    /**
     * A partir de quanto cheio o baú chama as outras profissões.
     *
     * <p><b>Decisão do autor, 2026-09-19:</b> <i>"quando o baú da
     * profissão passar da metade do preenchimento, as outras profissões
     * devem forçar a criação de itens que futuramente serão utilizados
     * para criar as estruturas do bioma"</i>.
     */
    public static final int CHEST_HALF_FULL = 50;

    /**
     * Se o baú desta profissão já pede ajuda das outras.
     *
     * <p><b>O que isto conserta, medido em 2026-09-19 12:09→12:28.</b> O
     * baú do mineiro encheu e ele jogou <b>660 itens no chão</b> — 365
     * deles {@code sandstone}, que era <b>exatamente</b> o que a obra
     * esperava em {@code WAITING_RESOURCES}. Um baú cheio, vinte livres
     * ao lado, e 318 de 323 blocos por pôr.
     *
     * <p><b>Por que puxar em vez de transbordar.</b> Transbordar para o
     * baú do vizinho resolveria o sintoma e adiaria o problema: o baú
     * seguinte enche também, e a colônia acumula matéria bruta que
     * ninguém converteu. Puxar ataca a causa — a matéria vira
     * <b>peça de construção</b>, o baú esvazia, e a obra encontra pronto
     * o que ia esperar.
     *
     * <p>É a mesma forma do {@link #logsToConvert}, que é regra do autor
     * de 09-05: converter o excedente em vez de guardá-lo. A diferença é
     * o gatilho — lá é a proporção entre tora e tábua, aqui é o
     * <b>espaço</b> acabando.
     *
     * @param percentFull quão cheio o baú está, de 0 a 100. A medida é da
     *     camada fabric, que é quem sabe ler inventário — o Core não
     *     conhece {@code ChestBlockEntity} (ADR-005)
     */
    public static boolean chestCallsForHelp(int percentFull) {
        return percentFull > CHEST_HALF_FULL;
    }
}
