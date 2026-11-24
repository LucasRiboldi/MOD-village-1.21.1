package com.villagecolony.fabric.integration;

import com.mojang.serialization.Codec;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.storage.service.StorageRegistry;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;

import java.util.UUID;

/** Reposiciona o baú residencial da colônia, com prova de origem (Regra 3 e ADR-036 item 4). */
public final class HomeChestMoves {
    private static final AttachmentType<Long> INSTALLED_FOR_BED = AttachmentRegistry.createPersistent(
            Identifier.of("villagecolony", "home_chest_bed"), Codec.LONG);

    private HomeChestMoves() {
    }

    /** Registra o tipo antes de ler os baús do mundo. */
    public static AttachmentType<Long> register() {
        return INSTALLED_FOR_BED;
    }

    static void recordPlacement(ServerWorld world, BlockPos chest, BlockPos foot) {
        if (world.getBlockEntity(chest) instanceof ChestBlockEntity placed) {
            placed.setAttached(INSTALLED_FOR_BED, foot.asLong());
            placed.markDirty();
        }
    }

    static boolean placedFor(ServerWorld world, BlockPos at, BlockPos foot) {
        return world.getBlockEntity(at) instanceof ChestBlockEntity chest
                && Long.valueOf(foot.asLong()).equals(chest.getAttached(INSTALLED_FOR_BED));
    }

    public static boolean repair(ServerWorld world, StorageRegistry storages, UUID worker, BlockPos bed, BlockBox house) {
        var association = storages.of(worker);
        if (association.isEmpty() || !Homes.isLoaded(world, house)) {
            return false;
        }

        BlockPos source = MinecraftTypeAdapter.toBlockPos(association.get().chestPosition());
        if (!house.contains(source) || !world.getBlockState(source).isOf(Blocks.CHEST)
                || world.getBlockState(source).get(Properties.CHEST_TYPE) != ChestType.SINGLE
                || !(world.getBlockEntity(source) instanceof ChestBlockEntity chest)
                || !chest.hasAttached(INSTALLED_FOR_BED) || chest.hasCustomName()
                || ChestBlockEntity.getPlayersLookingInChestCount(world, source) > 0
                || ChestMarker.hasForeignFrame(world, source)) {
            return false;
        }

        var candidate = ChestPlacer.repairSlot(world, bed, house, source, at ->
                new ChunkPos(source).equals(new ChunkPos(at))
                        && !storages.isTaken(MinecraftTypeAdapter.toColonyPos(at))
                        && world.isSpaceEmpty(new Box(at)));
        if (candidate.isEmpty()) {
            return false;
        }

        var slot = candidate.get();
        var state = Blocks.CHEST.getDefaultState().with(Properties.HORIZONTAL_FACING, slot.opening());
        var registries = world.getRegistryManager();
        var contents = chest.createNbt(registries);

        if (!world.setBlockState(slot.position(), state)) {
            return false;
        }
        if (!(world.getBlockEntity(slot.position()) instanceof ChestBlockEntity replacement)) {
            world.removeBlock(slot.position(), false);
            return false;
        }

        // Sem a entidade antiga, o callback vanilla da remoção não derruba o inventário.
        world.removeBlockEntity(source);
        if (!world.removeBlock(source, false)) {
            chest.cancelRemoval();
            world.addBlockEntity(chest);
            world.removeBlock(slot.position(), false);
            return false;
        }

        replacement.read(contents, registries);
        recordPlacement(world, slot.position(), slot.bedFoot());
        storages.register(WorkerStorage.of(worker, MinecraftTypeAdapter.toColonyPos(slot.position())));
        world.updateComparators(source, Blocks.CHEST);
        world.updateComparators(slot.position(), Blocks.CHEST);
        ChestMarker.unmark(world, association.get().chestPosition());
        VillageColonyMod.LOGGER.info("VC_HOME_CHEST_MOVED worker={} from={} to={}",
                worker, source.toShortString(), slot.position().toShortString());
        return true;
    }
}
