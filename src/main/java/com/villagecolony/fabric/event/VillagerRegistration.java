package com.villagecolony.fabric.event;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.ClusterRejection;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.coordination.IdleReason;
import com.villagecolony.core.colony.model.ColonyLifecycle;
import com.villagecolony.core.colony.model.ColonyState;
import com.villagecolony.core.colony.model.VillageCandidate;
import com.villagecolony.core.colony.service.ColonyAbandonment;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.construction.model.VillagePalette;
import com.villagecolony.core.coordination.ColonyCycle;
import com.villagecolony.core.coordination.ColonyGoals;
import com.villagecolony.core.coordination.WorkDemand;
import com.villagecolony.core.resource.model.ColonyResources;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.core.worker.service.HiringLog;
import com.villagecolony.core.worker.service.ProfessionAssigner;
import com.villagecolony.core.worker.service.VacancyEnforcer;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.fabric.integration.ChestInventoryReader;
import com.villagecolony.fabric.integration.ChestMarker;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.SiteMarker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.VillageBiomes;
import com.villagecolony.fabric.integration.VillageScanner;
import com.villagecolony.fabric.integration.VillageFoundation;
import com.villagecolony.fabric.integration.VanillaBedChests;
import com.villagecolony.fabric.integration.BigHouseFoundation;
import com.villagecolony.fabric.integration.VillagerScanner;
import com.villagecolony.fabric.integration.WorkerEquipment;
import com.villagecolony.fabric.integration.WorkerNameplate;
import com.villagecolony.fabric.work.IdleLog;
import com.villagecolony.fabric.work.MinerWork;
import com.villagecolony.fabric.work.FarmerWork;
import com.villagecolony.fabric.work.ShepherdWork;
import com.villagecolony.fabric.work.SmelterWork;
import com.villagecolony.fabric.work.SurfaceGatheringWork;
import com.villagecolony.fabric.work.WaitingWork;
import com.villagecolony.fabric.work.ChestRelief;
import com.villagecolony.fabric.work.WorkMaterials;
import com.villagecolony.fabric.work.HousePlans;
import com.villagecolony.fabric.work.LumberjackWork;
import com.villagecolony.fabric.work.BuilderWork;
import com.villagecolony.fabric.work.StrandedEscape;
import com.villagecolony.fabric.work.VillageMeals;
import com.villagecolony.fabric.work.ConstructionDemand;
import com.villagecolony.fabric.work.ConstructionPlanner;
import com.villagecolony.fabric.work.CraftingWork;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.poi.PointOfInterestStorage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.world.poi.PointOfInterestTypes;

/**
 * O registro dos aldeões de uma colônia: quem entra, quem perde a vaga para quem tem baú, e o que os baús guardam — separado de
 * {@link VillageDetectionHandler} em 2026-09-24, quando ele passou de 500
 * linhas. Os comentários vieram junto sem mudança.
 */
final class VillagerRegistration {

    private VillagerRegistration() {
    }

    /**
     * Registra os aldeões da colônia como trabalhadores e dá função a
     * quem não tem.
     *
     * <p>Registro e atribuição são passos separados de propósito: um
     * trabalhador vindo do save já chega com função, e a atribuição não
     * pode desfazê-la. Ver TASK-012b e Worker#assign.
     *
     * <p>A atribuição roda mesmo quando nada foi registrado agora: um
     * save anterior à TASK-012b traz trabalhadores sem função, e eles
     * precisam recebê-la sem depender de um aldeão novo aparecer.
     *
     * <p>Só produz linha de log quando algo muda. Reencontrar os mesmos
     * aldeões a cada ciclo é o caso comum e deve ser silencioso.
     */
    static void registerVillagers(ServerWorld world, Colony colony, ColonyPos around) {
        VillagerScanner.ScanResult result = VillagerScanner.scan(
                world, colony, around, VillageColonyMod.WORKERS, VillageColonyMod.STORAGES);

        if (result.registeredWorkers() > 0) {
            VillageColonyMod.LOGGER.info(
                    "Registered {} villagers in colony {} ({} total)",
                    result.registeredWorkers(),
                    colony.id(),
                    VillageColonyMod.WORKERS.countOfColony(colony.id()));
        }

        if (result.registeredStorages() > 0) {
            VillageColonyMod.LOGGER.info(
                    "Registered {} storages in colony {} ({} total)",
                    result.registeredStorages(),
                    colony.id(),
                    VillageColonyMod.STORAGES.count());

            logResources(
                    colony,
                    ChestInventoryReader.survey(
                            world,
                            ColonyChests.nearestFirst(world, colony.id(), colony.center())));
        }

        // Antes de atribuir: um save anterior a 2026-08-12 chega com
        // seis lenhadores gravados, e uma regra que só valesse para
        // aldeão novo nunca os desfaria.
        //
        // E é aqui que o trabalhador sem baú perde a vaga para quem
        // consegue um — a atribuição não o alcança, porque ele já tem
        // função.
        // Baús distintos, e não candidatos: dois aldeões do mesmo cômodo
        // enxergam o mesmo baú, e dispensar um trabalhador por candidato
        // trocava a vaga por alguém que também ficaria sem. É a decisão do
        // autor de 2026-08-15 — só se dispensa quando há baú livre de
        // verdade para o substituto. Ver o E11 do §17.
        // Quantas trocas cabem, e não quantos baús sobram — 2026-08-21.
        // Uma troca sem substituto esvazia uma função e não a preenche de
        // volta, e isso é a Regra 11 quebrando. Ver ScanResult.substitutes.
        dismissExtraWorkers(world, colony, result.substitutes());

        // A vaga vai primeiro para quem consegue baú: sem isso ela podia
        // ir para uma cama que não alcança baú nenhum, e o trabalhador
        // passava a sessão pegando a tarefa e devolvendo à fila.
        int assigned = ProfessionAssigner.assignMissing(
                VillageColonyMod.WORKERS,
                colony.id(),
                result.employable(),
                result.adultPopulation(),
                result.equippable()::contains);

        if (assigned > 0) {
            VillageColonyMod.LOGGER.info(
                    "Assigned {} professions in colony {}", assigned, colony.id());
        }

        // <b>E por que as outras não saíram</b> — 2026-09-18. A linha
        // acima só fala quando alguém foi contratado, e o caso que
        // interessa é justamente o mudo: a vila do deserto parou 28 vezes
        // esperando cut_sandstone, que é do pedreiro, sem nunca ter tido
        // pedreiro — e nada no log dizia se a vaga não abriu, se abriu e
        // ninguém a quis, ou se a colônia estava lotada.
        //
        // Sai a cada passagem que tenha algo a dizer, e o HiringLog só
        // devolve texto para profissão NÃO preenchida: quem conseguiu
        // gente não é o assunto.
        String hiring = HiringLog.report(colony.id());

        if (!hiring.isEmpty()) {
            VillageColonyMod.LOGGER.info(
                    "Colony {} hiring — {}", colony.id(), hiring);
        }

        // Depois da atribuição, e não só quando ela muda algo: um
        // trabalhador vindo do save já chega com função e sem nome.
        int labelled = WorkerNameplate.label(
                world, VillageColonyMod.WORKERS.ofColony(colony.id()));

        if (labelled > 0) {
            VillageColonyMod.LOGGER.info(
                    "Named {} workers in colony {}", labelled, colony.id());
        }

        // A ferramenta vem junto do nome, e pelo mesmo motivo: a
        // profissão foi decidida agora, e Profession-System.md diz que o
        // trabalhador a recebe ao assumir a função.
        //
        // <b>E agora muda a velocidade do trabalho</b> — 2026-09-04.
        // Estas linhas diziam o contrário — "a Regra 2 fixou isso em
        // ferro" — e diziam certo até o dia em que o BlockBreakTime
        // passou a perguntar à mão do aldeão em vez de a uma constante.
        // Esta passagem é a que põe a ferramenta naquela mão, e é
        // também a que troca pela melhor do baú: é aqui que a colônia
        // fica mais rápida ao longo da partida.
        int equipped = WorkerEquipment.equip(
                world, VillageColonyMod.WORKERS.ofColony(colony.id()));

        if (equipped > 0) {
            VillageColonyMod.LOGGER.info(
                    "Equipped {} workers in colony {}", equipped, colony.id());
        }

        // A marca do baú acompanha a profissão, e por isso vem depois
        // dela: um trabalhador que acabou de perder a função não pode
        // deixar o machado pendurado no baú.
        int marked = ChestMarker.mark(
                world,
                VillageColonyMod.WORKERS.ofColony(colony.id()),
                workerId -> VillageColonyMod.STORAGES.of(workerId)
                        .map(WorkerStorage::chestPosition));

        if (marked > 0) {
            VillageColonyMod.LOGGER.info(
                    "Marked {} chests in colony {}", marked, colony.id());
        }
    }

    /**
     * Aposenta quem excede a vaga da profissão.
     *
     * <p>Uma vila tem um trabalhador de cada tipo. Quem perde a função
     * larga o que segurava: a tarefa volta para a fila, o destino é
     * cedido e a árvore em curso é devolvida — senão a tarefa ficaria
     * reservada para quem já não sabe executá-la, e a árvore ficaria
     * marcada para sempre.
     *
     * <p>O baú fica com ele. Recolhê-lo tiraria da colônia a madeira que
     * já está lá dentro, e o aldeão pode voltar a ter função quando o
     * titular morrer.
     */
    static void dismissExtraWorkers(
            ServerWorld world, Colony colony, int replacements) {

        Set<UUID> demoted = VacancyEnforcer.enforceVacanciesPreservingFoundation(
                VillageColonyMod.WORKERS,
                colony.id(),
                villagerId -> VillageColonyMod.STORAGES.of(villagerId).isPresent(),
                replacements);

        if (demoted.isEmpty()) {
            return;
        }

        int chestless = 0;

        for (UUID villagerId : demoted) {
            if (VillageColonyMod.STORAGES.of(villagerId).isEmpty()) {
                chestless++;
            }
        }

        for (UUID villagerId : demoted) {
            VillageColonyMod.TASKS.releaseAllOf(villagerId);
            WorkTargets.clear(villagerId);
            MinerWork.forget(villagerId);
            SmelterWork.forget(villagerId);
            SurfaceGatheringWork.forget(villagerId);
            ShepherdWork.forget(villagerId);
            FarmerWork.forget(villagerId);
            LumberjackWork.forget(villagerId);
            CraftingWork.forget(villagerId);
            BuilderWork.forget(villagerId);

            // A marca do baú sai: um machado pendurado no baú de quem já
            // não é lenhador mente para quem está jogando. E o da mão
            // sai pelo mesmo motivo.
            VillageColonyMod.STORAGES.of(villagerId)
                    .ifPresent(storage -> ChestMarker.unmark(world, storage.chestPosition()));

            WorkerEquipment.unequip(world, villagerId);

            // E o baú volta para a colônia. Segurá-lo prendia o
            // armazenamento a quem não trabalha: a vila do autor tinha
            // treze baús reivindicados e quatro trabalhadores, e o
            // fazendeiro não conseguia nenhum. O conteúdo fica onde
            // está; o que sai é a reserva.
            VillageColonyMod.STORAGES.remove(villagerId);
        }

        VillageColonyMod.LOGGER.info(
                "Colony {} dismissed {} workers ({} of them had no chest and lost the job"
                        + " to someone who can get one)",
                colony.id(),
                demoted.size(),
                chestless);
    }

    /**
     * Registra no log o que a colônia tem guardado.
     *
     * <p>Só quando um baú novo entra no registro. O conteúdo muda a cada
     * baú aberto pelo jogador, e logar isso a cada ciclo encheria o
     * arquivo sem dizer nada — mas sem nenhuma linha, a contagem da
     * TASK-017 seria invisível em jogo, e o §11 do Project-State existe
     * justamente porque defeitos desta camada só aparecem lá.
     *
     * <p>Diz quantos baús foram alcançados, e não só quantos tinham algo
     * dentro. A linha antiga contava apenas os não vazios, e assim
     * "nenhum baú tem madeira" e "não consegui ler baú nenhum" saíam com
     * o mesmo texto — o defeito-que-parece-número do V5.
     */
    static void logResources(Colony colony, ChestInventoryReader.ChestSurvey survey) {
        ColonyResources resources = survey.resources();

        String stock = resources.isEmpty()
                ? "nothing tracked"
                : resources.total().counts().toString();

        // Só quando muda. A linha passou a sair todo ciclo, e estoque
        // parado repetido oitenta vezes afogaria o relatório — que é o
        // defeito que o IdleLog existe para não cometer.
        String coverage = survey.coverage();
        String snapshot = stock + " | " + coverage;

        if (snapshot.equals(ColonyCycleRunner.lastStock.put(colony.id(), snapshot))) {
            return;
        }

        // A cobertura vem pronta de ChestSurvey, e de propósito: montar a
        // frase aqui foi o que deixou "in 1 of 8 chests read" passar por
        // cobertura em 2026-09-11. Ver ChestSurvey#coverage.
        VillageColonyMod.LOGGER.info(
                "Colony {} stores {} — {}",
                colony.id(),
                stock,
                coverage);
    }
}
