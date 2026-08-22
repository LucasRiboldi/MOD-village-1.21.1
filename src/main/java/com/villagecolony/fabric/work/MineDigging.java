package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
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
            return Optional.empty();
        }

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
     */
    private static Optional<BlockPos> mouthOf(
            ServerWorld world, BlockPos center, Side towards) {

        int x = center.getX() + towards.offsetX() * mineDistance;
        int z = center.getZ() + towards.offsetZ() * mineDistance;

        for (int y = center.getY() + 4; y >= center.getY() - 8; y--) {
            BlockPos at = new BlockPos(x, y, z);

            if (world.getBlockState(at).isAir()) {
                continue;
            }

            if (BlockProtection.isVillageOriginal(world, at)
                    || BlockProtection.isColonyBuilt(at)) {

                return Optional.empty();
            }

            return Optional.of(at);
        }

        return Optional.empty();
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
