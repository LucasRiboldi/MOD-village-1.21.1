package com.villagecolony.fabric.integration;

import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * O minério que o mineiro reconhece, e a veia que ele segue — 2026-08-21.
 *
 * <p>A Regra 29 mandou o mineiro descer vinte blocos, e até aqui ele
 * descia sem <b>ver</b>: passava ao lado de carvão e de ferro e trazia
 * pedregulho. A casa de planície ficava sem as três tochas, e o lampião
 * da Regra 21 sem o lingote.
 *
 * <p><b>Dois minérios, e só dois.</b> Carvão e ferro são os que alguma
 * receita da colônia consome — a tocha e o lampião. Ouro, cobre,
 * redstone e o resto ficam de fora de propósito: cavar o que ninguém usa
 * enche o baú e faz a Regra 1 parar a coleta do que falta.
 *
 * <p><b>A veia se segue.</b> Minério não vem sozinho: um carvão tem
 * outro do lado. Achar um e voltar para o túnel deixaria a veia
 * pela metade e mandaria o aldeão andar de novo até lá na passagem
 * seguinte. Quem lembra onde ela está é a {@code Mine} da colônia — e
 * lembra em memória, porque veia interrompida por fechar o mundo é uma
 * viagem a mais, e não um erro.
 */
public final class OreVein {

    /**
     * O que se reconhece, com as variantes de ardósia.
     *
     * <p>A ardósia é o mesmo minério mais fundo, e a segunda sala da mina
     * fica no nível -20: sem as duas variantes, metade da mina seria cega
     * justamente onde há mais minério.
     *
     * <p><b>Eram dois em 2026-08-21 — carvão e ferro — e são oito desde
     * a Regra 30.</b> A regra manda o mineiro ir atrás de recurso
     * normalmente e recolher tudo o que cavar; seguir só a veia de dois
     * minérios era o mineiro passando ao lado de diamante sem ver.
     */
    /**
     * O que é minério — perguntado ao jogo, e não escrito aqui.
     *
     * <p><b>Decisão do autor, 2026-08-27:</b> <i>"ele deve minerar todo
     * tipo de minério"</i>. Havia aqui uma lista de dezesseis nomes, e
     * ela era a regra de ouro da ADR-009 sendo desobedecida: <i>uma
     * solução dessa forma deve ser questionada</i>. Cada minério novo —
     * de outra versão, de um datapack, de outro mod — pedia uma linha, e
     * até alguém escrevê-la o mineiro passava por cima dele como se
     * fosse pedra.
     *
     * <p>{@code c:ores} é a etiqueta convencional que o próprio jogo
     * mantém, e é o mesmo caminho da Regra 27: quem responde é o
     * catálogo. Ela já traz os dezesseis do Overworld com as variantes
     * de deepslate, mais quartzo e ouro do Nether e os escombros
     * antigos, e passa a trazer sozinha o que vier depois.
     *
     * <p><b>O que isto não conserta</b>, e fica dito: a lista antiga já
     * cobria todo minério de superfície do Overworld em 1.21. Quem
     * ganha com a troca é o mundo com datapack e a versão seguinte — não
     * a sessão de hoje.
     */
    private static final TagKey<Block> ORE = ConventionalBlockTags.ORES;

    /**
     * O carvão, que é minério e não é tesouro — Regra 30, 2026-08-22.
     *
     * <p>A colônia o consome o tempo todo: a tocha sai dele, e o
     * fundidor o queima. Guardar carvão no baú da boca da mina seria
     * mandá-lo para longe de quem o usa.
     */
    private static final TagKey<Block> COAL = BlockTags.COAL_ORES;

    /**
     * Do mais útil para o menos útil — decisão do autor, 2026-09-04.
     *
     * <p><b>Esta ordem substitui a de 2026-09-03, e a inverte quase
     * inteira.</b> Aquela era a raridade — <i>"deve sempre priorizar os
     * minerais diferentes e mais raros"</i> — e punha os escombros
     * antigos na frente e o carvão no fim. O autor trocou o critério: o
     * que decide não é quão raro o minério é no mundo, e sim <b>o que a
     * vila faz com ele</b>, na ordem em que ela precisa fazer.
     *
     * <p>Diamante numa vila sem tocha é diamante numa vila cujo chão
     * gera monstro. A justificativa de cada posto é do autor, e fica
     * escrita porque é ela que torna a lista conferível:
     *
     * <ol>
     * <li><b>Carvão</b> — tocha e fornalha. Ilumina a fundação e impede
     *     que monstro nasça nela; é o único que a colônia já consome
     *     hoje, pelas duas pontas.
     * <li><b>Ferro</b> — estações de trabalho, lampião, sino, e o Golem
     *     que defende a vila.
     * <li><b>Cobre</b> — para-raios. Telhado sem ele é aldeão virando
     *     bruxa.
     * <li><b>Ouro</b> — maçã dourada, que cura aldeão zumbi; e trilho
     *     elétrico.
     * <li><b>Esmeralda</b> — a moeda. Sem ela a profissão não evolui.
     * <li><b>Redstone</b> — automação: porta noturna, fazenda,
     *     procriador.
     * <li><b>Lápis-lazúli</b> — o clérigo, e o encantamento de quem
     *     protege.
     * <li><b>Diamante</b> — comércio de nível avançado.
     * <li><b>Quartzo do Nether</b> — sensor de luz solar e circuito.
     * <li><b>Escombros antigos</b> — a fase final, na mesa de ferraria.
     * </ol>
     *
     * <p><b>Por que uma lista escrita, num projeto que não gosta
     * delas.</b> A ADR-009 manda perguntar ao catálogo, e é o que o
     * {@link #ORE} faz — <i>o que é minério</i> é fato do jogo. <b>Para
     * que a vila usa cada um não é.</b> Não há etiqueta de utilidade, e
     * não haveria: ela é do mod, e muda quando a colônia aprende uma
     * receita nova.
     *
     * <p>Então isto é julgamento, e está escrito como julgamento — mas em
     * <b>etiquetas</b>, e não em nomes de bloco. É o que preserva o que a
     * troca de 08-27 comprou: a variante de ardósia, a de outra versão e
     * a de datapack entram sozinhas na etiqueta que já as cobre.
     *
     * <p>Minério que não está em nenhuma destas — de mod, de datapack —
     * fica {@link #UNRANKED}, <b>depois de todos</b>. Ver o javadoc de lá:
     * a troca de critério mudou também o lugar do desconhecido.
     */
    private static final List<TagKey<Block>> BY_USE = List.of(
            BlockTags.COAL_ORES,
            BlockTags.IRON_ORES,
            BlockTags.COPPER_ORES,
            BlockTags.GOLD_ORES,
            BlockTags.EMERALD_ORES,
            BlockTags.REDSTONE_ORES,
            BlockTags.LAPIS_ORES,
            BlockTags.DIAMOND_ORES,
            ConventionalBlockTags.QUARTZ_ORES,
            ConventionalBlockTags.NETHERITE_SCRAP_ORES);

    /**
     * Onde entra o minério que nenhuma etiqueta conhecida classifica.
     *
     * <p><b>Depois de todos, e isso mudou em 2026-09-04.</b> Sob a ordem
     * de raridade ele ficava no meio — logo após o ferro —, porque
     * <i>"um minério de mod é quase sempre mais raro que carvão"</i>.
     * Sob a ordem de utilidade a pergunta é outra, e a resposta se
     * inverte: <b>a colônia não tem receita nenhuma para ele</b>. Um
     * minério que ela não sabe usar não deve ganhar do carvão que ela
     * queima nem do ferro que vira Golem.
     *
     * <p>Ele continua ganhando da pedra comum, e sem precisar de linha
     * para isso: o {@link #beside} só olha o que é minério.
     */
    private static final int UNRANKED = 10;

    private OreVein() {
    }

    /**
     * Quanto a vila precisa deste minério — zero é o que ela precisa antes.
     *
     * <p>Público porque a escolha do alvo é de quem cava, e a resposta é
     * a mesma para o veio e para o túnel: entre dois minérios à vista,
     * ganha o de número menor.
     *
     * <p>Chamava-se {@code rarityOf} até 2026-09-04, e o nome mentia
     * desde a troca de critério: o número já não mede raridade. Ver
     * {@link #BY_USE}.
     */
    public static int priorityOf(BlockState state) {
        for (int rank = 0; rank < BY_USE.size(); rank++) {
            if (state.isIn(BY_USE.get(rank))) {
                return rank;
            }
        }

        return UNRANKED;
    }

    /** Se este bloco é minério que a colônia usa. */
    public static boolean isOre(BlockState state) {
        return state.isIn(ORE);
    }

    /**
     * Se este minério vai para o baú da boca da mina — Regra 30.
     *
     * <p>Regra do autor, 2026-08-22: o baú da boca guarda <b>todo
     * minério menos carvão</b> — cobre, ferro, ouro, redstone, lápis,
     * esmeralda e diamante. Pedra, terra e carvão vão direto para o baú
     * do mineiro na vila, que é de onde a obra e a fornalha tiram o que
     * consomem.
     */
    public static boolean isTreasure(BlockState state) {
        return isOre(state) && !state.isIn(COAL);
    }

    /**
     * O minério colado nesta posição, se houver e se puder ser cavado.
     *
     * <p>As seis faces, e nada além: o minério que se vê da parede do
     * túnel. Procurar mais longe seria o mineiro cavando às cegas atrás
     * do que ele não tem como saber que existe.
     *
     * <p>A Regra 3 vale aqui como vale para a pedra. Minério não é peça
     * de vila gerada nem casa de colônia, e a conferência é feita assim
     * mesmo: o dia em que um datapack puser minério numa parede de igreja
     * é o dia em que esta linha vale o que custou.
     */
    public static Optional<BlockPos> beside(ServerWorld world, BlockPos at) {
        BlockPos chosen = null;
        int best = Integer.MAX_VALUE;

        for (Direction face : Direction.values()) {
            BlockPos next = at.offset(face);

            if (!world.isInBuildLimit(next)) {
                continue;
            }

            BlockState state = world.getBlockState(next);

            if (!isOre(state)) {
                continue;
            }

            if (BlockProtection.isVillageOriginal(world, next)
                    || BlockProtection.isColonyBuilt(next)) {
                continue;
            }

            // <b>O mais útil das seis, e não a primeira das seis</b> —
            // 2026-09-03 pela raridade, 2026-09-04 pela utilidade. O laço
            // devolvia na primeira face que servisse, e o
            // Direction.values() começa em DOWN. Ver BY_USE.
            int priority = priorityOf(state);

            if (priority >= best) {
                continue;
            }

            chosen = next;
            best = priority;

            if (priority == 0) {
                // Nada supera o primeiro da lista — hoje o carvão: as
                // faces que faltam não podem mudar a resposta, e cada uma
                // custa uma leitura.
                break;
            }
        }

        return Optional.ofNullable(chosen);
    }
}
