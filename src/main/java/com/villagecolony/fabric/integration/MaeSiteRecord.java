package com.villagecolony.fabric.integration;

import com.villagecolony.core.type.ResourceId;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * O pedido de obra guardado na região, para ela continuar depois de reiniciar o jogo — Regra 187. O estado da obra
 * continua nos jobs do writer; aqui fica só o que o condutor da obra precisa para ser refeito.
 */
record MaeSiteRecord(ResourceId model, BlockPos origin, int quarterTurns, @Nullable UUID owner) {

    static final String KEY = "site";

    /** O pedido desta região; vazio sem ele, ou com ele ilegível, e aí a região só fica como está. */
    static Optional<MaeSiteRecord> of(NbtCompound region) {
        if (!region.contains(KEY, NbtElement.COMPOUND_TYPE)) {
            return Optional.empty();
        }

        NbtCompound site = region.getCompound(KEY);
        int rotation = site.getInt("rotation");

        if (site.getString("path").isEmpty() || !site.contains("origin", NbtElement.LONG_TYPE) || rotation < 0 || rotation > 3) {
            return Optional.empty();
        }

        return Optional.of(new MaeSiteRecord(new ResourceId(site.getString("namespace"), site.getString("path")),
                BlockPos.fromLong(site.getLong("origin")), rotation, site.containsUuid("owner") ? site.getUuid("owner") : null));
    }
}
