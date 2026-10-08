package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.FoundationPreparation;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * A trava de reserva da obra, lendo o mundo — ADR-035 §4.
 *
 * <p>Três perguntas: há peça a pôr, há lugar de pé ao alcance dela, e a base
 * se qualifica ({@link FoundationPreparation}). A reserva só acontece com as
 * três; {@link #refusal} diz qual falhou, para a linha {@code builders:} —
 * a recusa era muda e a obra ficava "AVAILABLE with nobody" sem causa
 * (playtest de 2026-10-07).
 */
public final class BuildSiteGate {

    private BuildSiteGate() {
    }

    /** Se um construtor pode reservar esta obra agora. Aterra a base quando sim. */
    public static boolean admits(ServerWorld world, ConstructionProject project) {
        return refusalBeforeFoundation(world, project).isEmpty()
                && FoundationPreparation.prepareIfQualified(world, project);
    }

    /** Por que ninguém pode reservar esta obra, ou vazio se pode — sem efeito no mundo. */
    public static Optional<String> refusal(ServerWorld world, ConstructionProject project) {
        Optional<String> before = refusalBeforeFoundation(world, project);

        return before.isPresent() ? before : FoundationPreparation.refusal(world, project);
    }

    private static Optional<String> refusalBeforeFoundation(
            ServerWorld world, ConstructionProject project) {

        Optional<BlueprintBlock> next = project.nextBlock();

        if (next.isEmpty()) {
            return Optional.of("every remaining piece is deferred");
        }

        BlockPos target = MinecraftTypeAdapter.toBlockPos(project.worldPositionOf(next.get()));

        if (!BuilderApproach.hasStandingSpotWithinReach(world, project, target)) {
            return Optional.of("no standing spot within reach of " + target.toShortString());
        }

        return Optional.empty();
    }
}
