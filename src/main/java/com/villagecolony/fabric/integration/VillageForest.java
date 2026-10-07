package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.coordination.ForestQuota;
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
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.SaplingGenerator;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.WorldChunk;

/**
 * As árvores da vila — ADR-037 F1, F2 e F3: cinco nas bordas para cada dez
 * aldeões, e uma árvore natural da espécie que a obra pede para cada cinco
 * aldeões. Nascem maduras, perto das bordas e longe das estruturas, sem
 * carregar chunk e sem tocar bloco protegido; capim, flor, samambaia e folha
 * natural cedem lugar.
 */
public final class VillageForest {

    static {
        ServerMemory.register(VillageForest.class, VillageForest::clearAll);
    }

    /** Quanto esperar para buscar lugar de novo depois de uma busca sem lugar (F13). */
    private static final int RETRY_TICKS = 6_000;

    /** Quando cada colônia pode buscar lugar para a árvore da dezena de novo. */
    private static final Map<UUID, Long> NEXT_TRY = new HashMap<>();

    /** Quando cada colônia pode buscar lugar para a árvore que a obra pede de novo. */
    private static final Map<UUID, Long> NEXT_REQUEST_TRY = new HashMap<>();

    /** Esquece as esperas. Chamado ao parar o servidor. */
    public static void clearAll() {
        NEXT_TRY.clear();
        NEXT_REQUEST_TRY.clear();
    }

    private static final int INNER_RADIUS = 48;
    private static final int OUTER_RADIUS = 56;

    /** As bordas: de 4 a 10 blocos fora da caixa da vila. */
    private static final int[] EDGE_GAPS = {4, 7, 10};

    private static final int EDGE_STEP = 3;
    private static final int CANOPY_RADIUS = 5;
    private static final int CANOPY_HEIGHT = 16;
    private static final int MIN_TREE_DISTANCE = 12;
    private static final int CANDIDATE_ANGLES = 64;

    private VillageForest() {
    }

    /** Resultado da tentativa de plantar as árvores de uma dezena populacional. */
    public enum PopulationPlanting {
        NOT_DUE,
        PLANTED,
        WAITING_FOR_SPACE,
        /** Sem lugar na última busca, e ainda dentro do intervalo de espera. */
        RESTING
    }

    /** As árvores da fundação: cinco por dezena de aldeões vivos ({@link ForestQuota}). */
    public static int seedInitial(ServerWorld world, Colony colony) {
        int adults = VillagerScanner.livingAdultPopulation(world, colony);
        int planted = VillageBiomes.forestSpeciesAt(world, colony.center())
                .map(species -> seedInitial(world, colony, species.getFirst(), species.getLast(),
                        ForestQuota.foundingTrees(adults)))
                .orElse(0);

        if (planted > 0 && ForestQuota.foundingMilestone(adults) > colony.forestPopulationMilestone()) {
            colony.markForestPopulationMilestone(ForestQuota.foundingMilestone(adults));
        }

        return planted;
    }

    /** Variante explícita para a fundação e os GameTests: as duas espécies alternadas. */
    public static int seedInitial(
            ServerWorld world, Colony colony, TreeSpecies first, TreeSpecies second, int count) {
        List<BlockPos> planted = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            TreeSpecies species = index % 2 == 0 ? first : second;
            plantOne(world, colony, species, planted).ifPresent(planted::add);
        }
        return planted.size();
    }

    /**
     * Cinco árvores quando a população viva alcança a próxima dezena. O marco
     * só avança com ao menos uma árvore no mundo.
     */
    public static PopulationPlanting plantForPopulation(ServerWorld world, Colony colony, int livingAdults) {
        plantWhatTheConstructionRequested(world, colony, livingAdults);

        int nextMilestone = colony.forestPopulationMilestone() + 10;
        if (livingAdults < nextMilestone) {
            return PopulationPlanting.NOT_DUE;
        }

        Long nextTry = NEXT_TRY.get(colony.id());
        if (nextTry != null && world.getTime() < nextTry) {
            return PopulationPlanting.RESTING;
        }

        Optional<List<TreeSpecies>> available = VillageBiomes.forestSpeciesAt(world, colony.center());
        int planted = available
                .map(species -> seedInitial(world, colony, species.getFirst(), species.getLast(),
                        ForestQuota.TREES_PER_TEN))
                .orElse(0);

        if (planted == 0) {
            NEXT_TRY.put(colony.id(), world.getTime() + RETRY_TICKS);
            return PopulationPlanting.WAITING_FOR_SPACE;
        }

        NEXT_TRY.remove(colony.id());
        colony.markForestPopulationMilestone(nextMilestone);
        VillageColonyMod.LOGGER.info("Colony {} reached {} villagers and grew {} trees at its edges",
                colony.id().toString().substring(0, 8), nextMilestone, planted);
        return PopulationPlanting.PLANTED;
    }

    /** Obra esperando madeira: na vigésima espera, as árvores naturais da espécie pedida. */
    private static void plantWhatTheConstructionRequested(ServerWorld world, Colony colony, int livingAdults) {
        Long nextTry = NEXT_REQUEST_TRY.get(colony.id());
        if (nextTry != null && world.getTime() < nextTry) {
            return;
        }

        VillageColonyMod.CONSTRUCTIONS.openOf(colony.id())
                .filter(project -> project.state()
                        == com.villagecolony.core.construction.model.ConstructionState.WAITING_RESOURCES)
                .ifPresent(project -> {
                    Set<TreeSpecies> waitingFor = new LinkedHashSet<>();
                    project.remainingMaterials().keySet().stream()
                            .map(TreeSpecies::ofConstructionResource)
                            .flatMap(Optional::stream)
                            .forEach(waitingFor::add);

                    for (TreeSpecies species : waitingFor) {
                        if (!BiomeConstructionSupply.treeWaitReached(colony.id(), species)) {
                            continue;
                        }

                        if (plantRequestedTrees(world, colony, species, ForestQuota.requestedTrees(livingAdults)) > 0) {
                            BiomeConstructionSupply.treePlanted(colony.id(), species);
                        } else {
                            NEXT_REQUEST_TRY.put(colony.id(), world.getTime() + RETRY_TICKS);
                        }
                    }
                });
    }

    /**
     * As árvores naturais que a obra pede, maduras, nas bordas da vila.
     *
     * @return quantas nasceram
     */
    public static int plantRequestedTrees(ServerWorld world, Colony colony, TreeSpecies species, int count) {
        List<BlockPos> planted = new ArrayList<>(count);

        for (int index = 0; index < count; index++) {
            plantOne(world, colony, species, planted).ifPresent(planted::add);
        }

        if (!planted.isEmpty()) {
            VillageColonyMod.LOGGER.info(
                    "Colony {} grew {} natural {} tree(s) at its edges for the build waiting on wood",
                    colony.id().toString().substring(0, 8), planted.size(), species);
        }

        return planted.size();
    }

    private static Optional<BlockPos> plantOne(
            ServerWorld world, Colony colony, TreeSpecies species, List<BlockPos> avoid) {
        Optional<SaplingGenerator> generator = generatorFor(species);
        if (generator.isEmpty()) {
            return Optional.empty();
        }

        for (BlockPos candidate : candidates(colony, colony.id().hashCode() + species.name().hashCode() + avoid.size())) {
            Optional<BlockPos> ground = naturalGroundAt(world, candidate);
            if (ground.isEmpty()
                    || tooCloseTo(ground.get(), avoid)
                    || !hasClearableNaturalCanopy(world, ground.get())) {
                continue;
            }
            Map<BlockPos, BlockState> cleared = clearNaturalCover(world, ground.get());
            if (generator.get().generate(
                    world,
                    world.getChunkManager().getChunkGenerator(),
                    ground.get().up(),
                    Blocks.AIR.getDefaultState(),
                    world.getRandom())) {
                return Optional.of(ground.get().up());
            }
            restore(world, cleared);
        }
        return Optional.empty();
    }

    private static Optional<SaplingGenerator> generatorFor(TreeSpecies species) {
        return switch (species) {
            case OAK -> Optional.of(SaplingGenerator.OAK);
            case BIRCH -> Optional.of(SaplingGenerator.BIRCH);
            case SPRUCE -> Optional.of(SaplingGenerator.SPRUCE);
            case ACACIA -> Optional.of(SaplingGenerator.ACACIA);
            case JUNGLE -> Optional.of(SaplingGenerator.JUNGLE);
            case CHERRY -> Optional.of(SaplingGenerator.CHERRY);
            case MANGROVE -> Optional.of(SaplingGenerator.MANGROVE);
            default -> Optional.empty();
        };
    }

    /** O chão da coluna pelo mapa de altura sem folhas: o topo do terreno, não o primeiro sólido. */
    private static Optional<BlockPos> naturalGroundAt(ServerWorld world, BlockPos candidate) {
        WorldChunk chunk = loadedChunk(world, candidate.withY(world.getBottomY()));
        if (chunk == null) {
            return Optional.empty();
        }

        int top = chunk.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, candidate.getX(), candidate.getZ());
        BlockPos ground = new BlockPos(candidate.getX(), top, candidate.getZ());

        if (!world.isInBuildLimit(ground)) {
            return Optional.empty();
        }

        BlockState state = chunk.getBlockState(ground);

        if (!LotGround.isNaturalGround(state)
                || !BlockProtection.mayBreak(world, ground, state)
                || BlockProtection.isColonyBuilt(ground)
                || BlockProtection.isVillageOriginal(world, ground)) {
            return Optional.empty();
        }

        return Optional.of(ground);
    }

    /**
     * Livre para a copa: só ar, folha ou planta natural acima do chão, e nenhuma
     * estrutura da vila ou da colônia no quadrado, nem rente ao chão.
     */
    private static boolean hasClearableNaturalCanopy(ServerWorld world, BlockPos ground) {
        for (int x = -CANOPY_RADIUS; x <= CANOPY_RADIUS; x++) {
            for (int z = -CANOPY_RADIUS; z <= CANOPY_RADIUS; z++) {
                for (int y = 0; y <= CANOPY_HEIGHT; y++) {
                    BlockPos position = ground.add(x, y, z);
                    WorldChunk chunk = loadedChunk(world, position);
                    if (chunk == null
                            || BlockProtection.isColonyBuilt(position)
                            || BlockProtection.isVillageOriginal(world, position)) {
                        return false;
                    }
                    if (y == 0) {
                        continue;
                    }
                    BlockState state = chunk.getBlockState(position);
                    if (!isEmpty(chunk, position) && !isClearableNatural(world, position, chunk, state)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static Map<BlockPos, BlockState> clearNaturalCover(ServerWorld world, BlockPos ground) {
        Map<BlockPos, BlockState> cleared = new HashMap<>();
        for (int x = -CANOPY_RADIUS; x <= CANOPY_RADIUS; x++) {
            for (int z = -CANOPY_RADIUS; z <= CANOPY_RADIUS; z++) {
                for (int y = 1; y <= CANOPY_HEIGHT; y++) {
                    BlockPos position = ground.add(x, y, z);
                    WorldChunk chunk = loadedChunk(world, position);
                    if (chunk == null) {
                        continue;
                    }
                    BlockState state = chunk.getBlockState(position);
                    if (!state.isAir() && isClearableNatural(world, position, chunk, state)) {
                        world.setBlockState(position, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
                        cleared.put(position, state);
                    }
                }
            }
        }
        return cleared;
    }

    private static void restore(ServerWorld world, Map<BlockPos, BlockState> cleared) {
        for (Map.Entry<BlockPos, BlockState> block : cleared.entrySet()) {
            world.setBlockState(block.getKey(), block.getValue(), Block.NOTIFY_LISTENERS);
        }
    }

    /** Folha natural ou planta que se substitui (capim, flor, samambaia, neve fina) — F3. */
    private static boolean isClearableNatural(ServerWorld world, BlockPos position, WorldChunk chunk, BlockState state) {
        boolean natural = state.isIn(BlockTags.LEAVES)
                || (state.isReplaceable() && state.getFluidState().isEmpty());

        return natural
                && chunk.getBlockEntity(position) == null
                && BlockProtection.mayBreak(world, position, state)
                && !BlockProtection.isColonyBuilt(position)
                && !BlockProtection.isVillageOriginal(world, position);
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

    /** As bordas da caixa da vila; sem caixa medida, o anel de 48 a 56 em volta do centro. */
    private static List<BlockPos> candidates(Colony colony, int seed) {
        Set<BlockPos> unique = new LinkedHashSet<>();
        ColonyPos center = colony.center();
        Optional<VillageBounds> box = colony.bounds();

        if (box.isPresent()) {
            for (int gap : EDGE_GAPS) {
                int minX = box.get().minX() - gap;
                int maxX = box.get().maxX() + gap;
                int minZ = box.get().minZ() - gap;
                int maxZ = box.get().maxZ() + gap;

                for (int x = minX; x <= maxX; x += EDGE_STEP) {
                    unique.add(new BlockPos(x, center.y(), minZ));
                    unique.add(new BlockPos(x, center.y(), maxZ));
                }
                for (int z = minZ; z <= maxZ; z += EDGE_STEP) {
                    unique.add(new BlockPos(minX, center.y(), z));
                    unique.add(new BlockPos(maxX, center.y(), z));
                }
            }
        } else {
            for (int radius = INNER_RADIUS; radius <= OUTER_RADIUS; radius++) {
                for (int step = 0; step < CANDIDATE_ANGLES; step++) {
                    double angle = Math.PI * 2 * step / CANDIDATE_ANGLES;
                    int x = center.x() + (int) Math.round(Math.cos(angle) * radius);
                    int z = center.z() + (int) Math.round(Math.sin(angle) * radius);
                    unique.add(new BlockPos(x, center.y(), z));
                }
            }
        }

        List<BlockPos> ordered = new ArrayList<>(unique);
        Collections.rotate(ordered, Math.floorMod(seed, Math.max(1, ordered.size())));
        return ordered;
    }
}
