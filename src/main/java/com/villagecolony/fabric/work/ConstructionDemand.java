package com.villagecolony.fabric.work;

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
 * Quanto as obras da colônia ainda pedem, e se a roça cabe ao alcance do
 * fazendeiro — separado de {@link ConstructionPlanner} em 2026-09-24, quando
 * ele passou de 500 linhas.
 *
 * <p>É o lado de <b>demanda</b> da construção, lido pelo ciclo da colônia e
 * pelos ofícios para decidir o que produzir. Os comentários vieram junto sem
 * mudança.
 */
public final class ConstructionDemand {

    private ConstructionDemand() {
    }

    /**
     * Se este lote está onde o fazendeiro trabalha.
     *
     * <p>Medido em quadrado e a partir do centro da vila, que é
     * exatamente como {@code CropPatch.survey} varre: usar aqui uma
     * conta diferente da dele poria a roça na borda que ele nunca
     * alcança, e é esse o defeito que esta guarda existe para não
     * repetir.
     *
     * <p><b>Pública para o teste chamá-la</b>, e não por precisar de
     * fora. É o precedente do {@code MinerApproach.footingIn}, e pelo mesmo
     * motivo: um teste que reimplementasse esta conta afirmaria a cópia
     * dele, e não a regra — foi assim que a correção do E32 passou sem
     * ninguém ver, em 2026-09-05.
     */
    public static boolean withinTheFarmersReach(Colony colony, BuildSiteScanner.Site site) {
        int dx = Math.abs(site.origin().x() - colony.center().x());
        int dz = Math.abs(site.origin().z() - colony.center().z());

        return Math.max(dx, dz) <= FarmerWork.reach();
    }

    /**
     * Quantas tábuas a obra em curso ainda pede.
     *
     * <p>É o número que a Regra 5 usa para substituir a metade do
     * armazém. Zero quando não há obra — e aí volta a valer a metade.
     */
    public static int planksNeededBy(ResourceId planks, Colony colony) {
        return materialNeededBy(planks, colony);
    }

    /**
     * Quanto deste material a obra aberta ainda pede.
     *
     * <p>Era só para tábua até 2026-08-20, e virou geral quando o
     * mineiro entrou: a meta da colônia precisa saber que a casa quer 43
     * pedregulhos, senão ninguém abre tarefa de mineração e a obra dorme
     * esperando um material que a colônia já sabe fazer.
     */
    /**
     * Se esta colônia já ergueu alguma coisa — 2026-09-19.
     *
     * <p>A guarda da regra <i>"a primeira construção deve ser uma
     * casa"</i>. Conta obra <b>erguida</b>, e não planejada: uma obra
     * planejada que ainda espera material não abriga ninguém, e deixar a
     * roça passar na frente dela devolveria o defeito com outro nome.
     *
     * <p>Qualquer prédio serve, e é de propósito — a regra é sobre a
     * <b>primeira</b> obra da vila, não sobre haver sempre mais casas
     * que roças. Depois da primeira, a cota da população volta a mandar.
     */
    static boolean hasBuiltSomething(UUID colonyId) {
        return !VillageColonyMod.BUILDINGS.ofColony(colonyId).isEmpty();
    }

    public static int materialNeededBy(ResourceId material, Colony colony) {
        return VillageColonyMod.CONSTRUCTIONS.openOf(colony.id())
                .map(project -> project.remainingMaterials().getOrDefault(material, 0))
                .orElse(0);
    }

    /**
     * Tudo o que a obra aberta ainda pede, material por material.
     *
     * <p>Existe para quem precisa <b>classificar</b> o que falta em vez
     * de somar um nome: a meta de fornalha da ADR-009 pergunta de cada
     * material se ele sai de forno, e para isso precisa vê-los todos.
     *
     * <p>Vazio quando não há obra, que é a resposta certa e não um erro.
     */
    public static Map<ResourceId, Integer> materialsNeededBy(Colony colony) {
        return VillageColonyMod.CONSTRUCTIONS.openOf(colony.id())
                .map(ConstructionProject::remainingMaterials)
                .orElse(Map.of());
    }

    /**
     * O mesmo, para uma família de materiais — 2026-08-21.
     *
     * <p>A cama tem dezesseis cores e a planta grava a que está no
     * arquivo. Perguntar por {@code white_bed} devolve zero numa casa que
     * pede {@code red_bed}, e zero é o pastor sem tarefa — o mesmo
     * defeito que o vidro teve por pedir vidraça.
     *
     * <p>A lã que a colônia produz é branca, e a cama colorida ainda
     * pede tinta que ninguém faz. O que isto conserta é a <b>demanda</b>
     * aparecer; a cor é problema do dia em que a colônia souber tingir.
     */
    public static int materialNeededBy(Predicate<ResourceId> family, Colony colony) {
        return VillageColonyMod.CONSTRUCTIONS.openOf(colony.id())
                .map(project -> project.remainingMaterials().entrySet().stream()
                        .filter(entry -> family.test(entry.getKey()))
                        .mapToInt(Map.Entry::getValue)
                        .sum())
                .orElse(0);
    }
}
