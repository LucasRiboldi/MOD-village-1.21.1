package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.Block;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;

import java.util.Optional;
import java.util.UUID;

/** Coloca e registra a casa grande exclusiva da fundação da colônia. */
public final class BigHouseFoundation {

    private static final int SEARCH_RADIUS = 32;

    private BigHouseFoundation() {
    }

    public record Result(boolean placed, Optional<Building> building) {
        public boolean changed() {
            return placed;
        }
    }

    /** Garante uma única BigHouseMOD por colônia. */
    public static Result ensure(ServerWorld world, Colony colony) {
        Optional<Building> existing = find(colony.id());

        if (existing.isPresent()) {
            return new Result(false, existing);
        }

        Optional<StructureTemplate> template = world.getStructureTemplateManager()
                .getTemplate(MinecraftTypeAdapter.toIdentifier(
                        StructureBlueprintReader.BIG_HOUSE_MOD));

        if (template.isEmpty()) {
            VillageColonyMod.LOGGER.warn(
                    "Colony {} could not find BigHouseMOD", colony.id());
            return new Result(false, Optional.empty());
        }

        Optional<BlockPos> origin = findSafeOrigin(world, colony);

        if (origin.isEmpty()) {
            VillageColonyMod.LOGGER.warn(
                    "Colony {} has no safe lot for BigHouseMOD", colony.id());
            return new Result(false, Optional.empty());
        }

        BlockPos placedAt = origin.get();
        StructurePlacementData placement = new StructurePlacementData()
                .setPosition(placedAt)
                .setIgnoreEntities(true)
                .setInitializeMobs(false);

        if (!template.get().place(
                world,
                placedAt,
                placedAt,
                placement,
                Random.create(),
                Block.NOTIFY_ALL)) {
            VillageColonyMod.LOGGER.warn(
                    "Colony {} could not place BigHouseMOD at {}",
                    colony.id(), placedAt.toShortString());
            return new Result(false, Optional.empty());
        }

        Vec3i size = template.get().getSize();
        Building building = new Building(
                UUID.randomUUID(),
                colony.id(),
                StructureBlueprintReader.BIG_HOUSE_MOD,
                MinecraftTypeAdapter.toColonyPos(placedAt),
                new ColonyPos(
                        placedAt.getX() + size.getX() - 1,
                        placedAt.getY() + size.getY() - 1,
                        placedAt.getZ() + size.getZ() - 1),
                true);
        VillageColonyMod.BUILDINGS.register(building);

        VillageColonyMod.LOGGER.info(
                "Placed BigHouseMOD for colony {} at {}",
                colony.id(), placedAt.toShortString());

        return new Result(true, Optional.of(building));
    }

    /** A casa persistida desta colônia, se já foi criada. */
    public static Optional<Building> find(UUID colonyId) {
        return VillageColonyMod.BUILDINGS.ofColony(colonyId).stream()
                .filter(building -> building.blueprint().equals(
                        StructureBlueprintReader.BIG_HOUSE_MOD))
                .findFirst();
    }

    /** Se a posição está dentro de uma BigHouseMOD registrada. */
    public static boolean containsHouseBlock(BlockPos position) {
        ColonyPos at = MinecraftTypeAdapter.toColonyPos(position);

        return VillageColonyMod.BUILDINGS.all().stream()
                .filter(building -> building.blueprint().equals(
                        StructureBlueprintReader.BIG_HOUSE_MOD))
                .anyMatch(building -> building.contains(at));
    }

    /**
     * Se um baú da BigHouseMOD está sendo procurado por alguém de fora dela.
     *
     * <p>Os baús da casa são infraestrutura dos seis moradores da fundação,
     * não estoque público da vila. Um aldeão cuja cama esteja na mesma casa
     * continua podendo reivindicar o baú; os demais devem procurar o próprio
     * cômodo ou um baú público separado.
     */
    public static boolean belongsToDifferentHouse(
            BlockPos bed, BlockPos chest) {
        ColonyPos bedAt = MinecraftTypeAdapter.toColonyPos(bed);
        ColonyPos chestAt = MinecraftTypeAdapter.toColonyPos(chest);

        Optional<Building> chestHouse = VillageColonyMod.BUILDINGS.all().stream()
                .filter(building -> building.blueprint().equals(
                        StructureBlueprintReader.BIG_HOUSE_MOD))
                .filter(building -> building.contains(chestAt))
                .findFirst();

        if (chestHouse.isEmpty()) {
            return false;
        }

        return VillageColonyMod.BUILDINGS.all().stream()
                .filter(building -> building.blueprint().equals(
                        StructureBlueprintReader.BIG_HOUSE_MOD))
                .filter(building -> building.contains(bedAt))
                .noneMatch(building -> building.id().equals(chestHouse.get().id()));
    }

    /** Procura uma caixa plana que não tenha estrutura nem construção. */
    private static Optional<BlockPos> findSafeOrigin(
            ServerWorld world, Colony colony) {
        BlockPos center = MinecraftTypeAdapter.toBlockPos(colony.center());
        Vec3i size = world.getStructureTemplateManager()
                .getTemplate(MinecraftTypeAdapter.toIdentifier(
                        StructureBlueprintReader.BIG_HOUSE_MOD))
                .orElseThrow()
                .getSize();

        for (int radius = 0; radius <= SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }

                    int x = center.getX() + dx - size.getX() / 2;
                    int z = center.getZ() + dz - size.getZ() / 2;
                    OptionalIntY floor = flatFloor(world, x, z, size);

                    if (floor.present()
                            && safe(world, colony, x, floor.y(), z, size)) {
                        return Optional.of(new BlockPos(x, floor.y(), z));
                    }
                }
            }
        }

        return Optional.empty();
    }

    private static OptionalIntY flatFloor(
            ServerWorld world, int x, int z, Vec3i size) {
        int y = world.getTopPosition(
                net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                new BlockPos(x, 0, z)).getY();

        for (int dx = 0; dx < size.getX(); dx++) {
            for (int dz = 0; dz < size.getZ(); dz++) {
                BlockPos column = new BlockPos(x + dx, 0, z + dz);
                int columnY = world.getTopPosition(
                        net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                        column).getY();

                if (columnY != y) {
                    return OptionalIntY.empty();
                }
            }
        }

        return OptionalIntY.of(y);
    }

    private static boolean safe(
            ServerWorld world, Colony colony, int x, int y, int z, Vec3i size) {
        ColonyPos min = new ColonyPos(x, y, z);
        ColonyPos max = new ColonyPos(
                x + size.getX() - 1,
                y + size.getY() - 1,
                z + size.getZ() - 1);

        if (VillageColonyMod.BUILDINGS.anythingBuiltInside(min, max)) {
            return false;
        }

        if (BuildSiteScanner.overlapsVillageStructure(world, min, max)) {
            return false;
        }

        // MOTION_BLOCKING_NO_LEAVES devolve o primeiro espaco acima da
        // superficie. Sem conferir o bloco de apoio, esse espaco pode ser o
        // telhado de uma construcao existente.
        for (int dx = 0; dx < size.getX(); dx++) {
            for (int dz = 0; dz < size.getZ(); dz++) {
                if (!BuildSiteScanner.isBiomeGround(
                        world, new BlockPos(x + dx, y - 1, z + dz))) {
                    return false;
                }
            }
        }

        for (int dx = 0; dx < size.getX(); dx++) {
            for (int dy = 0; dy < size.getY(); dy++) {
                for (int dz = 0; dz < size.getZ(); dz++) {
                    BlockPos pos = new BlockPos(x + dx, y + dy, z + dz);
                    if (BlockProtection.isVillageOriginal(world, pos)) {
                        return false;
                    }

                    if (dy == 0) {
                        continue;
                    }

                    // O lote inteiro precisa estar vazio acima do piso. Aceitar
                    // blocos substituiveis aqui ainda poderia apagar camas,
                    // baus ou decoracao de uma construcao ja existente.
                    if (!world.getBlockState(pos).isAir()) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private record OptionalIntY(boolean present, int y) {
        static OptionalIntY empty() {
            return new OptionalIntY(false, 0);
        }

        static OptionalIntY of(int y) {
            return new OptionalIntY(true, y);
        }
    }
}
