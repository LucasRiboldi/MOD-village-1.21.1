package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.util.math.BlockPos;

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
     * A perna de caminhada que a navegação do jogo cumpre sem se perder.
     *
     * <p>Oito blocos, e o número vem do que se viu: a mina desce vinte, e
     * um destino a vinte atravessando rocha devolve caminho parcial. Oito
     * é curto o bastante para o caminho ser contínuo e longo o bastante
     * para ele não reavaliar o destino a cada passo.
     */
    public static final int LEG = 8;

    /**
     * Quantas posições da ordem de cavar a busca do passo olha.
     *
     * <p>A galeria não tem fim, e a conta é aritmética pura — nenhuma
     * leitura de bloco —, mas ainda assim ela não pode crescer com a
     * sessão. Duas mil posições são umas seiscentas colunas: mais mina
     * do que qualquer sessão cavou até hoje, e barato o bastante para
     * rodar todo tique.
     *
     * <p>Mina maior que isto cai no destino de sempre, que é a boca —
     * o comportamento de antes deste conserto.
     */
    private static final int STEPS_SCANNED = 2000;

    /**
     * Para onde mandar o aldeão agora — um passo pela escada.
     *
     * <p><b>A sessão da meia-noite mostrou onde ele estava</b>, e foi a
     * primeira vez que se soube: <i>"the miner is at 734, 66, 878"</i>.
     * Y 66 é a superfície. Ele estava vinte e um blocos em linha reta
     * <b>acima</b> da galeria, em cima do chão, mirando uma pedra no
     * fundo da mina.
     *
     * <p>A navegação do jogo recebe um destino a vinte blocos
     * atravessando rocha maciça, devolve caminho parcial, e ele
     * estaciona no ponto mais próximo que consegue — bem ali em cima. É
     * o sintoma que o MineColonies registrou na issue 4297 com as mesmas
     * palavras, e o remendo do jogador é o mesmo que o autor fez: cavar
     * até lá.
     *
     * <p><b>Não se pede à navegação um caminho que ela não sabe
     * traçar.</b> Pede-se um passo de cada vez.
     *
     * <p><b>E a primeira versão só sabia dar dois passos — o E35.</b>
     * Longe, o destino era a boca; perto da boca, o destino virava a
     * pedra, vinte blocos abaixo e do outro lado da rocha. A sessão de
     * 2026-08-28 pegou o segundo mineiro <b>oscilando na fronteira</b>:
     *
     * <pre>
     * 740, 65, 895  ->  8,77 da boca   FORA da perna  -> mandado à boca
     * 739, 65, 896  ->  7,55 da boca   DENTRO         -> mandado à pedra
     * 741, 63, 898  ->  9,00 da boca   FORA           -> mandado à boca
     * </pre>
     *
     * <p>Ele andava para a boca, cruzava os oito blocos, recebia um
     * destino que a navegação não cumpre, derivava, saía dos oito, e
     * recomeçava. Para sempre. A descida tem vinte blocos e a perna tem
     * oito: são três passos, e o sistema só sabia dar dois.
     *
     * <p><b>Agora a mina sabe o caminho dela.</b> A ordem de cavar
     * <b>é</b> um corredor contínuo a partir da boca — tudo o que vem
     * antes da frente já está aberto —, e o passo seguinte é o ponto
     * mais avançado dessa ordem que ainda caiba numa perna, contado a
     * partir de onde ele está. Um degrau de cada vez, escada abaixo, e
     * sem fronteira nenhuma para oscilar em volta.
     *
     * <p>Não é a solução do MineColonies, que trocou a navegação inteira
     * por um A* próprio. É a que cabe aqui, e usa um dado que o mod já
     * tem de graça.
     *
     * @param mine a mina desta colônia, vazia quando não há uma — a
     *     pedra de superfície e a areia não têm descida a fazer
     */
    /**
     * O que o mundo responde sobre uma posição da ordem de cavar.
     *
     * <p>São <b>duas</b> perguntas, e o E34 é o que acontece quando se
     * usa uma no lugar da outra. Juntas num tipo só porque, soltas como
     * dois {@code Predicate}, nada impede trocá-las de lugar na chamada —
     * e o erro seria silencioso.
     */
    public interface Footing {

        /** A passagem continua por aqui? */
        boolean passable(BlockPos at);

        /** Este serve de destino — o aldeão fica de pé nele? */
        boolean standable(BlockPos at);
    }

    public static BlockPos legTowards(
            BlockPos villager, BlockPos destination, Optional<Mine> mine, Footing footing) {

        if (mine.isEmpty()) {
            return destination;
        }

        if (Math.sqrt(villager.getSquaredDistance(destination)) <= LEG) {
            return destination;
        }

        BlockPos step = stepAlongTheShaft(villager, mine.get(), footing);

        // <b>Passo que não sai do lugar não é passo</b> — 2026-09-02. A
        // ordem de cavar entregava a posição em que o mineiro já estava,
        // e o destino igual à posição faz a navegação não ter o que
        // fazer: ele "chega" sem andar, o contador de travamento sobe
        // até 2.400, e a tarefa volta para a fila dois minutos depois
        // sem um bloco cavado. Dez minutos de sessão, zero pedra:
        //
        //   digging Diorito at 709, 44, 878, 9,0 blocks away
        //   (out of reach, he is at 718, 44, 878, walking to 718, 44, 878)
        //
        // Vale como não ter achado passo nenhum, e a saída para isso já
        // existia: voltar à boca, de onde a ordem volta a funcionar.
        return step != null && !step.equals(villager)
                ? step
                : at(mine.get().shaft().entry());
    }

    /**
     * O ponto mais avançado da ordem de cavar que ainda cabe numa perna.
     *
     * <p><b>Contíguo, e é o que importa.</b> A ordem dobra — a escada
     * desce para um lado, a sala se abre, o segundo lance vira, e a
     * galeria corre para outro. Um ponto avançado pode passar
     * <b>perto</b> dele por fora da rocha sem que haja caminho: pegar
     * "o último que estiver a oito blocos" mandaria o aldeão atravessar
     * parede.
     *
     * <p>Então a busca anda a partir de onde ele está: acha a posição da
     * ordem mais perto dele e caminha para a frente <b>enquanto</b> as
     * posições continuarem ao alcance. A primeira que sair encerra o
     * passo, e o que ficou é um trecho contínuo do corredor.
     *
     * @return nulo quando ele não está na passagem — na superfície, longe
     *     da boca. Aí quem responde é a boca
     */
    private static BlockPos stepAlongTheShaft(
            BlockPos villager, Mine mine, Footing footing) {
        int scanned = Math.min(mine.cut(), STEPS_SCANNED);

        int here = -1;
        double nearest = Double.MAX_VALUE;

        for (int i = 0; i < scanned; i++) {
            double away = Math.sqrt(villager.getSquaredDistance(at(mine.shaft().positionAt(i))));

            if (away < nearest) {
                nearest = away;
                here = i;
            }
        }

        if (here < 0 || nearest > LEG) {
            return null;
        }

        // Só entra como destino o que aguenta um aldeão de pé — o E32,
        // 2026-09-02. A ordem é uma lista de blocos A CAVAR: duas de cada
        // três posições da escada são o peito e a cabeça, e as que o cursor
        // entregou podem nunca ter sido cavadas (`cut` conta entrega, não
        // picareta). Nenhuma das duas serve para caminhar até lá.
        //
        // O filtro pega as duas com a mesma pergunta, porque as duas
        // reprovam por motivos que o `standable` já sabe ver: na camada de
        // cabeça o que há embaixo é ar, e no bloco não cavado há colisão.
        //
        // E o laço para na primeira posição FECHADA — o E34, 2026-09-02.
        //
        // Escrito ao consertar o E32, este laço parava só no alcance e
        // pulava o que não fosse pisável para continuar somando adiante.
        // Isso é a contiguidade **perdida**: basta um vão aberto coincidir
        // com um índice mais avançado da ordem para o passo saltar a
        // parede que existe entre ele e o aldeão.
        //
        // Dois mundos produzem esse vão, e nenhum deles é raro: o túnel
        // que o jogador cavou à mão — o E34 como ele apareceu em 08-28,
        // com os dois mineiros mirando uma lanterna dentro de um bolsão
        // que não se liga à escada — e a caverna natural que a ordem
        // atravessa.
        //
        // São duas perguntas diferentes, e é por isso que existe o
        // Footing. **Atravessar** decide se o corredor continua: as
        // camadas do peito e da cabeça são abertas e ninguém fica de pé
        // nelas, então parar nelas travaria a descida no primeiro degrau.
        // **Ficar de pé** decide onde a perna termina.
        BlockPos start = at(mine.shaft().positionAt(here));

        BlockPos step = footing.standable(start) ? start : null;

        for (int i = here + 1; i < scanned; i++) {
            BlockPos ahead = at(mine.shaft().positionAt(i));

            if (Math.sqrt(villager.getSquaredDistance(ahead)) > LEG) {
                break;
            }

            if (!footing.passable(ahead)) {
                break;
            }

            if (footing.standable(ahead)) {
                step = ahead;
            }
        }

        // Nulo quando nada da passagem serve: quem responde é a boca, que
        // é superfície e onde se fica de pé por construção.
        return step;
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
    private static BlockPos at(ColonyPos position) {
        return MinecraftTypeAdapter.toBlockPos(position);
    }
}
