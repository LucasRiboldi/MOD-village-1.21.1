package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.VillageSurface;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Map;

/**
 * O chão em volta da obra suavizado como o da vila gerada — Regra 297 (autor, 2026-11-15, jogo da 0.4.88: parede de 4
 * colada no curral e na plantação). Numa janela de {@link #WINDOW} blocos em volta da pegada, a mesma conta da geração
 * ({@link VillageSurface#smooth}) leva o chão a degraus de no máximo 1 por lado.
 *
 * <p>Na conta, ficam na altura que têm: a pegada (na altura da rua), o caminho calçado e a borda da janela (no chão
 * natural, para a encosta encontrar o terreno de fora). A água fica e a margem desce até ela. A obra manda: a borda
 * fica no máximo a um bloco por passo do piso (o morro mais íngreme que isso guarda o degrau na borda, longe da obra), e
 * o caminho e a água que não cabem nesse degrau saem da conta. Outra construção, árvore, peça da vila, bloco do jogador
 * e chão acima do alcance da busca ficam fora da conta. Só se corta chão natural que pode sair
 * ({@link BlockProtection#mayDigOut}) e só se aterra o vazio, com o chão do bioma (Regra 111, {@code FOUNDATION_GROUND}).
 */
final class SiteSmoothing {

    /** Até onde, em volta da pegada, o chão é suavizado. */
    static final int WINDOW = 12;

    /** Quanto acima e abaixo da rua se procura o chão de cada coluna. */
    static final int SCAN = WINDOW + 4;

    private SiteSmoothing() {
    }

    /** O que a suavização fez: blocos mudados, blocos aterrados e o maior degrau que ficou na janela. */
    record Result(int changed, int filled, int maxStep, int steep, BlockPos worstAt) {
    }

    /** A coluna lida: o papel na conta, a altura do topo e o bloco do topo. */
    private record Column(VillageSurface.Kind kind, int top, BlockState surface) {
    }

    private static final Column NONE = new Column(VillageSurface.Kind.SKIP, 0, Blocks.AIR.getDefaultState());

    /** Suaviza a janela em volta da pegada {@code (x0, z0)–(x1, z1)} com piso na altura da rua {@code roadY}. */
    static Result smooth(ServerWorld world, int x0, int z0, int x1, int z1, int roadY, List<Building> others,
                         Map<Item, Integer> cut) {
        int minX = x0 - WINDOW;
        int minZ = z0 - WINDOW;
        int width = x1 - x0 + 1 + 2 * WINDOW;
        int depth = z1 - z0 + 1 + 2 * WINDOW;
        Column[][] columns = read(world, minX, minZ, width, depth, x0, z0, x1, z1, roadY, others);
        int[][] natural = new int[width][depth];
        VillageSurface.Kind[][] kinds = new VillageSurface.Kind[width][depth];

        for (int gx = 0; gx < width; gx++) {
            for (int gz = 0; gz < depth; gz++) {
                Column column = columns[gx][gz];
                boolean border = gx == 0 || gz == 0 || gx == width - 1 || gz == depth - 1;
                int x = minX + gx;
                int z = minZ + gz;
                int reach = Math.max(0, Math.max(x0 - x, x - x1)) + Math.max(0, Math.max(z0 - z, z - z1));
                boolean footprint = reach == 0;
                natural[gx][gz] = column.top();
                kinds[gx][gz] = column.kind();

                // A obra manda: o que fica fixo longe dela só fica se couber no degrau de 1 até o piso.
                if (border && column.kind() == VillageSurface.Kind.FREE) {
                    kinds[gx][gz] = VillageSurface.Kind.FIXED;
                    natural[gx][gz] = Math.max(roadY - reach, Math.min(roadY + reach, column.top()));
                } else if (!footprint && column.kind() != VillageSurface.Kind.FREE
                        && Math.abs(column.top() - roadY) > reach) {
                    kinds[gx][gz] = VillageSurface.Kind.SKIP;
                }
            }
        }

        int[][] target = VillageSurface.smooth(natural, kinds);
        int[] filled = new int[1];
        int changed = 0;

        for (int gx = 0; gx < width; gx++) {
            for (int gz = 0; gz < depth; gz++) {
                if (kinds[gx][gz] == VillageSurface.Kind.FREE && target[gx][gz] != natural[gx][gz]) {
                    changed += levelTo(world, minX + gx, minZ + gz, columns[gx][gz], target[gx][gz], cut, filled);
                }
            }
        }

        return measure(read(world, minX, minZ, width, depth, x0, z0, x1, z1, roadY, others), minX, minZ,
                changed, filled[0]);
    }

    private static Column[][] read(ServerWorld world, int minX, int minZ, int width, int depth, int x0, int z0, int x1,
                                   int z1, int roadY, List<Building> others) {
        Column[][] columns = new Column[width][depth];

        for (int gx = 0; gx < width; gx++) {
            for (int gz = 0; gz < depth; gz++) {
                int x = minX + gx;
                int z = minZ + gz;

                if (x >= x0 && x <= x1 && z >= z0 && z <= z1) {
                    columns[gx][gz] = new Column(VillageSurface.Kind.FIXED, roadY, Blocks.AIR.getDefaultState());
                } else if (insideAny(others, x, z) || !world.isChunkLoaded(x >> 4, z >> 4)) {
                    columns[gx][gz] = NONE;
                } else {
                    columns[gx][gz] = column(world, x, z, roadY);
                }
            }
        }

        return columns;
    }

    /**
     * O topo da coluna perto da rua: chão natural é livre; caminho calçado e chão acima do alcance da busca ficam;
     * água é água; o resto (tronco, construção, bloco do jogador) fica fora da conta.
     */
    private static Column column(ServerWorld world, int x, int z, int roadY) {
        for (int y = roadY + SCAN; y >= roadY - SCAN; y--) {
            BlockPos at = new BlockPos(x, y, z);
            BlockState state = world.getBlockState(at);

            // A barreira (teto da arena, mapa do jogador), a folha e o enfeite de luz não são chão: passa por eles.
            if (state.isIn(BlockTags.LEAVES) || state.isOf(Blocks.BARRIER) || LotGround.isLightDecor(state)
                    || (LotGround.isNothing(state) && state.getFluidState().isEmpty())) {
                continue;
            }

            if (!state.getFluidState().isEmpty()) {
                return new Column(VillageSurface.Kind.WATER, y, state);
            }

            // Chão já no alto da busca: morro além do alcance, que não se corta nem puxa as vizinhas.
            if (y == roadY + SCAN) {
                return new Column(VillageSurface.Kind.SKIP, y, state);
            }

            if (VillageRoad.isPathBlock(state)) {
                return new Column(VillageSurface.Kind.FIXED, y, state);
            }

            if (SiteSurroundings.isNaturalGround(state) && BlockProtection.mayDigOut(world, at, state)) {
                return new Column(VillageSurface.Kind.FREE, y, state);
            }

            return new Column(VillageSurface.Kind.SKIP, y, state);
        }

        return NONE;
    }

    /** Corta ou aterra a coluna até {@code target}; o bloco de cima (a grama) vai junto para o topo novo. */
    private static int levelTo(ServerWorld world, int x, int z, Column column, int target, Map<Item, Integer> cut,
                               int[] filled) {
        int changed = 0;
        int top = column.top();

        if (target < top) {
            int y = top;

            for (; y > target; y--) {
                BlockPos at = new BlockPos(x, y, z);
                BlockState state = world.getBlockState(at);

                if (!SiteSurroundings.isNaturalGround(state) || !BlockProtection.mayDigOut(world, at, state)) {
                    break;
                }

                cut.merge(state.getBlock().asItem(), 1, Integer::sum);
                world.setBlockState(at, Blocks.AIR.getDefaultState());
                changed++;
            }

            resurface(world, new BlockPos(x, y, z), column.surface());
        } else {
            int y = top + 1;

            for (; y <= target; y++) {
                BlockPos at = new BlockPos(x, y, z);
                BlockState state = world.getBlockState(at);

                if ((!LotGround.isNothing(state) && !state.isAir()) || !state.getFluidState().isEmpty()) {
                    break;
                }

                world.setBlockState(at, VillageBiomes.foundationGroundAt(world, at).getDefaultState());
                changed++;
                filled[0]++;
            }

            resurface(world, new BlockPos(x, y - 1, z), column.surface());
        }

        return changed;
    }

    /** O topo novo ganha o bloco de cima de antes (grama, areia), se ele for terra e o topo novo for chão natural. */
    private static void resurface(ServerWorld world, BlockPos at, BlockState surface) {
        BlockState now = world.getBlockState(at);

        if (surface.isIn(BlockTags.DIRT) && !now.isOf(surface.getBlock()) && SiteSurroundings.isNaturalGround(now)
                && BlockProtection.mayDigOut(world, at, now)) {
            world.setBlockState(at, surface);
        }
    }

    /** O maior degrau por lado entre colunas de chão da janela, depois da suavização. */
    private static Result measure(Column[][] columns, int minX, int minZ, int changed, int filled) {
        int worst = 0;
        int steep = 0;
        BlockPos worstAt = null;

        for (int gx = 1; gx < columns.length - 1; gx++) {
            for (int gz = 1; gz < columns[gx].length - 1; gz++) {
                if (columns[gx][gz].kind() == VillageSurface.Kind.SKIP) {
                    continue;
                }

                for (int[] side : new int[][] {{1, 0}, {0, 1}}) {
                    Column next = columns[gx + side[0]][gz + side[1]];

                    if (next.kind() == VillageSurface.Kind.SKIP || gx + side[0] == columns.length - 1
                            || gz + side[1] == columns[gx].length - 1) {
                        continue;
                    }

                    int step = Math.abs(columns[gx][gz].top() - next.top());

                    if (step > 1) {
                        steep++;
                    }

                    if (step > worst) {
                        worst = step;
                        worstAt = new BlockPos(minX + gx, columns[gx][gz].top(), minZ + gz);
                    }
                }
            }
        }

        return new Result(changed, filled, worst, steep, worstAt);
    }

    private static boolean insideAny(List<Building> buildings, int x, int z) {
        for (Building building : buildings) {
            if (x >= building.min().x() && x <= building.max().x() && z >= building.min().z() && z <= building.max().z()) {
                return true;
            }
        }

        return false;
    }
}
