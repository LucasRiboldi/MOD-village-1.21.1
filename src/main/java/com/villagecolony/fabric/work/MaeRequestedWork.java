package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BuildSiteScanner;
import com.villagecolony.fabric.integration.MaeBuildQueue;
import com.villagecolony.fabric.integration.MaeSites;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Direction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A obra pedida entra na vaga do construtor — Regra 213, ADR-047: é a próxima obra da colônia, antes do reparo e da busca de
 * lote, e dali em diante é uma obra como as outras, com material dos baús e bloco a bloco. O pedido só sai quando ela terminar.
 */
final class MaeRequestedWork {

    /** Lote que não abriu só procura outro de tempos em tempos: a busca lê o terreno inteiro. */
    private static final long MOVE_EVERY = 6_000;

    private static final Map<UUID, Long> LAST_MOVE = new HashMap<>();

    static {
        ServerMemory.register(MaeRequestedWork.class, LAST_MOVE::clear);
    }

    private MaeRequestedWork() {
    }

    /** Fecha o pedido da obra pronta; o da obra que sumiu sem terminar volta para a fila, para sempre terminar. */
    static void settle(ServerWorld world, Colony colony) {
        for (MaeBuildQueue.Request request : MaeBuildQueue.ofColony(world, colony.id())) {
            if (!request.opened() || stillOpen(colony, request)) {
                continue;
            }

            if (isBuilt(colony, request)) {
                MaeBuildQueue.remove(world, request.id());
                VillageColonyMod.LOGGER.info("VC_MAE_SITE version=2 model={} at={} result=done",
                        request.model(), request.floor().toShortString());
                MaeSites.tell(world.getServer(), request.owner(), Text.literal("Obra MAE " + request.model() + " pronta em "
                        + request.floor().toShortString() + ".").formatted(Formatting.GREEN));
            } else {
                MaeBuildQueue.opened(world, request.id(), false);
                VillageColonyMod.LOGGER.info("VC_MAE_SITE version=2 model={} at={} result=requeued",
                        request.model(), request.floor().toShortString());
            }
        }
    }

    private static boolean stillOpen(Colony colony, MaeBuildQueue.Request request) {
        ColonyPos origin = MinecraftTypeAdapter.toColonyPos(request.origin());

        return VillageColonyMod.CONSTRUCTIONS.openOf(colony.id()).filter(project -> project.origin().equals(origin)).isPresent()
                || VillageColonyMod.CONSTRUCTIONS.pendingOf(colony.id()).filter(pending -> pending.origin().equals(origin)).isPresent();
    }

    private static boolean isBuilt(Colony colony, MaeBuildQueue.Request request) {
        return VillageColonyMod.BUILDINGS.ofColony(colony.id()).stream().anyMatch(building -> building.finished()
                && building.blueprint().equals(request.model())
                && building.min().x() == request.origin().getX() && building.min().z() == request.origin().getZ());
    }

    /** Abre a primeira obra pedida; sem vaga ou sem lote, a colônia segue e a obra espera a próxima passada. */
    static Optional<ConstructionProject> openNext(ServerWorld world, Colony colony, int builders) {
        List<MaeBuildQueue.Request> requests = MaeBuildQueue.ofColony(world, colony.id());

        if (requests.isEmpty() || requests.getFirst().opened()) {
            return Optional.empty();
        }

        MaeBuildQueue.Request request = requests.getFirst();
        Blueprint blueprint;

        try {
            blueprint = MaeSites.blueprint(world, request.model(), request.quarterTurns());
        } catch (IllegalStateException | IllegalArgumentException unreadable) {
            VillageColonyMod.LOGGER.warn("VC_MAE_SITE version=2 model={} at={} result=dropped detail={}", request.model(),
                    request.floor().toShortString(), unreadable.getMessage());
            MaeBuildQueue.remove(world, request.id());

            return Optional.empty();
        }

        blueprint = com.villagecolony.fabric.integration.BiomeBlueprints.styledAt(world, colony.center(), blueprint);
        ConstructionProject project = ConstructionProject.plan(colony.id(), blueprint, MinecraftTypeAdapter.toColonyPos(request.origin()));
        Direction door = blueprint.doorSide().map(MinecraftTypeAdapter::toDirection).orElse(Direction.SOUTH);
        BuildSiteScanner.Site site = new BuildSiteScanner.Site(MinecraftTypeAdapter.toColonyPos(request.floor()), door, blueprint.size());
        Optional<ConstructionProject> opened = SiteOpening.openPlanned(world, colony, site, project, builders, "a request");

        if (opened.isPresent()) {
            MaeBuildQueue.opened(world, request.id(), true);
            LAST_MOVE.remove(request.id());
            VillageColonyMod.LOGGER.info("VC_MAE_SITE version=2 model={} at={} rotation={} result=building",
                    request.model(), request.floor().toShortString(), request.quarterTurns());
        } else if (world.getTime() - LAST_MOVE.getOrDefault(request.id(), Long.MIN_VALUE / 2) >= MOVE_EVERY) {
            LAST_MOVE.put(request.id(), world.getTime());
            MaeSites.move(world, request);
        }

        return opened;
    }
}
