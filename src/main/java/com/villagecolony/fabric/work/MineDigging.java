package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.core.coordination.IdleReason;
import com.villagecolony.fabric.integration.BlockProtection;
import com.villagecolony.fabric.integration.MineMouth;
import com.villagecolony.fabric.integration.OreVein;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
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

    /**
     * A distância em vigor. É {@link #MINE_DISTANCE}, menos nos testes.
     *
     * <p>A bateria roda arenas lado a lado no mesmo mundo, e uma mina
     * aberta a quarenta blocos sai da arena dela e cava o cenário do
     * teste vizinho. Um teste que destrói o cenário de outro é pior que
     * um teste que não existe.
     */
    private static int mineDistance = MINE_DISTANCE;

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
            return Optional.empty();
        }

        return followingTheVein(world, mine.get())
                .or(() -> nextCut(world, workerId, mine.get()));
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

        mine.followVein(MinecraftTypeAdapter.toColonyPos(more.get()));

        return more;
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
            furnishMouth(world, known.get());

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
                            + " can hold a mine mouth — tried 4 sides at 3 distances");

            return Optional.empty();
        }

        IdleLog.clear(colonyId, MOUTH_SUBJECT);

        Mine opened = VillageColonyMod.MINES.open(
                colonyId,
                MineShaft.from(MinecraftTypeAdapter.toColonyPos(mouth.get()), descent));

        VillageColonyMod.LOGGER.info(
                "Miner {} opens a mine at {} - down {} then {} more",
                workerId,
                mouth.get(),
                MineShaft.DESCENT,
                MineShaft.DESCENT);

        // A Regra 30: onde ele decide começar a cavar nascem a lanterna
        // e o baú da mina.
        furnishMouth(world, opened);

        return Optional.of(opened);
    }

    /**
     * A lanterna e o baú da boca — a Regra 30, 2026-08-22.
     *
     * <p>Chamada também para mina já conhecida, e de propósito: mina de
     * save antigo não passou pela regra, e boca em chunk descarregado
     * não pôde ser mobiliada na primeira tentativa. {@code MineMouth} não
     * faz nada quando o baú já está lá.
     */
    private static void furnishMouth(ServerWorld world, Mine mine) {
        MineMouth.furnish(world, MinecraftTypeAdapter.toBlockPos(mine.shaft().entry()));
    }

    /**
     * A primeira posição desta passagem que valha a picareta.
     *
     * <p>As já abertas são puladas de graça, e as impossíveis contam para
     * a curva da galeria.
     */
    private static Optional<BlockPos> nextCut(ServerWorld world, UUID workerId, Mine mine) {
        for (int look = 0; look < CUTS_PER_SEARCH; look++) {
            BlockPos at = MinecraftTypeAdapter.toBlockPos(mine.nextPosition());

            if (!world.isInBuildLimit(at)) {
                mine.turn();

                continue;
            }

            BlockState state = world.getBlockState(at);

            if (state.isAir() || !state.getFluidState().isEmpty()) {
                // Já aberto, ou água e lava. Nenhum dos dois se cava.
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
     * <p>Agora ela tenta <b>doze colunas</b>: quatro lados, três
     * distâncias. A ordem é determinística e começa na intenção do autor
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

        for (int part : REACHES) {
            int away = Math.max(NEAREST_MOUTH, mineDistance * part / 100);

            Side side = towards;

            for (int turn = 0; turn < 4; turn++) {
                Optional<BlockPos> found = surfaceAt(world, center, side, away);

                if (found.isPresent()) {
                    return found;
                }

                side = side.clockwise();
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
            ServerWorld world, BlockPos center, Side side, int away) {

        int x = center.getX() + side.offsetX() * away;
        int z = center.getZ() + side.offsetZ() * away;

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
        for (int step = 0; step <= Math.max(LOOK_UP, LOOK_DOWN); step++) {
            for (int sign = -1; sign <= 1; sign += 2) {
                int offset = step * sign;

                if (offset > LOOK_UP || offset < -LOOK_DOWN) {
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
