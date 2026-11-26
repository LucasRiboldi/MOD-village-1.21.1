package com.villagecolony.fabric.integration;

import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * O baú novo não fecha passagem dentro do cômodo — Regra 370 (autor 2026-11-26, H5 do jogo da 0.4.143: "todos baú
 * estarem encostados em uma parede e ter acesso de trânsito dentro da casa"). O lugar é recusado quando o baú nele
 * deixaria, para quem entra pela porta, uma cama sem nenhum lado alcançável ou outro baú sem a frente alcançável. Um
 * canto vazio que fique para trás não conta: não leva a nada.
 */
final class ChestPassage {

    private ChestPassage() {
    }

    /** Se um baú em {@code spot} tira de quem entra pela porta o acesso a uma cama ou à frente de um baú do cômodo. */
    static boolean closesAPassage(ServerWorld world, BlockPos spot, BlockBox room) {
        int y = spot.getY();
        Set<BlockPos> open = new HashSet<>();
        Set<BlockPos> doors = new HashSet<>();

        for (int x = room.getMinX(); x <= room.getMaxX(); x++) {
            for (int z = room.getMinZ(); z <= room.getMaxZ(); z++) {
                BlockPos at = new BlockPos(x, y, z);

                if (!at.equals(spot) && standable(world, at)) {
                    open.add(at);
                    if (world.getBlockState(at).getBlock() instanceof DoorBlock) {
                        doors.add(at);
                    }
                }
            }
        }

        if (doors.isEmpty()) {
            return false;
        }

        Set<BlockPos> after = reach(open, doors);
        open.add(spot);
        Set<BlockPos> before = reach(open, doors);

        for (BlockPos target : targets(world, room, y)) {
            List<BlockPos> access = accessOf(world, target);

            if (access.stream().anyMatch(before::contains) && access.stream().noneMatch(after::contains)) {
                return true;
            }
        }

        return false;
    }

    /** Cada cama (pela metade do pé) e cada baú do cômodo nesta altura. */
    private static List<BlockPos> targets(ServerWorld world, BlockBox room, int y) {
        List<BlockPos> targets = new java.util.ArrayList<>();

        for (int x = room.getMinX(); x <= room.getMaxX(); x++) {
            for (int z = room.getMinZ(); z <= room.getMaxZ(); z++) {
                BlockPos at = new BlockPos(x, y, z);
                BlockState state = world.getBlockState(at);

                if ((state.getBlock() instanceof BedBlock && state.get(BedBlock.PART) == BedPart.FOOT)
                        || state.getBlock() instanceof ChestBlock) {
                    targets.add(at);
                }
            }
        }

        return targets;
    }

    /** De onde se usa: a cama por qualquer lado das duas metades, o baú pela frente. */
    private static List<BlockPos> accessOf(ServerWorld world, BlockPos target) {
        BlockState state = world.getBlockState(target);

        if (state.getBlock() instanceof ChestBlock) {
            return List.of(target.offset(state.get(ChestBlock.FACING)));
        }

        BlockPos head = target.offset(state.get(BedBlock.FACING));

        return java.util.stream.Stream.of(target, head)
                .flatMap(half -> Direction.Type.HORIZONTAL.stream().map(half::offset))
                .filter(at -> !at.equals(target) && !at.equals(head))
                .collect(Collectors.toList());
    }

    /** Chão com corpo embaixo e dois blocos sem colisão em cima; a porta é passagem. */
    private static boolean standable(ServerWorld world, BlockPos at) {
        return !world.getBlockState(at.down()).getCollisionShape(world, at.down()).isEmpty()
                && passable(world, at) && passable(world, at.up());
    }

    private static boolean passable(ServerWorld world, BlockPos at) {
        return world.getBlockState(at).getBlock() instanceof DoorBlock
                || world.getBlockState(at).getCollisionShape(world, at).isEmpty();
    }

    /** O chão alcançado a partir das portas, nas quatro direções. */
    private static Set<BlockPos> reach(Set<BlockPos> open, Set<BlockPos> starts) {
        Set<BlockPos> seen = new HashSet<>(starts);
        Queue<BlockPos> pending = new ArrayDeque<>(starts);

        while (!pending.isEmpty()) {
            BlockPos current = pending.remove();

            for (Direction direction : Direction.Type.HORIZONTAL) {
                BlockPos next = current.offset(direction);

                if (open.contains(next) && seen.add(next)) {
                    pending.add(next);
                }
            }
        }

        return seen;
    }
}
