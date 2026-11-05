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
import com.villagecolony.fabric.integration.MaeRoadAccess;
import com.villagecolony.fabric.integration.RoadIndex;
import com.villagecolony.fabric.integration.StreetSurface;
import com.villagecolony.fabric.integration.WorldPathTerrain;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.Registries;
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
 * Quem assenta a rede de caminhos: somente o construtor, quando não há obra aberta.
 * Os demais ofícios permanecem na própria atividade.
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

    /** Construção de rua não desvia os ofícios produtivos. */
    static final List<ProfessionType> PAVERS =
            List.of(ProfessionType.BUILDER);

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
                // Coluna de reparo que ele não alcança sai do plano; o traçado de porta é refeito.
                if (plan.repair) {
                    skipRepairStep(colonyId, turn, plan, step);
                }

                endTurn(colonyId);
                return;
            }

            WorkTargets.set(paver.getUuid(), WalkLegs.towards(world, paver.getBlockPos(), floor.up()).orElse(floor.up()), 2);
            return;
        }

        if (BridgeDeck.isOverWater(world, floor)) {
            if (!BridgeDeck.lay(world, colonyId, paver.getUuid(), step, plan.steps)) {
                // Sem tábua nos baús: a ponte espera; a vez passa e ele volta a tentar.
                endTurn(colonyId);
                return;
            }

            paver.swingHand(Hand.MAIN_HAND);
            plan.next++;
            turn.walked = 0;
            if (plan.done()) {
                endTurn(colonyId);
            }
            return;
        }

        if (plan.repair) {
            com.villagecolony.fabric.integration.TreeClearing.openColumn(world, step.x(), step.z(), step.floorY(),
                    com.villagecolony.fabric.integration.ColonyChests.nearestFirst(world, colonyId,
                            new com.villagecolony.core.type.ColonyPos(step.x(), step.floorY(), step.z())));
        }

        if (!layAndRecord(world, colonyId, paver.getUuid(), step)) {
            if (plan.repair) {
                skipRepairStep(colonyId, turn, plan, step);
                return;
            }

            VillageColonyMod.LOGGER.info("Colony {} — the path step at {} no longer fits; the path is traced again",
                    colonyId.toString().substring(0, 8), floor.toShortString());
            PathLinks.drop(colonyId);
            endTurn(colonyId);
            return;
        }

        paver.swingHand(Hand.MAIN_HAND);
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

    private static void skipRepairStep(UUID colonyId, Turn turn, PathLinks.Plan plan, PathStep step) {
        RoadGraphRefresh.refused(colonyId, step);
        plan.next++;
        turn.walked = 0;

        if (plan.done()) {
            endTurn(colonyId);
        }
    }

    /** O diário conta apenas o piso realmente alterado, uma vez por coluna — ADR-043. */
    static boolean layAndRecord(ServerWorld world, UUID colonyId, UUID workerId, PathStep step) {
        BlockPos floor = new BlockPos(step.x(), step.floorY(), step.z());
        BlockState before = world.getBlockState(floor);
        if (!layStep(world, colonyId, step)) {
            return false;
        }
        BlockState after = world.getBlockState(floor);
        if (!before.equals(after)) {
            ActionJournal.action(workerId, "PAVED", Registries.BLOCK.getId(after.getBlock()).getPath(), 1,
                    floor, "path network");
        }
        return true;
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

        return pave(world, colonyId, step.x(), step.z(), axis.getAsInt(), step.floorY(), false);
    }

    /**
     * Uma coluna de caminho com o piso em {@code floorY} sobre o chão em {@code ground}: no chão, o bloco de
     * cima vira caminho; um abaixo, o bloco de cima sai (o corte); um acima, o caminho vai por cima do
     * buraco. Por cima do piso só sai planta.
     */
    static boolean pave(ServerWorld world, UUID colonyId, int x, int z, int ground, int floorY, boolean edge) {
        if (!MaeRoadAccess.legacyAllowed(world, new BlockPos(x, Math.min(ground, floorY), z),
                new BlockPos(x, Math.max(ground + 2, floorY + CLEAR + 1), z))) return false;
        BlockPos top = new BlockPos(x, ground, z);
        BlockState surface = world.getBlockState(top);
        BlockPos floor = new BlockPos(x, floorY, z);

        // Toda recusa precede a primeira escrita; caminho exige apoio físico — ADR-043.
        BlockPos support = floor.down();
        if (!world.getBlockState(support).isSideSolidFullSquare(world, support, net.minecraft.util.math.Direction.UP)
                || !BlockProtection.mayBuildOver(world, floor, world.getBlockState(floor))
                || (floorY < ground && !BlockProtection.mayDigOut(world, top, surface))
                || (floorY > ground && !LotGround.isNothing(world.getBlockState(floor)))) {
            return false;
        }
        for (int y = floorY + 1; y <= floorY + CLEAR; y++) {
            BlockPos above = new BlockPos(x, y, z);
            BlockState state = world.getBlockState(above);
            if (!state.getFluidState().isEmpty()
                    || (!state.isAir() && !LotGround.isNothing(state)
                    && !(floorY < ground && above.equals(top) && BlockProtection.mayDigOut(world, top, surface)))) {
                return false;
            }
        }

        if (floorY < ground) {
            world.breakBlock(top, false);
        }

        for (int y = floorY + 1; y <= floorY + CLEAR; y++) {
            BlockPos above = new BlockPos(x, y, z);
            BlockState state = world.getBlockState(above);

            if (!state.isAir() && LotGround.isNothing(state) && state.getFluidState().isEmpty()) {
                world.breakBlock(above, false);
            }
        }

        // Meio e borda são o caminho do chão (StreetSurface, autor 2026-10-28).
        BlockState paving = edge ? StreetSurface.edge(world, floor, surface) : StreetSurface.center(world, floor, surface);
        if (!world.getBlockState(floor).equals(paving) && !world.setBlockState(floor, paving)) {
            return false;
        }
        RoadIndex.remember(colonyId, floor);
        return true;
    }

    /** O construtor livre recebe a próxima etapa da rua. */
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
