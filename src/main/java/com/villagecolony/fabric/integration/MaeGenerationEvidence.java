package com.villagecolony.fabric.integration;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.WorldChunk;
import com.villagecolony.core.construction.model.WorkArea;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** Prova de geração por chunk; ausência nunca habilita escrita — ADR-044. */
public final class MaeGenerationEvidence {
    static final AttachmentType<NbtCompound> RECEIPT = AttachmentRegistry.createPersistent(
            Identifier.of("villagecolony", "mae_generation"), NbtCompound.CODEC);

    private MaeGenerationEvidence() { }

    public static void register() {
        ServerChunkEvents.CHUNK_GENERATE.register((world, chunk) -> {
            // Nunca substituir uma prova existente, inclusive se vier de versão desconhecida — ADR-044.
            if (!net.minecraft.world.World.OVERWORLD.equals(world.getRegistryKey()) || chunk.hasAttached(RECEIPT)) {
                return;
            }
            NbtCompound receipt = new NbtCompound();
            receipt.putInt("version", 1);
            receipt.putLong("chunk", chunk.getPos().toLong());
            receipt.putString("dimension", world.getRegistryKey().getValue().toString());
            NbtCompound sections = new NbtCompound();
            for (int y = world.getBottomSectionCoord(); y < world.getTopSectionCoord(); y++) {
                sections.putByteArray(Integer.toString(y), fingerprint(chunk, y));
            }
            receipt.put("sections", sections);
            chunk.setAttached(RECEIPT, receipt);
            chunk.setNeedsSaving(true);
        });
    }

    /** Nenhuma consulta carrega chunk; a impressão engloba a seção para rejeitar mudanças vizinhas — ADR-044. */
    static NbtCompound inspect(ServerWorld world, WorkArea area, boolean requireGeneration, int sectionLimit) {
        NbtCompound result = new NbtCompound();
        for (int x = area.min().x() >> 4; x <= area.max().x() >> 4; x++) {
            for (int z = area.min().z() >> 4; z <= area.max().z() >> 4; z++) {
                WorldChunk chunk = world.getChunkManager().getWorldChunk(x, z);
                if (chunk == null) {
                    throw new IllegalStateException("unknown_chunk");
                }
                NbtCompound receipt = chunk.getAttached(RECEIPT);
                if (receipt == null || !receipt.contains("version", NbtElement.INT_TYPE) || receipt.getInt("version") != 1
                        || !receipt.contains("sections", NbtElement.COMPOUND_TYPE)
                        || !receipt.contains("chunk", NbtElement.LONG_TYPE)
                        || receipt.getLong("chunk") != chunk.getPos().toLong()
                        || !world.getRegistryKey().getValue().toString().equals(receipt.getString("dimension"))) {
                    throw new IllegalStateException("missing_generation_evidence");
                }
                for (int y = area.min().y() >> 4; y <= area.max().y() >> 4; y++) {
                    byte[] actual = fingerprint(chunk, y);
                    byte[] original = receipt.getCompound("sections").getByteArray(Integer.toString(y));
                    if (original.length != 32 || (requireGeneration && !Arrays.equals(original, actual))) {
                        throw new IllegalStateException("changed_generation_evidence");
                    }
                    result.putByteArray(Long.toString(ChunkSectionPos.asLong(x, y, z)), actual);
                    if (result.getSize() > sectionLimit) {
                        throw new IllegalStateException("section_budget_exceeded");
                    }
                }
            }
        }
        return result;
    }

    static NbtCompound original(ServerWorld world, WorkArea area) {
        NbtCompound result = inspect(world, area, false, 8);
        for (String key : result.getKeys()) {
            long section = Long.parseLong(key);
            var chunk = world.getChunkManager().getWorldChunk(ChunkSectionPos.unpackX(section),
                    ChunkSectionPos.unpackZ(section));
            var receipt = chunk.getAttached(RECEIPT);
            result.putByteArray(key, receipt.getCompound("sections")
                    .getByteArray(Integer.toString(ChunkSectionPos.unpackY(section))));
        }
        return result;
    }

    static NbtCompound after(ServerWorld world, WorkArea area, net.minecraft.util.math.BlockPos pos, BlockState state) {
        NbtCompound result = inspect(world, area, false, 8);
        int sectionY = pos.getY() >> 4;
        var chunk = world.getChunkManager().getWorldChunk(pos.getX() >> 4, pos.getZ() >> 4);
        result.putByteArray(Long.toString(ChunkSectionPos.asLong(pos.getX() >> 4, sectionY, pos.getZ() >> 4)),
                fingerprint(chunk, sectionY, pos, state));
        return result;
    }

    private static byte[] fingerprint(WorldChunk chunk, int sectionY) {
        return fingerprint(chunk, sectionY, null, null);
    }

    private static byte[] fingerprint(WorldChunk chunk, int sectionY,
                                     net.minecraft.util.math.BlockPos override, BlockState replacement) {
        MessageDigest digest = sha256();
        Map<BlockState, byte[]> states = new HashMap<>();
        var section = chunk.getSectionArray()[chunk.sectionCoordToIndex(sectionY)];
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    BlockState value = override != null && (override.getX() & 15) == x
                            && (override.getY() & 15) == y && (override.getZ() & 15) == z
                            ? replacement : section.getBlockState(x, y, z);
                    byte[] state = states.computeIfAbsent(value,
                            block -> sha256().digest(block.toString().getBytes(StandardCharsets.UTF_8)));
                    digest.update(state);
                }
            }
        }
        return digest.digest();
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
