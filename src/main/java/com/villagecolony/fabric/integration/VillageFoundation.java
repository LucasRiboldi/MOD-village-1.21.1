package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.core.worker.service.ProfessionAssigner;
import com.villagecolony.core.worker.service.WorkerService;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestTypes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Garante a fundação física de uma colônia recém-detectada.
 *
 * <p>A profissão de colônia é uma decisão do mod, mas não pode existir
 * sem um aldeão real para ocupá-la. Quando a BigHouseMOD é colocada, nasce
 * um adulto para cada cama dela. Sem a casa, a fundação fica pendente:
 * nunca cria camas ou moradores ao ar livre. Só escreve em ar/blocos
 * substituíveis; blocos do jogador continuam sendo a fonte da verdade.
 * Morte não é reposta (N1).
 */
public final class VillageFoundation {

    private VillageFoundation() {
    }

    /** Resultado observável da passagem de garantia. */
    public record Result(int spawnedVillagers, int bedsPlaced) {
        public boolean changed() {
            return spawnedVillagers > 0 || bedsPlaced > 0;
        }
    }

    /**
     * Povoa a fundação desta vila — uma vez, e não a cada ciclo.
     *
     * <p><b>N1, 2026-09-24, decisão do autor.</b> Até aqui esta passagem
     * rodava em toda detecção e repunha, do nada, o titular que morresse:
     * a vila não perdia ninguém. Agora ela só age em dois momentos:
     *
     * <ul>
     *   <li>quando a BigHouseMOD acaba de ser colocada
     *       ({@code houseJustPlaced}): nasce um aldeão adulto para
     *       <b>cada cama</b> da casa, com a cama como {@code HOME};</li>
 *   <li>sem BigHouseMOD, não escreve nada: a próxima tentativa espera até
 *       que exista uma estrutura fechada para receber os moradores.</li>
     * </ul>
     *
     * <p>Quem chama decide o momento; fora deles a chamada não deve
     * acontecer. Depois da fundação a vila cresce como no Vanilla, por
     * procriação — ver {@code VillageMeals}.
     */
    public static Result ensure(
            ServerWorld world,
            Colony colony,
            ColonyPos around,
            WorkerService workers,
            boolean houseJustPlaced) {

        BlockPos center = MinecraftTypeAdapter.toBlockPos(around);
        Box area = Box.of(center.toCenterPos(), 128.0, 128.0, 128.0);
        List<VillagerEntity> villagers = world.getEntitiesByClass(
                VillagerEntity.class, area, VillagerEntity::isAlive);

        Map<UUID, VillagerEntity> byId = new HashMap<>();
        for (VillagerEntity villager : villagers) {
            byId.put(villager.getUuid(), villager);
        }

        Set<UUID> colonyVillagerIds = new HashSet<>();
        Optional<Building> foundationHouse = BigHouseFoundation.find(colony.id());

        if (foundationHouse.isEmpty()) {
            return new Result(0, 0);
        }

        for (Worker worker : workers.ofColony(colony.id())) {
            colonyVillagerIds.add(worker.villagerId());
        }

        int toSpawn = houseJustPlaced ? bedsIn(world, foundationHouse.get()) : 0;

        int bedsPlaced = 0;
        int spawned = 0;

        for (int index = 0; index < toSpawn; index++) {
            Optional<BlockPos> foot = findAvailableHouseBed(
                    world, foundationHouse.get(), byId, colonyVillagerIds);

            if (foot.isEmpty()) {
                VillageColonyMod.LOGGER.warn(
                        "Colony {} has no safe place for foundation bed {}/{}",
                        colony.id(), index + 1, toSpawn);
                break;
            }

            VillagerEntity villager = EntityType.VILLAGER.create(world);

            if (villager == null) {
                VillageColonyMod.LOGGER.warn(
                        "Colony {} could not create a foundation villager",
                        colony.id());
                break;
            }

            villager.setBreedingAge(0);
            villager.refreshPositionAndAngles(foot.get().up(), 0.0F, 0.0F);
            if (!world.spawnEntity(villager)) {
                VillageColonyMod.LOGGER.warn(
                        "Colony {} could not spawn a foundation villager at {}",
                        colony.id(), foot.get().toShortString());
                continue;
            }

            giveHome(world, villager, foot.get());
            spawned++;
            byId.put(villager.getUuid(), villager);
            colonyVillagerIds.add(villager.getUuid());
        }

        // Depois dos nascimentos, e não antes: as camas da casa são dos
        // moradores novos. Um titular Vanilla só entra nela se sobrar cama.
        bedsPlaced += ensureExistingHomes(
                world, colony.id(), workers, byId, colonyVillagerIds,
                foundationHouse.get());

        if (spawned > 0 || bedsPlaced > 0) {
            VillageColonyMod.LOGGER.info(
                    "Foundation for colony {}: spawned {} adults and placed {} beds",
                    colony.id(), spawned, bedsPlaced);
        }

        return new Result(spawned, bedsPlaced);
    }

    /** Dá cama exclusiva aos trabalhadores fundamentais já existentes. */
    private static int ensureExistingHomes(
            ServerWorld world,
            UUID colonyId,
            WorkerService workers,
            Map<UUID, VillagerEntity> villagers,
            Set<UUID> colonyVillagerIds,
            Building foundationHouse) {

        Set<BlockPos> occupiedBeds = new HashSet<>();
        int placed = 0;

        for (Worker worker : workers.ofColony(colonyId)) {
            if (worker.profession()
                    .filter(ProfessionAssigner.FOUNDATION_ORDER::contains)
                    .isEmpty()) {
                continue;
            }

            VillagerEntity villager = villagers.get(worker.villagerId());

            if (villager == null || villager.isBaby()) {
                continue;
            }

            Optional<GlobalPos> home = villager.getBrain()
                    .getOptionalRegisteredMemory(MemoryModuleType.HOME)
                    .filter(value -> value.dimension().equals(world.getRegistryKey()))
                    .filter(value -> isBed(world, value.pos()));

            if (home.isPresent()
                    && foundationHouse.contains(
                            MinecraftTypeAdapter.toColonyPos(canonicalBed(world, home.get().pos())))
                    && occupiedBeds.add(canonicalBed(world, home.get().pos()))) {
                continue;
            }

            Optional<BlockPos> foot = findAvailableHouseBed(
                    world, foundationHouse, villagers, colonyVillagerIds);

            if (foot.isEmpty()) {
                continue;
            }

            giveHome(world, villager, foot.get());
            occupiedBeds.add(foot.get());
            placed++;
        }

        return placed;
    }

    /**
     * Dá a cama ao aldeão do jeito que o Vanilla dá: memória e bilhete.
     *
     * <p>Só a memória {@code HOME} não bastava. O bilhete do ponto de
     * interesse é o que o Vanilla consulta para saber se a cama está livre,
     * e sem ele a procriação via as camas da BigHouseMOD como vagas e
     * punha um bebê na cama de um morador vivo. Com o bilhete, a cama só
     * volta a ficar livre quando o morador morre — e aí a vila pode crescer
     * de novo (N1).
     */
    static void giveHome(ServerWorld world, VillagerEntity villager, BlockPos foot) {
        PointOfInterestStorage pois = world.getPointOfInterestStorage();

        // O ponto de interesse da cama é a <b>cabeça</b>, e não o pé: é lá
        // que o Vanilla registra a cama, é lá que ele guarda a memória
        // HOME e é por lá que ele devolve o bilhete quando o aldeão morre.
        // Com o pé na memória, a morte não liberava cama nenhuma.
        BlockPos head = headOf(world, foot);

        villager.getBrain().getOptionalRegisteredMemory(MemoryModuleType.HOME)
                .filter(home -> home.dimension().equals(world.getRegistryKey()))
                .map(GlobalPos::pos)
                .filter(old -> !old.equals(head))
                .filter(old -> pois.getType(old).isPresent())
                .ifPresent(pois::releaseTicket);

        pois.getPosition(
                type -> type.matchesKey(PointOfInterestTypes.HOME),
                (type, pos) -> pos.equals(head),
                head,
                1);

        villager.getBrain().remember(
                MemoryModuleType.HOME,
                GlobalPos.create(world.getRegistryKey(), head));
    }

    /** A metade da cabeça de uma cama, a partir do pé. */
    private static BlockPos headOf(ServerWorld world, BlockPos foot) {
        BlockState state = world.getBlockState(foot);

        if (state.getBlock() instanceof BedBlock
                && state.get(Properties.BED_PART) == BedPart.FOOT) {
            return foot.offset(state.get(Properties.HORIZONTAL_FACING));
        }

        return foot.toImmutable();
    }

    /** Quantas camas inteiras a casa tem — uma por pé de cama. */
    private static int bedsIn(ServerWorld world, Building house) {
        int beds = 0;

        for (int x = house.min().x(); x <= house.max().x(); x++) {
            for (int y = house.min().y(); y <= house.max().y(); y++) {
                for (int z = house.min().z(); z <= house.max().z(); z++) {
                    BlockState state = world.getBlockState(new BlockPos(x, y, z));

                    if (state.getBlock() instanceof BedBlock
                            && state.get(Properties.BED_PART) == BedPart.FOOT) {
                        beds++;
                    }
                }
            }
        }

        return beds;
    }

    /** Encontra uma das sete camas que já vieram dentro da BigHouseMOD. */
    private static Optional<BlockPos> findAvailableHouseBed(
            ServerWorld world,
            Building house,
            Map<UUID, VillagerEntity> villagers,
            Set<UUID> colonyVillagerIds) {
        Set<BlockPos> occupied = new HashSet<>();

        for (UUID villagerId : colonyVillagerIds) {
            VillagerEntity villager = villagers.get(villagerId);

            if (villager == null) {
                continue;
            }

            villager.getBrain().getOptionalRegisteredMemory(MemoryModuleType.HOME)
                    .filter(home -> home.dimension().equals(world.getRegistryKey()))
                    .map(home -> canonicalBed(world, home.pos()))
                    .ifPresent(occupied::add);
        }

        for (int x = house.min().x(); x <= house.max().x(); x++) {
            for (int y = house.min().y(); y <= house.max().y(); y++) {
                for (int z = house.min().z(); z <= house.max().z(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (world.getBlockState(pos).getBlock() instanceof BedBlock
                            && world.getBlockState(pos).get(Properties.BED_PART)
                            == BedPart.FOOT) {
                        if (!occupied.contains(pos)) {
                            return Optional.of(pos);
                        }
                    }
                }
            }
        }

        return Optional.empty();
    }

    private static boolean isBed(ServerWorld world, BlockPos pos) {
        return world.getBlockState(pos).getBlock() instanceof BedBlock;
    }

    private static BlockPos canonicalBed(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);

        if (state.getBlock() instanceof BedBlock
                && state.get(Properties.BED_PART) == BedPart.HEAD) {
            return pos.offset(state.get(Properties.HORIZONTAL_FACING).getOpposite());
        }

        return pos.toImmutable();
    }

}
