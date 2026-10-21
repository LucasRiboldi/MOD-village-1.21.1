package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.world.GeneratedVillages;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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

    /** A busca mínima em volta do centro, e a de vila sem vila gerada. */
    private static final int SEARCH_RADIUS = 32;

    /** Além da borda da vila gerada, quanto ainda se procura lote. */
    private static final int BEYOND_VILLAGE = 16;

    /** O teto da busca: o custo é uma leitura de altura por coluna da área, uma vez por tentativa. */
    private static final int MAX_REACH = 96;

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

        // A project restored from the save has not become a ConstructionProject
        // yet. It still owns its old box, so never place a second foundation
        // while that pending project is waiting for the first server cycle.
        if (VillageColonyMod.CONSTRUCTIONS.pendingOf(colony.id())
                .map(project -> project.blueprint().equals(StructureBlueprintReader.BIG_HOUSE_MOD))
                .orElse(false)) {
            return new Result(false, Optional.empty());
        }

        Optional<StructureTemplate> template = world.getStructureTemplateManager()
                .getTemplate(MinecraftTypeAdapter.toIdentifier(
                        StructureBlueprintReader.BIG_HOUSE_MOD));

        if (template.isEmpty()) {
            VillageColonyMod.LOGGER.warn(
                    "Colony {} could not find BigHouseMOD", colony.id());
            return new Result(false, Optional.empty());
        }

        Optional<Site> site = findSafeOrigin(world, colony, template.get().getSize());

        if (site.isEmpty()) {
            VillageColonyMod.LOGGER.warn(
                    "Colony {} has no safe lot for BigHouseMOD", colony.id());
            return new Result(false, Optional.empty());
        }

        BlockPos placedAt = site.get().origin();

        if (site.get().clearing()) {
            VillageColonyMod.LOGGER.info("Colony {} cleared the BigHouseMOD lot at {} — {} blocks of trees and plants",
                    colony.id(), placedAt.toShortString(), clear(world, placedAt, template.get().getSize()));
        }

        if (site.get().levelled()) {
            VillageColonyMod.LOGGER.info("Colony {} levelled the BigHouseMOD lot at {} — {} blocks moved",
                    colony.id(), placedAt.toShortString(), level(world, placedAt, template.get().getSize()));
        }
        StructurePlacementData placement = new StructurePlacementData()
                .setPosition(placedAt)
                .setIgnoreEntities(true)
                .setInitializeMobs(false);

        if (!template.get().place(
                world,
                placedAt,
                placedAt,
                placement,
                // Semente do mundo e do lugar: a mesma fundação sai igual no mesmo mundo (AUD-22).
                Random.create(world.getSeed() ^ placedAt.asLong()),
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
     * <p>Os baús da casa são infraestrutura dos sete moradores da fundação,
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

    /** Um lugar para a casa: o canto na altura do piso, se o chão precisa ser aplainado e se há mato a tirar. */
    private record Site(BlockPos origin, boolean levelled, boolean clearing) {
    }

    /**
     * Procura onde assentar a casa — decisão do autor, 2026-10-20.
     *
     * <p>Plano exato primeiro, numa área até a borda da vila gerada mais {@link #BEYOND_VILLAGE};
     * sem plano exato, o lote mais nivelado com desnível até {@link FoundationLotSearch#MAX_SPREAD},
     * que {@link #level} aplaina. A altura é a do chão, sem a vegetação de pé nele; lote com árvore
     * só depois do limpo, e {@link #clear} a tira (G1). Só lê coluna de chunk carregado: pedir
     * chunk descarregado o carregaria no tique.
     */
    private static Optional<Site> findSafeOrigin(ServerWorld world, Colony colony, Vec3i size) {
        BlockPos center = MinecraftTypeAdapter.toBlockPos(colony.center());
        int reach = reachOf(world, center);
        int width = 2 * reach + size.getX();
        int depth = 2 * reach + size.getZ();
        int minX = center.getX() - reach - size.getX() / 2;
        int minZ = center.getZ() - reach - size.getZ() / 2;
        int[] heights = new int[width * depth];
        boolean[] growth = new boolean[width * depth];

        for (int dz = 0; dz < depth; dz++) {
            for (int dx = 0; dx < width; dx++) {
                int x = minX + dx;
                int z = minZ + dz;

                boolean loaded = world.isChunkLoaded(x >> 4, z >> 4);

                heights[dz * width + dx] = loaded ? LotGrowth.groundTop(world, x, z) : FoundationLotSearch.UNKNOWN;
                growth[dz * width + dx] = loaded && LotGrowth.hasGrowth(world, x, z);
            }
        }

        for (FoundationLotSearch.Lot lot
                : FoundationLotSearch.candidates(heights, growth, width, depth, size.getX(), size.getZ())) {
            int x = minX + lot.offsetX();
            int z = minZ + lot.offsetZ();
            boolean levelled = lot.spread() > 0;

            if (levelled ? safeAfterLevelling(world, colony, x, lot.floor(), z, size)
                    : safe(world, colony, x, lot.floor(), z, size)) {
                return Optional.of(new Site(new BlockPos(x, lot.floor(), z), levelled, lot.clearing()));
            }
        }

        return Optional.empty();
    }

    /** Até onde procurar: a borda da vila gerada mais a folga, entre os 32 de antes e {@link #MAX_REACH}. */
    private static int reachOf(ServerWorld world, BlockPos center) {
        return GeneratedVillages.boundsAt(world, center.getX(), center.getZ())
                .map(box -> Math.max(
                        Math.max(Math.abs(box.minX() - center.getX()), Math.abs(box.maxX() - center.getX())),
                        Math.max(Math.abs(box.minZ() - center.getZ()), Math.abs(box.maxZ() - center.getZ())))
                        + BEYOND_VILLAGE)
                .map(reach -> Math.min(MAX_REACH, Math.max(SEARCH_RADIUS, reach)))
                // Vila sem estrutura gerada (do jogador, ou posta por comando): sem a caixa das
                // peças, a busca vai à folga além do raio de antes. Nos 32, o lote da cerejeira
                // ficava no anel 31 e a colônia oscilava entre nascer e não nascer.
                .orElse(SEARCH_RADIUS + BEYOND_VILLAGE);
    }

    /**
     * Se o lote de chão irregular pode ser aplainado e receber a casa.
     *
     * <p>A mesma régua de {@link #safe}, para o chão depois de aplainado: o que se corta é só
     * chão do bioma (nunca peça de vila ou obra), o que se aterra é ar ou vegetação, e acima do
     * chão de hoje e do piso só há ar ou vegetação natural ({@link LotGrowth}).
     */
    static boolean safeAfterLevelling(
            ServerWorld world, Colony colony, int x, int y, int z, Vec3i size) {
        int clearThrough = Math.max(size.getY() - 1, BuildSiteScanner.VERTICAL_CLEARANCE);
        ColonyPos min = new ColonyPos(x, y - FoundationLotSearch.MAX_SPREAD, z);
        ColonyPos max = new ColonyPos(x + size.getX() - 1, y + clearThrough, z + size.getZ() - 1);

        if (VillageColonyMod.BUILDINGS.anythingBuiltInside(min, max)
                || LotClearance.overlapsVillageStructure(world, min, max)
                || LotClearance.overlapsConstructionSite(world, colony.id(), min, max, null)) {
            return false;
        }

        for (int dx = 0; dx < size.getX(); dx++) {
            for (int dz = 0; dz < size.getZ(); dz++) {
                if (!columnLevels(world, x + dx, y, z + dz, clearThrough)) {
                    return false;
                }
            }
        }

        return true;
    }

    /** Uma coluna do lote: apoio no chão do bioma, corte só de chão, aterro só sobre ar ou vegetação. */
    static boolean columnLevels(ServerWorld world, int x, int y, int z, int clearThrough) {
        int top = LotGrowth.groundTop(world, x, z);

        if (!LotGround.isBiomeGround(world, new BlockPos(x, Math.min(top, y) - 1, z))) {
            return false;
        }

        for (int at = Math.min(top, y); at <= y + clearThrough; at++) {
            BlockPos pos = new BlockPos(x, at, z);
            BlockState state = world.getBlockState(pos);

            if (BlockProtection.isVillageOriginal(world, pos)) {
                return false;
            }

            boolean ok;

            if (at < top) {
                ok = LotGround.isBiomeGround(world, pos); // corta
            } else {
                ok = LotGrowth.isClearable(world, pos, state); // ar, planta, árvore natural
            }

            if (!ok) {
                return false;
            }
        }

        return true;
    }

    /** Corta o chão acima do piso e aterra o de baixo com o chão do bioma; devolve os blocos mexidos. */
    static int level(ServerWorld world, BlockPos origin, Vec3i size) {
        int moved = 0;

        for (int dx = 0; dx < size.getX(); dx++) {
            for (int dz = 0; dz < size.getZ(); dz++) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                int top = LotGrowth.groundTop(world, x, z);

                for (int at = origin.getY(); at < top; at++) {
                    world.setBlockState(new BlockPos(x, at, z), Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                    moved++;
                }

                for (int at = top; at < origin.getY(); at++) {
                    BlockPos pos = new BlockPos(x, at, z);
                    world.setBlockState(pos, VillageBiomes.foundationGroundAt(world, pos).getDefaultState(),
                            Block.NOTIFY_ALL);
                    moved++;
                }
            }
        }

        return moved;
    }

    /** Tira do lote a vegetação natural, do chão ao topo da folga e da árvore; devolve os blocos tirados. */
    static int clear(ServerWorld world, BlockPos origin, Vec3i size) {
        int clearThrough = Math.max(size.getY() - 1, BuildSiteScanner.VERTICAL_CLEARANCE);
        int cleared = 0;

        for (int dx = 0; dx < size.getX(); dx++) {
            for (int dz = 0; dz < size.getZ(); dz++) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                int from = Math.min(LotGrowth.groundTop(world, x, z), origin.getY());

                cleared += LotGrowth.clearColumn(world, x, from, origin.getY() + clearThrough, z);
            }
        }

        return cleared;
    }

    static boolean safe(
            ServerWorld world, Colony colony, int x, int y, int z, Vec3i size) {
        ColonyPos min = new ColonyPos(x, y, z);
        int clearThrough = Math.max(size.getY() - 1, BuildSiteScanner.VERTICAL_CLEARANCE);
        ColonyPos max = new ColonyPos(
                x + size.getX() - 1,
                y + clearThrough,
                z + size.getZ() - 1);

        if (VillageColonyMod.BUILDINGS.anythingBuiltInside(min, max)) {
            return false;
        }

        if (LotClearance.overlapsVillageStructure(world, min, max)) {
            return false;
        }

        if (LotClearance.overlapsConstructionSite(world, colony.id(), min, max, null)) {
            return false;
        }

        // MOTION_BLOCKING_NO_LEAVES devolve o primeiro espaco acima da
        // superficie. Sem conferir o bloco de apoio, esse espaco pode ser o
        // telhado de uma construcao existente.
        for (int dx = 0; dx < size.getX(); dx++) {
            for (int dz = 0; dz < size.getZ(); dz++) {
                if (!LotGround.isBiomeGround(
                        world, new BlockPos(x + dx, y - 1, z + dz))) {
                    return false;
                }
            }
        }

        for (int dx = 0; dx < size.getX(); dx++) {
            for (int dy = 0; dy <= clearThrough; dy++) {
                for (int dz = 0; dz < size.getZ(); dz++) {
                    BlockPos pos = new BlockPos(x + dx, y + dy, z + dz);
                    if (BlockProtection.isVillageOriginal(world, pos)) {
                        return false;
                    }

                    if (dy == 0) {
                        continue;
                    }

                    // Acima do piso, só ar ou vegetação natural (G1): cama, baú,
                    // tábua e tronco descascado ou de cabana continuam recusando.
                    if (!LotGrowth.isClearable(world, pos, world.getBlockState(pos))) {
                        return false;
                    }
                }
            }
        }

        return true;
    }
}
