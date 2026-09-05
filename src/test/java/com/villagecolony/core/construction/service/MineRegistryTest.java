package com.villagecolony.core.construction.service;

import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineArm;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A mina é da colônia, e ela dura — 2026-08-20.
 *
 * <p>Duas propriedades, e as duas custaram trabalho de mineiro antes de
 * existirem: a colônia tem <b>uma</b> mina, e a fronteira do que já foi
 * cavado não volta a zero.
 */
class MineRegistryTest {

    private static final ColonyPos MOUTH = new ColonyPos(40, 64, 0);

    private static MineShaft shaft() {
        return MineShaft.from(MOUTH, Side.EAST);
    }

    @Test
    void aColonyWithoutAMineHasNone() {
        assertTrue(new MineRegistry().of(UUID.randomUUID()).isEmpty());
    }

    @Test
    void theSecondMinerFindsTheSameMine() {
        MineRegistry registry = new MineRegistry();

        UUID colonyId = UUID.randomUUID();

        Mine first = registry.open(colonyId, shaft());
        Mine second = registry.open(colonyId, MineShaft.from(new ColonyPos(-40, 64, 0), Side.WEST));

        // O segundo mineiro chegou com outra boca debaixo do braço, e a
        // colônia recusou: uma segunda escada apagaria a fronteira da
        // primeira, e ele recomeçaria dentro da mina já cavada.
        assertSame(first, second);
        assertEquals(MOUTH, second.entry());
        assertEquals(1, registry.count());
    }

    @Test
    void twoColoniesDigTheirOwnMines() {
        MineRegistry registry = new MineRegistry();

        UUID one = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        registry.open(one, shaft());
        registry.open(other, MineShaft.from(new ColonyPos(-40, 64, 0), Side.WEST));

        assertEquals(2, registry.count());
        assertNotEquals(
                registry.of(one).orElseThrow().entry(),
                registry.of(other).orElseThrow().entry());
    }

    @Test
    void theCursorAdvancesAndIsRemembered() {
        MineRegistry registry = new MineRegistry();

        UUID colonyId = UUID.randomUUID();

        Mine mine = registry.open(colonyId, shaft());

        for (int i = 0; i < 5; i++) {
            mine.arm(0).nextPosition();
        }

        assertEquals(5, registry.of(colonyId).orElseThrow().arm(0).cut());
    }

    /**
     * A posição sai na ordem de cavar, e a fronteira é onde se parou.
     *
     * <p>Prova a razão de o cursor existir: a sessão seguinte pergunta a
     * posição de número {@code cut} e recebe a que vem depois da última
     * olhada — sem revarrer degrau por degrau o que já está aberto.
     */
    @Test
    void theFrontierIsWhereTheNextCutBegins() {
        Mine mine = Mine.open(UUID.randomUUID(), shaft());

        ColonyPos first = mine.arm(0).nextPosition();
        ColonyPos second = mine.arm(0).nextPosition();

        assertEquals(shaft().positionAt(0), first);
        assertEquals(shaft().positionAt(1), second);

        Mine reopened = Mine.restore(UUID.randomUUID(), shaft(), mine.arm(0).cut());

        assertEquals(shaft().positionAt(2), reopened.arm(0).nextPosition());
    }

    /**
     * Os quatro ramais apontam para quatro lados — 2026-09-04.
     *
     * <p>Este teste dizia outra coisa até hoje: <i>a galeria guarda a
     * curva que deu</i>, porque um cursor só visitava os quatro rumos em
     * sequência e o rumo da vez tinha de sobreviver ao fechar do mundo.
     *
     * <p>Com um ramal por mineiro os quatro rumos existem ao mesmo tempo,
     * e o que se afirma passou a ser isso — nenhum ramal cava para onde
     * outro já cava. A boca continua sendo uma só, que é o que faz deles
     * ramais da mesma escada e não quatro minas.
     */
    @Test
    void theFourBranchesFaceFourDifferentWays() {
        Mine mine = Mine.open(UUID.randomUUID(), shaft());

        Set<Side> headings = new HashSet<>();

        for (MineArm arm : mine.arms()) {
            headings.add(arm.shaft().gallery());

            assertEquals(MOUTH, arm.shaft().entry());
        }

        assertEquals(Mine.ARMS, headings.size(),
                "dois ramais cavam para o mesmo lado: " + headings);

        assertEquals(MOUTH, mine.entry());
    }

    @Test
    void aRestoredMineNeverStartsBehindTheStart() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Mine.restore(UUID.randomUUID(), shaft(), -1));
    }

    /**
     * A posição do túnel espera quando a picareta vai ao minério.
     *
     * <p>O caso é real: o túnel chegou a uma pedra cavável e havia carvão
     * colado nela. Sem desandar o cursor, ele passaria por cima da pedra
     * e o túnel ficaria com um bloco no meio para sempre.
     */
    @Test
    void theTunnelPositionWaitsWhileTheOreIsTaken() {
        Mine mine = Mine.open(UUID.randomUUID(), shaft());

        ColonyPos first = mine.arm(0).nextPosition();

        mine.arm(0).holdPosition();

        assertEquals(first, mine.arm(0).nextPosition());
    }

    /**
     * Bloco que não deu para alcançar volta a ser oferecido —
     * 2026-08-27.
     *
     * <p><b>O autor foi olhar em jogo, e a frase dele fecha o caso:</b>
     * <i>"tive que cavar até lá"</i>. O mod dizia estar cavando a galeria
     * havia três sessões, e no mundo estava rocha maciça.
     *
     * <p>{@link Mine#nextPosition()} avança o cursor <b>sempre</b>. Quando
     * o mineiro não conseguia chegar na pedra, a tarefa voltava para a
     * fila e a posição ficava para trás: o cursor marchava por dentro da
     * rocha, coluna após coluna, e o túnel nunca era aberto. É o mesmo
     * defeito que o {@code holdPosition} já conserta quando a picareta
     * desvia para o minério — <i>"o túnel ficaria com um bloco no meio
     * para sempre"</i> —, e ninguém o chamava na desistência.
     */
    @Test
    void aStoneThatCouldNotBeReachedIsOfferedAgain() {
        Mine mine = Mine.open(UUID.randomUUID(), shaft());

        ColonyPos unreachable = mine.arm(0).nextPosition();

        assertTrue(mine.arm(0).holdPositionAt(unreachable), "o cursor não desandou");

        assertEquals(unreachable, mine.arm(0).nextPosition());
    }

    /**
     * Só desanda quando a posição abandonada é a última entregue.
     *
     * <p>A mina é da colônia e dois mineiros a partilham. Desandar às
     * cegas devolveria o cursor por cima do bloco que o <b>outro</b>
     * acabou de pegar, e os dois passariam a brigar pelo mesmo ponto.
     */
    @Test
    void onlyTheLastHandedOutPositionRollsBack() {
        Mine mine = Mine.open(UUID.randomUUID(), shaft());

        ColonyPos mine_ = mine.arm(0).nextPosition();
        ColonyPos theOther = mine.arm(0).nextPosition();

        assertFalse(
                mine.arm(0).holdPositionAt(mine_),
                "desandou por cima do bloco que o outro mineiro pegou");

        assertTrue(mine.arm(0).holdPositionAt(theOther));

        assertEquals(theOther, mine.arm(0).nextPosition());
    }

    /**
     * A galeria sabe recuar até a frente de verdade — 2026-08-27.
     *
     * <p>Quando o cursor marchou por dentro da rocha, a posição que ele
     * aponta não tem túnel atrás dela: não há de onde alcançá-la, e o
     * mineiro é mandado para dentro da pedra. Recuar é o único caminho
     * de volta — a ordem de cavar é um caminho para fora da boca, então
     * a posição anterior está sempre mais perto do que já está aberto.
     */
    @Test
    void theGalleryCanBackUpToWhereItReallyEnds() {
        Mine mine = Mine.open(UUID.randomUUID(), shaft());

        for (int i = 0; i < 10; i++) {
            mine.arm(0).nextPosition();
        }

        assertEquals(10, mine.arm(0).cut());

        mine.arm(0).backUp();
        mine.arm(0).backUp();

        assertEquals(8, mine.arm(0).cut());
        assertEquals(mine.shaft().positionAt(8), mine.arm(0).nextPosition());
    }

    /**
     * A frente é lida do mundo, e não lembrada — 2026-08-28.
     *
     * <p><b>O recuo de ontem parava cedo demais.</b> Ele voltava até achar
     * uma posição de onde dava para bater — e o <b>túnel que o jogador
     * cavou à mão</b> oferece exatamente isso, num bolsão que não se liga
     * à escada do mod. O mineiro ficava no degrau 7 mirando uma lanterna
     * a vinte e quatro blocos, do outro lado da rocha.
     *
     * <p>A frente de verdade é a <b>primeira posição ainda fechada</b> na
     * ordem de cavar. Ela é conectada por construção: tudo o que vem
     * antes já está aberto, e a ordem é um caminho contínuo a partir da
     * boca. Nenhum bolsão solto pode enganá-la.
     */
    @Test
    void theFrontierIsWhereverTheCursorIsToldToGo() {
        Mine mine = Mine.open(UUID.randomUUID(), shaft());

        for (int i = 0; i < 40; i++) {
            mine.arm(0).nextPosition();
        }

        mine.arm(0).rewindTo(7);

        assertEquals(7, mine.arm(0).cut());
        assertEquals(mine.shaft().positionAt(7), mine.arm(0).nextPosition());
    }

    /**
     * Bloco solto dentro de galeria aberta não é frente — 2026-09-02.
     *
     * <p><b>A sessão de 2026-09-02, 19:19.</b> O log e o código, lado a
     * lado:
     *
     * <pre>
     * The gallery really ends at 720, 44, 878 — the cursor was 83 steps ahead of it
     * private static final int CUTS_PER_SEARCH = 64;
     * </pre>
     *
     * <p>A procura varria a ordem inteira desde o primeiro degrau e
     * parava no primeiro bloco fechado que achasse. Numa galeria <b>já
     * cavada</b> — e o autor desceu e conferiu que ali começa corredor
     * aberto — esse primeiro bloco fechado é um resto solto no meio do
     * túnel, não a frente de escavação. O cursor recuava 83 passos, a
     * busca só reanda 64, e nenhuma passagem reencontrava o que a
     * anterior largou. Dois mineiros, vinte minutos, dois blocos.
     *
     * <p>O javadoc do {@code findTheFrontier} diz a premissa certa —
     * <i>tudo o que vem antes já está aberto</i> — e a usava ao
     * contrário, como se tudo o que vem <b>depois</b> estivesse fechado.
     */
    @Test
    void aLooseBlockInsideAnOpenGalleryIsNotTheFrontier() {
        Mine mine = Mine.open(UUID.randomUUID(), shaft());

        for (int i = 0; i < 100; i++) {
            mine.arm(0).nextPosition();
        }

        assertTrue(mine.arm(0).frontierWhereRockBegins(i -> i == 5).isEmpty());
    }

    /**
     * A frente é a rocha que continua, e não o primeiro bloco fechado.
     *
     * <p>É a distinção inteira numa linha: um resto solto tem túnel
     * aberto logo depois dele; a frente de escavação tem mais rocha.
     */
    @Test
    void theFrontierIsTheRockThatKeepsGoing() {
        Mine mine = Mine.open(UUID.randomUUID(), shaft());

        for (int i = 0; i < 100; i++) {
            mine.arm(0).nextPosition();
        }

        assertEquals(60, mine.arm(0).frontierWhereRockBegins(i -> i == 5 || i >= 60).orElseThrow());
    }

    /**
     * O cursor que disparou volta inteiro, ainda que o mundo não confirme
     * o caminho todo — o E33.
     *
     * <p>A outra ponta, e ela não pode ter teto nem depender do que há
     * logo atrás do cursor: o save de 08-27 chegou com ele a milhares de
     * passos, e o que se lê lá fora do limite de construção é <b>vazio</b>,
     * não rocha. Procurar de trás para frente morria no primeiro vazio e
     * deixava a mina presa — foi o que os dois gametests do E33 disseram
     * em 2026-09-02, e é por isso que a procura anda a partir da boca.
     */
    @Test
    void theCursorThatRanAheadOutOfTheWorldStillComesBack() {
        Mine mine = Mine.open(UUID.randomUUID(), shaft());

        for (int i = 0; i < 200; i++) {
            mine.arm(0).nextPosition();
        }

        assertEquals(40, mine.arm(0).frontierWhereRockBegins(i -> i >= 40 && i < 120).orElseThrow());
    }

    /** Galeria inteira aberta: não há frente para recuar. */
    @Test
    void anOpenGalleryHasNoFrontierToBackUpTo() {
        Mine mine = Mine.open(UUID.randomUUID(), shaft());

        for (int i = 0; i < 100; i++) {
            mine.arm(0).nextPosition();
        }

        assertTrue(mine.arm(0).frontierWhereRockBegins(i -> false).isEmpty());
    }

    /** A frente nunca vai para antes do primeiro degrau, nem para trás do fim. */
    @Test
    void theFrontierStaysInsideTheOrder() {
        Mine mine = Mine.open(UUID.randomUUID(), shaft());

        mine.arm(0).rewindTo(-5);

        assertEquals(0, mine.arm(0).cut());
    }

    /** Recuar do começo não leva a picareta para antes do primeiro degrau. */
    @Test
    void backingUpNeverGoesBehindTheStart() {
        Mine mine = Mine.open(UUID.randomUUID(), shaft());

        mine.arm(0).backUp();
        mine.arm(0).backUp();

        assertEquals(0, mine.arm(0).cut());
    }

    /** Desandar do começo não leva a picareta para antes do primeiro degrau. */
    @Test
    void theCursorNeverGoesBehindTheStart() {
        Mine mine = Mine.open(UUID.randomUUID(), shaft());

        mine.arm(0).holdPosition();

        assertEquals(0, mine.arm(0).cut());
    }

    /**
     * A veia é lembrada até acabar, e a virada da galeria não a leva.
     *
     * <p>Minério não vem sozinho, e voltar ao túnel com a veia pela
     * metade faria o aldeão andar até lá outra vez na passagem seguinte.
     */
    @Test
    void theVeinIsRememberedUntilItRunsOut() {
        Mine mine = Mine.open(UUID.randomUUID(), shaft());

        assertTrue(mine.arm(0).vein().isEmpty());

        ColonyPos ore = new ColonyPos(41, 54, 3);

        mine.arm(0).followVein(ore);

        assertEquals(ore, mine.arm(0).vein().orElseThrow());

        mine.arm(0).veinExhausted();

        assertTrue(mine.arm(0).vein().isEmpty());
    }

    /**
     * A galeria vira depois de oito recusas seguidas, e não de dezesseis.
     *
     * <p>A contagem é da mina, e não do mineiro: dois na mesma escada
     * esbarram na mesma lava, e duas contagens separadas pediriam o dobro
     * de recusas para uma curva que precisa de oito.
     */
    @Test
    void theGalleryTurnsOnceTheRefusalsAddUp() {
        Mine mine = Mine.open(UUID.randomUUID(), shaft());

        for (int i = 0; i < 7; i++) {
            assertFalse(mine.arm(0).blockedAgain(8), "fechou cedo demais, na recusa " + (i + 1));
        }

        // <b>Fecha o ramal, e não vira a galeria</b> — 2026-09-04. Este
        // teste media o rumo da galeria mudando, e media certo: com um
        // cursor só, oito recusas o giravam. Com um ramal por mineiro o
        // rumo é fixo, e desistir de uma barreira é desistir do ramal.
        assertTrue(mine.arm(0).blockedAgain(8));
        assertTrue(mine.arm(0).isDone());

        // E os outros três continuam de pé: a lava é de um lugar.
        assertFalse(mine.everyArmIsDone());

        // A contagem do ramal seguinte também pede oito, do zero.
        assertFalse(mine.arm(1).blockedAgain(8));
    }

    /** Picareta que pega zera a conta das recusas. */
    @Test
    void diggingClearsTheRefusals() {
        Mine mine = Mine.open(UUID.randomUUID(), shaft());

        Side before = mine.shaft().gallery();

        for (int i = 0; i < 7; i++) {
            mine.arm(0).blockedAgain(8);
        }

        mine.arm(0).digging();

        assertFalse(mine.arm(0).blockedAgain(8));
        assertEquals(before, mine.shaft().gallery());
    }

    @Test
    void aColonyThatGoesAwayTakesItsMine() {
        MineRegistry registry = new MineRegistry();

        UUID colonyId = UUID.randomUUID();

        registry.open(colonyId, shaft());
        registry.removeOfColony(colonyId);

        assertTrue(registry.of(colonyId).isEmpty());
        assertFalse(registry.all().iterator().hasNext());
    }

    /**
     * Fechado o círculo, a mina desce um nível — 2026-09-02, e a conta
     * mudou de forma em 2026-09-04 sem mudar de conteúdo.
     *
     * <p>Eram quatro curvas do mesmo cursor: a galeria voltava à direção
     * em que começou, tinha dado a volta no nível, e o que sobrava estava
     * abaixo. São quatro <b>ramais fechados</b> — os mesmos quatro rumos,
     * agora podendo ser fechados por quatro aldeões ao mesmo tempo.
     *
     * <p>Os cursores voltam a zero porque o poço do nível novo ainda não
     * foi cavado: são duas descidas e duas salas antes de a galeria
     * começar.
     */
    @Test
    void theGalleryDescendsAfterAFullCircle() {
        Mine mine = Mine.open(UUID.randomUUID(), shaft());

        for (int i = 0; i < 50; i++) {
            mine.arm(0).nextPosition();
        }

        int before = mine.shaft().positionAt(MineShaft.CARVED).y();

        for (MineArm arm : mine.arms()) {
            arm.finish();
        }

        assertTrue(mine.deepenIfEveryArmIsDone(), "os quatro ramais fecharam e ela não desceu");

        assertEquals(
                before - 2 * MineShaft.DESCENT,
                mine.shaft().positionAt(MineShaft.CARVED).y());

        assertEquals(0, mine.arm(0).cut());
    }

    /**
     * No fundo ela não desce: abaixo do pico não há o que procurar.
     *
     * <p>Os ramais reabrem no mesmo nível em vez de a mina parar — é pior
     * recavar do que deixar a colônia sem mineiro para sempre, e o
     * {@code findTheFrontier} passa por cima do que já é ar.
     */
    @Test
    void theGalleryKeepsTurningAtTheDeepestLevel() {
        Mine mine = Mine.open(
                UUID.randomUUID(),
                MineShaft.from(
                        new ColonyPos(40, MineShaft.DEEPEST + 2 * MineShaft.DESCENT, 0),
                        Side.EAST));

        int before = mine.shaft().positionAt(MineShaft.CARVED).y();

        for (MineArm arm : mine.arms()) {
            arm.finish();
        }

        assertFalse(mine.deepenIfEveryArmIsDone(), "ela desceu abaixo do pico");

        assertEquals(before, mine.shaft().positionAt(MineShaft.CARVED).y());

        // E os ramais voltaram a aceitar picareta, no mesmo nível.
        assertFalse(mine.everyArmIsDone(), "a mina do fundo ficou sem frente nenhuma");
    }
}
