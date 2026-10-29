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
     * <p>Chamada ao abrir uma boca nova, e silenciosa quando o arco já
     * existe no terreno.
     *
     * @param descent para que lado a escada desce. A mobília fica fora
     *     dessa coluna — ver {@code freeSpotNear}
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
        // deles. (O baú da boca saiu depois, em c0ccbe3.)
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

        // <b>A boca não ganha mais baú</b> — decisão do autor, 2026-09-15:
        // <i>"retire o baú da boca da mina, use só o baú de cada
        // mineiro"</i>. Revoga a Regra 30, que mandava o minério que não é
        // carvão para cá.
        //
        // O que o autor viu em jogo foi a boca sem lugar onde pôr o baú —
        // <i>"nothing beside it is free"</i>, repetido a cada passagem —, e
        // a decisão resolve a causa em vez do sintoma: sem baú de boca não
        // há posição a procurar, não há linha de recusa, e o minério vai
        // para o mesmo lugar que a pedra e o carvão já iam.
        //
        // O baú que o jogador já tem na boca <b>continua lá</b>, com o que
        // guarda: nada é removido do mundo. Ele deixa de ser destino de
        // depósito, e {@code ColonyChests} deixa de somá-lo ao estoque da
        // colônia — o que ele guarda a colônia passa a não ver, e é o preço
        // conhecido desta decisão.
        //
        // As lanternas não vêm por aqui: são peças do portão, postas pelo
        // {@code raiseArch}.
        return new Furnished(Optional.empty(), archRaisedNow);
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
     * Se a posição pertence ao portão da boca ({@link MineGate}). Essas peças nunca entram na ordem de escavação da
     * própria mina.
     */
    public static boolean isPortalBlock(BlockPos mouth, Direction descent, BlockPos at) {
        return MineGate.isPart(mouth, descent, at);
    }

    /**
     * O portão da entrada — a estrutura do autor (2026-10-29), no lugar do arco de pedregulho de 2026-09-05. A boca
     * fica no meio do vão, e o vão corre no rumo da descida. Só onde cabe (Regra 3); uma vez só, pelo save.
     *
     * @return se ao menos uma posição do portão está firme depois da passagem
     */
    private static boolean raiseArch(ServerWorld world, BlockPos mouth, Direction descent) {
        return MineGate.raise(world, mouth, descent);
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

    private static Object chunkAt(ServerWorld world, BlockPos at) {
        return world.getChunkManager().getWorldChunk(at.getX() >> 4, at.getZ() >> 4);
    }
}
