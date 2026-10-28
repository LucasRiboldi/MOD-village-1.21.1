package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BenchPresence;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.LotGround;
import com.villagecolony.fabric.integration.RoadIndex;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * As passivas de manutenção — Regra 132 (autor, 2026-10-27), só com a bancada do ofício na vila:
 * <ul>
 *   <li><b>fundidor:</b> fornalhas, alto-fornos e defumadores a até {@link #FURNACE_REACH} da bancada,
 *       inclusive os do jogador, ganham um tique a mais: o dobro da velocidade;</li>
 *   <li><b>pedreiro:</b> um buraco de rua a até {@link #ROAD_REACH} por minuto é tapado com o chão das
 *       ruas vizinhas, sem gastar material;</li>
 *   <li><b>construtor:</b> o material da obra aberta guardado em outros baús da vila passa para o baú do
 *       construtor, até {@link #MOVE_PER_ITEM} de cada por minuto.</li>
 * </ul>
 */
public final class BenchUpkeep {

    static {
        ServerMemory.register(BenchUpkeep.class, BenchUpkeep::clearAll);
    }

    static final int FURNACE_REACH = 32;
    static final int ROAD_REACH = 32;
    static final int MOVE_PER_ITEM = 64;

    /** Colunas de rua olhadas por passagem atrás de buraco. */
    static final int ROAD_SAMPLES = 48;

    private static final Map<UUID, List<BlockPos>> FURNACES = new HashMap<>();

    private BenchUpkeep() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerWorld world = server.getOverworld();
            long now = world.getTime();

            for (Colony colony : List.copyOf(VillageColonyMod.COLONIES.all())) {
                if (!colony.isActive() || !colony.isAttended(now)) {
                    continue;
                }

                if (now % 200 == 0) {
                    FURNACES.put(colony.id(), furnacesNear(world, colony));
                }

                speedUp(world, FURNACES.getOrDefault(colony.id(), List.of()));

                if (now % 1200 == 600) {
                    fillARoadHole(world, colony);
                    bringBuildMaterial(world, colony);
                }
            }
        });
    }

    /** As fornalhas a até {@link #FURNACE_REACH} das bancadas do fundidor. */
    static List<BlockPos> furnacesNear(ServerWorld world, Colony colony) {
        List<BlockPos> found = new ArrayList<>();

        for (BlockPos bench : BenchPresence.of(world, colony, ProfessionType.SMELTER)) {
            ChunkPos from = new ChunkPos(bench.add(-FURNACE_REACH, 0, -FURNACE_REACH));
            ChunkPos to = new ChunkPos(bench.add(FURNACE_REACH, 0, FURNACE_REACH));

            for (int cx = from.x; cx <= to.x; cx++) {
                for (int cz = from.z; cz <= to.z; cz++) {
                    if (!world.isChunkLoaded(cx, cz)) {
                        continue;
                    }

                    for (BlockEntity entity : world.getChunk(cx, cz).getBlockEntities().values()) {
                        if (entity instanceof AbstractFurnaceBlockEntity
                                && entity.getPos().isWithinDistance(bench, FURNACE_REACH)
                                && !found.contains(entity.getPos())) {
                            found.add(entity.getPos().toImmutable());
                        }
                    }
                }
            }
        }

        return found;
    }

    /** Um tique a mais em cada fornalha: o dobro da velocidade. */
    static void speedUp(ServerWorld world, List<BlockPos> furnaces) {
        for (BlockPos at : furnaces) {
            if (world.isChunkLoaded(at) && world.getBlockEntity(at) instanceof AbstractFurnaceBlockEntity furnace) {
                AbstractFurnaceBlockEntity.tick(world, at, world.getBlockState(at), furnace);
            }
        }
    }

    /** Tapa um buraco de rua perto da bancada do pedreiro: a coluna mais baixa que as ruas em volta. */
    static Optional<BlockPos> fillARoadHole(ServerWorld world, Colony colony) {
        List<BlockPos> benches = BenchPresence.of(world, colony, ProfessionType.MASON);
        List<Long> roads = RoadIndex.roadsOf(colony.id()).map(ColonyRoads::columns).orElse(List.of());

        if (benches.isEmpty() || roads.isEmpty()) {
            return Optional.empty();
        }

        for (int sample = 0; sample < ROAD_SAMPLES; sample++) {
            long column = roads.get(world.getRandom().nextInt(roads.size()));
            int x = (int) (column >> 32);
            int z = (int) column;
            BlockPos top = topOf(world, x, z);

            if (top == null || benches.stream().noneMatch(bench -> bench.isWithinDistance(top, ROAD_REACH))) {
                continue;
            }

            Optional<BlockPos> fixed = fillIfHole(world, roads, top);

            if (fixed.isPresent()) {
                VillageColonyMod.LOGGER.info("Colony {} — the mason bench filled a road hole at {} (Regra 132)",
                        colony.id().toString().substring(0, 8), fixed.get().toShortString());
                return fixed;
            }
        }

        return Optional.empty();
    }

    /** Se esta coluna de rua está abaixo de todas as ruas vizinhas (ao menos duas), sobe um bloco com o chão delas. */
    static Optional<BlockPos> fillIfHole(ServerWorld world, List<Long> roads, BlockPos top) {
        BlockState ground = null;
        int neighbours = 0;

        for (int[] step : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            int nx = top.getX() + step[0];
            int nz = top.getZ() + step[1];

            if (!roads.contains(ColonyRoads.column(nx, nz))) {
                continue;
            }

            BlockPos side = topOf(world, nx, nz);

            if (side == null || side.getY() <= top.getY()) {
                return Optional.empty();
            }

            neighbours++;
            ground = world.getBlockState(side);
        }

        BlockPos hole = top.up();

        if (neighbours < 2 || ground == null || !LotGround.isNothing(world.getBlockState(hole))
                || !world.getBlockState(hole).getFluidState().isEmpty()) {
            return Optional.empty();
        }

        world.setBlockState(hole, ground);

        return Optional.of(hole);
    }

    private static BlockPos topOf(ServerWorld world, int x, int z) {
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return null;
        }

        return world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z)).down();
    }

    /** O material da obra que está em outros baús vai para o baú do construtor. */
    static int bringBuildMaterial(ServerWorld world, Colony colony) {
        if (!BenchPresence.has(world, colony, ProfessionType.BUILDER)) {
            return 0;
        }

        var project = VillageColonyMod.CONSTRUCTIONS.openOf(colony.id());
        Optional<ColonyPos> builderChest = VillageColonyMod.WORKERS.ofColony(colony.id()).stream()
                .filter(worker -> worker.profession().filter(ProfessionType.BUILDER::equals).isPresent())
                .flatMap(worker -> VillageColonyMod.STORAGES.of(worker.villagerId()).stream())
                .map(WorkerStorage::chestPosition)
                .findFirst();

        if (project.isEmpty() || builderChest.isEmpty()) {
            return 0;
        }

        List<ColonyPos> others = new ArrayList<>(ColonyChests.nearestFirst(world, colony.id(), builderChest.get()));
        others.remove(builderChest.get());
        int moved = 0;

        for (Map.Entry<ResourceId, Integer> need : project.get().remainingMaterials().entrySet()) {
            Optional<Item> item = MinecraftTypeAdapter.toBlock(need.getKey()).map(net.minecraft.block.Block::asItem)
                    .filter(found -> found != net.minecraft.item.Items.AIR);

            if (item.isEmpty() || AppearingPieces.needsNoStock(need.getKey())) {
                continue;
            }

            int missing = need.getValue() - ColonyChests.countIn(world, List.of(builderChest.get()), item.get());
            int wanted = Math.min(MOVE_PER_ITEM, missing);

            if (wanted <= 0) {
                continue;
            }

            int taken = ColonyChests.withdraw(world, others, item.get(), wanted);
            int left = ChestDepositor.deposit(world, builderChest.get(), item.get(), taken);

            if (left > 0) {
                ChestDepositor.deposit(world, others.getFirst(), item.get(), left);
            }

            moved += taken - left;
        }

        return moved;
    }

    static void clearAll() {
        FURNACES.clear();
    }
}
