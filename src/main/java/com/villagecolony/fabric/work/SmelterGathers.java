package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.Production;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BiomeConstructionSupply;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * O fundidor sem cru solta a fornalha — regras do autor, 2026-10-11.
 *
 * <p>Com o britador dando conta da areia ({@link Crusher#covers}), ele chama o
 * pedreiro e vai ajudar outro ofício (a cadeia de alternativas da Regra 51) até
 * a areia chegar. Sem pedra a moer, e sendo o cru de superfície, ele mesmo vai
 * buscá-lo: antes os dois fundidores seguravam "fundir vidro" esperando areia e a
 * colônia contratou um terceiro só para pegá-la.
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
    static boolean handOff(ServerWorld world, UUID workerId, Task smelting, List<Item> raws) {
        Optional<ResourceType> raw = raws.stream()
                .map(MinecraftTypeAdapter::toResourceType)
                .flatMap(Optional::stream)
                .filter(type -> type.production() == Production.SURFACE_GATHERED)
                .findFirst();

        if (raw.isEmpty()) {
            return false;
        }

        UUID colonyId = smelting.colonyId();
        Optional<Colony> colony = VillageColonyMod.COLONIES.find(colonyId);

        if (raw.get() == ResourceType.SAND && colony.isPresent() && Crusher.covers(world, colony.get())) {
            Crusher.askTheMason(world, colony.get());
            smelting.release("no sand in any chest — the mason crushes it; the smelter helps elsewhere");
            VillageColonyMod.LOGGER.info("Smelter {} leaves the furnace to help elsewhere — the crusher makes the sand",
                    workerId.toString().substring(0, 8));

            return true;
        }

        // A vila já provou que não tem este cru no alcance (KnownAbsent, regra do autor de 2026-10-20):
        // registra e o material aparece no baú do fundidor, que segue na fornalha em vez de sair de novo.
        if (colony.isPresent() && KnownAbsent.isAbsent(colonyId, raw.get(), world.getTime())
                && stockWhatIsAbsent(world, colony.get(), workerId, raw.get())) {
            return false;
        }

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

    /**
     * O cru registrado como ausente aparece no baú do fundidor ({@link BiomeConstructionSupply}, que
     * também escreve a linha {@code VC_SUPPLY_ERROR}). O que a vila já tem em outro baú é levado antes.
     *
     * @return se o baú dele passou a ter o pedido; sem espaço, ele cai na busca de sempre
     */
    private static boolean stockWhatIsAbsent(ServerWorld world, Colony colony, UUID workerId, ResourceType raw) {
        Optional<Item> item = MinecraftTypeAdapter.toItem(raw);

        if (item.isEmpty()) {
            return false;
        }

        boolean stocked = BiomeConstructionSupply.stockAfterEmptySearches(
                world, colony.id(), colony.center(), item.get(), ORDER, ProfessionType.SMELTER);

        if (stocked) {
            VillageColonyMod.LOGGER.info(
                    "Smelter {} stays at the furnace — the village has no {} in reach (registered absent);"
                            + " it was stocked into its chest instead of another search",
                    workerId.toString().substring(0, 8), raw.name().toLowerCase(java.util.Locale.ROOT));
        }

        return stocked;
    }
}
