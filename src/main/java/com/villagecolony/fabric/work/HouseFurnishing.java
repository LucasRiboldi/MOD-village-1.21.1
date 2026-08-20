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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
     * A mobília de cada casa, lida uma vez.
     *
     * <p>Esquecida ao parar o servidor. Perder isto custa uma leitura de
     * template por casa na primeira passagem seguinte, e nada mais.
     */
    private static final Map<UUID, List<BlueprintBlock>> PIECES = new HashMap<>();

    /** Teto de plantas guardadas. Uma por casa. */
    private static final int MAX_PLANS = 1024;

    private HouseFurnishing() {
    }

    /** Esquece as plantas lidas. Chamado ao parar o servidor. */
    public static void clearAll() {
        PIECES.clear();
    }

    /**
     * Uma passagem por todas as casas desta colônia.
     *
     * <p>Chamada uma vez por ciclo, junto com o resto do trabalho.
     * Custa uma leitura de bloco por peça de mobília por casa — três
     * leituras por casa —, e só age quando falta alguma.
     */
    public static int run(ServerWorld world, Colony colony) {
        List<String> missing = new ArrayList<>();

        for (Building house : List.copyOf(VillageColonyMod.BUILDINGS.ofColony(colony.id()))) {
            List<BlueprintBlock> pieces = furnishingsOf(world, house);

            if (pieces.isEmpty()) {
                continue;
            }

            // A conta cresce dentro da casa e é gravada uma vez no fim.
            // Registrar peça a peça a partir de `house` perderia as
            // anteriores: a construção é imutável, e cada `withFurnished`
            // partiria da cópia velha. Só a última peça ficaria marcada,
            // e as outras duas voltariam no ciclo seguinte.
            Building current = house;

            for (BlueprintBlock piece : pieces) {
                switch (furnish(world, colony, current, piece)) {
                    case PLACED -> current = current.withFurnished(piece.block());
                    case MISSING -> missing.add(piece.block().path());
                    case ALREADY -> {
                    }
                }
            }

            if (current != house) {
                VillageColonyMod.BUILDINGS.register(current);
            }
        }

        if (!missing.isEmpty()) {
            // Uma linha por ciclo, e só quando falta algo. Sem ela,
            // "casa sem cama" e "casa que a colônia esqueceu" são o
            // mesmo silêncio — é o §11 outra vez.
            VillageColonyMod.LOGGER.info(
                    "Colony {} has houses still missing {}", colony.id(), missing);
        }

        // Quantas peças de lã a colônia ainda quer — 2026-08-20. A cama
        // custa três, e é a única peça de mobília feita de lã. Quem usa
        // este número é a meta da colônia: sem ele o pastor nunca recebe
        // tarefa, e sem tarefa não há tosquia, cama, aldeão nem vila
        // crescendo. Era o laço aberto que a Regra 21 deixou.
        return (int) missing.stream().filter(piece -> piece.endsWith("_bed")).count() * 3;
    }

    /**
     * Onde vai a mobília desta casa, e qual é.
     *
     * <p><b>De qualquer casa, e não só da cabana.</b> Até 2026-08-20
     * esta passagem pulava tudo que não fosse {@code ColonyHut}, com o
     * argumento de que casa lida do jogo tem a mobília que o arquivo
     * manda. O argumento ignorava o que o construtor faz quando falta
     * material: ele pula a peça com {@code finishes without ...} e a
     * casa termina sem ela. Ninguém voltava.
     *
     * <p>E o efeito era de sistema. Casa sem cama não vira aldeão,
     * aldeão é quem trabalha, e a casa que a colônia passou a preferir
     * naquele mesmo dia era justamente a que nunca ganhava cama — o laço
     * da vila ficava aberto onde ele devia fechar.
     *
     * <p>A planta vem girada como a casa foi levantada. Sem isso os
     * deslocamentos da mobília seriam os do arquivo, e a cama entraria
     * atravessada na parede de uma casa que o mundo girou.
     *
     * <p><b>Lida uma vez por casa por sessão.</b> Ler um template é
     * abrir e decodificar centenas de blocos, e esta passagem roda a
     * cada ciclo para cada casa. O aviso está no cabeçalho de
     * {@code StructureBlueprintReader}: quem chama guarda o resultado.
     */
    private static List<BlueprintBlock> furnishingsOf(ServerWorld world, Building house) {
        List<BlueprintBlock> known = PIECES.get(house.id());

        if (known != null) {
            return known;
        }

        List<BlueprintBlock> pieces = HousePlans
                .blueprintOf(world, house.blueprint(), house.min())
                .map(plan -> plan.blocks().stream()
                        .filter(BlueprintBlock::furniture)
                        .toList())
                .orElse(List.of());

        if (PIECES.size() < MAX_PLANS) {
            PIECES.put(house.id(), pieces);
        }

        return pieces;
    }

    /** O que a passagem fez com uma peça. */
    private enum Outcome {

        /** Não havia o que fazer: já está lá, ou não pode ir para lá. */
        ALREADY,

        /** Entrou agora, e a casa passa a contá-la. */
        PLACED,

        /** Falta material na colônia. É a pendência que o log conta. */
        MISSING
    }

    /**
     * Põe uma peça, se ela faltar e houver material.
     */
    private static Outcome furnish(
            ServerWorld world, Colony colony, Building house, BlueprintBlock piece) {

        Optional<Block> block = MinecraftTypeAdapter.toBlock(piece.block());

        if (block.isEmpty()) {
            // O jogo não conhece esta peça. Não é pendência: é peça que
            // não existe, e insistir por ela produziria uma linha por
            // ciclo para sempre.
            return Outcome.ALREADY;
        }

        BlockPos where = MinecraftTypeAdapter.toBlockPos(new ColonyPos(
                house.min().x() + piece.offset().x(),
                house.min().y() + piece.offset().y(),
                house.min().z() + piece.offset().z()));

        BlockState standing = world.getBlockState(where);

        if (standing.isOf(block.get())) {
            return Outcome.ALREADY;
        }

        if (house.wasFurnishedWith(piece.block())) {
            // Esta peça já entrou nesta casa uma vez, e não está mais lá:
            // alguém a destruiu. Regra do autor, 2026-08-20 — peça
            // destruída não volta. Repor seria a colônia desfazendo, a
            // cada trinta segundos, o que o jogador fez de propósito.
            return Outcome.ALREADY;
        }

        if (!standing.isReplaceable()) {
            // Tem outra coisa ali, e a Regra 3 manda não mexer: pode ser
            // do jogador. A casa fica sem esta peça, e isso não é
            // pendência da colônia — é escolha de quem mora nela.
            return Outcome.ALREADY;
        }

        BlockState state = block.get().getDefaultState();

        if (!state.canPlaceAt(world, where)) {
            return Outcome.ALREADY;
        }

        if (!ColonySupply.take(world, colony.id(), house.min(), block.get().asItem())) {
            return Outcome.MISSING;
        }

        world.setBlockState(where, state, Block.NOTIFY_ALL);

        // A cama ocupa dois lugares, como a porta. É o mesmo caminho da
        // obra, e por isso mora lá.
        BuilderWork.placeSecondHalf(world, where, state);

        VillageColonyMod.LOGGER.info(
                "Colony {} furnished the house at {} with {}",
                colony.id(),
                house.min(),
                piece.block());

        return Outcome.PLACED;
    }
}
