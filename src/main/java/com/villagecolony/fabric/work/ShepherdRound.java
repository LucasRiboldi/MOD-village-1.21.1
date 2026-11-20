package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.brain.WorkHours;
import com.villagecolony.fabric.brain.WorkTargets;
import net.minecraft.block.BlockState;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.event.GameEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A ronda do pastor — Regra 329. Uma vez por dia, no expediente e antes de buscar desgarrados, ele passa pela frente
 * de cada curral da vila, conta o rebanho de dentro e fecha a porteira que ficou aberta sem ninguém passando. O curral
 * que ele não alcança em {@link #PATIENCE} tiques fica de fora da ronda. No fim, uma linha {@code VC_ROUND} diz o que
 * ele contou.
 */
final class ShepherdRound {

    static {
        ServerMemory.register(ShepherdRound.class, ShepherdRound::clearAll);
    }

    /** Trinta segundos para chegar à frente de cada curral. */
    static final long PATIENCE = 600;

    /** A que distância da frente do curral ele conta e fecha. */
    static final int REACH = 3;

    private static final long DAY_TICKS = 24_000L;

    static final class Round {
        final Colony colony;
        final List<ShepherdPens.Pen> pens;
        int next;
        long since;
        int visited;
        int skipped;
        int inside;
        int closed;
        final Map<String, Integer> species = new TreeMap<>();

        Round(Colony colony, List<ShepherdPens.Pen> pens, long since) {
            this.colony = colony;
            this.pens = pens;
            this.since = since;
        }
    }

    private static final Map<UUID, Round> ROUNDS = new HashMap<>();

    /** Pastor → o dia da última ronda. */
    private static final Map<UUID, Long> DONE = new HashMap<>();

    private ShepherdRound() {
    }

    /** O dia do mundo, que vira à meia-noite do relógio do jogo. */
    static long dayOf(ServerWorld world) {
        return Math.floorDiv(world.getTimeOfDay(), DAY_TICKS);
    }

    static boolean isWalking(UUID shepherd) {
        return ROUNDS.containsKey(shepherd);
    }

    /** Se a ronda de hoje ainda não foi feita. */
    static boolean due(ServerWorld world, UUID shepherd) {
        return !ROUNDS.containsKey(shepherd) && DONE.getOrDefault(shepherd, Long.MIN_VALUE) != dayOf(world);
    }

    /**
     * Começa a ronda de hoje.
     *
     * @return se há curral a visitar; sem curral, o dia conta como feito
     */
    static boolean start(ServerWorld world, Colony colony, VillagerEntity shepherd) {
        DONE.put(shepherd.getUuid(), dayOf(world));
        List<ShepherdPens.Pen> pens = ShepherdPens.pensOf(world, colony);

        if (pens.isEmpty()) {
            return false;
        }

        ROUNDS.put(shepherd.getUuid(), new Round(colony, pens, world.getTime()));
        return true;
    }

    /** Um passo de cada ronda, a cada cinco tiques. */
    static void tick(ServerWorld world) {
        if (ROUNDS.isEmpty() || world.getTime() % 5 != 0) {
            return;
        }

        ROUNDS.entrySet().removeIf(entry -> !step(world, entry.getKey(), entry.getValue()));
    }

    /**
     * Um passo da ronda.
     *
     * @return se ela continua
     */
    static boolean step(ServerWorld world, UUID shepherdId, Round round) {
        if (!(world.getEntity(shepherdId) instanceof VillagerEntity shepherd) || !WorkHours.isWorkTime(world, shepherd)) {
            WorkTargets.clear(shepherdId);
            return false;
        }

        if (round.next >= round.pens.size()) {
            report(world, shepherd, round);
            WorkTargets.clear(shepherdId);
            return false;
        }

        ShepherdPens.Pen pen = round.pens.get(round.next);
        BlockPos front = ShepherdPens.nearestAccess(world, pen, shepherd.getBlockPos(), access -> true)
                .map(ShepherdPens.Pen::outside).orElse(pen.outside());

        if (shepherd.getBlockPos().isWithinDistance(front, REACH)) {
            visit(world, round, pen);
            round.visited++;
            round.next++;
            round.since = world.getTime();
            return true;
        }

        if (world.getTime() - round.since > PATIENCE) {
            round.skipped++;
            round.next++;
            round.since = world.getTime();
            return true;
        }

        ShepherdLead.walkTo(world, shepherd, front, REACH - 1);
        return true;
    }

    /** Conta o rebanho de dentro e fecha a porteira aberta sem ninguém nela. */
    static void visit(ServerWorld world, Round round, ShepherdPens.Pen pen) {
        for (Map.Entry<EntityType<?>, Integer> kind : ShepherdPens.speciesIn(world, pen).entrySet()) {
            round.species.merge(Registries.ENTITY_TYPE.getId(kind.getKey()).getPath(), kind.getValue(), Integer::sum);
            round.inside += kind.getValue();
        }

        for (ShepherdPens.Access access : pen.accesses()) {
            if (closeIfLeftOpen(world, access.gate())) {
                round.closed++;
            }
        }
    }

    /** A porteira aberta que ninguém está atravessando e que nenhuma passagem segura fecha (Regra 329). */
    static boolean closeIfLeftOpen(ServerWorld world, BlockPos gate) {
        BlockState state = world.getBlockState(gate);

        if (!(state.getBlock() instanceof FenceGateBlock) || !state.get(FenceGateBlock.OPEN)
                || FencedIn.isHeldOpen(gate)
                || !world.getEntitiesByClass(Entity.class, new Box(gate), Entity::isAlive).isEmpty()) {
            return false;
        }

        world.setBlockState(gate, state.with(FenceGateBlock.OPEN, false));
        world.playSound(null, gate, SoundEvents.BLOCK_FENCE_GATE_CLOSE, SoundCategory.BLOCKS, 1.0F, 1.0F);
        world.emitGameEvent(null, GameEvent.BLOCK_CLOSE, gate);
        return true;
    }

    private static void report(ServerWorld world, VillagerEntity shepherd, Round round) {
        int strays = HerdCandidates.of(world, round.colony, shepherd).size();
        String species = round.species.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue()).collect(Collectors.joining(","));

        VillageColonyMod.LOGGER.info(
                "VC_ROUND version=1 colony={} worker={} day={} pens={} visited={} skipped={} inside={} strays={} closed={} species={}",
                round.colony.id().toString().substring(0, 8), shepherd.getUuid().toString().substring(0, 8),
                dayOf(world), round.pens.size(), round.visited, round.skipped, round.inside, strays, round.closed,
                species.isEmpty() ? "-" : species);
    }

    static void clearAll() {
        ROUNDS.clear();
        DONE.clear();
    }
}
