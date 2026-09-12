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
     * @param archAlreadyRaised se o arco desta boca já subiu alguma vez.
     *     Verdadeiro impede que ele seja reerguido — o jogador que o
     *     derrubou não o quer de volta. Vem do save, não do mundo: o
     *     mundo não distingue pedra derrubada de pedra que nunca subiu
     * @return o baú da boca e se o arco subiu nesta passagem — dois
     *     sinais, e <b>independentes de propósito</b>. Ver {@link Furnished}
     */
    public static Furnished furnish(
            ServerWorld world, BlockPos mouth, Direction descent, boolean archAlreadyRaised) {
        if (chunkAt(world, mouth) == null) {
            // Nunca forçar carregamento de dentro do ciclo — §11. E nada
            // foi tentado: a boca continua devendo o arco, e a passagem
            // que pegar o chunk carregado é que o ergue.
            return new Furnished(Optional.empty(), false);
        }

        // <b>O arco primeiro, e o baú ao lado dele</b> — 2026-09-11. A
        // ordem era a inversa, e ela nasceu de um defeito real: a
        // primeira versão levantava os pilares no chão e comia o lugar do
        // baú. O conserto de então foi começar os pilares um bloco acima
        // — o que deixou o baú livre para cair <b>debaixo</b> de um
        // deles. Ver besideTheArch.
        //
        // <b>E uma vez só</b> — 2026-09-11, tarde. Quem já ergueu o arco
        // não o confere de novo: a pedra que o jogador derrubou deixa ar,
        // ar é substituível, e o arco renascia a cada passagem. Ver
        // {@code Mine.archRaised}. O baú segue sendo reconferido abaixo,
        // porque o vestígio dele é ele mesmo.
        //
        // <b>E pelo estado do arco, não pela intenção de erguê-lo</b> —
        // 2026-09-12, e levou três voltas do Gauntlet para chegar aqui.
        // A versão anterior marcava por {@code !archAlreadyRaised}, antes
        // de saber de nada; a do meio, pela substituição ter dado certo —
        // e pedra natural nunca passa por substituição, então boca cavada
        // em rocha não marcava e o mod repunha o que o jogador cavasse ali.
        //
        // Uma posição firme basta, e firme por obra de quem quer que seja.
        // Arco parcial é o que a Regra 3 manda fazer onde ele não cabe
        // inteiro, e marcá-lo é certo: ele existe. O único caso que não
        // marca é não haver arco nenhum depois da tentativa.
        boolean archRaisedNow = !archAlreadyRaised && raiseArch(world, mouth, descent);

        Optional<BlockPos> chest = chestAt(world, mouth);

        if (chest.isEmpty()) {
            chest = placeChest(world, mouth, descent);
        }

        return new Furnished(chest, archRaisedNow);
    }

    /**
     * O que uma passagem de {@link #furnish} fez — 2026-09-12.
     *
     * <p><b>Dois sinais, e separados de propósito.</b> A primeira versão
     * devolvia só o baú, e quem chamava marcava o arco quando o baú
     * aparecia: duas coisas sem relação amarradas por conveniência. O
     * {@code gauntlet-verifier} provou o preço disso com um teste que
     * bloqueou todos os vizinhos da boca — sem lugar para baú, o arco
     * nunca era marcado e <b>voltava para sempre</b>, que é exatamente o
     * defeito que esta correção veio fechar.
     *
     * <p>O mesmo erro tinha uma segunda cara: arco que nasce incompleto
     * pela Regra 3 — pilar que teria de derrubar casa não nasce — era
     * marcado como pronto ao primeiro baú e nunca mais completado.
     *
     * <p><b>E o sinal fala do estado, não da intenção nem da autoria.</b>
     * Foram mais duas voltas para isso: a primeira marcava por <i>"esta
     * passagem tentou"</i>, e a seguinte por <i>"a substituição deu
     * certo"</i> — que exclui pedra natural, e fazia o mod repor a rocha
     * lateral que o jogador cavasse numa boca aberta dentro de um morro.
     *
     * <p>Um sinalizador de <i>"já fiz"</i> só vale se perguntar <b>ao
     * mundo</b> o que está feito. Onde a pergunta mudou três vezes, a
     * resposta sempre foi a mesma coisa mal formulada.
     *
     * @param chest onde está o baú da boca, ou vazio quando nenhum
     *     vizinho serve — encosta, água, ou chunk fora de memória
     * @param archRaisedNow se ao menos uma posição do arco <b>está firme</b>
     *     depois desta passagem — o pedregulho que acabou de entrar, ou o
     *     bloco que já estava lá. É o sinal de que não se ergue de novo, e
     *     ele fala do arco e de mais nada: verdadeiro mesmo que o arco
     *     tenha saído incompleto pela Regra 3, e mesmo que o baú não tenha
     *     achado lugar. Falso quando não há arco nenhum a preservar —
     *     chunk fora de memória, ou as nove posições ainda vazias depois da
     *     tentativa —, e aí a boca continua devendo o arco
     */
    public record Furnished(Optional<BlockPos> chest, boolean archRaisedNow) {
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
    private static boolean raiseArch(ServerWorld world, BlockPos mouth, Direction descent) {
        Direction side = descent.rotateYClockwise();

        boolean anyStone = false;

        // <b>Começa um bloco acima do chão</b> — e é o que faz o arco
        // conviver com a mobília. O baú e a lanterna da boca moram nos
        // vizinhos do nível do chão, e um pilar ali disputaria o lugar
        // deles: a primeira versão deixou a mina sem baú. Daqui para
        // cima não há disputa, e o arco emoldura a entrada na altura em
        // que ela é vista.
        for (int up = 1; up < ARCH_HIGH; up++) {
            anyStone |= layStone(world, mouth.offset(side).up(up));
            anyStone |= layStone(world, mouth.offset(side.getOpposite()).up(up));
        }

        anyStone |= layStone(world, mouth.offset(side).up(ARCH_HIGH));
        anyStone |= layStone(world, mouth.up(ARCH_HIGH));
        anyStone |= layStone(world, mouth.offset(side.getOpposite()).up(ARCH_HIGH));

        // <b>A lanterna fica aqui dentro, e é decisão</b> — 2026-09-12. O
        // {@code gauntlet-verifier} pediu para tirá-la deste portão,
        // porque posição de lanterna bloqueada na passagem em que o arco
        // sobe deixa a boca sem luz para sempre. O achado é real; a
        // correção proposta, não: acender a cada passagem faz a lanterna
        // <b>voltar quando o jogador a quebra</b>, que é literalmente o
        // defeito que o autor relatou — <i>"deve permitir que seja
        // destruído normalmente e não reaparecendo infinitamente"</i>. A
        // lanterna é peça do portal tanto quanto a pedra.
        //
        // {@code lightTheTop} é idempotente quanto a <b>não duplicar</b>,
        // e isso não é o mesmo que não ressuscitar — a distinção que esta
        // sessão inteira existe para aprender. Entre uma boca sem luz num
        // caso raro e uma lanterna que o jogador não consegue remover, o
        // pedido do autor decide.
        lightTheTop(world, mouth);

        return anyStone;
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

    /**
     * Uma pedra do arco, se o lugar aceitar — e se ele está resolvido.
     *
     * <p><b>Devolve "há pedra aqui", e não "eu a pus"</b> — 2026-09-12,
     * terceira volta. A diferença custou duas iterações do Gauntlet. O
     * critério anterior era a substituição ter dado certo, e pedra
     * <b>natural</b> ocupando a posição nunca passa por substituição: boca
     * cavada em rocha intacta — o caso comum, porque o {@code MineSite} só
     * valida a coluna da própria boca — não marcava nada, e o mod repunha
     * com pedregulho a pedra lateral que o jogador cavasse. O defeito
     * relatado em jogo, de volta com outra roupa.
     *
     * <p>Pedra natural já faz o papel do pilar. Quem pergunta quer saber
     * se a posição está resolvida, não quem a resolveu.
     *
     * @return se esta posição do arco tem algo firme — o pedregulho que
     *     acabou de entrar, a rocha que já estava lá, ou o bloco que o
     *     jogador pôs. O {@code raiseArch} soma estes retornos, e é a soma
     *     que decide se há arco — ver {@link Furnished#archRaisedNow()}
     */
    private static boolean layStone(ServerWorld world, BlockPos at) {
        if (world.getBlockState(at).isReplaceable()) {
            world.setBlockState(at, Blocks.COBBLESTONE.getDefaultState(), Block.NOTIFY_ALL);
        }

        // Relido do mundo, e de propósito: é a única resposta que não
        // depende de quem pôs o bloco. Substituível ainda aqui significa
        // que a posição segue vazia — e aí não há arco a preservar.
        return !world.getBlockState(at).isReplaceable();
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
