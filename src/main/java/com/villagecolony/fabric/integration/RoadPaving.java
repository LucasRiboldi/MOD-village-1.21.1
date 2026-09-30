package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
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

        return pave(world, colonyId, end, paving, new PavingRefusals());
    }

    /**
     * O mesmo, anotando em {@code refusals} o que parou o trecho — 2026-09-30.
     * Ver {@link PavingRefusals}.
     */
    static List<ColonyPos> pave(
            ServerWorld world, UUID colonyId, RoadExtension.End end, Block paving,
            PavingRefusals refusals) {

        BlockPos previous = end.at();

        List<ColonyPos> laid = new ArrayList<>();

        for (int step = 0; step < RoadExtension.STRETCH; step++) {
            BlockPos ahead = previous.offset(end.towards());

            Optional<BlockPos> ground = groundNear(world, ahead, previous.getY());

            if (ground.isEmpty()) {
                refusals.record("no ground within a step");

                return laid;
            }

            BlockPos at = ground.get();

            BlockState state = world.getBlockState(at);

            if (RoadIndex.isRoadArea(world, colonyId, at)) {
                // Já é rua: a ponta encostou noutro trecho. Segue por
                // cima dela sem gastar nada, que é o que dois calçamentos
                // que se encontram fazem.
                previous = at;

                continue;
            }

            // <b>Caminho que esta colônia não calçou também é rua</b> — F3,
            // 2026-09-30. No playtest daquele dia, 9 a 11 das 24 pontas
            // recusadas bateram em dirt_path de outra colônia da mesma vila:
            // a ponta o via como chão proibido e a rua não crescia. Seguir
            // por cima não troca bloco nenhum; a coluna entra no índice e a
            // rua, com os arredores, passa a ser da vila que cresce.
            if (VillageRoad.isPaving(world, state)) {
                RoadIndex.remember(colonyId, at);

                previous = at;

                laid.add(MinecraftTypeAdapter.toColonyPos(at));

                continue;
            }

            // A Regra 3 nas duas pontas, e aqui ela morde: a vila gerada
            // é feita de bloco que passaria por chão.
            Optional<String> refusal = refusalAt(world, at, state);

            if (refusal.isPresent()) {
                refusals.record(refusal.get());

                return laid;
            }

            // A planta em cima sai junto: rua não tem grama por cima.
            if (!world.getBlockState(at.up()).isAir()) {
                world.setBlockState(at.up(), net.minecraft.block.Blocks.AIR.getDefaultState());
            }

            world.setBlockState(at, paving.getDefaultState());

            // O índice de ruas precisa saber da beira nova — 2026-08-27.
            // A rua cresce justamente quando não houve lote, e o lote
            // novo nasce encostado no que acabou de ser calçado: índice
            // que não soubesse disto nunca mais acharia nada.
            RoadIndex.remember(colonyId, at);

            previous = at;

            laid.add(MinecraftTypeAdapter.toColonyPos(at));
        }

        return laid;
    }

    /**
     * Por que esta coluna não recebe calçamento, se não receber.
     *
     * <p>As quatro perguntas de antes, na mesma ordem, agora com nome: a
     * primeira que disser não é a que o registro conta.
     */
    private static Optional<String> refusalAt(ServerWorld world, BlockPos at, BlockState state) {
        // <b>O chão do bioma não é peça de vila</b> — pedido do autor,
        // 2026-09-30: estradas nascem para fora da vila. A caixa da peça de
        // rua inclui a grama em volta do caminho, e no playtest das 02:45 era
        // ela que parava 8 a 12 das 24 pontas. É a mesma exceção que o lote
        // tem desde 2026-09-18 (LotLevel): cerca, escada e o resto da peça
        // continuam protegidos pela Regra 3.
        if (BlockProtection.isVillageOriginal(world, at) && !LotGround.isBiomeGround(world, at)) {
            return Optional.of("village-original block (" + nameOf(state) + ")");
        }

        if (BlockProtection.isColonyBuilt(at)) {
            return Optional.of("colony-built block (" + nameOf(state) + ")");
        }

        if (!LotGround.isNaturalGround(state)) {
            return Optional.of("not natural ground (" + nameOf(state) + ")");
        }

        BlockState above = world.getBlockState(at.up());

        // Planta e flor saem ao calçar; o que não sai é bloco, nem água.
        if (!isClearable(above)) {
            return Optional.of("something above (" + nameOf(above) + ")");
        }

        return Optional.empty();
    }

    private static String nameOf(BlockState state) {
        return Registries.BLOCK.getId(state.getBlock()).getPath();
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

            // Planta não é chão — 2026-09-30. A procura parava na grama curta
            // e a tratava como o chão; água continua parando, porque não se
            // calça rua sob ela.
            if (isClearable(world.getBlockState(at))) {
                continue;
            }

            return Optional.of(at);
        }

        return Optional.empty();
    }

    /** Ar, planta ou flor sem fluido: o que a rua atravessa e tira. */
    private static boolean isClearable(BlockState state) {
        return LotGround.isNothing(state) && state.getFluidState().isEmpty();
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

    /**
     * Laterais livres de um trecho reto, usadas somente quando as pontas nao
     * conseguem levar a rua a solo novo.
     */
    static List<Direction> openBranchSidesOf(
            ServerWorld world, UUID colonyId, BlockPos road) {
        boolean northSouth = isRoadNear(world, colonyId, road.north(), road.getY())
                && isRoadNear(world, colonyId, road.south(), road.getY());
        boolean eastWest = isRoadNear(world, colonyId, road.east(), road.getY())
                && isRoadNear(world, colonyId, road.west(), road.getY());

        if (northSouth == eastWest) {
            return List.of();
        }

        List<Direction> sides = northSouth
                ? List.of(Direction.EAST, Direction.WEST)
                : List.of(Direction.NORTH, Direction.SOUTH);

        return sides.stream()
                .filter(side -> !isRoadNear(world, colonyId, road.offset(side), road.getY()))
                .toList();
    }

    static boolean isRoadNear(
            ServerWorld world, UUID colonyId, BlockPos column, int aroundY) {
        for (int dy = RoadExtension.MAX_STEP; dy >= -RoadExtension.MAX_STEP; dy--) {
            BlockPos at = new BlockPos(column.getX(), aroundY + dy, column.getZ());

            if (RoadIndex.isRoadArea(world, colonyId, at)) {
                return true;
            }
        }

        return false;
    }
}
