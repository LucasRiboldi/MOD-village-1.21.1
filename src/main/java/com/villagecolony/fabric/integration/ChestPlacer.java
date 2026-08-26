package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.service.VillageDetector;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * O baú que faltava ao lado da cama — Regra 8.
 *
 * <p>A regra do autor, de 2026-08-15: <b>toda vila gerada pelo Minecraft
 * ganha um baú ao lado de cada cama</b>, e cada aldeão fica vinculado a
 * uma cama e ao baú mais perto dela.
 *
 * <p>Até aqui o {@code Storage-System.md} dizia o contrário — "o sistema
 * não cria casas ou baús automaticamente" — e o resultado foi o E16: na
 * sessão de 2026-08-15 dois lenhadores e dois fabricantes passaram doze
 * minutos sem baú, pegando tarefa e devolvendo a cada ciclo, numa vila
 * onde simplesmente não havia baú livre ao alcance das camas deles.
 *
 * <p><b>De onde vem o baú.</b> Do nada, e isso é exceção declarada. A
 * regra de arquitetura do {@code Construction-System.md} — a colônia não
 * cria recurso — continua valendo para tudo o mais: o que a obra consome
 * sai de baú, e nada do que o trabalhador produz nasce do vazio. Este
 * baú não é produção da colônia; é completar o que a geração de vila do
 * Minecraft deixou incompleto. Decisão do autor na mesma data.
 *
 * <p><b>O cuidado.</b> Isto escreve no mundo do jogador, que é a coisa
 * mais perigosa que o mod faz. Por isso a escolha do lugar recusa mais
 * do que aceita: só entra em bloco substituível, só com chão firme
 * embaixo, só com espaço livre em cima — baú com bloco opaco em cima não
 * abre, nem para o aldeão nem para o jogador —, e nunca encostado onde
 * já existe outro baú, que viraria um baú duplo de dono ambíguo.
 */
public final class ChestPlacer {

    /**
     * As camas sem lugar para baú, e desde quando — E24, 2026-08-25.
     *
     * <p>Sem isto a colônia repergunta a cada ciclo, para sempre. Na
     * sessão de 2026-08-25 três camas coladas bastaram para
     * {@code No room for a chest beside the bed at -7580, 64, -5129}
     * sair a cada trinta segundos até o servidor parar — oito posições
     * relidas por ciclo e por cama, e uma linha de log que só ensina que
     * o mod não muda de ideia.
     *
     * <p><b>E a recusa envelhece</b>, que é a Regra 23: o jogador tira o
     * bloco, abre o vão, muda a cama de lugar. Dez ciclos depois a cama
     * volta a ser tentada.
     */
    private static final Map<BlockPos, Long> REFUSED = new HashMap<>();

    /** Por quantos ticks uma cama recusada fica de fora. Dez ciclos. */
    private static final int REFUSED_MEMORY = 10 * VillageDetector.CYCLE_TICKS;

    /** Teto, e não regra: vila grande tem muita cama sem vão ao lado. */
    private static final int MAX_REFUSED = 1024;

    private ChestPlacer() {
    }

    /** Esquece as recusas. Chamado ao parar o servidor. */
    public static void clearAll() {
        REFUSED.clear();
    }

    /**
     * Põe um baú ao lado desta cama, se houver lugar bom.
     *
     * <p>Percorre os quatro vizinhos horizontais no nível da cama e, se
     * nenhum servir, os quatro um bloco abaixo — chão de vila vanilla tem
     * degrau, e é o mesmo motivo pelo qual {@code ChestScanner} aceita um
     * bloco de diferença entre cama e baú.
     *
     * @return onde o baú foi posto, ou vazio quando nenhum vizinho serve
     */
    public static Optional<BlockPos> placeBeside(ServerWorld world, BlockPos bed) {
        if (isRefused(world, bed)) {
            // Já se olhou, e não havia vão. Volta a valer em dez ciclos —
            // ver REFUSED.
            return Optional.empty();
        }

        for (int drop = 0; drop <= 1; drop++) {
            for (Direction side : Direction.Type.HORIZONTAL) {
                BlockPos spot = bed.offset(side).down(drop);

                if (!isGoodSpot(world, spot)) {
                    continue;
                }

                world.setBlockState(spot, Blocks.CHEST.getDefaultState());

                VillageColonyMod.LOGGER.info(
                        "Put a chest at {} for the bed at {}",
                        spot.toShortString(),
                        bed.toShortString());

                return Optional.of(spot);
            }
        }

        refuse(world, bed);

        VillageColonyMod.LOGGER.info(
                "No room for a chest beside the bed at {} — not asking again for {} cycles",
                bed.toShortString(),
                REFUSED_MEMORY / VillageDetector.CYCLE_TICKS);

        return Optional.empty();
    }

    /** Se esta cama está de castigo, e ainda não envelheceu. */
    private static boolean isRefused(ServerWorld world, BlockPos bed) {
        Long since = REFUSED.get(bed);

        if (since == null) {
            return false;
        }

        if (world.getTime() - since < REFUSED_MEMORY) {
            return true;
        }

        REFUSED.remove(bed);

        return false;
    }

    /** Anota que esta cama não tinha vão agora. */
    private static void refuse(ServerWorld world, BlockPos bed) {
        if (REFUSED.size() >= MAX_REFUSED) {
            REFUSED.clear();
        }

        REFUSED.put(bed.toImmutable(), world.getTime());
    }

    /**
     * Se este lugar aceita um baú sem tirar nada de ninguém.
     *
     * <p>Cinco recusas, e todas existem por um motivo próprio:
     *
     * <ul>
     *   <li>o lugar precisa ser substituível — ar, grama alta, flor. Um
     *       bloco do jogador fica onde está, que é a Regra 3;
     *   <li>não pode ser cama: os dois vizinhos de uma cama incluem a
     *       outra metade dela, e trocar meia cama por um baú tiraria o
     *       aldeão de casa;
     *   <li>precisa de chão firme embaixo, senão o baú fica flutuando —
     *       e, sobre areia ou cascalho, cai;
     *   <li>precisa de espaço livre em cima, senão não abre;
     *   <li>não pode encostar noutro baú, que viraria um baú duplo e
     *       daria dois donos ao mesmo inventário.
     * </ul>
     */
    private static boolean isGoodSpot(ServerWorld world, BlockPos spot) {
        BlockState here = world.getBlockState(spot);

        if (!here.isReplaceable() || here.getBlock() instanceof BedBlock) {
            return false;
        }

        if (!world.getBlockState(spot.down()).isSolidBlock(world, spot.down())) {
            return false;
        }

        if (world.getBlockState(spot.up()).isSolidBlock(world, spot.up())) {
            return false;
        }

        for (Direction side : Direction.Type.HORIZONTAL) {
            if (world.getBlockState(spot.offset(side)).isOf(Blocks.CHEST)) {
                return false;
            }
        }

        return true;
    }
}
