package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.construction.model.LampPostPlan;
import com.villagecolony.core.construction.model.VillagePalette;
import com.villagecolony.core.coordination.IdleReason;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.VillageBiomes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * O poste de luz entra na vaga ociosa do construtor — Regra 261 (autor, 2026-11-12, opção B): só quando a busca de lote
 * terminou sem obra, para a casa nunca atrasar por causa do poste. Dali em diante é uma obra comum, com material dos baús
 * e bloco a bloco: a pedra do baú, a cerca do carpinteiro e a tocha da colônia pela cadeia dela (tochas e ouro).
 *
 * <p>A abertura é leve: o poste fica ao lado da rua fixa, então não há lote a aplainar, chão em volta a nivelar nem
 * caminho a calçar até ele. A base toma o lugar do solo, na camada da rua ({@code BuriedPieces.mayReplaceGround}).
 */
final class LampPostWork {

    private LampPostWork() {
    }

    /** Se a busca de lote terminou sem obra, e a vaga do construtor está ociosa: nada a construir ou nenhum lote. */
    static boolean mayOpenAfter(IdleReason reason) {
        return reason == IdleReason.VILLAGE_COMPLETE || reason == IdleReason.NO_TARGET;
    }

    /** Abre o próximo poste, se a busca de lote acabou de deixar a vaga ociosa. */
    static Optional<ConstructionProject> openIfIdle(ServerWorld world, Colony colony, int builders) {
        boolean idle = IdleLog.lastOf(colony.id(), ConstructionPlanner.SUBJECT).filter(LampPostWork::mayOpenAfter).isPresent();

        return idle ? openNext(world, colony, builders, RoadGraphRefresh.dayOf(world)) : Optional.empty();
    }

    /** Abre o próximo poste ao lado da linha fixa; sem vaga, sem construtor ou sem lugar, nada muda. */
    static Optional<ConstructionProject> openNext(ServerWorld world, Colony colony, int builders, long today) {
        if (builders == 0 || VillageColonyMod.CONSTRUCTIONS.openOf(colony.id()).isPresent()
                || VillageColonyMod.CONSTRUCTIONS.pendingOf(colony.id()).isPresent()) {
            return Optional.empty();
        }

        Optional<BlockPos> site = LampPostSites.find(world, colony, today);

        if (site.isEmpty()) {
            return Optional.empty();
        }

        ResourceId stone = VillageBiomes.paletteAt(world, colony.center())
                .map(VillagePalette::stone)
                .orElse(VillagePalette.COBBLESTONE);
        ResourceId fence = LampPostPlan.fenceOf(VillageBiomes.woodAt(world, colony.center())
                .orElse(ResourceId.vanilla("oak_planks")));
        Blueprint blueprint = LampPostPlan.blueprint(stone, fence);
        ConstructionProject project = ConstructionProject.plan(colony.id(), blueprint, MinecraftTypeAdapter.toColonyPos(site.get()));

        VillageColonyMod.CONSTRUCTIONS.register(project);
        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);
        ConstructionPlanner.ensureTask(colony, project);
        IdleLog.clear(colony.id(), ConstructionPlanner.SUBJECT);

        VillageColonyMod.LOGGER.info("VC_LAMP_POST version=1 colony={} at={} plan={} result=open",
                colony.id(), site.get().toShortString(), blueprint.id());

        return Optional.of(project);
    }
}
