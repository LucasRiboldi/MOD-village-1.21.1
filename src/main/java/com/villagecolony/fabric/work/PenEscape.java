package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.brain.WalkOverride;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * O aldeão sai do curral: abre o portão ou pula a cerca — E52, 2026-10-01.
 *
 * <p><b>Por que não basta a fuga do encalhado.</b> O primeiro conserto
 * (48d77ac) pendurava a saída do curral no E47, que só marca quem congela
 * duas vezes no mesmo bloco. Na sessão das 09:16, um mineiro e dois
 * construtores ficaram no curral sete minutos sem nunca serem marcados — quem
 * desiste da tarefa ou anda encostado na cerca não congela. E o único marcado
 * abriu o portão e foi solto um segundo depois, ainda dentro: a busca, feita
 * de novo com o portão aberto, passava por ele. Ver {@link FencedIn}.
 *
 * <p><b>O que esta classe faz.</b> A cada dois segundos, cada trabalhador
 * perto de uma cerca é medido pelo {@link FencedIn}. Cercado, ele larga o
 * ofício e anda até a saída mais perto:
 *
 * <ol>
 *   <li><b>portão</b> — abre, atravessa, e o portão fecha atrás dele;
 *   <li><b>cerca ou muro</b>, quando não há portão ou o portão não serviu —
 *       pula. Sobe reto até passar da altura da cerca (um bloco e meio) e só
 *       então vai para a frente, para não bater no poste na subida.
 * </ol>
 *
 * <p><b>Fora é sair do que ele alcançava com o portão fechado</b>, medido uma
 * vez, no começo — nunca perguntar de novo de dentro. Cada saída tem
 * {@link #ATTEMPT_TICKS} tiques; esgotadas todas, a medição recomeça do zero,
 * e ele nunca é dado como perdido.
 *
 * <p><b>E o golem da vila</b> — 2026-10-02. Ver {@link PenGolems}: ele pula a
 * cerca do mesmo jeito, e só usa porteira dupla, porque não cabe na simples.
 */
public final class PenEscape {

    static {
        ServerMemory.register(PenEscape.class, PenEscape::clearAll);
    }

    /** De quanto em quanto tempo a varredura passa. */
    static final int SCAN_EVERY = 20;

    /** Em quantas levas os trabalhadores se dividem: cada um é medido a cada 40 tiques. */
    private static final int SCAN_GROUPS = 2;

    /** Quanto tempo cada saída tem antes de ele tentar a seguinte. */
    static final int ATTEMPT_TICKS = 400;

    /** Quantos pulos na mesma cerca antes de tentar outra. */
    private static final int MAX_JUMPS = 3;

    private enum Phase { APPROACH, THROUGH, AIRBORNE }

    private static final class Escape {

        final Set<BlockPos> reach;

        final List<FencedIn.Exit> exits;

        final long began;

        int index;

        Phase phase = Phase.APPROACH;

        long since;

        final PenMoves.Motion motion = new PenMoves.Motion();

        int jumps;

        boolean opened;

        /** A outra folha da porteira dupla, aberta junto, para o corpo largo do golem. */
        BlockPos twin;

        Escape(Set<BlockPos> reach, List<FencedIn.Exit> exits, long now) {
            this.reach = reach;
            this.exits = exits;
            this.began = now;
            this.since = now;
        }

        FencedIn.Exit exit() {
            return exits.get(index);
        }
    }

    private static final Map<UUID, Escape> ESCAPES = new HashMap<>();

    /** Onde cada um estava da última vez que a medição disse "solto" — não se mede de novo parado. */
    private static final Map<UUID, BlockPos> SEEN_FREE = new HashMap<>();

    /** Quantas vezes cada um saiu de um cercado nesta sessão: o log diz se ele volta a entrar. */
    private static final Map<UUID, Integer> TIMES = new HashMap<>();

    private PenEscape() {
    }

    /** A cada tique: os que estão saindo andam; a cada {@link #SCAN_EVERY}, uma leva é medida. */
    public static void tick(ServerWorld world) {
        if (!ESCAPES.isEmpty()) {
            for (UUID id : List.copyOf(ESCAPES.keySet())) {
                drive(world, id);
            }
        }

        if (world.getTime() % SCAN_EVERY != 0) {
            return;
        }

        long group = (world.getTime() / SCAN_EVERY) % SCAN_GROUPS;

        for (Worker worker : List.copyOf(VillageColonyMod.WORKERS.all())) {
            UUID id = worker.villagerId();

            if (ESCAPES.containsKey(id) || Math.floorMod(id.hashCode(), SCAN_GROUPS) != group
                    || !isWorking(world, worker.colonyId())) {
                continue;
            }

            if (world.getEntity(id) instanceof VillagerEntity villager) {
                check(world, villager);
            }
        }

        PenGolems.scan(world, group, SCAN_GROUPS);
    }

    // O mesmo que VillageFocus.isWorking: sem jogador (GameTest), sempre; com
    // jogador, a vila ativa e atendida. Repetido aqui de propósito — chamar o
    // pacote event daqui faria crescer o ciclo work ↔ event que o ArchUnit
    // mantém congelado.
    static boolean isWorking(ServerWorld world, UUID colonyId) {
        return world.getPlayers().isEmpty() || VillageColonyMod.COLONIES.find(colonyId)
                .filter(colony -> colony.isActive() && colony.isAttended(world.getTime()))
                .isPresent();
    }

    /** Se ele está saindo de um cercado agora. */
    static boolean isEscaping(UUID villagerId) {
        return ESCAPES.containsKey(villagerId);
    }

    /** Em que pé está a saída dele, para o teste dizer onde parou. */
    static String stateOf(UUID villagerId) {
        Escape escape = ESCAPES.get(villagerId);

        if (escape == null) {
            return "none";
        }

        return escape.phase + " via " + describe(escape.exit()) + " (exit " + (escape.index + 1) + "/"
                + escape.exits.size() + ", jumps " + escape.jumps + ")";
    }

    /**
     * Mede se ele está num cercado com saída por portão ou cerca e, se está,
     * começa a saída.
     *
     * @return se ele está saindo de um cercado — começou agora ou já estava
     */
    static boolean check(ServerWorld world, MobEntity villager) {
        UUID id = villager.getUuid();

        if (ESCAPES.containsKey(id)) {
            return true;
        }

        BlockPos feet = villager.getBlockPos();

        // De pé, e não no meio de um pulo: o chão debaixo dos pés segura.
        if (!villager.isAlive() || villager.isSleeping() || villager.hasVehicle()
                || world.getBlockState(feet.down()).getCollisionShape(world, feet.down()).isEmpty()) {
            return false;
        }

        if (feet.equals(SEEN_FREE.get(id)) || !FencedIn.isNearAFence(world, feet)) {
            return false;
        }

        FencedIn.Result pen = FencedIn.check(world, feet);
        List<FencedIn.Exit> exits = pen.enclosed() ? FencedIn.exits(world, pen, feet) : List.of();

        if (PenGolems.isWide(villager)) {
            // Porteira simples não passa o golem: só a dupla, e a cerca.
            exits = exits.stream()
                    .filter(exit -> !exit.gate() || PenGolems.twinOf(world, exit).isPresent())
                    .toList();
        }

        if (exits.isEmpty()) {
            // Solto, ou preso por outra coisa que não cerca — buraco é do E47.
            SEEN_FREE.put(id, feet.toImmutable());

            return false;
        }

        // Para onde ia, antes de a fuga trocar o destino — estudo de 01-10, §8-A.
        PenEntryLog.record(world, villager, who(villager), feet, SEEN_FREE.remove(id));

        Escape escape = new Escape(pen.reach(), exits, world.getTime());
        ESCAPES.put(id, escape);

        if (villager instanceof VillagerEntity) {
            WalkOverride.hold(id);

            // Preso é preso: larga o trabalho e sai da escala até sair — quem o
            // devolve é a passagem do encalhado, que o vê fora do curral. Sem
            // isto o ofício seguia mandando nele, e recebia tarefa nova no meio.
            StrandedWorkers.strandNow(id, feet, "fenced in");
        }

        long gates = exits.stream().filter(FencedIn.Exit::gate).count();

        VillageColonyMod.LOGGER.info(
                "{} {} is fenced in at {} ({} gate(s), {} fence spot(s) to jump; {} time this"
                        + " session) — heading for {}",
                who(villager), shortId(id), feet.toShortString(), gates, exits.size() - gates,
                TIMES.merge(id, 1, Integer::sum), describe(escape.exit()));

        return true;
    }

    private static void drive(ServerWorld world, UUID id) {
        Escape escape = ESCAPES.get(id);

        if (!(world.getEntity(id) instanceof MobEntity villager) || !villager.isAlive()) {
            // Chunk descarregado ou aldeão morto: a medição recomeça quando ele voltar.
            finish(id, escape, null);

            return;
        }

        if (villager.isSleeping()) {
            return;
        }

        long now = world.getTime();
        FencedIn.Exit exit = escape.exit();
        BlockPos feet = villager.getBlockPos();

        if (isOut(world, villager, escape, exit)) {
            VillageColonyMod.LOGGER.info(
                    "{} {} is out of the pen at {} — {} after {} ticks",
                    who(villager), shortId(id), feet.toShortString(),
                    exit.gate() ? "through the gate at " + exit.barrier().toShortString()
                            : "jumped the fence at " + exit.barrier().toShortString(),
                    now - escape.began);

            finish(id, escape, villager);

            return;
        }

        if (now - escape.since > ATTEMPT_TICKS
                || (escape.phase == Phase.APPROACH && escape.jumps >= MAX_JUMPS)) {
            nextExit(world, id, escape, villager);

            return;
        }

        switch (escape.phase) {
            case AIRBORNE -> {
                if (PenMoves.fly(villager, escape.motion, exit, now)) {
                    // Pousou ainda dentro: chega de novo e pula de novo.
                    escape.phase = Phase.APPROACH;
                }
            }
            case THROUGH -> {
                // Alguém fechou o portão no meio da passagem: abre de novo.
                FencedIn.open(world, exit.barrier());

                if (escape.twin != null) {
                    FencedIn.open(world, escape.twin);
                }

                PenMoves.walk(world, villager, escape.motion, exit.outside(), floor(escape), now);
            }
            case APPROACH -> {
                if (!approach(world, villager, escape, exit, now)) {
                    nextExit(world, id, escape, villager);
                }
            }
        }
    }

    // Anda até a saída e, chegando, abre o portão ou pula. Devolve false
    // quando esta saída não serve mais — o portão sumiu, ou não há caminho.
    private static boolean approach(
            ServerWorld world, MobEntity villager, Escape escape, FencedIn.Exit exit, long now) {

        PenMoves.Walk walk = PenMoves.walk(world, villager, escape.motion, exit.inside(), floor(escape), now);

        if (walk == PenMoves.Walk.NO_WAY) {
            return false;
        }

        BlockPos feet = villager.getBlockPos();

        if (exit.gate()) {
            boolean beside = walk == PenMoves.Walk.ARRIVED || feet.equals(exit.inside())
                    || PenMoves.horizontalDistance(villager.getPos(), Vec3d.ofBottomCenter(exit.barrier())) <= 1.3;

            if (!beside) {
                return true;
            }

            Optional<BlockPos> twin = PenGolems.isWide(villager) ? PenGolems.twinOf(world, exit) : Optional.empty();

            if (!FencedIn.open(world, exit.barrier()) || (twin.isPresent() && !FencedIn.open(world, twin.get()))) {
                return false;
            }

            escape.opened = true;
            escape.twin = twin.orElse(null);
            escape.phase = Phase.THROUGH;
            // Pelo vão do portão até o lado de fora, em linha reta — o golem,
            // pela junta das duas folhas.
            escape.motion.follow(List.of(exit.barrier(), exit.outside()), exit.outside(), now);
            twin.ifPresent(other -> escape.motion.shift(PenGolems.towards(exit.barrier(), other)));

            return true;
        }

        if (walk != PenMoves.Walk.ARRIVED || !feet.equals(exit.inside()) || !villager.isOnGround()) {
            return true;
        }

        PenMoves.jump(villager, escape.motion, now);
        escape.phase = Phase.AIRBORNE;
        escape.jumps++;

        return true;
    }

    // O chão da saída: o que ele alcança de dentro e, com o portão aberto, o
    // vão do portão.
    private static Set<BlockPos> floor(Escape escape) {
        if (!escape.opened) {
            return escape.reach;
        }

        Set<BlockPos> floor = new HashSet<>(escape.reach);
        floor.add(escape.exit().barrier());

        return floor;
    }

    // Fora: de pé, num bloco que ele não alcançava de dentro, e não em cima
    // de cerca nem dentro do bloco de uma.
    //
    // <b>Dentro do bloco da cerca não é fora</b> — teste de mutação, 01-10. O
    // poste da cerca e o portão fechado têm um quarto de bloco de largura: o
    // aldeão encostado neles pelo lado de dentro tem o centro já no bloco da
    // cerca. Com o portão que não abria, ele foi dado como fora assim, ainda
    // no curral — o mesmo defeito das 09:19 por outra porta.
    private static boolean isOut(ServerWorld world, MobEntity villager, Escape escape, FencedIn.Exit exit) {
        BlockPos feet = villager.getBlockPos();

        if (!villager.isOnGround() || escape.reach.contains(feet) || feet.equals(exit.barrier())) {
            return false;
        }

        return !FencedIn.isBarrier(world.getBlockState(feet)) && !FencedIn.isBarrier(world.getBlockState(feet.down()));
    }

    private static void nextExit(ServerWorld world, UUID id, Escape escape, MobEntity villager) {
        FencedIn.Exit failed = escape.exit();

        if (escape.opened) {
            closeGates(escape, failed);
        }

        escape.index++;

        if (escape.index >= escape.exits.size()) {
            VillageColonyMod.LOGGER.warn(
                    "{} {} is still fenced in at {} after trying {} way(s) out — measuring the pen"
                            + " again",
                    who(villager), shortId(id), villager.getBlockPos().toShortString(), escape.exits.size());

            finish(id, escape, villager);

            return;
        }

        // Onde ele estava de verdade quando desistiu desta saída — 2026-10-02.
        // O 862b0a6b falhou todas as saídas por 7 minutos na sessão de 01-10
        // sem que o log dissesse por quê; a posição fracionária, o chão e o
        // passo do caminho separam "não anda", "anda e não chega" e "não pula".
        VillageColonyMod.LOGGER.info(
                "{} {} could not use {} in {} phase (at {}, {}, {}{}, {}) — trying {}",
                who(villager), shortId(id), describe(failed), escape.phase,
                String.format(java.util.Locale.ROOT, "%.2f", villager.getX()),
                String.format(java.util.Locale.ROOT, "%.2f", villager.getY()),
                String.format(java.util.Locale.ROOT, "%.2f", villager.getZ()),
                villager.isOnGround() ? "" : ", in the air",
                escape.motion.progress(), describe(escape.exit()));

        escape.phase = Phase.APPROACH;
        escape.since = world.getTime();
        escape.motion.reroute();
        escape.jumps = 0;
    }

    private static void finish(UUID id, Escape escape, MobEntity villager) {
        ESCAPES.remove(id);
        SEEN_FREE.remove(id);
        WalkOverride.release(id);

        if (escape != null && escape.opened) {
            closeGates(escape, escape.exit());
        }

        if (villager instanceof VillagerEntity worker) {
            worker.getBrain().forget(MemoryModuleType.WALK_TARGET);
        }
    }

    private static void closeGates(Escape escape, FencedIn.Exit exit) {
        FencedIn.closeWhenClear(exit.barrier());

        if (escape.twin != null) {
            FencedIn.closeWhenClear(escape.twin);
        }

        escape.opened = false;
        escape.twin = null;
    }

    private static String who(MobEntity mob) {
        return mob instanceof VillagerEntity ? "Worker" : "Golem";
    }

    private static String describe(FencedIn.Exit exit) {
        return (exit.gate() ? "the gate at " : "the fence to jump at ") + exit.barrier().toShortString();
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }

    /** Esquece a saída de um aldeão, fechando o portão que ela abriu. */
    static void forget(UUID villagerId) {
        finish(villagerId, ESCAPES.get(villagerId), null);
    }

    /** Esquece tudo. Chamado ao abrir e ao parar o servidor. */
    public static void clearAll() {
        for (UUID id : ESCAPES.keySet()) {
            WalkOverride.release(id);
        }

        ESCAPES.clear();
        SEEN_FREE.clear();
        TIMES.clear();
    }
}
