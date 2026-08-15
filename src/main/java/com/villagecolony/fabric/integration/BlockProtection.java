package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.StructureAccessor;

/**
 * O que o trabalhador nunca pode quebrar.
 *
 * <p>Regra do autor, 2026-08-13:
 *
 * <ul>
 *   <li>nunca destruir um bloco da vila original;
 *   <li>nunca destruir um bloco colocado pelo jogador.
 * </ul>
 *
 * <p><b>A árvore é a exceção, e é a única.</b> Ela fica fora desta regra
 * por decisão do autor: o lenhador derruba árvore onde a achar, inclusive
 * dentro da caixa da vila. Sem essa exceção não haveria colheita — vila
 * de planície nasce cercada de carvalho, e boa parte dele cai dentro dos
 * limites que o jogo registra para a vila.
 *
 * <p>Árvore que nasceu junto com a vila é derrubável como qualquer
 * outra — perguntado ao autor em 2026-08-13 e confirmado por ele. A
 * exceção é da árvore, não do lugar onde ela está. O que separa a casa
 * da floresta não é esta classe: é a regra da copa, e tronco sem folha
 * viva não é árvore.
 *
 * <p><b>Por que "colocado pelo jogador" é uma pergunta difícil.</b> O
 * Minecraft não guarda quem pôs cada bloco. A única marca que o mundo
 * carrega é a folha: colocada à mão vem {@code persistent}, nascida de
 * árvore não. Para tudo o mais, o mod não tem como saber — e é por isso
 * que a proteção real é a inversa, e mora nas outras classes: o
 * trabalhador só quebra o que ele consegue provar ser floresta. Ver
 * {@code TreeHarvester}.
 *
 * <p>Esta classe é a porta única. Fabricar e construir tocam no mundo, e é
 * aqui que a pergunta "posso quebrar isto?" é feita — uma vez, num lugar
 * só.
 *
 * <p>São três os que não se quebram: o bloco da vila que o jogo gerou, o
 * bloco que o jogador pôs, e — desde 2026-08-14 — o bloco de uma casa que
 * a própria colônia levantou. Ver {@link #isColonyBuilt}.
 */
public final class BlockProtection {

    private BlockProtection() {
    }

    /**
     * Se o trabalhador pode quebrar este bloco.
     *
     * <p>Não pergunta sobre árvore: quem colhe árvore não passa por aqui,
     * pela exceção do autor. Ver o cabeçalho desta classe.
     *
     * @param state o estado já lido pelo chamador, que tem o cuidado de
     *     não forçar chunk. Passar o estado em vez de lê-lo aqui evita
     *     uma segunda leitura e mantém a regra de ADR-002 num lugar só
     */
    public static boolean mayBreak(ServerWorld world, BlockPos pos, BlockState state) {
        return !isPlayerPlaced(state)
                && !isColonyBuilt(pos)
                && !isVillageOriginal(world, pos);
    }

    /**
     * Se este bloco é de uma casa que a própria colônia levantou.
     *
     * <p>A terceira metade da regra, e a que o autor não precisou
     * enunciar: a vila original é do jogo, o bloco do jogador é dele, e
     * este é da colônia. Nenhum dos três se derruba.
     *
     * <p>Ligada em 2026-08-14 — o E7. A resposta existia desde que a Fase
     * 11 entrou, e esta porta não a perguntava. Não causava dano enquanto
     * a única coisa que o mod quebrava era árvore, porque a regra da copa
     * já separa tronco de construção; passaria a causar na primeira
     * demolição de qualquer outra natureza — e é o tipo de furo que só
     * aparece depois de ter sido usado.
     *
     * <p>Pergunta pela caixa da construção, e não por bloco colocado: é o
     * que {@code Building} guarda, e é de propósito. Um bloco que o
     * construtor pulou continua sendo parte da casa, senão a casa teria
     * buracos por onde uma demolição passaria.
     */
    public static boolean isColonyBuilt(BlockPos pos) {
        return VillageColonyMod.BUILDINGS.isColonyInfrastructure(
                MinecraftTypeAdapter.toColonyPos(pos));
    }

    /**
     * Se este bloco faz parte da vila que o jogo gerou.
     *
     * <p>Pergunta ao próprio jogo: o Minecraft guarda, por chunk, as
     * peças de cada estrutura gerada — a casa, o poço, a rua, o lampião.
     * A verificação é por peça, e não pela caixa da vila inteira: a caixa
     * cobre também o campo aberto entre as casas, e proibir o campo
     * aberto proibiria a colônia de trabalhar dentro da própria vila.
     *
     * <p>Vale para vila de qualquer bioma — a etiqueta
     * {@code minecraft:village} cobre as cinco.
     *
     * <p>Vila construída pelo jogador não tem estrutura registrada, e
     * esta pergunta responde "não" para ela. Isso não é buraco: bloco
     * posto pelo jogador é a outra metade da regra, e o que protege a
     * construção dele é o trabalhador só quebrar o que prova ser
     * floresta.
     */
    public static boolean isVillageOriginal(ServerWorld world, BlockPos pos) {
        StructureAccessor structures = world.getStructureAccessor();

        // Barato primeiro: a esmagadora maioria dos blocos não tem
        // referência de estrutura nenhuma, e aí não há o que consultar.
        if (!structures.hasStructureReferences(pos)) {
            return false;
        }

        StructureStart village = structures.getStructureContaining(pos, StructureTags.VILLAGE);

        if (village == null || village == StructureStart.DEFAULT || !village.hasChildren()) {
            return false;
        }

        return structures.structureContains(pos, village);
    }

    /**
     * Se este bloco foi colocado à mão.
     *
     * <p>Só a folha responde. É a mesma marca que a colheita usa para
     * separar copa de decoração — ver {@code TreeHarvester#isNaturalLeaf}
     * — e é a única que o mundo guarda.
     */
    public static boolean isPlayerPlaced(BlockState state) {
        return state.contains(LeavesBlock.PERSISTENT) && state.get(LeavesBlock.PERSISTENT);
    }
}
