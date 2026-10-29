package com.villagecolony.fabric.mixin;

import com.villagecolony.fabric.integration.VillageGround;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * O solo da vila na geração do chunk — {@link VillageGround} (autor, 2026-10-29).
 *
 * <p>Injeção no começo, permitida pela ADR-004 §4 Regra 1 com justificativa: é o único ponto em que o terreno do chunk
 * está pronto e as peças da vila ainda não foram postas, de modo que as ruas do Minecraft nascem sobre o chão novo. O
 * {@code terrain_adaptation} (dado) só mexe em volta de cada peça. Nada é cancelado (Regra 2); a decisão mora no
 * {@code VillageGround} (Regra 3).
 */
@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin {

    // Chamado pelo Mixin por injeção de bytecode, e o CallbackInfo é obrigatório na assinatura do @Inject.
    @SuppressWarnings({"UnusedMethod", "UnusedVariable"})
    @Inject(method = "generateFeatures", at = @At("HEAD"))
    private void villagecolony$shapeTheVillageGround(StructureWorldAccess world, Chunk chunk,
                                                     StructureAccessor structures, CallbackInfo info) {
        VillageGround.shape(world, chunk, structures, (ChunkGenerator) (Object) this);
    }
}
