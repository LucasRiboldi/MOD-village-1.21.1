package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ServerMemory;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * O alvo que a conferência recusou fica de lado — Regra 315, guarda contra laço.
 *
 * <p>Quando o fazendeiro larga um alvo porque ele já não vale ({@code FarmerWork.stillWorth}), aquele bloco fica de lado
 * por {@value #TICKS} tiques: a busca da roça e da vila não o devolve logo em seguida. Sem isso, uma discordância entre quem
 * escolhe o alvo e quem o confere prende o fazendeiro em silêncio, pegando e largando o mesmo bloco a cada tique. A
 * terceira recusa seguida do mesmo bloco sai no log.
 */
final class FarmerSetAside {

    static {
        ServerMemory.register(FarmerSetAside.class, FarmerSetAside::clearAll);
    }

    /** Quanto tempo o alvo recusado fica de lado: 30 segundos. */
    static final long TICKS = 600;

    /** Recusas seguidas do mesmo bloco antes da linha no log. */
    static final int LOG_AFTER = 3;

    private record Refused(BlockPos at, long until, int times) {
    }

    private static final Map<UUID, Refused> REFUSED = new HashMap<>();

    private FarmerSetAside() {
    }

    /** Põe de lado o alvo que a conferência recusou agora. */
    static void refuse(UUID farmer, BlockPos at, long now) {
        Refused last = REFUSED.get(farmer);
        int times = last != null && last.at().equals(at) ? last.times() + 1 : 1;

        REFUSED.put(farmer, new Refused(at.toImmutable(), now + TICKS, times));

        if (times == LOG_AFTER) {
            VillageColonyMod.LOGGER.warn("Farmer {} refused the same target at {} {} times in a row — it sets it aside for {}"
                    + " ticks (Regra 315)", farmer.toString().substring(0, 8), at.toShortString(), times, TICKS);
        }
    }

    /** Se este bloco está de lado para este fazendeiro. */
    static boolean isSetAside(UUID farmer, BlockPos at, long now) {
        Refused last = REFUSED.get(farmer);

        return last != null && now < last.until() && last.at().equals(at);
    }

    static void clearAll() {
        REFUSED.clear();
    }
}
