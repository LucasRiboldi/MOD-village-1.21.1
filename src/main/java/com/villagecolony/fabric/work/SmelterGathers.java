package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.Production;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.item.Item;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * O fundidor sem cru vai buscá-lo — regra do autor, 2026-10-11, depois do
 * playtest da 0.3.25.
 *
 * <p>Os dois fundidores seguravam "fundir vidro" esperando areia, ninguém ficava
 * livre para a tarefa de areia, e a colônia contratou um terceiro só para pegá-la
 * (7,6 minutos depois). Agora quem está com a fornalha parada por falta de um cru
 * que se recolhe na superfície (areia) solta a fundição e assume a coleta.
 */
final class SmelterGathers {

    /** Quanto cru o fundidor vai buscar quando ele mesmo abre a coleta. */
    static final int ORDER = 8;

    private SmelterGathers() {
    }

    /**
     * Passa o fundidor da fundição parada para a coleta do cru.
     *
     * @return se ele soltou a fundição e ficou com a coleta
     */
    static boolean handOff(UUID workerId, Task smelting, List<Item> raws) {
        Optional<ResourceType> raw = raws.stream()
                .map(MinecraftTypeAdapter::toResourceType)
                .flatMap(Optional::stream)
                .filter(type -> type.production() == Production.SURFACE_GATHERED)
                .findFirst();

        if (raw.isEmpty()) {
            return false;
        }

        UUID colonyId = smelting.colonyId();
        Task gather = VillageColonyMod.TASKS.availableFor(colonyId).stream()
                .filter(task -> task.type() == TaskType.COLLECT_SURFACE_RESOURCE
                        && task.targetResource() == raw.get())
                .findFirst()
                .orElseGet(() -> VillageColonyMod.TASKS.create(colonyId, TaskType.COLLECT_SURFACE_RESOURCE,
                        TaskPriority.CONSTRUCTION_MATERIAL, raw.get(), ORDER));

        smelting.release("no " + raw.get().name().toLowerCase(java.util.Locale.ROOT)
                + " in any chest — the smelter goes to gather it");
        gather.reserveFor(workerId);

        VillageColonyMod.LOGGER.info("Smelter {} leaves the furnace to gather {} — none in any colony chest",
                workerId.toString().substring(0, 8), raw.get().name().toLowerCase(java.util.Locale.ROOT));

        return true;
    }
}
