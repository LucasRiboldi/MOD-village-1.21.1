package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BlockProtection;
import com.villagecolony.fabric.integration.EditLedger;
import com.villagecolony.fabric.integration.PlayerGround;
import com.villagecolony.fabric.integration.SiteSurroundings;
import com.villagecolony.fabric.integration.VillageBiomes;
import com.villagecolony.fabric.world.GeneratedVillages;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * A frente da porta fica livre — Regra 356 (autor, 2026-11-25, jogo da 0.4.136: "no mínimo os aldeões devem limpar a
 * entrada da casa"). O Zelador tira o chão natural que tapa a porta térrea, na altura dela e da cabeça, nas
 * {@value #STEPS} colunas de fora em linha reta. A areia de cima sai junto, para não cair de novo; o piso que faltar
 * embaixo vira chão do bioma. O que é peça da vila, obra da colônia ou chão do jogador fica.
 */
final class DoorClearing {

    /** Quantas colunas de fora da porta ficam livres. */
    static final int STEPS = 2;

    /** Quantos blocos o Zelador tira por passada. */
    static final int BUDGET = 12;

    /** Até quantos blocos de areia acima da passagem saem junto. */
    private static final int LOOSE_ABOVE = 6;

    private DoorClearing() {
    }

    /** Uma passada: as portas das construções prontas da colônia e das casas da vila gerada; diz quantos blocos tirou. */
    static int pass(ServerWorld world, Colony colony) {
        BlockPos hub = MinecraftTypeAdapter.toBlockPos(colony.center());
        List<BlockBox> boxes = new ArrayList<>(GeneratedVillages.structuresAt(world, hub.getX(), hub.getZ()));
        int left = BUDGET;

        for (Building building : VillageColonyMod.BUILDINGS.ofColony(colony.id())) {
            if (building.finished()) {
                boxes.add(BlockBox.create(MinecraftTypeAdapter.toBlockPos(building.min()),
                        MinecraftTypeAdapter.toBlockPos(building.max())));
            }
        }

        for (BlockBox box : boxes) {
            if (left <= 0) {
                break;
            }

            left -= clear(world, new BlockPos(box.getMinX(), box.getMinY(), box.getMinZ()),
                    new BlockPos(box.getMaxX(), box.getMaxY(), box.getMaxZ()), left);
        }

        int cleared = BUDGET - left;

        if (cleared > 0) {
            VillageColonyMod.LOGGER.info("VC_DOOR_CLEAR version=1 colony={} blocks={} rule=356",
                    colony.id().toString().substring(0, 8), cleared);
        }

        return cleared;
    }

    /** Limpa a frente das portas térreas desta caixa; diz quantos blocos tirou ou pôs, até {@code budget}. */
    static int clear(ServerWorld world, BlockPos min, BlockPos max, int budget) {
        int changed = 0;

        for (DoorFronts.GroundDoor door : DoorFronts.groundDoors(world, min, max)) {
            for (int step = 1; step <= STEPS && changed < budget; step++) {
                BlockPos column = door.at().offset(door.out(), step);

                if (isInside(column, min, max) || PlayerGround.isMarked(column.getX(), column.getZ())) {
                    continue;
                }

                for (int up = 0; up <= 1 && changed < budget; up++) {
                    changed += free(world, column.up(up));
                }

                changed += floor(world, column.down());
            }
        }

        return changed;
    }

    /** Tira o chão natural deste lugar e a areia que cairia nele; diz quantos blocos tirou. */
    private static int free(ServerWorld world, BlockPos at) {
        if (!mayClear(world, at)) {
            return 0;
        }

        int removed = 0;
        BlockPos above = at.up();

        // De cima para baixo: a areia de cima sai antes, e não cai no vão aberto.
        List<BlockPos> loose = new ArrayList<>();

        for (int up = 0; up < LOOSE_ABOVE && world.getBlockState(above.up(up)).getBlock() instanceof FallingBlock
                && mayClear(world, above.up(up)); up++) {
            loose.add(above.up(up));
        }

        for (BlockPos pos : loose.reversed()) {
            removed += remove(world, pos);
        }

        return removed + remove(world, at);
    }

    private static int remove(ServerWorld world, BlockPos at) {
        world.setBlockState(at, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
        EditLedger.mod(world, at, Blocks.AIR.getDefaultState(), "porta"); // Regra 345

        return 1;
    }

    /** O piso de quem passa: o vão logo abaixo vira chão do bioma, se houver apoio embaixo dele. */
    private static int floor(ServerWorld world, BlockPos at) {
        BlockState state = world.getBlockState(at);

        if (!(state.isAir() || state.isReplaceable()) || !state.getFluidState().isEmpty()
                || !world.getBlockState(at.down()).isSideSolidFullSquare(world, at.down(), net.minecraft.util.math.Direction.UP)
                || BlockProtection.isColonyBuilt(at) || BlockProtection.isVillageOriginal(world, at)) {
            return 0;
        }

        BlockState ground = VillageBiomes.foundationGroundAt(world, at).getDefaultState();

        world.setBlockState(at, ground, Block.NOTIFY_ALL);
        EditLedger.mod(world, at, ground, "porta"); // Regra 345

        return 1;
    }

    private static boolean mayClear(ServerWorld world, BlockPos at) {
        BlockState state = world.getBlockState(at);

        return !state.isAir() && SiteSurroundings.isNaturalGround(state) && world.getBlockEntity(at) == null
                && !BlockProtection.isColonyBuilt(at) && !BlockProtection.isVillageOriginal(world, at)
                && BlockProtection.mayBreak(world, at, state);
    }

    private static boolean isInside(BlockPos column, BlockPos min, BlockPos max) {
        return column.getX() >= min.getX() && column.getX() <= max.getX()
                && column.getZ() >= min.getZ() && column.getZ() <= max.getZ();
    }
}
