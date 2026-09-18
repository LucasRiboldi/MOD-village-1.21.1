package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.Building;
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
 * <p>Árvores não têm exceção estrutural: troncos e folhas que pertencem
 * a uma construção da vila original ou da colônia são protegidos. O
 * lenhador consulta esta porta ao planejar a árvore e novamente antes de
 * quebrar cada bloco.
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
     *
     * <p><b>E a obra EM ANDAMENTO conta</b> — 2026-09-16. O autor viu em
     * jogo: <i>"casa começou e não continuou, mas confundiu colocar tronco
     * com recolher tronco e plantar árvore"</i>. O log de 00:59:51 tem as
     * duas linhas na mesma coordenada — o construtor assentando em
     * {@code 2503,63,-3035} e o lenhador indo colher <i>"tree at 2503, 63,
     * -3035, block 1 of 1"</i>. A casa caía enquanto subia.
     *
     * <p>A causa era de <b>momento</b>, não de regra: {@code Building} só
     * nasce quando a obra termina, ou quando ela é abandonada. O canteiro
     * ficava desprotegido justamente durante as horas em que há material
     * solto nele — e o material solto de uma casa de planície é tronco de
     * carvalho, que é exatamente o que o lenhador procura.
     *
     * <p>As duas perguntas juntas cobrem a vida inteira de uma casa: a
     * obra aberta pelo registro de construções, a casa pronta pelo de
     * construções levantadas.
     */
    public static boolean isColonyBuilt(BlockPos pos) {
        ColonyPos at = MinecraftTypeAdapter.toColonyPos(pos);

        if (VillageColonyMod.BUILDINGS.isColonyInfrastructure(at)) {
            return true;
        }

        return isOpenSite(at);
    }

    /**
     * Se esta posição está dentro de uma obra aberta — 2026-09-16.
     *
     * <p>Percorre as obras em curso, e são poucas: uma por colônia, pela
     * vaga única que {@code ConstructionService.register} guarda. O custo
     * é uma comparação de caixa por colônia ativa, e esta pergunta só é
     * feita quando as duas anteriores já disseram não.
     */
    private static boolean isOpenSite(ColonyPos at) {
        for (ConstructionProject project : VillageColonyMod.CONSTRUCTIONS.all()) {
            if (!project.state().isOpen()) {
                continue;
            }

            if (Building.of(project).contains(at)) {
                return true;
            }
        }

        return false;
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

        boolean inside = structures.structureContains(pos, village);

        if (inside) {
            ProtectionSample.saw(world, pos);
        }

        return inside;
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
