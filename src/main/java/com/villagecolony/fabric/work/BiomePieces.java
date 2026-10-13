package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.coordination.AdvanceStock;
import com.villagecolony.core.type.Production;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import com.villagecolony.fabric.integration.VillageStructures;
import net.minecraft.block.Block;
import net.minecraft.server.world.ServerWorld;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * As peças que as casas do bioma da vila usam — ADR-039 item 4: o adiantamento
 * de carpinteiro, pedreiro e fundidor segue as plantas da vila (pinheiro na
 * taiga, acácia na savana, arenito no deserto), das mais usadas para as menos.
 * Sem planta lida, cada fila fica com a lista fixa de antes.
 */
public final class BiomePieces {

    static {
        ServerMemory.register(BiomePieces.class, BiomePieces::clearAll);
    }

    /** Releitura das plantas: o estilo da vila não muda de um ciclo para outro. */
    static final int REFRESH_TICKS = 6_000;

    private static final Map<UUID, Long> READ_AT = new HashMap<>();

    /** O maior uso de cada peça numa só casa do estilo da vila. */
    private static final Map<UUID, Map<ResourceType, Integer>> LARGEST = new HashMap<>();

    private BiomePieces() {
    }

    static void clearAll() {
        READ_AT.clear();
        LARGEST.clear();
    }

    /**
     * Anota, para cada fila, as peças que as casas do estilo desta vila pedem,
     * e publica a cada ciclo o teto do adiantamento ({@link AdvanceStock#ceilings}).
     */
    public static void refresh(ServerWorld world, Colony colony) {
        long now = world.getTime();
        Long last = READ_AT.get(colony.id());

        if (last == null || now - last >= REFRESH_TICKS) {
            READ_AT.put(colony.id(), now);
            readPlans(world, colony);
        }

        AdvanceStock.ceilings(colony.id(), ceilings(
                LARGEST.getOrDefault(colony.id(), Map.of()), ConstructionDemand.materialsNeededBy(colony)));
    }

    private static void readPlans(ServerWorld world, Colony colony) {
        String style = HousePlans.paletteOf(world, colony.center()).style();
        List<Blueprint> plans = new java.util.ArrayList<>();

        for (ResourceId id : VillageStructures.housesFor(style)) {
            HousePlans.READ.computeIfAbsent(id, missing -> StructureBlueprintReader.read(world, missing))
                    .ifPresent(plans::add);
        }

        lanesFrom(plans).forEach((lane, pieces) -> AdvanceStock.piecesOfTheVillage(colony.id(), lane, pieces));
        LARGEST.put(colony.id(), largestUse(plans));
    }

    /** O maior uso de cada peça numa só planta: a próxima casa é sorteada, e a maior cobre qualquer uma. */
    static Map<ResourceType, Integer> largestUse(List<Blueprint> plans) {
        Map<ResourceType, Integer> largest = new EnumMap<>(ResourceType.class);

        for (Blueprint blueprint : plans) {
            Map<ResourceType, Integer> used = new EnumMap<>(ResourceType.class);

            blueprint.blocks().forEach(block -> count(block, used));
            used.forEach((type, amount) -> largest.merge(type, amount, Math::max));
        }

        return largest;
    }

    /** A reserva fixa de cada peça das plantas, acima da maior casa; o teto geral ainda manda. */
    static final int RESERVE = 32;

    /** O teto: a maior casa do estilo, a reserva e o que a obra aberta ainda pede de cada peça. */
    static Map<ResourceType, Integer> ceilings(
            Map<ResourceType, Integer> largest, Map<ResourceId, Integer> openBuild) {

        Map<ResourceType, Integer> ceilings = new EnumMap<>(ResourceType.class);

        // E uma reserva fixa por peça (item 2 do autor, 2026-10-13): sem ela, coberta a casa
        // típica, o artesão parava enquanto nenhuma obra abria (reanálise de 13-10, C2).
        largest.forEach((type, amount) -> ceilings.put(type, amount + RESERVE));
        openBuild.forEach((id, amount) -> MinecraftTypeAdapter.toBlock(id)
                .map(Block::asItem)
                .flatMap(MinecraftTypeAdapter::toResourceType)
                .ifPresent(type -> ceilings.merge(type, amount, Integer::sum)));

        return ceilings;
    }

    /** As peças de cada fila nestas plantas, das mais usadas para as menos; fila sem peça fica de fora. */
    static Map<String, List<ResourceType>> lanesFrom(List<Blueprint> plans) {
        Map<ResourceType, Integer> used = new EnumMap<>(ResourceType.class);

        plans.forEach(blueprint -> blueprint.blocks().forEach(block -> count(block, used)));

        Map<String, List<ResourceType>> lanes = new java.util.LinkedHashMap<>();

        put(lanes, "carpenter", used, type -> type.production() == Production.CRAFTED_WOOD
                && type.group() == ResourceGroup.NONE);
        put(lanes, "mason", used, type -> type.production() == Production.CRAFTED_STONE);
        put(lanes, "smelter", used, type -> type.production() == Production.SMELTED
                && type != ResourceType.IRON_INGOT);

        return lanes;
    }

    private static void count(BlueprintBlock block, Map<ResourceType, Integer> used) {
        MinecraftTypeAdapter.toBlock(block.block())
                .map(Block::asItem)
                .flatMap(MinecraftTypeAdapter::toResourceType)
                .ifPresent(type -> used.merge(type, 1, Integer::sum));
    }

    private static void put(Map<String, List<ResourceType>> lanes, String lane, Map<ResourceType, Integer> used,
            java.util.function.Predicate<ResourceType> ofTheLane) {
        List<ResourceType> pieces = used.entrySet().stream()
                .filter(entry -> ofTheLane.test(entry.getKey()))
                .sorted(Map.Entry.<ResourceType, Integer>comparingByValue(Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .toList();

        if (!pieces.isEmpty()) {
            lanes.put(lane, pieces);
        }
    }
}
