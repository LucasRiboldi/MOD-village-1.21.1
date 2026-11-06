package com.villagecolony.fabric.work;

import com.villagecolony.fabric.integration.PublicChests;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.MineMouth;
import com.villagecolony.fabric.integration.SiteSurroundings;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * As alternativas do próprio ofício — regra temporária do autor de 2026-11-02
 * (docs/technical/Plano-Playtest-2026-11-02.md, opção 6).
 * Uma vez por minuto, no expediente:
 *
 * <ul>
 *   <li><b>pedreiro</b>: repara o terreno, começando pelo lote da obra aberta (Regra 215);</li>
 *   <li><b>pastor</b>: alimenta o rebanho;</li>
 *   <li><b>adulto sem profissão</b>: transporta itens e alivia os baús cheios.</li>
 * </ul>
 * As demais ajudas cruzadas da Regra 149 ficam suspensas.
 */
public final class VillageHelpers {

    static final int HAUL = 64;
    static final int LIGHT_SPACING = 12;

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
                    || !IdleWorkers.isFree(world, villager, null)) {
                continue;
            }

            int done = helps(world, colony, trade, villager);

            if (done > 0) {
                villager.swingHand(Hand.MAIN_HAND);
            }
        }
    }

    /**
     * Alternativas sem troca de ofício: pastor cuida do rebanho (O3), pedreiro nivela (Regra 215), adulto sem
     * profissão carrega (O8).
     *
     * @return quanto ele fez
     */
    static int helps(ServerWorld world, Colony colony, Optional<ProfessionType> trade, VillagerEntity villager) {
        if (trade.isEmpty()) {
            return haulerHelps(world, colony, villager);
        }

        return switch (trade.get()) {
            case SHEPHERD -> FlockCare.feed(world, colony, villager);
            case MASON -> masonHelps(world, colony, villager);
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

    /**
     * A função secundária do pedreiro ocioso (Regra 215): alisa o chão da vila, conserta o lote da obra aberta e nivela em volta da
     * próxima casa.
     */
    static int masonHelps(ServerWorld world, Colony colony, VillagerEntity mason) {
        int repaired = GroundSmoothing.aroundTheOpenBuild(world, colony, mason);

        if (repaired > 0) {
            return repaired;
        }

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
}
