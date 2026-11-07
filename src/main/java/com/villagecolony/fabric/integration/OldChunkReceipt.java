package com.villagecolony.fabric.integration;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

/**
 * O recibo que versões anteriores gravaram em cada chunk, sem uso desde a ADR-047. O tipo continua registrado só para o Fabric ler o
 * chunk antigo sem avisar a cada carga ("Unknown attachment type"). Nada novo é gravado.
 */
public final class OldChunkReceipt {
    public static final AttachmentType<NbtCompound> TYPE = AttachmentRegistry.createPersistent(
            Identifier.of("villagecolony", "mae_generation"), NbtCompound.CODEC);

    private OldChunkReceipt() {
    }

    /** Carrega a classe, e com ela o registro, antes do primeiro chunk. */
    public static AttachmentType<NbtCompound> register() {
        return TYPE;
    }
}
