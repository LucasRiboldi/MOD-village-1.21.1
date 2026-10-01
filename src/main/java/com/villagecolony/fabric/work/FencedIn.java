package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ServerMemory;
import net.minecraft.block.BlockState;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

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
 * Nenhum bloco é quebrado (Regra 3).
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
     * Abre o portão para ele sair e devolve onde ele fica do lado de fora.
     * O portão fecha sozinho depois de {@link #OPEN_TICKS}.
     */
    static Optional<BlockPos> openTheGate(ServerWorld world, BlockPos gate, Set<BlockPos> reach) {
        BlockState state = world.getBlockState(gate);

        if (!isClosedGate(state)) {
            return Optional.empty();
        }

        world.setBlockState(gate, state.with(FenceGateBlock.OPEN, true));
        OPENED.put(gate.toImmutable(), world.getTime() + OPEN_TICKS);

        for (Direction way : Direction.Type.HORIZONTAL) {
            BlockPos outside = gate.offset(way);

            if (!reach.contains(outside) && isStandable(world, outside)) {
                return Optional.of(outside.toImmutable());
            }
        }

        return Optional.of(gate.toImmutable());
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
}
