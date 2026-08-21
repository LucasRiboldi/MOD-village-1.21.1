package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A rua cresce com a vila — a Regra 15, 2026-08-21.
 *
 * <pre>
 * quando não houver mais lote livre encostado em rua, o construtor
 * estende a rua — e o lote novo nasce na beira do trecho novo
 * </pre>
 *
 * <p>Era a pendência declarada no cabeçalho do {@link BuildSiteScanner}
 * desde 08-14: "a vila cresce enquanto houver beira de rua livre, e para
 * quando não houver". É esse <b>para</b> que esta regra tira.
 *
 * <p><b>A ponta sai da varredura que já acontece.</b> Procurar a ponta da
 * rua numa varredura própria custaria o raio de 64 inteiro — dezessete
 * passagens, oito minutos e meio de relógio — logo depois da varredura de
 * lote que acabou de falhar percorrendo exatamente as mesmas colunas.
 * Então quem acha a ponta é a própria busca de lote: ela já visita cada
 * coluna e já pergunta se aquilo é rua. O que se acrescenta é uma
 * pergunta a mais nas poucas colunas que <b>são</b>, e a resposta fica
 * guardada para quando a varredura terminar sem lote.
 *
 * <p><b>Da ponta mais distante do centro</b>, que é a frase da regra:
 * estrada que cresce pelo meio racha a vila.
 *
 * <p><b>Calçar não custa material</b> — decisão registrada no Backlog. O
 * que a colônia gasta é a passagem, e o teto de {@link #STRETCH} blocos é
 * o "trecho curto por casa" da regra: rua que cresce sozinha vira rua sem
 * nada em volta.
 */
public final class RoadExtension {

    /**
     * Quantos blocos por vez.
     *
     * <p>Curto de propósito. A rua só cresce quando a vila não tem mais
     * onde construir, e cinco blocos abrem beira para uma casa — que é a
     * medida da regra: um trecho por casa.
     */
    public static final int STRETCH = 5;

    /**
     * Quanto a rua pode subir ou descer por bloco.
     *
     * <p>Um. Rua que sobe mais que isso por passo não é rua, é escada — e
     * o aldeão que a percorre fica preso no degrau.
     */
    private static final int MAX_STEP = 1;

    /** A ponta mais distante que a varredura desta colônia viu. */
    private static final Map<UUID, End> ENDS = new HashMap<>();

    /** Uma ponta de rua, e para que lado ela continuaria. */
    private record End(BlockPos at, Direction towards, double fromCenter) {
    }

    /** O que aconteceu na tentativa de estender. */
    public enum Outcome {

        /** Varreu tudo e não há ponta de rua que dê para prolongar. */
        NO_END,

        /** Achou a ponta, e nem o primeiro bloco pôde ser calçado. */
        BLOCKED,

        /** Calçou. O lote novo nasce na beira deste trecho. */
        EXTENDED
    }

    private RoadExtension() {
    }

    /**
     * Esta coluna é rua — vale a pena olhar se ela é ponta?
     *
     * <p>Chamada pela busca de lote, uma vez por coluna de rua. Guarda a
     * mais distante do centro e descarta o resto: só uma vai ser
     * prolongada, e guardar todas seria uma lista que cresce com o
     * tamanho da vila sem servir a nada.
     */
    static void consider(ServerWorld world, UUID colonyId, BlockPos road, BlockPos center) {
        Optional<Direction> towards = openSideOf(world, road);

        if (towards.isEmpty()) {
            return;
        }

        double distance = center.getSquaredDistance(road);

        End best = ENDS.get(colonyId);

        if (best == null || distance > best.fromCenter()) {
            ENDS.put(colonyId, new End(road, towards.get(), distance));
        }
    }

    /** Esquece a ponta desta colônia. A varredura recomeçou. */
    public static void forgetEnds(UUID colonyId) {
        ENDS.remove(colonyId);
    }

    /** Esvazia o registro. Chamado ao parar o servidor. */
    public static void clearAll() {
        ENDS.clear();
    }

    /**
     * Prolonga a rua desta colônia, a partir da ponta mais distante.
     *
     * <p>Só faz sentido depois de uma varredura de lote que terminou sem
     * achar nada: é ela que enche o registro de pontas, e é o "não há
     * mais lote" dela que autoriza a rua a crescer.
     *
     * @param paving o bloco com que esta vila calça — ver
     *     {@link VillageRoad}
     */
    public static Outcome extend(ServerWorld world, UUID colonyId, ResourceId paving) {
        End end = ENDS.remove(colonyId);

        if (end == null) {
            return Outcome.NO_END;
        }

        Optional<Block> block = MinecraftTypeAdapter.toBlock(paving);

        if (block.isEmpty()) {
            return Outcome.NO_END;
        }

        int laid = pave(world, end, block.get());

        if (laid == 0) {
            return Outcome.BLOCKED;
        }

        VillageColonyMod.LOGGER.info(
                "Colony {} extended the road {} blocks {} from {}",
                colonyId,
                laid,
                end.towards(),
                end.at());

        return Outcome.EXTENDED;
    }

    /**
     * Assenta o trecho, bloco a bloco, e para no primeiro que recusar.
     *
     * <p>Para de verdade, e não pula: rua com buraco no meio é rua que o
     * aldeão não atravessa, e a beira depois do buraco não serve de lote.
     *
     * @return quantos blocos entraram
     */
    private static int pave(ServerWorld world, End end, Block paving) {
        BlockPos previous = end.at();

        int laid = 0;

        for (int step = 0; step < STRETCH; step++) {
            BlockPos ahead = previous.offset(end.towards());

            Optional<BlockPos> ground = groundNear(world, ahead, previous.getY());

            if (ground.isEmpty()) {
                return laid;
            }

            BlockPos at = ground.get();

            BlockState state = world.getBlockState(at);

            if (VillageRoad.isPaving(world, state)) {
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

            previous = at;

            laid++;
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
    private static Optional<BlockPos> groundNear(ServerWorld world, BlockPos column, int fromY) {
        for (int dy = MAX_STEP; dy >= -MAX_STEP; dy--) {
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
    private static Optional<Direction> openSideOf(ServerWorld world, BlockPos road) {
        for (Direction side : Direction.Type.HORIZONTAL) {
            if (!isRoadNear(world, road.offset(side.getOpposite()), road.getY())) {
                continue;
            }

            if (isRoadNear(world, road.offset(side), road.getY())) {
                continue;
            }

            return Optional.of(side);
        }

        return Optional.empty();
    }

    private static boolean isRoadNear(ServerWorld world, BlockPos column, int aroundY) {
        for (int dy = MAX_STEP; dy >= -MAX_STEP; dy--) {
            BlockPos at = new BlockPos(column.getX(), aroundY + dy, column.getZ());

            if (VillageRoad.isPaving(world, world.getBlockState(at))) {
                return true;
            }
        }

        return false;
    }
}
