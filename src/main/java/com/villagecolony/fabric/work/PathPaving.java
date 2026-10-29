package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.PathStep;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.brain.WorkHours;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.core.construction.model.VillagePathPlanner;
import com.villagecolony.fabric.content.ModContent;
import com.villagecolony.fabric.integration.BlockProtection;
import com.villagecolony.fabric.integration.LotGround;
import com.villagecolony.fabric.integration.RoadIndex;
import com.villagecolony.fabric.integration.StreetSurface;
import com.villagecolony.fabric.integration.WorldPathTerrain;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.tag.BlockTags;
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
 * <p>Cada passo é uma coluna de caminho ({@link #layStep}, 1 de largura desde 2026-10-28), <b>rente ao chão</b> (autor,
 * 2026-10-28): o bloco de cima vira caminho; o buraco de um bloco ganha o caminho por cima; o corte é de
 * um bloco no máximo; por cima só sai planta, nunca terreno. Nunca nasce bloco acima do chão natural —
 * na 0.3.98 o aterro de até 4 por coluna levantou paredes de terra. Nada que não seja chão natural é
 * tocado (Regra 3): se o mundo mudou e o eixo não serve mais, o traçado sai e é refeito.
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
     * Assenta o passo: só o eixo — o caminho da porta até a rua tem 1 bloco de largura (autor, 2026-10-28, depois
     * do jogo da 0.3.104). A rua de 3 é a da Regra 140 ({@code RoadPaving}).
     *
     * @return se o eixo coube; falso quando o mundo mudou e ele não é mais chão a no máximo um bloco do piso
     */
    static boolean layStep(ServerWorld world, UUID colonyId, PathStep step) {
        WorldPathTerrain terrain = new WorldPathTerrain(world, colonyId);
        OptionalInt axis = terrain.groundY(step.x(), step.z(), step.floorY());

        if (axis.isEmpty() || terrain.blocked(step.x(), step.z())
                || step.floorY() - axis.getAsInt() > VillagePathPlanner.MAX_FILL
                || axis.getAsInt() - step.floorY() > VillagePathPlanner.MAX_CUT) {
            return false;
        }

        pave(world, colonyId, step.x(), step.z(), axis.getAsInt(), step.floorY(), false);

        return true;
    }

    /**
     * Uma coluna de caminho com o piso em {@code floorY} sobre o chão em {@code ground}: no chão, o bloco de
     * cima vira caminho; um abaixo, o bloco de cima sai (o corte); um acima, o caminho vai por cima do
     * buraco. Por cima do piso só sai planta.
     */
    static void pave(ServerWorld world, UUID colonyId, int x, int z, int ground, int floorY, boolean edge) {
        BlockPos top = new BlockPos(x, ground, z);
        BlockState surface = world.getBlockState(top);
        BlockPos floor = new BlockPos(x, floorY, z);

        if (floorY < ground) {
            if (!BlockProtection.mayDigOut(world, top, surface)) {
                return;
            }

            world.breakBlock(top, false);
        }

        if (floorY > ground && !LotGround.isNothing(world.getBlockState(floor))) {
            return;
        }

        for (int y = floorY + 1; y <= floorY + CLEAR; y++) {
            BlockPos above = new BlockPos(x, y, z);
            BlockState state = world.getBlockState(above);

            if (!state.isAir() && LotGround.isNothing(state) && state.getFluidState().isEmpty()) {
                world.breakBlock(above, false);
            }
        }

        // Meio e borda são o caminho do chão (StreetSurface, autor 2026-10-28).
        world.setBlockState(floor, edge ? StreetSurface.edge(world, floor, surface) : StreetSurface.center(world, floor, surface));
        RoadIndex.remember(colonyId, floor);
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
