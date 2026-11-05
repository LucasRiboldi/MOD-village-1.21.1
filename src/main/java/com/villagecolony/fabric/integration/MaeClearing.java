package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.WorkArea;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.world.ColonyTrees;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A obra MAE limpa o próprio terreno — Regra 200 (autor, 2026-11-05): árvore natural, folha e mato alto da região não recusam
 * mais o lote. Tudo é conferido antes do primeiro corte; o corte vem antes da reserva, e o tronco cai como item.
 *
 * <p>A espera de {@value #WAIT} tiques deixa as folhas vizinhas recalcularem a distância; então a prova das seções da região
 * passa a valer a partir da limpeza (ADR-044, item 9) e a obra começa. Uma limpeza por vez, só na sessão: depois de reiniciar,
 * o pedido se refaz à mão.
 */
final class MaeClearing {

    private static final int WAIT = 40;

    /** Até onde a folha de um tronco cortado pode ficar: além disso ela nem pertence a ele. */
    private static final int CANOPY = 6;

    private static final int TALLEST_TRUNK = 40;

    record Pending(ServerWorld world, MaeWorldWriter writer, UUID colonyId, ResourceId model, BlockPos origin,
                   int quarterTurns, @Nullable UUID owner, WorkArea area, long readyAt) {
    }

    private static @Nullable Pending pending;

    static {
        ServerMemory.register(MaeClearing.class, MaeClearing::forget);
    }

    private MaeClearing() {
    }

    private static void forget() {
        pending = null;
    }

    static boolean isBusy() {
        return pending != null;
    }

    /** A limpeza que já pode virar obra, tirada da fila. */
    static Optional<Pending> takeDue(long now) {
        Pending due = pending;

        if (due == null || now < due.readyAt()) {
            return Optional.empty();
        }

        pending = null;

        return Optional.of(due);
    }

    static void schedule(Pending next) {
        pending = next;
    }

    static long readyAt(ServerWorld world) {
        return world.getTime() + WAIT;
    }

    /**
     * O que a limpeza tiraria desta região; nada no mundo muda aqui.
     *
     * @throws IllegalStateException com o mesmo motivo da reserva, se a região não serve nem limpa
     */
    static List<BlockPos> survey(ServerWorld world, MaeWorldWriter writer, UUID colonyId, WorkArea area) {
        MaeProtection.inspectClearing(world, colonyId, area);
        MaeGenerationEvidence.inspect(world, area, true, 128);

        String dimension = world.getRegistryKey().getValue().toString();

        for (var other : writer.store().regions()) {
            if (!MaeHandover.isHandedOver(other) && dimension.equals(other.getString("dimension"))
                    && area.intersects(MaeWorldWriter.area(other))) {
                throw new IllegalStateException("overlapping_region");
            }
        }

        List<BlockPos> growth = new ArrayList<>();

        for (BlockPos pos : BlockPos.iterate(area.min().x(), area.min().y(), area.min().z(),
                area.max().x(), area.max().y(), area.max().z())) {
            BlockState state = world.getBlockState(pos);

            if (!MaeProtection.terrain(state) && !MaeProtection.vegetation(state) && isGrowth(world, pos, state)) {
                growth.add(pos.toImmutable());
            }
        }

        return growth;
    }

    /** Se este bloco a limpeza pode tirar: nunca o de obra, de peça da vila gerada ou posto à mão. */
    static boolean isGrowth(ServerWorld world, BlockPos pos, BlockState state) {
        if (!state.getFluidState().isEmpty() || state.isAir()) {
            return false;
        }

        if (LotGrowth.isLooseGrowth(state)) {
            return LotGrowth.isClearableOnSite(world, pos, state);
        }

        return isPlant(pos, state) && BlockProtection.mayBreak(world, pos, state);
    }

    private static boolean isPlant(BlockPos pos, BlockState state) {
        return state.isIn(BlockTags.REPLACEABLE_BY_TREES) || state.isIn(BlockTags.FLOWERS)
                || (state.isIn(BlockTags.SAPLINGS) && !ColonyTrees.isPlanted(pos.down()))
                || state.isOf(Blocks.SWEET_BERRY_BUSH) || state.isOf(Blocks.RED_MUSHROOM) || state.isOf(Blocks.BROWN_MUSHROOM)
                || state.isOf(Blocks.SNOW) || state.isOf(Blocks.SUGAR_CANE) || state.isOf(Blocks.CACTUS)
                || state.isOf(Blocks.PUMPKIN) || state.isOf(Blocks.MELON) || state.isOf(Blocks.MOSS_CARPET)
                || state.isOf(Blocks.BEE_NEST);
    }

    /** Tira o que o levantamento achou, o resto do tronco acima da região e a copa dele, para não sobrar folha secando. */
    static void clear(ServerWorld world, List<BlockPos> growth) {
        List<BlockPos> trunks = new ArrayList<>();

        for (BlockPos pos : growth) {
            if (remove(world, pos)) {
                trunks.add(pos);
            }
        }

        for (BlockPos log : trunks) {
            climb(world, log);
            canopy(world, log);
        }
    }

    /** @return se era tronco */
    private static boolean remove(ServerWorld world, BlockPos pos) {
        boolean log = world.getBlockState(pos).isIn(BlockTags.LOGS);

        if (log) {
            world.breakBlock(pos, true);
        } else {
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
        }

        return log;
    }

    private static void climb(ServerWorld world, BlockPos log) {
        for (int height = 1; height <= TALLEST_TRUNK; height++) {
            BlockPos above = log.up(height);
            BlockState state = world.getBlockState(above);

            if (!LotGrowth.isClearableOnSite(world, above, state)) {
                return;
            }

            if (!state.isAir()) {
                remove(world, above);
            }
        }
    }

    private static void canopy(ServerWorld world, BlockPos log) {
        for (BlockPos pos : BlockPos.iterate(log.add(-CANOPY, -CANOPY, -CANOPY), log.add(CANOPY, CANOPY, CANOPY))) {
            BlockState state = world.getBlockState(pos);

            if (state.isIn(BlockTags.LEAVES) && LotGrowth.isClearableOnSite(world, pos, state)) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
            }
        }
    }
}
