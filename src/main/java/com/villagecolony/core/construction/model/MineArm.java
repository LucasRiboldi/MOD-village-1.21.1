package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.IntPredicate;

/**
 * Um ramal da mina, e o mineiro que cava nele — decisão do autor,
 * 2026-09-04.
 *
 * <p><b>A frase dele:</b> <i>"mineiros distintos escolhem caminhos de
 * perfuração distintos dentro das minas"</i>, e a forma escolhida foi
 * <i>ramais da mesma escada</i>: um poço só, e cada mineiro com o seu
 * braço a partir dele.
 *
 * <p><b>O que ele desfaz.</b> Até aqui a mina tinha <b>um</b> cursor e
 * <b>um</b> dono. Na sessão de 2026-09-04 isso apareceu por inteiro: o
 * terceiro mineiro passou os trinta e sete minutos em {@code waiting for
 * the shaft — 6dccfd1e is in it}. A reserva estava certa — dois na mesma
 * escada se atropelam —, e o que faltava era haver mais de uma frente
 * para reservar.
 *
 * <p><b>A geometria já estava pronta.</b> O nível da mina virou, em
 * 2026-09-04 de manhã, um anel de quatro braços em volta do poço: a
 * galeria anda {@link MineShaft#ARM} colunas, vira, e na quarta curva a
 * mina desce. Aqueles quatro braços eram percorridos <b>em sequência</b>
 * por um mineiro só. São os mesmos quatro, agora <b>ao mesmo tempo</b>,
 * um por mineiro.
 *
 * <p><b>Cada braço tem a própria forma</b>, e ela difere das outras só na
 * direção da galeria: boca e descida são do poço, que é compartilhado.
 * Os índices abaixo de {@link MineShaft#CARVED} — a escada e as duas
 * salas — apontam para as <b>mesmas</b> posições em todos os braços, e é
 * isso que faz o poço ser cavado uma vez só: quem chegar primeiro o abre,
 * e os outros varrem por cima do que já é ar.
 */
public final class MineArm {

    /** A forma deste braço: o poço da mina, virado para o rumo dele. */
    private MineShaft shaft;

    /**
     * A próxima posição a olhar, na ordem de cavar deste braço.
     *
     * <p>Anda mesmo quando a posição não se cava — ar, água, bedrock, a
     * casa da vila. É uma fronteira, e não uma conta de blocos: para trás
     * dela está tudo o que já foi decidido.
     */
    private int cut;

    /**
     * Quantas posições seguidas vieram impossíveis de cavar.
     *
     * <p><b>Não vai para o disco</b>: é a contagem de uma passagem, não
     * um fato sobre a mina.
     *
     * <p><b>E ela mudou de dono em 2026-09-04.</b> Morava na
     * {@link Mine}, com a razão escrita: <i>"a galeria que ela faz virar
     * é da colônia: dois mineiros na mesma escada esbarram na mesma lava,
     * e duas contagens separadas pediriam dezesseis recusas para uma
     * curva que precisa de oito"</i>. Aquilo valia quando havia uma
     * frente só. Com um braço por mineiro a razão <b>se inverte</b>: dois
     * mineiros esbarram em lavas <b>diferentes</b>, e uma contagem
     * compartilhada encerraria o braço de quem está trabalhando por causa
     * da pedra que o outro não conseguiu cavar.
     */
    private int blocked;

    /**
     * O último minério cavado, enquanto a veia não acabar.
     *
     * <p>Também não vai para o disco, e também passou a ser do braço:
     * duas veias em dois ramais são duas veias, e lembrá-las num campo só
     * faria um mineiro herdar o carvão do outro, a vinte blocos dele.
     */
    private ColonyPos vein;

    /**
     * Se este braço acabou.
     *
     * <p>Duas portas para cá: chegar ao fim das {@link MineShaft#ARM}
     * colunas, ou esbarrar tantas vezes que não valha insistir. Nas duas
     * o braço para de aceitar picareta, e o mineiro procura outro livre.
     *
     * <p>Quando os quatro acabam, a {@link Mine} desce — que é a mesma
     * regra de antes, quando as quatro curvas eram dadas em sequência
     * pelo mesmo aldeão.
     */
    private boolean done;

    MineArm(MineShaft shaft, int cut) {
        this.shaft = Objects.requireNonNull(shaft, "shaft");
        this.cut = cut;
    }

    /** A forma deste braço. */
    public MineShaft shaft() {
        return shaft;
    }

    public int cut() {
        return cut;
    }

    /** Se este braço já não aceita picareta. */
    public boolean isDone() {
        return done;
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
     * Se a galeria deste braço chegou ao fim dele.
     *
     * <p>O teto de raio do autor, perguntado antes de a posição ser
     * gasta. Ver {@link MineShaft#ARM}.
     */
    public boolean reachedTheEndOfTheArm() {
        return shaft.beyondTheArm(cut);
    }

    /**
     * Este braço acabou: o mineiro procura outro.
     *
     * <p>Ocupa o lugar do antigo {@code Mine.turn}. A diferença é que o
     * braço <b>não vira</b>: o rumo dele é fixo, e virar era o jeito de
     * um cursor só visitar quatro rumos em sequência. Quem troca de rumo
     * agora é o mineiro, trocando de braço.
     */
    public void finish() {
        done = true;
        blocked = 0;
    }

    /**
     * Mais uma posição que não se cava, e se já basta para encerrar o
     * braço.
     *
     * @param limit quantas recusas seguidas bastam
     * @return true quando esta foi a que fechou a conta
     */
    public boolean blockedAgain(int limit) {
        if (++blocked < limit) {
            return false;
        }

        finish();

        return true;
    }

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
     * Desanda o cursor se ele acabou de dar <b>esta</b> posição.
     *
     * <p>{@link #nextPosition()} avança sempre; quando quem chamou não
     * gastou a posição, ela não pode ser perdida.
     */
    public boolean holdPositionAt(ColonyPos at) {
        if (cut == 0 || !shaft.positionAt(cut - 1).equals(at)) {
            return false;
        }

        holdPosition();

        return true;
    }

    /** Recua o cursor um passo. */
    public void backUp() {
        holdPosition();
    }

    /** Põe o cursor nesta posição da ordem de cavar. */
    public void rewindTo(int index) {
        cut = Math.max(0, index);
    }

    /**
     * Onde a rocha recomeça e não para mais.
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

    /** A veia que este mineiro está seguindo, se está seguindo alguma. */
    public Optional<ColonyPos> vein() {
        return Optional.ofNullable(vein);
    }

    public void followVein(ColonyPos ore) {
        vein = Objects.requireNonNull(ore, "ore");
    }

    public void veinExhausted() {
        vein = null;
    }

    /**
     * O braço recomeça num nível novo: forma nova, cursor no primeiro
     * degrau, e ele volta a aceitar picareta.
     */
    void restartAt(MineShaft deeper) {
        shaft = Objects.requireNonNull(deeper, "deeper");
        cut = 0;
        blocked = 0;
        vein = null;
        done = false;
    }
}
