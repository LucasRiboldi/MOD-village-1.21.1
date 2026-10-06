package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.worker.model.Profession;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.service.ProfessionRegistry;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BiomeConstructionSupply;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;

import java.util.Optional;

/**
 * A quarta busca vazia põe o material no baú da profissão que buscou —
 * regra das buscas (2026-10-03), quatro tentativas desde a ADR-036 item 6.
 *
 * <p>Achar no alcance da vila continua sendo trabalho da profissão. Quatro
 * varreduras completas e vazias seguidas querem dizer que o alcance não tem:
 * o material aparece no baú de quem buscou, ou no primeiro baú da vila com
 * espaço, e a linha {@code VC_SUPPLY_ERROR} fica no log. Quem consome tira de
 * qualquer baú da colônia ({@code ColonyChests.nearestFirst}).
 */
final class LocateFallback {

    /** Varreduras vazias seguidas antes de o material aparecer — 4 desde a ADR-036 item 6. */
    static final int ATTEMPTS = BiomeConstructionSupply.ATTEMPTS_BEFORE_STOCKING;

    private LocateFallback() {
    }

    /**
     * Uma varredura completa não achou o recurso da tarefa.
     *
     * @param empty quantas seguidas não acharam, contando esta
     * @return se o material foi posto no baú
     */
    static boolean afterEmptySearch(ServerWorld world, Task task, int empty) {
        if (empty < ATTEMPTS) {
            return false;
        }

        Optional<Item> item = MinecraftTypeAdapter.toItem(task.targetResource());

        if (item.isEmpty()) {
            return false;
        }

        int amount = Math.clamp(task.amount(), 1, item.get().getMaxCount());
        ProfessionType gatherer = gathererOf(task);
        boolean stocked = VillageColonyMod.COLONIES.find(task.colonyId())
                .map(colony -> BiomeConstructionSupply.stockAfterEmptySearches(
                        world, colony.id(), colony.center(), item.get(), amount, gatherer))
                .orElse(false);

        if (stocked) {
            // A contagem recomeça: se o baú esvaziar, a profissão volta a
            // procurar antes de o material aparecer de novo.
            EmptySweeps.found(task.colonyId(), task.targetResource());
        }

        return stocked;
    }

    /**
     * Quem buscou e não achou: a profissão da tarefa — fundidor para areia e
     * argila, fazendeiro para terra, pastor para lã. O material aparece no baú
     * dela (ADR-036 item 6); quem consome tira de qualquer baú da colônia.
     */
    static ProfessionType gathererOf(Task task) {
        return ProfessionRegistry.withCapability(task.type().required()).stream()
                .map(Profession::type)
                .findFirst()
                .orElse(ProfessionType.BUILDER);
    }
}
