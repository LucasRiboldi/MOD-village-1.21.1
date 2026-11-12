package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.LampPostPlan;
import com.villagecolony.core.construction.model.RoadGraph;
import com.villagecolony.core.construction.model.RoadSettling;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.data.save.ColonySavedData;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.content.ModContent;
import com.villagecolony.fabric.integration.BlockProtection;
import com.villagecolony.fabric.world.GeneratedVillages;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.minecraft.world.poi.PointOfInterestStorage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Onde o próximo poste de luz fica — Regra 261 (autor, 2026-11-12).
 *
 * <ul>
 *   <li>Só ao lado de coluna fixa do grafo (Regra 225), da mais perto do centro para fora, até a ponta.</li>
 *   <li>No bloco ao lado da linha, na altura dela: a base toma o lugar do solo natural.</li>
 *   <li>Nenhuma estrutura a {@link #CLEAR} blocos em volta: construção da vila, obra aberta e peça da vila gerada.</li>
 *   <li>Céu aberto acima da base; folha conta como bloqueio.</li>
 *   <li>A {@link #SPACING} blocos ou mais de outro poste: tocha da colônia de pé, poste do mod e poste da vila gerada. A luz
 *       da tocha não passa desse raio, então onde a luz de outro poste chega não há poste novo.</li>
 * </ul>
 */
final class LampPostSites {

    /** Os blocos livres de estrutura em volta da base. */
    static final int CLEAR = 2;

    /** A distância mínima entre dois postes. */
    static final int SPACING = 30;

    private static final List<Direction> SIDES = List.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);

    private LampPostSites() {
    }

    /** A base do próximo poste, ou vazio sem linha fixa ou sem lugar que passe nas regras. */
    static Optional<BlockPos> find(ServerWorld world, Colony colony, long today) {
        var store = ColonySavedData.get(world.getServer()).roadGraphs();
        Optional<RoadGraph> graph = store.graph(colony.id());

        if (graph.isEmpty()) {
            return Optional.empty();
        }

        Set<Long> fixed = RoadSettling.fixed(store.settled(colony.id()), today);

        if (fixed.isEmpty()) {
            return Optional.empty();
        }

        Set<Long> line = new HashSet<>();
        Set<ColonyPos> cells = new HashSet<>();

        graph.get().nodes().forEach(node -> cells.add(node.position()));
        graph.get().edges().forEach(edge -> cells.addAll(edge.columns()));
        cells.forEach(cell -> line.add(ColonyRoads.column(cell.x(), cell.z())));

        ColonyPos center = colony.center();
        List<ColonyPos> along = cells.stream()
                .filter(cell -> fixed.contains(ColonyRoads.column(cell.x(), cell.z())))
                .sorted(Comparator.comparingLong((ColonyPos cell) -> squared(cell.x() - center.x(), cell.z() - center.z()))
                        .thenComparingInt(ColonyPos::x).thenComparingInt(ColonyPos::z))
                .toList();

        List<BlockPos> posts = standingPosts(world, colony);
        List<BlockBox> structures = structures(world, colony);

        for (ColonyPos cell : along) {
            BlockPos road = MinecraftTypeAdapter.toBlockPos(cell);

            for (Direction side : SIDES) {
                BlockPos base = road.offset(side);

                if (!line.contains(ColonyRoads.column(base.getX(), base.getZ()))
                        && isGround(world, base) && isOpenAbove(world, base) && isFarFrom(base, posts)
                        && isClearOf(base, structures) && noColonyTorchNear(world, base)) {
                    return Optional.of(base);
                }
            }
        }

        return Optional.empty();
    }

    /** Solo natural que a proteção deixa trocar, sem caminho e sem bloco com inventário. */
    private static boolean isGround(ServerWorld world, BlockPos base) {
        BlockState state = world.getBlockState(base);

        return world.isChunkLoaded(base) && WorldTerrain.isNaturalGround(state) && world.getBlockEntity(base) == null
                && BlockProtection.mayBuildOver(world, base, state);
    }

    /** Céu aberto: do bloco acima da base até o topo, só o que o construtor tira (grama, flor, neve fina). */
    private static boolean isOpenAbove(ServerWorld world, BlockPos base) {
        int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING, base.getX(), base.getZ());

        for (int y = base.getY() + 1; y < Math.max(top, base.getY() + 4); y++) {
            BlockState state = world.getBlockState(base.withY(y));

            if (!state.isAir() && (!state.isReplaceable() || !state.getFluidState().isEmpty() || state.isIn(BlockTags.LEAVES))) {
                return false;
            }
        }

        return true;
    }

    private static boolean isClearOf(BlockPos base, List<BlockBox> structures) {
        for (BlockBox box : structures) {
            if (near(box, base)) {
                return false;
            }
        }

        return true;
    }

    private static boolean near(BlockBox box, BlockPos base) {
        return base.getX() >= box.getMinX() - CLEAR && base.getX() <= box.getMaxX() + CLEAR
                && base.getZ() >= box.getMinZ() - CLEAR && base.getZ() <= box.getMaxZ() + CLEAR;
    }

    /** Longe dos postes já conhecidos: conta barata, antes das outras. */
    private static boolean isFarFrom(BlockPos base, List<BlockPos> posts) {
        for (BlockPos post : posts) {
            if (tooClose(base, post)) {
                return false;
            }
        }

        return true;
    }

    /** Nenhuma tocha da colônia de pé a menos de {@link #SPACING}, pelo ponto de interesse dela. */
    private static boolean noColonyTorchNear(ServerWorld world, BlockPos base) {
        return world.getPointOfInterestStorage()
                .getInCircle(type -> type.matchesKey(ModContent.colonyTorchPoi()), base, SPACING + 1,
                        PointOfInterestStorage.OccupationStatus.ANY)
                .noneMatch(torch -> tooClose(base, torch.getPos()));
    }

    private static boolean tooClose(BlockPos base, BlockPos post) {
        return squared(base.getX() - post.getX(), base.getZ() - post.getZ()) < (long) SPACING * SPACING;
    }

    private static long squared(int dx, int dz) {
        return (long) dx * dx + (long) dz * dz;
    }

    /** Os postes já de pé ou em obra: os do mod, pela planta, e os da vila gerada. */
    private static List<BlockPos> standingPosts(ServerWorld world, Colony colony) {
        List<BlockPos> posts = new ArrayList<>();

        for (Building building : VillageColonyMod.BUILDINGS.ofColony(colony.id())) {
            if (LampPostPlan.isLampPost(building.blueprint())) {
                posts.add(MinecraftTypeAdapter.toBlockPos(building.min()));
            }
        }

        VillageColonyMod.CONSTRUCTIONS.openOf(colony.id())
                .filter(project -> LampPostPlan.isLampPost(project.blueprint().id()))
                .ifPresent(project -> posts.add(MinecraftTypeAdapter.toBlockPos(project.origin())));

        BlockPos hub = MinecraftTypeAdapter.toBlockPos(colony.center());

        for (BlockBox lamp : GeneratedVillages.piecesAt(world, hub.getX(), hub.getZ(), "lamp")) {
            posts.add(lamp.getCenter());
        }

        return posts;
    }

    /** As construções da vila, postes inclusive, a obra aberta e as peças da vila gerada. */
    private static List<BlockBox> structures(ServerWorld world, Colony colony) {
        List<BlockBox> boxes = new ArrayList<>(generated(world, MinecraftTypeAdapter.toBlockPos(colony.center())));

        for (Building building : VillageColonyMod.BUILDINGS.ofColony(colony.id())) {
            boxes.add(box(building.min(), building.max()));
        }

        VillageColonyMod.CONSTRUCTIONS.openOf(colony.id()).ifPresent(project -> boxes.add(boxOf(project)));
        VillageColonyMod.CONSTRUCTIONS.pendingOf(colony.id()).ifPresent(pending -> boxes.add(
                box(pending.origin(), pending.origin())));

        return boxes;
    }

    /** As peças da vila gerada que alcança esta coluna e ocupam lugar: estrutura, enfeite e plantação. */
    private static List<BlockBox> generated(ServerWorld world, BlockPos base) {
        List<BlockBox> boxes = new ArrayList<>(GeneratedVillages.structuresAt(world, base.getX(), base.getZ()));

        boxes.addAll(GeneratedVillages.ornamentsAt(world, base.getX(), base.getZ()));
        boxes.addAll(GeneratedVillages.piecesAt(world, base.getX(), base.getZ(), "farm"));

        return boxes;
    }

    private static BlockBox boxOf(ConstructionProject project) {
        ColonyPos origin = project.origin();
        ColonyPos size = project.blueprint().size();

        return BlockBox.create(MinecraftTypeAdapter.toBlockPos(origin),
                MinecraftTypeAdapter.toBlockPos(origin).add(size.x() - 1, size.y() - 1, size.z() - 1));
    }

    private static BlockBox box(ColonyPos min, ColonyPos max) {
        return BlockBox.create(MinecraftTypeAdapter.toBlockPos(min), MinecraftTypeAdapter.toBlockPos(max));
    }
}
