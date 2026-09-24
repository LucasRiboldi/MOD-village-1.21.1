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
 * A perna do mineiro: onde ele pisa a caminho da pedra — separado de
 * {@link MinerReach} em 2026-09-24, quando ele passou de 500 linhas.
 *
 * <p>O {@code MinerReach} responde "alcança daqui?"; esta classe responde
 * "para onde ele anda até alcançar", pela galeria e pela escada do poço, sem
 * atravessar a rocha. Os comentários de cada membro vieram junto sem mudança;
 * ver também {@code MinerLegTest}.
 */
public final class MinerLeg {

    private MinerLeg() {
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
    static final int STEPS_SCANNED = 2000;

    /**
     * A que distância da ordem de cavar o aldeão ainda está <b>dentro</b>
     * da passagem — 2026-09-09.
     *
     * <p>Dois blocos, e o número foi medido, não escolhido. O caracol
     * entrega cada degrau como três posições — piso, peito e cabeça — e
     * isso muda a resposta conforme <b>de onde</b> se olha:
     *
     * <pre>
     * na boca,     731,63,898 → 731,63,897 (piso)    1,00
     * em cima,     735,64,895 → 732,64,896 (cabeça)  3,16
     * </pre>
     *
     * <p>Quem está no corredor está encostado no piso dele. Quem está na
     * superfície em cima da mina só alcança a <b>cabeça</b> da escada,
     * que passa perto da superfície e é o que confundia a conta: há
     * posição da ordem a três blocos dele, e entre os dois há chão.
     *
     * <p>Não é {@link BuilderApproach#REACH} nem {@link #LEG}, e as três perguntas são
     * diferentes: o braço é o que ele alcança para <b>cavar</b>, a perna
     * é o que ele cumpre <b>andando</b>, e esta é onde ele <b>está</b>.
     * Usar a perna aqui foi o defeito de 09-09; o braço também não
     * serve, porque a cabeça da escada cabe nele.
     */
    static final int IN_THE_PASSAGE = 2;

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

        BlockPos mouth = MinerReach.at(mine.get().shaft().entry());

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
        //
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
    static int orderIndexNear(BlockPos position, MineArm mine, int scanned) {
        int nearest = -1;
        double best = Double.MAX_VALUE;

        for (int i = 0; i < scanned; i++) {
            double away =
                    Math.sqrt(position.getSquaredDistance(MinerReach.at(mine.shaft().positionAt(i))));

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
    static BlockPos stepAlongTheShaft(
            BlockPos villager, int goal, MineArm mine, int scanned, Footing footing) {

        int here = orderIndexNear(villager, mine, scanned);

        if (here < 0) {
            return null;
        }

        // <b>Estar perto da passagem não é estar nela</b> — 2026-09-09.
        //
        // O javadoc acima sempre prometeu <i>"nulo quando ele não está na
        // passagem"</i>, e o {@link #orderIndexNear} respondia outra
        // pergunta: se havia posição da ordem a menos de uma <b>perna</b>
        // — oito blocos. Quem está na superfície em cima da mina passa
        // nesse teste, e a escada corre debaixo dele.
        //
        // A partir daí a cadeia inteira mente. Ela é contígua <b>dentro
        // da ordem de cavar</b>, e é isso que os dois guardas ao lado
        // protegem — a parede do E34, o bloco não cavado do E32. Nenhum
        // dos dois olha o primeiro elo: se o aldeão não está no corredor,
        // não há corredor entre ele e o passo, por mais contíguo que o
        // trecho seja daí para frente.
        //
        // A sessão de 09-09 pagou por isso, e a queixa do autor foi
        // <i>"o mineiro está perdido, rodando no próprio eixo"</i>: com a
        // boca a cinco blocos dele, o passo saiu três blocos abaixo, do
        // outro lado do chão. A navegação não traça caminho para dentro
        // da rocha, então ela o virou para o alvo e o deixou lá — e o
        // guarda de imobilidade não pega quem <b>gira</b>. Dois minutos
        // por tentativa, até o guarda de travamento devolver a tarefa.
        //
        if (Math.sqrt(villager.getSquaredDistance(MinerReach.at(mine.shaft().positionAt(here))))
                > IN_THE_PASSAGE) {

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
        BlockPos start = MinerReach.at(mine.shaft().positionAt(here));

        BlockPos step = footing.standable(start) ? start : null;

        for (int i = here + direction; i >= 0 && i < scanned; i += direction) {
            BlockPos ahead = MinerReach.at(mine.shaft().positionAt(i));

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
}
