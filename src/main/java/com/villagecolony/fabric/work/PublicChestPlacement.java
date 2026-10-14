package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ColonyChests;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A vila sem baú público ganha um — regra do autor, 2026-10-12: <i>"ela cria um baú
 * dentro de alguma casa com espaço, a ponto de não ficar na frente de porta e encostado
 * em uma parede"</i>. É para onde vai o excedente de madeira e pedra crua (Regra 58).
 *
 * <p>O lugar: dentro de uma casa da colônia (chão firme, bloco em cima até a altura da
 * casa, ar acima do baú),
 * com uma parede cheia ao lado, sem porta a até {@link #DOOR_CLEARANCE} blocos e sem
 * baú, cama ou bloco de oficina encostado. O baú abre para longe da parede. Sai dos
 * baús um baú pronto ou oito tábuas; sem nenhum dos dois, espera.
 */
public final class PublicChestPlacement {

    static {
        ServerMemory.register(PublicChestPlacement.class, PublicChestPlacement::clearAll);
    }

    /** Distância mínima, em blocos, de qualquer porta. */
    static final int DOOR_CLEARANCE = 2;

    /** Sem lugar ou sem material, quanto tempo até tentar de novo. */
    static final long RETRY = 6_000;

    /** As tábuas que viram baú, oito de uma madeira só. */
    private static final List<Item> PLANKS = List.of(
            Items.OAK_PLANKS, Items.SPRUCE_PLANKS, Items.BIRCH_PLANKS, Items.JUNGLE_PLANKS, Items.ACACIA_PLANKS,
            Items.DARK_OAK_PLANKS, Items.MANGROVE_PLANKS, Items.CHERRY_PLANKS);

    private static final Map<java.util.UUID, Long> TRIED_AT = new HashMap<>();

    private PublicChestPlacement() {
    }

    /**
     * Põe o baú público quando a vila não tem nenhum.
     *
     * @return onde o baú foi posto
     */
    public static Optional<BlockPos> ensure(ServerWorld world, Colony colony) {
        BlockPos center = MinecraftTypeAdapter.toBlockPos(colony.center());
        long now = world.getTime();

        if (!PublicChests.of(world, colony.id(), center).isEmpty()
                || now - TRIED_AT.getOrDefault(colony.id(), Long.MIN_VALUE / 2) < RETRY) {
            return Optional.empty();
        }

        TRIED_AT.put(colony.id(), now);

        for (Building building : VillageColonyMod.BUILDINGS.ofColony(colony.id())) {
            if (!building.finished() || !HousePlans.isHouse(building.blueprint())) {
                continue;
            }

            Optional<Spot> spot = spotIn(world, building);

            if (spot.isEmpty()) {
                continue;
            }

            if (!takeMaterial(world, colony, center)) {
                VillageColonyMod.LOGGER.info("Colony {} has no public chest and no chest or eight planks to make one",
                        colony.id().toString().substring(0, 8));

                return Optional.empty();
            }

            world.setBlockState(spot.get().at(),
                    Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, spot.get().facing()), Block.NOTIFY_ALL);
            VillageColonyMod.LOGGER.info("Colony {} had no public chest — placed one at {} in {}, against a wall",
                    colony.id().toString().substring(0, 8), spot.get().at().toShortString(), building.blueprint());

            return Optional.of(spot.get().at());
        }

        VillageColonyMod.LOGGER.info("Colony {} has no public chest and no house with room for one",
                colony.id().toString().substring(0, 8));

        return Optional.empty();
    }

    /** O lugar do baú e para onde ele abre. */
    record Spot(BlockPos at, Direction facing) {
    }

    /** O primeiro lugar desta casa que serve. */
    static Optional<Spot> spotIn(ServerWorld world, Building house) {
        ColonyPos min = house.min();
        ColonyPos max = house.max();

        for (int y = min.y(); y <= max.y(); y++) {
            for (int x = min.x() + 1; x < max.x(); x++) {
                for (int z = min.z() + 1; z < max.z(); z++) {
                    BlockPos at = new BlockPos(x, y, z);
                    Optional<Direction> wall = wallBeside(world, at);

                    if (wall.isPresent() && fits(world, at) && roofed(world, at, max.y())) {
                        return Optional.of(new Spot(at, wall.get().getOpposite()));
                    }
                }
            }
        }

        return Optional.empty();
    }

    private static boolean fits(ServerWorld world, BlockPos at) {
        if (!world.isChunkLoaded(at) || !world.getBlockState(at).isAir() || !world.getBlockState(at.up()).isAir()
                || !world.getBlockState(at.down()).isSideSolidFullSquare(world, at.down(), Direction.UP)) {
            return false;
        }

        for (BlockPos near : BlockPos.iterate(at.add(-DOOR_CLEARANCE, -1, -DOOR_CLEARANCE),
                at.add(DOOR_CLEARANCE, 1, DOOR_CLEARANCE))) {
            if (world.getBlockState(near).isIn(BlockTags.DOORS)) {
                return false;
            }
        }

        for (Direction side : Direction.Type.HORIZONTAL) {
            BlockState next = world.getBlockState(at.offset(side));

            if (next.getBlock() instanceof ChestBlock || next.isIn(BlockTags.BEDS)
                    || world.getBlockEntity(at.offset(side)) != null) {
                return false;
            }
        }

        return true;
    }

    /** Dentro da casa: há bloco em cima, até a altura dela (a luz do céu atrasa, a geometria não). */
    private static boolean roofed(ServerWorld world, BlockPos at, int top) {
        for (int y = at.getY() + 2; y <= top; y++) {
            if (!world.getBlockState(at.withY(y)).isAir()) {
                return true;
            }
        }

        return false;
    }

    /** O lado em que há uma parede cheia, se há. */
    private static Optional<Direction> wallBeside(ServerWorld world, BlockPos at) {
        for (Direction side : Direction.Type.HORIZONTAL) {
            BlockPos next = at.offset(side);
            BlockState state = world.getBlockState(next);

            if (state.isFullCube(world, next) && world.getBlockEntity(next) == null) {
                return Optional.of(side);
            }
        }

        return Optional.empty();
    }

    /** Um baú pronto dos baús da colônia, ou oito tábuas de uma madeira. */
    private static boolean takeMaterial(ServerWorld world, Colony colony, BlockPos center) {
        List<ColonyPos> chests = ColonyChests.nearestFirst(world, colony.id(), MinecraftTypeAdapter.toColonyPos(center));

        if (ColonyChests.withdraw(world, chests, Items.CHEST, 1) == 1) {
            return true;
        }

        for (Item planks : PLANKS) {
            if (ColonyChests.countIn(world, chests, planks) >= 8) {
                return ColonyChests.withdraw(world, chests, planks, 8) == 8;
            }
        }

        return false;
    }

    static void clearAll() {
        TRIED_AT.clear();
    }
}
