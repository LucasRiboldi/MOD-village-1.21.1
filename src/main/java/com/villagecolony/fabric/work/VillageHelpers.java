package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkHours;
import com.villagecolony.fabric.integration.MineMouth;
import com.villagecolony.fabric.integration.RoadIndex;
import com.villagecolony.fabric.integration.SiteSurroundings;
import com.villagecolony.fabric.integration.StreetGaps;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LightType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Os ajudantes da vila — Regra 149 (autor, 2026-10-28; taiga simulada: pedreiro 64% e fundidor 80% parados).
 * Uma vez por minuto, o titular sem tarefa do ofício, no expediente:
 *
 * <ul>
 *   <li><b>pedreiro</b>: tapa até {@link #GAPS_PER_PASS} falhas da rua ({@link StreetGaps}), das mais perto
 *       dele; sem falha, nivela o chão em volta de uma casa pronta, uma por vez
 *       ({@link SiteSurroundings#level(ServerWorld, Building)});</li>
 *   <li><b>fundidor</b>: leva até {@link #HAUL} itens do baú da boca da mina para os baús públicos; e acende a
 *       rua — uma lanterna por minuto ao lado da coluna de rua escura (luz de bloco abaixo de
 *       {@link #DARK}), a {@link #LIGHT_SPACING} blocos de outra luz.</li>
 * </ul>
 */
public final class VillageHelpers {

    static final int GAPS_PER_PASS = 8;
    static final int HAUL = 64;
    static final int DARK = 8;
    static final int LIGHT_SPACING = 12;

    /** Quantas colunas de rua escura a busca de luz olha por passada: cada pergunta lê 845 blocos. */
    static final int LIGHT_TRIES = 64;

    /** Qual construção o pedreiro nivela na próxima vez, por colônia. */
    private static final Map<UUID, Integer> NEXT_BUILDING = new HashMap<>();

    static {
        com.villagecolony.core.type.ServerMemory.register(VillageHelpers.class, NEXT_BUILDING::clear);
    }

    private VillageHelpers() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerWorld world = server.getOverworld();
            long now = world.getTime();

            if (now % 1200 != 900) {
                return;
            }

            for (Colony colony : List.copyOf(VillageColonyMod.COLONIES.all())) {
                if (colony.isActive() && colony.isAttended(now)) {
                    pass(world, colony);
                }
            }
        });
    }

    static void pass(ServerWorld world, Colony colony) {
        for (Worker worker : VillageColonyMod.WORKERS.ofColony(colony.id())) {
            Optional<ProfessionType> trade = worker.profession();

            if (!(world.getEntity(worker.villagerId()) instanceof VillagerEntity villager)
                    || !VillageColonyMod.TASKS.assignedTo(worker.villagerId()).isEmpty()
                    || !WorkHours.isWorkTime(world, villager) || villager.isSleeping()) {
                continue;
            }

            int done = helps(world, colony, trade, villager);

            if (done > 0) {
                villager.swingHand(Hand.MAIN_HAND);
            }
        }
    }

    /**
     * A ajuda de quem está sem tarefa: o pedreiro tapa a rua e nivela, o fundidor leva o minério e acende a rua, o
     * pastor cuida do rebanho (O3), o construtor com a obra parada limpa e nivela o lote (O2) e o adulto sem profissão
     * carrega (O8).
     *
     * @return quanto ele fez
     */
    static int helps(ServerWorld world, Colony colony, Optional<ProfessionType> trade, VillagerEntity villager) {
        if (trade.isEmpty()) {
            return haulerHelps(world, colony, villager);
        }

        return switch (trade.get()) {
            case MASON -> masonHelps(world, colony, villager);
            case SMELTER -> smelterHelps(world, colony, villager);
            case SHEPHERD -> FlockCare.feed(world, colony, villager);
            case BUILDER -> GroundSmoothing.aroundTheOpenBuild(world, colony, villager);
            default -> 0;
        };
    }

    /**
     * O adulto sem profissão é o carregador da vila — O8 (autor, 2026-10-28): leva o baú da boca da mina para os baús
     * públicos e alivia os baús de profissão quase cheios.
     */
    static int haulerHelps(ServerWorld world, Colony colony, VillagerEntity hauler) {
        int carried = haulFromTheMine(world, colony, hauler);
        int spread = com.villagecolony.fabric.integration.ProfessionChestOverflow.spread(world, colony);

        if (spread > 0) {
            ActionJournal.action(hauler.getUuid(), "HAULED", "minecraft:chest", spread, null, "full chests to the village");
        }

        return carried + spread;
    }

    /** Tapa as falhas da rua; sem falha, alisa o chão da vila; sem nada a alisar, nivela em volta da próxima casa. */
    static int masonHelps(ServerWorld world, Colony colony, VillagerEntity mason) {
        int mended = StreetGaps.mend(world, colony.id(), mason.getBlockPos(), GAPS_PER_PASS);

        if (mended > 0) {
            ActionJournal.action(mason.getUuid(), "PAVED", "minecraft:dirt_path", mended, null, "street gaps");

            return mended;
        }

        // Sem falha de rua, alisa o chão da vila (autor, 2026-10-29).
        int smoothed = GroundSmoothing.pass(world, colony, mason);

        if (smoothed > 0) {
            return smoothed;
        }

        List<Building> standing = VillageColonyMod.BUILDINGS.ofColony(colony.id()).stream()
                .filter(Building::finished)
                .toList();

        if (standing.isEmpty()) {
            return 0;
        }

        int next = NEXT_BUILDING.merge(colony.id(), 1, Integer::sum) % standing.size();
        int levelled = SiteSurroundings.level(world, standing.get(next));

        if (levelled > 0) {
            ActionJournal.action(mason.getUuid(), "LEVELLED", standing.get(next).blueprint().toString(), levelled, null,
                    "around the house");
        }

        return levelled;
    }

    /** Leva o minério da boca da mina aos baús públicos e acende a rua escura. */
    static int smelterHelps(ServerWorld world, Colony colony, VillagerEntity smelter) {
        int carried = haulFromTheMine(world, colony, smelter);

        return carried + lightTheStreet(world, colony, smelter);
    }

    private static int haulFromTheMine(ServerWorld world, Colony colony, VillagerEntity carrier) {
        Optional<BlockPos> chest = VillageColonyMod.MINES.of(colony.id())
                .map(mine -> MinecraftTypeAdapter.toBlockPos(mine.shaft().entry()))
                .flatMap(mouth -> MineMouth.chestAt(world, mouth));

        if (chest.isEmpty() || !(world.getBlockEntity(chest.get()) instanceof ChestBlockEntity box)) {
            return 0;
        }

        int moved = 0;

        for (int slot = 0; slot < box.size() && moved < HAUL; slot++) {
            ItemStack stack = box.getStack(slot);

            if (stack.isEmpty()) {
                continue;
            }

            int take = Math.min(stack.getCount(), HAUL - moved);
            int left = PublicChests.store(world, colony.id(), chest.get(), stack.getItem(), take);
            int stored = take - left;

            if (stored > 0) {
                stack.decrement(stored);
                moved += stored;
            }
        }

        if (moved > 0) {
            box.markDirty();
            ActionJournal.action(carrier.getUuid(), "HAULED", "minecraft:chest", moved, null, "mine mouth to the village");
        }

        return moved;
    }

    static int lightTheStreet(ServerWorld world, Colony colony, VillagerEntity smelter) {
        Optional<ColonyRoads> roads = RoadIndex.roadsOf(colony.id());

        if (roads.isEmpty()) {
            return 0;
        }

        List<Long> columns = roads.get().columns();
        int tries = 0;

        for (int index : com.villagecolony.core.construction.model.LotScore.nearestFirst(columns,
                smelter.getBlockX(), smelter.getBlockZ())) {
            if (++tries > LIGHT_TRIES) {
                return 0;
            }

            long column = columns.get(index);
            int x = ColonyRoads.xOf(column);
            int z = ColonyRoads.zOf(column);

            if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }

            BlockPos above = new BlockPos(x, world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z), z);

            if (world.getLightLevel(LightType.BLOCK, above) >= DARK || lightNear(world, above)) {
                continue;
            }

            for (Direction side : Direction.Type.HORIZONTAL) {
                BlockPos spot = above.offset(side);
                BlockPos ground = spot.down();

                // Nem no chunk descarregado nem na borda dele: ler o lugar carregaria o chunk, e pôr a lanterna
                // avisa os vizinhos, que também seriam lidos.
                if (!insideTheLoadedArea(world, spot)) {
                    continue;
                }

                if (world.getBlockState(spot).isAir() && world.getBlockState(ground).isSideSolidFullSquare(world, ground,
                        Direction.UP) && SiteSurroundings.isNaturalGround(world.getBlockState(ground))
                        && !RoadIndex.isRoadArea(world, colony.id(), ground) && !insideABuilding(colony.id(), spot)) {
                    world.setBlockState(spot, Blocks.LANTERN.getDefaultState());
                    ActionJournal.action(smelter.getUuid(), "LIT", "minecraft:lantern", 1, spot, "street light");

                    return 1;
                }
            }
        }

        return 0;
    }

    /**
     * Se já há luz a {@link #LIGHT_SPACING} blocos: a luz de bloco no ponto conta o alcance dela. O bloco de chunk
     * descarregado é pulado — lê-lo carregaria o chunk na thread do servidor.
     */
    static boolean lightNear(ServerWorld world, BlockPos at) {
        for (BlockPos near : BlockPos.iterate(at.add(-LIGHT_SPACING / 2, -2, -LIGHT_SPACING / 2),
                at.add(LIGHT_SPACING / 2, 2, LIGHT_SPACING / 2))) {
            if (world.isChunkLoaded(near) && world.getBlockState(near).getLuminance() > 0) {
                return true;
            }
        }

        return false;
    }

    /** Se o lugar e os quatro vizinhos dele estão em chunk carregado. */
    static boolean insideTheLoadedArea(ServerWorld world, BlockPos spot) {
        if (!world.isChunkLoaded(spot)) {
            return false;
        }

        for (Direction side : Direction.Type.HORIZONTAL) {
            if (!world.isChunkLoaded(spot.offset(side))) {
                return false;
            }
        }

        return true;
    }

    private static boolean insideABuilding(UUID colonyId, BlockPos spot) {
        return VillageColonyMod.BUILDINGS.ofColony(colonyId).stream()
                .anyMatch(building -> building.contains(MinecraftTypeAdapter.toColonyPos(spot)));
    }
}
