package com.villagecolony.fabric.integration;

import com.villagecolony.core.resource.model.ColonyResources;
import com.villagecolony.core.resource.model.ResourceTally;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.type.ServerMemory;
import net.minecraft.server.world.ServerWorld;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Monta a fotografia física de uma colônia em fatias curtas.
 *
 * <p>O estado é somente o cursor de leitura e os resultados já lidos da
 * rodada atual; ele não é estoque virtual e nunca vai ao disco. Enquanto
 * a rodada não fecha, o chamador recebe uma {@code ChestSurvey} parcial
 * formada somente pelos baús já observados; a integração pode usá-la sem
 * inferir conteúdo dos baús ainda pendentes.
 */
public final class ColonyChestSurvey {

    /** Limite por colônia e por ciclo; evita uma vila grande dominar o tick. */
    public static final int CHESTS_PER_CYCLE = 8;

    /**
     * Por quanto tempo a última fotografia completa vale — F13 e pedido do
     * autor, 2026-09-30: ler os baús no máximo uma vez por minuto e guardar a
     * lista em memória. Enquanto ela valer e a lista de baús não mudar, o
     * ciclo decide com ela sem reler nenhum baú.
     */
    public static final int SURVEY_INTERVAL_TICKS = 1_200;

    private static final Map<UUID, Completed> COMPLETED = new HashMap<>();

    private record Completed(
            List<ColonyPos> chests, List<ResourceGroup> groups,
            ChestInventoryReader.ChestSurvey survey, long at) {
    }

    private static final Map<UUID, Round> ROUNDS = new HashMap<>();

    static {
        ServerMemory.register(ColonyChestSurvey.class, ColonyChestSurvey::clearAll);
    }

    private ColonyChestSurvey() {
    }

    /** Avança uma rodada e só devolve uma fotografia completa na última fatia. */
    public static ChestInventoryReader.ChestSurvey advance(
            ServerWorld world,
            UUID colonyId,
            List<ColonyPos> chests,
            ResourceGroup... capacityGroups) {

        return advance(world, colonyId, chests, Set.of(), capacityGroups);
    }

    /** Baús compartilhados são observados antes dos reservados a profissões. */
    public static ChestInventoryReader.ChestSurvey advance(
            ServerWorld world,
            UUID colonyId,
            List<ColonyPos> chests,
            Set<ColonyPos> professionChests,
            ResourceGroup... capacityGroups) {

        ProfessionChestOverflow.relieve(world, chests, professionChests);

        List<ColonyPos> knownChests = chests.stream()
                .sorted((first, second) -> Boolean.compare(
                        professionChests.contains(first), professionChests.contains(second)))
                .toList();
        List<ResourceGroup> groups = List.copyOf(Arrays.asList(capacityGroups));

        Completed done = COMPLETED.get(colonyId);

        if (done != null
                && world.getTime() - done.at() < SURVEY_INTERVAL_TICKS
                && done.chests().equals(knownChests)
                && done.groups().equals(groups)) {
            return done.survey();
        }
        Round round = ROUNDS.get(colonyId);

        if (round == null || !round.matches(knownChests, groups)) {
            round = new Round(knownChests, groups);
            ROUNDS.put(colonyId, round);
        }

        int end = Math.min(round.next + CHESTS_PER_CYCLE, knownChests.size());
        ChestInventoryReader.ChestSurvey slice = ChestInventoryReader.survey(
                world,
                knownChests.subList(round.next, end),
                capacityGroups);
        round.add(slice);
        round.next = end;

        int pending = knownChests.size() - end;
        ChestInventoryReader.ChestSurvey result = round.snapshot(pending);

        if (pending == 0) {
            ROUNDS.remove(colonyId);
            COMPLETED.put(colonyId, new Completed(knownChests, groups, result, world.getTime()));
        }

        return result;
    }

    /** Esquece uma rodada interrompida pela remoção ou troca de estado da colônia. */
    public static void forget(UUID colonyId) {
        ROUNDS.remove(colonyId);
        COMPLETED.remove(colonyId);
    }

    /** Esquece as rodadas transitórias ao parar o servidor. */
    public static void clearAll() {
        ROUNDS.clear();
        COMPLETED.clear();
    }

    private static final class Round {
        private final List<ColonyPos> chests;
        private final List<ResourceGroup> groups;
        private final Map<ColonyPos, ResourceTally> byChest = new LinkedHashMap<>();
        private final Map<ResourceGroup, Integer> freeSpace = new EnumMap<>(ResourceGroup.class);
        private int next;
        private int read;
        private int unreachable;

        private Round(List<ColonyPos> chests, List<ResourceGroup> groups) {
            this.chests = chests;
            this.groups = groups;
            for (ResourceGroup group : groups) {
                freeSpace.put(group, 0);
            }
        }

        private boolean matches(List<ColonyPos> currentChests, List<ResourceGroup> currentGroups) {
            return chests.equals(currentChests) && groups.equals(currentGroups);
        }

        private void add(ChestInventoryReader.ChestSurvey slice) {
            byChest.putAll(slice.resources().byChest());
            read += slice.chestsRead();
            unreachable += slice.chestsUnreachable();
            for (ResourceGroup group : groups) {
                freeSpace.merge(group, slice.freeSpaceForGroup(group), Integer::sum);
            }
        }

        private ChestInventoryReader.ChestSurvey snapshot(int pending) {
            return new ChestInventoryReader.ChestSurvey(
                    ColonyResources.of(byChest), freeSpace, read, unreachable, pending);
        }
    }
}
