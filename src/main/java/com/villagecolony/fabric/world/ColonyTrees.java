package com.villagecolony.fabric.world;

import com.villagecolony.core.type.ServerMemory;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.Set;

/**
 * O chão das mudas que a colônia plantou — o viveiro e o bosque do lenhador ({@code TreeNursery.plant}).
 *
 * <p>Existe para a limpeza de lote não derrubar árvore da colônia (2026-10-20). A terra enraizada
 * sob a muda não basta como marca: a cerejeira do Vanilla também nasce de terra enraizada, e a
 * bancada de simulação mostrou a casa fundadora sem lote na cerejeira por isso.
 *
 * <p>Em memória, como o bosque ({@code VillageGrove}): depois de reiniciar o servidor a marca some,
 * e a árvore volta a ser só árvore. Pacote à parte pelo mesmo motivo de {@link GeneratedVillages}.
 */
public final class ColonyTrees {

    static {
        ServerMemory.register(ColonyTrees.class, ColonyTrees::clearAll);
    }

    private static final Set<Long> PLANTED = new HashSet<>();

    private ColonyTrees() {
    }

    /** A colônia plantou uma muda sobre este chão. */
    public static void planted(BlockPos ground) {
        PLANTED.add(ground.asLong());
    }

    /** Se a árvore que nasce deste chão é da colônia. */
    public static boolean isPlanted(BlockPos ground) {
        return PLANTED.contains(ground.asLong());
    }

    public static void clearAll() {
        PLANTED.clear();
    }
}
