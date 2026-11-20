package com.villagecolony.fabric.work;

import com.villagecolony.core.coordination.SupplyException;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.CropPatch;
import com.villagecolony.fabric.integration.Supplies;
import net.minecraft.block.BlockState;
import net.minecraft.block.ComposterBlock;
import net.minecraft.block.CropBlock;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldEvents;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * O pó de osso na plantação — Regra 310.
 *
 * <p>O fazendeiro sem colheita nem canteiro põe uma aplicação em cada pé verde da plantação, o mais perto primeiro, e
 * nenhum pé recebe a segunda antes de todos receberem a primeira. Fechada a volta, o que cresceu é colhido e
 * replantado (a colheita ganha de tudo no {@link FarmerChores#findWork}), e outra volta começa. A cada dia a volta
 * recomeça do zero.
 *
 * <p>O pó sai, nesta ordem: da composteira do fazendeiro cheia; dos baús da vila, que guardam sempre
 * {@value #CHEST_RESERVE} (acima disso ele pega); e só sem nenhum dos dois ele surge
 * ({@link SupplyException#BONE_MEAL_FOR_THE_FARM}), de {@value #SURGE} em {@value #SURGE} na conta do {@code VC_SUPPLY}.
 * Uma aplicação a cada {@value #EVERY} tiques por fazendeiro.
 */
final class BoneMealRound {

    static {
        ServerMemory.register(BoneMealRound.class, BoneMealRound::clearAll);
    }

    /** O pó de osso que os baús da vila guardam sempre. */
    static final int CHEST_RESERVE = 16;

    /** Quanto pó surge de cada vez, para a linha {@code VC_SUPPLY} não sair a cada aplicação. */
    static final int SURGE = 16;

    /** Uma aplicação a cada segundo, como o compostor ({@link FarmerCompost#EVERY}). */
    static final int EVERY = 20;

    /** Folga de altura na leitura da roça própria. */
    private static final int SLOPE = 2;

    /** De onde veio o pó de uma aplicação. */
    enum Source { COMPOSTER, CHEST, APPEARED }

    /** A volta de uma colônia: o dia dela e os pés que já receberam. */
    private static final class Round {

        long day;

        final Set<BlockPos> fed = new HashSet<>();

        Round(long day) {
            this.day = day;
        }
    }

    private static final Map<UUID, Round> ROUNDS = new HashMap<>();

    /** O pó surgido que ainda não foi usado, por colônia. */
    private static final Map<UUID, Integer> APPEARED = new HashMap<>();

    private static final Map<UUID, Long> LAST = new HashMap<>();

    /** Até quando o fazendeiro que não achou pé verde não procura de novo. */
    private static final Map<UUID, Long> QUIET = new HashMap<>();

    private BoneMealRound() {
    }

    /** Sem colheita nem canteiro: o próximo pé verde da volta vira o alvo dele. */
    static boolean aim(ServerWorld world, VillagerEntity villager, FarmerWork.Job job) {
        UUID farmer = villager.getUuid();
        UUID colonyId = job.task.colonyId();

        if (world.getTime() < QUIET.getOrDefault(farmer, Long.MIN_VALUE)) {
            return false;
        }

        Optional<BlockPos> crop = next(world, colonyId, cropsOf(world, colonyId, farmer), villager.getBlockPos(),
                dayOf(world));

        if (crop.isEmpty()) {
            QUIET.put(farmer, world.getTime() + EVERY);

            return false;
        }

        IdleLog.clear(colonyId, FarmerWork.SUBJECT);
        job.target = crop.get();
        job.chore = FarmerWork.Chore.FERTILIZE;
        job.stalled = 0;
        WorkTargets.set(farmer, job.target);

        return true;
    }

    /** Ao alcance do pé: uma aplicação, na vez dele; o pé que amadureceu volta a valer para a colheita. */
    static void apply(ServerWorld world, VillagerEntity villager, FarmerWork.Job job, WorkerStorage storage) {
        if (!ready(villager.getUuid(), world.getTime())) {
            return;
        }

        UUID colonyId = job.task.colonyId();
        Optional<Source> source = feed(world, colonyId, job.target, composterOf(world, villager),
                ColonyChests.ownFirst(world, colonyId, storage.chestPosition()));

        if (source.isPresent()) {
            villager.swingHand(Hand.MAIN_HAND, true);
            ActionJournal.action(villager.getUuid(), "BONE_MEAL", "crop", 1, job.target, source.get().name());
            WorkerStrikes.worked(villager.getUuid(), job.task);

            if (CropPatch.isRipe(world.getBlockState(job.target))) {
                job.ownFarmQuietUntil = 0;
                FieldRest.thereIsWorkAgain(colonyId);
            }
        }

        FarmerWork.release(villager.getUuid(), job);
    }

    /** Se este fazendeiro já pode fazer outra aplicação; quando pode, conta a vez dele. */
    static boolean ready(UUID farmer, long now) {
        Long last = LAST.get(farmer);

        if (last != null && now - last < EVERY) {
            return false;
        }

        LAST.put(farmer, now);

        return true;
    }

    /** Os pés da plantação deste fazendeiro: a roça dele e a roça conhecida da vila, em chunk carregado. */
    static Collection<BlockPos> cropsOf(ServerWorld world, UUID colonyId, UUID farmer) {
        Set<BlockPos> crops = new LinkedHashSet<>();
        Optional<BlockBox> farm = FarmOwners.farmOf(farmer);

        if (farm.isPresent() && world.isChunkLoaded(farm.get().getMinX() >> 4, farm.get().getMinZ() >> 4)
                && world.isChunkLoaded(farm.get().getMaxX() >> 4, farm.get().getMaxZ() >> 4)) {
            BlockBox box = farm.get();

            for (BlockPos at : BlockPos.iterate(box.getMinX(), box.getMinY() - SLOPE, box.getMinZ(),
                    box.getMaxX(), box.getMaxY() + SLOPE, box.getMaxZ())) {
                if (world.getBlockState(at).getBlock() instanceof CropBlock) {
                    crops.add(at.toImmutable());
                }
            }
        }

        for (BlockPos farmland : CropPatch.knownOf(colonyId)) {
            if (world.isChunkLoaded(farmland.getX() >> 4, farmland.getZ() >> 4)) {
                crops.add(farmland.up());
            }
        }

        return crops;
    }

    /**
     * O próximo pé desta volta: o verde mais perto de {@code from} que ainda não recebeu. Sem nenhum, a volta fecha e
     * outra começa; dia novo também começa outra.
     */
    static Optional<BlockPos> next(ServerWorld world, UUID colonyId, Collection<BlockPos> crops, BlockPos from, long day) {
        Round round = ROUNDS.computeIfAbsent(colonyId, id -> new Round(day));

        if (round.day != day) {
            round.day = day;
            round.fed.clear();
        }

        BlockPos nearest = null;
        BlockPos nearestFed = null;

        for (BlockPos crop : crops) {
            if (!isGreen(world.getBlockState(crop))) {
                continue;
            }

            if (!round.fed.contains(crop)) {
                nearest = closer(from, nearest, crop);
            } else {
                nearestFed = closer(from, nearestFed, crop);
            }
        }

        if (nearest == null && nearestFed != null) {
            round.fed.clear();
            nearest = nearestFed;
        }

        return Optional.ofNullable(nearest);
    }

    /** Uma aplicação neste pé; diz de onde veio o pó, ou nada se o pé não é lavoura verde. */
    static Optional<Source> feed(ServerWorld world, UUID colonyId, BlockPos crop, Optional<BlockPos> composter,
                                 List<ColonyPos> chests) {
        BlockState state = world.getBlockState(crop);

        if (!isGreen(state)) {
            return Optional.empty();
        }

        Source source = take(world, colonyId, composter, chests);

        ((CropBlock) state.getBlock()).grow(world, world.getRandom(), crop, state);
        world.syncWorldEvent(WorldEvents.BONE_MEAL_USED, crop, 15);
        ROUNDS.computeIfAbsent(colonyId, id -> new Round(world.getTimeOfDay() / 24_000L)).fed.add(crop.toImmutable());

        return Optional.of(source);
    }

    /** A composteira do fazendeiro: o local de trabalho dele, se é composteira. */
    static Optional<BlockPos> composterOf(ServerWorld world, VillagerEntity villager) {
        return villager.getBrain().getOptionalRegisteredMemory(MemoryModuleType.JOB_SITE)
                .filter(site -> site.dimension().equals(world.getRegistryKey()))
                .map(site -> site.pos())
                .filter(at -> world.getBlockState(at).getBlock() instanceof ComposterBlock);
    }

    /** Se este bloco é lavoura que ainda cresce. */
    static boolean isGreen(BlockState state) {
        return state.getBlock() instanceof CropBlock crop && !crop.isMature(state);
    }

    /** O dia do mundo, que recomeça a volta. */
    static long dayOf(ServerWorld world) {
        return world.getTimeOfDay() / 24_000L;
    }

    private static Source take(ServerWorld world, UUID colonyId, Optional<BlockPos> composter, List<ColonyPos> chests) {
        if (composter.isPresent()) {
            BlockState state = world.getBlockState(composter.get());

            if (state.getBlock() instanceof ComposterBlock && state.get(ComposterBlock.LEVEL) == ComposterBlock.MAX_LEVEL + 1) {
                world.setBlockState(composter.get(), state.with(ComposterBlock.LEVEL, 0));
                world.playSound(null, composter.get(), SoundEvents.BLOCK_COMPOSTER_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);

                return Source.COMPOSTER;
            }
        }

        if (!KeepLedger.atMost("bone_meal", ColonyChests.countIn(world, chests, Items.BONE_MEAL), CHEST_RESERVE)
                && ColonyChests.withdraw(world, chests, Items.BONE_MEAL, 1) == 1) {
            return Source.CHEST;
        }

        int left = APPEARED.getOrDefault(colonyId, 0);

        if (left == 0) {
            Supplies.record(SupplyException.BONE_MEAL_FOR_THE_FARM, colonyId, "bone_meal", SURGE);
            left = SURGE;
        }

        APPEARED.put(colonyId, left - 1);

        return Source.APPEARED;
    }

    private static BlockPos closer(BlockPos from, BlockPos best, BlockPos candidate) {
        return best == null || from.getSquaredDistance(candidate) < from.getSquaredDistance(best) ? candidate : best;
    }

    /** Esquece a volta e o pó surgido de uma colônia. Para o teste de jogo. */
    static void forget(UUID colonyId) {
        ROUNDS.remove(colonyId);
        APPEARED.remove(colonyId);
    }

    static void clearAll() {
        ROUNDS.clear();
        APPEARED.clear();
        LAST.clear();
        QUIET.clear();
    }
}
