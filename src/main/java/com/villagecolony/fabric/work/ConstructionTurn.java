package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ConstructionPriority;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.integration.VillageStructures;
import net.minecraft.server.world.ServerWorld;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * De quem é a vez na próxima obra — Regra 49, pedido do autor de 2026-10-02:
 * <i>"intercalado com a necessidade de camas: daí entra casa de moradia e
 * depois volta à construção de cada ofício; só depois da construção da
 * estrutura de cada ofício cada uma das restantes entra na lista de construção,
 * sempre priorizando casa se a vila precisar de cama"</i>.
 *
 * <p>A ordem:
 * <ol>
 *   <li>falta cama → casa;</li>
 *   <li>falta a casa de algum ofício → essa casa ({@link ConstructionOrder}
 *       põe a do ofício que falta à frente);</li>
 *   <li>tudo de pé → o rodízio de sempre (casa, outra), com as demais obras do
 *       catálogo.</li>
 * </ol>
 *
 * <p>O lenhador e o construtor não têm casa de ofício no catálogo do jogo; os
 * outros cinco têm — ver {@link ConstructionOrder#WORKSHOPS}.
 */
public final class ConstructionTurn {

    private ConstructionTurn() {
    }

    /** A prioridade, sabendo se falta a casa de algum ofício. */
    static ConstructionPriority of(List<Building> buildings, int adults, int beds, boolean workshopMissing) {
        Building last = buildings.isEmpty() ? null : buildings.get(buildings.size() - 1);

        return ConstructionPriority.decide(
                last != null,
                last != null && HousePlans.isHouse(last.blueprint()),
                adults,
                beds,
                workshopMissing);
    }

    /** A prioridade de agora, para o {@code /vc log}. */
    public static ConstructionPriority priorityFor(ServerWorld world, Colony colony) {
        int adults = VillageColonyMod.WORKERS.countOfColony(colony.id());
        List<Building> buildings = VillageColonyMod.BUILDINGS.ofColony(colony.id());

        return of(buildings, adults,
                HousePlans.effectiveBedsForPriority(colony.id(), adults, colony.observedBeds()),
                workshopMissing(world, colony, buildings));
    }

    /**
     * Se algum ofício ainda não tem a sua casa de pé.
     *
     * <p>Só conta o ofício cuja casa o catálogo deste estilo oferece: a vila
     * de deserto sem ferramenteiro não espera por ele.
     */
    static boolean workshopMissing(ServerWorld world, Colony colony, List<Building> buildings) {
        String style = HousePlans.paletteOf(world, colony.center()).style();
        Set<ProfessionType> offered = new HashSet<>();

        for (ResourceId id : VillageStructures.buildableFor(style)) {
            ConstructionOrder.professionOf(id).ifPresent(offered::add);
        }

        return !ConstructionOrder.missingWorkshops(buildings, offered).isEmpty();
    }
}
