package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.fabric.integration.BlockProtection;
import com.villagecolony.fabric.integration.EditLedger;
import com.villagecolony.fabric.integration.MineGate;
import com.villagecolony.fabric.integration.MineMouths;
import com.villagecolony.fabric.integration.SiteSurroundings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A escada da boca da mina fica aberta — Regra 356 (autor, 2026-11-25, jogo da 0.4.136: "o portal da mina se fechou
 * sozinho"). Depois que a mina desce abaixo do primeiro nível, o caracol da boca já foi cavado; o chão natural e a pedra
 * que aparecem nele — areia que caiu, aterro, o pátio refeito — são tirados pelo Zelador, de cima para baixo. Enquanto o
 * mineiro ainda cava o primeiro nível, a escada é dele. O chão natural por cima da boca sai desde o primeiro dia, sem
 * tocar no arco (Regra 359).
 */
final class MineStairClearing {

    /** Quantos blocos o Zelador tira por passada. */
    static final int BUDGET = 16;

    private MineStairClearing() {
    }

    /** Uma passada nas bocas da superfície desta colônia; diz quantos blocos tirou. */
    static int pass(ServerWorld world, Colony colony) {
        Optional<Mine> mine = VillageColonyMod.MINES.of(colony.id());

        if (mine.isEmpty() || mine.get().shaft().isRamp()) {
            return 0;
        }

        int cleared = 0;

        for (Map.Entry<BlockPos, Direction> mouth : MineMouths.of(colony.id()).entrySet()) {
            // O céu da boca fica aberto desde o primeiro dia (Regra 359).
            cleared += clearSky(world, mouth.getKey(), mouth.getValue(), BUDGET - cleared);

            // O mineiro ainda está no primeiro nível: a escada é dele.
            if (mine.get().entry().y() > mouth.getKey().getY() - MineShaft.DESCENT) {
                continue;
            }

            cleared += clear(world, mouth.getKey(), mouth.getValue(), BUDGET - cleared);
        }

        if (cleared > 0) {
            VillageColonyMod.LOGGER.info("VC_MINE_STAIR_CLEAR version=1 colony={} blocks={} rule=356,359",
                    colony.id().toString().substring(0, 8), cleared);
        }

        return cleared;
    }

    /** Tira o que tapa o caracol desta boca, de cima para baixo; diz quantos blocos tirou, até {@code budget}. */
    static int clear(ServerWorld world, BlockPos mouth, Direction descent, int budget) {
        List<BlockPos> cells = MineMouths.stairCells(mouth, descent).stream()
                .sorted((a, b) -> Integer.compare(b.getY(), a.getY()))
                .toList();
        int cleared = 0;

        for (BlockPos cell : cells) {
            if (cleared >= budget) {
                break;
            }

            if (!world.isChunkLoaded(cell) || !refill(world, mouth, descent, cell)) {
                continue;
            }

            world.setBlockState(cell, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
            EditLedger.mod(world, cell, Blocks.AIR.getDefaultState(), "mina"); // Regra 345
            cleared++;
        }

        return cleared;
    }

    /**
     * Tira o chão natural por cima da boca, até {@link MineSky#OVERHANG_TOP} acima dela, na coluna da boca e nas do
     * caracol — areia que caiu e a sobra do morro —, de cima para baixo e sem tocar no arco (Regra 359).
     *
     * @return quantos blocos tirou, até {@code budget}
     */
    static int clearSky(ServerWorld world, BlockPos mouth, Direction descent, int budget) {
        java.util.Set<Long> columns = new java.util.LinkedHashSet<>();

        columns.add(BlockPos.asLong(mouth.getX(), 0, mouth.getZ()));
        MineMouths.stairCells(mouth, descent).forEach(cell -> columns.add(BlockPos.asLong(cell.getX(), 0, cell.getZ())));

        int cleared = 0;

        for (int dy = MineSky.OVERHANG_TOP; dy >= 1 && cleared < budget; dy--) {
            for (long column : columns) {
                BlockPos cell = new BlockPos(BlockPos.unpackLongX(column), mouth.getY() + dy, BlockPos.unpackLongZ(column));

                if (cleared >= budget || !world.isChunkLoaded(cell) || !overSky(world, mouth, descent, cell)) {
                    continue;
                }

                world.setBlockState(cell, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                EditLedger.mod(world, cell, Blocks.AIR.getDefaultState(), "mina"); // Regra 345
                cleared++;
            }
        }

        return cleared;
    }

    /** Chão natural por cima da boca, que não é peça do arco nem obra de ninguém. */
    private static boolean overSky(ServerWorld world, BlockPos mouth, Direction descent, BlockPos cell) {
        BlockState state = world.getBlockState(cell);

        return SiteSurroundings.isNaturalGround(state) && world.getBlockEntity(cell) == null
                && !MineGate.isPart(mouth, descent, cell) && !BlockProtection.isColonyBuilt(cell)
                && BlockProtection.mayBreak(world, cell, state);
    }

    /** O que entrou na escada cavada: chão natural ou pedra, que não é peça do arco nem obra de ninguém. */
    private static boolean refill(ServerWorld world, BlockPos mouth, Direction descent, BlockPos cell) {
        BlockState state = world.getBlockState(cell);

        return !state.isAir() && state.getFluidState().isEmpty()
                && (SiteSurroundings.isNaturalGround(state) || state.isOf(Blocks.STONE))
                && world.getBlockEntity(cell) == null && !MineGate.isPart(mouth, descent, cell)
                && !BlockProtection.isColonyBuilt(cell) && BlockProtection.mayBreak(world, cell, state);
    }
}
