package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;

import java.util.Objects;
import java.util.UUID;

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
     */
    public void turn() {
        shaft = shaft.turned();
        blocked = 0;
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

    /** A picareta pegou. A contagem de recusas recomeça. */
    public void digging() {
        blocked = 0;
    }
}
