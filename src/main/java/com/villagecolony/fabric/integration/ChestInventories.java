package com.villagecolony.fabric.integration;

import com.villagecolony.core.storage.model.VillageChestRule;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.inventory.Inventory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

/** Resolve baús simples e duplos sem forçar carregamento de chunks. */
final class ChestInventories {

    private ChestInventories() {
    }

    static Optional<Handle> at(ServerWorld world, ColonyPos chest) {
        BlockPos position = MinecraftTypeAdapter.toBlockPos(chest);
        WorldChunk chunk = world.getChunkManager()
                .getWorldChunk(position.getX() >> 4, position.getZ() >> 4);
        if (chunk == null) {
            return Optional.empty();
        }

        BlockState state = chunk.getBlockState(position);
        if (!(state.getBlock() instanceof ChestBlock block)) {
            return Optional.empty();
        }

        Inventory inventory = ChestBlock.getInventory(block, state, world, position, true);
        if (inventory == null) {
            return Optional.empty();
        }

        Set<ColonyPos> members = new LinkedHashSet<>();
        members.add(chest);
        ChestType type = state.get(ChestBlock.CHEST_TYPE);
        if (type != ChestType.SINGLE) {
            net.minecraft.util.math.Direction facing = state.get(ChestBlock.FACING);
            net.minecraft.util.math.Direction partner = type == ChestType.LEFT
                    ? facing.rotateYClockwise()
                    : facing.rotateYCounterclockwise();
            BlockPos neighbor = position.offset(partner);
            members.add(MinecraftTypeAdapter.toColonyPos(neighbor));
        }

        return Optional.of(new Handle(inventory, Set.copyOf(members), isNamed(world, members)));
    }

    /** Se o jogador nomeou alguma metade do baú ({@link VillageChestRule}). */
    private static boolean isNamed(ServerWorld world, Set<ColonyPos> members) {
        for (ColonyPos member : members) {
            BlockPos pos = MinecraftTypeAdapter.toBlockPos(member);
            WorldChunk chunk = world.getChunkManager().getWorldChunk(pos.getX() >> 4, pos.getZ() >> 4);

            if (chunk != null
                    && chunk.getBlockEntity(pos) instanceof ChestBlockEntity chest
                    && chest.getCustomName() != null
                    && !VillageChestRule.mayTake(Optional.of(chest.getCustomName().getString()))) {
                return true;
            }
        }

        return false;
    }

    record Handle(Inventory inventory, Set<ColonyPos> members, boolean named) {
        boolean isProfession(Set<ColonyPos> professionChests) {
            return members.stream().anyMatch(professionChests::contains);
        }

        ColonyPos key() {
            return members.stream()
                    .min(java.util.Comparator.comparingInt(ColonyPos::x)
                            .thenComparingInt(ColonyPos::y)
                            .thenComparingInt(ColonyPos::z))
                    .orElseThrow();
        }
    }
}
