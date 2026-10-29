package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.RoadIndex;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * O bosque do lenhador perto da vila — pedido do autor, 2026-10-08: <i>"plantar
 * três mudas perto da vila em espaços que não atrapalhariam"</i>, longe de onde a
 * vila vai construir.
 *
 * <p>No playtest de 08-10 nenhuma árvore ficava no miolo da vila; as 32 cortadas
 * estavam a 48–107 blocos do centro, e o lenhador passou 26% do tempo parado nas
 * caminhadas longas. O viveiro da borda ({@link LumberjackNursery}) planta num anel
 * fixo a partir do centro, que numa vila grande cai dentro dela.
 *
 * <p><b>Onde.</b> Fora da caixa da vila, de {@link #NEAR} a {@link #FAR} blocos da
 * borda, do lado do baú do lenhador primeiro. <b>O que não atrapalha:</b> a caixa
 * da vila (as casas e os lotes de dentro), a casa ou obra da colônia com
 * {@link #CLEARANCE} de folga, e a rua — o lote novo nasce ao lado da rua, então a
 * muda fica a {@link #ROAD_CLEARANCE} dela —, e as outras mudas do bosque a
 * {@link #SPACING}. Mantém {@link #SIZE} vivas: a cortada é reposta.
 *
 * <p><b>E cresce logo</b> (A8): a cada conferência, uma farinha de osso em cada muda
 * do bosque — do baú da colônia se houver, senão ela aparece, como todo ingrediente
 * de drop (decisão do autor de 2026-09-30, {@code DropIngredients}).
 */
public final class VillageGrove {

    static {
        ServerMemory.register(VillageGrove.class, VillageGrove::clearAll);
    }

    /** Quantas mudas o bosque tem — eram 3; o autor pediu mais (A8, 2026-10-08). */
    public static final int SIZE = 6;

    /** A distância mínima e máxima da borda da vila — o bosque perto da vila (O5, autor 2026-10-28; era até 10). */
    static final int NEAR = 4;
    static final int FAR = 6;

    /** Folga até casa ou obra da colônia. */
    static final int CLEARANCE = 4;

    /** Folga até a rua, onde nascem os lotes novos. */
    static final int ROAD_CLEARANCE = 6;

    /** Entre duas mudas do bosque. */
    static final int SPACING = 5;

    /**
     * No deserto o bosque é o dobro e se espalha por pontos sorteados das bordas — Regra 118 (jogo da
     * 0.3.93: três lenhadores 61% ociosos esperando as 6 mudas juntas perto do baú crescerem).
     */
    static final int DESERT_FACTOR = 2;

    /** De quanto em quanto tempo o bosque é conferido: 1 min. */
    static final long CHECK_EVERY = 1_200;

    private static final Map<UUID, List<BlockPos>> GROVES = new HashMap<>();

    /** Uma em cada tantas mudas do bosque é de outra espécie (autor, 2026-10-27). */
    static final int OTHER_SPECIES_EVERY = 10;

    /**
     * As espécies que uma muda só faz crescer. O carvalho escuro fica de fora: precisa de quatro mudas
     * juntas.
     */
    static final List<Block> OTHER_SPECIES = List.of(Blocks.OAK_SAPLING, Blocks.BIRCH_SAPLING,
            Blocks.SPRUCE_SAPLING, Blocks.JUNGLE_SAPLING, Blocks.ACACIA_SAPLING, Blocks.CHERRY_SAPLING,
            Blocks.MANGROVE_PROPAGULE);

    private static final Map<UUID, Integer> PLANTED = new HashMap<>();

    /**
     * A muda desta vez: a do bioma, e a cada {@link #OTHER_SPECIES_EVERY} uma de outra espécie, sorteada.
     * As toras dela também servem à colônia.
     */
    static Block speciesFor(ServerWorld world, UUID colonyId, Block nativeSapling) {
        int count = PLANTED.merge(colonyId, 1, Integer::sum);

        if (count % OTHER_SPECIES_EVERY != 0) {
            return nativeSapling;
        }

        List<Block> others = OTHER_SPECIES.stream().filter(other -> other != nativeSapling).toList();

        return others.get(world.getRandom().nextInt(others.size()));
    }

    private static final Map<UUID, Long> CHECKED = new HashMap<>();

    /** Mudas que vieram de árvores cortadas longe, a mais no bosque — regra do autor, 2026-10-11. */
    private static final Map<UUID, Integer> MOVED = new HashMap<>();

    private VillageGrove() {
    }

    /** Confere o bosque desta colônia, no máximo uma vez por {@link #CHECK_EVERY}. */
    static void tend(ServerWorld world, UUID colonyId) {
        long now = world.getTime();
        Long last = CHECKED.get(colonyId);

        if (last != null && now - last < CHECK_EVERY) {
            return;
        }

        CHECKED.put(colonyId, now);
        VillageColonyMod.COLONIES.find(colonyId).ifPresent(colony -> tendNow(world, colony));
    }

    /**
     * Repõe o bosque até {@link #SIZE}, sem o relógio — para o teste.
     *
     * @return quantas mudas foram plantadas agora
     */
    static int tendNow(ServerWorld world, Colony colony) {
        Optional<VillageBounds> box = colony.bounds();
        Optional<Block> sapling = TreeNursery.saplingFor(world, colony.center());

        if (box.isEmpty() || sapling.isEmpty()) {
            return 0;
        }

        List<BlockPos> grove = GROVES.computeIfAbsent(colony.id(), id -> new ArrayList<>());

        grove.removeIf(ground -> !alive(world, ground));

        if (grove.isEmpty()) {
            grove.addAll(found(world, box.get(), colony.center().y()));
        }

        int planted = 0;
        int size = sizeFor(colony.id()) * (isDesert(world, colony) ? DESERT_FACTOR : 1);

        while (grove.size() < size) {
            Optional<BlockPos> spot = spotFor(world, colony, box.get(), grove);

            if (spot.isEmpty()) {
                break;
            }

            Block chosen = speciesFor(world, colony.id(), sapling.get());

            if (!TreeNursery.plant(world, spot.get(), chosen)
                    && (chosen == sapling.get() || !TreeNursery.plant(world, spot.get(), sapling.get()))) {
                break;
            }

            grove.add(spot.get());
            planted++;
            com.villagecolony.fabric.integration.VillageTrees.rememberSapling(colony.id(), spot.get().up());

            VillageColonyMod.LOGGER.info(
                    "Colony {} — the lumberjack planted {} near the village at {} (grove {} of {})",
                    colony.id().toString().substring(0, 8),
                    TreeNursery.idOf(world.getBlockState(spot.get().up()).getBlock()),
                    spot.get().toShortString(), grove.size(), size);
        }

        fertilize(world, colony, grove);

        return planted;
    }

    /** Uma farinha de osso em cada muda do bosque. */
    private static void fertilize(ServerWorld world, Colony colony, List<BlockPos> grove) {
        java.util.List<ColonyPos> chests = null;

        for (BlockPos ground : grove) {
            BlockPos sapling = ground.up();
            net.minecraft.block.BlockState state = world.getBlockState(sapling);

            if (!(state.getBlock() instanceof net.minecraft.block.Fertilizable plant)
                    || !state.isIn(BlockTags.SAPLINGS)
                    || !plant.isFertilizable(world, sapling, state)) {
                continue;
            }

            if (chests == null) {
                chests = com.villagecolony.fabric.integration.ColonyChests.nearestFirst(
                        world, colony.id(), colony.center());
            }

            if (com.villagecolony.fabric.integration.ColonyChests.countIn(
                    world, chests, net.minecraft.item.Items.BONE_MEAL) > 0) {
                com.villagecolony.fabric.integration.ColonyChests.withdraw(
                        world, chests, net.minecraft.item.Items.BONE_MEAL, 1);
            }

            if (plant.canGrow(world, world.getRandom(), sapling, state)) {
                plant.grow(world, world.getRandom(), sapling, state);
            }

            world.syncWorldEvent(net.minecraft.world.WorldEvents.BONE_MEAL_USED, sapling, 15);
        }
    }

    /**
     * O tamanho do bosque: {@link #SIZE} mudas por lenhador da colônia — P7a,
     * bosque-fazenda (2026-10-11). Com dois lenhadores, 6 mudas não davam árvore
     * perto para os dois, e metade das caminhadas ia a mais de 48 blocos.
     */
    static int sizeFor(UUID colonyId) {
        long lumberjacks = VillageColonyMod.WORKERS.ofColony(colonyId).stream()
                .filter(worker -> worker.profession()
                        .filter(com.villagecolony.core.worker.model.ProfessionType.LUMBERJACK::equals).isPresent())
                .count();

        int base = SIZE * (int) Math.max(1, lumberjacks);

        return base + Math.min(base, MOVED.getOrDefault(colonyId, 0));
    }

    /**
     * A árvore cortada longe do bosque não é replantada lá: a muda dela vai para o
     * bosque perto do baú do lenhador, que cresce até o dobro ({@link #sizeFor}) —
     * regra do autor, 2026-10-11. No playtest da 0.3.26 o lenhador andava 48% do
     * expediente atrás de árvores a 80–100 blocos do centro.
     *
     * @return se a muda foi para o bosque (e não deve ser replantada na base)
     */
    static boolean takesTheSapling(ServerWorld world, UUID colonyId, BlockPos base) {
        Optional<VillageBounds> box = VillageColonyMod.COLONIES.find(colonyId).flatMap(Colony::bounds);

        if (box.isEmpty() || box.get().containsColumn(base.getX(), base.getZ(), FAR + SPACING)) {
            return false;
        }

        MOVED.merge(colonyId, 1, Integer::sum);
        VillageColonyMod.LOGGER.info("Colony {} — the sapling of the tree at {} goes to the grove near the village"
                + " (grove of {})", colonyId.toString().substring(0, 8), base.toShortString(), sizeFor(colonyId));

        return true;
    }

    /**
     * A árvore madura do bosque mais perto de {@code from}, se houver — P7a: o
     * lenhador corta o bosque perto antes da floresta longe, e a muda é reposta.
     */
    static Optional<BlockPos> matureTree(UUID colonyId, BlockPos from,
            java.util.function.Predicate<BlockPos> usable, ServerWorld world) {

        return GROVES.getOrDefault(colonyId, List.of()).stream()
                .map(BlockPos::up)
                .filter(log -> world.getBlockState(log).isIn(BlockTags.LOGS))
                .filter(usable)
                .min(java.util.Comparator.comparingDouble(log -> log.getSquaredDistance(from)));
    }

    static boolean isDesert(ServerWorld world, Colony colony) {
        return "desert".equals(HousePlans.paletteOf(world, colony.center()).style());
    }

    /** As mudas de agora, para o teste. */
    static List<BlockPos> of(UUID colonyId) {
        return List.copyOf(GROVES.getOrDefault(colonyId, List.of()));
    }

    /** O primeiro lugar que serve, do lado do baú do lenhador para os outros. */
    private static Optional<BlockPos> spotFor(ServerWorld world, Colony colony, VillageBounds box, List<BlockPos> grove) {
        double towards = towardsTheLumberjack(colony.id(), box);
        List<int[]> ring = new ArrayList<>();

        for (int away = NEAR; away <= FAR; away++) {
            for (int x = box.minX() - away; x <= box.maxX() + away; x += 2) {
                ring.add(new int[] {x, box.minZ() - away, away});
                ring.add(new int[] {x, box.maxZ() + away, away});
            }

            for (int z = box.minZ() - away; z <= box.maxZ() + away; z += 2) {
                ring.add(new int[] {box.minX() - away, z, away});
                ring.add(new int[] {box.maxX() + away, z, away});
            }
        }

        // Perto do baú do lenhador primeiro — um bosque junto, e não espalhado pela borda.
        Optional<ColonyPos> chest = lumberjackChest(colony.id());

        ring.sort(chest.isPresent()
                ? Comparator.<int[]>comparingLong(cell -> flat(cell, chest.get()))
                : Comparator.<int[]>comparingDouble(cell -> turn(towards,
                                Math.atan2(cell[1] - box.centerZ(), cell[0] - box.centerX())))
                        .thenComparingInt(cell -> cell[2]));

        // No deserto, bordas sorteadas: o bosque se espalha em volta da vila (Regra 118).
        if (isDesert(world, colony)) {
            java.util.Collections.shuffle(ring, new java.util.Random(world.getRandom().nextLong()));
        }

        for (int[] cell : ring) {
            Optional<BlockPos> ground = groundAt(world, cell[0], cell[1], colony.center().y());

            if (ground.isPresent() && fits(world, colony, ground.get(), grove)) {
                return ground;
            }
        }

        return Optional.empty();
    }

    /** Não atrapalha: lugar de viveiro, longe de casa, obra, rua e das outras mudas. */
    private static boolean fits(ServerWorld world, Colony colony, BlockPos ground, List<BlockPos> grove) {
        if (!TreeNursery.isSpotForANursery(world, ground)) {
            return false;
        }

        for (BlockPos other : grove) {
            if (other.getSquaredDistance(ground) < SPACING * SPACING) {
                return false;
            }
        }

        ColonyPos at = MinecraftTypeAdapter.toColonyPos(ground);

        for (Building building : VillageColonyMod.BUILDINGS.ofColony(colony.id())) {
            if (near(building.min(), building.max(), at, CLEARANCE)) {
                return false;
            }
        }

        if (VillageColonyMod.CONSTRUCTIONS.openOf(colony.id())
                .map(project -> Building.of(project))
                .filter(site -> near(site.min(), site.max(), at, CLEARANCE))
                .isPresent()) {
            return false;
        }

        OptionalInt road = RoadIndex.roadsOf(colony.id())
                .map(roads -> roads.blocksToTheNearestRoad(at))
                .orElseGet(OptionalInt::empty);

        return road.isPresent() ? road.getAsInt() >= ROAD_CLEARANCE : !pathNear(world, ground);
    }

    private static boolean near(ColonyPos min, ColonyPos max, ColonyPos at, int margin) {
        return at.x() >= min.x() - margin && at.x() <= max.x() + margin
                && at.z() >= min.z() - margin && at.z() <= max.z() + margin;
    }

    /** Sem índice de ruas: chão de caminho a menos de {@link #ROAD_CLEARANCE}. */
    private static boolean pathNear(ServerWorld world, BlockPos ground) {
        for (int dx = -ROAD_CLEARANCE + 1; dx < ROAD_CLEARANCE; dx++) {
            for (int dz = -ROAD_CLEARANCE + 1; dz < ROAD_CLEARANCE; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (com.villagecolony.fabric.integration.VillageRoad.isPathBlock(world.getBlockState(ground.add(dx, dy, dz)))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /** As mudas do bosque que já estão de pé no anel (depois de reiniciar o servidor). */
    private static List<BlockPos> found(ServerWorld world, VillageBounds box, int centreY) {
        List<BlockPos> found = new ArrayList<>();

        for (int x = box.minX() - FAR; x <= box.maxX() + FAR && found.size() < SIZE; x++) {
            for (int z = box.minZ() - FAR; z <= box.maxZ() + FAR && found.size() < SIZE; z++) {
                if (box.containsColumn(x, z, NEAR - 1)) {
                    continue;
                }

                groundAt(world, x, z, centreY)
                        .filter(ground -> alive(world, ground))
                        .ifPresent(found::add);
            }
        }

        return found;
    }

    /**
     * Muda de pé na terra de viveiro, ou árvore crescida no lugar dela — a árvore, ao
     * crescer, pode trocar a terra de viveiro por terra comum, e sem isto o bosque a
     * deixaria de contar e plantaria outra, crescendo sem teto.
     */
    private static boolean alive(ServerWorld world, BlockPos ground) {
        if (world.getChunkManager().getWorldChunk(ground.getX() >> 4, ground.getZ() >> 4) == null) {
            return false;
        }

        return world.getBlockState(ground.up()).isIn(BlockTags.LOGS)
                || (world.getBlockState(ground).isOf(TreeNursery.BED)
                        && world.getBlockState(ground.up()).isIn(BlockTags.SAPLINGS));
    }

    /** O chão nesta coluna, perto da altura da vila; chunk só se carregado. */
    private static Optional<BlockPos> groundAt(ServerWorld world, int x, int z, int centreY) {
        if (world.getChunkManager().getWorldChunk(x >> 4, z >> 4) == null) {
            return Optional.empty();
        }

        for (int y = centreY + 4; y >= centreY - 8; y--) {
            BlockPos at = new BlockPos(x, y, z);

            if (!world.getBlockState(at).isAir() && world.getBlockState(at.up()).isAir()) {
                return Optional.of(at);
            }
        }

        return Optional.empty();
    }

    /** O ângulo do baú do lenhador a partir do centro da vila; zero sem ele. */
    private static double towardsTheLumberjack(UUID colonyId, VillageBounds box) {
        return lumberjackChest(colonyId)
                .map(chest -> Math.atan2(chest.z() - box.centerZ(), chest.x() - box.centerX()))
                .orElse(0.0);
    }

    /** O baú do primeiro lenhador da colônia que tem um. */
    private static Optional<ColonyPos> lumberjackChest(UUID colonyId) {
        for (var worker : VillageColonyMod.WORKERS.ofColony(colonyId)) {
            if (worker.profession()
                    .filter(com.villagecolony.core.worker.model.ProfessionType.LUMBERJACK::equals).isEmpty()) {
                continue;
            }

            var chest = VillageColonyMod.STORAGES.of(worker.villagerId());

            if (chest.isPresent()) {
                return Optional.of(chest.get().chestPosition());
            }
        }

        return Optional.empty();
    }

    private static long flat(int[] cell, ColonyPos chest) {
        long dx = (long) cell[0] - chest.x();
        long dz = (long) cell[1] - chest.z();

        return dx * dx + dz * dz;
    }

    /** Quanto girar de um ângulo ao outro, de 0 a π. */
    private static double turn(double from, double to) {
        double difference = Math.abs(from - to) % (2 * Math.PI);

        return difference > Math.PI ? 2 * Math.PI - difference : difference;
    }

    public static void clearAll() {
        GROVES.clear();
        CHECKED.clear();
        MOVED.clear();
        PLANTED.clear();
    }
}
