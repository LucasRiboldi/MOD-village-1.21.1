package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.construction.model.VillageSurface;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BlockProtection;
import com.villagecolony.fabric.integration.SiteSurroundings;
import com.villagecolony.fabric.integration.StreetSurface;
import com.villagecolony.fabric.integration.VillageRoad;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O pedreiro alisa o chão da vila — autor, 2026-10-29: tapar buraco, tirar calombo e deixar todo chão, caminho inclusive,
 * com degrau de no máximo 1, pondo e tirando solo; qualquer tamanho e qualquer altura. Nunca mexe em construção, peça da
 * vila, obra ou bloco do jogador, em roça e água, nem na boca e na escada da mina.
 *
 * <p>Uma janela de {@link #WINDOW}×{@link #WINDOW} por passada, que varre a caixa da vila em faixas com sobreposição; nela,
 * a mesma superfície do solo da geração ({@link VillageSurface}) com o anel da janela fixo, e só {@link #EDITS} colunas
 * por passada, as de maior diferença primeiro.
 */
final class GroundSmoothing {

    static {
        ServerMemory.register(GroundSmoothing.class, GroundSmoothing::clearAll);
    }

    /** O lado da janela. */
    static final int WINDOW = 24;

    /** O passo entre janelas: elas se sobrepõem para a borda de uma ser alisada pela outra. */
    static final int STEP = WINDOW - 6;

    /** Colunas mexidas por passada. */
    static final int EDITS = 32;

    /** Em volta da boca da mina nada é mexido: o buraco e a escada ficam. */
    static final int MINE_GUARD = 8;

    private static final Map<UUID, Integer> CURSOR = new HashMap<>();

    private GroundSmoothing() {
    }

    /** Uma passada do pedreiro: a próxima janela da caixa da vila. */
    static int pass(ServerWorld world, Colony colony, VillagerEntity mason) {
        Optional<VillageBounds> bounds = colony.bounds();

        if (bounds.isEmpty()) {
            return 0;
        }

        int cleared = clearPens(world, colony.id());

        if (cleared > 0) {
            ActionJournal.action(mason.getUuid(), "LEVELLED", "minecraft:grass_block", cleared, mason.getBlockPos(),
                    "the inside of a pen");
        }

        VillageBounds box = bounds.get();
        int across = Math.max(1, (box.maxX() - box.minX()) / STEP + 1);
        int down = Math.max(1, (box.maxZ() - box.minZ()) / STEP + 1);
        int index = CURSOR.merge(colony.id(), 1, Integer::sum) % (across * down);
        int minX = box.minX() + (index % across) * STEP;
        int minZ = box.minZ() + (index / across) * STEP;
        int edits = smoothWindow(world, colony.id(), minX, minZ, WINDOW, EDITS);

        if (edits > 0) {
            ActionJournal.action(mason.getUuid(), "LEVELLED", "minecraft:dirt", edits,
                    new BlockPos(minX + WINDOW / 2, mason.getBlockY(), minZ + WINDOW / 2), "village ground");
        }

        return edits;
    }

    /** Quanto em volta da obra o construtor parado nivela. */
    static final int LOT_MARGIN = 6;

    /**
     * O construtor com a obra parada limpa e nivela o lote — O2 (autor, 2026-10-29): sem tarefa (a obra espera material,
     * Regra 128), ele alisa o chão da obra aberta e de {@link #LOT_MARGIN} em volta; a própria obra não é mexida.
     */
    static int aroundTheOpenBuild(ServerWorld world, Colony colony, VillagerEntity builder) {
        Optional<com.villagecolony.core.construction.model.ConstructionProject> open =
                VillageColonyMod.CONSTRUCTIONS.openOf(colony.id());

        if (open.isEmpty()) {
            return 0;
        }

        BlockPos origin = MinecraftTypeAdapter.toBlockPos(open.get().origin());
        com.villagecolony.core.type.ColonyPos size = open.get().blueprint().size();
        int side = Math.max(size.x(), size.z()) + 2 * LOT_MARGIN;
        int edits = smoothWindow(world, colony.id(), origin.getX() - LOT_MARGIN, origin.getZ() - LOT_MARGIN, side, EDITS);

        if (edits > 0) {
            ActionJournal.action(builder.getUuid(), "LEVELLED", "minecraft:dirt", edits, origin, "the lot of the open build");
        }

        return edits;
    }

    /**
     * Alisa a janela de {@code size} a partir de {@code (minX, minZ)}, mexendo em até {@code budget} colunas.
     *
     * @param colonyId a colônia dona da mina a proteger; nulo no teste
     * @return quantas colunas mudaram; zero também quando a janela tem chunk não carregado
     */
    static int smoothWindow(ServerWorld world, @Nullable UUID colonyId, int minX, int minZ, int size, int budget) {
        int[][] natural = new int[size][size];
        VillageSurface.Kind[][] kinds = new VillageSurface.Kind[size][size];
        Optional<BlockPos> mine = colonyId == null ? Optional.empty() : VillageColonyMod.MINES.of(colonyId)
                .map(found -> MinecraftTypeAdapter.toBlockPos(found.shaft().entry()));
        // Dentro de construção e obra da colônia nada se mexe, nem o miolo: o cercado do jogo da 0.3.110 teve o chão
        // de dentro tampado até o alto da cerca, porque o vão entre as cercas parecia buraco.
        List<int[]> footprints = colonyId == null ? List.of() : footprintsOf(colonyId);

        for (int dx = 0; dx < size; dx++) {
            for (int dz = 0; dz < size; dz++) {
                int x = minX + dx;
                int z = minZ + dz;

                if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                    return 0;
                }

                int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                BlockPos at = new BlockPos(x, top, z);
                BlockState state = world.getBlockState(at);
                boolean ring = dx == 0 || dz == 0 || dx == size - 1 || dz == size - 1;
                boolean nearMine = mine.isPresent() && Math.abs(mine.get().getX() - x) <= MINE_GUARD
                        && Math.abs(mine.get().getZ() - z) <= MINE_GUARD;

                natural[dx][dz] = top;

                if (!state.getFluidState().isEmpty()) {
                    kinds[dx][dz] = VillageSurface.Kind.WATER;
                } else if (nearMine || insideAny(footprints, x, z) || !isGround(world, at, state)) {
                    kinds[dx][dz] = VillageSurface.Kind.SKIP;
                } else {
                    kinds[dx][dz] = ring ? VillageSurface.Kind.FIXED : VillageSurface.Kind.FREE;
                }
            }
        }

        int[][] surface = VillageSurface.smooth(natural, kinds);
        List<int[]> changes = new ArrayList<>();

        for (int dx = 0; dx < size; dx++) {
            for (int dz = 0; dz < size; dz++) {
                if (kinds[dx][dz] == VillageSurface.Kind.FREE && surface[dx][dz] != natural[dx][dz]) {
                    changes.add(new int[] {dx, dz});
                }
            }
        }

        changes.sort(Comparator.comparingInt(
                (int[] cell) -> -Math.abs(surface[cell[0]][cell[1]] - natural[cell[0]][cell[1]])));

        int done = 0;

        for (int[] cell : changes) {
            if (done >= budget) {
                break;
            }

            column(world, minX + cell[0], minZ + cell[1], natural[cell[0]][cell[1]], surface[cell[0]][cell[1]]);
            done++;
        }

        return done;
    }

    /** Quantos blocos acima do piso o miolo do cercado fica livre. */
    static final int PEN_HEADROOM = 3;

    /**
     * O miolo de cada cercado da colônia fica livre — autor, 2026-10-29 (jogo da 0.3.110: o cercado nasceu com blocos
     * de grama dentro, à altura da cerca, e ficou inútil): o chão natural e a planta acima do piso, dentro da cerca,
     * saem; o que a colônia pôs (feno, cocho) fica.
     *
     * @return quantos blocos saíram
     */
    static int clearPens(ServerWorld world, UUID colonyId) {
        int cleared = 0;

        for (com.villagecolony.core.construction.model.Building pen : VillageColonyMod.BUILDINGS.ofColony(colonyId)) {
            if (!PenPlans.isPen(pen.blueprint())) {
                continue;
            }

            for (int x = pen.min().x() + 1; x < pen.max().x(); x++) {
                for (int z = pen.min().z() + 1; z < pen.max().z(); z++) {
                    for (int y = pen.min().y() + 1; y <= pen.min().y() + PEN_HEADROOM; y++) {
                        BlockPos at = new BlockPos(x, y, z);

                        if (!world.isChunkLoaded(x >> 4, z >> 4) || BlockProtection.isColonyBuilt(at)) {
                            continue;
                        }

                        BlockState state = world.getBlockState(at);
                        boolean plant = state.isReplaceable() && !state.isAir() && state.getFluidState().isEmpty();

                        if (plant || SiteSurroundings.isNaturalGround(state)) {
                            world.setBlockState(at, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                            cleared++;
                        }
                    }
                }
            }
        }

        return cleared;
    }

    /** A caixa em x e z de cada construção da colônia e da obra aberta, com 1 de folga: {minX, minZ, maxX, maxZ}. */
    static List<int[]> footprintsOf(UUID colonyId) {
        List<int[]> boxes = new ArrayList<>();

        for (com.villagecolony.core.construction.model.Building building : VillageColonyMod.BUILDINGS.ofColony(colonyId)) {
            boxes.add(new int[] {building.min().x() - 1, building.min().z() - 1, building.max().x() + 1,
                    building.max().z() + 1});
        }

        VillageColonyMod.CONSTRUCTIONS.openOf(colonyId).ifPresent(open -> boxes.add(new int[] {
                open.origin().x() - 1, open.origin().z() - 1,
                open.origin().x() + open.blueprint().size().x(), open.origin().z() + open.blueprint().size().z()}));

        return boxes;
    }

    static boolean insideAny(List<int[]> boxes, int x, int z) {
        for (int[] box : boxes) {
            if (x >= box[0] && z >= box[1] && x <= box[2] && z <= box[3]) {
                return true;
            }
        }

        return false;
    }

    /**
     * Chão que o pedreiro pode mexer: solo natural ou caminho, fora de casa e obra da colônia; dentro de peça da vila, só
     * a rua — o telhado de arenito da casa do deserto também é "chão natural" pelo bloco.
     */
    static boolean isGround(ServerWorld world, BlockPos at, BlockState state) {
        boolean soil = SiteSurroundings.isNaturalGround(state) && !state.isOf(Blocks.FARMLAND);
        boolean path = VillageRoad.isPathBlock(state) || StreetSurface.isStreetMaterial(state);
        boolean villagePiece = BlockProtection.isVillageOriginal(world, at) && !BlockProtection.isVillageStreet(world, at);

        return (soil || path) && !villagePiece && !BlockProtection.isColonyBuilt(at)
                && BlockProtection.mayBuildOver(world, at, state);
    }

    /**
     * Corta ou aterra a coluna de {@code top} até {@code target}, com o bloco de cima e o de baixo que ela já tinha:
     * o caminho continua caminho, a grama continua grama.
     */
    static void column(ServerWorld world, int x, int z, int top, int target) {
        BlockPos.Mutable at = new BlockPos.Mutable(x, top, z);
        BlockState surface = world.getBlockState(at);
        BlockState below = world.getBlockState(at.setY(top - 1));
        BlockState fill = below.isAir() || !below.getFluidState().isEmpty() || below.isOf(Blocks.FARMLAND)
                ? Blocks.DIRT.getDefaultState() : below;

        for (int y = top; y > target; y--) {
            at.setY(y + 1);

            // A planta de cima sai junto com o chão que a segura.
            if (world.getBlockState(at).isReplaceable() && !world.getBlockState(at).isAir()) {
                world.setBlockState(at, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
            }

            world.setBlockState(at.setY(y), Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
        }

        for (int y = top; y < target; y++) {
            world.setBlockState(at.setY(y), fill, Block.NOTIFY_ALL);
        }

        world.setBlockState(at.setY(target), surface, Block.NOTIFY_ALL);
    }

    static void clearAll() {
        CURSOR.clear();
    }
}
