package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.Mine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Um mineiro por ramal — 2026-08-28, e por ramal desde 2026-09-04.
 *
 * <p><b>A sessão de 2026-08-26, 23:23:08.</b> A colônia tinha dois
 * mineiros e duas tarefas de pedra — uma por recurso pedido, que é como
 * o {@code ColonyCycle} as abre —, e as duas apontavam para a mesma
 * escada. A reserva existia, mas era da <b>tarefa</b>: cada um tinha a
 * sua, e nenhuma delas falava da mina.
 *
 * <p>O cursor da galeria é do {@code Mine}, e é um só. Os dois recebiam
 * a mesma posição na mesma passagem, andavam para o mesmo lugar, e
 * davam {@code could not reach the stone} no mesmo tique — e o recuo do
 * cursor que esse aviso dispara rodava duas vezes por um bloco.
 *
 * <p>É o mesmo problema que o {@link TreeClaims} resolveu do lado do
 * lenhador, e a resposta é a mesma: a coisa disputada é que tem dono.
 * Ali é a árvore; aqui era a mina.
 *
 * <p><b>E passou a ser o ramal</b> — decisão do autor, 2026-09-04:
 * <i>"mineiros distintos escolhem caminhos de perfuração distintos
 * dentro das minas"</i>. O raciocínio de cima continua inteiro — dois na
 * <b>mesma</b> frente ainda recuariam o cursor duas vezes por um bloco —,
 * e o que mudou é que a {@code Mine} passou a ter quatro frentes. Vários
 * testes daqui afirmavam a exclusividade da escada e hoje afirmam a do
 * ramal; onde a resposta se inverteu, o javadoc do teste diz que se
 * inverteu.
 */
class MineClaimsTest {

    private static final UUID COLONY = UUID.randomUUID();

    private static final UUID OTHER_COLONY = UUID.randomUUID();

    private final UUID first = UUID.randomUUID();

    private final UUID second = UUID.randomUUID();

    @BeforeEach
    void emptyTheShafts() {
        MineClaims.clearAll();
    }

    /** Mina livre é de quem chegar. */
    @Test
    void theFirstMinerToAskGetsTheShaft() {
        assertTrue(MineClaims.claimArm(COLONY, first, Mine.ARMS).isPresent());
        assertEquals(first, MineClaims.diggerIn(COLONY).orElseThrow());
    }

    /** O segundo não desce: é a escada de um só. */
    /**
     * O segundo mineiro ganha <b>outro</b> ramal — 2026-09-04.
     *
     * <p><b>Este teste afirmava o contrário</b>, e afirmava certo: com um
     * cursor só, o segundo a pedir tinha de ser recusado. O autor pediu
     * ramais, a {@code Mine} passou a ter quatro frentes, e a recusa
     * virou repartição.
     *
     * <p>O que <b>não</b> mudou é o que a recusa protegia: os dois não
     * podem cair na mesma frente. É o índice diferente que afirma isso.
     */
    @Test
    void theSecondMinerGetsAnotherBranch() {
        int mine = MineClaims.claimArm(COLONY, first, Mine.ARMS).orElseThrow();
        int other = MineClaims.claimArm(COLONY, second, Mine.ARMS).orElseThrow();

        assertNotEquals(mine, other, "os dois mineiros caíram no mesmo ramal");
    }

    /** E o quinto espera: são quatro rumos, e o quinto recavaria o primeiro. */
    @Test
    void theFifthMinerWaits() {
        for (int i = 0; i < Mine.ARMS; i++) {
            assertTrue(MineClaims.claimArm(COLONY, UUID.randomUUID(), Mine.ARMS).isPresent());
        }

        assertFalse(MineClaims.claimArm(COLONY, second, Mine.ARMS).isPresent());
        assertEquals(Mine.ARMS, MineClaims.diggersIn(COLONY));
    }

    /**
     * Quem já está dentro continua dentro.
     *
     * <p>A pergunta é feita uma vez por pedra, e não uma vez por mina:
     * uma reserva que não se renovasse expulsaria o dono na segunda
     * pedra dele.
     */
    @Test
    void theHolderKeepsTheShaftOnEveryPass() {
        MineClaims.claimArm(COLONY, first, Mine.ARMS);

        assertTrue(MineClaims.claimArm(COLONY, first, Mine.ARMS).isPresent());
        assertTrue(MineClaims.claimArm(COLONY, first, Mine.ARMS).isPresent());
    }

    /** Duas colônias, duas minas, e nenhuma sabe da outra. */
    @Test
    void eachColonyHasItsOwnShaft() {
        assertTrue(MineClaims.claimArm(COLONY, first, Mine.ARMS).isPresent());
        assertTrue(MineClaims.claimArm(OTHER_COLONY, second, Mine.ARMS).isPresent());
    }

    /** Solto o dono, a mina passa adiante. */
    @Test
    void releasingHandsTheShaftOver() {
        MineClaims.claimArm(COLONY, first, Mine.ARMS);

        MineClaims.release(first);

        assertTrue(MineClaims.diggerIn(COLONY).isEmpty());
        assertTrue(MineClaims.claimArm(COLONY, second, Mine.ARMS).isPresent());
    }

    /**
     * Mineiro sem trabalho aberto perde a mina, e não a tranca.
     *
     * <p><b>É o que impede a reserva de vazar.</b> Morte, zumbificação,
     * tarefa devolvida pelo guarda de travamento: nem todo fim de
     * trabalho passa por um lugar só, e uma mina trancada por um aldeão
     * que não existe mais é pior que dois cavando a mesma escada.
     */
    @Test
    void theShaftPassesOnWhenTheHolderHasNoJobLeft() {
        MineClaims.claimArm(COLONY, first, Mine.ARMS);

        MineClaims.retainOnly(Set.of(second));

        assertTrue(MineClaims.diggerIn(COLONY).isEmpty());
        assertTrue(MineClaims.claimArm(COLONY, second, Mine.ARMS).isPresent());
    }

    /** Quem ainda trabalha não é despejado pela conferência. */
    @Test
    void theWorkingHolderSurvivesTheSweep() {
        int held = MineClaims.claimArm(COLONY, first, Mine.ARMS).orElseThrow();

        MineClaims.retainOnly(Set.of(first, second));

        assertEquals(held, MineClaims.claimArm(COLONY, first, Mine.ARMS).orElseThrow(),
                "a varredura tirou o ramal de quem ainda tem trabalho");

        // E o segundo recebe outro, que é o que mudou em 2026-09-04: a
        // varredura nunca barrou ninguém, quem barrava era a escada
        // única.
        assertNotEquals(held, MineClaims.claimArm(COLONY, second, Mine.ARMS).orElseThrow());
    }

    /** Mina de colônia que ninguém pediu não tem dono. */
    @Test
    void anUnaskedShaftHasNoDigger() {
        assertTrue(MineClaims.diggerIn(COLONY).isEmpty());
    }

    /**
     * Quem desiste cede a vez — 2026-08-29, 04:40.
     *
     * <p><b>A sessão que mostrou por que isto faltava.</b> Um mineiro
     * ficou preso num poço a dois blocos abaixo da passarela — oito
     * leituras seguidas em {@code 757, 42, 877}, sem andar um bloco em
     * seis minutos. Aldeão não sobe dois.
     *
     * <p>E ele <b>segurava a mina</b>. O guarda de travamento devolvia a
     * tarefa a cada 2400 tiques, ele a pegava de volta, e o segundo
     * mineiro — que não estava preso — passou a sessão inteira em
     * {@code waiting for the shaft}. A colônia não recebeu uma pedra.
     *
     * <p>A reserva sozinha não resolve isso: ela impede dois na mesma
     * escada, e nada dizia sobre <b>rodar a vez</b> quando o de dentro
     * não consegue trabalhar. Agora quem desiste é recusado <b>uma
     * vez</b>, e essa uma basta para o outro entrar.
     */
    @Test
    void theMinerWhoGaveUpLetsTheOtherIn() {
        MineClaims.claimArm(COLONY, first, Mine.ARMS);

        MineClaims.stepAside(COLONY, first);

        assertTrue(
                MineClaims.claimArm(COLONY, second, Mine.ARMS).isPresent(),
                "o segundo continuou de fora depois de o primeiro desistir");
    }

    /**
     * E ele recupera a vez se não houver mais ninguém.
     *
     * <p>Uma recusa, e só. Mineiro sozinho numa vila não pode ser punido
     * para sempre por um poço — se ninguém mais pede a mina, ela volta a
     * ser dele na passagem seguinte.
     */
    @Test
    void aLoneMinerGetsTheShaftBackOnTheNextPass() {
        MineClaims.claimArm(COLONY, first, Mine.ARMS);

        MineClaims.stepAside(COLONY, first);

        assertFalse(MineClaims.claimArm(COLONY, first, Mine.ARMS).isPresent());
        assertTrue(MineClaims.claimArm(COLONY, first, Mine.ARMS).isPresent());
    }

    /** Ceder a vez numa mina não mexe na vez da outra. */
    @Test
    void steppingAsideInOneShaftDoesNotTouchTheOther() {
        MineClaims.claimArm(COLONY, first, Mine.ARMS);
        MineClaims.claimArm(OTHER_COLONY, first, Mine.ARMS);

        MineClaims.stepAside(COLONY, first);

        assertTrue(
                MineClaims.claimArm(OTHER_COLONY, first, Mine.ARMS).isPresent(),
                "ele perdeu a mina da outra colônia sem ter desistido dela");
    }

    /**
     * Perguntar quem tem a mina não toma a mina.
     *
     * <p>A diferença entre {@code heldByOther} e {@code claim}, e ela é
     * o ponto: {@code claim} responde e reserva na mesma passagem, então
     * quem só quer saber se vale tentar não pode usá-lo — a pergunta
     * mudaria a resposta.
     */
    @Test
    void askingWhoHoldsTheShaftDoesNotTakeIt() {
        assertFalse(MineClaims.heldByOther(COLONY, first, Mine.ARMS));
        assertTrue(MineClaims.diggerIn(COLONY).isEmpty(), "a pergunta reservou a mina");

        assertTrue(MineClaims.claimArm(COLONY, first, Mine.ARMS).isPresent());
    }

    /**
     * Para quem tem ramal, a mina não está com outro — e para quem não
     * tem, só está quando os quatro acabaram.
     *
     * <p>Aqui também a resposta se inverteu: um mineiro dentro bastava
     * para barrar o segundo, e hoje sobram três frentes para ele.
     */
    @Test
    void theOwnerIsNotAnotherMiner() {
        MineClaims.claimArm(COLONY, first, Mine.ARMS);

        assertFalse(MineClaims.heldByOther(COLONY, first, Mine.ARMS));
        assertFalse(MineClaims.heldByOther(COLONY, second, Mine.ARMS), "sobravam três ramais livres");

        for (int i = 0; i < Mine.ARMS - 1; i++) {
            MineClaims.claimArm(COLONY, UUID.randomUUID(), Mine.ARMS);
        }

        assertTrue(MineClaims.heldByOther(COLONY, second, Mine.ARMS));
    }

    /** Mina de outra colônia não barra ninguém aqui. */
    @Test
    void aShaftInAnotherColonyDoesNotHoldThisOne() {
        MineClaims.claimArm(OTHER_COLONY, first, Mine.ARMS);

        assertFalse(MineClaims.heldByOther(COLONY, second, Mine.ARMS));
    }
}
