package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.worker.model.ProfessionType;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A boca da mina, mobiliada — a Regra 30.
 *
 * <p><b>Regra do autor, 2026-08-22:</b> onde o mineiro decide começar a
 * cavar aparecem duas coisas — uma <b>lanterna</b> de um lado do buraco
 * e um <b>baú marcado como do mineiro</b> do outro. Esse baú guarda só
 * minério, e quando lotar o minério passa a ir para o baú principal.
 *
 * <p><b>Não custa material</b>, e é a mesma decisão que a mina inteira
 * já carrega: a escada, as duas salas e a galeria são <b>cavadas</b>, e
 * ninguém paga por elas. É também o precedente do {@code ChestPlacer},
 * que põe baú ao lado da cama sem tirar tábua de baú nenhum. Cobrar
 * material aqui faria a mina não abrir até a colônia ter lanterna — e
 * lanterna pede ferro, que vem da mina.
 *
 * <p><b>Sem estado novo em disco.</b> Onde está o baú é lido do mundo:
 * dos vizinhos da boca, o que for um baú é ele. Gravar a posição seria
 * uma segunda verdade que o jogador desfaz com uma picareta.
 */
public final class MineMouth {

    /** Quantos blocos abaixo da boca ainda contam como "ao lado dela". */
    private static final int DROP = 1;

    /**
     * Até quantos blocos ao redor da boca a mobília dela pode estar.
     *
     * <p>Dois desde 2026-09-11, quando o baú saiu de debaixo do pilar e
     * foi para o lado do arco. Um continua valendo porque as minas
     * abertas antes dessa data têm o baú encostado na boca, e elas não
     * podem perder o que já guardam.
     */
    private static final int REACH = 2;

    private MineMouth() {
    }

    /**
     * Põe a lanterna e o baú, se ainda não estiverem lá.
     *
     * <p>Idempotente, e as <b>duas</b> peças o são — 2026-08-27. Até
     * aqui só o baú era conferido: quem já tinha baú voltava na primeira
     * linha, e a lanterna nunca chegava a ser tentada. O autor achou o
     * buraco em jogo, e a frase dele foi <i>"faltou o lampião na entrada
     * da mina, eu mesmo botei"</i>.
     *
     * <p>Duas bocas caíam nesse caso, e as duas são comuns: a mina que
     * volta de um save anterior à Regra 30, e a boca em que a primeira
     * tentativa achou lugar para o baú e não para a lanterna — encosta,
     * água, borda de chunk. Nas duas a segunda chance não existia.
     *
     * <p>Chamada a cada passagem em que a mina existe, e silenciosa em
     * todas menos naquelas em que põe alguma coisa.
     *
     * @param descent para que lado a escada desce. A mobília fica fora
     *     dessa coluna — ver {@link #freeSpotNear}
     * @return onde está o baú da boca, ou vazio quando nenhum vizinho
     *     serve — encosta, água, ou chunk fora de memória
     */
    public static Optional<BlockPos> furnish(
            ServerWorld world, BlockPos mouth, Direction descent) {
        if (chunkAt(world, mouth) == null) {
            // Nunca forçar carregamento de dentro do ciclo — §11.
            return Optional.empty();
        }

        // <b>O arco primeiro, e o baú ao lado dele</b> — 2026-09-11. A
        // ordem era a inversa, e ela nasceu de um defeito real: a
        // primeira versão levantava os pilares no chão e comia o lugar do
        // baú. O conserto de então foi começar os pilares um bloco acima
        // — o que deixou o baú livre para cair <b>debaixo</b> de um
        // deles. Ver besideTheArch.
        raiseArch(world, mouth, descent);

        Optional<BlockPos> chest = chestAt(world, mouth);

        if (chest.isEmpty()) {
            chest = placeChest(world, mouth, descent);
        }

        return chest;
    }

    /**
     * A altura do arco — quatro, por decisão do autor em 2026-09-11.
     *
     * <p>Os pilares sobem até {@code ARCH_HIGH - 1} e a verga fecha em
     * {@code ARCH_HIGH}, então a passagem por baixo tem quatro blocos de
     * vão: a boca e os três acima dela. Eram três, e a entrada ficava da
     * altura de quem passa.
     */
    private static final int ARCH_HIGH = 4;

    /**
     * O arco de pedra da entrada — decisão do autor, 2026-09-05:
     * <i>"colocar um arco de pedra com lanterna na entrada da mina"</i>.
     *
     * <p>Dois pilares nos lados da boca e uma verga ligando os dois por
     * cima. Os lados são os perpendiculares ao rumo da descida, que é o
     * que emoldura a entrada em vez de tapá-la.
     *
     * <p><b>A lanterna fica em cima da verga</b> — decisão do autor em
     * 2026-09-11, e ela é a <b>única</b> da boca. Havia duas: uma
     * pendurada sob a verga, dentro do vão, e outra no chão ao lado do
     * buraco, da primeira versão da Regra 30. A de dentro roubava altura
     * da passagem e a de fora disputava o lugar do baú. Em cima do arco
     * ela ilumina de mais longe e não atrapalha ninguém.
     *
     * <p><b>Pedregulho, e não a paleta da vila.</b> A paleta mora em
     * {@code fabric.work} e esta classe é {@code fabric.integration} —
     * puxá-la daqui inverteria a dependência que o
     * {@code DependencyRuleTest} guarda. Pedregulho é o que a própria
     * mina produz, e lê como pedra em qualquer bioma.
     *
     * <p><b>Só onde cabe.</b> Cada bloco é posto apenas sobre o que é
     * substituível — a Regra 3 vale para o arco como vale para o resto:
     * pilar que teria de derrubar a casa de alguém simplesmente não
     * nasce, e o arco sai incompleto em vez de sair por cima.
     *
     * <p>Idempotente: com o arco lá, isto não faz nada.
     */
    private static void raiseArch(ServerWorld world, BlockPos mouth, Direction descent) {
        Direction side = descent.rotateYClockwise();

        // <b>Começa um bloco acima do chão</b> — e é o que faz o arco
        // conviver com a mobília. O baú e a lanterna da boca moram nos
        // vizinhos do nível do chão, e um pilar ali disputaria o lugar
        // deles: a primeira versão deixou a mina sem baú. Daqui para
        // cima não há disputa, e o arco emoldura a entrada na altura em
        // que ela é vista.
        for (int up = 1; up < ARCH_HIGH; up++) {
            layStone(world, mouth.offset(side).up(up));
            layStone(world, mouth.offset(side.getOpposite()).up(up));
        }

        layStone(world, mouth.offset(side).up(ARCH_HIGH));
        layStone(world, mouth.up(ARCH_HIGH));
        layStone(world, mouth.offset(side.getOpposite()).up(ARCH_HIGH));

        lightTheTop(world, mouth);
    }

    /**
     * A lanterna, de pé sobre o meio da verga.
     *
     * <p><b>Só se a verga existe.</b> O arco nasce incompleto onde a
     * Regra 3 o impede — pilar que teria de derrubar casa não nasce —, e
     * uma lanterna posta assim mesmo ficaria boiando no ar sobre o
     * buraco. Ela pergunta pelo bloco de baixo antes de existir.
     *
     * <p>Idempotente pelo mesmo teste de sempre: lugar que não é
     * substituível já tem alguma coisa, e o mod não discorda do dono do
     * mundo — inclusive quando a coisa é a lanterna da passagem anterior.
     */
    private static void lightTheTop(ServerWorld world, BlockPos mouth) {
        BlockPos lintel = mouth.up(ARCH_HIGH);
        BlockPos lamp = lintel.up();

        if (!world.getBlockState(lintel).isSolidBlock(world, lintel)) {
            return;
        }

        if (world.getBlockState(lamp).isReplaceable()) {
            world.setBlockState(lamp, Blocks.LANTERN.getDefaultState(), Block.NOTIFY_ALL);
        }
    }

    /** Uma pedra do arco, se o lugar aceitar. */
    private static void layStone(ServerWorld world, BlockPos at) {
        if (world.getBlockState(at).isReplaceable()) {
            world.setBlockState(at, Blocks.COBBLESTONE.getDefaultState(), Block.NOTIFY_ALL);
        }
    }

    /** O baú da boca, recém-posto e marcado como do mineiro. */
    private static Optional<BlockPos> placeChest(
            ServerWorld world, BlockPos mouth, Direction descent) {

        Optional<BlockPos> spot = besideTheArch(world, mouth, descent);

        if (spot.isEmpty()) {
            return Optional.empty();
        }

        world.setBlockState(spot.get(), Blocks.CHEST.getDefaultState(), Block.NOTIFY_ALL);

        ChestMarker.markAt(world, spot.get(), ProfessionType.MINER);

        VillageColonyMod.LOGGER.info(
                "Mine mouth at {} got its miner chest at {}",
                mouth.toShortString(),
                spot.get().toShortString());

        return spot;
    }

    /**
     * O baú da boca desta mina, se ele existe.
     *
     * <p>Lido do mundo, e por isso sobrevive ao servidor parar sem
     * ocupar um campo no save.
     */
    public static Optional<BlockPos> chestAt(ServerWorld world, BlockPos mouth) {
        if (chunkAt(world, mouth) == null) {
            return Optional.empty();
        }

        // <b>Um e dois blocos, e os dois importam</b> — 2026-09-11. O baú
        // passou a nascer a dois, ao lado do arco; procurar só a dois
        // deixaria sem baú toda mina aberta antes desta data, e procurar
        // só a um faria o furnish pôr um baú novo a cada passagem por
        // não achar o que ele mesmo acabou de pôr.
        for (int out = 1; out <= REACH; out++) {
            for (int drop = 0; drop <= DROP; drop++) {
                for (Direction side : Direction.Type.HORIZONTAL) {
                    BlockPos at = mouth.offset(side, out).down(drop);

                    if (world.getBlockState(at).isOf(Blocks.CHEST)) {
                        return Optional.of(at);
                    }
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Um vizinho da boca onde caiba coisa nova, fora da escada.
     *
     * <p><b>Fora da escada — visto no log de 2026-08-27, 21:39.</b> O
     * primeiro degrau é {@code mouth.offset(descent)}, na mesma altura da
     * boca, e este método o tratava como qualquer outro vizinho. O
     * lampião foi parar exatamente ali:
     *
     * <pre>
     * Mine mouth at 732, 63, 898 got its lantern at 731, 63, 898
     * miners: 68f4dcde digging Lanterna at 731, 63, 898, 48 blocks away
     * </pre>
     *
     * <p>O mineiro recebeu ordem de cavar a própria lanterna. E desde que
     * a mobília virou idempotente, o mod a reporia na passagem seguinte:
     * põe, o mineiro quebra, põe de novo. Sobram três lados, e três
     * bastam para duas peças.
     *
     * <p>A coluna inteira sai, e não só o degrau: um bloco abaixo dele é
     * onde o aldeão põe os pés ao descer.
     *
     * @param taken o lugar que a peça anterior ocupou, para a seguinte
     *     não disputar com ela; {@code null} na primeira
     * @param descent para que lado a escada desce
     */
    /**
     * Onde o baú cabe ao lado do arco — decisão do autor, 2026-09-11:
     * <i>"ao seu lado o baú que nasce com o arco"</i>.
     *
     * <p><b>Fora da pegada do arco, e isso é a correção de um defeito de
     * jogo.</b> A busca antiga olhava os quatro vizinhos da boca, e dois
     * deles são exatamente onde os pilares sobem. O baú que caísse ali
     * ficava com pedregulho em cima — e <b>baú com bloco sólido em cima
     * não abre</b>, é regra do próprio Minecraft. O jogador via o baú do
     * mineiro na entrada e não conseguia olhar dentro dele.
     *
     * <p>O conserto do arco de 2026-09-05 começou os pilares um bloco
     * acima do chão justamente para não <b>substituir</b> a mobília, e
     * resolveu metade: o baú deixou de ser apagado e passou a ser
     * tapado. Esta busca fecha a outra metade, saindo dois blocos para o
     * lado — encostado no arco, e não debaixo dele.
     *
     * <p>Os dois pés, na ordem, e a coluna da descida nunca: a escada
     * desce por ali e o baú taparia a entrada.
     */
    private static Optional<BlockPos> besideTheArch(
            ServerWorld world, BlockPos mouth, Direction descent) {

        Direction side = descent.rotateYClockwise();

        for (Direction leg : List.of(side, side.getOpposite())) {
            for (int drop = 0; drop <= DROP; drop++) {
                BlockPos at = mouth.offset(leg, 2).down(drop);

                if (isGoodSpot(world, at) && opensFrom(world, at)) {
                    return Optional.of(at);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Se um baú posto aqui poderia ser aberto.
     *
     * <p>A regra é do jogo, e não do mod: {@code ChestBlock} recusa abrir
     * com bloco sólido inteiro em cima. Perguntar antes é mais barato que
     * descobrir em sessão — foi assim que o baú debaixo do pilar passou
     * despercebido de 2026-09-05 a 09-11.
     */
    private static boolean opensFrom(ServerWorld world, BlockPos at) {
        BlockPos above = at.up();

        return !world.getBlockState(above).isSolidBlock(world, above);
    }

    /** Ar sobre chão sólido: onde uma peça da boca pode ficar. */
    private static boolean isGoodSpot(ServerWorld world, BlockPos at) {
        return world.getBlockState(at).isReplaceable()
                && world.getBlockState(at.down()).isSolidBlock(world, at.down());
    }

    private static Object chunkAt(ServerWorld world, BlockPos at) {
        return world.getChunkManager().getWorldChunk(at.getX() >> 4, at.getZ() >> 4);
    }
}
