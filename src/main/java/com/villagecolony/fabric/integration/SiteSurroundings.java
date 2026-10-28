package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.content.ModContent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * O chão em volta da obra nova — Regra 131 (autor, 2026-10-27): antes de assentar o primeiro bloco, o
 * construtor nivela um anel de {@link #RING} blocos em volta da pegada na altura da rua, com no máximo
 * um bloco para cima ou para baixo, e depois uma rampa de até {@link #RAMP} blocos em que cada passo
 * afasta a faixa permitida em um, até encontrar o terreno natural. Anda-se em volta da casa sem degrau.
 *
 * <p>Só chão natural é cortado (terra, areia, cascalho, pedra do mundo) e só o vazio é aterrado, com o
 * chão do bioma. Nunca mexe em construção, peça da vila, bloco do jogador, caminho, água nem na coluna
 * de árvore. A terra cortada vai para o baú da colônia mais perto com lugar.
 */
public final class SiteSurroundings {

    /** O anel nivelado em volta da pegada. */
    static final int RING = 2;

    /** A rampa depois do anel até o terreno natural. */
    static final int RAMP = 3;

    /** Quanto acima e abaixo da rua se procura o chão de cada coluna. */
    static final int SCAN = 8;

    private SiteSurroundings() {
    }

    /** Quanto o chão pode subir ou descer da rua a {@code distance} blocos da pegada. */
    static int bandAt(int distance) {
        return distance <= RING ? 1 : distance - RING + 1;
    }

    /** Nivela em volta da obra; devolve quantos blocos mudaram. */
    public static int level(ServerWorld world, ConstructionProject project) {
        Blueprint blueprint = project.blueprint();
        ColonyPos origin = project.origin();
        int roadY = origin.y() + (blueprint.hasStreetLayer() ? blueprint.streetLayer() : 0);
        int x1 = origin.x() + blueprint.size().x() - 1;
        int z1 = origin.z() + blueprint.size().z() - 1;

        return levelAround(world, project.colonyId(), origin, x1, z1, roadY, blueprint.id().path());
    }

    /**
     * Nivela em volta de uma construção já de pé — o pedreiro sem tarefa, Regra 149 (autor, 2026-10-28). A
     * altura de referência é a mediana do chão logo em volta das paredes: a planta não diz onde fica a rua de
     * uma casa antiga.
     */
    public static int level(ServerWorld world, com.villagecolony.core.construction.model.Building building) {
        List<Integer> heights = new java.util.ArrayList<>();
        ColonyPos min = building.min();
        ColonyPos max = building.max();

        for (int x = min.x() - 1; x <= max.x() + 1; x++) {
            for (int z = min.z() - 1; z <= max.z() + 1; z++) {
                boolean wall = x == min.x() - 1 || x == max.x() + 1 || z == min.z() - 1 || z == max.z() + 1;

                if (wall && world.isChunkLoaded(x >> 4, z >> 4)) {
                    heights.add(world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1);
                }
            }
        }

        if (heights.isEmpty()) {
            return 0;
        }

        java.util.Collections.sort(heights);

        return levelAround(world, building.colonyId(), min, max.x(), max.z(), heights.get(heights.size() / 2),
                building.blueprint().path());
    }

    private static int levelAround(ServerWorld world, java.util.UUID colonyId, ColonyPos origin, int x1, int z1,
            int roadY, String what) {
        int x0 = origin.x();
        int z0 = origin.z();
        Set<Long> roads = RoadIndex.roadsOf(colonyId).map(index -> Set.copyOf(index.columns())).orElse(Set.of());
        List<com.villagecolony.core.construction.model.Building> others =
                List.copyOf(VillageColonyMod.BUILDINGS.ofColony(colonyId));
        Map<Item, Integer> cut = new HashMap<>();
        int changed = 0;

        for (int distance = 1; distance <= RING + RAMP; distance++) {
            int band = bandAt(distance);

            for (int x = x0 - distance; x <= x1 + distance; x++) {
                for (int z = z0 - distance; z <= z1 + distance; z++) {
                    boolean onRing = x == x0 - distance || x == x1 + distance || z == z0 - distance || z == z1 + distance;

                    if (onRing && !roads.contains(ColonyRoads.column(x, z)) && !insideAny(others, x, z)) {
                        changed += levelColumn(world, x, z, roadY, band, cut);
                    }
                }
            }
        }

        store(world, colonyId, origin, cut);

        if (changed > 0) {
            VillageColonyMod.LOGGER.info("Colony {} levelled {} blocks around {} — ring of {} at the road height"
                            + " ±1 and a ramp of {} (Regra 131)", colonyId.toString().substring(0, 8), changed,
                    what, RING, RAMP);
        }

        return changed;
    }

    private static boolean insideAny(List<com.villagecolony.core.construction.model.Building> buildings, int x, int z) {
        for (com.villagecolony.core.construction.model.Building building : buildings) {
            if (x >= building.min().x() && x <= building.max().x() && z >= building.min().z() && z <= building.max().z()) {
                return true;
            }
        }

        return false;
    }

    /** Leva o chão desta coluna para dentro de {@code roadY ± band}. */
    static int levelColumn(ServerWorld world, int x, int z, int roadY, int band, Map<Item, Integer> cut) {
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return 0;
        }

        int top = Integer.MIN_VALUE;

        for (int y = roadY + SCAN; y >= roadY - SCAN; y--) {
            BlockState state = world.getBlockState(new BlockPos(x, y, z));

            if (state.isAir() || (LotGround.isNothing(state) && state.getFluidState().isEmpty())) {
                continue;
            }

            if (!state.getFluidState().isEmpty() || !isNaturalGround(state)) {
                return 0;
            }

            top = y;
            break;
        }

        if (top == Integer.MIN_VALUE) {
            return 0;
        }

        int changed = 0;

        for (int y = top; y > roadY + band; y--) {
            BlockPos at = new BlockPos(x, y, z);
            BlockState state = world.getBlockState(at);

            if (!isNaturalGround(state) || !BlockProtection.mayDigOut(world, at, state)) {
                return changed;
            }

            cut.merge(state.getBlock().asItem(), 1, Integer::sum);
            world.setBlockState(at, Blocks.AIR.getDefaultState());
            changed++;
        }

        for (int y = top + 1; y <= roadY - band; y++) {
            BlockPos at = new BlockPos(x, y, z);
            BlockState state = world.getBlockState(at);

            if ((!LotGround.isNothing(state) && !state.isAir()) || !state.getFluidState().isEmpty()) {
                return changed;
            }

            world.setBlockState(at, VillageBiomes.foundationGroundAt(world, at).getDefaultState());
            changed++;
        }

        return changed;
    }

    /** Terra, areia, cascalho e pedra do mundo: o que o nivelamento pode cortar. Caminho fica. */
    public static boolean isNaturalGround(BlockState state) {
        Block block = state.getBlock();

        return (state.isIn(BlockTags.DIRT) || state.isIn(BlockTags.SAND) || state.isIn(BlockTags.BASE_STONE_OVERWORLD)
                || block == Blocks.GRAVEL || block == Blocks.SNOW_BLOCK || block == Blocks.SANDSTONE
                || block == Blocks.RED_SANDSTONE)
                && block != Blocks.DIRT_PATH && block != ModContent.SAND_PATH;
    }

    private static void store(ServerWorld world, java.util.UUID colonyId, ColonyPos near, Map<Item, Integer> cut) {
        List<ColonyPos> chests = ColonyChests.nearestFirst(world, colonyId, near);

        cut.forEach((item, count) -> {
            int left = count;

            for (ColonyPos chest : chests) {
                if (left <= 0 || item == Blocks.AIR.asItem()) {
                    break;
                }

                left = ChestDepositor.deposit(world, chest, item, left);
            }
        });
    }
}
