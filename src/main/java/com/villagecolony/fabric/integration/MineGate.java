package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * O portão da entrada da mina — estrutura do autor ({@code data/villagecolony/structure/mine_entrance.nbt}, 2026-10-29),
 * no lugar do arco de pedregulho: dois pilares de pedra e tijolo, telhado de carvalho escuro, três lanternas
 * penduradas, um baú e um barril de enfeite, e um vão de 3 de largura e 4 de altura no meio.
 *
 * <p>A entrada da mina — as três pistas da escada — fica no centro do vão ({@link #CENTER_X}, 0, {@link #CENTER_Z}), e o
 * vão corre no rumo em que a escada desce: o {@code +z} da estrutura vira o rumo da descida. Cada bloco entra só onde há ar ou chão natural, e nunca
 * sobre casa da vila ou obra da colônia (Regra 3).
 */
public final class MineGate {

    /** A coluna do meio do vão, onde fica a boca. */
    static final int CENTER_X = 4;
    static final int CENTER_Z = 2;

    /** A altura do topo do telhado, no meio do vão: a primeira coisa que a varredura vê do portão. */
    static final int PEAK_Y = 6;

    /** Quanto da estrutura precisa estar de pé para ser o portão: a Regra 3 pode ter deixado peças de fora. */
    static final double STANDING_SHARE = 0.75;

    /** Um bloco da estrutura, em coordenadas dela. */
    record Piece(int x, int y, int z, BlockState state) {
    }

    /** Um portão de pé no mundo. */
    public record Standing(BlockPos mouth, Direction descent) {
    }

    private MineGate() {
    }

    /** O giro que leva o {@code +z} da estrutura (sul) ao rumo da descida. */
    static BlockRotation rotationFor(Direction descent) {
        return switch (descent) {
            case WEST -> BlockRotation.CLOCKWISE_90;
            case NORTH -> BlockRotation.CLOCKWISE_180;
            case EAST -> BlockRotation.COUNTERCLOCKWISE_90;
            default -> BlockRotation.NONE;
        };
    }

    /**
     * Onde fica, no mundo, o bloco {@code (x, y, z)} da estrutura erguida nesta boca. O centro do vão fica sobre a pista
     * do meio da escada: as três pistas saem da boca para a esquerda da descida ({@code MineShaft.stair}), e assim
     * passam inteiras pelo vão de 3.
     */
    public static BlockPos at(BlockPos mouth, Direction descent, int x, int y, int z) {
        BlockRotation rotation = rotationFor(descent);
        Direction across = rotation.rotate(Direction.EAST);
        Direction along = rotation.rotate(Direction.SOUTH);
        BlockPos center = mouth.offset(descent.rotateYCounterclockwise());

        return center.offset(across, x - CENTER_X).offset(along, z - CENTER_Z).up(y);
    }

    /** As posições de todos os blocos do portão nesta boca. */
    public static List<BlockPos> positions(BlockPos mouth, Direction descent) {
        List<BlockPos> positions = new ArrayList<>();

        for (Piece piece : Template.PIECES) {
            positions.add(at(mouth, descent, piece.x(), piece.y(), piece.z()));
        }

        return positions;
    }

    /** Se este bloco pode ser o topo do telhado de um portão. */
    public static boolean isPeak(BlockState state) {
        return state.isOf(Blocks.DARK_OAK_LOG);
    }

    /** O portão cujo topo do telhado é {@code peak}, no giro que mais confere com o mundo — Regra 210. */
    public static Optional<Standing> standingAt(ServerWorld world, BlockPos peak) {
        Optional<Standing> best = Optional.empty();
        int most = 0;

        for (Direction descent : Direction.Type.HORIZONTAL) {
            BlockPos mouth = peak.down(PEAK_Y).offset(descent.rotateYClockwise());
            int matched = matched(world, mouth, descent);

            if (matched > most) {
                most = matched;
                best = Optional.of(new Standing(mouth, descent));
            }
        }

        return most >= Template.PIECES.size() * STANDING_SHARE ? best : Optional.empty();
    }

    /** Se o portão continua de pé. Chunk fora de memória não desmente. */
    public static boolean stands(ServerWorld world, Standing gate) {
        return matched(world, gate.mouth(), gate.descent()) >= Template.PIECES.size() * STANDING_SHARE;
    }

    private static int matched(ServerWorld world, BlockPos mouth, Direction descent) {
        int matched = 0;

        for (Piece piece : Template.PIECES) {
            BlockPos at = at(mouth, descent, piece.x(), piece.y(), piece.z());

            if (!world.isChunkLoaded(at) || world.getBlockState(at).isOf(piece.state().getBlock())) {
                matched++;
            }
        }

        return matched;
    }

    /** A coluna a {@code blocks} blocos do vão, pelo lado de onde se chega: a escada desce para o outro. */
    public static BlockPos approach(BlockPos mouth, Direction descent, int blocks) {
        return at(mouth, descent, CENTER_X, 0, -blocks);
    }

    /** Se a posição é bloco do portão: a mina nunca a escava. */
    public static boolean isPart(BlockPos mouth, Direction descent, BlockPos at) {
        for (Piece piece : Template.PIECES) {
            if (at(mouth, descent, piece.x(), piece.y(), piece.z()).equals(at)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Ergue o portão onde couber: cada bloco só sobre ar, planta ou chão natural, e nunca sobre casa da vila ou obra da
     * colônia. Sem atualizar os vizinhos, para a lanterna pendurada não cair antes do teto.
     *
     * @return se ao menos uma posição do portão tem algo firme depois da passagem — o sinal de que ele não sobe de novo
     */
    static boolean raise(ServerWorld world, BlockPos mouth, Direction descent) {
        BlockRotation rotation = rotationFor(descent);
        boolean anyFirm = false;

        for (Piece piece : Template.PIECES) {
            BlockPos target = at(mouth, descent, piece.x(), piece.y(), piece.z());
            BlockState there = world.getBlockState(target);
            boolean free = there.isReplaceable()
                    || (LotGround.isNaturalGround(there) && !BlockProtection.isVillageOriginal(world, target)
                    && !BlockProtection.isColonyBuilt(target));

            if (free) {
                world.setBlockState(target, piece.state().rotate(rotation), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
            }

            anyFirm |= !world.getBlockState(target).isReplaceable();
        }

        return anyFirm;
    }

    /**
     * A posição de um bloco da estrutura: lista de inteiros, como o Minecraft grava, ou vetor de inteiros, como o
     * arquivo do autor veio.
     */
    static int[] positionOf(NbtCompound block) {
        if (block.contains("pos", NbtElement.INT_ARRAY_TYPE)) {
            return block.getIntArray("pos");
        }

        NbtList list = block.getList("pos", NbtElement.INT_TYPE);

        return list.size() == 3 ? new int[] {list.getInt(0), list.getInt(1), list.getInt(2)} : new int[0];
    }

    /** A estrutura lida uma vez do arquivo do mod. */
    private static final class Template {

        static final List<Piece> PIECES = load();

        private static List<Piece> load() {
            try (InputStream in = MineGate.class.getResourceAsStream("/data/villagecolony/structure/mine_entrance.nbt")) {
                if (in == null) {
                    VillageColonyMod.LOGGER.error("The mine gate structure is missing from the mod");
                    return List.of();
                }

                NbtCompound root = NbtIo.readCompressed(in, NbtSizeTracker.ofUnlimitedBytes());
                NbtList palette = root.getList("palette", NbtElement.COMPOUND_TYPE);
                NbtList blocks = root.getList("blocks", NbtElement.COMPOUND_TYPE);
                List<Piece> pieces = new ArrayList<>();

                for (int i = 0; i < blocks.size(); i++) {
                    NbtCompound block = blocks.getCompound(i);
                    BlockState state = NbtHelper.toBlockState(Registries.BLOCK.getReadOnlyWrapper(),
                            palette.getCompound(block.getInt("state")));
                    int[] pos = positionOf(block);

                    if (!state.isAir() && pos.length == 3) {
                        pieces.add(new Piece(pos[0], pos[1], pos[2], state));
                    }
                }

                // De cima para baixo: o teto antes da lanterna que pende dele.
                pieces.sort((a, b) -> Integer.compare(b.y(), a.y()));

                return List.copyOf(pieces);
            } catch (IOException failure) {
                VillageColonyMod.LOGGER.error("The mine gate structure could not be read", failure);
                return List.of();
            }
        }
    }
}
