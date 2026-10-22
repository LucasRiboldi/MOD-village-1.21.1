package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.Corridor;
import com.villagecolony.fabric.integration.LotGround;
import com.villagecolony.fabric.integration.LotTerraform;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * O corte do corredor na encosta, na abertura da obra — Regra 70, degrau cortado (autor, 2026-10-22).
 *
 * <p>O {@link Corridor} da faixa anexada foi planejado com cada bloco na altura que ele tem depois do
 * corte. Aqui a coluna de cada bloco que corta é aberta até ele, e as duas colunas do lado até a mesma
 * altura: o caminho continua com um bloco, num vale de três ("trincheira de 3", escolha do autor). O
 * caminho no terreno natural fica como está. Corta-se só o que o lote cortaria
 * ({@link LotTerraform#cutAbove}); o que sai vai para os baús públicos da vila.
 */
final class CorridorCut {

    private CorridorCut() {
    }

    /**
     * Abre a encosta para o caminho {@code path}, da porta até a rua.
     *
     * @return quantos blocos saíram
     */
    static int cut(ServerWorld world, UUID colonyId, List<ColonyPos> path) {
        if (path.size() < 2) {
            return 0;
        }

        Set<Long> columns = new HashSet<>();

        for (ColonyPos cell : path) {
            columns.add(column(cell.x(), cell.z()));
        }

        Map<Item, Integer> taken = new LinkedHashMap<>();
        int moved = 0;

        for (int i = 0; i < path.size(); i++) {
            BlockPos at = MinecraftTypeAdapter.toBlockPos(path.get(i));

            if (LotTerraform.cutAbove(world, at, Corridor.MAX_CUT).orElse(0) == 0) {
                continue;
            }

            moved += open(world, at, taken);

            for (Direction side : sidesOf(path, i)) {
                BlockPos beside = at.offset(side);

                if (!columns.contains(column(beside.getX(), beside.getZ()))
                        && LotTerraform.cutAbove(world, beside, Corridor.MAX_CUT).isPresent()) {
                    moved += open(world, beside, taken);
                }
            }
        }

        BlockPos door = MinecraftTypeAdapter.toBlockPos(path.get(0));

        taken.forEach((item, amount) -> PublicChests.store(world, colonyId, door, item, amount));

        if (moved > 0) {
            VillageColonyMod.LOGGER.info("Colony {} cut {} blocks of the slope for the path from {} to the road",
                    colonyId.toString().substring(0, 8), moved, door.toShortString());
        }

        return moved;
    }

    /** Os lados de quem anda por este bloco: perpendiculares à reta que chega e à que sai. */
    static Set<Direction> sidesOf(List<ColonyPos> path, int i) {
        Set<Direction> sides = new HashSet<>();

        for (int j : new int[] {i - 1, i + 1}) {
            if (j < 0 || j >= path.size()) {
                continue;
            }

            Direction way = Direction.fromVector(
                    path.get(j).x() - path.get(i).x(), 0, path.get(j).z() - path.get(i).z());

            if (way != null && way.getAxis().isHorizontal()) {
                sides.add(way.rotateYClockwise());
                sides.add(way.rotateYCounterclockwise());
            }
        }

        return sides;
    }

    /** Tira tudo acima de {@code floor} até o céu, e a planta que ficaria flutuando no topo. */
    private static int open(ServerWorld world, BlockPos floor, Map<Item, Integer> taken) {
        int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, floor.getX(), floor.getZ()) - 1;
        BlockPos plant = new BlockPos(floor.getX(), top + 1, floor.getZ());
        int moved = 0;

        if (top > floor.getY() && !world.getBlockState(plant).isAir() && LotGround.isNothing(world.getBlockState(plant))) {
            world.setBlockState(plant, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
        }

        for (int y = top; y > floor.getY(); y--) {
            BlockPos pos = new BlockPos(floor.getX(), y, floor.getZ());
            BlockState state = world.getBlockState(pos);

            if (state.isAir()) {
                continue;
            }

            Item item = state.getBlock().asItem();

            if (item != Items.AIR && !LotGround.isNothing(state)) {
                taken.merge(item, 1, Integer::sum);
            }

            world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
            moved++;
        }

        return moved;
    }

    private static long column(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }
}
