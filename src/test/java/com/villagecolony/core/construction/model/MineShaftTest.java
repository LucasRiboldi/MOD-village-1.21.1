package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A mina — a Regra 29, e ela é geometria.
 *
 * <p>Fora do jogo de propósito: a mina tem cento e cinquenta e duas
 * posições antes da galeria, e conferi-las cavando levaria uma sessão.
 * Aqui levam milissegundos, e o que se afirma é a forma — que é o que o
 * autor descreveu.
 */
class MineShaftTest {

    private static final ColonyPos ENTRY = new ColonyPos(100, 64, 200);

    private static MineShaft shaft() {
        return MineShaft.from(ENTRY, Side.EAST);
    }

    /**
     * A escada desce um por passo, e nunca dois.
     *
     * <p>É a frase do autor: <i>"de modo que ele possa subir de volta"</i>.
     * Um degrau de dois blocos é um poço com aparência de escada.
     */
    @Test
    void theStairDropsOneBlockPerStep() {
        MineShaft mine = shaft();

        for (int step = 1; step <= MineShaft.DESCENT; step++) {
            ColonyPos feet = mine.positionAt((step - 1) * MineShaft.STAIR_HEADROOM);

            assertEquals(ENTRY.y() - step + 1, feet.y(),
                    "o degrau " + step + " não desceu um bloco");

            assertEquals(ENTRY.x() + step, feet.x(), "o degrau " + step + " não andou um bloco");
        }
    }

    /** Cada degrau abre os pés e a cabeça, e mais um para passar. */
    @Test
    void everyStepIsTallEnoughToStandIn() {
        MineShaft mine = shaft();

        for (int step = 1; step <= MineShaft.DESCENT; step++) {
            ColonyPos feet = mine.positionAt((step - 1) * MineShaft.STAIR_HEADROOM);
            ColonyPos head = mine.positionAt((step - 1) * MineShaft.STAIR_HEADROOM + 1);

            assertEquals(feet.y() + 1, head.y(), "a cabeça do degrau " + step + " não abriu");
            assertEquals(feet.x(), head.x());
            assertEquals(feet.z(), head.z());
        }
    }

    /**
     * O aldeão consegue andar escada abaixo sem cavar mais nada.
     *
     * <p><b>Visto em jogo em 2026-08-27</b>, e a frase do autor: <i>"o
     * mineiro precisa quebrar mais um bloco na sua frente para poder
     * descer a escada"</i>. A escada tinha dois blocos por degrau, que é
     * quanto o aldeão ocupa parado — e não é quanto ele precisa para
     * <b>andar</b>.
     *
     * <p>Descer um degrau é primeiro andar para a frente, no mesmo nível,
     * e só então cair. Nesse instante a cabeça dele está um bloco acima
     * do teto do degrau seguinte, e o teto não tinha sido cavado. Ele
     * ficava preso, batia a picareta, e a mina só descia porque o
     * jogador abria o caminho.
     *
     * <p>Vale para os dois lances: o segundo parte do canto da sala e
     * desce igual.
     */
    @Test
    void theVillagerWalksDownWithoutDiggingAgain() {
        MineShaft mine = shaft();

        for (int flight = 0; flight < 2; flight++) {
            int base = flight == 0
                    ? 0
                    : MineShaft.DESCENT * MineShaft.STAIR_HEADROOM
                            + MineShaft.ROOM_LONG * MineShaft.ROOM_WIDE * MineShaft.HEADROOM;

            for (int step = 1; step < MineShaft.DESCENT; step++) {
                Set<Integer> ahead = new HashSet<>();

                for (int k = 0; k < MineShaft.STAIR_HEADROOM; k++) {
                    ahead.add(mine.positionAt(base + step * MineShaft.STAIR_HEADROOM + k).y());
                }

                int feet = mine.positionAt(base + (step - 1) * MineShaft.STAIR_HEADROOM).y();

                assertTrue(
                        ahead.contains(feet),
                        "lance " + flight + ", degrau " + step
                                + ": os pés não passam para o degrau seguinte");

                assertTrue(
                        ahead.contains(feet + 1),
                        "lance " + flight + ", degrau " + step
                                + ": a cabeça bate no teto do degrau seguinte");
            }
        }
    }

    /** Dez degraus levam a dez blocos abaixo da entrada. */
    @Test
    void tenStepsReachTenBlocksDown() {
        ColonyPos last = shaft().positionAt(
                MineShaft.DESCENT * MineShaft.STAIR_HEADROOM - MineShaft.STAIR_HEADROOM);

        assertEquals(ENTRY.y() - MineShaft.DESCENT + 1, last.y());
    }

    /**
     * O segundo lance vira, e não continua reto.
     *
     * <p>Continuar reto daria um corredor inclinado de vinte blocos; a
     * curva é o que mantém a mina compacta e a subida curta.
     */
    @Test
    void theSecondFlightTurns() {
        MineShaft mine = shaft();

        ColonyPos first = mine.positionAt(0);
        ColonyPos second = mine.positionAt(MineShaft.CARVED - 1);

        assertNotEquals(first.z(), second.z(), "o segundo lance não virou para lado nenhum");
    }

    /** A sala é sete por quatro, e de dois de altura. */
    @Test
    void eachRoomIsSevenByFourAndTwoTall() {
        MineShaft mine = shaft();

        Set<String> floor = new HashSet<>();
        Set<Integer> heights = new HashSet<>();

        int from = MineShaft.DESCENT * MineShaft.STAIR_HEADROOM;
        int upTo = from + MineShaft.ROOM_LONG * MineShaft.ROOM_WIDE * MineShaft.HEADROOM;

        for (int i = from; i < upTo; i++) {
            ColonyPos at = mine.positionAt(i);

            floor.add(at.x() + ":" + at.z());
            heights.add(at.y());
        }

        assertEquals(MineShaft.ROOM_LONG * MineShaft.ROOM_WIDE, floor.size(),
                "a sala não tem sete por quatro de chão");

        assertEquals(MineShaft.HEADROOM, heights.size(), "a sala não tem dois de altura");
    }

    /** A segunda sala fica vinte blocos abaixo da entrada. */
    @Test
    void theSecondRoomSitsTwentyBlocksDown() {
        ColonyPos at = shaft().positionAt(MineShaft.CARVED - 1);

        assertTrue(
                at.y() <= ENTRY.y() - 2 * MineShaft.DESCENT + MineShaft.HEADROOM,
                "a segunda sala ficou em " + at.y() + ", e devia estar por volta de "
                        + (ENTRY.y() - 2 * MineShaft.DESCENT));
    }

    /**
     * A galeria não acaba, e fica toda no nível da segunda sala.
     *
     * <p>É a frase do autor: <i>"na camada 20 ele começa a recolher na
     * altura do aldeão mais 1 infinitamente"</i>.
     *
     * <p><b>Continua verdadeiro depois do braço de 2026-09-04</b>, e a
     * distinção importa: a <b>forma</b> não tem fim — {@code positionAt}
     * responde para qualquer índice, e tem de responder, porque o cursor
     * do save pode estar em qualquer um. Quem tem fim é o <b>trecho que
     * se cava antes de virar</b>, e quem o faz virar é o
     * {@code MineDigging}, perguntando ao {@link MineShaft#beyondTheArm}.
     */
    @Test
    void theGalleryRunsForeverOnOneLevel() {
        MineShaft mine = shaft();

        int level = mine.positionAt(MineShaft.CARVED).y();

        for (int i = MineShaft.CARVED; i < MineShaft.CARVED + 2_000; i++) {
            int y = mine.positionAt(i).y();

            assertTrue(
                    y == level || y == level + 1,
                    "a galeria saiu do nível dela em " + y);
        }
    }

    /** E cada passo dela avança de verdade, sem repetir posição. */
    @Test
    void theGalleryAdvancesInsteadOfDiggingTheSameHole() {
        MineShaft mine = shaft();

        Set<ColonyPos> seen = new HashSet<>();

        for (int i = MineShaft.CARVED; i < MineShaft.CARVED + 200; i++) {
            assertTrue(seen.add(mine.positionAt(i)), "a galeria repetiu uma posição");
        }
    }

    /**
     * Barreira à frente: a galeria vira, e a escada fica onde estava.
     *
     * <p>Virar a mina inteira jogaria fora os cento e cinquenta e dois
     * blocos já cavados e o caminho de volta do aldeão.
     */
    @Test
    void aBlockedGalleryTurnsWithoutMovingTheStair() {
        MineShaft mine = shaft();
        MineShaft turned = mine.turned();

        assertEquals(mine.positionAt(0), turned.positionAt(0), "a escada mudou de lugar");

        assertNotEquals(
                mine.positionAt(MineShaft.CARVED + 4),
                turned.positionAt(MineShaft.CARVED + 4),
                "a galeria não virou");
    }

    /** Nenhuma posição da parte cavada se repete. */
    @Test
    void theCarvedPartNeverDigsTheSameBlockTwice() {
        MineShaft mine = shaft();

        Set<ColonyPos> seen = new HashSet<>();

        for (int i = 0; i < MineShaft.CARVED; i++) {
            assertTrue(seen.add(mine.positionAt(i)), "a mina repetiu a posição de índice " + i);
        }
    }

    /**
     * A mina desce um nível de cada vez — 2026-09-02.
     *
     * <p><b>A galeria trabalhava a cem blocos do minério bom.</b> A
     * sessão daquele dia mostrou os alvos em {@code y=44}, e o pico do
     * diamante em 1.21 é {@code y=-59}: enquanto a mina ficar naquela
     * altura, procurar minério melhor não tem o que achar.
     *
     * <p>A forma é a do MineColonies, e é decisão do autor: a
     * profundidade <b>cresce aos poucos</b> em vez de mudar de uma vez.
     * Lá quem manda é o nível do prédio; aqui é a galeria ter fechado o
     * círculo — quatro curvas e ela voltou à direção em que começou,
     * tendo dado a volta no nível.
     *
     * <p>O nível seguinte começa onde a galeria deste está, e por isso
     * cada nível custa duas descidas: vinte blocos.
     */
    @Test
    void theNextLevelIsTwoDescentsBelowThisOne() {
        MineShaft shaft = MineShaft.from(new ColonyPos(40, 64, 0), Side.EAST);

        MineShaft deeper = shaft.deepened();

        assertEquals(
                shaft.positionAt(MineShaft.CARVED).y() - 2 * MineShaft.DESCENT,
                deeper.positionAt(MineShaft.CARVED).y());
    }

    /** E o resto da forma continua: mesma descida, mesma galeria. */
    @Test
    void theNextLevelKeepsTheShapeOfThisOne() {
        MineShaft shaft = MineShaft.from(new ColonyPos(40, 64, 0), Side.EAST);

        MineShaft deeper = shaft.deepened();

        assertEquals(shaft.descent(), deeper.descent());
        assertEquals(shaft.gallery(), deeper.gallery());
    }

    /**
     * E ela para antes da rocha-mãe.
     *
     * <p>O fundo é o pico do diamante, e não o fundo do mundo: abaixo
     * dele não há o que procurar, e a rocha-mãe começa cinco blocos
     * depois.
     */
    @Test
    void aMineAtTheBottomMayNotDeepen() {
        MineShaft deep = MineShaft.from(
                new ColonyPos(40, MineShaft.DEEPEST + 2 * MineShaft.DESCENT, 0), Side.EAST);

        assertFalse(deep.mayDeepen());
    }

    /** Perto da superfície ela pode, e é o caso comum. */
    @Test
    void aMineNearTheSurfaceMayDeepen() {
        assertTrue(MineShaft.from(new ColonyPos(40, 64, 0), Side.EAST).mayDeepen());
    }

    /**
     * A galeria deixou de ser uma linha reta — decisão do autor,
     * 2026-09-03.
     *
     * <p>A frase dele: <i>"o caminho de mineração pode ser de modo mais
     * aleatório em bolsões e não uma linha reta"</i>. Até aqui a galeria
     * era um cano de um bloco de largura: toda posição dela caía sobre o
     * mesmo eixo, e a mina inteira cabia numa linha.
     */
    @Test
    void theGalleryOpensPocketsInsteadOfOneStraightLine() {
        MineShaft mine = shaft();

        ColonyPos first = mine.positionAt(MineShaft.CARVED);

        boolean offTheAxis = false;

        for (int i = MineShaft.CARVED; i < MineShaft.CARVED + 200; i++) {
            if (mine.positionAt(i).z() != first.z()) {
                offTheAxis = true;

                break;
            }
        }

        assertTrue(offTheAxis, "a galeria continua cabendo numa linha reta");
    }

    /**
     * E o corredor continua reto, que é do que a navegação depende.
     *
     * <p><b>É o E34 pela porta de trás.</b> O {@code MinerReach.legTowards}
     * anda pela ordem de cavar porque ela <i>é</i> um corredor contínuo a
     * partir da boca. Serpentear a espinha poria dois blocos em diagonal,
     * e de diagonal a navegação não passa sem que os cantos estejam
     * abertos.
     *
     * <p>Então o bolsão fica <b>pendurado ao lado</b>: a espinha do
     * corredor — as primeiras {@link MineShaft#RUN} colunas de cada ciclo
     * — anda um bloco por coluna, sempre no mesmo eixo.
     */
    @Test
    void theCorridorSpineStaysStraightUnderneathThePockets() {
        MineShaft mine = shaft();

        ColonyPos previous = null;

        for (int column = 0; column < MineShaft.RUN; column++) {
            ColonyPos feet = mine.positionAt(
                    MineShaft.CARVED + column * MineShaft.HEADROOM);

            if (previous != null) {
                int walked = Math.abs(feet.x() - previous.x())
                        + Math.abs(feet.y() - previous.y())
                        + Math.abs(feet.z() - previous.z());

                assertEquals(1, walked,
                        "a espinha do corredor pulou de " + previous + " para " + feet);
            }

            previous = feet;
        }
    }

    /**
     * Todo bloco do bolsão encosta em algum que veio antes dele.
     *
     * <p>É a contiguidade que o {@code findTheFrontier} assume por
     * escrito: <i>tudo o que vem antes da frente já está aberto, e a ordem
     * é um caminho contínuo a partir da boca</i>. Um bolsão que se abrisse
     * a dois blocos do corredor seria um bolsão dentro da rocha.
     */
    @Test
    void everyPocketBlockTouchesSomethingAlreadyOpen() {
        MineShaft mine = shaft();

        Set<ColonyPos> open = new HashSet<>();

        for (int i = 0; i < MineShaft.CARVED; i++) {
            open.add(mine.positionAt(i));
        }

        for (int i = MineShaft.CARVED; i < MineShaft.CARVED + 200; i++) {
            ColonyPos at = mine.positionAt(i);

            assertTrue(
                    touchesSomethingIn(at, open),
                    "a posição " + i + " em " + at + " não encosta em nada já aberto");

            open.add(at);
        }
    }

    private static boolean touchesSomethingIn(ColonyPos at, Set<ColonyPos> open) {
        for (int[] face : new int[][] {
                {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}}) {

            if (open.contains(
                    new ColonyPos(at.x() + face[0], at.y() + face[1], at.z() + face[2]))) {

                return true;
            }
        }

        return false;
    }

    /**
     * A mesma mina cava os mesmos bolsões depois de reiniciar o servidor.
     *
     * <p><b>O "aleatório" do pedido não pode ser sorteio</b>, e é o que
     * este teste tranca. O cursor da galeria é um <b>índice</b> gravado no
     * save: se {@code positionAt} respondesse outra coisa no carregamento
     * seguinte, o cursor passaria a apontar para um lugar que ninguém
     * cavou, e a mina de ontem viraria rocha maciça com um número em cima.
     */
    @Test
    void theSameMineDigsTheSamePocketsAfterAReload() {
        MineShaft before = shaft();
        MineShaft after = MineShaft.from(ENTRY, Side.EAST);

        for (int i = MineShaft.CARVED; i < MineShaft.CARVED + 200; i++) {
            assertEquals(before.positionAt(i), after.positionAt(i),
                    "a posição " + i + " mudou entre duas leituras da mesma mina");
        }
    }

    /** E minas diferentes não cavam o mesmo desenho. */
    @Test
    void twoMinesDoNotDigTheSamePocketPattern() {
        MineShaft here = shaft();
        MineShaft elsewhere = MineShaft.from(new ColonyPos(517, 64, 88), Side.EAST);

        boolean differed = false;

        for (int cycle = 0; cycle < 12 && !differed; cycle++) {
            // O primeiro bloco do bolsão de cada ciclo: é nele que o lado
            // sorteado aparece.
            int pocket = MineShaft.CARVED
                    + cycle * (MineShaft.RUN * MineShaft.HEADROOM
                            + MineShaft.POCKET_LONG * MineShaft.POCKET_WIDE
                                    * MineShaft.HEADROOM)
                    + MineShaft.RUN * MineShaft.HEADROOM;

            ColonyPos mine = here.positionAt(pocket);
            ColonyPos other = elsewhere.positionAt(pocket);

            // Só o lado interessa: as duas minas nascem em bocas
            // diferentes, então as coordenadas absolutas diferem sempre.
            int sideHere = mine.z() - here.positionAt(pocket - 1).z();
            int sideThere = other.z() - elsewhere.positionAt(pocket - 1).z();

            differed = sideHere != sideThere;
        }

        assertTrue(differed, "as duas minas abrem todos os bolsões para o mesmo lado");
    }

    /**
     * A galeria tem fim, e ele é o perímetro — decisão do autor,
     * 2026-09-04.
     *
     * <p>Até aqui o {@code cycle} do túnel crescia sem teto, e a sessão
     * de 09-04 mostrou o preço: o mineiro em {@code 1456,44,87} mirando
     * a ordem em {@code 1454,44,158} — 70,7 blocos, {@code out of reach},
     * {@code 0/0 ticks} por vinte minutos.
     */
    @Test
    void theGalleryStopsAtTheEndOfTheArm() {
        MineShaft shaft = MineShaft.from(ENTRY, Side.NORTH);

        assertFalse(shaft.beyondTheArm(MineShaft.CARVED),
                "o primeiro bloco da galeria já estaria além do braço");

        assertFalse(shaft.beyondTheArm(MineShaft.CARVED - 1),
                "o poço e as salas não são galeria, e não têm braço");

        // Vinte e quatro colunas são três trechos de RUN=8 com os bolsões
        // deles: dentro do último ciclo ainda cava, no seguinte não.
        int lastCycle = MineShaft.ARM / MineShaft.RUN - 1;
        int inside = MineShaft.CARVED + lastCycle * galleryCycle();
        int outside = MineShaft.CARVED + (lastCycle + 1) * galleryCycle();

        assertFalse(shaft.beyondTheArm(inside),
                "o último ciclo do braço foi cortado antes da hora");

        assertTrue(shaft.beyondTheArm(outside),
                "a galeria passou do braço e continuou andando");
    }

    /**
     * E o mais distante do braço cabe na perna do mineiro.
     *
     * <p>É o que dá sentido ao número: um teto que ainda pusesse a frente
     * fora de alcance não seria teto nenhum. Medido da <b>boca</b>, que é
     * de onde o aldeão desce.
     */
    @Test
    void theFarthestCutOfAnArmStaysWithinReach() {
        MineShaft shaft = MineShaft.from(ENTRY, Side.NORTH);

        int last = MineShaft.CARVED
                + (MineShaft.ARM / MineShaft.RUN) * galleryCycle() - 1;

        ColonyPos far = shaft.positionAt(last);

        int flat = Math.abs(far.x() - ENTRY.x()) + Math.abs(far.z() - ENTRY.z());

        assertTrue(flat <= 48,
                "a ponta do braço ficou a " + flat + " blocos da boca, no plano");
    }

    /** O ciclo da galeria, deduzido da forma em vez de repetido aqui. */
    private static int galleryCycle() {
        MineShaft shaft = MineShaft.from(ENTRY, Side.NORTH);

        ColonyPos first = shaft.positionAt(MineShaft.CARVED);

        for (int i = MineShaft.CARVED + 1; i < MineShaft.CARVED + 400; i++) {
            ColonyPos here = shaft.positionAt(i);

            if (here.y() == first.y() && sameLane(first, here)
                    && stepOf(shaft, i) == stepOf(shaft, MineShaft.CARVED) + MineShaft.RUN) {

                return i - MineShaft.CARVED;
            }
        }

        throw new IllegalStateException("não achei o ciclo da galeria");
    }

    private static boolean sameLane(ColonyPos a, ColonyPos b) {
        return a.x() == b.x() || a.z() == b.z();
    }

    private static int stepOf(MineShaft shaft, int i) {
        ColonyPos at = shaft.positionAt(i);
        ColonyPos origin = shaft.positionAt(MineShaft.CARVED);

        return Math.abs(at.x() - origin.x()) + Math.abs(at.z() - origin.z());
    }
}
