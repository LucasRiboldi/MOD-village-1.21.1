package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.ChestCoverage;
import com.villagecolony.core.colony.model.ColonyProfession;
import com.villagecolony.core.colony.model.VillageInventory;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import net.minecraft.server.world.ServerWorld;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Monta um {@link VillageInventory} lendo os registros da colônia —
 * decisão 11A, 2026-09-24.
 *
 * <p><b>Nunca chama {@code ConstructionPlanner.plan}.</b> É a garantia
 * que dá nome à decisão: observar a vila não pode empurrar o
 * planejamento adiante, senão a própria pergunta muda a resposta da
 * próxima. Nem {@code HousePlans} nem {@code ConstructionPlanner} são
 * tocados aqui — só lidos, e só através dos registros que já existem.
 *
 * <p>Faz a tradução entre os domínios do {@code core}
 * ({@code ProfessionType} → {@link ColonyProfession},
 * {@code ChestSurvey} → {@link ChestCoverage}) que
 * {@code DependencyRuleTest} proíbe {@code core/colony} de fazer
 * sozinho.
 */
public final class VillageInventoryObserver {

    private VillageInventoryObserver() {
    }

    /**
     * O inventário desta colônia agora, ou vazio se ela não existir no
     * registro.
     */
    public static Optional<VillageInventory> observe(ServerWorld world, UUID colonyId) {
        Optional<Colony> colony = VillageColonyMod.COLONIES.find(colonyId);

        if (colony.isEmpty()) {
            return Optional.empty();
        }

        List<Worker> workers = VillageColonyMod.WORKERS.ofColony(colonyId);

        List<ColonyPos> chests = ColonyChests.nearestFirst(
                world, colonyId, colony.get().center());

        ChestInventoryReader.ChestSurvey survey = ChestInventoryReader.survey(world, chests);

        List<Building> buildings = VillageColonyMod.BUILDINGS.ofColony(colonyId);

        int completed = (int) buildings.stream().filter(Building::finished).count();
        int active = buildings.size() - completed;

        return Optional.of(new VillageInventory(
                colonyId,
                workers.size(),
                colony.get().observedBeds(),
                professionsOf(workers),
                completed,
                active,
                new ChestCoverage(survey.chestsRead(), survey.chestsUnreachable()),
                survey.resources().total().idCounts()));
    }

    private static Map<ColonyProfession, Integer> professionsOf(List<Worker> workers) {
        Map<ColonyProfession, Integer> counts = new EnumMap<>(ColonyProfession.class);

        for (Worker worker : workers) {
            worker.profession()
                    .map(VillageInventoryObserver::toColonyProfession)
                    .ifPresent(profession -> counts.merge(profession, 1, Integer::sum));
        }

        return counts;
    }

    private static ColonyProfession toColonyProfession(ProfessionType profession) {
        return switch (profession) {
            case MINER -> ColonyProfession.MINER;
            case LUMBERJACK -> ColonyProfession.LUMBERJACK;
            case MASON -> ColonyProfession.MASON;
            case SMELTER -> ColonyProfession.SMELTER;
            case CARPENTER -> ColonyProfession.CARPENTER;
            case FARMER -> ColonyProfession.FARMER;
            case SHEPHERD -> ColonyProfession.SHEPHERD;
            case BUILDER -> ColonyProfession.BUILDER;
        };
    }

}
