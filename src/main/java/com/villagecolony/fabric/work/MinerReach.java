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
            BlockPos villager, BlockPos destination, Optional<MineArm> mine, Footing footing) {

        return legTowards(villager, destination, mine, true, footing);
    }

    /**
     * O mesmo, sabendo se este corredor leva ao alvo — 2026-09-05.
     *
     * <p>A sobrecarga acima existe para quem tem uma frente só, que é o
     * caso de toda a bateria de unidade e era o caso da mina inteira até
     * 2026-09-04. Com quatro ramais os dois deixaram de coincidir: o
     * aldeão pode estar dentro do corredor de um e com o alvo no de
     * outro.
     *
     * @param corridorLeadsToTheTarget se avançar a frente <b>deste</b>
     *     corredor aproxima do alvo. Falso manda sair pela boca, que é
     *     por onde se chega a qualquer outro ramal e à superfície
     */
    public static BlockPos legTowards(
            BlockPos villager,
            BlockPos destination,
            Optional<MineArm> mine,
            boolean corridorLeadsToTheTarget,
            Footing footing) {

        if (mine.isEmpty()) {
            return destination;
        }

        if (Math.sqrt(villager.getSquaredDistance(destination)) <= LEG) {
            return destination;
        }

        BlockPos mouth = at(mine.get().shaft().entry());

        int scanned = Math.min(mine.get().cut(), STEPS_SCANNED);

        int there = orderIndexNear(destination, mine.get(), scanned);

        // <b>Destino fora da mina</b> — 2026-09-04. Fora da ordem de
        // cavar e não abaixo dele: dentro de uma mina o que se cava está
        // sempre embaixo, então alvo que não está nem na ordem nem abaixo
        // não é da mina. É a areia da praia, e a tarefa dela é do mesmo
        // mineiro.
        //
        // A distinção importa porque "fora da ordem" sozinho não decide:
        // a pedra que ele vai cavar também está fora dela enquanto a
        // frente não chegou lá. Aquela é embaixo, e para aquela se desce.
        //
        // <b>Medido contra a boca, e não contra o aldeão</b> — 2026-09-05,
        // e é a diferença entre a conta parada e a conta que oscila. Era
        // {@code destination.getY() >= villager.getY()}, e o Y do aldeão
        // muda de 44 para 45 a cada passo que ele dá no chão da galeria.
        // O mesmo alvo era lido ora como superfície, ora como fundo de
        // mina, em tiques seguidos — e o destino de caminhada alternava
        // junto. A sessão de 2026-09-04 22:40 tem os dois estados na
        // mesma linha de relatório, trinta segundos um do outro:
        //
        //   he is at 1449, 45, 66, walking to 1448, 44, 64
        //   he is at 1448, 45, 65, walking to 1455, 44, 67
        //
        // Dezesseis minutos assim, com a distância ao alvo parada em
        // 50,7. A boca não anda, e por isso responde sempre igual.
        boolean underground = destination.getY() < mouth.getY() - 1;

        // Três destinos, e não dois. O que está na ordem tem índice e o
        // passo vai até ele. O que está fora dela e <b>abaixo</b> é a
        // pedra que a frente de escavação ainda não alcançou: para lá se
        // desce, e o alvo é adiante da frente. O que está fora e não
        // abaixo é a superfície, e para lá se sai pela boca, que é o
        // índice zero.
        //
        // <b>Descer só vale se este corredor for o do alvo</b> —
        // 2026-09-05. Desde que a mina ganhou quatro rumos a ordem de um
        // ramal não é caminho para os outros três, e "abaixo e fora da
        // ordem" deixou de bastar: a pedra de outro ramal também é
        // abaixo, e avançar a frente <b>deste</b> enterra o mineiro cada
        // vez mais longe do que ele foi buscar. Foi o que af897f92 fez
        // por dezesseis minutos — o corredor dele corria para o sul e a
        // pedra estava a oeste.
        //
        // Quem sabe a resposta é o chamador, e não a geometria: ele tem o
        // ramal reservado, que é o dono do alvo, e o corredor em que o
        // aldeão está. Ver MineDigging.armToWalk.
        int goal = there >= 0 ? there : underground && corridorLeadsToTheTarget ? scanned : 0;

        BlockPos step = stepAlongTheShaft(villager, goal, mine.get(), scanned, footing);

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
        if (step != null && !step.equals(villager)) {
            return step;
        }

        // Nenhum passo pelo corredor deste ramal.
        //
        // Alvo lá embaixo: entra-se pela boca. Não se pede à navegação um
        // caminho de vinte blocos por dentro da rocha, que é a razão de
        // esta classe existir.
        if (underground) {
            return mouth;
        }

        // A boca é o desvio de quem <b>vai entrar</b>. Quem já está fora
        // indo para outro ponto de fora não passa por ela, e mandá-lo
        // para lá era devolver à mina o mineiro de areia toda vez que ele
        // conseguia sair. A céu aberto a navegação dá conta sozinha — é
        // justamente o caminho que ela sabe traçar.
        //
        // <b>Mas quem está lá dentro sai pela boca primeiro</b> —
        // 2026-09-05. A frase acima só vale para quem <b>já está fora</b>,
        // e a conta não perguntava isso: devolvia o destino cru a
        // qualquer um. A tarefa de areia não reserva ramal, então o
        // mineiro que acabava de cavar recebia a duna da superfície com
        // {@code armOf} vazio e a galeria inteira entre ele e ela:
        //
        //   gave up the stone at 1434, 62, 67 — it walked for 2400 ticks
        //   of work time without arriving. the miner is at 1448, 45, 65
        //
        // Vinte e dois blocos, dezenove deles de altura, sem escada que a
        // navegação enxergue. Ele nunca chegou, em nenhuma das oito vezes
        // que tentou naquela sessão.
        return villager.getY() >= mouth.getY() - 1 ? destination : mouth;
    }

    /**
     * Se este ponto está no corredor deste ramal — 2026-09-05.
     *
     * <p>A pergunta que faltava para o {@code MineDigging} saber por qual
     * corredor mandar o aldeão andar. O passo do {@link #legTowards} anda
     * pela ordem de cavar de <b>um</b> ramal, e com quatro rumos abertos
     * o corredor de um não serve de caminho para o outro: pedir um passo
     * pelo ramal que ele reservou, estando ele parado dentro de outro,
     * devolve nulo em todo tique.
     */
    public static boolean isOnCorridorOf(BlockPos position, MineArm arm) {
        return orderIndexNear(position, arm, Math.min(arm.cut(), STEPS_SCANNED)) >= 0;
    }

    /**
     * O ponto da ordem de cavar mais perto desta posição, se algum estiver
     * a uma perna dela.
     *
     * <p>É a pergunta <i>"isto está no corredor?"</i>, e serve às duas
     * pontas: onde o aldeão está e para onde ele vai. Conta aritmética
     * pura, sem leitura de bloco.
     *
     * @return o índice, ou {@code -1} quando nada da ordem está perto —
     *     inclusive quando a mina não tem nada cavado
     */
    private static int orderIndexNear(BlockPos position, MineArm mine, int scanned) {
        int nearest = -1;
        double best = Double.MAX_VALUE;

        for (int i = 0; i < scanned; i++) {
            double away =
                    Math.sqrt(position.getSquaredDistance(at(mine.shaft().positionAt(i))));

            if (away < best) {
                best = away;
                nearest = i;
            }
        }

        return best <= LEG ? nearest : -1;
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
            BlockPos villager, int goal, MineArm mine, int scanned, Footing footing) {

        int here = orderIndexNear(villager, mine, scanned);

        if (here < 0) {
            return null;
        }

        // <b>Para que lado</b> — 2026-09-04. Até aqui o passo só sabia
        // andar para a frente, rumo à frente de escavação, e o destino
        // nem chegava a entrar nesta conta. Acertava por acidente no caso
        // comum — entrar para cavar fundo é ir para a frente — e errava
        // sempre que o alvo estava atrás: o mineiro descia cada vez mais
        // para longe dele, galeria adentro, até a sessão acabar.
        //
        // De dentro da mina, sair é andar para trás — e quem decide se
        // é o caso é o {@code goal} que o chamador montou.
        int direction = goal >= here ? 1 : -1;

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

        for (int i = here + direction; i >= 0 && i < scanned; i += direction) {
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
     * As posições de onde se pode bater numa pedra, da mais perto para a
     * mais longe — 2026-09-03.
     *
     * <p><b>É a ordem que torna a busca barata.</b> O
     * {@code MinerWork.approachTo} varria o cubo de raio quatro inteiro
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
    private static BlockPos at(ColonyPos position) {
        return MinecraftTypeAdapter.toBlockPos(position);
    }
}
