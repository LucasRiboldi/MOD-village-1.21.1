package com.villagecolony.fabric.integration;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A retomada da varredura em anel, e o que custa não tê-la — P1.5,
 * 2026-09-11.
 *
 * <p>O {@code CropPatch} tinha a própria espiral escrita à mão, com
 * orçamento de 2.048 colunas e <b>sem cursor</b>. Toda passagem
 * recomeçava do centro, e o quadrado de raio 32 do fazendeiro tem
 * <b>4.225 colunas</b>: a varredura fechava o anel 22 e abortava no 23.
 * <b>Metade da área prometida, sempre a mesma metade, para sempre.</b>
 *
 * <p>E o efeito não era só desperdício. O {@code ConstructionPlanner}
 * abre roça até {@code FarmerWork.reach()} do centro — 32 —, então uma
 * roça que a própria colônia mandou construir entre 23 e 32 blocos ficava
 * invisível ao fazendeiro dela.
 *
 * <p>O {@link RingSweep} já resolvia isso desde 2026-08-20, e era a
 * quarta espiral do projeto; estes casos fixam o contrato de que o
 * {@code CropPatch} passou a depender. O último deles reproduz o
 * comportamento antigo de propósito — varrer sempre do zero — e mostra
 * que o alvo nunca aparece.
 */
class RingSweepResumeTest {

    private static final BlockPos CENTER = new BlockPos(0, 64, 0);

    /**
     * Um anel fora do alcance de uma passagem só.
     *
     * <p>Vinte: a casca inteira até ele são {@code 41² = 1.681} colunas,
     * contra as {@link RingSweep#MAX_COLUMNS} de uma passagem. Escolhido
     * assim para que o caso não dependa do valor exato do orçamento —
     * qualquer teto abaixo de 1.681 mantém a prova de pé.
     */
    private static final int BEYOND_ONE_PASS = 20;

    private final UUID owner = UUID.randomUUID();

    private final List<BlockPos> looked = new ArrayList<>();

    @BeforeEach
    void forgetTheLastSweep() {
        RingSweep.clearAll();

        looked.clear();
    }

    /** O que a varredura pergunta a cada coluna, anotando por onde passou. */
    private Optional<BlockPos> lookingFor(BlockPos target, BlockPos at) {
        looked.add(at);

        return at.getX() == target.getX() && at.getZ() == target.getZ()
                ? Optional.of(at)
                : Optional.empty();
    }

    /**
     * Uma coluna que nunca serve, anotando por onde a varredura passou.
     *
     * <p>Anotar é a parte que importa: um predicado que só devolve vazio
     * deixa o caso sem como contar colunas, e foi o defeito da primeira
     * versão deste arquivo.
     */
    private Optional<BlockPos> nothingAt(BlockPos at) {
        looked.add(at);

        return Optional.empty();
    }

    /**
     * Varre até a volta fechar, com teto.
     *
     * <p>O teto não é enfeite: sem ele um cursor que deixasse de andar
     * trava a bateria inteira em vez de falhar.
     */
    private void sweepUntilTheRoundCloses(int radius) {
        for (int pass = 0; pass < 20; pass++) {
            RingSweep.around(owner, CENTER, radius, this::nothingAt);

            if (RingSweep.pausedAt(owner).isEmpty()) {
                return;
            }
        }

        throw new AssertionError("a volta não fechou em vinte passagens");
    }

    private static BlockPos atRing(int ring) {
        return CENTER.add(ring, 0, 0);
    }

    /**
     * Uma passagem não alcança o anel 20, e <b>diz</b> que não alcançou.
     *
     * <p>As duas metades importam. Que não ache é o orçamento fazendo o
     * trabalho dele; que o vazio venha acompanhado de um cursor é o que
     * separa <i>"não há"</i> de <i>"não terminei de olhar"</i>. Sem a
     * segunda, quem chama conclui que o campo está vazio e vai embora.
     */
    @Test
    void onePassDoesNotReachTheOuterRingAndSaysSo() {
        BlockPos target = atRing(BEYOND_ONE_PASS);

        Optional<BlockPos> found = RingSweep.around(
                owner, CENTER, BEYOND_ONE_PASS, at -> lookingFor(target, at));

        assertTrue(found.isEmpty(), "uma passagem não deveria alcançar o anel 20");
        assertEquals(RingSweep.MAX_COLUMNS, looked.size(),
                "a passagem tinha de gastar o orçamento inteiro");
        assertTrue(RingSweep.pausedAt(owner).isPresent(),
                "vazio sem cursor é a varredura dizendo 'não há' quando quer dizer "
                        + "'não terminei'");
    }

    /**
     * E a passagem seguinte o acha, porque retoma de onde parou.
     *
     * <p>É o contrato inteiro numa frase: a volta atravessa passagens.
     */
    @Test
    void theNextPassFindsWhatTheFirstDidNotReach() {
        BlockPos target = atRing(BEYOND_ONE_PASS);

        Optional<BlockPos> found = Optional.empty();
        int passes = 0;

        while (found.isEmpty() && passes < 10) {
            found = RingSweep.around(
                    owner, CENTER, BEYOND_ONE_PASS, at -> lookingFor(target, at));

            passes++;
        }

        assertTrue(found.isPresent(), "o alvo no anel 20 nunca foi achado");
        assertEquals(target.getX(), found.orElseThrow().getX());
        assertTrue(passes > 1, "o caso não prova retomada se uma passagem bastava");
        assertTrue(RingSweep.pausedAt(owner).isEmpty(),
                "achou, e o cursor tinha de sair junto");
    }

    /**
     * A retomada não repaga o que a passagem anterior já respondeu.
     *
     * <p>Era metade do orçamento perdido por passagem antes de 2026-08-25,
     * quando o cursor guardava só o anel e recomeçava do primeiro bloco
     * da casca. O teste conta as colunas: se a retomada repagasse, a
     * soma passaria do total de colunas do raio.
     */
    @Test
    void aResumedSweepDoesNotPayForTheColumnsAlreadyAnswered() {
        sweepUntilTheRoundCloses(BEYOND_ONE_PASS);

        int columnsInRadius = (2 * BEYOND_ONE_PASS + 1) * (2 * BEYOND_ONE_PASS + 1);

        assertEquals(columnsInRadius, looked.size(),
                "a volta inteira tinha de custar exatamente as colunas do raio");
    }

    /** Volta fechada sem achar nada não deixa cursor: a próxima recomeça do centro. */
    @Test
    void aCompletedSweepLeavesNoCursorBehind() {
        sweepUntilTheRoundCloses(BEYOND_ONE_PASS);

        assertTrue(RingSweep.pausedAt(owner).isEmpty(),
                "a volta fechou e o cursor tinha de sair — Regra 23");
    }

    /**
     * <b>E é isto que o {@code CropPatch} fazia:</b> varrer sempre do
     * zero, e nunca chegar lá.
     *
     * <p>O {@code forget} antes de cada passagem reproduz a espiral
     * antiga, que não guardava onde parou. Dez passagens — cinco segundos
     * de jogo, com o fazendeiro perguntando por tique — e o alvo no anel
     * 20 continua invisível. Na sessão de 2026-09-04 foram 86 ciclos.
     *
     * <p>A asserção sobre as colunas é a que explica o resto: as dez
     * passagens olharam <b>a mesma</b> primeira coluna dez vezes, e
     * nenhuma chegou perto do anel 20.
     */
    @Test
    void aSweepThatAlwaysStartsOverNeverReachesTheOuterRing() {
        BlockPos target = atRing(BEYOND_ONE_PASS);

        for (int pass = 0; pass < 10; pass++) {
            RingSweep.forget(owner);

            assertTrue(
                    RingSweep.around(
                            owner, CENTER, BEYOND_ONE_PASS,
                            at -> lookingFor(target, at)).isEmpty(),
                    "sem cursor, nenhuma passagem pode alcançar o anel 20");
        }

        assertEquals(10 * RingSweep.MAX_COLUMNS, looked.size(),
                "dez passagens, e todas gastaram o orçamento inteiro do mesmo miolo");

        assertFalse(looked.contains(target),
                "o alvo no anel 20 foi olhado, e o caso deixou de reproduzir o defeito");
    }
}
