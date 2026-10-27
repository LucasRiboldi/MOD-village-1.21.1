package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.integration.BlockProtection;
import com.villagecolony.fabric.integration.CropPatch;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O fazendeiro lavra canteiro novo — regra do autor, 2026-10-11: com nada maduro e
 * nenhum canteiro vazio, ele ara a terra ao lado da roça em vez de esperar o trigo
 * crescer (no playtest da 0.3.26 ele esperou 46% do expediente).
 *
 * <p>Só ao lado de terra arada que já existe, com água a até {@link #WATER} blocos
 * (a terra fica molhada, como no jogo), fora da rua e do que a vila ou a colônia
 * construiu, e com semente nos baús — o mesmo portão do semear: a roça cresce no
 * ritmo em que a colheita paga a semente. Até {@link #MOST_PER_FARMER} canteiros
 * novos por fazendeiro; o teto do trigo ({@code StockCeiling}) faz o resto.
 */
final class FarmerTill {

    static {
        ServerMemory.register(FarmerTill.class, FarmerTill::clearAll);
    }

    /** A que distância da água a terra arada fica molhada. */
    static final int WATER = 4;

    /** Até onde, a partir do fazendeiro, ele procura a borda da roça. */
    static final int REACH = 12;

    /** Um canteiro novo a cada cinco segundos, no máximo. */
    static final int EVERY = 100;

    /** Quantos canteiros novos um fazendeiro abre. */
    static final int MOST_PER_FARMER = 32;

    private static final Map<UUID, Long> LAST = new HashMap<>();

    private static final Map<UUID, Integer> TILLED = new HashMap<>();

    private FarmerTill() {
    }

    static void clearAll() {
        LAST.clear();
        TILLED.clear();
    }

    /**
     * Ara um canteiro novo na borda da roça e o deixa como alvo de semear.
     *
     * @return se ele arou
     */
    static boolean till(ServerWorld world, VillagerEntity villager, FarmerWork.Job job, WorkerStorage storage) {
        UUID id = villager.getUuid();
        long now = world.getTime();
        Long last = LAST.get(id);

        if ((last != null && now - last < EVERY) || TILLED.getOrDefault(id, 0) >= MOST_PER_FARMER
                || FarmerChores.seedChest(world, job.task.colonyId(), storage.chestPosition()).isEmpty()) {
            return false;
        }

        LAST.put(id, now);

        Optional<BlockPos> plot = nextPlot(world, villager.getBlockPos());

        if (plot.isEmpty()) {
            return false;
        }

        world.setBlockState(plot.get(), Blocks.FARMLAND.getDefaultState());
        com.villagecolony.fabric.integration.KnownFarmland.register(job.task.colonyId(), plot.get());
        villager.swingHand(Hand.MAIN_HAND, true);
        TILLED.merge(id, 1, Integer::sum);

        job.target = plot.get();
        job.chore = FarmerWork.Chore.SOW;
        job.stalled = 0;
        com.villagecolony.fabric.brain.WorkTargets.set(id, plot.get());

        VillageColonyMod.LOGGER.info("Farmer {} tilled a new plot at {} — nothing was ripe and no plot was empty",
                id.toString().substring(0, 8), plot.get().toShortString());

        return true;
    }

    /**
     * O chão perto da roça que serve de canteiro novo, a partir de {@code from}: colado num canteiro,
     * ou até dois além dele e um abaixo — deserto jogado, 2026-10-21: a roça da vila do deserto tem
     * borda de arenito cortado e areia um abaixo do lado de fora, e o canteiro novo nunca cabia; a
     * vila ficou com uma roça só e o pastor sem trigo para cruzar os bichos.
     */
    static Optional<BlockPos> nextPlot(ServerWorld world, BlockPos from) {
        for (BlockPos at : BlockPos.iterateOutwards(from, REACH, 3, REACH)) {
            if (!CropPatch.isFarmland(world.getBlockState(at))) {
                continue;
            }

            for (int reach = 1; reach <= 2; reach++) {
                for (Direction side : Direction.Type.HORIZONTAL) {
                    for (int dy = 0; dy >= -1; dy--) {
                        BlockPos next = at.offset(side, reach).up(dy).toImmutable();

                        if (fits(world, next)) {
                            return Optional.of(next);
                        }
                    }
                }
            }
        }

        return Optional.empty();
    }

    /** Terra, grama ou areia com ar em cima, água perto, longe da rua e de obra. */
    static boolean fits(ServerWorld world, BlockPos ground) {
        BlockState state = world.getBlockState(ground);
        boolean soil = state.isOf(Blocks.GRASS_BLOCK) || state.isOf(Blocks.DIRT);
        // A areia é o chão do deserto, e a da vila gerada também serve (BlockProtection.mayBuildOver).
        boolean sand = state.isIn(net.minecraft.registry.tag.BlockTags.SAND)
                && BlockProtection.mayBuildOver(world, ground, state);

        if (!((soil && BlockProtection.mayBreak(world, ground, state)) || sand)
                || !world.getBlockState(ground.up()).isAir()
                || BlockProtection.isColonyBuilt(ground)
                || pathNear(world, ground)) {
            return false;
        }

        for (BlockPos at : BlockPos.iterate(ground.add(-WATER, 0, -WATER), ground.add(WATER, 1, WATER))) {
            if (world.getFluidState(at).isIn(FluidTags.WATER)) {
                return true;
            }
        }

        return false;
    }

    /** Caminho encostado — de terra, ou a rua de arenito do deserto: a rua não vira roça. */
    private static boolean pathNear(ServerWorld world, BlockPos ground) {
        for (BlockPos at : BlockPos.iterate(ground.add(-1, -1, -1), ground.add(1, 1, 1))) {
            if (com.villagecolony.fabric.integration.VillageRoad.isPathBlock(world.getBlockState(at))
                    || com.villagecolony.fabric.integration.VillageRoad.isPaving(world, at.toImmutable())) {
                return true;
            }
        }

        return false;
    }
}
