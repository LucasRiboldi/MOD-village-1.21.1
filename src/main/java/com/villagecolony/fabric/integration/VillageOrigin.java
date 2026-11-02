package com.villagecolony.fabric.integration;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.structure.StructureStart;
import org.jspecify.annotations.Nullable;

/** Origem física no próprio start; ausência não prova geração nova — ADR-043. */
public interface VillageOrigin {
    String KEY = "villagecolony:origin";

    /** Devolve uma cópia; payload desconhecido permanece opaco — ADR-043. */
    @Nullable NbtElement villagecolony$getOrigin();

    /** Guarda uma cópia, inclusive de formatos futuros — ADR-043. */
    void villagecolony$setOrigin(@Nullable NbtElement origin);

    static void read(@Nullable StructureStart start, NbtCompound nbt) {
        if (start != null && start.hasChildren()) {
            ((VillageOrigin) (Object) start).villagecolony$setOrigin(nbt.get(KEY));
        }
    }

    static void write(StructureStart start, NbtCompound nbt) {
        if (start.hasChildren()) {
            NbtElement origin = ((VillageOrigin) (Object) start).villagecolony$getOrigin();
            if (origin != null) {
                nbt.put(KEY, origin);
            }
        }
    }

    /** Só o callsite de criação pode fornecer esta prova; nunca a descoberta — ADR-043. */
    static void markCreated(StructureStart start, DynamicRegistryManager registries) {
        if (!start.hasChildren() || !registries.get(RegistryKeys.STRUCTURE)
                .getEntry(start.getStructure()).isIn(StructureTags.VILLAGE)) {
            return;
        }
        VillageOrigin origin = (VillageOrigin) (Object) start;
        if (origin.villagecolony$getOrigin() == null) {
            NbtCompound payload = new NbtCompound();
            payload.putInt("version", 1);
            payload.putString("policy", "hierarchical_roads_v1");
            origin.villagecolony$setOrigin(payload);
        }
    }

    /** Versões e políticas desconhecidas não habilitam comportamento — ADR-043. */
    public static boolean isCurrent(@Nullable StructureStart start) {
        if (start == null || !start.hasChildren()) {
            return false;
        }
        NbtElement payload = ((VillageOrigin) (Object) start).villagecolony$getOrigin();
        return payload instanceof NbtCompound nbt
                && nbt.contains("version", NbtElement.INT_TYPE) && nbt.getInt("version") == 1
                && nbt.contains("policy", NbtElement.STRING_TYPE)
                && "hierarchical_roads_v1".equals(nbt.getString("policy"));
    }
}
