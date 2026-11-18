package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.integration.WorldPathTerrain;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

/**
 * A porta do jogador é entrada do grafo — Regra 321 (autor, 2026-11-18, para todos os modos do grafo). Dentro da caixa da
 * vila, toda porta térrea que não é de construção registrada nem de peça da vila gerada ganha a entrada no primeiro chão em
 * frente a ela, primeiro do lado de fora, com 2 blocos livres em cima, a até {@value #REACH} blocos. Só as seções de chunk que têm porta são
 * lidas.
 */
final class PlayerDoors {

    /** Até quantos blocos da porta se procura o chão do lado de fora. */
    static final int REACH = 3;

    private PlayerDoors() {
    }

    /** As entradas das portas do jogador na caixa da colônia, fora de {@code known}. */
    static Set<ColonyPos> fronts(ServerWorld world, Colony colony, WorldPathTerrain terrain, List<BlockBox> known) {
        Set<ColonyPos> fronts = new HashSet<>();
        VillageBounds box = colony.bounds().orElse(null);

        if (box == null) {
            return fronts;
        }

        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                WorldChunk chunk = world.getChunkManager().getWorldChunk(cx, cz);

                if (chunk == null) {
                    continue;
                }

                for (int index = 0; index < chunk.getSectionArray().length; index++) {
                    ChunkSection section = chunk.getSectionArray()[index];
                    int bottom = chunk.sectionIndexToCoord(index) << 4;

                    if (section.isEmpty() || bottom + 15 < box.minY() - 8 || bottom > box.maxY() + 8
                            || !section.hasAny(state -> state.isIn(BlockTags.DOORS))) {
                        continue;
                    }

                    for (BlockPos at : BlockPos.iterate(cx << 4, bottom, cz << 4, (cx << 4) + 15, bottom + 15, (cz << 4) + 15)) {
                        BlockState state = section.getBlockState(at.getX() & 15, at.getY() & 15, at.getZ() & 15);

                        if (state.isIn(BlockTags.DOORS) && state.get(DoorBlock.HALF) == DoubleBlockHalf.LOWER
                                && inside(box, at) && known.stream().noneMatch(piece -> piece.contains(at))) {
                            front(world, terrain, at.toImmutable(), state.get(DoorBlock.FACING)).ifPresent(fronts::add);
                        }
                    }
                }
            }
        }

        return fronts;
    }

    /** O primeiro chão em frente à porta, primeiro do lado de fora (o {@code facing} aponta para dentro), com 2 livres em cima. */
    private static java.util.Optional<ColonyPos> front(ServerWorld world, WorldPathTerrain terrain, BlockPos door,
                                                       Direction facing) {
        for (Direction side : new Direction[] {facing.getOpposite(), facing}) {
            for (int step = 1; step <= REACH; step++) {
                BlockPos column = door.offset(side, step);
                OptionalInt ground = terrain.groundY(column.getX(), column.getZ(), door.getY() - 1);

                BlockPos stand = new BlockPos(column.getX(), ground.isPresent() ? ground.getAsInt() + 1 : 0, column.getZ());

                if (ground.isPresent() && Math.abs(ground.getAsInt() - (door.getY() - 1)) <= 1
                        && world.getBlockState(stand).getCollisionShape(world, stand).isEmpty()
                        && world.getBlockState(stand.up()).getCollisionShape(world, stand.up()).isEmpty()) {
                    return java.util.Optional.of(new ColonyPos(column.getX(), ground.getAsInt(), column.getZ()));
                }
            }
        }

        return java.util.Optional.empty();
    }

    private static boolean inside(VillageBounds box, BlockPos at) {
        return at.getX() >= box.minX() && at.getX() <= box.maxX() && at.getZ() >= box.minZ() && at.getZ() <= box.maxZ();
    }
}
