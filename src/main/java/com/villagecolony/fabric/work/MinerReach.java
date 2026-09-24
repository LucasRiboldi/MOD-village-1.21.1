package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.MineArm;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A que distância o mineiro alcança a pedra — 2026-08-27.
 *
 * <p><b>Uma conta só, e é esta.</b> Até hoje havia duas: o alcance media
 * com a posição real do aldeão, e o relatório media com
 * {@code getBlockPos()} — inteiro — e ainda truncava a raiz. Qualquer
 * distância entre 4,0 e 4,99 saía no log como <i>"4 blocks away"</i> e
 * estava <b>fora</b> de alcance.
 *
 * <p>A sessão das 22:19 passou dois mil e quatrocentos tiques dizendo
 * que o mineiro estava a quatro blocos da pedra que ele não alcançava, e
 * mandou procurar o defeito onde ele não estava. Instrumento que mente é
 * pior que instrumento nenhum.
 *
 * <p><b>Por que numa classe só dela.</b> {@code MinerWork} não carrega
 * fora do jogo — os estáticos dele pedem o registro de itens —, e isto
 * aqui é geometria: três subtrações e uma raiz, que se afirmam sem subir
 * servidor. Ver {@code MinerReachTest}.
 */
public final class MinerReach {

    /**
     * Quanto o braço do mineiro alcança, em blocos.
     *
     * <p>Medido em três dimensões, e o {@code dy} é o E30: quatro blocos
     * no plano mais quatro de altura são cinco e meia de distância real.
     * Enquanto a altura não contava, o mineiro batia na pedra de cima do
     * buraco e ela caía — o que fazia a mina descer sem ninguém dentro
     * dela.
     */
    public static final int REACH = 4;

    /**
     * Com que folga a navegação pode se dar por chegada, para o mineiro.
     *
     * <p><b>A sessão de 2026-08-28, 23:19</b>, e o mineiro que enfim
     * estava dentro da mina — y=44 é a galeria. Ele ficou lá parado
     * seiscentos tiques:
     *
     * <pre>
     * digging Pedra at 760, 44, 878, 4,2 blocks away
     *   (out of reach, he is at 756, 44, 878, walking to 758, 44, 878)
     * </pre>
     *
     * <p><b>Exatamente dois blocos do destino</b>, que era a folga de
     * casa do {@code WorkTargets}. A navegação se deu por chegada e
     * parou; o mod continuou dizendo "fora de alcance"; e ele moeu os
     * últimos dois blocos até o guarda devolver a tarefa. É o "rodando na
     * escada e não desce" que o autor viu.
     *
     * <p><b>Duas contas certas que não compunham.</b> O
     * {@code approachTo} escolhe um lugar <b>dentro</b> do braço — 758
     * está a 2,0 da pedra, e o braço é 4. O caminhante parava até dois
     * antes desse lugar. Somadas, 4,2: fora do braço para sempre, sem
     * que nenhuma das duas estivesse errada sozinha.
     *
     * <p><b>Zero, e a primeira tentativa foi um</b> — 2026-08-29, 04:26.
     * Com a folga em dois ele parava dois blocos antes; baixada para um,
     * ele passou a parar um bloco antes, e continuou fora do braço:
     *
     * <pre>
     * he is at 757, 44, 878, 4,4 blocks away;
     * it was walking to 758, 44, 878;
     * the place to stand is 758, 44, 878;
     * </pre>
     *
     * <p>O lugar escolhido era bom — a frase de desistência não traz
     * ressalva nenhuma —, e de cima dele ele alcançaria a 3,35. Um bloco
     * de folga jogou a conta para 4,27.
     *
     * <p><b>A lição é que folga nenhuma serve.</b> Enquanto ela existir,
     * a composição depende de sorte: o {@code approachTo} escolhe um
     * lugar <b>dentro</b> do braço, e qualquer sobra empurra para fora —
     * mais ainda quando a pedra está uma camada acima, porque o
     * {@code dy} come folga que o plano não come.
     *
     * <p>Com zero a garantia passa a ser <b>por construção</b>: ele fica
     * onde foi escolhido, e o lugar escolhido alcança. O medo que
     * segurava o zero — a navegação perseguir uma casa decimal e o
     * guarda devolver a tarefa — não custa nada de novo: era exatamente
     * isso que já acontecia, {@code stall 2399/2400}, com a diferença de
     * que agora ele tenta fechar o último bloco em vez de parar de
     * propósito.
     */
    public static final int ARRIVAL = 0;

    private MinerReach() {
    }

    /** A distância daqui até o centro daquele bloco. */
    public static double distanceTo(double x, double y, double z, BlockPos target) {
        double dx = x - (target.getX() + 0.5);
        double dy = y - (target.getY() + 0.5);
        double dz = z - (target.getZ() + 0.5);

        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Se daqui se alcança aquele bloco. */
    public static boolean isWithinReach(double x, double y, double z, BlockPos target) {
        return distanceTo(x, y, z, target) <= REACH;
    }

    /**
     * As posições de onde se pode bater numa pedra, da mais perto para a
     * mais longe — 2026-09-03.
     *
     * <p><b>É a ordem que torna a busca barata.</b> O
     * {@code MinerApproach.approachTo} varria o cubo de raio quatro inteiro
     * para ficar com o vizinho mais perto: setecentas e vinte e oito
     * posições, umas seiscentas leituras de bloco, <b>sempre</b> — mesmo
     * quando o lugar bom era o bloco colado ao lado.
     *
     * <p>O javadoc dele dizia que isso era aceitável porque rodava
     * <i>uma vez por pedra</i>. Deixou de rodar: a guarda de emparedada
     * de 2026-09-02 a chamou de dentro do laço do {@code nextCut}, que
     * olha até sessenta e quatro posições por passagem. Seiscentas
     * leituras viraram até <b>trinta e oito mil por tique</b>, e este
     * ciclo ainda estende a guarda ao minério.
     *
     * <p>Com as posições ordenadas por distância, a resposta é a
     * primeira que servir: dentro de um corredor o vizinho colado
     * responde, e a varredura inteira só é paga quando a resposta é
     * <i>não há nenhuma</i> — que é exatamente o caso em que ela
     * precisa ser paga.
     *
     * <p>O resultado é <b>o mesmo bloco de antes</b>, e não um parecido:
     * a distância é a mesma conta, o filtro do braço é o mesmo, e a
     * ordenação é estável — entre empatadas continua vencendo a primeira
     * na ordem {@code dx, dy, dz} do laço original.
     */
    public static final List<Vec3i> APPROACH_OFFSETS = approachOffsets();

    /**
     * A distância a que um aldeão de pé neste deslocamento fica da pedra.
     *
     * <p>O meio bloco de cada eixo se cancela no {@code x} e no
     * {@code z} — os dois são medidos do centro da coluna —, e sobra no
     * {@code y} porque quem fica de pé mede pelos <b>pés</b> e a pedra
     * mede pelo centro. É a mesma conta do {@link #distanceTo}, escrita
     * sem o bloco de referência.
     */
    static double offsetDistance(int dx, int dy, int dz) {
        return Math.sqrt(dx * dx + (dy - 0.5) * (dy - 0.5) + dz * dz);
    }

    private static List<Vec3i> approachOffsets() {
        List<Vec3i> offsets = new ArrayList<>();

        for (int dx = -REACH; dx <= REACH; dx++) {
            for (int dy = -REACH; dy <= REACH; dy++) {
                for (int dz = -REACH; dz <= REACH; dz++) {

                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }

                    if (offsetDistance(dx, dy, dz) > REACH) {
                        continue;
                    }

                    offsets.add(new Vec3i(dx, dy, dz));
                }
            }
        }

        // Estável de propósito — ver APPROACH_OFFSETS. List.sort é um
        // merge sort, e empate nenhum troca de lugar.
        offsets.sort(java.util.Comparator.comparingDouble(
                offset -> offsetDistance(offset.getX(), offset.getY(), offset.getZ())));

        return List.copyOf(offsets);
    }

    /**
     * A posição do Core no vocabulário do jogo.
     *
     * <p>Era um {@code new BlockPos(position.x(), position.y(),
     * position.z())} escrito aqui — o {@code toBlockPos} do adaptador
     * refeito por inteiro, e a única conversão do mod que morava fora da
     * fronteira. A ADR-005 §4 diz que ela acontece apenas lá; a frase
     * passou a ser verificada por {@code ConversionBoundaryTest}, e este
     * era o lugar que a desmentia.
     *
     * <p>Duas cópias não custam nada enquanto concordam. O dia em que o
     * jogo mudar o construtor de {@code BlockPos} — já mudou de pacote
     * uma vez, e é o argumento da própria ADR — só uma das duas seria
     * consertada.
     */
    static BlockPos at(ColonyPos position) {
        return MinecraftTypeAdapter.toBlockPos(position);
    }
}
