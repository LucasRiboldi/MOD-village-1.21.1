package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.core.coordination.IdleReason;
import com.villagecolony.fabric.integration.BlockProtection;
import com.villagecolony.fabric.integration.MineLighting;
import com.villagecolony.fabric.integration.MineMouth;
import com.villagecolony.fabric.integration.OreVein;
import com.villagecolony.fabric.integration.RingSweep;
import com.villagecolony.fabric.integration.StonePatch;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Descer a mina, e achar a próxima posição a cavar — a Regra 29.
 *
 * <p><b>Por que saiu do {@code MinerWork}.</b> O mineiro faz dois
 * trabalhos que não se parecem: desce a escada atrás de pedra e varre a
 * superfície atrás de areia. Os dois moravam no mesmo arquivo, que
 * chegou a 690 linhas — o pior caso do teto de 500 que o projeto se
 * impôs. O que ficou lá é o que os dois compartilham: a picareta, o baú,
 * o guarda de travamento e a tarefa.
 *
 * <p>Aqui está só a geometria em movimento: onde a mina começa, para que
 * lado ela desce, e qual é a próxima posição que vale a picareta.
 *
 * <p><b>Sem estado próprio.</b> O que dura entre uma passagem e outra
 * mora na {@link Mine} da colônia, que é gravada — a fronteira e o lado
 * da galeria — ou é derivado do identificador da colônia, como o lado da
 * descida. Duas cópias disso já custaram uma segunda escada por colônia.
 */
public final class MineDigging {

    /** Quantas posições da mina uma passagem examina antes de desistir. */
    private static final int CUTS_PER_SEARCH = 64;

    /** Recusas seguidas antes de a galeria virar. */
    private static final int BLOCKED_BEFORE_TURNING = 8;

    /** A que distância do centro a mina se abre — o fim da vila. */
    private static final int MINE_DISTANCE = 40;

    /**
     * As frações da distância que a busca tenta, em centésimos.
     *
     * <p>Cheia primeiro, que é a intenção do autor, e depois mais perto.
     * <b>Nunca mais longe:</b> "o fim da vila" é um teto, e a bateria
     * encurta essa distância para o mineiro não comer a pedra da arena
     * do lado.
     */
    private static final int[] REACHES = {100, 75, 50};

    /**
     * As frações que a segunda passagem tenta, quando a primeira falha.
     *
     * <p><b>Aqui a busca vai mais longe de propósito</b>, e é decisão do
     * autor em 2026-08-26: <i>ela aceita uma boca ruim, procura mais
     * longe</i>. "O fim da vila" deixa de ser teto quando a alternativa
     * é a colônia sem pedra.
     *
     * <p>Continua proporcional a {@link #mineDistance}, e não um número
     * solto: a bateria encurta essa distância para o mineiro não comer a
     * arena vizinha, e uma segunda passagem em blocos absolutos furaria
     * essa garantia.
     */
    private static final int[] FARTHER = {150, 200};

    /**
     * A janela de altura da boca ruim, para cima e para baixo.
     *
     * <p>Mais larga que {@link #LOOK_UP} e {@link #LOOK_DOWN}: a boca
     * boa é o fim da vila, no nível dela; a ruim aceita subir o morro ou
     * descer a depressão, porque a alternativa é não haver mina.
     *
     * <p><b>O que ela não relaxa:</b> água em cima e a Regra 3. Mina
     * inundada não é mina ruim, é mina quebrada; e peça de vila gerada
     * ou construção da colônia continua intocável em qualquer passagem.
     */
    private static final int POOR_UP = 12;

    private static final int POOR_DOWN = 24;

    /**
     * Quanto anda a diagonal, em centésimos da distância cheia.
     *
     * <p>Setenta, que é o cateto de um quadrado de hipotenusa cem. Assim
     * a boca na diagonal fica <b>à mesma distância</b> do centro que a
     * boca no eixo, e o teto de "o fim da vila" continua sendo teto.
     */
    private static final int DIAGONAL = 70;

    /** Mais perto que isto a escada desceria sob a própria vila. */
    private static final int NEAREST_MOUTH = 2;

    /**
     * Quanto acima do nível da vila a boca pode nascer.
     *
     * <p>Curto de propósito: a boca é <b>o fim da vila</b>, e não o topo
     * do morro ao lado. Foi por olhar oito para cima que a primeira
     * versão desta busca abriu uma mina seis blocos acima do centro, em
     * cima do piso da arena vizinha.
     */
    private static final int LOOK_UP = 3;

    /** E quanto abaixo, para a boca numa depressão. */
    private static final int LOOK_DOWN = 12;

    /** O assunto do registrador para a boca que não se acha — 2026-08-22. */
    private static final String MOUTH_SUBJECT = "miner mine mouth";

    /** O assunto do baú da boca, que não se acha onde pôr — 2026-09-02. */
    private static final String CHEST_SUBJECT = "miner mouth chest";

    /** E o da pedra de superfície, que é a alternativa a ela — 2026-08-25. */
    private static final String SURFACE_SUBJECT = "miner surface stone";

    /**
     * A que distância se raspa pedra exposta quando não há mina.
     *
     * <p>O mesmo raio da areia, e pelo mesmo motivo: é o que um aldeão
     * percorre e volta dentro do expediente. Mais que isso e ele passa o
     * dia andando.
     */
    private static final int SURFACE_RADIUS = 48;

    /**
     * A distância em vigor. É {@link #MINE_DISTANCE}, menos nos testes.
     *
     * <p>A bateria roda arenas lado a lado no mesmo mundo, e uma mina
     * aberta a quarenta blocos sai da arena dela e cava o cenário do
     * teste vizinho. Um teste que destrói o cenário de outro é pior que
     * um teste que não existe.
     */
    private static int mineDistance = MINE_DISTANCE;

    /** O raio de superfície em vigor. É {@link #SURFACE_RADIUS}, menos nos testes. */
    private static int surfaceRadius = SURFACE_RADIUS;

    private MineDigging() {
    }

    /** Aproxima a boca da mina. Só os testes precisam disso. */
    public static void shortenMineDistanceTo(int blocks) {
        if (blocks <= 0) {
            throw new IllegalArgumentException("Distance must be positive: " + blocks);
        }

        mineDistance = blocks;
    }

    /** Devolve a distância ao valor de jogo. */
    public static void restoreMineDistance() {
        mineDistance = MINE_DISTANCE;
    }

    /**
     * Encurta a varredura de pedra exposta. Só os testes precisam disso.
     *
     * <p>Mesmo motivo de {@link #shortenMineDistanceTo}: quarenta e oito
     * blocos saem da arena e raspam o cenário do teste vizinho.
     */
    public static void shortenSurfaceRadiusTo(int blocks) {
        if (blocks <= 0) {
            throw new IllegalArgumentException("Radius must be positive: " + blocks);
        }

        surfaceRadius = blocks;
    }

    /** Devolve o raio de superfície ao valor de jogo. */
    public static void restoreSurfaceRadius() {
        surfaceRadius = SURFACE_RADIUS;
    }

    /**
     * A próxima posição da mina, e a mina se ela ainda não existe.
     *
     * <p>Posição que não se pode cavar não para a mina — pula-se para a
     * seguinte, e a galeria vira depois de uma sequência de recusas. É a
     * frase do autor: <i>sempre que encontrar uma barreira que impeça de
     * realizar estas ações ele começa a recolher para outro lado</i>.
     *
     * <p>Vazio quer dizer "nesta passagem, não": ou a boca não pôde ser
     * aberta, ou as sessenta e quatro posições olhadas estavam todas
     * abertas ou proibidas. A passagem seguinte continua de onde esta
     * parou, porque a fronteira ficou guardada na mina.
     */
    public static Optional<BlockPos> nextTarget(
            ServerWorld world, UUID workerId, UUID colonyId, BlockPos center) {

        Optional<Mine> mine = mineOf(world, workerId, colonyId, center);

        if (mine.isEmpty()) {
            // A boca não pôde nascer. Em vez de a colônia ficar sem a
            // única fonte de pedra que tem, ela raspa o que estiver
            // exposto em volta — ver exposedStone.
            return exposedStone(world, workerId, colonyId, center);
        }

        IdleLog.clear(colonyId, SURFACE_SUBJECT);

        if (!MineClaims.claim(colonyId, workerId)) {
            // <b>A escada é de um só</b> — 2026-08-28. O cursor da
            // galeria mora no Mine e é um; dois mineiros perguntando na
            // mesma passagem recebiam a mesma posição, andavam para o
            // mesmo bloco, e escreviam "could not reach the stone" no
            // mesmo tique. Esse aviso recua o cursor, e ele recuava duas
            // vezes por um bloco.
            //
            // Quem não é o dono fica sem alvo, e não em alvo errado:
            // ele volta a perguntar na passagem seguinte, e herda a mina
            // no ciclo em que o dono largar o trabalho. Ver MineClaims.
            return Optional.empty();
        }

        Optional<BlockPos> found = followingTheVein(world, mine.get())
                .or(() -> nextCut(world, workerId, mine.get()));

        if (found.isEmpty()) {
            // <b>Escada que ninguém está usando volta a ser de quem
            // quiser</b> — 2026-09-02. A reserva seguia o trabalho
            // <i>aberto</i>, e não o trabalho <i>que anda</i>: um mineiro
            // que pegou a mina, cavou um bloco e parou de achar pedra
            // segurava a escada por dezesseis minutos, com o outro em
            // "waiting for the shaft" o tempo todo e a colônia recebendo
            // duas pedras. Nenhuma das saídas existentes alcançava esse
            // caso — o retainOnly só tira quem perdeu o trabalho, e o
            // guarda de travamento conta tiques andando até a pedra, que
            // é o que quem não tem alvo não faz.
            //
            // Uma passagem sem pedra não está usando o cursor, então
            // largar é seguro: a passagem seguinte pergunta de novo, e a
            // vez vai para quem estiver com trabalho.
            MineClaims.release(workerId);
        }

        return found;
    }

    /**
     * A pedra de superfície, quando a mina não tem onde nascer.
     *
     * <p><b>É alternativa, e não substituto.</b> A escada continua sendo
     * o caminho: é ela que traz carvão e ferro, e ela rende mais. Isto só
     * roda quando as vinte e quatro colunas da boca falharam — e o que
     * ele evita é o que a sessão de 2026-08-25 mostrou: uma vila cercada
     * de água ficou sem pedra nenhuma, a obra morreu de fome esperando
     * pedregulho, e a colônia parou de crescer por causa do terreno em
     * volta.
     *
     * <p>Mesma espiral da areia, mesmo teto por passagem, e a distinção
     * que o log precisa: "não terminei de olhar" não é "não há".
     */
    private static Optional<BlockPos> exposedStone(
            ServerWorld world, UUID workerId, UUID colonyId, BlockPos center) {

        Optional<BlockPos> found = RingSweep.around(
                workerId,
                center,
                surfaceRadius,
                column -> StonePatch.in(world, column, center.getY()));

        if (found.isEmpty()) {
            IdleLog.record(
                    colonyId,
                    SURFACE_SUBJECT,
                    RingSweep.pausedAt(workerId).isPresent()
                            ? IdleReason.SWEEP_INCOMPLETE
                            : IdleReason.NO_TARGET,
                    "no mine mouth, and no exposed stone within "
                            + surfaceRadius + " blocks either");

            return Optional.empty();
        }

        IdleLog.clear(colonyId, SURFACE_SUBJECT);

        return found;
    }

    /**
     * O minério colado no que acabou de sair, se a veia continuar.
     *
     * <p><b>A veia manda no túnel.</b> Minério não vem sozinho, e voltar
     * para a escada com metade da veia aberta faria o aldeão andar até lá
     * outra vez na passagem seguinte. Enquanto houver minério ao lado do
     * último, é ele o alvo.
     *
     * <p>Quando acabar, a memória da veia sai e o túnel volta a mandar —
     * senão o mineiro reperguntaria por ela a cada passagem, para sempre.
     */
    private static Optional<BlockPos> followingTheVein(ServerWorld world, Mine mine) {
        Optional<BlockPos> from = mine.vein().map(MinecraftTypeAdapter::toBlockPos);

        if (from.isEmpty()) {
            return Optional.empty();
        }

        Optional<BlockPos> more = OreVein.beside(world, from.get());

        if (more.isEmpty()) {
            mine.veinExhausted();

            return Optional.empty();
        }

        if (more.get().getY() < from.get().getY()) {
            Optional<BlockPos> step = stepBackUp(world, from.get());

            if (step.isEmpty()) {
                // Sem degrau possível não se desce. A colônia prefere
                // perder o minério a perder o mineiro — a escada volta a
                // mandar, e ela é subível por construção.
                mine.veinExhausted();

                return Optional.empty();
            }

            if (!step.get().equals(from.get())) {
                // O degrau primeiro, e o veio NÃO avança: a passagem
                // seguinte acha o mesmo minério com a saída pronta.
                return step;
            }
        }

        mine.followVein(MinecraftTypeAdapter.toColonyPos(more.get()));

        return more;
    }

    /**
     * O bloco que falta abrir para se voltar de um degrau abaixo —
     * decisão do autor, 2026-08-27.
     *
     * <p><b>Por que o veio precisa disto e a escada não.</b> A escada da
     * Regra 29 abre três blocos por degrau desde 08-27, e sobe-se por
     * ela na mesma geometria em que se desce. O veio não tem geometria:
     * {@link OreVein#beside} olha as seis faces, e a de baixo é a
     * primeira da lista. Minério empilhado abre um poço de um bloco de
     * largura, e de poço não se sobe — o aldeão não pula dois.
     *
     * <p><b>Qual bloco falta é sempre o mesmo:</b> o teto do nível de
     * onde ele veio. Subir um degrau pede dois blocos de ar no nível de
     * destino, e o de baixo já é o minério recém-tirado; o de cima é
     * este. Com ele aberto, a subida se faz um degrau de cada vez até a
     * boca do poço.
     *
     * @param from o minério de onde o veio parte — o nível ao qual o
     *     mineiro precisa conseguir voltar
     * @return o bloco a abrir; o próprio {@code from} quando já dá para
     *     subir; vazio quando não há degrau possível e portanto não se
     *     deve descer
     */
    private static Optional<BlockPos> stepBackUp(ServerWorld world, BlockPos from) {
        BlockPos ceiling = from.up();

        if (world.getBlockState(ceiling).isAir()) {
            return Optional.of(from);
        }

        return canDig(world, ceiling) ? Optional.of(ceiling) : Optional.empty();
    }

    /**
     * Se este bloco pode ser cavado — a Regra 3 e o impossível.
     *
     * <p>A mesma pergunta que {@link #nextCut} faz na sua volta, aqui
     * porque o veio precisa saber se consegue abrir a saída antes de
     * descer. Bedrock, lava e o que é da vila respondem não.
     */
    private static boolean canDig(ServerWorld world, BlockPos at) {
        if (!world.isInBuildLimit(at)) {
            return false;
        }

        BlockState state = world.getBlockState(at);

        if (!state.getFluidState().isEmpty()) {
            return false;
        }

        if (MineLighting.isLight(world, at, state)) {
            // A luz da mina não se cava, e o findTheFrontier a pula por
            // aqui: uma tocha na ordem de cavar seria "fronteira" para
            // sempre, e o cursor recuaria até ela toda passagem.
            return false;
        }

        return state.getHardness(world, at) >= 0
                && !BlockProtection.isVillageOriginal(world, at)
                && !BlockProtection.isColonyBuilt(at);
    }

    /**
     * Onde a galeria de fato acaba, lido do mundo — 2026-08-28.
     *
     * <p><b>A primeira posição ainda fechada, na ordem de cavar.</b> Ela
     * é conectada por construção: tudo o que vem antes já está aberto, e
     * a ordem é um caminho contínuo a partir da boca. É o que faz o
     * mineiro cavar sempre a partir de onde ele consegue estar.
     *
     * <p><b>Por que o recuo passo a passo não bastou</b> — sessão de
     * 2026-08-28, 00:14. Ele voltava até achar uma posição de onde dava
     * para bater, e o <b>túnel que o jogador cavou à mão</b> oferece
     * exatamente isso. Os dois mineiros ficaram parados no degrau 7 da
     * escada mirando uma lanterna a vinte e quatro blocos, dentro de um
     * bolsão que não se liga à escada por lugar nenhum:
     *
     * <pre>
     * the miner is at 725, 57, 898 ... the stone at 732, 45, 878 is Lanterna
     * </pre>
     *
     * <p>Lido do mundo, e não lembrado: é a mesma escolha que o baú da
     * boca e a marca do baú já faziam. O cursor gravado no save deixa de
     * poder mentir, e nenhum buraco solto engana a conta.
     *
     * <p>Posição que não se cava — bedrock, casa da vila — é pulada: ela
     * ficaria sendo a frente para sempre. Quem a trata é o
     * {@code blockedAgain}, que vira a galeria.
     *
     * <p><b>E ela procura de trás para frente</b> — 2026-09-02. A
     * premissa acima vale para um lado só: tudo o que vem <b>antes</b> da
     * frente está aberto, e não o contrário. Numa galeria já cavada, o
     * primeiro bloco fechado da ordem inteira é um resto solto dentro do
     * túnel — e o cursor recuava 83 passos até ele, passagem após
     * passagem, com o corredor à frente aberto. Ver
     * {@link Mine#frontierWhereRockBegins}.
     */
    private static void findTheFrontier(ServerWorld world, Mine mine) {
        OptionalInt frontier =
                mine.frontierWhereRockBegins(i -> isStillClosed(world, mine.shaft().positionAt(i)));

        if (frontier.isEmpty()) {
            return;
        }

        int step = frontier.getAsInt();
        BlockPos at = MinecraftTypeAdapter.toBlockPos(mine.shaft().positionAt(step));

        VillageColonyMod.LOGGER.info(
                "The gallery really ends at {} — the cursor was {} steps ahead of it",
                at.toShortString(),
                mine.cut() - step);

        mine.rewindTo(step);
    }

    /**
     * Se esta posição da ordem de cavar ainda é rocha que vale a picareta.
     *
     * <p>Ar, água, lava e a tocha da própria mina são espaço aberto; o
     * que não se cava — bedrock, casa da vila — não é frente, porque
     * ficaria sendo frente para sempre.
     */
    private static boolean isStillClosed(ServerWorld world, ColonyPos position) {
        BlockPos at = MinecraftTypeAdapter.toBlockPos(position);

        if (!world.isInBuildLimit(at)) {
            return false;
        }

        BlockState state = world.getBlockState(at);

        return !state.isAir() && state.getFluidState().isEmpty() && canDig(world, at);
    }

    /**
     * Recua o cursor até a frente que dá para alcançar — 2026-08-27.
     *
     * <p><b>A mina do autor ficou assim, e ele a diagnosticou a pé:</b>
     * <i>"tive que cavar até lá"</i>. O cursor marchou por dentro da
     * rocha enquanto o mineiro não conseguia chegar em nada, e parou
     * dezenas de blocos à frente do túnel de verdade. Dali para a frente
     * nada é alcançável, e a mina fica presa para sempre — inclusive
     * depois de a marcha ter sido consertada, porque o cursor já está no
     * lugar errado e gravado no save.
     *
     * <p>Recuar funciona porque a ordem de cavar é um caminho <b>para
     * fora da boca</b>: a posição anterior está sempre mais perto do que
     * já está aberto. Para quando acha uma de onde dá para bater, e ela
     * é a frente de verdade.
     *
     * <p>Para também no ar — chegou ao túnel aberto —, e é o que impede
     * o vaivém: a posição seguinte a uma aberta é justamente a que se
     * alcança de dentro dela.
     */
    private static void backUpToTheRealFrontier(ServerWorld world, Mine mine) {
        for (int back = 0; back < CUTS_PER_SEARCH && mine.cut() > 0; back++) {
            BlockPos at = MinecraftTypeAdapter.toBlockPos(
                    mine.shaft().positionAt(mine.cut()));

            if (!world.isInBuildLimit(at)) {
                return;
            }

            BlockState state = world.getBlockState(at);

            if (state.isAir() || !state.getFluidState().isEmpty()) {
                // Túnel aberto: a frente é aqui.
                return;
            }

            if (!MinerWork.approachTo(world, at).equals(at)) {
                // Há de onde bater nela. É a frente de verdade.
                return;
            }

            mine.backUp();

            if (back == 0) {
                VillageColonyMod.LOGGER.info(
                        "The gallery backs up from {} — there is nowhere to stand to dig it",
                        at.toShortString());
            }
        }
    }

    /**
     * O mineiro desistiu desta pedra — 2026-08-27.
     *
     * <p>Devolve a posição ao cursor da galeria, quando ela é de lá.
     * Sem isto o cursor passava por cima dela: o mod marchava pela ordem
     * de cavar enquanto o mundo continuava rocha maciça, e três sessões
     * seguidas terminaram com zero blocos e a galeria intacta.
     *
     * <p>Silencioso quando a pedra não era do túnel — veio, areia, ou a
     * posição que o outro mineiro já ultrapassou. Ver
     * {@link Mine#holdPositionAt}.
     */
    public static void couldNotReach(UUID colonyId, BlockPos stone) {
        VillageColonyMod.MINES.of(colonyId).ifPresent(mine -> {
            if (mine.holdPositionAt(MinecraftTypeAdapter.toColonyPos(stone))) {
                VillageColonyMod.LOGGER.info(
                        "The gallery keeps its place at {} — it was not dug",
                        stone.toShortString());
            }
        });
    }

    /**
     * A mina desta colônia, aberta agora se ainda não existir.
     *
     * <p>A mina é da colônia, e não deste mineiro: o segundo a descer
     * continua a mesma escada, e a que o save trouxe já vem com a
     * fronteira de ontem.
     */
    private static Optional<Mine> mineOf(
            ServerWorld world, UUID workerId, UUID colonyId, BlockPos center) {

        Optional<Mine> known = VillageColonyMod.MINES.of(colonyId);

        if (known.isPresent()) {
            // A boca de uma mina que veio do save pode nunca ter sido
            // mobiliada — a Regra 30 é de 2026-08-22 e há minas mais
            // velhas que ela. Idempotente: com baú lá, isto não faz nada.
            furnishAndLight(world, known.get());

            return known;
        }

        Side descent = sideOf(colonyId);

        Optional<BlockPos> mouth = mouthOf(world, center, descent);

        if (mouth.isEmpty()) {
            // A linha que faltava, e a falta dela custou três sessões.
            // O mineiro ficava "looking for stone" para sempre e nada
            // dizia que a mina sequer tinha onde nascer. Assunto próprio
            // porque MinerWork.run limpa o dele quando há tarefa aberta
            // — e aqui há tarefa, e mesmo assim não há mina.
            IdleLog.record(
                    colonyId,
                    MOUTH_SUBJECT,
                    IdleReason.NO_TARGET,
                    "no column within " + mineDistance + " blocks of " + center.toShortString()
                            + " can hold a mine mouth — tried 8 directions at 5 distances,"
                            + " the last two settling for a poor one");

            return Optional.empty();
        }

        IdleLog.clear(colonyId, MOUTH_SUBJECT);

        Mine opened = VillageColonyMod.MINES.open(
                colonyId,
                MineShaft.from(MinecraftTypeAdapter.toColonyPos(mouth.get()), descent));

        VillageColonyMod.LOGGER.info(
                "Miner {} opens a mine at {} - down {} then {} more",
                workerId,
                mouth.get().toShortString(),
                MineShaft.DESCENT,
                MineShaft.DESCENT);

        // A Regra 30: onde ele decide começar a cavar nascem a lanterna
        // e o baú da mina.
        furnishAndLight(world, opened);

        return Optional.of(opened);
    }

    /**
     * A lanterna e o baú da boca, e a luz da galeria.
     *
     * <p>Chamada também para mina já conhecida, e de propósito: mina de
     * save antigo não passou pela regra, e boca em chunk descarregado
     * não pôde ser mobiliada na primeira tentativa. {@code MineMouth} não
     * faz nada quando o baú já está lá.
     *
     * <p><b>E a luz da galeria desde 2026-08-28</b>, que é da mesma
     * natureza: de graça, idempotente, e no que já está cavado.
     */
    private static void furnishAndLight(ServerWorld world, Mine mine) {
        BlockPos mouth = MinecraftTypeAdapter.toBlockPos(mine.shaft().entry());

        Optional<BlockPos> chest = MineMouth.furnish(
                world, mouth, MinecraftTypeAdapter.toDirection(mine.shaft().descent()));

        if (chest.isEmpty()) {
            // <b>As três saídas do furnish eram mudas</b> — 2026-09-02. O
            // autor viu em jogo: "não nasceu baú de mineiro na entrada da
            // mina, nem lanterna". As duas faltas são a mesma — a lanterna
            // só é posta quando há baú —, e nenhuma delas escrevia linha:
            // chunk fora de memória, ou nenhum dos três vizinhos da boca
            // livre. Duas causas, duas correções diferentes, e o log não
            // sabia escolher entre elas.
            //
            // Ela diz onde, para o autor poder ir olhar o lugar.
            IdleLog.record(
                    mine.colonyId(),
                    CHEST_SUBJECT,
                    IdleReason.NO_TARGET,
                    "the mine mouth at " + mouth.toShortString() + " has no chest and none"
                            + " could be placed — nothing beside it is free, or the chunk"
                            + " is out of memory. The mouth lantern waits on the chest;"
                            + " the gallery torches do not");
        } else {
            IdleLog.clear(mine.colonyId(), CHEST_SUBJECT);
        }

        // <b>A luz da galeria não espera pelo baú</b> — 2026-09-02. Sair
        // aqui quando o baú falha apaga o túnel inteiro, e o gametest
        // theGalleryIsLitWhereItWasAlreadyDug pegou isso na mesma
        // passagem em que a linha nasceu. A lanterna da boca é do
        // MineMouth e depende do baú; as tochas do túnel são outra
        // coisa, e vinte blocos abaixo do chão sem luz é criatura
        // nascendo dentro da mina.

        // E a luz do que já foi cavado — 2026-08-28. Mesma porta e mesma
        // natureza: idempotente, de graça, e chamada a cada passagem em
        // que a mina existe. Ver MineLighting.
        MineLighting.light(world, mine);
    }

    /**
     * A primeira posição desta passagem que valha a picareta.
     *
     * <p>As já abertas são puladas de graça, e as impossíveis contam para
     * a curva da galeria.
     */
    private static Optional<BlockPos> nextCut(ServerWorld world, UUID workerId, Mine mine) {
        findTheFrontier(world, mine);

        for (int look = 0; look < CUTS_PER_SEARCH; look++) {
            BlockPos at = MinecraftTypeAdapter.toBlockPos(mine.nextPosition());

            if (!world.isInBuildLimit(at)) {
                mine.turn();

                continue;
            }

            BlockState state = world.getBlockState(at);

            if (state.isAir() || !state.getFluidState().isEmpty()
                    || MineLighting.isLight(world, at, state)) {

                // Já aberto, ou água e lava. Nenhum dos dois se cava.
                //
                // <b>E a tocha da própria mina</b> — 2026-08-28. Uma
                // posição com luz é espaço aberto, não rocha: sem isto
                // o mineiro cavaria a luz que acabou de pôr, que é o
                // defeito do lampião no primeiro degrau de 08-27 de
                // volta pela porta da frente.
                continue;
            }

            if (state.getHardness(world, at) < 0
                    || BlockProtection.isVillageOriginal(world, at)
                    || BlockProtection.isColonyBuilt(at)) {

                // Bedrock, casa da vila, casa da colônia. A Regra 3 e o
                // impossível, pela mesma porta.
                if (mine.blockedAgain(BLOCKED_BEFORE_TURNING)) {
                    VillageColonyMod.LOGGER.info(
                            "Miner {} hit something it cannot dig - the gallery turns", workerId);
                }

                continue;
            }

            if (MinerWork.approachTo(world, at).equals(at)) {
                // <b>Emparedada: não há vizinho onde um aldeão caiba</b> —
                // 2026-09-02. O approachTo devolve a própria pedra quando
                // não acha lugar de ficar de pé, e o javadoc dele
                // delegava o caso ao guarda de travamento. A sessão das
                // 21:44 mostrou o preço: seis vezes a mesma frase — "the
                // place to stand is the stone itself (no free neighbour
                // to stand on)" —, dois minutos de expediente cada, e
                // zero pedra em dezessete minutos.
                //
                // Impossível de trabalhar é impossível, e vai pela mesma
                // porta do bedrock: conta para a curva, e a galeria
                // contorna o vão em vez de mirar para dentro dele.
                if (mine.blockedAgain(BLOCKED_BEFORE_TURNING)) {
                    VillageColonyMod.LOGGER.info(
                            "Miner {} hit stone with nowhere to stand - the gallery turns",
                            workerId);
                }

                continue;
            }

            mine.digging();

            // O minério da parede vem antes da parede — 2026-08-21. Um
            // túnel de dois blocos de altura mostra o que está colado
            // nele, e passar direto era o mineiro trazendo pedregulho de
            // uma galeria cheia de carvão.
            Optional<BlockPos> ore = OreVein.isOre(state)
                    ? Optional.of(at)
                    : OreVein.beside(world, at);

            if (ore.isEmpty()) {
                return Optional.of(at);
            }

            mine.followVein(MinecraftTypeAdapter.toColonyPos(ore.get()));

            if (!ore.get().equals(at)) {
                // A posição do túnel não foi cavada, e não pode ser
                // perdida: sem isto o cursor passaria por cima dela e o
                // túnel ficaria com um bloco no meio para sempre.
                mine.holdPosition();
            }

            return ore;
        }

        return Optional.empty();
    }

    /**
     * A boca da mina: o fim da vila, na direção em que ela se abre.
     *
     * <p>É a frase do autor — <i>anda até o final da vila</i>. Longe o
     * bastante para a escada não descer sob as casas, perto o bastante
     * para o aldeão ir e voltar dentro do expediente.
     *
     * <p><b>Era uma coluna só, e por isso a mina nunca abriu.</b> Até
     * 2026-08-22 esta busca olhava exatamente um ponto — centro mais
     * quarenta blocos numa direção fixa — e desistia se ele não
     * servisse. Sem alternativa, sem nova tentativa e <b>sem uma linha
     * de log</b>: três sessões de jogo terminaram com {@code 0 mines} no
     * save e mineiros mudos com tarefa aberta.
     *
     * <p>Agora ela tenta <b>vinte e quatro colunas</b>: oito direções —
     * os quatro lados e as quatro diagonais entre eles —, em três
     * distâncias. Eram doze até 2026-08-25, e as quatro do eixo caíram
     * todas na água da mesma vila. A ordem é determinística e começa na intenção do autor
     * — o lado da colônia, na distância cheia —, e só depois encurta.
     * <b>Nunca vai mais longe</b> que a distância pedida: "o fim da
     * vila" é um teto, e a bateria encurta essa distância justamente
     * para o mineiro não comer a pedra da arena vizinha.
     *
     * <p>Pública para o teste de jogo, e é uma leitura sem efeito: nada
     * no mundo muda por perguntar onde a boca caberia.
     */
    public static Optional<BlockPos> mouthOf(
            ServerWorld world, BlockPos center, Side towards) {

        return mouthWithin(world, center, towards, REACHES, LOOK_UP, LOOK_DOWN)
                .or(() -> mouthWithin(world, center, towards, FARTHER, POOR_UP, POOR_DOWN));
    }

    /**
     * As oito direções, nestas distâncias, com esta janela de altura.
     *
     * <p>Chamada duas vezes: a primeira com a boca boa — o fim da vila,
     * no nível dela —, a segunda com a ruim, mais longe e menos exigente
     * quanto à altura. Decisão do autor em 2026-08-26.
     */
    private static Optional<BlockPos> mouthWithin(
            ServerWorld world, BlockPos center, Side towards,
            int[] reaches, int up, int down) {

        for (int part : reaches) {
            int away = Math.max(NEAREST_MOUTH, mineDistance * part / 100);

            int corner = Math.max(NEAREST_MOUTH, away * DIAGONAL / 100);

            Side side = towards;

            for (int turn = 0; turn < 4; turn++) {
                Side next = side.clockwise();

                // O eixo primeiro — é a intenção do autor, "anda até o
                // fim da vila" —, e a diagonal entre ele e o seguinte
                // logo depois. Oito por distância, e não quatro: em 08-25
                // as quatro do eixo caíram todas na água da mesma vila, e
                // a colônia ficou sem pedra por falta de amostra.
                Optional<BlockPos> found = surfaceAt(
                        world, center, side.offsetX() * away, side.offsetZ() * away, up, down);

                if (found.isPresent()) {
                    return found;
                }

                found = surfaceAt(
                        world,
                        center,
                        (side.offsetX() + next.offsetX()) * corner,
                        (side.offsetZ() + next.offsetZ()) * corner,
                        up,
                        down);

                if (found.isPresent()) {
                    return found;
                }

                side = next;
            }
        }

        return Optional.empty();
    }

    /**
     * O chão desta coluna, se ela servir de boca.
     *
     * <p><b>O topo sólido, e não o primeiro sólido.</b> A busca antiga
     * descia do centro mais quatro e devolvia o que encontrasse — numa
     * encosta, isso é o <b>miolo do morro</b>, e a boca nascia enterrada.
     * Aqui um bloco só vale se o que está sobre ele puder ser ocupado.
     *
     * <p><b>Nem debaixo d'água.</b> Água é substituível, então o leito do
     * lago passaria por superfície. A boca de uma mina dentro de um lago
     * é a mina inundada no primeiro degrau.
     *
     * <p>Vazio quando a coluna não serve — e vazio é "tente a próxima",
     * e não "desista", que era o defeito.
     */
    private static Optional<BlockPos> surfaceAt(
            ServerWorld world, BlockPos center, int dx, int dz, int up, int down) {

        int x = center.getX() + dx;
        int z = center.getZ() + dz;

        if (world.getChunkManager().getWorldChunk(x >> 4, z >> 4) == null) {
            // Nunca forçar carregamento de dentro do ciclo — §11.
            return Optional.empty();
        }

        // Do nível da vila para fora, e não do céu para baixo. "O fim da
        // vila" é um lugar no chão dela: pegar o topo sólido da coluna
        // punha a boca em cima do que estivesse acima — numa arena de
        // bateria, o piso do teste vizinho; num mundo, o galho de uma
        // árvore ou a laje de um morro que a vila não ocupa.
        //
        // Desce primeiro: o chão costuma estar abaixo do marco do centro,
        // que é cama ou baú e fica um bloco acima dele.
        for (int step = 0; step <= Math.max(up, down); step++) {
            for (int sign = -1; sign <= 1; sign += 2) {
                int offset = step * sign;

                if (offset > up || offset < -down) {
                    continue;
                }

                int y = center.getY() + offset;

                Optional<BlockPos> found = surfaceOn(world, new BlockPos(x, y, z));

                if (found.isPresent()) {
                    return found;
                }

                if (step == 0) {
                    break;
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Se esta posição é chão de verdade: sólida, com espaço livre em cima.
     *
     * <p><b>Nem debaixo d'água.</b> Água é substituível, então o leito do
     * lago passaria por superfície — e boca de mina dentro de um lago é
     * a mina inundada no primeiro degrau.
     *
     * <p>Vazio também quando o bloco é peça de vila gerada ou construção
     * da colônia: a Regra 3 vale para a boca como vale para o resto.
     */
    private static Optional<BlockPos> surfaceOn(ServerWorld world, BlockPos at) {
        if (!world.getBlockState(at).isSolidBlock(world, at)) {
            return Optional.empty();
        }

        BlockPos above = at.up();

        if (!world.getBlockState(above).isReplaceable()
                || !world.getFluidState(above).isEmpty()) {

            return Optional.empty();
        }

        if (BlockProtection.isVillageOriginal(world, at) || BlockProtection.isColonyBuilt(at)) {
            return Optional.empty();
        }

        return Optional.of(at);
    }

    /**
     * Para que lado esta colônia abre a mina.
     *
     * <p>Sai do identificador da colônia, e é de propósito: duas colônias
     * vizinhas cavam para lados diferentes. Desde que a mina é gravada
     * isto virou redundância — e continua valendo a pena: save perdido, a
     * mina nova abre para o mesmo lado da antiga.
     */
    private static Side sideOf(UUID colonyId) {
        return Side.values()[Math.floorMod(colonyId.hashCode(), Side.values().length)];
    }
}
