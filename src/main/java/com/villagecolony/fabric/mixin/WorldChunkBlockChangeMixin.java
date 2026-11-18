package com.villagecolony.fabric.mixin;

import com.villagecolony.fabric.integration.RoadRegionEvents;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Bloco mudado na vila marca a região do grafo — ADR-057, modo 2.
 *
 * <p>Injeção no fim, só quando o bloco mudou de fato (o retorno é o estado anterior; nulo quando nada mudou). Não cancela
 * nem altera o Vanilla (ADR-004 §4, Regras 1 e 2); a conta mora em {@link RoadRegionEvents} (Regra 3).
 */
@Mixin(WorldChunk.class)
public abstract class WorldChunkBlockChangeMixin {

    // Chamado pelo Mixin por injeção de bytecode; o estado novo e o movimento vêm na assinatura do alvo.
    @SuppressWarnings({"UnusedMethod", "UnusedVariable"})
    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void villagecolony$markRoadRegion(BlockPos pos, BlockState state, boolean moved,
                                              CallbackInfoReturnable<BlockState> returned) {
        if (returned.getReturnValue() != null) {
            RoadRegionEvents.changed(((WorldChunk) (Object) this).getWorld(), pos, returned.getReturnValue(), state);
        }
    }
}
