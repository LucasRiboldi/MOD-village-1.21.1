package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineArm;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.construction.service.MineRecovery;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.core.coordination.IdleReason;
import com.villagecolony.fabric.integration.BlockProtection;
import com.villagecolony.fabric.integration.MineFlooding;
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
    static final int CUTS_PER_SEARCH = 64;

    /** Recusas seguidas antes de a galeria virar. */
    static final int BLOCKED_BEFORE_TURNING = 8;









    /** O assunto do registrador para a boca que não se acha — 2026-08-22. */
    static final String MOUTH_SUBJECT = "miner mine mouth";

    /** O assunto do baú da boca, que não se acha onde pôr — 2026-09-02. */
    static final String CHEST_SUBJECT = "miner mouth chest";

    /** O do ramal que acabou, e da mina que não tem para onde descer. */
    static final String ARM_SUBJECT = "miner branch";

    /** E o da busca que olhou as 64 posições e não achou pedra. */
    static final String CUT_SUBJECT = "miner cut";

    /** E o da pedra de superfície, que é a alternativa a ela — 2026-08-25. */
    static final String SURFACE_SUBJECT = "miner surface stone";

    /**
     * A que distância se raspa pedra exposta quando não há mina.
     *
     * <p>O mesmo raio da areia, e pelo mesmo motivo: é o que um aldeão
     * percorre e volta dentro do expediente. Mais que isso e ele passa o
     * dia andando.
     */
    static final int SURFACE_RADIUS = 48;


    /** O raio de superfície em vigor. É {@link #SURFACE_RADIUS}, menos nos testes. */
    static int surfaceRadius = SURFACE_RADIUS;

    private MineDigging() {
    }

    /** Aproxima a boca da mina. Só os testes precisam disso. */
    public static void shortenMineDistanceTo(int blocks) {
        MineSite.shortenTo(blocks);
    }

    /** Devolve a distância ao valor de jogo. */
    public static void restoreMineDistance() {
        MineSite.restore();
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
            // exposto em volta — ver MineVein.exposedStone.
            return MineVein.exposedStone(world, workerId, colonyId, center);
        }

        IdleLog.clear(colonyId, SURFACE_SUBJECT);

        OptionalInt claimed = MineClaims.claimArm(
                colonyId,
                workerId,
                mine.get().branchesOpenNow(),
                index -> !mine.get().arm(index).isDone());

        if (claimed.isEmpty()) {
            // <b>Nenhum ramal aceita picareta agora</b>: ou os abertos
            // estão todos com outros mineiros, ou todos acabaram. O
            // segundo caso é o que faz a mina descer, e é aqui que ele se
            // decide — antes ficava no ramo do {@code isDone} logo
            // abaixo, que a reserva nunca deixava alcançar quando o ramal
            // acabado continuava sendo entregue.
            Mine.LevelAdvance advance = mine.get().advanceIfEveryOpenArmIsDone();

            if (advance == Mine.LevelAdvance.DEEPENED) {
                VillageColonyMod.LOGGER.info(
                        "Mine {} finished every branch and went one level deeper",
                        colonyId);

                IdleLog.clear(colonyId, ARM_SUBJECT);

                return Optional.empty();
            }

            if (advance == Mine.LevelAdvance.EXHAUSTED) {
                MineTrouble.abandonAtBottom(world, colonyId, mine.get(), center);
                return Optional.empty();
            }

            // <b>A volta que não deu pedra custa</b> — E45, 2026-09-16.
            // Chegar aqui é a mina ter servido o ramal, ele ter fechado, e
            // nada ter descido: se isso se repete, o desenho é que não se
            // cava. Só conta quando não há mineiro trabalhando — com
            // digger ocupado a volta ainda pode render, e cobrá-la seria
            // punir a mina cheia.
            if (MineClaims.diggersIn(colonyId) == 0
                    && mine.get().turnedWithoutAPickaxe()) {

                MineTrouble.rerouteOrBlameTheMouth(world, colonyId, mine.get(), center);

                return Optional.empty();
            }

            IdleLog.record(
                    colonyId,
                    ARM_SUBJECT,
                    IdleReason.NO_TARGET,
                    "every open branch is taken or finished — "
                            + MineClaims.diggersIn(colonyId) + " digger(s) in "
                            + mine.get().branchesOpenNow() + " open branch(es)");

            // <b>A escada é de um só</b> — 2026-08-28. O cursor da
            // galeria mora no Mine e é um; dois mineiros perguntando na
            // mesma passagem recebiam a mesma posição, andavam para o
            // mesmo bloco, e escreviam "could not reach the stone" no
            // mesmo tique. Esse aviso recua o cursor, e ele recuava duas
            // vezes por um bloco.
            //
            // Quem não achou ramal livre fica sem alvo, e não em alvo
            // errado: ele volta a perguntar na passagem seguinte, e herda
            // um ramal no ciclo em que algum dono largar o trabalho. Ver
            // MineClaims.
            //
            // <b>São quatro ramais desde 2026-09-04</b>, e não uma escada
            // só: o "waiting for the shaft" que a sessão daquele dia
            // mostrou por trinta e sete minutos passa a valer só a partir
            // do quinto mineiro da colônia.
            return Optional.empty();
        }

        MineArm arm = mine.get().arm(claimed.getAsInt());

        IdleLog.clear(colonyId, ARM_SUBJECT);

        Optional<BlockPos> found = MineVein.followingTheVein(world, arm)
                .or(() -> MineCuts.nextCut(world, workerId, mine.get(), arm));

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

            // <b>E este também</b> — 2026-09-05, mesmo motivo. Sessenta e
            // quatro posições olhadas e nenhuma que valha a picareta é
            // uma frase, e a falta dela era silêncio idêntico ao do ramal
            // acabado.
            IdleLog.record(
                    colonyId,
                    CUT_SUBJECT,
                    IdleReason.NO_TARGET,
                    "branch " + claimed.getAsInt() + " gave no stone in "
                            + CUTS_PER_SEARCH + " positions from cursor " + arm.cut());
        } else {
            IdleLog.clear(colonyId, CUT_SUBJECT);
        }

        return found;
    }


    /**
     * O ramal em que este mineiro está cavando — 2026-09-04.
     *
     * <p>Existe para quem precisa da <b>ordem de cavar</b> dele e não da
     * mina inteira: a perna do {@code MinerReach} anda pelo corredor do
     * ramal, e com quatro rumos abertos o corredor de um não serve de
     * caminho para o outro.
     *
     * <p><b>Não reserva ramal a quem não tem.</b> O
     * {@code MineClaims.claimArm} reserva na mesma passagem em que
     * responde, e quem só quer saber por onde o aldeão volta não pode
     * pagar esse efeito: perguntar tiraria a última frente livre de quem
     * ia cavar nela.
     */
    public static Optional<MineArm> armOf(UUID colonyId, UUID workerId) {
        Optional<Mine> mine = VillageColonyMod.MINES.of(colonyId);

        if (mine.isEmpty()) {
            return Optional.empty();
        }

        OptionalInt held = MineClaims.armAlreadyHeld(colonyId, workerId);

        return held.isPresent()
                ? Optional.of(mine.get().arm(held.getAsInt()))
                : Optional.empty();
    }

    /**
     * Por qual corredor este aldeão anda agora — 2026-09-05.
     *
     * <p><b>O ramal reservado diz onde ele vai cavar; este diz por onde
     * ele caminha</b>, e os dois deixaram de ser o mesmo no dia em que a
     * mina ganhou quatro rumos. O passo do {@code MinerReach} percorre a
     * ordem de cavar de um ramal só, e ela não é caminho para os outros
     * três: pedir um passo pelo ramal reservado, com o aldeão parado
     * dentro de outro, devolve nulo em todo tique — e o mineiro fica no
     * fundo do túnel errado até o guarda devolver a tarefa.
     *
     * <p>Dois caminhos levam a esse descasamento, e os dois apareceram na
     * sessão de 2026-09-04:
     *
     * <ul>
     *   <li>a reserva muda de ramal. Uma passagem sem pedra chama
     *       {@code MineClaims.release}, e o {@code claimArm} seguinte dá
     *       o primeiro ramal livre, que pode ser outro — enquanto o
     *       aldeão continua onde estava;</li>
     *   <li>a tarefa de <b>areia</b> não reserva ramal nenhum. Ali o
     *       reservado é vazio, e o corredor por onde ele precisa sair
     *       existe do mesmo jeito.</li>
     * </ul>
     *
     * <p>Por isso a resposta é <b>onde ele está</b>, e o reservado só
     * responde quando nenhum corredor o contém — que é o caso de quem
     * está na superfície, e ali quem conduz é a navegação do jogo.
     */
    public static Optional<MineArm> armToWalk(
            UUID colonyId, UUID workerId, BlockPos villager) {

        Optional<Mine> mine = VillageColonyMod.MINES.of(colonyId);

        if (mine.isEmpty()) {
            return Optional.empty();
        }

        Optional<MineArm> held = armOf(colonyId, workerId);

        // O caso comum, e é o primeiro de propósito: ele está cavando no
        // ramal que reservou. Perguntar aos outros três seria pagar a
        // varredura para chegar na mesma resposta.
        if (held.isPresent() && MinerLeg.isOnCorridorOf(villager, held.get())) {
            return held;
        }

        for (MineArm arm : mine.get().arms()) {
            if (MinerLeg.isOnCorridorOf(villager, arm)) {
                return Optional.of(arm);
            }
        }

        return held;
    }

    /**
     * Se avançar a frente deste corredor aproxima do alvo — 2026-09-05.
     *
     * <p>Dono do alvo é o ramal <b>reservado</b>: foi dele que a posição
     * saiu, em {@link #nextTarget}. Corredor é onde o aldeão está. Quando
     * são o mesmo, cavar para a frente é ir na direção certa; quando não
     * são, o alvo está do outro lado do poço e a única ligação entre os
     * dois ramais é a boca.
     *
     * <p>Recebe o corredor já resolvido em vez de calculá-lo: o
     * {@link #armToWalk} varre as posições dos quatro ramais, e chamá-lo
     * duas vezes por tique por mineiro seria pagar a varredura em dobro
     * para responder à mesma pergunta.
     *
     * <p>Sem ramal reservado — a tarefa de areia — a resposta é
     * <b>não</b>, e ela é a certa pelo mesmo caminho: quem cava areia não
     * tem frente a avançar, e o que ele precisa é sair.
     */
    public static boolean leadsToTheTarget(
            UUID colonyId, UUID workerId, Optional<MineArm> corridor) {

        Optional<MineArm> owner = armOf(colonyId, workerId);

        return owner.isPresent() && corridor.isPresent() && owner.get() == corridor.get();
    }

    /**
     * A mina desta colônia, aberta agora se ainda não existir.
     *
     * <p>A mina é da colônia, e não deste mineiro: o segundo a descer
     * continua a mesma escada, e a que o save trouxe já vem com a
     * fronteira de ontem.
     */
    static Optional<Mine> mineOf(
            ServerWorld world, UUID workerId, UUID colonyId, BlockPos center) {

        Optional<Mine> known = VillageColonyMod.MINES.of(colonyId);

        if (known.isPresent()) {
            MineFurnishing.lightMine(world, known.get());

            return known;
        }

        Side descent = MineCuts.sideOf(colonyId);

        Optional<BlockPos> mouth = MineSite.mouthOf(world, center, descent);

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
                    "no column within " + MineSite.distance() + " blocks of " + center.toShortString()
                            + " can hold a mine mouth — tried 8 directions at 5 distances,"
                            + " the last two settling for a poor one");

            return Optional.empty();
        }

        IdleLog.clear(colonyId, MOUTH_SUBJECT);

        Mine opened = VillageColonyMod.MINES.open(
                colonyId,
                MineShaft.from(MinecraftTypeAdapter.toColonyPos(mouth.get()), descent));

        VillageColonyMod.LOGGER.info(
                "Miner {} opens a mine at {} - a {} step spiral then four {} step branches",
                workerId,
                mouth.get().toShortString(),
                MineShaft.DESCENT,
                MineShaft.ARM_STAIRS);

        // A Regra 30: onde ele decide começar a cavar nascem a lanterna
        // e o baú da mina.
        MineFurnishing.furnishAndLight(world, opened);

        return Optional.of(opened);
    }

}
