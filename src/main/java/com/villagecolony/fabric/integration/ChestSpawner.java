package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.storage.service.StorageRegistry;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;

import java.util.Optional;

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
 * <p><b>Onde o baú nasce.</b> Ao lado da cama dele, pela regra (b) do autor —
 * encostado numa parede, nunca diante da porta. Sem cama, ou sem lugar ao lado
 * dela, perto do centro da vila, no chão livre mais próximo.
 */
public final class ChestSpawner {

    /** Até onde, a partir do centro, se procura chão para o baú de quem não tem cama. */
    static final int CENTRE_RADIUS = 12;

    private ChestSpawner() {
    }

    /**
     * Garante um baú para este trabalhador: põe no mundo e registra.
     *
     * @return o baú, ou vazio quando nem ao lado da cama nem perto do centro
     *     havia lugar — raro, e dito no log
     */
    public static Optional<WorkerStorage> ensureChest(
            ServerWorld world, VillagerEntity villager, StorageRegistry storages,
            BlockPos villageCentre, String profession) {

        if (storages.hasStorage(villager.getUuid())) {
            return storages.of(villager.getUuid());
        }

        Optional<BlockPos> bed = villager.getBrain()
                .getOptionalRegisteredMemory(MemoryModuleType.HOME)
                .filter(home -> home.dimension().equals(world.getRegistryKey()))
                .map(home -> home.pos());

        String where = "beside its bed";
        Optional<BlockPos> chest = bed.flatMap(at -> ChestPlacer.placeBesideBed(world, at).chest());

        if (chest.isEmpty()) {
            where = "near the village centre";
            chest = placeNear(world, villageCentre);
        }

        if (chest.isEmpty()) {
            VillageColonyMod.LOGGER.warn(
                    "{} {} has no chest and none could be placed — it gets no tasks until one exists",
                    profession, villager.getUuid().toString().substring(0, 8));

            return Optional.empty();
        }

        WorkerStorage storage = WorkerStorage.of(
                villager.getUuid(), MinecraftTypeAdapter.toColonyPos(chest.get()));
        storages.register(storage);

        VillageColonyMod.LOGGER.info(
                "{} {} got a chest of its own at {}, {}",
                profession, villager.getUuid().toString().substring(0, 8),
                chest.get().toShortString(), where);

        return Optional.of(storage);
    }

    /**
     * Um baú no chão livre mais perto do centro — o recurso de quem não tem
     * cama. Chão firme, espaço em cima para abrir, fora da rua de terra batida e
     * sem baú encostado (dois virariam baú duplo).
     */
    public static Optional<BlockPos> placeNear(ServerWorld world, BlockPos centre) {
        for (int ring = 2; ring <= CENTRE_RADIUS; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue;
                    }

                    int x = centre.getX() + dx;
                    int z = centre.getZ() + dz;

                    if (!world.getChunkManager().isChunkLoaded(x >> 4, z >> 4)) {
                        continue;
                    }

                    BlockPos spot = world.getTopPosition(
                            Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));

                    if (Math.abs(spot.getY() - centre.getY()) > 4 || !isGoodSpot(world, spot)) {
                        continue;
                    }

                    world.setBlockState(spot, Blocks.CHEST.getDefaultState()
                            .with(Properties.HORIZONTAL_FACING, Direction.NORTH));

                    return Optional.of(spot);
                }
            }
        }

        return Optional.empty();
    }

    private static boolean isGoodSpot(ServerWorld world, BlockPos spot) {
        BlockState floor = world.getBlockState(spot.down());

        if (!world.getBlockState(spot).isReplaceable()
                || !floor.isSolidBlock(world, spot.down())
                || floor.isOf(Blocks.DIRT_PATH)
                || !world.getBlockState(spot.up()).isAir()
                || BlockProtection.isColonyBuilt(spot.down())) {
            return false;
        }

        for (Direction side : Direction.Type.HORIZONTAL) {
            if (world.getBlockState(spot.offset(side)).isOf(Blocks.CHEST)) {
                return false;
            }
        }

        return true;
    }

    /** O baú registrado ainda é um baú? Quebrado pelo jogador, deixa de valer. */
    public static boolean stillStands(ServerWorld world, ColonyPos chest) {
        BlockPos at = MinecraftTypeAdapter.toBlockPos(chest);

        return !world.getChunkManager().isChunkLoaded(at.getX() >> 4, at.getZ() >> 4)
                || world.getBlockState(at).isOf(Blocks.CHEST);
    }
}
