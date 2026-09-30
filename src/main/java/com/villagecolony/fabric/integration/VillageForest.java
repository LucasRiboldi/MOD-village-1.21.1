package com.villagecolony.fabric.integration;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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

    static {
        ServerMemory.register(VillageForest.class, VillageForest::clearAll);
    }

    /**
     * Quanto esperar para buscar lugar de novo, depois de uma busca sem lugar
     * — F13, 2026-09-30. O mesmo intervalo do viveiro do fazendeiro
     * ({@code FarmerNursery.BETWEEN_PLANTINGS}), e pelo mesmo motivo: a busca
     * era refeita a cada ciclo, dentro da fase que o log chamava de "chests".
     */
    private static final int RETRY_TICKS = 6_000;

    /** Quando cada colônia pode buscar lugar para a árvore da dezena de novo. */
    private static final Map<UUID, Long> NEXT_TRY = new HashMap<>();

    /** Esquece as esperas. Chamado ao parar o servidor. */
    public static void clearAll() {
        NEXT_TRY.clear();
    }
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
        WAITING_FOR_SPACE,
        /** Sem lugar na última busca, e ainda dentro do intervalo de espera. */
        RESTING
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

        Long nextTry = NEXT_TRY.get(colony.id());
        if (nextTry != null && world.getTime() < nextTry) {
            return PopulationPlanting.RESTING;
        }

        Optional<List<TreeSpecies>> available = VillageBiomes.forestSpeciesAt(world, colony.center());
        if (available.isEmpty()) {
            NEXT_TRY.put(colony.id(), world.getTime() + RETRY_TICKS);
            return PopulationPlanting.WAITING_FOR_SPACE;
        }

        List<TreeSpecies> species = available.get();
        TreeSpecies selected = species.get((nextMilestone / 10 - 1) % species.size());
        if (plantOne(world, colony, selected, List.of()).isEmpty()) {
            NEXT_TRY.put(colony.id(), world.getTime() + RETRY_TICKS);
            return PopulationPlanting.WAITING_FOR_SPACE;
        }

        NEXT_TRY.remove(colony.id());
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
