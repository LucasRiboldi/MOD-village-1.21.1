package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.core.coordination.WorkClock;
import com.villagecolony.fabric.integration.SiteSurroundings;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O Zelador, dono do chão da vila — Regra 330, ADR-059. Ninguém mais calça nem nivela.
 *
 * <p>O dia dele, na ordem do que mais atrapalha quem anda:
 * <ol>
 *   <li>a linha do grafo a calçar, com o fardo de feno no caminho tirado para os baús ({@link PathPaving}, que tem a
 *       vez enquanto há trecho a calçar);</li>
 *   <li>o caminho fora da linha volta ao solo do bioma, para ficar uma linha só ({@link PathTrim});</li>
 *   <li>entre as construções: o lote da obra aberta, o vão sob a peça da vila, o vão entre as bases e a volta das
 *       casas, na altura da rua da construção e com o excesso limpo;</li>
 *   <li>a fila de quadrados do terreno, com desnível de até 1 ({@link TerrainQueue}, Regra 323);</li>
 *   <li>a neve que a obra pede ({@link SnowShovel}).</li>
 * </ol>
 * No fim do expediente, a linha {@code VC_KEEPER} conta o que falta e o que ele fez no dia ({@link TerrainReport}).
 */
public final class GroundKeeper {

    /** Uma passada a cada dez segundos. */
    static final int EVERY = 200;

    /** Colônia → [dia, calçados, devolvidos ao solo, nivelados]. */
    private static final Map<UUID, long[]> DAY = new HashMap<>();

    /** Colônia → [trechos a calçar, caminho fora da linha], da última leitura do grafo. */
    private static final Map<UUID, long[]> GRAPH = new HashMap<>();

    /** Qual construção ele arruma em volta na próxima vez, por colônia. */
    private static final Map<UUID, Integer> NEXT_BUILDING = new HashMap<>();

    static {
        ServerMemory.register(GroundKeeper.class, GroundKeeper::clearAll);
    }

    private GroundKeeper() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerWorld world = server.getOverworld();
            long now = world.getTime();

            if (now % EVERY != EVERY / 2) {
                return;
            }

            for (Colony colony : List.copyOf(VillageColonyMod.COLONIES.all())) {
                if (colony.isActive() && colony.isAttended(now)) {
                    pass(world, colony);
                }
            }
        });
    }

    /** Se este trabalhador é o Zelador. */
    static boolean isKeeper(Worker worker) {
        return worker.profession().filter(ProfessionType.GROUNDSKEEPER::equals).isPresent();
    }

    static void pass(ServerWorld world, Colony colony) {
        for (Worker worker : VillageColonyMod.WORKERS.ofColony(colony.id())) {
            if (!isKeeper(worker) || !(world.getEntity(worker.villagerId()) instanceof VillagerEntity keeper)
                    || !IdleWorkers.isFree(world, keeper, null)) {
                continue;
            }

            if (day(world, colony, keeper) > 0) {
                keeper.swingHand(Hand.MAIN_HAND);
                WorkTime.groundWork(keeper.getUuid(), world.getTime());
            }
        }
    }

    /**
     * Um passo do dia do Zelador, depois da calçada: fora da linha, entre as construções, a fila do terreno.
     *
     * @return quanto ele fez
     */
    static int day(ServerWorld world, Colony colony, VillagerEntity keeper) {
        int trimmed = PathTrim.pass(world, colony);

        if (trimmed > 0) {
            ActionJournal.action(keeper.getUuid(), "LEVELLED", "minecraft:grass_block", trimmed, keeper.getBlockPos(),
                    "the path beside the line");
            count(world, colony.id(), 2, trimmed);
            return trimmed;
        }

        int between = betweenBuildings(world, colony, keeper);

        if (between > 0) {
            count(world, colony.id(), 3, between);
            return between;
        }

        int levelled = TerrainQueue.pass(world, colony, keeper);

        if (levelled > 0) {
            count(world, colony.id(), 3, levelled);
        }

        return levelled;
    }

    /** O chão entre as construções, na altura da rua de cada uma e com o excesso limpo (Regras 242, 297 e 304). */
    private static int betweenBuildings(ServerWorld world, Colony colony, VillagerEntity keeper) {
        int repaired = GroundSmoothing.aroundTheOpenBuild(world, colony, keeper);

        if (repaired > 0) {
            return repaired;
        }

        int under = com.villagecolony.fabric.integration.UnderPieceFill.pass(world, colony);

        if (under > 0) {
            ActionJournal.action(keeper.getUuid(), "LEVELLED", "village piece", under, null, "under the building");
            return under;
        }

        int pens = GroundSmoothing.clearPens(world, colony.id());

        if (pens > 0) {
            ActionJournal.action(keeper.getUuid(), "LEVELLED", "minecraft:grass_block", pens, keeper.getBlockPos(),
                    "the inside of a pen");
            return pens;
        }

        int gaps = BuildingGaps.pass(world, colony);

        if (gaps > 0) {
            ActionJournal.action(keeper.getUuid(), "LEVELLED", "minecraft:dirt", gaps, keeper.getBlockPos(),
                    "around the buildings");
            return gaps;
        }

        List<Building> standing = VillageColonyMod.BUILDINGS.ofColony(colony.id()).stream()
                .filter(Building::finished)
                .toList();

        if (standing.isEmpty()) {
            return 0;
        }

        Building next = standing.get(NEXT_BUILDING.merge(colony.id(), 1, Integer::sum) % standing.size());
        int levelled = SiteSurroundings.level(world, next);

        if (levelled > 0) {
            ActionJournal.action(keeper.getUuid(), "LEVELLED", next.blueprint().toString(), levelled, null,
                    "around the house");
        }

        return levelled;
    }

    /** O Zelador calçou {@code blocks} da linha do grafo. */
    static void paved(ServerWorld world, UUID colonyId, int blocks) {
        count(world, colonyId, 1, blocks);
    }

    /** A leitura do grafo de hoje: quanto falta calçar e quanto caminho está fora da linha. */
    static void graphRead(UUID colonyId, int toPave, long offLine) {
        GRAPH.put(colonyId, new long[] {toPave, offLine});
    }

    private static void count(ServerWorld world, UUID colonyId, int slot, int amount) {
        long today = Math.floorDiv(world.getTimeOfDay(), WorkClock.DAY);
        long[] day = DAY.computeIfAbsent(colonyId, ignored -> new long[4]);

        if (day[0] != today) {
            java.util.Arrays.fill(day, 0);
            day[0] = today;
        }

        day[slot] += amount;
    }

    /**
     * A linha do dia: quantos Zeladores, o que falta (trechos a calçar, caminho fora da linha, degraus maiores que 1) e
     * o que ele fez (calçou, devolveu ao solo, nivelou).
     */
    static void report(ServerWorld world, Colony colony, long day, int steps) {
        long[] done = Optional.ofNullable(DAY.get(colony.id())).filter(counts -> counts[0] == day).orElse(new long[4]);
        long[] graph = GRAPH.getOrDefault(colony.id(), new long[2]);
        long keepers = VillageColonyMod.WORKERS.ofColony(colony.id()).stream().filter(GroundKeeper::isKeeper).count();

        VillageColonyMod.LOGGER.info("VC_KEEPER version=1 colony={} day={} keepers={} toPave={} offLine={} steps={} "
                        + "paved={} trimmed={} levelled={}", colony.id().toString().substring(0, 8), day, keepers,
                graph[0], graph[1], steps, done[1], done[2], done[3]);
    }

    static void clearAll() {
        DAY.clear();
        GRAPH.clear();
        NEXT_BUILDING.clear();
    }
}
