package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ServerMemory;
import net.minecraft.block.BlockState;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.event.GameEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Preso num cercado na superfície — E52, playtest de 2026-10-01.
 *
 * <p><b>O defeito.</b> Na sessão das 04:54 às 08:46, oito trabalhadores
 * (carpinteiros, mineiros e os três lenhadores) ficaram presos num curral de
 * vila: pulavam para dentro de cima dos fardos de feno encostados na cerca e,
 * lá dentro, não tinham por onde sair — aldeão não abre portão de cerca. O
 * E47 os marcava como encalhados, mas {@code StrandedEscape.isOut} só olha o
 * terreno em volta, e no curral ele está no nível do chão: "is out after 0
 * steps", de volta à escala, de volta à cerca. Sem lenhador, a vila ficou sem
 * madeira por quatro horas, e a casa parou esperando cerca.
 *
 * <p><b>O teste.</b> Uma busca pelo chão a partir dos pés, como um aldeão
 * andaria: degrau de um bloco para cima, queda de até três, porta de madeira
 * abre, cerca, muro e portão fechado não passam. Quem chega a
 * {@link #RADIUS} blocos de onde está não está preso.
 *
 * <p><b>A saída.</b> O aldeão abre o portão do cercado, como abre uma porta,
 * e o portão fecha de novo depois — o curral continua segurando os animais.
 * Sem portão, ou com o portão fora de alcance, ele pula a cerca. Nenhum bloco
 * é quebrado (Regra 3). Quem conduz a saída é o {@link PenEscape}.
 *
 * <p><b>A armadilha que soltou o primeiro conserto</b> — 2026-10-01, 09:19.
 * Com o portão aberto, esta busca passa por ele e responde "não cercado".
 * Perguntar de novo, de dentro, depois de abrir, soltava o aldeão no curral
 * um segundo depois, e o portão fechava com ele lá. Fora é sair do
 * {@link Result#reach} medido com o portão fechado, e não esta resposta.
 */
final class FencedIn {

    static {
        ServerMemory.register(FencedIn.class, FencedIn::clearAll);
    }

    /** Quem chega a esta distância de onde está não está cercado. */
    static final int RADIUS = 12;

    /** Até onde a busca vai antes de desistir de provar que está preso. */
    private static final int MAX_VISITED = 4_000;

    /** Quanto tempo o portão fica aberto para ele passar. */
    static final long OPEN_TICKS = 600;

    /** Os portões abertos por quem fugia, e quando fecham. */
    private static final Map<BlockPos, Long> OPENED = new HashMap<>();

    private FencedIn() {
    }

    /**
     * O que a busca achou.
     *
     * @param enclosed se ele não alcança {@link #RADIUS} blocos andando
     * @param gates os portões fechados na borda do que ele alcança
     * @param reach até onde ele anda, para saber de que lado do portão sair
     */
    record Result(boolean enclosed, List<BlockPos> gates, Set<BlockPos> reach) {

        static final Result FREE = new Result(false, List.of(), Set.of());
    }

    /** Se ele está cercado, e por quais portões poderia sair. */
    static Result check(ServerWorld world, BlockPos feet) {
        BlockPos start = feet.toImmutable();
        Set<BlockPos> seen = new HashSet<>();
        Set<BlockPos> gates = new HashSet<>();
        Deque<BlockPos> open = new ArrayDeque<>();

        seen.add(start);
        open.add(start);

        while (!open.isEmpty()) {
            BlockPos at = open.poll();

            if (horizontalSquared(at, start) > (long) RADIUS * RADIUS || seen.size() > MAX_VISITED) {
                return Result.FREE;
            }

            for (Direction way : Direction.Type.HORIZONTAL) {
                BlockPos ahead = at.offset(way);

                if (!world.isChunkLoaded(ahead)) {
                    // Não dá para provar que está preso sem olhar: solto.
                    return Result.FREE;
                }

                if (isClosedGate(world.getBlockState(ahead))) {
                    gates.add(ahead.toImmutable());
                    continue;
                }

                stepTo(world, at, ahead).ifPresent(next -> {
                    if (seen.add(next)) {
                        open.add(next);
                    }
                });
            }
        }

        List<BlockPos> nearestFirst = new ArrayList<>(gates);
        nearestFirst.sort(Comparator.comparingLong(gate -> horizontalSquared(gate, start)));

        return new Result(true, List.copyOf(nearestFirst), Set.copyOf(seen));
    }

    /**
     * Uma saída do cercado: de onde ele parte, por cima ou através de quê, e
     * onde ele pisa do lado de fora.
     *
     * @param gate se é um portão, que se abre; senão é cerca ou muro, que se pula
     * @param inside o chão de dentro, encostado no {@code barrier}
     * @param barrier o portão ou a cerca
     * @param outside onde ele fica de pé do lado de fora
     */
    record Exit(boolean gate, BlockPos inside, BlockPos barrier, BlockPos outside) {
    }

    /** Quantas cercas a pular entram na lista, das mais perto às mais longe. */
    private static final int MAX_VAULTS = 6;

    /**
     * As saídas deste cercado, os portões primeiro e as cercas depois, cada
     * grupo do mais perto de {@code from} ao mais longe.
     *
     * <p>Vazia quando o que o prende não é cerca nem portão — buraco, parede
     * de pedra: aí é a fuga do E47 que cava.
     *
     * <p><b>Só conta saída que leva para mais longe.</b> Quem está do lado de
     * fora, numa faixa entre a cerca e um barranco, também "não chega a 12
     * blocos" — e a saída mais perto seria pular para dentro do curral. Onde
     * ele pisa precisa ser solto, ou ao menos mais largo que onde ele está.
     */
    static List<Exit> exits(ServerWorld world, Result pen, BlockPos from) {
        Map<BlockPos, Boolean> wider = new HashMap<>();
        Predicate<BlockPos> leadsOut = landing -> wider.computeIfAbsent(landing,
                at -> isWider(world, at, pen));
        List<Exit> gates = new ArrayList<>();

        for (BlockPos gate : pen.gates()) {
            for (Direction way : Direction.Type.HORIZONTAL) {
                BlockPos inside = gate.offset(way.getOpposite());

                if (pen.reach().contains(inside)) {
                    landing(world, gate.offset(way), pen.reach())
                            .filter(leadsOut)
                            .ifPresent(outside -> gates.add(new Exit(true, inside, gate, outside)));
                }
            }
        }

        List<Exit> vaults = new ArrayList<>();

        for (BlockPos inside : pen.reach()) {
            for (Direction way : Direction.Type.HORIZONTAL) {
                BlockPos barrier = inside.offset(way);

                if (isVaultable(world, inside, barrier)) {
                    landing(world, barrier.offset(way), pen.reach())
                            .filter(leadsOut)
                            .ifPresent(outside -> vaults.add(new Exit(false, inside, barrier, outside)));
                }
            }
        }

        Comparator<Exit> nearest = Comparator.comparingDouble(exit -> exit.inside().getSquaredDistance(from));

        gates.sort(nearest);
        vaults.sort(nearest);

        List<Exit> all = new ArrayList<>(gates);
        all.addAll(vaults.subList(0, Math.min(MAX_VAULTS, vaults.size())));

        return List.copyOf(all);
    }

    /**
     * O caminho de {@code from} até {@code to} pelo chão que ele alcança, um
     * bloco por passo, sem {@code from} e terminando em {@code to}.
     *
     * <p>É a mesma busca que provou que ele está preso, e por isso não
     * depende da navegação Vanilla: no teste de 2026-10-01 ela dava "não
     * alcança" para um bloco a dois passos, dentro do curral, e o aldeão
     * ficava parado os 400 tiques da saída.
     *
     * @return vazio se {@code to} não está ao alcance pelo chão
     */
    static Optional<List<BlockPos>> route(ServerWorld world, BlockPos from, BlockPos to, Set<BlockPos> reach) {
        Map<BlockPos, BlockPos> cameFrom = new HashMap<>();
        Deque<BlockPos> open = new ArrayDeque<>();

        cameFrom.put(from, from);
        open.add(from);

        while (!open.isEmpty()) {
            BlockPos at = open.poll();

            if (at.equals(to)) {
                List<BlockPos> path = new ArrayList<>();

                for (BlockPos step = to; !step.equals(from); step = cameFrom.get(step)) {
                    path.add(0, step);
                }

                return Optional.of(List.copyOf(path));
            }

            for (Direction way : Direction.Type.HORIZONTAL) {
                stepTo(world, at, at.offset(way))
                        .filter(next -> reach.contains(next) || next.equals(to))
                        .filter(next -> cameFrom.putIfAbsent(next, at) == null)
                        .ifPresent(open::add);
            }
        }

        return Optional.empty();
    }

    /** Abre o portão, com o som de quem abre. O portão fecha sozinho depois de {@link #OPEN_TICKS}. */
    static boolean open(ServerWorld world, BlockPos gate) {
        BlockState state = world.getBlockState(gate);

        if (!isClosedGate(state)) {
            return state.getBlock() instanceof FenceGateBlock;
        }

        world.setBlockState(gate, state.with(FenceGateBlock.OPEN, true));
        world.playSound(null, gate, SoundEvents.BLOCK_FENCE_GATE_OPEN, SoundCategory.BLOCKS, 1.0F, 1.0F);
        world.emitGameEvent(null, GameEvent.BLOCK_OPEN, gate);
        OPENED.put(gate.toImmutable(), world.getTime() + OPEN_TICKS);

        return true;
    }

    /** Ele passou: o portão fecha assim que ninguém estiver nele. */
    static void closeWhenClear(BlockPos gate) {
        OPENED.computeIfPresent(gate, (at, when) -> 0L);
    }

    /** Fecha os portões abertos na fuga, quando ninguém está passando por eles. */
    static void tick(ServerWorld world) {
        long now = world.getTime();

        OPENED.entrySet().removeIf(entry -> {
            if (now < entry.getValue()) {
                return false;
            }

            BlockPos gate = entry.getKey();

            // Chunk descarregado: o portão espera o chunk voltar para fechar.
            // Esquecê-lo aqui deixaria o curral aberto para sempre.
            if (!world.isChunkLoaded(gate)) {
                return false;
            }

            if (!world.getEntitiesByClass(LivingEntity.class, new Box(gate), LivingEntity::isAlive)
                    .isEmpty()) {
                return false;
            }

            BlockState state = world.getBlockState(gate);

            if (state.getBlock() instanceof FenceGateBlock && state.get(FenceGateBlock.OPEN)) {
                world.setBlockState(gate, state.with(FenceGateBlock.OPEN, false));
                world.playSound(null, gate, SoundEvents.BLOCK_FENCE_GATE_CLOSE, SoundCategory.BLOCKS,
                        1.0F, 1.0F);
                world.emitGameEvent(null, GameEvent.BLOCK_CLOSE, gate);
            }

            return true;
        });
    }

    /** Esquece os portões. Chamado ao abrir e ao parar o servidor. */
    static void clearAll() {
        OPENED.clear();
    }

    // Para onde ele vai a partir de `at` andando na direção de `ahead`: o
    // mesmo nível, um degrau acima ou uma queda de até três.
    private static Optional<BlockPos> stepTo(ServerWorld world, BlockPos at, BlockPos ahead) {
        if (isStandable(world, ahead)) {
            return Optional.of(ahead.toImmutable());
        }

        BlockPos up = ahead.up();

        if (isPassable(world, at.up(2)) && isStandable(world, up)) {
            return Optional.of(up.toImmutable());
        }

        if (!isPassable(world, ahead) || !isPassable(world, ahead.up())) {
            return Optional.empty();
        }

        for (int drop = 1; drop <= 3; drop++) {
            BlockPos down = ahead.down(drop);

            if (isStandable(world, down)) {
                return Optional.of(down.toImmutable());
            }

            if (!isPassable(world, down)) {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    // Cerca ou muro na altura dos pés, com o ar livre por cima dela e por
    // cima dele até onde o pulo leva a cabeça.
    private static boolean isVaultable(ServerWorld world, BlockPos inside, BlockPos barrier) {
        BlockState state = world.getBlockState(barrier);

        if (!state.isIn(BlockTags.FENCES) && !state.isIn(BlockTags.WALLS)) {
            return false;
        }

        for (int up = 1; up <= 3; up++) {
            if (!isPassable(world, barrier.up(up)) || !isPassable(world, inside.up(up))) {
                return false;
            }
        }

        return true;
    }

    // Do lado de lá ele fica solto, ou ao menos com mais chão que de cá. O
    // portão é medido fechado, como estava quando ele foi visto preso.
    private static boolean isWider(ServerWorld world, BlockPos landing, Result pen) {
        Result there = check(world, landing);

        return !there.enclosed() || there.reach().size() > pen.reach().size();
    }

    // Onde ele pisa do lado de fora: no mesmo nível, um degrau acima ou até
    // dois abaixo — e fora do que ele alcança de dentro.
    private static Optional<BlockPos> landing(ServerWorld world, BlockPos ahead, Set<BlockPos> reach) {
        for (int dy = 1; dy >= -2; dy--) {
            BlockPos feet = ahead.up(dy);

            if (isStandable(world, feet)) {
                return reach.contains(feet) ? Optional.empty() : Optional.of(feet.toImmutable());
            }

            if (dy < 1 && !isPassable(world, feet)) {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    // Os pés e a cabeça cabem, e o chão segura — e o chão não é cerca, muro
    // ou portão, em que não se sobe.
    private static boolean isStandable(ServerWorld world, BlockPos feet) {
        BlockPos ground = feet.down();
        BlockState below = world.getBlockState(ground);

        return isPassable(world, feet) && isPassable(world, feet.up())
                && !below.getCollisionShape(world, ground).isEmpty()
                && !isTall(below);
    }

    // Sem colisão, ou porta de madeira, que o aldeão abre.
    private static boolean isPassable(ServerWorld world, BlockPos at) {
        BlockState state = world.getBlockState(at);

        return state.getCollisionShape(world, at).isEmpty() || state.isIn(BlockTags.WOODEN_DOORS);
    }

    private static boolean isTall(BlockState state) {
        return state.isIn(BlockTags.FENCES) || state.isIn(BlockTags.WALLS)
                || state.isIn(BlockTags.FENCE_GATES);
    }

    private static boolean isClosedGate(BlockState state) {
        return state.getBlock() instanceof FenceGateBlock && !state.get(FenceGateBlock.OPEN);
    }

    private static long horizontalSquared(BlockPos a, BlockPos b) {
        long dx = (long) a.getX() - b.getX();
        long dz = (long) a.getZ() - b.getZ();

        return dx * dx + dz * dz;
    }

    /**
     * A que distância de uma cerca, portão ou muro vale a pena medir.
     *
     * <p>Seis, e não dois: com dois, quem fica parado no meio de um curral de
     * 7 × 7 — três blocos de cada cerca — nunca era medido. Seis cobre o
     * meio de um curral de 13 × 13; num maior, quem quer sair anda até a
     * cerca e é visto lá.
     */
    static final int NEAR_FENCE = 6;

    static boolean isBarrier(BlockState state) {
        return state.isIn(BlockTags.FENCES) || state.isIn(BlockTags.WALLS) || state.isIn(BlockTags.FENCE_GATES);
    }

    static boolean isNearAFence(ServerWorld world, BlockPos feet) {
        for (BlockPos at : BlockPos.iterate(
                feet.add(-NEAR_FENCE, -1, -NEAR_FENCE), feet.add(NEAR_FENCE, 1, NEAR_FENCE))) {

            if (isBarrier(world.getBlockState(at))) {
                return true;
            }
        }

        return false;
    }
}
