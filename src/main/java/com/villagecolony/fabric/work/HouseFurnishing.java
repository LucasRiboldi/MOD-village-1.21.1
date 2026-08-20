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

    private HouseFurnishing() {
    }

    /**
     * Uma passagem por todas as casas desta colônia.
     *
     * <p>Chamada uma vez por ciclo, junto com o resto do trabalho.
     * Custa uma leitura de bloco por peça de mobília por casa — três
     * leituras por casa —, e só age quando falta alguma.
     */
    public static void run(ServerWorld world, Colony colony) {
        List<String> missing = new ArrayList<>();

        for (Building house : List.copyOf(VillageColonyMod.BUILDINGS.ofColony(colony.id()))) {
            if (!ColonyHut.ID.equals(house.blueprint())) {
                // Casa lida do jogo tem a mobília que o arquivo dela
                // manda, e não a desta regra.
                continue;
            }

            // A conta cresce dentro da casa e é gravada uma vez no fim.
            // Registrar peça a peça a partir de `house` perderia as
            // anteriores: a construção é imutável, e cada `withFurnished`
            // partiria da cópia velha. Só a última peça ficaria marcada,
            // e as outras duas voltariam no ciclo seguinte.
            Building current = house;

            for (BlueprintBlock piece : ColonyHut.furnishings()) {
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
