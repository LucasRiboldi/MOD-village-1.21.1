package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkHours;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.CropPatch;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * As ajudas de quem está sem tarefa do ofício — item 8 das escolhas do autor (2026-10-27), cada ajuda com
 * um ofício fixo. Por ora: <b>o pastor colhe e replanta a roça madura</b>, como o fazendeiro, sem arar
 * terra nova; a colheita vai para o baú da colônia mais perto. Um pastor por colônia por vez.
 */
public final class IdleHelps {

    static {
        ServerMemory.register(IdleHelps.class, IdleHelps::clearAll);
    }

    /** A que distância da lavoura ele colhe. */
    static final int REACH = 2;

    /** Quanto ele anda até a lavoura antes de largar. */
    static final int GIVE_UP_AFTER = 600;

    private static final class Help {
        final UUID worker;
        final BlockPos target;
        int walked;

        Help(UUID worker, BlockPos target) {
            this.worker = worker;
            this.target = target;
        }
    }

    private static final Map<UUID, Help> FIELD = new HashMap<>();

    private IdleHelps() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerWorld world = server.getOverworld();

            if (world.getTime() % 10 == 5) {
                tick(world);
            }
        });
    }

    /** Se este aldeão está numa ajuda agora — conta como trabalho no {@code WorkTime}. */
    public static boolean isHelping(UUID workerId) {
        return FIELD.values().stream().anyMatch(help -> help.worker.equals(workerId));
    }

    private static void tick(ServerWorld world) {
        long now = world.getTime();

        for (Colony colony : List.copyOf(VillageColonyMod.COLONIES.all())) {
            if (colony.isActive() && colony.isAttended(now)) {
                shepherdInTheField(world, colony);
            }
        }
    }

    /** O pastor sem tarefa colhe e replanta a lavoura madura conhecida mais perto dele. */
    static void shepherdInTheField(ServerWorld world, Colony colony) {
        Help help = FIELD.get(colony.id());

        if (help != null && !(world.getEntity(help.worker) instanceof VillagerEntity helper && isFree(world, helper))) {
            stop(colony.id());
            help = null;
        }

        if (help == null) {
            for (Worker worker : VillageColonyMod.WORKERS.ofColony(colony.id())) {
                if (worker.profession().filter(ProfessionType.SHEPHERD::equals).isEmpty()
                        || !(world.getEntity(worker.villagerId()) instanceof VillagerEntity shepherd)
                        || !isFree(world, shepherd)) {
                    continue;
                }

                Optional<BlockPos> ripe = com.villagecolony.fabric.integration.KnownFarmland.ripeNear(world, colony.id(), shepherd.getBlockPos());

                if (ripe.isEmpty()) {
                    return;
                }

                help = new Help(shepherd.getUuid(), ripe.get());
                FIELD.put(colony.id(), help);
                break;
            }
        }

        if (help == null || !(world.getEntity(help.worker) instanceof VillagerEntity shepherd)) {
            return;
        }

        if (!CropPatch.isRipe(world.getBlockState(help.target))) {
            stop(colony.id());
            return;
        }

        if (!shepherd.getBlockPos().isWithinDistance(help.target, REACH + 1)) {
            help.walked += 10;

            if (help.walked > GIVE_UP_AFTER) {
                stop(colony.id());
                return;
            }

            WorkTargets.set(shepherd.getUuid(), help.target, 1);
            return;
        }

        shepherd.swingHand(Hand.MAIN_HAND);
        int stored = harvestAt(world, colony.id(), help.target);

        ActionJournal.action(shepherd.getUuid(), "HARVESTED", "crop", stored, help.target, "helping the farmer");
        VillageColonyMod.LOGGER.info("Shepherd {} harvested {} at {} for the farmer — replanted",
                shepherd.getUuid().toString().substring(0, 8), stored, help.target.toShortString());
        stop(colony.id());
    }

    /**
     * Colhe e replanta esta lavoura, e guarda o que sobra no baú da colônia mais perto.
     *
     * @return quantos itens foram guardados
     */
    static int harvestAt(ServerWorld world, UUID colonyId, BlockPos crop) {
        BlockState state = world.getBlockState(crop);
        List<ItemStack> drops = new ArrayList<>(Block.getDroppedStacks(state, world, crop, null, null, ItemStack.EMPTY));

        CropPatch.replant(world, crop, state, drops);

        List<ColonyPos> chests = ColonyChests.nearestFirst(world, colonyId, MinecraftTypeAdapter.toColonyPos(crop));
        int stored = 0;

        for (ItemStack stack : drops) {
            int left = ColonyChests.deposit(world, chests, stack.getItem(), stack.getCount());
            stored += stack.getCount() - left;
        }

        return stored;
    }

    private static boolean isFree(ServerWorld world, VillagerEntity villager) {
        UUID id = villager.getUuid();

        return villager.isAlive() && !villager.isSleeping()
                && VillageColonyMod.TASKS.assignedTo(id).isEmpty()
                && WorkHours.isWorkTime(world, villager)
                && !StrandedWorkers.isStranded(id) && !PenEscape.isEscaping(id)
                && !ShepherdHerding.isHerding(id) && !GroundPickup.isHelping(id);
    }

    private static void stop(UUID colonyId) {
        Help help = FIELD.remove(colonyId);

        if (help != null) {
            WorkTargets.clear(help.worker);
        }
    }

    static void clearAll() {
        FIELD.clear();
    }
}
