package com.villagecolony.fabric.integration;

import com.mojang.serialization.Codec;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.storage.service.StorageRegistry;
import com.villagecolony.core.type.ServerMemory;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reposiciona o baú residencial da colônia, com prova de origem (Regra 3 e ADR-036 item 4).
 *
 * <p>O baú que perdeu a parede e não mudou de lugar sai no log como {@code VC_HOME_CHEST_STAY}, com o motivo, uma vez
 * por mudança de motivo de cada morador.
 */
public final class HomeChestMoves {
    private static final AttachmentType<Long> INSTALLED_FOR_BED = AttachmentRegistry.createPersistent(
            Identifier.of("villagecolony", "home_chest_bed"), Codec.LONG);

    /** O último motivo registrado por morador, para não repetir a linha a cada ciclo. */
    private static final Map<UUID, String> LAST_STAY = new ConcurrentHashMap<>();

    static {
        ServerMemory.register(HomeChestMoves.class, LAST_STAY::clear);
    }

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
        Optional<String> refusal = move(world, storages, worker, bed, house);
        if (refusal.isEmpty()) {
            LAST_STAY.remove(worker);
            return true;
        }
        Optional<BlockPos> source = storages.of(worker).map(s -> MinecraftTypeAdapter.toBlockPos(s.chestPosition()));
        boolean wallLost = source.isPresent() && world.getBlockState(source.get()).isOf(Blocks.CHEST)
                && !ChestPlacer.leansOnWall(world, source.get());
        if (wallLost && !refusal.get().equals(LAST_STAY.put(worker, refusal.get()))) {
            VillageColonyMod.LOGGER.info("VC_HOME_CHEST_STAY worker={} chest={} bed={} reason={}",
                    worker, source.get().toShortString(), bed.toShortString(), refusal.get());
        }
        return false;
    }

    /** Muda o baú de lugar; devolve o motivo quando não muda, vazio quando mudou. */
    public static Optional<String> move(ServerWorld world, StorageRegistry storages, UUID worker, BlockPos bed,
                                        BlockBox house) {
        var association = storages.of(worker);
        if (association.isEmpty()) {
            return Optional.of("no_chest_registered");
        }
        if (!Homes.isLoaded(world, house)) {
            return Optional.of("house_not_loaded");
        }

        BlockPos source = MinecraftTypeAdapter.toBlockPos(association.get().chestPosition());
        Optional<String> unprovable = unprovable(world, house, source);
        if (unprovable.isPresent()) {
            return unprovable;
        }
        ChestBlockEntity chest = (ChestBlockEntity) world.getBlockEntity(source);

        // A casa inteira já está carregada; o destino pode cair no chunk vizinho.
        List<String> why = new ArrayList<>();
        var candidate = ChestPlacer.repairSlot(world, bed, house, source, at ->
                world.getChunkManager().isChunkLoaded(at.getX() >> 4, at.getZ() >> 4)
                        && !storages.isTaken(MinecraftTypeAdapter.toColonyPos(at))
                        && world.isSpaceEmpty(new Box(at)), why);
        if (candidate.isEmpty()) {
            return Optional.of("no_slot" + why);
        }

        var slot = candidate.get();
        var state = Blocks.CHEST.getDefaultState().with(Properties.HORIZONTAL_FACING, slot.opening());
        var registries = world.getRegistryManager();
        var contents = chest.createNbt(registries);

        if (!world.setBlockState(slot.position(), state)) {
            return Optional.of("place_failed");
        }
        if (!(world.getBlockEntity(slot.position()) instanceof ChestBlockEntity replacement)) {
            world.removeBlock(slot.position(), false);
            return Optional.of("no_block_entity");
        }

        // Sem a entidade antiga, o callback vanilla da remoção não derruba o inventário.
        world.removeBlockEntity(source);
        if (!world.removeBlock(source, false)) {
            chest.cancelRemoval();
            world.addBlockEntity(chest);
            world.removeBlock(slot.position(), false);
            return Optional.of("source_not_removed");
        }

        replacement.read(contents, registries);
        recordPlacement(world, slot.position(), slot.bedFoot());
        storages.register(WorkerStorage.of(worker, MinecraftTypeAdapter.toColonyPos(slot.position())));
        world.updateComparators(source, Blocks.CHEST);
        world.updateComparators(slot.position(), Blocks.CHEST);
        ChestMarker.unmark(world, association.get().chestPosition());
        VillageColonyMod.LOGGER.info("VC_HOME_CHEST_MOVED worker={} from={} to={}",
                worker, source.toShortString(), slot.position().toShortString());
        return Optional.empty();
    }

    /** Por que o baú registrado não pode ser mudado: fora da casa, duplo, sem prova de origem, aberto ou marcado. */
    private static Optional<String> unprovable(ServerWorld world, BlockBox house, BlockPos source) {
        if (!house.contains(source)) {
            return Optional.of("source_outside_house");
        }
        if (!world.getBlockState(source).isOf(Blocks.CHEST)) {
            return Optional.of("source_not_chest");
        }
        if (world.getBlockState(source).get(Properties.CHEST_TYPE) != ChestType.SINGLE) {
            return Optional.of("double_chest");
        }
        if (!(world.getBlockEntity(source) instanceof ChestBlockEntity chest)) {
            return Optional.of("no_chest_entity");
        }
        if (!chest.hasAttached(INSTALLED_FOR_BED)) {
            return Optional.of("no_placement_receipt");
        }
        if (chest.hasCustomName()) {
            return Optional.of("named_by_player");
        }
        if (ChestBlockEntity.getPlayersLookingInChestCount(world, source) > 0) {
            return Optional.of("player_looking");
        }
        if (ChestMarker.hasForeignFrame(world, source)) {
            return Optional.of("foreign_frame");
        }
        return Optional.empty();
    }
}
