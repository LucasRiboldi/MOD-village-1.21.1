package com.villagecolony.fabric.integration;

import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.type.ServerMemory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

/**
 * As pontas de rua que não se deixaram calçar — separadas de
 * {@link RoadExtension} em 2026-10-02 pelo teto de 500 linhas. O texto veio
 * sem mudança, e o registro se esvazia sozinho ao parar o servidor, pelo
 * {@link ServerMemory}.
 */
final class RoadRefusals {

    static {
        ServerMemory.register(RoadRefusals.class, RoadRefusals::clear);
    }

    /**
     * As pontas que não se deixaram calçar, e desde quando.
     *
     * <p><b>A recusa envelhece</b> — é a Regra 23, e o mesmo molde de
     * {@code TreeMarks}. Sem envelhecer, uma ponta impossível sairia da
     * lista para sempre e a vila perderia candidatos a cada terreno
     * ruim; sem recusa nenhuma, as doze mesmas pontas seriam tentadas
     * toda varredura e a décima terceira nunca teria vez.
     *
     * <p>O jogador aplaina o barranco, tira a árvore, quebra a cerca — e
     * dez ciclos depois a ponta volta a valer.
     */
    private static final Map<BlockPos, Long> REFUSED = new HashMap<>();

    /** Por quantos ticks uma ponta recusada fica de fora. Dez ciclos. */
    static final int MEMORY = 10 * VillageDetector.CYCLE_TICKS;

    /**
     * Quantas recusas se guarda antes de esquecer tudo.
     *
     * <p>Teto, e não regra: uma vila cercada de construção encheria o
     * mapa sem limite. Esquecer tudo custa uma tentativa perdida por
     * ponta, e é melhor que crescer para sempre.
     */
    private static final int MAX_REFUSED = 1024;

    private RoadRefusals() {
    }

    /** Se esta ponta está de castigo, e ainda não envelheceu. */
    static boolean isRefused(ServerWorld world, BlockPos road) {
        Long since = REFUSED.get(road);

        if (since == null) {
            return false;
        }

        if (world.getTime() - since < MEMORY) {
            return true;
        }

        REFUSED.remove(road);

        return false;
    }

    /** Anota que esta ponta não se deixou calçar agora. */
    static void refuse(ServerWorld world, BlockPos road) {
        if (REFUSED.size() >= MAX_REFUSED) {
            REFUSED.clear();
        }

        REFUSED.put(road, world.getTime());
    }

    static void clear() {
        REFUSED.clear();
    }
}
