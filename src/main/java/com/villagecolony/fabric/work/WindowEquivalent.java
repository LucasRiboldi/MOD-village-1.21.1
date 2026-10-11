package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.coordination.GatheringReach;
import com.villagecolony.core.coordination.WorkAssignment;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.SandNearWater;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A janela sem vidro — P2d, autossuficiência, 2026-10-11 (escolha do autor).
 *
 * <p>Sem areia de beira d'água ao alcance e sem pedra para o britador
 * ({@link Crusher}), o vidro não tem rota na vila. Nesse caso, e só nele, a
 * vidraça da planta aceita a cerca de madeira, peça Vanilla que o carpinteiro
 * faz. Antes a 4ª tentativa criava a vidraça do nada.
 */
final class WindowEquivalent {

    private WindowEquivalent() {
    }

    /**
     * As escolhas de material desta peça na obra: as do {@link MaterialChoice}; e,
     * se a janela não tem vidro, a cerca na frente — ela vira a peça pedida, e a
     * 4ª tentativa não cria vidraça do nada.
     */
    static List<Item> choicesFor(ServerWorld world, ConstructionProject project, Block wanted) {
        List<Item> choices = MaterialChoice.forBlock(wanted);

        if (!applies(world, project, wanted)) {
            return choices;
        }

        List<Item> widened = new ArrayList<>(MaterialChoice.forBlock(Blocks.OAK_FENCE));

        for (Item glass : choices) {
            if (!widened.contains(glass)) {
                widened.add(glass);
            }
        }

        return List.copyOf(widened);
    }

    /** Se esta peça é vidraça e a vila não tem como fazer vidro. */
    static boolean applies(ServerWorld world, ConstructionProject project, Block wanted) {
        if (!MinecraftTypeAdapter.toResourceId(wanted).path().endsWith("glass_pane")) {
            return false;
        }

        Optional<Colony> colony = VillageColonyMod.COLONIES.find(project.colonyId());

        return colony.isPresent() && !glassHasARoute(world, colony.get());
    }

    private static boolean glassHasARoute(ServerWorld world, Colony colony) {
        boolean sandNearWater = SandNearWater.around(world, colony.id(),
                MinecraftTypeAdapter.toBlockPos(colony.center()),
                GatheringReach.radius(colony.observedBeds(), SandNearWater.RADIUS));

        boolean crusher = WorkAssignment.countCapableOf(colony.id(),
                TaskType.CRAFT_STONE_MATERIAL.required(), VillageColonyMod.WORKERS) > 0
                && Crusher.canCrush(world, colony);

        return sandNearWater || crusher;
    }
}
