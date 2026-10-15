package com.villagecolony.fabric.work;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A regra de que o fim do caminho precisa encostar no lugar de pé da pedra — spike de 2026-10-14
 * (playtest 0.3.58). O {@code findPathTo} do jogo SOBE o alvo sólido/distante até a superfície
 * (E32) e devolve {@code reachesTarget()==true} com o fim dezenas de blocos acima no eixo Y; sem
 * conferir onde o caminho termina, o mineiro andava para a superfície sobre a pedra e nunca descia.
 *
 * <p>Lógica pura, sem mundo nem navegação — no padrão do {@link MineApproachProgressTest}.
 */
class MinerDirectPathTest {

    @Test
    void endOnTheStandingSpotReaches() {
        BlockPos approach = new BlockPos(254, 80, 1153);

        assertTrue(MinerDirectPath.endReachesApproach(approach, approach),
                "o fim exatamente no lugar de pé alcança");
        assertTrue(MinerDirectPath.endReachesApproach(approach.add(1, 0, 1), approach),
                "um bloco de folga em x e z (Manhattan 2) ainda alcança");
    }

    @Test
    void endOnTheSurfaceAboveTheStoneDoesNotReach() {
        BlockPos approach = new BlockPos(254, 80, 1153);

        // O falso caminho à superfície: mesmo x/z, Y dez blocos acima (o mineiro a y90 sobre y80).
        assertFalse(MinerDirectPath.endReachesApproach(approach.up(10), approach),
                "o fim na superfície, dez blocos acima no Y, não alcança a pedra");
        assertFalse(MinerDirectPath.endReachesApproach(approach.up(3), approach),
                "três blocos acima no Y já passa da folga de dois");
    }

    @Test
    void noPathEndDoesNotReach() {
        BlockPos approach = new BlockPos(254, 80, 1153);

        assertFalse(MinerDirectPath.endReachesApproach(null, approach),
                "sem fim de caminho (path nulo) não alcança");
    }
}
