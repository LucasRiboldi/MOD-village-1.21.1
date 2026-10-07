package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.coordination.IdleReason;
import com.villagecolony.fabric.integration.BuildSiteScanner;
import com.villagecolony.fabric.integration.RoadExtension;
import com.villagecolony.fabric.integration.SweepLog;
import com.villagecolony.fabric.integration.SweepState;
import net.minecraft.server.world.ServerWorld;

import java.util.List;
import java.util.Optional;

/**
 * A busca do lote da próxima obra, e a rua que cresce quando ela termina sem
 * lote — o fim de {@link ConstructionPlanner#plan}, separado em 2026-10-02.
 *
 * <p>Separado para ter dois chamadores com a mesma regra: o ciclo de 30 s,
 * depois da obra aberta e do reparo, e a passagem extra da vila em foco
 * ({@link SweepCadence}), que só continua uma varredura já em curso. Uma
 * cópia do trecho deixaria as duas acharem lotes diferentes — e a Regra 15
 * (a rua cresce quando a volta termina sem lote) precisa valer nas duas.
 * O texto e os comentários vieram de {@code ConstructionPlanner} sem mudança.
 */
final class LotSeeking {

    private LotSeeking() {
    }

    static Optional<ConstructionProject> seek(ServerWorld world, Colony colony, int builders) {
        // A seleção é global, mas a descoberta do lote continua única: a
        // família escolhida aqui passa pelo mesmo scanner e pelas mesmas
        // recusas físicas, seja casa, roça, cercado ou templo.
        //
        // A ordem permanente é casa -> tipo A -> casa -> tipo B. O tipo B
        // não repete o último tipo não residencial concluído; a decisão
        // deriva do registro de construções, sem estado paralelo.
        List<Blueprint> plans = HousePlans.plansForNext(world, colony);

        if (plans.isEmpty()) {
            return ConstructionPlanner.silent(
                    colony,
                    IdleReason.NOT_IN_GAME,
                    "this game has no buildable village structure for the "
                            + colony.id() + " style");
        }

        Blueprint blueprint = plans.get(0);

        // <b>Roça não sai atrás da estrada</b> — 2026-09-05, visto em
        // jogo. A primeira roça da colônia nasceu em {x=1517, z=113} com
        // o centro em {x=1435, z=47}: <b>105 blocos</b>, porque o lote
        // veio da ponta da estrada que a vila estava esticando. O
        // fazendeiro procura lavoura a 32 do centro, então ele nunca a
        // veria — e a linha do log logo depois de ela ficar pronta era
        // exatamente "no empty plot within 32 blocks of the village".
        //
        // Pior: o pedido de roça nunca se fechava, e a colônia levantou
        // <b>duas</b> em quatro minutos, a caminho de encher o mapa.
        //
        // A extensão de rua existe para a vila <b>crescer</b>, e casa
        // nova na ponta é o que ela quer. Roça é o contrário: o autor
        // pediu "um espaço livre <b>dentro da vila</b>", e a varredura em
        // anéis a partir do centro já devolve o lote livre mais perto.
        boolean farming = FarmPlans.isFarm(blueprint.id());

        // A ponta que já rendeu continua rendendo, e isso vem antes da
        // varredura — 2026-08-26. Sem isto a colônia pagava dezessete
        // ciclos por bloco de rua, e a sessão das 03:11 mediu o custo:
        // um bloco calçado, e o lote de sete por sete continuou sem
        // caber. Quem autorizou a rua a crescer foi a varredura que
        // terminou sem lote, e essa autorização vale para o trecho.
        if (!farming && RoadExtension.isGrowing(colony.id())) {
            Optional<ConstructionProject> onTheStretch =
                    RoadGrowthPlanning.keepGrowing(world, colony, blueprint, plans, builders);

            if (onTheStretch.isPresent() || RoadExtension.isGrowing(colony.id())) {
                // Ou nasceu lote no trecho novo, ou a passagem foi gasta
                // crescendo. Nos dois casos a varredura espera.
                return onTheStretch;
            }

            // A ponta parou de render. A varredura volta a mandar, e
            // nesta mesma passagem.
        }

        // O denominador da conta do SweepLog: quantas vezes o planejador
        // chegou a pedir lote. Obra já aberta, falta de construtor e
        // planta ausente saem antes daqui e não são dívida da varredura.
        SweepLog.asked(colony.id());

        Optional<BuildSiteScanner.Site> site = BuildSiteScanner.findForFootprints(
                world, colony.id(), colony.center(), ConstructionPlanner.searchRadius(colony),
                SiteOpening.footprintsFor(plans));

        if (site.isEmpty()) {
            // Duas respostas, e a diferença importa: uma diz que não há
            // lote, a outra diz que ninguém terminou de olhar. A linha
            // anterior dizia a primeira nos dois casos, e no segundo isso
            // era mentira — ver o E14 do §17.
            //
            // Sem o número do anel de propósito: IdleLog só registra
            // quando o motivo muda, e um anel diferente por ciclo faria a
            // linha voltar toda vez.
            // Os dois jeitos de procurar contam — 2026-09-11. Esta
            // linha perguntava só pelo cursor do quadrado, e desde que a
            // volta pelo índice de ruas também pode parar no meio, ela
            // deixaria a Regra 15 crescer a rua sem ninguém ter visto o
            // raio inteiro. Ver SweepState.stillLookingForALot.
            if (SweepState.stillLookingForALot(colony.id())) {
                return ConstructionPlanner.silent(colony, IdleReason.SWEEP_INCOMPLETE, "looking for a lot");
            }

            // A Regra 15, e é aqui que ela cabe: a varredura terminou o
            // raio inteiro e não há beira de rua livre. Antes desta
            // linha a vila parava para sempre — a rua era algo que a
            // colônia encontrava, e nunca algo que ela produzia.
            //
            // O lote novo não nasce nesta passagem de propósito: a
            // varredura seguinte é que vai encontrá-lo, e ela recomeça
            // do centro no ciclo que vem. Trinta segundos, e a ordem da
            // regra fica respeitada — estrada primeiro, casa depois.
            return RoadGrowthPlanning.extendTheRoad(world, colony, blueprint, plans, builders);
        }

        // A recusa de lote sobre casa da colônia morava aqui, e daqui não
        // funcionava. O comentário dizia "a próxima passagem tenta outro
        // anel", e era falso: achar um lote apaga o cursor da varredura,
        // então a passagem seguinte recomeçava do centro e reencontrava o
        // mesmo lugar. Em 2026-08-20 a vila do autor ficou nesse laço.
        //
        // A pergunta desceu para `LotClearance.isClearAbove`, que é
        // onde a varredura ainda pode seguir para o anel seguinte.

        if (farming && !ConstructionDemand.withinTheFarmersReach(colony, site.get())) {
            // Lote livre, mas longe demais: o fazendeiro procura lavoura
            // a FarmerWork.reach() do centro, e roça que ele não vê é
            // roça que ninguém planta — e que não fecha o pedido, então
            // a colônia levantaria outra, e outra.
            //
            // <b>E a recusa não pode parar a vila</b> — 2026-09-09. Ela
            // encerrava a passagem inteira, e como o lote de amanhã é o
            // mesmo de hoje, a colônia repetia a recusa para sempre sem
            // nunca tentar uma casa: uma hora de jogo, 108 passagens do
            // planejador, nenhuma obra aberta, e o autor sem ver
            // trabalhador nenhum trabalhando. A roça cede a vez por
            // vinte ciclos e as casas passam — ver FarmPlans.postponed.
            FarmPlans.postpone(colony.id(), world.getTime());

            return ConstructionPlanner.silent(
                    colony,
                    IdleReason.NO_TARGET,
                    "the only free lot is outside the farmer's reach"
                            + " — the houses go first for now");
        }

        return SiteOpening.open(world, colony, site.get(), plans, blueprint, builders);
    }
}
