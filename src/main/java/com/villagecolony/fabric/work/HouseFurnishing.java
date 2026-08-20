package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ColonyHut;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ColonySupply;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A mobília que entra depois de a casa estar de pé — a Regra 21.
 *
 * <p>A casa termina sem cama e sem lampião, e é decisão: dos três
 * móveis, a colônia só sabe fazer o baú. Cama pede lã e lampião pede
 * ferro, e nenhum aldeão deste mod tosquia ou minera (Regra 13).
 * Segurar a obra por eles faria nenhuma casa terminar e a vila parar de
 * crescer.
 *
 * <p>Então a pendência muda de dono: sai da <b>obra</b> e passa para a
 * <b>casa</b>. Toda passagem do ciclo, cada casa da colônia é olhada, e
 * o que faltar entra assim que houver material — inclusive material que
 * o jogador guardou no baú, que é como lã e ferro chegam hoje.
 *
 * <p><b>Sem estado novo em disco.</b> O que falta numa casa é lido do
 * mundo: a construção guarda o canto e a planta, a cabana sabe onde a
 * mobília dela vai, e o resto é olhar se o bloco está lá. Gravar a lista
 * de pendências seria uma segunda verdade que o jogador poderia
 * contradizer com uma picareta.
 */
public final class HouseFurnishing {

    /**
     * Que peça já foi posta em que casa, e quando.
     *
     * <p><b>Por que existe.</b> Sem ela a pergunta "esta peça está lá?"
     * é feita ao mundo toda passagem, e a resposta "não" produz uma peça
     * nova do baú — a cada trinta segundos, para sempre. Na sessão de
     * 2026-08-19 a mesma casa recebeu sete baús, quatro camas e quatro
     * lampiões em quatro minutos.
     *
     * <p>O que tirou os blocos não está no log e pode não ser do mod:
     * creeper, o jogador com uma picareta. Nos dois casos repor sem
     * limite está errado — no primeiro sangra o estoque da colônia, e no
     * segundo escreve por cima da escolha do jogador, que é a Regra 3.
     *
     * <p><b>E por que ela vence.</b> A Regra 23: marca que não vence é
     * uma afirmação sobre o futuro do mundo do jogador, e o mod não tem
     * como fazer nenhuma. Dez ciclos é o mesmo prazo da recusa de árvore
     * e do alvo fora de alcance.
     */
    private static final Map<Mark, Long> FURNISHED = new HashMap<>();

    /** Dez ciclos da colônia, o prazo da Regra 23. */
    private static final int MEMORY = 10 * VillageDetector.CYCLE_TICKS;

    /**
     * Teto de marcas guardadas.
     *
     * <p>Três por casa. Mil casas é vila que este mod nunca viu, e o
     * teto existe pelo mesmo motivo que o do lenhador: mapa estático que
     * só cresce é vazamento com outro nome.
     */
    private static final int MAX_MARKS = 3072;

    /** Uma peça de uma casa. */
    private record Mark(UUID building, ResourceId piece) {
    }

    private HouseFurnishing() {
    }

    /** Esquece as marcas. Chamado ao parar o servidor. */
    public static void clearAll() {
        FURNISHED.clear();
    }

    /**
     * Uma passagem por todas as casas desta colônia.
     *
     * <p>Chamada uma vez por ciclo, junto com o resto do trabalho.
     * Custa uma leitura de bloco por peça de mobília por casa — três
     * leituras por casa —, e só age quando falta alguma.
     */
    public static void run(ServerWorld world, Colony colony) {
        forgetOldMarks(world);

        List<String> missing = new ArrayList<>();

        for (Building house : VillageColonyMod.BUILDINGS.ofColony(colony.id())) {
            if (!ColonyHut.ID.equals(house.blueprint())) {
                // Casa lida do jogo tem a mobília que o arquivo dela
                // manda, e não a desta regra.
                continue;
            }

            for (BlueprintBlock piece : ColonyHut.furnishings()) {
                if (!furnish(world, colony, house, piece)) {
                    missing.add(piece.block().path());
                }
            }
        }

        if (!missing.isEmpty()) {
            // Uma linha por ciclo, e só quando falta algo. Sem ela,
            // "casa sem cama" e "casa que a colônia esqueceu" são o
            // mesmo silêncio — é o §11 outra vez.
            VillageColonyMod.LOGGER.info(
                    "Colony {} has houses still missing {}", colony.id(), missing);
        }
    }

    /**
     * Deixa vencer o que passou de dez ciclos.
     *
     * <p>É a Regra 23, e o mesmo desenho do lenhador: a marca guarda
     * quando nasceu, e esquece sozinha.
     */
    private static void forgetOldMarks(ServerWorld world) {
        FURNISHED.values().removeIf(since -> world.getTime() - since >= MEMORY);
    }

    /**
     * Põe uma peça, se ela faltar e houver material.
     *
     * @return true quando a peça está lá — já estava, ou acabou de
     *     entrar. False quer dizer que ainda falta
     */
    private static boolean furnish(
            ServerWorld world, Colony colony, Building house, BlueprintBlock piece) {

        Optional<Block> block = MinecraftTypeAdapter.toBlock(piece.block());

        if (block.isEmpty()) {
            // O jogo não conhece esta peça. Não é pendência: é peça que
            // não existe, e insistir por ela produziria uma linha por
            // ciclo para sempre.
            return true;
        }

        BlockPos where = MinecraftTypeAdapter.toBlockPos(new ColonyPos(
                house.min().x() + piece.offset().x(),
                house.min().y() + piece.offset().y(),
                house.min().z() + piece.offset().z()));

        BlockState standing = world.getBlockState(where);

        if (standing.isOf(block.get())) {
            return true;
        }

        if (FURNISHED.containsKey(new Mark(house.id(), piece.block()))) {
            // Esta peça já entrou nesta casa, e não está mais lá. Alguém
            // a tirou — e não é a colônia que decide desfazer isso toda
            // passagem. A marca vence em dez ciclos, e então a casa tem
            // outra chance.
            return true;
        }

        if (!standing.isReplaceable()) {
            // Tem outra coisa ali, e a Regra 3 manda não mexer: pode ser
            // do jogador. A casa fica sem esta peça, e isso não é
            // pendência da colônia — é escolha de quem mora nela.
            return true;
        }

        BlockState state = block.get().getDefaultState();

        if (!state.canPlaceAt(world, where)) {
            return true;
        }

        if (!ColonySupply.take(world, colony.id(), house.min(), block.get().asItem())) {
            return false;
        }

        world.setBlockState(where, state, Block.NOTIFY_ALL);

        if (FURNISHED.size() < MAX_MARKS) {
            FURNISHED.put(new Mark(house.id(), piece.block()), world.getTime());
        }

        // A cama ocupa dois lugares, como a porta. É o mesmo caminho da
        // obra, e por isso mora lá.
        BuilderWork.placeSecondHalf(world, where, state);

        VillageColonyMod.LOGGER.info(
                "Colony {} furnished the house at {} with {}",
                colony.id(),
                house.min(),
                piece.block());

        return true;
    }
}
