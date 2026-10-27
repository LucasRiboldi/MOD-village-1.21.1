package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.PathStep;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.brain.WorkHours;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.BlockProtection;
import com.villagecolony.fabric.integration.RoadIndex;
import com.villagecolony.fabric.integration.WorldPathTerrain;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Quem assenta a rede de caminhos — Regra 125 (decisão D6 do autor, 2026-10-27): o pedreiro, o
 * carpinteiro e o fundidor, quando estão sem tarefa do ofício, <b>um por vez</b> por colônia, revezando
 * a cada {@link #STEPS_PER_TURN} passos.
 *
 * <p>Cada passo é uma seção de três colunas ({@link #layStep}): o buraco é aterrado com o chão do bioma
 * até o piso (decisão D2), o morro em cima do piso é tirado, o piso vira caminho de terra (decisão D4) e
 * ficam dois blocos livres em cima. Nada que não seja chão natural é tocado (Regra 3): se o mundo mudou e
 * a seção não serve mais, o traçado sai e é refeito no ciclo seguinte.
 */
public final class PathPaving {

    static {
        ServerMemory.register(PathPaving.class, PathPaving::clearAll);
    }

    /** O rodízio de quem assenta, na ordem do autor. */
    static final List<ProfessionType> PAVERS =
            List.of(ProfessionType.MASON, ProfessionType.CARPENTER, ProfessionType.SMELTER);

    /** Passos que um assenta antes de passar a vez. */
    static final int STEPS_PER_TURN = 8;

    /** A que distância do passo ele assenta. */
    static final int REACH = 3;

    /** Quanto ele anda até um passo antes de largar a vez. */
    static final int GIVE_UP_AFTER = 600;

    /** A altura livre em cima do piso. */
    static final int CLEAR = 2;

    private static final class Turn {
        final UUID worker;
        int laid;
        int walked;

        Turn(UUID worker) {
            this.worker = worker;
        }
    }

    private static final Map<UUID, Turn> TURNS = new HashMap<>();

    /** Qual profissão teve a última vez, por colônia. */
    private static final Map<UUID, Integer> LAST = new HashMap<>();

    private PathPaving() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerWorld world = server.getOverworld();

            if (world.getTime() % 10 == 0) {
                tick(world);
            }
        });
    }

    /** Se este aldeão está assentando caminho — conta como trabalho no {@code WorkTime}. */
    public static boolean isPaving(UUID workerId) {
        return TURNS.values().stream().anyMatch(turn -> turn.worker.equals(workerId));
    }

    private static void tick(ServerWorld world) {
        long now = world.getTime();

        for (Colony colony : List.copyOf(VillageColonyMod.COLONIES.all())) {
            if (colony.isActive() && colony.isAttended(now)) {
                PathLinks.tend(world, colony);
                pass(world, colony);
            }
        }
    }

    /** Uma passagem desta colônia: segue a vez em curso, ou dá a vez ao próximo do rodízio que está livre. */
    static void pass(ServerWorld world, Colony colony) {
        Optional<PathLinks.Plan> plan = PathLinks.planOf(colony.id());

        if (plan.isEmpty()) {
            endTurn(colony.id());
            return;
        }

        Turn turn = TURNS.get(colony.id());

        if (turn != null && !(world.getEntity(turn.worker) instanceof VillagerEntity paver && isFree(world, paver))) {
            endTurn(colony.id());
            turn = null;
        }

        if (turn == null) {
            Optional<VillagerEntity> next = nextPaver(world, colony.id());

            if (next.isEmpty()) {
                return;
            }

            turn = new Turn(next.get().getUuid());
            TURNS.put(colony.id(), turn);
        }

        if (world.getEntity(turn.worker) instanceof VillagerEntity paver) {
            step(world, colony.id(), paver, turn, plan.get());
        }
    }

    private static void step(ServerWorld world, UUID colonyId, VillagerEntity paver, Turn turn, PathLinks.Plan plan) {
        PathStep step = plan.steps.get(plan.next);
        BlockPos floor = new BlockPos(step.x(), step.floorY(), step.z());

        if (!paver.getBlockPos().isWithinDistance(floor, REACH + 1)) {
            turn.walked += 10;

            if (turn.walked > GIVE_UP_AFTER) {
                endTurn(colonyId);
                return;
            }

            WorkTargets.set(paver.getUuid(), WalkLegs.towards(world, paver.getBlockPos(), floor.up()).orElse(floor.up()), 2);
            return;
        }

        if (!layStep(world, colonyId, step)) {
            VillageColonyMod.LOGGER.info("Colony {} — the path step at {} no longer fits; the path is traced again",
                    colonyId.toString().substring(0, 8), floor.toShortString());
            PathLinks.drop(colonyId);
            endTurn(colonyId);
            return;
        }

        paver.swingHand(Hand.MAIN_HAND);
        ActionJournal.action(paver.getUuid(), "PAVED", "dirt_path", 3, floor, "path network");
        plan.next++;
        turn.walked = 0;

        if (++turn.laid >= STEPS_PER_TURN || plan.done()) {
            if (plan.done()) {
                VillageColonyMod.LOGGER.info("Colony {} finished a path of {} steps (Regra 123)",
                        colonyId.toString().substring(0, 8), plan.steps.size());
            }

            endTurn(colonyId);
        }
    }

    /**
     * Assenta a seção de três colunas deste passo.
     *
     * @return se a seção coube; falso quando o mundo mudou e alguma coluna não é mais chão natural
     */
    static boolean layStep(ServerWorld world, UUID colonyId, PathStep step) {
        WorldPathTerrain terrain = new WorldPathTerrain(world, colonyId);

        for (int offset = -1; offset <= 1; offset++) {
            int x = step.sideX(offset);
            int z = step.sideZ(offset);
            OptionalInt ground = terrain.groundY(x, z, step.floorY());

            if (ground.isEmpty() || terrain.blocked(x, z)
                    || step.floorY() - ground.getAsInt() > com.villagecolony.core.construction.model.VillagePathPlanner.MAX_FILL
                    || ground.getAsInt() - step.floorY() > com.villagecolony.core.construction.model.VillagePathPlanner.MAX_CUT) {
                return false;
            }
        }

        for (int offset = -1; offset <= 1; offset++) {
            int x = step.sideX(offset);
            int z = step.sideZ(offset);
            int ground = terrain.groundY(x, z, step.floorY()).orElseThrow();
            BlockState fill = fillFor(world.getBlockState(new BlockPos(x, ground, z)));

            for (int y = ground + 1; y < step.floorY(); y++) {
                world.setBlockState(new BlockPos(x, y, z), fill);
            }

            for (int y = step.floorY() + 1; y <= step.floorY() + CLEAR; y++) {
                BlockPos above = new BlockPos(x, y, z);
                BlockState state = world.getBlockState(above);

                if (!state.isAir() && BlockProtection.mayDigOut(world, above, state)) {
                    world.breakBlock(above, false);
                }
            }

            BlockPos floor = new BlockPos(x, step.floorY(), z);
            world.setBlockState(floor, Blocks.DIRT_PATH.getDefaultState());
            RoadIndex.remember(colonyId, floor);
        }

        return true;
    }

    /** O aterro: o chão do bioma da coluna, e terra quando ele não é solo. */
    private static BlockState fillFor(BlockState ground) {
        return ground.isOf(Blocks.SAND) || ground.isOf(Blocks.RED_SAND) || ground.isOf(Blocks.SANDSTONE)
                || ground.isOf(Blocks.DIRT) || ground.isOf(Blocks.COARSE_DIRT)
                ? ground.getBlock().getDefaultState()
                : Blocks.DIRT.getDefaultState();
    }

    /** O próximo do rodízio que está livre; sem ele, o primeiro livre de qualquer um dos três. */
    private static Optional<VillagerEntity> nextPaver(ServerWorld world, UUID colonyId) {
        int last = LAST.getOrDefault(colonyId, PAVERS.size() - 1);

        for (int turn = 1; turn <= PAVERS.size(); turn++) {
            int index = (last + turn) % PAVERS.size();
            ProfessionType profession = PAVERS.get(index);

            for (Worker worker : VillageColonyMod.WORKERS.ofColony(colonyId)) {
                if (worker.profession().filter(profession::equals).isEmpty()) {
                    continue;
                }

                if (world.getEntity(worker.villagerId()) instanceof VillagerEntity villager && isFree(world, villager)) {
                    LAST.put(colonyId, index);
                    return Optional.of(villager);
                }
            }
        }

        return Optional.empty();
    }

    /** Sem tarefa do ofício, no expediente, solto e sem outra ajuda em curso. */
    private static boolean isFree(ServerWorld world, VillagerEntity villager) {
        UUID id = villager.getUuid();

        return villager.isAlive() && !villager.isSleeping()
                && VillageColonyMod.TASKS.assignedTo(id).isEmpty()
                && WorkHours.isWorkTime(world, villager)
                && !StrandedWorkers.isStranded(id) && !PenEscape.isEscaping(id)
                && !BuilderPaving.isPaving(id) && !GroundPickup.isHelping(id);
    }

    private static void endTurn(UUID colonyId) {
        Turn turn = TURNS.remove(colonyId);

        if (turn != null) {
            WorkTargets.clear(turn.worker);
        }
    }

    static void clearAll() {
        TURNS.clear();
        LAST.clear();
    }
}
