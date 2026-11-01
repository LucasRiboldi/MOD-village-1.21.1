package com.villagecolony.fabric.mixin;

import com.villagecolony.fabric.integration.VillageOrigin;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.ChunkPos;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Persistência por start sem PersistentState na geração — ADR-043. */
@Mixin(StructureStart.class)
public abstract class StructureStartMixin implements VillageOrigin {
    @Unique
    private @Nullable NbtElement villagecolony$origin;

    @Override
    public @Nullable NbtElement villagecolony$getOrigin() {
        return villagecolony$origin == null ? null : villagecolony$origin.copy();
    }

    @Override
    public void villagecolony$setOrigin(@Nullable NbtElement origin) {
        villagecolony$origin = origin == null ? null : origin.copy();
    }

    // Chamado por injeção; os argumentos preservam a assinatura Vanilla — ADR-043.
    @SuppressWarnings({"UnusedMethod", "UnusedVariable"})
    @Inject(method = "fromNbt", at = @At("RETURN"))
    private static void villagecolony$readOrigin(StructureContext context, NbtCompound nbt, long seed,
                                               CallbackInfoReturnable<StructureStart> info) {
        VillageOrigin.read(info.getReturnValue(), nbt);
    }

    // Chamado por injeção; os argumentos preservam a assinatura Vanilla — ADR-043.
    @SuppressWarnings({"UnusedMethod", "UnusedVariable"})
    @Inject(method = "toNbt", at = @At("RETURN"))
    private void villagecolony$writeOrigin(StructureContext context, ChunkPos pos,
                                          CallbackInfoReturnable<NbtCompound> info) {
        VillageOrigin.write((StructureStart) (Object) this, info.getReturnValue());
    }
}
