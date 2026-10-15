package com.villagecolony.fabric.work;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Antes de largar uma pedra que não tem lugar de pé, o mineiro tenta cavar a pedra ao lado para
 * abrir o pé (ADR-038 P6, estendido ao julgamento de perto) — P1 do plano de melhoria, 2026-10-15.
 *
 * <p>A pedra ao lado só ajuda quando o motivo é <b>não há pé</b> ({@code NO_STANDING_POSITION}):
 * cavá-la abaixa o acesso e expõe um lugar de pé. Quando o motivo é <b>pé alto demais</b>
 * ({@code TOO_HIGH} — caverna embaixo da galeria, o único pé fica acima do degrau), cavar o vizinho
 * não baixa o pé; aí não se tenta o desvio e a galeria contorna (o {@code passOver} marca e o cursor
 * pula). {@code NO_PATH} é caminho, não geometria, e também não se resolve cavando ao lado.
 *
 * <p>Lógica pura — no padrão de {@link MineApproachProgressTest} e {@link MinerDirectPathTest}.
 */
class MinerRerouteTest {

    @BeforeEach
    void forgetMarks() {
        MineMarks.clearAll();
    }

    @Test
    void noStandingPositionIsWorthARerouteAttempt() {
        assertTrue(MinerHands.worthRerouting(MiningTarget.Rejection.NO_STANDING_POSITION),
                "sem pé ao lado: cavar o vizinho abre o pé — vale tentar");
    }

    @Test
    void anIsolatedRefusalStillReroutes() {
        // Uma recusa só não forma beco: o desvio ao vizinho ainda vale.
        BlockPos stone = new BlockPos(-120, 63, 668);
        MineMarks.refuseAt(1000, stone);

        assertTrue(MinerHands.rerouteWorthTryingAt(1000, stone),
                "pedra emparedada isolada: o vizinho abre o pé, vale desviar");
    }

    @Test
    void aStoneInsideADeadEndDoesNotReroute() {
        // Parede/teto de caverna: três ou mais recusas num cubo pequeno. O vizinho está no mesmo
        // beco, então o desvio só troca NO_STANDING por outro NO_STANDING/TOO_HIGH — contorna, não
        // desvia (playtest 0.3.60, cluster y67-68).
        MineMarks.refuseAt(1000, new BlockPos(-128, 68, 656));
        MineMarks.refuseAt(1000, new BlockPos(-128, 68, 657));
        MineMarks.refuseAt(1000, new BlockPos(-129, 68, 656));
        BlockPos inWall = new BlockPos(-128, 68, 658);

        assertFalse(MinerHands.rerouteWorthTryingAt(1000, inWall),
                "pedra dentro de um beco: o vizinho está na mesma parede — não desvia");
    }

    @Test
    void tooHighAndNoPathAreNotReroutable() {
        assertFalse(MinerHands.worthRerouting(MiningTarget.Rejection.TOO_HIGH),
                "pé alto demais não baixa cavando ao lado — contorna, não desvia");
        assertFalse(MinerHands.worthRerouting(MiningTarget.Rejection.NO_PATH),
                "sem caminho é navegação, não geometria — contorna");
    }

    @Test
    void noneIsNotReroutable() {
        assertFalse(MinerHands.worthRerouting(MiningTarget.Rejection.NONE),
                "pedra elegível não chega a passOver — nada a desviar");
    }
}
