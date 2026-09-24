package com.villagecolony.fabric.work;

import com.villagecolony.fabric.integration.RoadsideSites;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionReach;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.construction.service.ConstructionService;
import com.villagecolony.core.coordination.IdleReason;
import com.villagecolony.core.coordination.WorkAssignment;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.type.Side;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BuildSiteScanner;
import com.villagecolony.fabric.integration.VillageRoad;
import com.villagecolony.fabric.integration.RoadExtension;
import com.villagecolony.fabric.integration.SweepLog;
import com.villagecolony.fabric.integration.SitePreparation;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import net.minecraft.block.Block;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Quando não há lote: seguir estendendo a rua ou começar um trecho novo —
 * separado de {@link ConstructionPlanner} em 2026-09-24, quando ele passou de
 * 500 linhas. É a Regra 15 vista do planejador; o assentamento em si mora em
 * {@code RoadExtension} e {@code RoadPaving}. Os comentários vieram junto sem
 * mudança.
 */
final class RoadGrowthPlanning {

    private RoadGrowthPlanning() {
    }

    /**
     * Continua a rua que esta colônia começou, sem varrer de novo.
     *
     * <p>Irmão de {@link #extendTheRoad}, e a diferença é de onde vem a
     * autorização: lá, de uma varredura que acabou de terminar sem lote;
     * aqui, da mesma varredura, que continua valendo enquanto a ponta
     * render. Ver {@code RoadExtension.MAX_RUN} para onde isso para.
     *
     * @return a obra, quando o trecho novo já coube uma casa
     */
    static Optional<ConstructionProject> keepGrowing(
            ServerWorld world, Colony colony, Blueprint blueprint,
            List<Blueprint> plans, int builders) {

        Optional<ResourceId> paving = VillageRoad.pavingFor(
                world, HousePlans.paletteOf(world, colony.center()).style());

        if (paving.isEmpty()) {
            return Optional.empty();
        }

        if (RoadExtension.keepGrowing(world, colony.id(), paving.get())
                != RoadExtension.Outcome.EXTENDED) {

            return Optional.empty();
        }

        IdleLog.clear(colony.id(), ConstructionPlanner.SUBJECT);

        return RoadsideSites.findBeside(
                        world,
                        colony.id(),
                        colony.center(),
                        SiteOpening.sizesOf(plans),
                        RoadExtension.justPaved(colony.id()))
                .flatMap(beside -> SiteOpening.open(world, colony, beside, plans, blueprint, builders));
    }

    /**
     * Prolonga a rua quando não há mais beira livre — a Regra 15.
     *
     * <p>A ponta já foi escolhida: a varredura que acabou de falhar
     * anotou a mais distante do centro enquanto procurava lote. Aqui só
     * se calça, e o que se decide é o que dizer quando não dá.
     *
     * <p><b>Três respostas, e as três são diferentes no log.</b> Sem rua
     * nenhuma para prolongar, a vila realmente parou e o motivo é o
     * antigo. Com ponta e sem poder calçar — encosta, água, peça de vila
     * — a vila também parou, mas por outra razão, e confundir as duas
     * mandaria o autor procurar no lugar errado.
     */
    static Optional<ConstructionProject> extendTheRoad(
            ServerWorld world, Colony colony, Blueprint blueprint,
            List<Blueprint> plans, int builders) {

        Optional<ResourceId> paving = VillageRoad.pavingFor(
                world, HousePlans.paletteOf(world, colony.center()).style());

        if (paving.isEmpty()) {
            return ConstructionPlanner.silent(
                    colony,
                    IdleReason.NOT_IN_GAME,
                    "this game has no street to say what the road is made of");
        }

        RoadExtension.Outcome outcome =
                RoadExtension.extend(world, colony.id(), paving.get());

        return switch (outcome) {
            case EXTENDED -> {
                // Fala sempre, e não pelo IdleLog: rua nova é coisa que
                // aconteceu, e o log de transições cala o que se repete.
                IdleLog.clear(colony.id(), ConstructionPlanner.SUBJECT);

                // E o lote nasce agora — E26. A colônia acabou de criar a
                // beira; mandá-la redescobri-la varrendo o raio inteiro
                // custaria dezessete ciclos por uma informação que ela
                // tem na mão. Vazio aqui não é erro: o trecho novo pode
                // não caber casa, e aí a varredura seguinte decide.
                yield RoadsideSites.findBeside(
                                world,
                                colony.id(),
                                colony.center(),
                                SiteOpening.sizesOf(plans),
                                RoadExtension.justPaved(colony.id()))
                        .flatMap(beside ->
                                SiteOpening.open(world, colony, beside, plans, blueprint, builders));
            }

            case BLOCKED -> ConstructionPlanner.silent(
                    colony,
                    IdleReason.NO_TARGET,
                    "none of the road ends this colony can see may be paved — up to "
                            + RoadExtension.CANDIDATES + " were tried, and each one that"
                            + " refused sits out a while before being tried again");

            case NO_END -> ConstructionPlanner.silent(
                    colony,
                    IdleReason.NO_TARGET,
                    "no free lot beside a road in the whole "
                            + ConstructionPlanner.searchRadius + "-block radius of "
                            + colony.center() + " that fits " + blueprint.size()
                            + ", and no road end to extend either");
        };
    }
}
