package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Por onde o mineiro entra na mina, e como ele desce — 2026-08-28 e 29.
 *
 * <p><b>A sessão da meia-noite mostrou onde ele estava</b>, e foi a
 * primeira vez que se soube:
 *
 * <pre>
 * the miner is at 734, 66, 878, 20,5 blocks away;
 * the stone at 735, 45, 878 is Pedra
 * </pre>
 *
 * <p><b>Y 66 é a superfície.</b> Ele estava vinte e um blocos em linha
 * reta <b>acima</b> da galeria, mirando uma pedra no fundo da mina. A
 * navegação do jogo recebe um destino a vinte blocos atravessando rocha
 * maciça, devolve caminho parcial, e ele estaciona no ponto mais próximo
 * que consegue — bem ali em cima. É o sintoma que o MineColonies
 * registrou na
 * <a href="https://github.com/ldtteam/minecolonies/issues/4297">issue
 * 4297</a>, e o remendo do jogador é o mesmo que o autor fez: cavar até
 * lá.
 *
 * <p><b>A primeira perna resolveu a entrada e criou o E35.</b> Ela tinha
 * duas pontas e nada no meio: longe, o destino era a boca; perto da
 * boca, o destino virava a pedra do fundo. A sessão de 2026-08-28 pegou
 * o segundo mineiro <b>oscilando na fronteira</b>:
 *
 * <pre>
 * 740, 65, 895  ->  8,77 da boca   FORA da perna  -> mandado à boca
 * 739, 65, 896  ->  7,55 da boca   DENTRO         -> mandado à pedra
 * 741, 63, 898  ->  9,00 da boca   FORA           -> mandado à boca
 * </pre>
 *
 * <p>Ele andava para a boca, cruzava os oito blocos, recebia um destino
 * que a navegação não cumpre, derivava, saía dos oito, e recomeçava.
 * Para sempre. <b>A descida tem vinte blocos e a perna tem oito: são
 * três passos, e o sistema só sabia dar dois.</b>
 *
 * <p>Agora quem dá o passo é a <b>ordem de cavar</b>: ela é um corredor
 * contínuo a partir da boca, e o passo seguinte é o ponto mais avançado
 * dela que ainda caiba numa perna.
 */
class MinerLegTest {

    private static final ColonyPos MOUTH = new ColonyPos(732, 63, 898);

    private static final BlockPos MOUTH_BLOCK = new BlockPos(732, 63, 898);

    private static final BlockPos DEEP = new BlockPos(735, 45, 878);

    /** A mina desta colônia, com a escada já aberta até certa posição. */
    private static Optional<Mine> mine(int cut) {
        return Optional.of(
                Mine.restore(UUID.randomUUID(), MineShaft.from(MOUTH, Side.NORTH), cut));
    }

    /** Um mundo de mentira, feito das duas respostas que a perna pede. */
    private static MinerReach.Footing world(
            Predicate<BlockPos> passable, Predicate<BlockPos> standable) {

        return new MinerReach.Footing() {

            @Override
            public boolean passable(BlockPos at) {
                return passable.test(at);
            }

            @Override
            public boolean standable(BlockPos at) {
                return standable.test(at);
            }
        };
    }

    /**
     * Onde só a geometria está em jogo, o mundo não entra: tudo é aberto e
     * tudo é pisável.
     *
     * <p>Preserva o que estas afirmações sempre mediram — para onde o passo
     * aponta —, sem misturar as perguntas novas, que são <i>se dá para
     * passar</i> e <i>se dá para ficar de pé lá</i>.
     */
    private static final MinerReach.Footing ANYWHERE = world(at -> true, at -> true);

    /**
     * O mundo de uma escada de verdade: só o piso de cada degrau é pisável.
     *
     * <p>A ordem de cavar abre <b>três</b> camadas por degrau — os pés, o
     * peito e a cabeça. Só a de baixo tem bloco sólido embaixo; nas outras
     * duas o que há embaixo é o próprio degrau, já cavado. É o que o
     * {@code BuilderApproach.standable} responde no mundo de verdade, e é
     * por isso que duas de cada três posições da ordem <b>não</b> servem
     * como destino de caminhada.
     *
     * <p>O piso do degrau {@code s} fica em {@code y = 64 - s},
     * {@code z = 898 - s}: a diferença entre os dois é constante, e é ela
     * que identifica o piso sem precisar do índice.
     */
    private static Predicate<BlockPos> dugStaircase() {
        return at -> at.getZ() - at.getY() == MOUTH.z() - MOUTH.y() - 1;
    }

    /** Longe da pedra e longe da boca: entra pela boca. */
    @Test
    void fromTheSurfaceHeAimsForTheMouth() {
        assertEquals(
                MOUTH_BLOCK,
                MinerReach.legTowards(new BlockPos(734, 66, 878), DEEP, mine(30), ANYWHERE));
    }

    /**
     * <b>O E35.</b> Já na boca, o passo é escada abaixo — e não a pedra.
     *
     * <p>Este teste afirmava o contrário até 2026-08-29, e o que ele
     * travava era o defeito: <i>"já na boca, ele mira a pedra"</i>. Mirar
     * a pedra dali é mirar vinte blocos abaixo, do outro lado da rocha,
     * e a navegação não cumpre — ele derivava, saía do alcance da perna,
     * e o destino voltava a ser a boca.
     */
    @Test
    void atTheMouthTheLegIsAStepDownTheShaft() {
        BlockPos leg = MinerReach.legTowards(new BlockPos(731, 63, 898), DEEP, mine(30), ANYWHERE);

        assertNotEquals(
                DEEP, leg,
                "à boca ele continua sendo mandado à pedra do fundo, que é o E35");

        assertTrue(
                leg.getY() < MOUTH_BLOCK.getY(),
                "o passo não desce: " + leg.toShortString());

        assertTrue(
                Math.sqrt(new BlockPos(731, 63, 898).getSquaredDistance(leg)) <= MinerReach.LEG,
                "o passo saiu fora do alcance de uma perna: " + leg.toShortString());
    }

    /**
     * E de dentro da escada ele continua descendo, em vez de voltar.
     *
     * <p>É a metade que impede a troca de um travamento por outro: um
     * passo que devolvesse a boca desfaria a descida a cada tique.
     */
    @Test
    void fromInsideTheShaftTheLegKeepsGoingDown() {
        BlockPos onTheStairs = new BlockPos(732, 58, 893);

        BlockPos leg = MinerReach.legTowards(onTheStairs, DEEP, mine(60), ANYWHERE);

        assertTrue(
                leg.getY() < onTheStairs.getY(),
                "de dentro da escada ele foi mandado para cima: " + leg.toShortString());
    }

    /**
     * Perto da pedra, nem a boca nem a ordem interessam.
     *
     * <p>Dentro da galeria ele está a metros do alvo: o passo é o alvo.
     */
    @Test
    void insideTheGalleryHeKeepsAimingForTheStone() {
        assertEquals(
                DEEP,
                MinerReach.legTowards(new BlockPos(730, 45, 878), DEEP, mine(200), ANYWHERE));
    }

    /**
     * Sem mina, não há perna: a pedra é o destino.
     *
     * <p>É a pedra de superfície, que o mineiro raspa quando a boca não
     * pôde nascer. Ali não há descida nenhuma a fazer.
     */
    @Test
    void withoutAMineTheStoneIsTheOnlyLeg() {
        assertEquals(
                DEEP,
                MinerReach.legTowards(new BlockPos(734, 66, 878), DEEP, Optional.empty(), ANYWHERE));
    }

    /**
     * Mina recém-aberta, sem nada cavado, ainda manda para a boca.
     *
     * <p>Não há ordem por onde andar, e a boca é a única resposta que
     * não inventa caminho.
     */
    @Test
    void anUntouchedMineStillSendsHimToTheMouth() {
        assertEquals(
                MOUTH_BLOCK,
                MinerReach.legTowards(new BlockPos(734, 66, 878), DEEP, mine(0), ANYWHERE));
    }

    /**
     * <b>O E32.</b> A perna não manda o mineiro para onde ele não fica de pé.
     *
     * <p>A ordem de cavar é uma lista de <b>blocos a cavar</b>, não de
     * lugares onde se fica de pé — e a perna devolvia um deles cru. Da boca,
     * o ponto mais avançado dentro dos oito blocos é a <b>cabeça</b> do
     * degrau 6, {@code 732, 60, 892}, cujo bloco de baixo é o próprio degrau
     * já cavado.
     *
     * <p><b>E alvo ruim não faz a navegação desistir.</b> O
     * {@code MobNavigation.findPathTo} do 1.21.1 abaixa alvo no ar até o
     * chão — esse caso ele perdoa — mas <b>sobe</b> alvo sólido até sair da
     * rocha. Dentro de uma mina isso é a superfície, que é exatamente onde
     * as sessões acharam o mineiro. Ver
     * {@code docs/research/E32-miner-walk-target.md}.
     */
    @Test
    void theLegNeverAimsAtABlockHeCannotStandOn() {
        Predicate<BlockPos> dug = dugStaircase();

        // Escada inteira aberta: tudo se atravessa, e só o piso se pisa.
        BlockPos leg = MinerReach.legTowards(
                new BlockPos(731, 63, 898), DEEP, mine(30), world(at -> true, dug));

        assertTrue(
                dug.test(leg),
                "a perna mandou o mineiro para um bloco onde ele não fica de pé: "
                        + leg.toShortString());
    }

    /**
     * E o que o cursor entregou mas ninguém cavou continua sendo rocha.
     *
     * <p>{@code Mine.cut} conta posição <b>entregue</b> — {@code nextPosition}
     * é {@code positionAt(cut++)} —, não bloco cavado. Uma tarefa devolvida
     * deixa posição entregue e fechada para trás. Mandar o aldeão para dentro
     * dela é mandá-lo para dentro da pedra, e a navegação responde subindo o
     * alvo até a superfície.
     *
     * <p>Aqui o cursor entregou a escada inteira e a picareta parou no degrau
     * 6: tudo abaixo de {@code y = 58} ainda é maciço.
     */
    @Test
    void theLegSkipsWhatTheCursorHandedOutButNobodyDug() {
        Predicate<BlockPos> floors = dugStaircase();
        Predicate<BlockPos> openDownToStepSix = at -> at.getY() >= 58;
        Predicate<BlockPos> dugDownToStepSix = at -> floors.test(at) && openDownToStepSix.test(at);

        BlockPos leg = MinerReach.legTowards(
                new BlockPos(732, 59, 893), DEEP, mine(30),
                world(openDownToStepSix, dugDownToStepSix));

        assertTrue(
                dugDownToStepSix.test(leg),
                "a perna mandou o mineiro para rocha que ninguém cavou: "
                        + leg.toShortString());
    }

    /**
     * <b>O E34.</b> A perna não salta a parede para cair num bolsão aberto.
     *
     * <p>A ordem de cavar é contígua <b>como ordem</b> — mas o mundo não é
     * obrigado a segui-la. Um vão aberto pode coincidir com um índice mais
     * avançado sem que exista caminho até ele, e são dois os mundos que
     * produzem isso, nenhum raro:
     *
     * <ul>
     *   <li>o <b>túnel que o jogador cavou à mão</b> — o E34 como apareceu
     *       em 2026-08-28, com os dois mineiros parados no degrau 7 mirando
     *       uma lanterna dentro de um bolsão que não se liga à escada;</li>
     *   <li>a <b>caverna natural</b> que a ordem atravessa.</li>
     * </ul>
     *
     * <p>Aqui os degraus 1 a 3 estão abertos, o 4 e o 5 são rocha maciça, e
     * o 6 é o bolsão: aberto, pisável, e a 7,87 blocos — dentro da perna.
     * Pisável e alcançável em linha reta, e ainda assim <b>do outro lado de
     * uma parede</b>.
     *
     * <p>Enquanto o laço parava só no alcance, o passo era o bolsão. O
     * conserto é parar na primeira posição que não se atravessa, e é por
     * isso que <i>atravessar</i> e <i>ficar de pé</i> são perguntas
     * separadas: parar no que não é pisável travaria a descida no primeiro
     * degrau, porque duas de cada três posições da escada são o peito e a
     * cabeça.
     */
    @Test
    void theLegStopsAtTheWallInsteadOfJumpingToAPocketBehindIt() {
        Set<Integer> open = Set.of(897, 896, 895, 892);

        Predicate<BlockPos> floors = dugStaircase();
        Predicate<BlockPos> passable = at -> open.contains(at.getZ());

        BlockPos leg = MinerReach.legTowards(
                new BlockPos(731, 63, 898), DEEP, mine(30),
                world(passable, at -> passable.test(at) && floors.test(at)));

        assertEquals(
                new BlockPos(732, 61, 895), leg,
                "a perna atravessou a rocha dos degraus 4 e 5 para chegar ao bolsão");
    }

    /**
     * A perna nunca manda ele andar para onde já está — 2026-09-02.
     *
     * <p><b>A sessão das 22:59, e ela custou a mina inteira:</b>
     *
     * <pre>
     * digging Diorito at 709, 44, 878, 9,0 blocks away
     * (out of reach, he is at 718, 44, 878, walking to 718, 44, 878)
     * </pre>
     *
     * <p>Destino igual à posição dele. O aldeão chega no destino sem dar
     * um passo, a navegação não tem o que fazer, e o contador de
     * travamento sobe até 2.400 — dois minutos de expediente por vez,
     * para não andar nada. Dez minutos de sessão, zero pedra.
     *
     * <p>Passo que não é passo é o mesmo que não achar passo nenhum, e
     * para esse caso já havia saída: voltar à boca, de onde a ordem de
     * cavar volta a funcionar.
     */
    @Test
    void theLegNeverSendsHimToWhereHeAlreadyIs() {
        Optional<Mine> mine = mine(60);

        MineShaft shaft = mine.orElseThrow().shaft();

        for (int i = 0; i < 60; i++) {
            ColonyPos position = shaft.positionAt(i);

            BlockPos where = new BlockPos(position.x(), position.y(), position.z());

            assertNotEquals(
                    where,
                    MinerReach.legTowards(where, DEEP, mine, ANYWHERE),
                    "a perna mandou o mineiro para onde ele já está, no passo " + i);
        }
    }

    /**
     * <b>O "unable to climb" — 2026-09-04.</b> A perna leva de volta
     * quando o destino ficou para trás.
     *
     * <p>Até aqui {@code stepAlongTheShaft} só sabia andar <b>para a
     * frente</b> na ordem de cavar, rumo à frente de escavação — e o
     * {@code destination} não chegava a entrar nele. Acertava por
     * acidente no caso comum, que é entrar para cavar fundo; errava
     * sempre que o alvo estava atrás, e o mineiro descia cada vez mais
     * para longe dele.
     *
     * <p>Aqui ele está no degrau 12 e a pedra é lá em cima, perto da
     * boca. O passo tem de subir.
     */
    @Test
    void whenTheStoneIsBehindHimTheLegWalksBackUp() {
        // Degrau 8 do primeiro lance, que é onde o piso fica em
        // y = 64 - degrau, z = 898 - degrau.
        BlockPos villager = new BlockPos(732, 56, 890);
        BlockPos behind = new BlockPos(732, 63, 897);

        BlockPos leg = MinerReach.legTowards(
                villager, behind, mine(30), world(at -> true, dugStaircase()));

        assertTrue(
                leg.getY() > villager.getY(),
                "a perna mandou o mineiro descer para alcançar uma pedra acima dele: "
                        + leg.toShortString());
    }

    /**
     * Destino na superfície tira o mineiro da mina.
     *
     * <p>A areia mora na praia, e a tarefa dela é do mesmo mineiro. Na
     * sessão de 2026-09-04 ele estava a dezenove blocos de profundidade
     * mirando areia em {@code y 62}, e a perna o mandava galeria adentro:
     *
     * <pre>
     * digging Areia at 1472, 62, 48 ... he is at 1450, 44, 67,
     * walking to 1454, 44, 70
     * </pre>
     *
     * <p>Ele varreu a galeria inteira até a frente de escavação e nunca
     * saiu. Alvo fora da mina não tem índice na ordem, e a saída é a
     * boca: o passo aponta para trás.
     */
    @Test
    void aTargetOnTheSurfaceSendsHimBackOut() {
        BlockPos villager = new BlockPos(732, 56, 890);
        BlockPos surface = new BlockPos(760, 64, 930);

        BlockPos leg = MinerReach.legTowards(
                villager, surface, mine(30), world(at -> true, dugStaircase()));

        assertTrue(
                leg.getY() > villager.getY(),
                "a perna afundou o mineiro atrás de um alvo de superfície: "
                        + leg.toShortString());
    }

    /**
     * Fora da mina, dos dois lados, a boca não tem o que fazer.
     *
     * <p>A boca é o desvio de quem <b>vai entrar</b>. Quem já está na
     * superfície indo para outro ponto da superfície não passa por ela —
     * mandá-lo à boca é um desvio inventado, e era o que acontecia com o
     * mineiro de areia depois de sair: ele saía e era devolvido à boca.
     *
     * <p>A navegação do jogo dá conta de um caminho a céu aberto. É
     * justamente o caminho que ela <b>sabe</b> traçar, e a perna existe
     * para os outros.
     */
    @Test
    void onTheSurfaceHeWalksStraightToASurfaceTarget() {
        BlockPos villager = new BlockPos(760, 64, 928);
        BlockPos surface = new BlockPos(762, 64, 942);

        assertEquals(
                surface,
                MinerReach.legTowards(
                        villager, surface, mine(30), world(at -> true, dugStaircase())),
                "o mineiro foi desviado para a boca sem ter de entrar na mina");
    }
}
