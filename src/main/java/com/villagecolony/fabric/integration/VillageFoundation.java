package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.core.worker.service.ProfessionAssigner;
import com.villagecolony.core.worker.service.WorkerService;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.Heightmap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Garante a fundação física de uma colônia recém-detectada.
 *
 * <p>A profissão de colônia é uma decisão do mod, mas não pode existir
 * sem um aldeão real para ocupá-la. Quando uma vila vanilla nasce com
 * menos adultos que o piso da {@link ProfessionAssigner#FOUNDATION_ORDER},
 * esta classe completa a população com aldeões adultos, dá a cada um uma
 * cama exclusiva e deixa um baú ao lado. Só escreve em ar/blocos
 * substituíveis; blocos do jogador continuam sendo a fonte da verdade.
 */
public final class VillageFoundation {

    /** Raio de procura de uma posição segura ao redor do centro observado. */
    private static final int SEARCH_RADIUS = 16;

    private VillageFoundation() {
    }

    /** Resultado observável da passagem de garantia. */
    public record Result(int spawnedVillagers, int bedsPlaced) {
        public boolean changed() {
            return spawnedVillagers > 0 || bedsPlaced > 0;
        }
    }

    /**
     * Completa a população e as camas das funções fundamentais desta vila.
     *
     * <p>É idempotente: numa nova passagem, adultos e funções já presentes
     * não geram aldeões extras. Funções ausentes em um save antigo geram
     * somente a quantidade necessária para que o próximo ciclo as atribua.
     */
    public static Result ensure(
            ServerWorld world,
            Colony colony,
            ColonyPos around,
            WorkerService workers) {

        BlockPos center = MinecraftTypeAdapter.toBlockPos(around);
        Box area = Box.of(center.toCenterPos(), 128.0, 128.0, 128.0);
        List<VillagerEntity> villagers = world.getEntitiesByClass(
                VillagerEntity.class, area, VillagerEntity::isAlive);

        Map<UUID, VillagerEntity> byId = new HashMap<>();
        int adults = 0;

        for (VillagerEntity villager : villagers) {
            byId.put(villager.getUuid(), villager);

            if (!villager.isBaby()) {
                adults++;
            }
        }

        Set<ProfessionType> presentRoles = new HashSet<>();

        for (Worker worker : workers.ofColony(colony.id())) {
            worker.profession()
                    .map(ProfessionAssigner::foundationRole)
                    .filter(ProfessionAssigner.FOUNDATION_ORDER::contains)
                    .ifPresent(presentRoles::add);
        }

        int missingRoles = (int) ProfessionAssigner.FOUNDATION_ORDER.stream()
                .filter(role -> !presentRoles.contains(role))
                .count();
        int populationDeficit = Math.max(
                0, ProfessionAssigner.FOUNDATION_ORDER.size() - adults);
        int toSpawn = Math.max(missingRoles, populationDeficit);

        int bedsPlaced = ensureExistingHomes(
                world, center, colony.id(), workers, byId);
        int spawned = 0;

        for (int index = 0; index < toSpawn; index++) {
            Optional<BlockPos> foot = findBedSpot(world, center);

            if (foot.isEmpty()) {
                VillageColonyMod.LOGGER.warn(
                        "Colony {} has no safe place for foundation bed {}/{}",
                        colony.id(), index + 1, toSpawn);
                break;
            }

            if (!placeBed(world, foot.get())) {
                VillageColonyMod.LOGGER.warn(
                        "Colony {} could not place foundation bed at {}",
                        colony.id(), foot.get().toShortString());
                break;
            }

            bedsPlaced++;
            ChestPlacer.placeBeside(world, foot.get());

            VillagerEntity villager = EntityType.VILLAGER.create(world);

            if (villager == null) {
                VillageColonyMod.LOGGER.warn(
                        "Colony {} could not create a foundation villager",
                        colony.id());
                break;
            }

            villager.setBreedingAge(0);
            villager.refreshPositionAndAngles(foot.get().up(), 0.0F, 0.0F);
            villager.getBrain().remember(
                    MemoryModuleType.HOME,
                    GlobalPos.create(world.getRegistryKey(), foot.get()));

            if (!world.spawnEntity(villager)) {
                VillageColonyMod.LOGGER.warn(
                        "Colony {} could not spawn a foundation villager at {}",
                        colony.id(), foot.get().toShortString());
                continue;
            }

            spawned++;
            byId.put(villager.getUuid(), villager);
        }

        if (spawned > 0 || bedsPlaced > 0) {
            VillageColonyMod.LOGGER.info(
                    "Foundation for colony {}: spawned {} adults and placed {} beds",
                    colony.id(), spawned, bedsPlaced);
        }

        return new Result(spawned, bedsPlaced);
    }

    /** Dá cama exclusiva aos trabalhadores fundamentais já existentes. */
    private static int ensureExistingHomes(
            ServerWorld world,
            BlockPos center,
            UUID colonyId,
            WorkerService workers,
            Map<UUID, VillagerEntity> villagers) {

        Set<BlockPos> occupiedBeds = new HashSet<>();
        int placed = 0;

        for (Worker worker : workers.ofColony(colonyId)) {
            if (worker.profession()
                    .map(ProfessionAssigner::foundationRole)
                    .filter(ProfessionAssigner.FOUNDATION_ORDER::contains)
                    .isEmpty()) {
                continue;
            }

            VillagerEntity villager = villagers.get(worker.villagerId());

            if (villager == null || villager.isBaby()) {
                continue;
            }

            Optional<GlobalPos> home = villager.getBrain()
                    .getOptionalRegisteredMemory(MemoryModuleType.HOME)
                    .filter(value -> value.dimension().equals(world.getRegistryKey()))
                    .filter(value -> isBed(world, value.pos()));

            if (home.isPresent() && occupiedBeds.add(canonicalBed(world, home.get().pos()))) {
                continue;
            }

            Optional<BlockPos> foot = findBedSpot(world, center);

            if (foot.isEmpty() || !placeBed(world, foot.get())) {
                continue;
            }

            villager.getBrain().remember(
                    MemoryModuleType.HOME,
                    GlobalPos.create(world.getRegistryKey(), foot.get()));
            occupiedBeds.add(foot.get());
            ChestPlacer.placeBeside(world, foot.get());
            placed++;
        }

        return placed;
    }

    /** Procura terreno livre sem remover blocos existentes. */
    private static Optional<BlockPos> findBedSpot(ServerWorld world, BlockPos center) {
        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                BlockPos column = center.add(dx, 0, dz);
                BlockPos foot = world.getTopPosition(
                        Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, column);

                if (canPlaceBed(world, foot)) {
                    return Optional.of(foot.toImmutable());
                }
            }
        }

        return Optional.empty();
    }

    private static boolean canPlaceBed(ServerWorld world, BlockPos foot) {
        if (!world.getBlockState(foot).isReplaceable()
                || !world.getBlockState(foot.up()).isReplaceable()
                || !world.getBlockState(foot.up(2)).isReplaceable()
                || !world.getBlockState(foot.down()).isSolidBlock(world, foot.down())) {
            return false;
        }

        for (Direction facing : Direction.Type.HORIZONTAL) {
            BlockPos head = foot.offset(facing);

            if (!world.getBlockState(head).isReplaceable()
                    || !world.getBlockState(head.up()).isReplaceable()
                    || !world.getBlockState(head.up(2)).isReplaceable()
                    || !world.getBlockState(head.down()).isSolidBlock(world, head.down())) {
                continue;
            }

            return true;
        }

        return false;
    }

    /** Coloca as duas metades da cama, somente depois de validar ambas. */
    private static boolean placeBed(ServerWorld world, BlockPos foot) {
        for (Direction facing : Direction.Type.HORIZONTAL) {
            BlockPos head = foot.offset(facing);

            if (!canPlaceBedFacing(world, foot, head)) {
                continue;
            }

            BlockState base = Blocks.WHITE_BED.getDefaultState()
                    .with(Properties.BED_PART, BedPart.FOOT)
                    .with(Properties.HORIZONTAL_FACING, facing);
            BlockState top = base.with(Properties.BED_PART, BedPart.HEAD);

            world.setBlockState(head, top, Block.NOTIFY_ALL);
            world.setBlockState(foot, base, Block.NOTIFY_ALL);

            return isBed(world, foot);
        }

        return false;
    }

    private static boolean canPlaceBedFacing(
            ServerWorld world, BlockPos foot, BlockPos head) {
        return world.getBlockState(foot).isReplaceable()
                && world.getBlockState(head).isReplaceable()
                && world.getBlockState(foot.up()).isReplaceable()
                && world.getBlockState(head.up()).isReplaceable()
                && world.getBlockState(foot.down()).isSolidBlock(world, foot.down())
                && world.getBlockState(head.down()).isSolidBlock(world, head.down());
    }

    private static boolean isBed(ServerWorld world, BlockPos pos) {
        return world.getBlockState(pos).getBlock() instanceof BedBlock;
    }

    private static BlockPos canonicalBed(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);

        if (state.getBlock() instanceof BedBlock
                && state.get(Properties.BED_PART) == BedPart.HEAD) {
            return pos.offset(state.get(Properties.HORIZONTAL_FACING).getOpposite());
        }

        return pos.toImmutable();
    }

}
