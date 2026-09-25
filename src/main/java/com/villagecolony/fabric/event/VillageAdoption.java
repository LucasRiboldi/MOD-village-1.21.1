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
 * Achar, adotar e acompanhar vilas: a detecção em volta de um ponto e do centro, o abandono, o ciclo de vida e os avisos de sobreposição e de encolhimento recusado — separado de
 * {@link VillageDetectionHandler} em 2026-09-24, quando ele passou de 500
 * linhas. Os comentários vieram junto sem mudança.
 */
final class VillageAdoption {

    private VillageAdoption() {
    }

    /**
     * Reavalia cada colônia ativa a partir do próprio centro.
     *
     * <p>É a âncora estável que permite encolher. Roda depois de
     * {@link #updateLifecycles} para não varrer colônia dormente, cujos
     * chunks não estão carregados — a varredura não acharia cama alguma
     * e a colônia se veria vazia.
     *
     * <p>Uma consulta de POI por colônia ativa a cada ciclo. O limite de
     * Performance-Rules.md §5 continua respeitado: a busca é por raio em
     * torno de um ponto, nunca pelo mundo.
     */
    static void detectFromColonyCenters(ServerWorld overworld) {
        detectFromColonyCenters(overworld, colony -> true);
    }

    /**
     * O mesmo, só nas colônias que a regra deixa analisar — 2026-09-24. Em
     * jogo, a vila foco e as que têm jogador dentro; ver VillageFocus.
     */
    static void detectFromColonyCenters(
            ServerWorld overworld, java.util.function.Predicate<Colony> analyzed) {
        List<Colony> active = new ArrayList<>();

        for (Colony colony : VillageColonyMod.COLONIES.all()) {
            if (colony.isActive() && analyzed.test(colony)) {
                active.add(colony);
            }
        }

        for (Colony colony : active) {
            // Antes da varredura: a adoção move centros, e a pergunta do
            // abandono é sobre o que a sonda enxergou de onde ela partiu.
            ColonyPos probedFrom = colony.center();

            VillageScanner.ScanResult result = detectAround(
                    overworld, MinecraftTypeAdapter.toBlockPos(probedFrom), true);

            judgeAbandonment(colony, probedFrom, result);
        }
    }

    /**
     * Marca — ou desmarca — a colônia cuja própria sonda não achou vila.
     *
     * <p>É o único escritor de {@link com.villagecolony.core.colony.model.ColonyState}
     * em produção. Até 2026-08-13 não havia nenhum: o valor
     * {@code ABANDONED} existia no enum, a ADR-003 §6 o exigia, e nada o
     * atribuía.
     *
     * <p>Roda só aqui, dentro da sonda, e só para colônia ACTIVE — as
     * duas condições que separam "a vila acabou" de "ninguém olhou". A
     * regra em si é de {@code ColonyAbandonment}, no Core; o que mora
     * nesta camada é o log e a aplicação.
     *
     * <p>Silencioso quando nada muda, que é sempre. Uma vila viva
     * produziria uma linha a cada ciclo.
     */
    static void judgeAbandonment(
            Colony colony, ColonyPos probedFrom, VillageScanner.ScanResult result) {

        ColonyAbandonment.judge(colony, probedFrom, result.candidates(), result.ignoredByBiome())
                .ifPresent(state -> {
                    ColonyState was = colony.state();

                    colony.setState(state);

                    // O motivo sai nos dois sentidos desde 2026-08-21, e
                    // a soma da sessão sai ao parar o servidor: é o
                    // dado que o E9 pede antes de a TASK-048 poder ser
                    // escrita. Ver ColonyStateLog.
                    ColonyStateLog.transition(
                            colony.id(),
                            was,
                            state,
                            "probed from " + probedFrom + ", " + describe(result));
                });
    }

    /**
     * O que a sonda viu, para as duas linhas de transição.
     *
     * <p>É o "instrumentar antes de suspeitar" do §11 aplicado a esta
     * regra: sem o motivo, uma colônia marcada como abandonada manda
     * alguém adivinhar entre camas demolidas, aldeões mortos e uma sonda
     * que não achou nada porque o chunk não estava onde se pensava. As
     * três têm correções diferentes.
     *
     * <p><b>Desde 2026-08-21 fala também da volta</b>, e é metade do que
     * o E9 pede. A linha da desmarcação não dizia nada, então uma vila
     * reconstruída e uma sonda que enxergou mal produziam a mesma frase.
     * Quando há candidato, o que se relata é ele.
     */
    static String describe(VillageScanner.ScanResult result) {
        if (!result.candidates().isEmpty()) {
            List<String> seen = new ArrayList<>();

            for (VillageCandidate candidate : result.candidates()) {
                seen.add(candidate.bedCount() + " beds at " + candidate.center());
            }

            return "saw " + String.join("; ", seen);
        }

        if (result.rejected().isEmpty()) {
            return "no bed cluster within range at all";
        }

        StringBuilder text = new StringBuilder();

        for (ClusterRejection rejection : result.rejected()) {
            if (text.length() > 0) {
                text.append("; ");
            }

            text.append(rejection.reason())
                    .append(" at ")
                    .append(rejection.center())
                    .append(" — ")
                    .append(rejection.bedCount())
                    .append(" beds, ")
                    .append(rejection.villagersAsText())
                    .append(" villagers");
        }

        return text.toString();
    }

    /**
     * Acorda e adormece colônias conforme seus chunks.
     *
     * <p>Sem isto, uma colônia visitada uma vez permaneceria
     * {@link ColonyLifecycle#ACTIVE} pelo resto da sessão, mesmo a
     * milhares de blocos do jogador — e o loop de simulação, que só roda
     * para colônias ACTIVE, gastaria tick com vila que ninguém observa.
     *
     * <p>{@code shouldTick} é o critério certo: pergunta se o chunk está
     * de fato sendo tickado, que é a definição de DORMANT na ADR-002.
     *
     * <p>Limite do MVP: consulta apenas o Overworld. Só existem colônias
     * lá, porque o único bioma aceito é PLAINS.
     */
    static void updateLifecycles(ServerWorld overworld) {
        for (Colony colony : VillageColonyMod.COLONIES.all()) {
            ChunkPos chunk = new ChunkPos(MinecraftTypeAdapter.toBlockPos(colony.center()));

            ColonyLifecycle current = overworld.shouldTick(chunk)
                    ? ColonyLifecycle.ACTIVE
                    : ColonyLifecycle.DORMANT;

            if (colony.lifecycle() == current) {
                continue;
            }

            colony.setLifecycle(current);

            VillageColonyMod.LOGGER.info("Colony {} is now {}", colony.id(), current);
        }
    }

    /**
     * Registra o resultado de cada detecção.
     *
     * <p>Criação e mudança de centro são logadas; reavaliação que não
     * muda nada é silenciosa. Sem isso o log não distingue "a detecção
     * rodou e a vila já era conhecida" de "a detecção nunca rodou" — foi
     * essa cegueira que escondeu o gatilho de chunk quebrado.
     *
     * <p>Não vira spam: o centro só se move quando o conjunto de camas ao
     * alcance muda. Jogador parado não gera linha. Ver CODE-STANDARDS §8.
     */
    static void detectAround(ServerWorld world, BlockPos trigger) {
        detectAround(world, trigger, false);
    }

    /**
     * A detecção em volta do gatilho, dizendo se ela é a sonda do centro.
     *
     * @param isProbe se a varredura é a sonda ancorada no centro de uma
     *     colônia, a única cujas leituras se confirmam entre ciclos
     * @return tudo o que a varredura viu, aprovado e recusado. Só a sonda
     *     usa a parte recusada, para decidir abandono
     */
    static VillageScanner.ScanResult detectAround(
            ServerWorld world, BlockPos trigger, boolean isProbe) {
        // Uma observação por colônia, e não uma por aglomerado de camas.
        // Entre 32 e 64 blocos existe a faixa em que um punhado de camas
        // é outro aglomerado e a mesma colônia: os dois candidatos
        // chegavam com a mesma âncora, e o segundo era confirmado pelo
        // primeiro dentro do mesmo tick. Ver ColonyService#bestPerColony
        // e §17, E2.
        VillageScanner.ScanResult result = VillageDetectionHandler.SCANNER.survey(world, trigger, isProbe);

        for (VillageCandidate candidate
                : VillageColonyMod.COLONIES.bestPerColony(result.candidates())) {
            int before = VillageColonyMod.COLONIES.count();

            Optional<Colony> known = VillageColonyMod.COLONIES
                    .findNearest(candidate.center(), VillageDetector.DUPLICATE_DISTANCE);

            ColonyPos previousCenter = known.map(Colony::center).orElse(null);

            logRefusedShrink(known, candidate);

            Colony colony = VillageColonyMod.COLONIES.adopt(candidate);

            boolean created = VillageColonyMod.COLONIES.count() > before;
            if (created && !candidate.beds().isEmpty()) {
                // Só a primeira adoção recebe esta passagem. A lista é o
                // cluster exato que acabou de provar a vila, nunca uma
                // varredura posterior de trabalhador ou de fundação.
                VanillaBedChests.ensure(world, candidate.beds());
            }

            BigHouseFoundation.Result house = BigHouseFoundation.ensure(world, colony);

            // A partir das camas vistas, e não do centro — 2026-08-22.
            // Desde a Emenda 4 o centro não persegue mais a observação,
            // então uma colônia que adote um aglomerado longe do próprio
            // centro procuraria aldeões no lugar errado.
            VillagerRegistration.registerVillagers(world, colony, candidate.center());

            // A fundação povoa a vila uma vez só: quando ela nasce ou
            // quando a BigHouseMOD acaba de subir (um aldeão por cama).
            // Em qualquer outro ciclo, quem morreu fica morto e a vila
            // cresce por procriação — N1, 2026-09-24.
            VillageFoundation.Result foundation = created || house.placed()
                    ? VillageFoundation.ensure(
                            world, colony, candidate.center(),
                            VillageColonyMod.WORKERS, house.placed())
                    : new VillageFoundation.Result(0, 0);

            if (foundation.changed()) {
                VillagerRegistration.registerVillagers(world, colony, candidate.center());
                VillagerRegistration.registerVillagers(world, colony, candidate.center());
            }

            if (created) {
                VillageColonyMod.LOGGER.info(
                        "Colony created at {} with {} beds",
                        colony.center(),
                        candidate.bedCount());
            } else if (previousCenter != null && !colony.center().equals(previousCenter)) {
                VillageColonyMod.LOGGER.info(
                        "Colony {} moved from {} to {} with {} beds",
                        colony.id(),
                        previousCenter,
                        colony.center(),
                        candidate.bedCount());
            }

            warnIfOverlapping(colony);
        }

        return result;
    }

    /**
     * Avisa quando dois centros ficam perto demais.
     *
     * <p>ADR-003 §5, e é a linha que a ADR pede desde 2026-08-06 sem que
     * ninguém a escrevesse. O MVP não funde as duas colônias — fundir
     * exige nova ADR, e a decisão de 2026-08-12 já disse qual será o
     * critério: um bloco de uma encostando no da outra, o que depende da
     * construção existir.
     *
     * <p>Até lá, o que este aviso dá é o nome do problema quando ele
     * aparecer em jogo: duas colônias sobrepostas disputam trabalhador —
     * a vaga de profissão vale por colônia do registro, não por vila
     * física —, e sem esta linha o sintoma seria um aldeão que troca de
     * vila sem motivo aparente. É o risco aberto do §11 do Project-State,
     * que até aqui acontecia em silêncio.
     *
     * <p>Cada par é avisado uma vez por sessão. A sobreposição não se
     * resolve sozinha, e a sonda passa por aqui a cada 600 ticks: sem a
     * memória do par, seriam cem linhas iguais por hora dizendo a mesma
     * coisa.
     */
    static void warnIfOverlapping(Colony colony) {
        for (Colony other : VillageColonyMod.COLONIES.overlapping(colony)) {
            if (!VillageDetectionHandler.overlapsReported.add(pairKey(colony.id(), other.id()))) {
                continue;
            }

            VillageColonyMod.LOGGER.warn(
                    "Overlapping colonies detected — {} at {} and {} at {} are {} blocks apart"
                            + " (less than {}); the MVP does not merge them",
                    colony.id(),
                    colony.center(),
                    other.id(),
                    other.center(),
                    (int) Math.sqrt(colony.center().horizontalDistanceSquared(other.center())),
                    VillageDetector.OVERLAP_DISTANCE);
        }
    }

    /**
     * O par, na mesma ordem venha de que lado vier.
     *
     * <p>A sonda de cada uma das duas encontra a outra, e sem a ordem
     * fixa o mesmo par seria contado duas vezes — uma por colônia.
     */
    static String pairKey(UUID one, UUID other) {
        return one.compareTo(other) <= 0 ? one + "|" + other : other + "|" + one;
    }

    /**
     * Quando uma observação viu menos camas mas não teve autoridade para
     * baixar a contagem.
     *
     * <p>Existe porque em 2026-08-07 camas foram destruídas em jogo e a
     * colônia não encolheu, e o log não sabia dizer se a regra de
     * completude tinha recusado a observação ou se a observação menor
     * nunca tinha chegado. São causas diferentes com correções
     * diferentes.
     *
     * <p>É o "instrumentar antes de suspeitar" do §11: a linha que expõe
     * o caso precisa existir antes de alguém desconfiar dele.
     *
     * <p>Não vira spam por si: só sai quando a contagem observada está
     * abaixo da registrada, que é justamente o caso raro.
     *
     * <p>Diz onde o candidato estava e de onde a varredura partiu. Sem
     * isso a linha é um número sem lugar, e foi essa cegueira que
     * escondeu o E2 por três dias: "viu 5 de 31" parecia sonda com
     * defeito e era, o tempo todo, um segundo aglomerado de camas a
     * quarenta blocos. Com o centro na linha, dois aglomerados
     * diferentes se distinguem de imediato de uma leitura pobre do
     * mesmo.
     */
    static void logRefusedShrink(Optional<Colony> known, VillageCandidate candidate) {
        known.ifPresent(colony -> {
            if (candidate.bedCount() >= colony.observedBeds() || candidate.complete()) {
                return;
            }

            VillageColonyMod.LOGGER.info(
                    "Colony {} saw {} beds at {} from anchor {}, keeping {}"
                            + " — view not provably complete",
                    colony.id(),
                    candidate.bedCount(),
                    candidate.center(),
                    candidate.anchor() == null ? "none" : candidate.anchor(),
                    colony.observedBeds());
        });
    }
}
