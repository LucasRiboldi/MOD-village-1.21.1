package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.storage.service.StorageRegistry;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.work.WorkerHousingNeeds;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Todo trabalhador de profissão tem um baú — decisão do autor, 2026-09-26:
 * <i>"todos aldeões de profissão devem ter um baú nascido destinado a cada um
 * deles"</i>.
 *
 * <p><b>O defeito que isto fecha.</b> Na sessão longa de 26-09 o lenhador, o
 * mineiro e o pedreiro passaram 4h45 sem baú: o baú era só <i>achado</i> — um
 * livre a até seis blocos da cama, no mesmo cômodo —, e quem não achava ficava
 * sem. A distribuição de tarefas exige baú, então as tarefas de madeira e
 * pedra ficaram abertas a sessão inteira sem ninguém que pudesse pegá-las.
 *
 * <p><b>Onde o baú nasce.</b> Ao lado da cama dele, dentro da estrutura que a
 * contém, encostado numa parede e nunca diante da porta. Sem cama, quarto ou
 * posição segura, não nasce baú: o trabalhador espera até haver uma estrutura
 * válida, sem ocupar o centro da vila ou a entrada de uma casa.
 */
public final class ChestSpawner {

    static {
        ServerMemory.register(ChestSpawner.class, ChestSpawner::clearAll);
    }

    private static final Set<UUID> WARNED = new HashSet<>();

    private ChestSpawner() {
    }

    /**
     * Garante um baú para este trabalhador: põe no mundo e registra.
     *
     * @return o baú, ou vazio quando a cama não pertence a uma estrutura ou
     *     não há posição segura ao lado dela
     */
    public static Optional<WorkerStorage> ensureChest(
            ServerWorld world,
            VillagerEntity villager,
            StorageRegistry storages,
            UUID colonyId,
            String profession) {

        if (storages.hasStorage(villager.getUuid())) {
            housingResolved(colonyId, villager.getUuid());
            return storages.of(villager.getUuid());
        }

        Optional<BlockPos> bed = villager.getBrain()
                .getOptionalRegisteredMemory(MemoryModuleType.HOME)
                .filter(home -> home.dimension().equals(world.getRegistryKey()))
                .map(home -> home.pos());

        Optional<BlockPos> chest = bed.flatMap(at -> placeBesideBedInStructure(world, at, colonyId));

        if (chest.isEmpty()) {
            chest = migrateToFinishedHouse(world, villager, colonyId);
        }

        if (chest.isEmpty()) {
            WorkerHousingNeeds.mark(colonyId, villager.getUuid());
            if (WARNED.add(villager.getUuid())) {
                VillageColonyMod.LOGGER.warn(
                        "{} {} has no safe home and chest — housing is now a colony priority",
                        profession, villager.getUuid().toString().substring(0, 8));
            }

            return Optional.empty();
        }

        WorkerStorage storage = WorkerStorage.of(
                villager.getUuid(), MinecraftTypeAdapter.toColonyPos(chest.get()));
        storages.register(storage);
        housingResolved(colonyId, villager.getUuid());

        VillageColonyMod.LOGGER.info(
                "{} {} got a chest of its own at {}, {}",
                profession, villager.getUuid().toString().substring(0, 8),
                chest.get().toShortString(), "beside its bed inside its structure");

        return Optional.of(storage);
    }

    private static Optional<BlockPos> placeBesideBedInStructure(
            ServerWorld world, BlockPos bed, UUID colonyId) {
        Optional<BlockBox> vanillaPiece = VanillaBedChests.originalVillagePiece(world, bed);
        if (vanillaPiece.isPresent()) {
            return ChestPlacer.placeBesideBedInStructure(world, bed, vanillaPiece.get()).chest();
        }

        return VillageColonyMod.BUILDINGS.ofColony(colonyId).stream()
                .filter(building -> building.finished()
                        && building.contains(MinecraftTypeAdapter.toColonyPos(bed)))
                .findFirst()
                .flatMap(building -> ChestPlacer.placeBesideBedInStructure(
                        world, bed, new BlockBox(
                                building.min().x(), building.min().y(), building.min().z(),
                                building.max().x(), building.max().y(), building.max().z())).chest());
    }

    private static Optional<BlockPos> migrateToFinishedHouse(
            ServerWorld world, VillagerEntity villager, UUID colonyId) {
        return VillageColonyMod.BUILDINGS.ofColony(colonyId).stream()
                .filter(building -> building.finished())
                .flatMap(building -> BlockPos.stream(
                                building.min().x(), building.min().y(), building.min().z(),
                                building.max().x(), building.max().y(), building.max().z())
                        .map(BlockPos::toImmutable)
                        .filter(foot -> isFreeBed(world, foot))
                        .map(foot -> new BedInStructure(foot, new BlockBox(
                                building.min().x(), building.min().y(), building.min().z(),
                                building.max().x(), building.max().y(), building.max().z()))))
                .map(candidate -> migrateBeside(world, villager, candidate))
                .flatMap(Optional::stream)
                .findFirst();
    }

    private static boolean isFreeBed(ServerWorld world, BlockPos foot) {
        BlockState state = world.getBlockState(foot);
        if (!(state.getBlock() instanceof BedBlock)
                || state.get(Properties.BED_PART) != BedPart.FOOT) {
            return false;
        }

        BlockPos head = foot.offset(state.get(Properties.HORIZONTAL_FACING));
        return world.getPointOfInterestStorage().getFreeTickets(head) > 0;
    }

    private static Optional<BlockPos> migrateBeside(
            ServerWorld world, VillagerEntity villager, BedInStructure candidate) {
        Optional<BlockPos> chest = ChestPlacer.placeBesideBedInStructure(
                world, candidate.foot(), candidate.structure()).chest();
        chest.ifPresent(ignored -> VillageFoundation.giveHome(world, villager, candidate.foot()));
        return chest;
    }

    private static void housingResolved(UUID colonyId, UUID workerId) {
        WorkerHousingNeeds.resolve(colonyId, workerId);
        WARNED.remove(workerId);
    }

    public static void clearAll() {
        WARNED.clear();
    }

    /** Esquece somente o controle derivado de aviso deste trabalhador. */
    public static void forget(UUID workerId) {
        WARNED.remove(workerId);
    }

    private record BedInStructure(BlockPos foot, BlockBox structure) {
    }

    /** O baú registrado ainda é um baú? Quebrado pelo jogador, deixa de valer. */
    public static boolean stillStands(ServerWorld world, ColonyPos chest) {
        BlockPos at = MinecraftTypeAdapter.toBlockPos(chest);

        return !world.getChunkManager().isChunkLoaded(at.getX() >> 4, at.getZ() >> 4)
                || world.getBlockState(at).isOf(Blocks.CHEST);
    }
}
