package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.integration.BiomeSoil;
import com.villagecolony.fabric.integration.BlockProtection;
import com.villagecolony.fabric.integration.EditLedger;
import com.villagecolony.fabric.integration.PlayerGround;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O Zelador devolve ao chão de areia o bloco do bioma — Regra 363 (G1 🅲 do jogo da 0.4.136, notas do jogo da 0.4.141).
 * A terra no topo de um chão de areia (cinco ou mais das oito colunas vizinhas com topo de areia) volta a ser areia; a
 * terra e a grama até {@value #DEPTH} blocos debaixo de um topo de areia viram arenito, que segura a areia. Fica de fora a
 * coluna do jogador (Regra 334), a obra da colônia, a peça da vila que não é chão do bioma e a terra a até 2 blocos de
 * uma roça (terra arada ou planta). Anda pela caixa da vila aos poucos, {@value #SCAN} colunas por passada e até
 * {@value #BUDGET} trocas.
 */
final class SoilRestore {

    static final int BUDGET = 32;

    static final int SCAN = 1024;

    static final int DEPTH = 3;

    /** Quantas das oito vizinhas precisam ter topo de areia para o chão ser de areia. */
    static final int SANDY = 5;

    private static final Map<UUID, Integer> NEXT = new HashMap<>();

    static {
        ServerMemory.register(SoilRestore.class, NEXT::clear);
    }

    private SoilRestore() {
    }

    /** Uma passada nesta colônia; diz quantos blocos trocou. */
    static int pass(ServerWorld world, Colony colony) {
        Optional<VillageBounds> bounds = colony.bounds();

        if (bounds.isEmpty()) {
            return 0;
        }

        VillageBounds box = bounds.get();
        int width = box.maxX() - box.minX() + 1;
        int columns = width * (box.maxZ() - box.minZ() + 1);
        int next = NEXT.getOrDefault(colony.id(), 0) % Math.max(1, columns);
        int changed = 0;

        for (int step = 0; step < Math.min(SCAN, columns) && changed < BUDGET; step++) {
            int index = (next + step) % columns;

            changed += column(world, box.minX() + index % width, box.minZ() + index / width, BUDGET - changed);
            NEXT.put(colony.id(), index + 1);
        }

        if (changed > 0) {
            VillageColonyMod.LOGGER.info("VC_SOIL_RESTORE version=1 colony={} blocks={} rule=363",
                    colony.id().toString().substring(0, 8), changed);
        }

        return changed;
    }

    static void forget(UUID colonyId) {
        NEXT.remove(colonyId);
    }

    private static int column(ServerWorld world, int x, int z, int budget) {
        if (!world.isChunkLoaded(x >> 4, z >> 4) || PlayerGround.isMarked(x, z)) {
            return 0;
        }

        BlockPos top = new BlockPos(x, world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
        BlockState surface = world.getBlockState(top);
        int changed = 0;

        if (isDirt(surface) && mayChange(world, top) && sandyAround(world, top) && !nearAFarm(world, top)) {
            put(world, top, Blocks.SAND.getDefaultState());
            surface = Blocks.SAND.getDefaultState();
            changed++;
        }

        BlockState firm = surface.isIn(BlockTags.SAND) ? BiomeSoil.firmFor(surface) : null;

        for (int depth = 1; firm != null && depth <= DEPTH && changed < budget; depth++) {
            BlockPos at = top.down(depth);
            BlockState state = world.getBlockState(at);

            if ((isDirt(state) || state.isOf(Blocks.GRASS_BLOCK)) && mayChange(world, at) && !nearAFarm(world, top)) {
                put(world, at, firm);
                changed++;
            }
        }

        return changed;
    }

    private static boolean isDirt(BlockState state) {
        return state.isOf(Blocks.DIRT) || state.isOf(Blocks.COARSE_DIRT);
    }

    private static boolean mayChange(ServerWorld world, BlockPos at) {
        return !BlockProtection.isColonyBuilt(at)
                && !(BlockProtection.isVillageOriginal(world, at) && !com.villagecolony.fabric.integration.LotGround
                        .isBiomeGround(world, at));
    }

    private static boolean sandyAround(ServerWorld world, BlockPos top) {
        int sandy = 0;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if ((dx != 0 || dz != 0) && world.isChunkLoaded((top.getX() + dx) >> 4, (top.getZ() + dz) >> 4)) {
                    int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, top.getX() + dx, top.getZ() + dz) - 1;

                    sandy += world.getBlockState(new BlockPos(top.getX() + dx, y, top.getZ() + dz)).isIn(BlockTags.SAND)
                            ? 1 : 0;
                }
            }
        }

        return sandy >= SANDY;
    }

    /** Terra arada ou planta a até 2 blocos, na altura do topo ou um acima ou abaixo: a terra é da roça. */
    private static boolean nearAFarm(ServerWorld world, BlockPos top) {
        for (BlockPos at : BlockPos.iterate(top.add(-2, -1, -2), top.add(2, 2, 2))) {
            BlockState state = world.getBlockState(at);

            if (state.isOf(Blocks.FARMLAND) || state.getBlock() instanceof CropBlock) {
                return true;
            }
        }

        return false;
    }

    private static void put(ServerWorld world, BlockPos at, BlockState state) {
        world.setBlockState(at, state, Block.NOTIFY_ALL);
        EditLedger.mod(world, at.toImmutable(), state, "solo_do_bioma"); // Regra 345
    }
}
