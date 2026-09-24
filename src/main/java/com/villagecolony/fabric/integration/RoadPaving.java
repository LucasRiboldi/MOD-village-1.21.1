package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O calçamento bloco a bloco e a leitura de onde a rua acaba — separado de
 * {@link RoadExtension} em 2026-09-24, quando ele passou de 500 linhas.
 *
 * <p>O {@code RoadExtension} decide <b>quando</b> e <b>de qual ponta</b> a rua
 * cresce; esta classe assenta o trecho respeitando a Regra 3 e o degrau
 * máximo, e responde para que lado uma rua termina. Os comentários vieram
 * junto sem mudança.
 */
final class RoadPaving {

    private RoadPaving() {
    }

    /**
     * Assenta o trecho, bloco a bloco, e para no primeiro que recusar.
     *
     * <p>Para de verdade, e não pula: rua com buraco no meio é rua que o
     * aldeão não atravessa, e a beira depois do buraco não serve de lote.
     *
     * @return as colunas que entraram, na ordem
     */
    static List<ColonyPos> pave(
            ServerWorld world, UUID colonyId, RoadExtension.End end, Block paving) {

        BlockPos previous = end.at();

        List<ColonyPos> laid = new ArrayList<>();

        for (int step = 0; step < RoadExtension.STRETCH; step++) {
            BlockPos ahead = previous.offset(end.towards());

            Optional<BlockPos> ground = groundNear(world, ahead, previous.getY());

            if (ground.isEmpty()) {
                return laid;
            }

            BlockPos at = ground.get();

            BlockState state = world.getBlockState(at);

            if (BuildSiteScanner.isRoadArea(world, colonyId, at)) {
                // Já é rua: a ponta encostou noutro trecho. Segue por
                // cima dela sem gastar nada, que é o que dois calçamentos
                // que se encontram fazem.
                previous = at;

                continue;
            }

            // A Regra 3 nas duas pontas, e aqui ela morde: a vila gerada
            // é feita de bloco que passaria por chão.
            if (BlockProtection.isVillageOriginal(world, at)
                    || BlockProtection.isColonyBuilt(at)
                    || !BuildSiteScanner.isNaturalGround(state)
                    || !world.getBlockState(at.up()).isAir()) {

                return laid;
            }

            world.setBlockState(at, paving.getDefaultState());

            // O índice de ruas precisa saber da beira nova — 2026-08-27.
            // A rua cresce justamente quando não houve lote, e o lote
            // novo nasce encostado no que acabou de ser calçado: índice
            // que não soubesse disto nunca mais acharia nada.
            BuildSiteScanner.remember(colonyId, at);

            previous = at;

            laid.add(MinecraftTypeAdapter.toColonyPos(at));
        }

        return laid;
    }

    /**
     * O chão desta coluna, se ele estiver ao alcance de um degrau.
     *
     * <p>Um bloco acima ou um abaixo do anterior. Mais que isso e a rua
     * vira escada — e a regra do autor manda parar onde o desnível passa
     * do limite, não escalar.
     */
    static Optional<BlockPos> groundNear(ServerWorld world, BlockPos column, int fromY) {
        for (int dy = RoadExtension.MAX_STEP; dy >= -RoadExtension.MAX_STEP; dy--) {
            BlockPos at = new BlockPos(column.getX(), fromY + dy, column.getZ());

            if (!world.isInBuildLimit(at)) {
                continue;
            }

            if (world.getBlockState(at).isAir()) {
                continue;
            }

            return Optional.of(at);
        }

        return Optional.empty();
    }

    /**
     * Para que lado esta rua acaba, se acabar.
     *
     * <p>Duas perguntas, e as duas precisam: <b>atrás</b> tem rua — senão
     * é um bloco solto, e prolongar calçamento perdido no mato não faz
     * vila —, e <b>à frente</b> não tem. Aí este é o fim daquele trecho.
     *
     * <p>Olha um acima e um abaixo junto com o nível: a rua de vila sobe e
     * desce, e exigir o mesmo y faria toda ladeira parecer uma ponta.
     */
    static Optional<Direction> openSideOf(
            ServerWorld world, UUID colonyId, BlockPos road) {
        for (Direction side : Direction.Type.HORIZONTAL) {
            if (!isRoadNear(world, colonyId, road.offset(side.getOpposite()), road.getY())) {
                continue;
            }

            if (isRoadNear(world, colonyId, road.offset(side), road.getY())) {
                continue;
            }

            return Optional.of(side);
        }

        return Optional.empty();
    }

    static boolean isRoadNear(
            ServerWorld world, UUID colonyId, BlockPos column, int aroundY) {
        for (int dy = RoadExtension.MAX_STEP; dy >= -RoadExtension.MAX_STEP; dy--) {
            BlockPos at = new BlockPos(column.getX(), aroundY + dy, column.getZ());

            if (BuildSiteScanner.isRoadArea(world, colonyId, at)) {
                return true;
            }
        }

        return false;
    }
}
