package com.villagecolony.fabric.integration;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ColonyPos;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SaplingGenerator;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

/**
 * Reserva árvores maduras para o lenhador sem carregar chunks, tocar estruturas
 * ou substituir blocos protegidos.
 */
public final class VillageForest {
    private static final int INNER_RADIUS = 48;
    private static final int OUTER_RADIUS = 56;
    private static final int CANOPY_RADIUS = 5;
    private static final int CANOPY_HEIGHT = 16;
    private static final int MIN_TREE_DISTANCE = 12;
    private static final int CANDIDATE_ANGLES = 64;

    private VillageForest() {
    }

    /** Resultado da tentativa de plantar a árvore de uma dezena populacional. */
    public enum PopulationPlanting {
        NOT_DUE,
        PLANTED,
        WAITING_FOR_SPACE
    }

    /** Planta duas árvores maduras distintas para uma vila recém-criada. */
    public static int seedInitial(ServerWorld world, Colony colony) {
        return VillageBiomes.forestSpeciesAt(world, colony.center())
                .map(species -> seedInitial(world, colony, species.getFirst(), species.getLast()))
                .orElse(0);
    }

    /**
     * Variante explícita para a criação da vila e os GameTests. A segunda
     * espécie é sempre mantida distante da primeira.
     */
    public static int seedInitial(ServerWorld world, Colony colony, TreeSpecies first, TreeSpecies second) {
        if (first == second) {
            return plantOne(world, colony, first, List.of()).isPresent() ? 1 : 0;
        }

        List<BlockPos> planted = new ArrayList<>(2);
        plantOne(world, colony, first, planted).ifPresent(planted::add);
        plantOne(world, colony, second, planted).ifPresent(planted::add);
        return planted.size();
    }

    /**
     * Planta uma árvore adicional quando a população viva alcança a próxima
     * dezena. O marco só avança após a geração física no mundo.
     */
    public static PopulationPlanting plantForPopulation(ServerWorld world, Colony colony, int livingAdults) {
        int nextMilestone = colony.forestPopulationMilestone() + 10;
        if (livingAdults < nextMilestone) {
            return PopulationPlanting.NOT_DUE;
        }

        Optional<List<TreeSpecies>> available = VillageBiomes.forestSpeciesAt(world, colony.center());
        if (available.isEmpty()) {
            return PopulationPlanting.WAITING_FOR_SPACE;
        }

        List<TreeSpecies> species = available.get();
        TreeSpecies selected = species.get((nextMilestone / 10 - 1) % species.size());
        if (plantOne(world, colony, selected, List.of()).isEmpty()) {
            return PopulationPlanting.WAITING_FOR_SPACE;
        }

        colony.markForestPopulationMilestone(nextMilestone);
        return PopulationPlanting.PLANTED;
    }

    private static Optional<BlockPos> plantOne(
            ServerWorld world, Colony colony, TreeSpecies species, List<BlockPos> avoid) {
        Optional<SaplingGenerator> generator = generatorFor(species);
        if (generator.isEmpty()) {
            return Optional.empty();
        }

        for (BlockPos candidate : candidates(colony.center(), colony.id().hashCode() + species.name().hashCode())) {
            Optional<BlockPos> ground = naturalGroundAt(world, candidate);
            if (ground.isEmpty() || tooCloseTo(ground.get(), avoid) || !hasClearCanopy(world, ground.get())) {
                continue;
            }
            if (generator.get().generate(
                    world,
                    world.getChunkManager().getChunkGenerator(),
                    ground.get().up(),
                    Blocks.AIR.getDefaultState(),
                    world.getRandom())) {
                return Optional.of(ground.get().up());
            }
        }
        return Optional.empty();
    }

    private static Optional<SaplingGenerator> generatorFor(TreeSpecies species) {
        return switch (species) {
            case OAK -> Optional.of(SaplingGenerator.OAK);
            case BIRCH -> Optional.of(SaplingGenerator.BIRCH);
            case SPRUCE -> Optional.of(SaplingGenerator.SPRUCE);
            case ACACIA -> Optional.of(SaplingGenerator.ACACIA);
            default -> Optional.empty();
        };
    }

    private static Optional<BlockPos> naturalGroundAt(ServerWorld world, BlockPos candidate) {
        for (int y = candidate.getY() + 4; y >= candidate.getY() - 8; y--) {
            BlockPos ground = new BlockPos(candidate.getX(), y, candidate.getZ());
            WorldChunk chunk = loadedChunk(world, ground);
            if (chunk == null) {
                return Optional.empty();
            }
            BlockState state = chunk.getBlockState(ground);
            if (state.isAir()) {
                continue;
            }
            if (!LotGround.isNaturalGround(state)
                    || !BlockProtection.mayBreak(world, ground, state)
                    || BlockProtection.isColonyBuilt(ground)
                    || !isEmpty(chunk, ground.up())) {
                return Optional.empty();
            }
            return Optional.of(ground);
        }
        return Optional.empty();
    }

    private static boolean hasClearCanopy(ServerWorld world, BlockPos ground) {
        for (int x = -CANOPY_RADIUS; x <= CANOPY_RADIUS; x++) {
            for (int z = -CANOPY_RADIUS; z <= CANOPY_RADIUS; z++) {
                for (int y = 1; y <= CANOPY_HEIGHT; y++) {
                    BlockPos position = ground.add(x, y, z);
                    WorldChunk chunk = loadedChunk(world, position);
                    if (chunk == null
                            || !isEmpty(chunk, position)
                            || BlockProtection.isColonyBuilt(position)
                            || BlockProtection.isVillageOriginal(world, position)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean isEmpty(WorldChunk chunk, BlockPos position) {
        BlockState state = chunk.getBlockState(position);
        return state.isAir() && chunk.getBlockEntity(position) == null;
    }

    private static WorldChunk loadedChunk(ServerWorld world, BlockPos position) {
        if (!world.isInBuildLimit(position)) {
            return null;
        }
        return world.getChunkManager().getWorldChunk(position.getX() >> 4, position.getZ() >> 4);
    }

    private static boolean tooCloseTo(BlockPos candidate, List<BlockPos> planted) {
        return planted.stream().anyMatch(other -> candidate.getSquaredDistance(other) < MIN_TREE_DISTANCE * MIN_TREE_DISTANCE);
    }

    private static List<BlockPos> candidates(ColonyPos center, int seed) {
        Set<BlockPos> unique = new LinkedHashSet<>();
        for (int radius = INNER_RADIUS; radius <= OUTER_RADIUS; radius++) {
            for (int step = 0; step < CANDIDATE_ANGLES; step++) {
                double angle = Math.PI * 2 * step / CANDIDATE_ANGLES;
                int x = center.x() + (int) Math.round(Math.cos(angle) * radius);
                int z = center.z() + (int) Math.round(Math.sin(angle) * radius);
                unique.add(new BlockPos(x, center.y(), z));
            }
        }
        List<BlockPos> ordered = new ArrayList<>(unique);
        Collections.rotate(ordered, Math.floorMod(seed, ordered.size()));
        return ordered;
    }
}
