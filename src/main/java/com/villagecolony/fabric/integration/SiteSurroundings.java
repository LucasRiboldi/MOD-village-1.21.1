package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.type.ColonyPos;
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

/**
 * O chão em volta da obra nova — Regras 131 e 297: antes de assentar o primeiro bloco, o chão numa janela de
 * {@link SiteSmoothing#WINDOW} blocos em volta da pegada é suavizado como o da vila gerada, com a pegada na altura da
 * rua e degraus de no máximo 1 até encontrar o terreno natural ({@link SiteSmoothing}). Anda-se em volta da casa sem
 * degrau, e o morro colado no lote vira encosta, sem parede.
 *
 * <p>Só chão natural é cortado (terra, areia, cascalho, pedra do mundo) e só o vazio é aterrado, com o
 * chão do bioma. Nunca mexe em construção, peça da vila, bloco do jogador, caminho calçado, água nem na coluna
 * de árvore. A terra cortada vai para o baú da colônia mais perto com lugar.
 */
public final class SiteSurroundings {

    /** Quanto acima e abaixo do piso se procura o chão em volta das paredes da casa de pé. */
    static final int SCAN = 8;

    private SiteSurroundings() {
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
                    groundAt(world, x, z, min.y()).ifPresent(heights::add);
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

    /** O chão natural desta coluna perto do piso, com espaço livre em cima, de {@link #SCAN} acima a abaixo. */
    private static java.util.Optional<Integer> groundAt(ServerWorld world, int x, int z, int floorY) {
        for (int y = floorY + SCAN; y >= floorY - SCAN; y--) {
            net.minecraft.util.math.BlockPos at = new net.minecraft.util.math.BlockPos(x, y, z);

            // Só chão natural: laje, telhado ou a barreira de cima não são o chão em volta da casa.
            if (isNaturalGround(world.getBlockState(at)) && world.getBlockState(at.up()).isReplaceable()) {
                return java.util.Optional.of(y);
            }
        }

        return java.util.Optional.empty();
    }

    private static int levelAround(ServerWorld world, java.util.UUID colonyId, ColonyPos origin, int x1, int z1,
            int roadY, String what) {
        List<com.villagecolony.core.construction.model.Building> others =
                List.copyOf(VillageColonyMod.BUILDINGS.ofColony(colonyId));
        Map<Item, Integer> cut = new HashMap<>();
        SiteSmoothing.Result result = SiteSmoothing.smooth(world, origin.x(), origin.z(), x1, z1, roadY, others, cut);

        store(world, colonyId, origin, cut);
        // O aterro é chão que surge (Regra 111).
        Supplies.record(com.villagecolony.core.coordination.SupplyException.FOUNDATION_GROUND, colonyId,
                "site slope", result.filled());

        if (result.changed() > 0) {
            VillageColonyMod.LOGGER.info("Colony {} levelled {} blocks around {} — the ground smoothed {} blocks around"
                            + " at the road height (Regra 297)", colonyId.toString().substring(0, 8), result.changed(),
                    what, SiteSmoothing.WINDOW);
        }

        // O degrau que ficou em volta da obra, depois da suavização: a medida da parede.
        VillageColonyMod.LOGGER.info("VC_SITE_EDGE colony={} what={} road={} changed={} maxStep={} steep={} worstAt={}",
                colonyId.toString().substring(0, 8), what, roadY, result.changed(), result.maxStep(), result.steep(),
                result.worstAt() == null ? "-" : result.worstAt().toShortString());

        return result.changed();
    }

    /**
     * Regra 292: o chão natural ou o minério dentro dele — para o solo da vila, que corta a coluna inteira. Fora disso o
     * minério não é chão: lote, rua e lenhador seguem com {@link #isNaturalGround}.
     */
    public static boolean isNaturalOrOre(BlockState state) {
        return isNaturalGround(state)
                || state.isIn(BlockTags.COAL_ORES) || state.isIn(BlockTags.IRON_ORES) || state.isIn(BlockTags.COPPER_ORES)
                || state.isIn(BlockTags.GOLD_ORES) || state.isIn(BlockTags.REDSTONE_ORES) || state.isIn(BlockTags.LAPIS_ORES)
                || state.isIn(BlockTags.DIAMOND_ORES) || state.isIn(BlockTags.EMERALD_ORES);
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
