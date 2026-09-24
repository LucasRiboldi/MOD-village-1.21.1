package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.coordination.ColonyGoals;
import com.villagecolony.core.coordination.StockRules;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ColonyChests;

import net.minecraft.server.world.ServerWorld;

import java.util.List;

/**
 * O baú que enche chama as outras profissões — 2026-09-19.
 *
 * <p><b>Decisão do autor</b>, e ela é melhor que as três opções que eu
 * tinha levantado: <i>"quando o baú da profissão passar da metade do
 * preenchimento, as outras profissões devem forçar a criação de itens que
 * futuramente serão utilizados para criar as estruturas do bioma,
 * ajudando assim aos baús das profissões não ficarem cheios rapidamente e
 * adiantando a construção de blocos necessários"</i>.
 *
 * <p><b>O que isto conserta, medido na sessão de 12:09→12:28.</b> O baú do
 * mineiro encheu — <b>uma</b> vez, com vinte baús livres ao lado — e ele
 * jogou <b>660 itens no chão</b>, dos quais <b>365 {@code sandstone}</b>.
 * Ao mesmo tempo, a obra estava parada em {@code WAITING_RESOURCES}
 * esperando <b>{@code sandstone}</b>, com 318 de 323 blocos por pôr. O
 * material que faltava era destruído a poucos blocos de quem o esperava.
 *
 * <p><b>Por que puxar, e não transbordar.</b> Transbordar para o baú do
 * vizinho — que era a minha proposta — resolve o sintoma e adia a causa: o
 * baú seguinte enche também, e a colônia passa a acumular matéria bruta
 * que ninguém converteu. Puxar ataca a causa: a matéria vira <b>peça de
 * construção</b>, o baú esvazia porque quatro areias viram um arenito, e a
 * obra encontra pronto o que ia esperar. Um movimento resolve os dois
 * lados.
 *
 * <p><b>A demanda hoje só nasce de obra aberta</b>, e é essa a lacuna.
 * {@code WorkMaterials.stone} pergunta ao
 * {@code ConstructionPlanner.materialNeededBy}, que lê a obra <b>aberta</b>
 * da colônia; sem obra — ou com ela parada esperando — a demanda é zero e
 * ninguém produz nada. A colônia só começa a fabricar depois que a obra
 * pede, que é tarde demais.
 *
 * <p>É a mesma forma do {@code StockRules.logsToConvert}, regra do autor
 * de 09-05: converter o excedente em vez de guardá-lo. A diferença é o
 * gatilho — lá é a proporção entre tora e tábua, aqui é o <b>espaço</b>
 * acabando.
 */
public final class ChestRelief {

    /**
     * Quanto pedir da peça do bioma quando o baú aperta.
     *
     * <p>Uma pilha. É o bastante para a conversão começar e drenar o
     * excedente, e pequeno o bastante para não virar a colônia inteira
     * fabricando pedra que ninguém pediu — a meta real continua sendo a
     * da obra, que manda quando existe.
     */
    public static final int RELIEF_BATCH = 64;

    private ChestRelief() {
    }

    /**
     * Se algum baú desta colônia passou da metade.
     *
     * <p>Basta <b>um</b>: o aperto é local, e foi um único baú cheio que
     * parou a vila inteira na sessão de 12:09. Esperar a média subir
     * seria esperar o estrago se espalhar.
     */
    public static boolean anyChestIsTight(ServerWorld world, Colony colony) {
        List<ColonyPos> chests =
                ColonyChests.nearestFirst(world, colony.id(), colony.center());

        for (ColonyPos chest : chests) {
            if (StockRules.chestCallsForHelp(ChestDepositor.howFull(world, chest))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Quanto da pedra do bioma pedir, somando obra e alívio.
     *
     * <p>A obra manda quando existe — o que ela pede é o que se produz. O
     * alívio só entra quando a obra <b>não</b> está pedindo e algum baú
     * apertou; é aí que a colônia estava parada olhando o baú encher.
     *
     * @param forWork quanto a obra aberta ainda pede desta pedra
     */
    public static int stoneToAskFor(ServerWorld world, Colony colony, int forWork) {
        if (forWork > 0) {
            return forWork;
        }

        if (!anyChestIsTight(world, colony)) {
            return 0;
        }

        VillageColonyMod.LOGGER.info(
                "Colony {} — a chest is over half full and no build is asking;"
                        + " pulling {} of the biome's stone forward",
                colony.id().toString().substring(0, 8),
                RELIEF_BATCH);

        return RELIEF_BATCH;
    }
}
