package com.villagecolony.core.coordination;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.resource.model.ResourceTally;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.type.ResourceType;

import java.util.Map;
import java.util.Objects;

/**
 * Quanto a colônia quer ter de cada recurso.
 *
 * <p>Decisão do autor em 2026-08-08, Regra 1: a colônia colhe até os
 * baús encherem. A meta deixou de ser um número escrito aqui e passou a
 * ser uma propriedade do mundo — o espaço que a colônia tem para
 * guardar.
 *
 * <p>A conta é uma só:
 *
 * <pre>{@code
 * meta = o que já está guardado + o que ainda cabe
 * }</pre>
 *
 * <p>e o déficit que {@code ResourceDemand} tira dela é exatamente o
 * espaço livre. Baú cheio dá meta igual ao estoque, déficit zero e
 * nenhuma tarefa nova — que é o que a regra pede, e o que faz a fila
 * parar de crescer para sempre. Ver §17, erro E1.
 *
 * <p>O número fixo anterior — 64 de madeira e 32 de pedra — foi o que
 * gerou o E1: uma meta constante que o lenhador demorava a atingir fazia
 * a colônia pedir de novo a cada ciclo.
 *
 * <p><b>A tábua entrou na Fase 9</b>, pela Regra 5 do §18, e com outra
 * conta: metade do que os baús comportam. A conta da Regra 1 não serve
 * para ela — um tronco vira quatro tábuas, então fabricar aumenta o
 * volume guardado, e "fabricar até encher" transformaria toda a madeira
 * da colônia em tábua e pararia a coleta junto.
 *
 * <p><b>A pedra saiu.</b> Ninguém minera no MVP, e
 * {@code ColonyCycle.typeFor} manda todo recurso NATURAL para
 * {@code COLLECT_WOOD}: a meta de pedra virava uma tarefa de coleta que
 * só o lenhador podia pegar, e ele derrubava árvore para atendê-la.
 * Volta quando existir minerador, e aí com o espaço dos baús pela mesma
 * conta.
 */
public final class ColonyGoals {

    private ColonyGoals() {
    }

    /**
     * A meta desta colônia agora.
     *
     * <p>Recebe estoque e espaço porque os dois mudam a cada ciclo: o
     * jogador tira madeira do baú, quebra um baú, constrói outro. Uma
     * meta guardada envelheceria sem que nada avisasse.
     *
     * @param owned o que a colônia tem, tipicamente o total de
     *     {@code ColonyResources}. Contagem parcial produz meta a menos —
     *     quem chama já recusa decidir sobre leitura incompleta
     * @param woodRoom quantos troncos ainda cabem nos baús da colônia,
     *     medido na camada fabric. "Baús da colônia" são os baús dos
     *     trabalhadores registrados, que é o que {@code StorageRegistry}
     *     conhece; baú comunitário não existe ainda
     */
    public static Map<ResourceType, Integer> of(
            Colony colony, ResourceTally owned, int woodRoom) {

        return of(colony, owned, woodRoom, 0);
    }

    /**
     * @param plankRoom quantas tábuas ainda cabem nos baús da colônia.
     *     Entra na Regra 5 — ver §18 —, que responde "quanto fabricar":
     *     a meta é a da obra e, enquanto não houver obra, metade do que
     *     os baús comportam em tábua.
     *
     *     <p>Metade porque a resposta da Regra 1 não serve aqui: um
     *     tronco vira quatro tábuas, então fabricar aumenta o volume
     *     guardado. "Fabricar até encher" transformaria toda a madeira da
     *     colônia em tábua e pararia a coleta junto, porque é o baú cheio
     *     que faz o lenhador parar.
     */
    public static Map<ResourceType, Integer> of(
            Colony colony, ResourceTally owned, int woodRoom, int plankRoom) {

        Objects.requireNonNull(colony, "colony");
        Objects.requireNonNull(owned, "owned");

        if (woodRoom < 0) {
            throw new IllegalArgumentException("Negative storage room: " + woodRoom);
        }

        if (plankRoom < 0) {
            throw new IllegalArgumentException("Negative plank room: " + plankRoom);
        }

        // OAK_LOG responde pelo grupo inteiro: ResourceDemand compara a
        // meta de uma madeira com a soma de todas. Sessenta e quatro
        // troncos de abeto satisfazem esta linha tanto quanto os de
        // carvalho. Ver ResourceGroup e ResourceDemand.deficit.
        int wood = owned.amountOfGroup(ResourceGroup.WOOD) + woodRoom;

        int storedPlanks = owned.amountOfGroup(ResourceGroup.PLANKS);

        // A capacidade em tábua é o que já está guardado mais o que ainda
        // cabe. A meta é metade dela, e é isso que converge: cada peça
        // feita sobe o guardado e desce o que cabe, até os dois se
        // encontrarem na metade.
        //
        // Sem tronco guardado, porém, a meta é o que já se tem: não se
        // pede o que não há com que fazer. Sem esta linha, uma colônia
        // sem madeira abriria tarefa de fabricação a cada ciclo para o
        // fabricante encerrá-la no tick seguinte, por falta de material
        // — trabalho nenhum e uma linha de log por ciclo, que é o E1
        // voltando por outra porta.
        int planks = owned.amountOfGroup(ResourceGroup.WOOD) == 0
                ? storedPlanks
                : (storedPlanks + plankRoom) / 2;

        return Map.of(
                ResourceType.OAK_LOG, wood,
                ResourceType.OAK_PLANKS, planks);
    }
}
