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
 * O arco da entrada da mina — Regra 256 (autor, 2026-11-12): dois pilares e uma viga de tora descascada, um
 * degrau em cada ponta da viga com um lampião pendurado, e um baú no chão do lado de fora, à direita de quem entra
 * ({@code data/villagecolony/structure/mine_arch.nbt}). O arco surge sozinho, sem profissão nem material, na madeira da
 * vila (Regra 262), e é conferido pelo papel de cada peça.
 *
 * <p>O portão de 2026-10-29 ({@code mine_entrance.nbt}: pilares de pedra, telhado de carvalho escuro) não é mais
 * erguido, mas continua reconhecido onde já está de pé, e a mina continua sem cavar as peças dele.
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

    /** O arco erguido hoje; o portão antigo, só reconhecido. As peças do arco ficam no sistema do portão (z + 2). */
    private static final Template ARCH = new Template("mine_arch.nbt", CENTER_Z);

    private static final Template OLD = new Template("mine_entrance.nbt", 0);

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

    /** As posições de todos os blocos do arco nesta boca. */
    public static List<BlockPos> positions(BlockPos mouth, Direction descent) {
        List<BlockPos> positions = new ArrayList<>();

        for (Piece piece : ARCH.pieces) {
            positions.add(at(mouth, descent, piece.x(), piece.y(), piece.z()));
        }

        return positions;
    }

    /** Se este bloco pode ser o topo do telhado de um portão antigo. */
    public static boolean isPeak(BlockState state) {
        return state.isOf(Blocks.DARK_OAK_LOG);
    }

    /** O portão cujo topo do telhado é {@code peak}, no giro que mais confere com o mundo — Regra 210. */
    public static Optional<Standing> standingAt(ServerWorld world, BlockPos peak) {
        Optional<Standing> best = Optional.empty();
        int most = 0;

        for (Direction descent : Direction.Type.HORIZONTAL) {
            BlockPos mouth = peak.down(PEAK_Y).offset(descent.rotateYClockwise());
            int matched = matched(world, mouth, descent, OLD);

            if (matched > most) {
                most = matched;
                best = Optional.of(new Standing(mouth, descent));
            }
        }

        return most >= OLD.pieces.size() * STANDING_SHARE ? best : Optional.empty();
    }

    /**
     * O arco ou o portão de pé nesta boca, lido pelas peças e não pelo topo — Regra 256. A boca vem da mina da colônia:
     * o arco não precisa de marcador próprio, e uma peça tirada (o lenhador levou o topo do portão antigo) não o apaga.
     */
    public static Optional<Standing> standingAtMouth(ServerWorld world, BlockPos mouth, Direction descent) {
        Standing gate = new Standing(mouth, descent);

        return stands(world, gate) ? Optional.of(gate) : Optional.empty();
    }

    /** Se o arco ou o portão antigo continua de pé. Chunk fora de memória não desmente. */
    public static boolean stands(ServerWorld world, Standing gate) {
        return matched(world, gate.mouth(), gate.descent(), ARCH) >= ARCH.pieces.size() * STANDING_SHARE
                || matched(world, gate.mouth(), gate.descent(), OLD) >= OLD.pieces.size() * STANDING_SHARE;
    }

    private static int matched(ServerWorld world, BlockPos mouth, Direction descent, Template template) {
        int matched = 0;

        for (Piece piece : template.pieces) {
            BlockPos at = at(mouth, descent, piece.x(), piece.y(), piece.z());

            // Pelo papel da peça: o arco de outra madeira, de antes da Regra 262 ou de outro bioma, segue reconhecido.
            if (!world.isChunkLoaded(at) || BiomeBlueprints.sameRole(world.getBlockState(at), piece.state().getBlock())) {
                matched++;
            }
        }

        return matched;
    }

    /** A coluna a {@code blocks} blocos do vão, pelo lado de onde se chega: a escada desce para o outro. */
    public static BlockPos approach(BlockPos mouth, Direction descent, int blocks) {
        return at(mouth, descent, CENTER_X, 0, -blocks);
    }

    /** Se a posição é bloco do arco ou do portão antigo: a mina nunca a escava. */
    public static boolean isPart(BlockPos mouth, Direction descent, BlockPos at) {
        for (Template template : List.of(ARCH, OLD)) {
            for (Piece piece : template.pieces) {
                if (at(mouth, descent, piece.x(), piece.y(), piece.z()).equals(at)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Ergue o arco onde couber: cada bloco só sobre ar, planta ou chão natural, e nunca sobre casa da vila ou obra da
     * colônia. Sem atualizar os vizinhos, para o lampião pendurado não cair antes do degrau. A madeira é a da vila em volta
     * da boca (Regra 262).
     *
     * @return se ao menos uma posição do portão tem algo firme depois da passagem — o sinal de que ele não sobe de novo
     */
    static boolean raise(ServerWorld world, BlockPos mouth, Direction descent) {
        BlockRotation rotation = rotationFor(descent);
        com.villagecolony.core.construction.model.BiomeBlockSwap swap =
                BiomeBlueprints.swapAt(world, com.villagecolony.fabric.adapter.MinecraftTypeAdapter.toColonyPos(mouth));
        boolean anyFirm = false;

        for (Piece piece : ARCH.pieces) {
            BlockPos target = at(mouth, descent, piece.x(), piece.y(), piece.z());
            BlockState there = world.getBlockState(target);
            boolean free = there.isReplaceable()
                    || (LotGround.isNaturalGround(there) && !BlockProtection.isVillageOriginal(world, target)
                    && !BlockProtection.isColonyBuilt(target));

            if (free) {
                world.setBlockState(target, BiomeBlueprints.swapped(piece.state(), swap).rotate(rotation),
                        Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
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

    /** Uma estrutura lida uma vez do arquivo do mod, com o deslocamento em z que a põe no sistema do portão. */
    private static final class Template {

        final List<Piece> pieces;

        Template(String file, int shiftZ) {
            this.pieces = load(file, shiftZ);
        }

        private static List<Piece> load(String file, int shiftZ) {
            try (InputStream in = MineGate.class.getResourceAsStream("/data/villagecolony/structure/" + file)) {
                if (in == null) {
                    VillageColonyMod.LOGGER.error("The mine gate structure {} is missing from the mod", file);
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
                        pieces.add(new Piece(pos[0], pos[1], pos[2] + shiftZ, state));
                    }
                }

                // De cima para baixo: o teto antes da lanterna que pende dele.
                pieces.sort((a, b) -> Integer.compare(b.y(), a.y()));

                return List.copyOf(pieces);
            } catch (IOException failure) {
                VillageColonyMod.LOGGER.error("The mine gate structure {} could not be read", file, failure);
                return List.of();
            }
        }
    }
}
