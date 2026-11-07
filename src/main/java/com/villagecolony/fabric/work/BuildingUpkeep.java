package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.coordination.WorkClock;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ColonyChests;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A manutenção das construções — Regra 221: sem tarefa, o carpinteiro e o pedreiro visitam uma construção por vez e gastam
 * material do estoque nela, uma vez por dia em cada uma. Com obra aberta ou estoque abaixo da reserva ninguém gasta: a obra vem
 * primeiro. O mundo não muda: quem repõe bloco que falta é o reparo.
 */
public final class BuildingUpkeep {

    /** O material gasto por visita. */
    static final int ITEMS = 4;
    /** Abaixo disto o estoque fica para a obra. */
    static final int RESERVE = 32;
    /** Até onde ele aceita caminhar até a construção. */
    private static final int REACH = 48;
    /** A esta distância da caixa da construção ele já chegou. */
    private static final int AT_THE_WALL = 3;
    private static final int GIVE_UP_AFTER = 1200;
    private static final float SPEED = 0.5F;

    private record Visit(UUID colonyId, Building building, BlockPos spot, int range, ResourceGroup material, long deadline) {
    }

    private static final Map<UUID, Visit> VISITS = new HashMap<>();
    /** O dia em que cada construção recebeu a última visita. */
    private static final Map<UUID, Long> KEPT_ON = new HashMap<>();

    static {
        ServerMemory.register(BuildingUpkeep.class, () -> {
            VISITS.clear();
            KEPT_ON.clear();
        });
    }

    private BuildingUpkeep() {
    }

    static boolean isVisiting(UUID workerId) {
        return VISITS.containsKey(workerId);
    }

    /**
     * Manda o trabalhador à construção mais próxima que ainda não teve visita hoje.
     *
     * @return 1 se ele foi, 0 se não há o que fazer
     */
    static int start(ServerWorld world, Colony colony, VillagerEntity worker, ResourceGroup material) {
        if (VillageColonyMod.CONSTRUCTIONS.openOf(colony.id()).isPresent()
                || ColonyChests.countGroup(world, chests(world, colony), material) < RESERVE + ITEMS) {
            return 0;
        }

        long day = Math.floorDiv(world.getTimeOfDay(), (long) WorkClock.DAY);
        Optional<Building> next = VillageColonyMod.BUILDINGS.ofColony(colony.id()).stream()
                .filter(Building::finished)
                .filter(building -> KEPT_ON.getOrDefault(building.id(), Long.MIN_VALUE) != day)
                .filter(building -> distanceTo(worker, building) <= REACH)
                .min(Comparator.comparingDouble(building -> distanceTo(worker, building)));

        if (next.isEmpty()) {
            return 0;
        }

        Building building = next.get();
        ColonyPos low = building.min();
        ColonyPos high = building.max();
        BlockPos spot = new BlockPos((low.x() + high.x()) / 2, low.y(), (low.z() + high.z()) / 2);
        int range = Math.max(high.x() - low.x(), high.z() - low.z()) / 2 + AT_THE_WALL - 1;

        KEPT_ON.put(building.id(), day);
        VISITS.put(worker.getUuid(), new Visit(colony.id(), building, spot, range, material, world.getTime() + GIVE_UP_AFTER));
        worker.getBrain().remember(MemoryModuleType.WALK_TARGET, new WalkTarget(spot, SPEED, range));

        return 1;
    }

    /** Conduz quem está a caminho e gasta o material de quem chegou. */
    static void tick(ServerWorld world) {
        for (Map.Entry<UUID, Visit> entry : List.copyOf(VISITS.entrySet())) {
            UUID id = entry.getKey();
            Visit visit = entry.getValue();

            if (!(world.getEntity(id) instanceof VillagerEntity worker)
                    || !IdleWorkers.isFree(world, worker, IdleWorkers.Help.UPKEEP)
                    || world.getTime() > visit.deadline()) {
                VISITS.remove(id);
            } else if (distanceTo(worker, visit.building()) <= AT_THE_WALL) {
                VISITS.remove(id);
                spend(world, worker, visit);
            } else if (!worker.getBrain().hasMemoryModule(MemoryModuleType.WALK_TARGET)) {
                worker.getBrain().remember(MemoryModuleType.WALK_TARGET, new WalkTarget(visit.spot(), SPEED, visit.range()));
            }
        }
    }

    private static void spend(ServerWorld world, VillagerEntity worker, Visit visit) {
        Optional<Colony> colony = VillageColonyMod.COLONIES.find(visit.colonyId());

        if (colony.isEmpty() || VillageColonyMod.CONSTRUCTIONS.openOf(visit.colonyId()).isPresent()) {
            return;
        }

        List<ColonyPos> chests = chests(world, colony.get());

        if (ColonyChests.countGroup(world, chests, visit.material()) < RESERVE + ITEMS) {
            return;
        }

        int spent = ColonyChests.withdrawGroup(world, chests, visit.material(), ITEMS);

        worker.swingHand(Hand.MAIN_HAND);
        ActionJournal.action(worker.getUuid(), "UPKEEP", visit.material().name(), spent,
                visit.spot(), "kept " + visit.building().blueprint().path());
    }

    private static List<ColonyPos> chests(ServerWorld world, Colony colony) {
        return ColonyChests.nearestFirst(world, colony.id(), colony.center());
    }

    /** A distância em blocos até a caixa da construção, sem contar a altura. */
    private static double distanceTo(VillagerEntity worker, Building building) {
        BlockPos at = worker.getBlockPos();
        ColonyPos low = building.min();
        ColonyPos high = building.max();
        int dx = Math.max(Math.max(low.x() - at.getX(), at.getX() - high.x()), 0);
        int dz = Math.max(Math.max(low.z() - at.getZ(), at.getZ() - high.z()), 0);

        return Math.sqrt(dx * dx + dz * dz);
    }
}
