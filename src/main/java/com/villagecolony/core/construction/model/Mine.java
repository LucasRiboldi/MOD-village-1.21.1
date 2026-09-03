package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.IntPredicate;

/**
 * A mina de uma colônia, e até onde ela já foi cavada.
 *
 * <p>{@link MineShaft} é a forma — a geometria da escada, das salas e da
 * galeria. Isto aqui é a <b>mina daquela colônia</b>: a forma mais o
 * cursor, que é a única coisa que muda enquanto o mineiro trabalha.
 *
 * <p><b>Por que ela precisava existir.</b> Até 2026-08-20 a mina era
 * campo de um {@code Job} do {@code MinerWork} — memória, e só. Fechar o
 * mundo apagava as duas coisas que custam a refazer:
 *
 * <ul>
 *   <li><b>a boca.</b> A sessão seguinte reprocurava o primeiro bloco
 *       sólido na coluna do fim da vila, e esse bloco tinha sido cavado:
 *       a busca descia mais, achava outro, e a mina de ontem ganhava uma
 *       entrada nova alguns blocos abaixo — duas escadas para a mesma
 *       colônia;
 *   <li><b>o cursor.</b> Voltava a zero, e o mineiro revarria índice por
 *       índice tudo o que já estava aberto para chegar onde parou. Custa
 *       pouco no primeiro dia e cresce com a profundidade, porque a
 *       galeria não acaba.
 * </ul>
 *
 * <p>Guarda também a <b>direção da galeria</b>, que muda quando ela
 * esbarra em lava, bedrock ou caverna. Sem isso a mina reabria virada
 * para a barreira que já a tinha feito virar.
 *
 * <p>Mutável de propósito, como {@code Colony}: o cursor anda a cada
 * posição olhada, e trocar o registro inteiro sessenta e quatro vezes por
 * busca seria cerimônia sem ganho.
 */
public final class Mine {

    private final UUID colonyId;

    /** A forma. Troca quando a galeria vira. */
    private MineShaft shaft;

    /**
     * A próxima posição a olhar, na ordem de cavar.
     *
     * <p>Anda mesmo quando a posição não se cava — ar, água, bedrock, a
     * casa da vila. É uma fronteira, e não uma conta de blocos: para trás
     * dela está tudo o que já foi decidido.
     */
    private int cut;

    /**
     * Quantas posições seguidas vieram impossíveis de cavar.
     *
     * <p><b>Não vai para o disco</b>, e é de propósito: é a contagem de
     * uma passagem, não um fato sobre a mina. Reabrir o mundo com ela
     * zerada custa no máximo oito posições a mais antes de a galeria
     * virar, e guardá-la faria a mina reabrir prestes a virar por causa
     * de uma barreira que ninguém mais está olhando.
     *
     * <p>Mora aqui, e não no trabalho do mineiro, porque a galeria que
     * ela faz virar é da colônia: dois mineiros na mesma escada esbarram
     * na mesma lava, e duas contagens separadas pediriam dezesseis
     * recusas para uma curva que precisa de oito.
     */
    private int blocked;

    /**
     * O último minério cavado, enquanto a veia não acabar.
     *
     * <p><b>Também não vai para o disco</b>, e pela mesma razão do
     * {@link #blocked}: uma veia interrompida por fechar o mundo custa
     * uma viagem a mais, e não um erro. O que se perde é o mineiro voltar
     * ao túnel em vez de terminar o carvão — e o túnel passa por ele de
     * novo.
     */
    private ColonyPos vein;

    /**
     * Quantas curvas a galeria já deu neste nível.
     *
     * <p><b>Não vai para o disco</b>, como o {@link #blocked} e pela
     * mesma razão: é a contagem de uma passagem. Reabrir o mundo custa,
     * no pior caso, um nível trabalhado mais tempo do que precisava — e
     * gravá-la faria a mina reabrir prestes a descer por causa de curvas
     * que ninguém está mais olhando.
     */
    private int turns;

    private Mine(UUID colonyId, MineShaft shaft, int cut) {
        this.colonyId = Objects.requireNonNull(colonyId, "colonyId");
        this.shaft = Objects.requireNonNull(shaft, "shaft");
        this.cut = cut;
    }

    /** Uma mina que começa agora, com a picareta ainda no primeiro degrau. */
    public static Mine open(UUID colonyId, MineShaft shaft) {
        return new Mine(colonyId, shaft, 0);
    }

    /**
     * A mina que o save trouxe.
     *
     * @param cut a fronteira gravada. Nunca negativa — quem lê o disco
     *     corrige antes de chegar aqui
     */
    public static Mine restore(UUID colonyId, MineShaft shaft, int cut) {
        if (cut < 0) {
            throw new IllegalArgumentException("cut negativo: " + cut);
        }

        return new Mine(colonyId, shaft, cut);
    }

    public UUID colonyId() {
        return colonyId;
    }

    public MineShaft shaft() {
        return shaft;
    }

    public int cut() {
        return cut;
    }

    /** Onde a mina começa: a boca, no fim da vila. */
    public ColonyPos entry() {
        return shaft.entry();
    }

    /**
     * A próxima posição a olhar, e o cursor avança.
     *
     * <p>Avança sempre, tenha ou não dado pedra: quem pula a posição
     * aberta de graça é justamente este avanço.
     */
    public ColonyPos nextPosition() {
        return shaft.positionAt(cut++);
    }

    /**
     * A galeria vira, e o que já foi cavado continua onde está.
     *
     * <p>Lava, bedrock, uma caverna. É a frase do autor sobre recolher
     * para outro lado, e agora ela sobrevive ao fechar do mundo.
     *
     * <p><b>E fechado o círculo, ela desce</b> — 2026-09-02. Quatro
     * curvas e a galeria voltou à direção em que começou: ela deu a volta
     * neste nível, e o que sobra está abaixo. A forma é a do MineColonies,
     * onde a profundidade cresce aos poucos com o nível do prédio; aqui o
     * gatilho é a volta completa, que o mod já tinha em mãos.
     *
     * <p>Por que isso importa: a sessão de 2026-09-02 trabalhou em
     * {@code y=44}, e o pico do diamante em 1.21 é {@code y=-59}. Uma
     * mina que não desce não tem como achar minério melhor, por mais que
     * se conserte a busca. Ver {@link MineShaft#deepened}.
     *
     * <p>O cursor volta a zero porque o poço do nível novo ainda não foi
     * cavado — são duas descidas e duas salas antes de a galeria começar.
     */
    public void turn() {
        blocked = 0;

        if (++turns < TURNS_PER_LEVEL || !shaft.mayDeepen()) {
            shaft = shaft.turned();

            return;
        }

        shaft = shaft.deepened();

        cut = 0;
        turns = 0;
    }

    /**
     * Mais uma posição que não se cava, e se já é hora de virar.
     *
     * @param limit quantas recusas seguidas bastam para a galeria virar
     * @return true quando esta foi a que fechou a conta. A galeria já
     *     virou, e a contagem recomeçou
     */
    public boolean blockedAgain(int limit) {
        if (++blocked < limit) {
            return false;
        }

        turn();

        return true;
    }

    /**
     * Quantas curvas fecham o círculo de um nível.
     *
     * <p>Quatro: a curva é no sentido horário, e na quarta a galeria
     * está de volta à direção em que começou. Ela deu a volta no nível.
     */
    public static final int TURNS_PER_LEVEL = 4;

    /** A picareta pegou. A contagem de recusas recomeça. */
    public void digging() {
        blocked = 0;
    }

    /**
     * A posição de agora fica para a passagem seguinte.
     *
     * <p>Serve a um caso só, e ele é real: o túnel chegou a uma pedra
     * cavável e havia minério colado nela. A picareta vai ao minério, e a
     * pedra do túnel continua lá — sem desandar o cursor, ele passaria
     * por cima dela e o túnel ficaria com um bloco no meio para sempre.
     */
    public void holdPosition() {
        if (cut > 0) {
            cut--;
        }
    }

    /**
     * Onde a rocha recomeça e não para mais — 2026-09-02.
     *
     * <p>Quem lê o mundo é o {@code MineDigging}; aqui só se percorre a
     * ordem de cavar a partir da boca e se pergunta. Vazio quer dizer
     * <b>não recue</b>: não há rocha entre a boca e o cursor.
     *
     * <p><b>O primeiro bloco fechado não é a frente</b>, e foi o que a
     * sessão de 2026-09-02 mostrou. Numa galeria já cavada ele costuma
     * ser um <b>resto solto no meio do túnel</b> — o autor desceu e
     * conferiu que ali começa corredor aberto. O cursor recuava 83 passos
     * até um desses restos, e a busca só reanda 64: passagem após
     * passagem ele voltava ao mesmo lugar, com o corredor à frente aberto
     * e a mina parada. Dois mineiros, vinte minutos, dois blocos.
     *
     * <p><b>O que distingue os dois é o que vem depois.</b> Resto solto
     * tem túnel aberto logo adiante; frente de escavação tem mais rocha.
     * Por isso só vale como frente a posição fechada cuja seguinte também
     * está fechada — ou que já é a última antes do cursor.
     *
     * <p><b>E por que a procura não anda de trás para frente</b>, que
     * seria mais curta. O cursor que disparou — o E33, que o save de
     * 08-27 trouxe a milhares de passos — aponta para fora do mundo
     * carregado, e o que se lê lá é <b>vazio, não rocha</b>: a volta
     * morria no primeiro vazio e deixava a mina presa. Os dois gametests
     * do E33 disseram isso em 2026-09-02, nesta ordem. Andando a partir
     * da boca, o recuo é tão longo quanto precise ser.
     *
     * <p>A premissa do recuo continua de pé: tudo o que vem antes da
     * frente devolvida está aberto ou é resto solto dentro de túnel
     * aberto, e nos dois casos há de onde alcançá-la.
     *
     * @param closed se a posição de índice {@code i} ainda está fechada
     */
    public OptionalInt frontierWhereRockBegins(IntPredicate closed) {
        for (int i = 0; i < cut; i++) {
            if (!closed.test(i)) {
                continue;
            }

            if (i + 1 >= cut || closed.test(i + 1)) {
                return OptionalInt.of(i);
            }
        }

        return OptionalInt.empty();
    }

    /**
     * Põe o cursor nesta posição da ordem de cavar — 2026-08-28.
     *
     * <p><b>A frente da galeria passou a ser lida do mundo</b>, e não
     * lembrada. O recuo passo a passo parava cedo demais: ele voltava até
     * achar uma posição de onde dava para bater, e o <b>túnel que o
     * jogador cavou à mão</b> oferece exatamente isso, num bolsão que não
     * se liga à escada do mod. O mineiro ficava no degrau 7 mirando uma
     * lanterna a vinte e quatro blocos, do outro lado da rocha.
     *
     * <p>Quem decide qual índice é a frente é {@code MineDigging}, que vê
     * o mundo; aqui só se obedece. Índice negativo vira zero — a picareta
     * não vai para antes do primeiro degrau.
     */
    public void rewindTo(int index) {
        cut = Math.max(0, index);
    }

    /**
     * Recua o cursor um passo — 2026-08-27.
     *
     * <p><b>Para quando ele marchou por dentro da rocha.</b> A posição
     * que o cursor aponta pode não ter túnel atrás dela: não há de onde
     * alcançá-la, e o mineiro é mandado para dentro da pedra. Recuar é o
     * único caminho de volta, e ele funciona porque a ordem de cavar é
     * um caminho <b>para fora da boca</b> — a posição anterior está
     * sempre mais perto do que já está aberto.
     *
     * <p>Mesma conta do {@link #holdPosition()}, e nome próprio porque a
     * intenção é outra: aquele guarda a posição de agora, este anda para
     * trás.
     */
    public void backUp() {
        holdPosition();
    }

    /**
     * Devolve ao cursor a posição que não chegou a ser cavada —
     * 2026-08-27.
     *
     * <p><b>O autor foi olhar em jogo, e a frase dele fecha o caso:</b>
     * <i>"tive que cavar até lá"</i>. O mod dizia estar abrindo a
     * galeria havia três sessões, e no mundo estava rocha maciça.
     *
     * <p>{@link #nextPosition()} avança o cursor <b>sempre</b>. Quando o
     * mineiro não conseguia chegar na pedra, a tarefa voltava para a
     * fila e a posição ficava para trás — o cursor marchava por dentro da
     * rocha, coluna após coluna, e o túnel nunca era aberto. É o mesmo
     * defeito que o {@link #holdPosition()} já conserta quando a picareta
     * desvia para o minério, e ninguém o chamava na desistência.
     *
     * <p><b>Por que a posição vem por parâmetro.</b> A mina é da colônia
     * e dois mineiros a partilham. Desandar às cegas devolveria o cursor
     * por cima do bloco que o <b>outro</b> acabou de pegar, e os dois
     * passariam a brigar pelo mesmo ponto. Só desanda quem abandona a
     * última posição entregue.
     *
     * @return true quando o cursor de fato voltou
     */
    public boolean holdPositionAt(ColonyPos at) {
        Objects.requireNonNull(at, "at");

        if (cut == 0 || !shaft.positionAt(cut - 1).equals(at)) {
            return false;
        }

        cut--;

        return true;
    }

    /** Onde a veia de minério estava, se o mineiro ainda a segue. */
    public Optional<ColonyPos> vein() {
        return Optional.ofNullable(vein);
    }

    /**
     * O mineiro achou minério aqui, e é daqui que a veia continua.
     *
     * <p>Minério não vem sozinho: um carvão tem outro do lado. Guardar a
     * posição é o que permite terminar a veia antes de voltar ao túnel —
     * sem isso o aldeão sairia de perto e teria de andar até lá de novo
     * na passagem seguinte.
     */
    public void followVein(ColonyPos ore) {
        vein = Objects.requireNonNull(ore, "ore");
    }

    /** Acabou o minério em volta. O túnel volta a mandar. */
    public void veinExhausted() {
        vein = null;
    }
}
