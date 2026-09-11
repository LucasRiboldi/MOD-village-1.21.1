package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A segunda sala da mina — P1.11, 2026-09-11.
 *
 * <p><b>O item pedia isto em jogo, e em jogo não cabe.</b> A arena de
 * gametest vive perto de {@code y=-58}, o mundo acaba em {@code -64} e
 * {@link MineShaft#DEEPEST} é {@code -59}; um nível custa
 * {@link MineShaft#DESCENT} = {@code HELIX_SIDE × HELIX_FLIGHTS} = 20
 * blocos, então a segunda sala cairia fora do mundo antes de cair fora da
 * arena. A altura da arena é do runner, e o mod não a controla.
 *
 * <p><b>O caminho que restava era encurtar a hélice por gancho de teste</b>,
 * no molde do {@code searchRadius} do fazendeiro — e ele foi medido antes
 * de ser tomado. {@code DESCENT}, {@code FLIGHT_BLOCKS} e
 * {@link MineShaft#CARVED} saem todos de {@code HELIX_SIDE} no
 * carregamento da classe; torná-lo variável obriga os três a virarem
 * método, e só {@code CARVED} tem <b>52 usos</b> no projeto. É cirurgia de
 * geometria no Core — que governa a forma da mina inteira e o que o
 * {@code MineSave} grava — pelo alcance de um caso.
 *
 * <p><b>Então a segunda sala é provada onde ela é pura.</b> O que a
 * versão em jogo acrescentaria sobre a versão aqui é o mineiro batendo a
 * picareta, e esse caminho não muda de nível para nível: o
 * {@code MineDigging} pede posições ao {@code positionAt} e cava, com a
 * mesma rotina que o primeiro nível já exercita em jogo. O que muda entre
 * um nível e outro é <b>geometria</b>, e geometria não precisa de mundo.
 *
 * <p><b>A invariante que faltava</b> é a última daqui, e é a que dá nome
 * ao item: nada no projeto afirmava que a segunda sala <b>não invade a
 * primeira</b>. O {@code MineShaftTest} conferia o deslocamento em y de
 * uma posição — {@code positionAt(CARVED)} — e que os lados se preservam;
 * a forma inteira do nível de baixo não era olhada por ninguém.
 *
 * <p>O que continua sem cobertura, e fica dito: <b>o mineiro cavando o
 * segundo nível em mundo de verdade</b>. Só uma arena mais funda o
 * alcançaria, e ela não é do mod.
 */
class MineSecondLevelTest {

    private static final ColonyPos ENTRY = new ColonyPos(40, 64, 0);

    private static MineShaft first() {
        return MineShaft.from(ENTRY, Side.EAST);
    }

    private static MineShaft second() {
        return first().deepened();
    }

    /** As posições que um nível abre, da boca ao fim do caracol. */
    private static Set<ColonyPos> carvedBy(MineShaft shaft) {
        Set<ColonyPos> carved = new HashSet<>();

        for (int i = 0; i < MineShaft.CARVED; i++) {
            carved.add(shaft.positionAt(i));
        }

        return carved;
    }

    /**
     * <b>A segunda sala fica embaixo da primeira, e não ao lado.</b>
     *
     * <p>É a lacuna que esta classe veio fechar, e ela estava aberta de
     * verdade: das afirmações que o projeto fazia sobre o
     * {@code deepened}, uma olhava o <b>y</b> de uma posição e a outra os
     * lados de descida e galeria. <b>Ninguém olhava o x e o z.</b> Uma
     * mina que descesse torta passava por todos os casos existentes.
     *
     * <p>E o que ela custaria está escrito no javadoc do {@code levelFloor}:
     * a escada reta deixava o fundo vinte blocos <b>de lado</b>, e essa
     * distância entrava na caminhada do mineiro toda vez que ele subia
     * para depositar. O caracol existe para acabar com isso — e um desvio
     * ao descer traria de volta, um nível por vez, exatamente o que ele
     * foi feito para resolver.
     */
    @Test
    void theSecondRoomSitsDirectlyUnderTheFirst() {
        ColonyPos below = second().entry();

        assertEquals(ENTRY.x(), below.x(),
                "a segunda sala desceu torta em x — a caminhada do mineiro volta a crescer");
        assertEquals(ENTRY.z(), below.z(),
                "a segunda sala desceu torta em z — a caminhada do mineiro volta a crescer");
        assertEquals(ENTRY.y() - MineShaft.DESCENT, below.y(),
                "a segunda sala não desceu um nível inteiro");
    }

    /**
     * <b>A segunda sala não invade a primeira.</b>
     *
     * <p>É a invariante que dá nome ao P1.11, e ninguém a afirmava. Duas
     * salas que se sobrepusessem fariam o mineiro gastar a picareta em
     * bloco já aberto — e pior: a galeria de cima abriria no teto da de
     * baixo, que é buraco por onde aldeão cai.
     *
     * <p>A conta sozinha não basta como prova. Vinte blocos de descida
     * separam os dois <b>chãos</b>; o que este caso mede é que nenhuma
     * das posições abertas se encontra, incluindo as do caracol, que
     * atravessa a altura inteira entre um nível e o outro.
     */
    @Test
    void theSecondRoomDoesNotDigIntoTheFirst() {
        Set<ColonyPos> above = carvedBy(first());
        Set<ColonyPos> below = carvedBy(second());

        Set<ColonyPos> shared = new HashSet<>(above);
        shared.retainAll(below);

        assertTrue(shared.isEmpty(),
                "os dois níveis abrem as mesmas posições: " + shared);
    }

    /**
     * E ela também não se repete por dentro.
     *
     * <p>O mesmo que o {@code MineShaftTest} exige do primeiro nível.
     * Vale repetir aqui porque a hélice do nível de baixo nasce de uma
     * entrada calculada, e não da que o autor escreveu: um erro de um
     * bloco no {@code deepened} não apareceria em nenhum caso do nível de
     * cima.
     */
    @Test
    void theSecondRoomNeverDigsTheSameBlockTwice() {
        MineShaft deeper = second();

        Set<ColonyPos> seen = new HashSet<>();

        for (int i = 0; i < MineShaft.CARVED; i++) {
            assertTrue(
                    seen.add(deeper.positionAt(i)),
                    "a segunda sala repetiu a posição de índice " + i);
        }
    }

    /**
     * O caracol do segundo nível fecha a volta debaixo da boca dele.
     *
     * <p>É o ponto do caracol, e o javadoc do {@code levelFloor} o diz:
     * quatro curvas fecham a volta e o x e o z voltam a ser os da
     * entrada, de modo que a galeria nasce embaixo de quem mandou cavar.
     * Se isso se perdesse ao descer, o mineiro do segundo nível passaria
     * a caminhar o nível inteiro toda vez que subisse para depositar.
     */
    @Test
    void theSecondHelixAlsoClosesItsTurnUnderTheMouth() {
        MineShaft deeper = second();

        ColonyPos entry = deeper.entry();

        // O último degrau do último lance, na primeira pista — e não o
        // índice CARVED, que já é a galeria e por isso sai um passo para
        // o lado. É a mesma conta do caso do primeiro nível.
        int perStep = MineShaft.STAIR_HEADROOM * MineShaft.STAIR_LANES;
        int perFlight = MineShaft.HELIX_SIDE * perStep;

        ColonyPos last = deeper.positionAt(
                (MineShaft.HELIX_FLIGHTS - 1) * perFlight
                        + (MineShaft.HELIX_SIDE - 1) * perStep);

        assertEquals(entry.x(), last.x(), "o caracol do segundo nível não fechou em x");
        assertEquals(entry.z(), last.z(), "o caracol do segundo nível não fechou em z");
    }

    /**
     * E cabe no mesmo quadrado que o de cima.
     *
     * <p>A pegada não pode crescer com a profundidade: a mina ocupa uma
     * coluna da vila, e um nível mais largo sairia por baixo de casa que
     * ninguém mandou minar.
     */
    @Test
    void theSecondHelixFitsInTheSameSquare() {
        MineShaft deeper = second();

        ColonyPos entry = deeper.entry();

        int wide = MineShaft.HELIX_SIDE + MineShaft.STAIR_LANES;

        for (int i = 0; i < MineShaft.CARVED; i++) {
            ColonyPos at = deeper.positionAt(i);

            assertTrue(
                    Math.abs(at.x() - entry.x()) <= wide
                            && Math.abs(at.z() - entry.z()) <= wide,
                    "o caracol do segundo nível saiu do quadrado em " + at);
        }
    }

    /**
     * Nenhuma posição do segundo nível passa do fundo permitido.
     *
     * <p>{@link MineShaft#DEEPEST} é o pico do diamante, e a rocha-mãe
     * começa cinco blocos abaixo. A guarda do {@code mayDeepen} olha o
     * <b>chão</b> do nível seguinte; este caso olha <b>todas</b> as
     * posições que ele abre — é onde um bloco a mais de escada passaria
     * despercebido.
     */
    @Test
    void nothingTheSecondRoomOpensGoesBelowTheAllowedFloor() {
        // A mina mais funda que ainda pode descer: o mayDeepen olha o chão
        // do nível SEGUINTE, então a entrada precisa estar dois DESCENT
        // acima do fundo. Um bloco mais abaixo e ela já não desceria — é
        // a borda exata, que é onde a conta aperta.
        MineShaft shallow = MineShaft.from(
                new ColonyPos(40, MineShaft.DEEPEST + 2 * MineShaft.DESCENT, 0), Side.EAST);

        assertTrue(shallow.mayDeepen(), "a montagem precisa de uma mina que possa descer");

        MineShaft deeper = shallow.deepened();

        // O caracol e um giro inteiro de galeria: é a galeria que pousa
        // no chão do nível, e um degrau a mais nela passaria do fundo sem
        // que o guarda do mayDeepen visse.
        for (int i = 0; i < MineShaft.CARVED + MineShaft.GALLERY_CYCLE; i++) {
            assertTrue(
                    deeper.positionAt(i).y() >= MineShaft.DEEPEST,
                    "a segunda sala abriu abaixo do fundo permitido, em "
                            + deeper.positionAt(i));
        }
    }
}
