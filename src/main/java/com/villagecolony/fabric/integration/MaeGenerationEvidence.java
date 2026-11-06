package com.villagecolony.fabric.integration;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.world.chunk.WorldChunk;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    private static byte[] fingerprint(WorldChunk chunk, int sectionY) {
        MessageDigest digest = sha256();
        Map<BlockState, byte[]> states = new HashMap<>();
        var section = chunk.getSectionArray()[chunk.sectionCoordToIndex(sectionY)];
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    BlockState value = section.getBlockState(x, y, z);
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
