package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.coordination.BlockReserve;
import com.villagecolony.core.coordination.BlockReserve.StockClass;
import com.villagecolony.core.coordination.WorkClock;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.integration.ChestWithdrawer;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.VillageFocus;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * O registro de blocos — Regra 342, fase 3 do plano de ofícios e reservas (ADR-062). Por colônia e por dia, cada bloco
 * conta o que a obra pôs ({@code PLACED}), quantas vezes ela o deixou de lado por falta ({@code SET_ASIDE}) e quanto dele
 * surgiu ({@code VC_SUPPLY}, lido do {@code Supplies} na hora da tabela). No fim do expediente sai a linha {@code VC_BLOCKS} com a classe, a reserva e o estoque de
 * cada bloco nos baús; o {@code /vc blocos} mostra o mesmo na hora. É a base para subir ou descer a reserva de cada tipo.
 */
public final class BlockLedger {

    /** O instante do dia em que a linha sai: o fim do expediente. */
    private static final long AT = WorkClock.WORK_END;

    private static final int USED = 0;

    private static final int WAITED = 1;

    private static final int SURGED = 2;

    /** Só nome de item entra; "under the building" e afins são exceções do surgimento, não blocos. */
    private static final Pattern ITEM_ID = Pattern.compile("[a-z0-9_./-]+(:[a-z0-9_./-]+)?");

    private static final Map<UUID, Map<String, int[]>> DAY = new HashMap<>();

    private static final Map<UUID, Long> REPORTED_DAY = new HashMap<>();

    static {
        ServerMemory.register(BlockLedger.class, () -> {
            DAY.clear();
            REPORTED_DAY.clear();
        });
    }

    private BlockLedger() {
    }

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getRegistryKey().equals(World.OVERWORLD) && world.getTime() % 100 == 0
                    && Math.floorMod(world.getTimeOfDay(), WorkClock.DAY) >= AT) {
                long day = world.getTimeOfDay() / WorkClock.DAY;

                for (Colony colony : VillageColonyMod.COLONIES.all()) {
                    if (VillageFocus.isWorking(world, colony.id())
                            && !Long.valueOf(day).equals(REPORTED_DAY.put(colony.id(), day))) {
                        VillageColonyMod.LOGGER.info(report(world, colony, day));
                        clear(colony.id());
                    }
                }
            }
        });
    }

    /** Uma ação do diário: a obra pôs ou deixou de lado este bloco. */
    static void action(UUID worker, String action, String what, int amount) {
        int column = switch (action) {
            case "PLACED" -> USED;
            case "SET_ASIDE" -> WAITED;
            default -> -1;
        };

        if (column >= 0 && amount > 0) {
            VillageColonyMod.WORKERS.find(worker).map(Worker::colonyId)
                    .ifPresent(colonyId -> count(colonyId, what, column, amount));
        }
    }

    /** Este bloco surgiu nesta colônia ({@code VC_SUPPLY}). */
    public static void surged(UUID colonyId, String what, int count) {
        if (count > 0) {
            count(colonyId, what, SURGED, count);
        }
    }

    /** As contas do dia: bloco → {posto, esperado, surgido}. */
    static Map<String, int[]> today(UUID colonyId) {
        return DAY.getOrDefault(colonyId, Map.of());
    }

    /** Esquece o dia desta colônia. */
    static void clear(UUID colonyId) {
        DAY.remove(colonyId);
    }

    /** {@code oak_planks=V,12,40,2,1,0}: classe, reserva, estoque, posto, esperado, surgido. */
    static String entry(String key, StockClass stockClass, int reserve, int stock, int[] counts) {
        return key + "=" + stockClass.name().charAt(0) + "," + reserve + "," + stock + ","
                + counts[USED] + "," + counts[WAITED] + "," + counts[SURGED];
    }

    /** A linha {@code VC_BLOCKS} do dia: todo bloco nos baús e todo bloco que a obra usou, esperou ou viu surgir. */
    public static String report(ServerWorld world, Colony colony, long day) {
        List<String> entries = new ArrayList<>();

        for (Row row : rows(world, colony)) {
            entries.add(entry(row.key(), row.stockClass(), row.reserve(), row.stock(),
                    new int[] {row.used(), row.waited(), row.surged()}));
        }

        return "VC_BLOCKS version=1 colony=" + colony.id().toString().substring(0, 8) + " day=" + day
                + " blocks=" + entries.size() + (entries.isEmpty() ? "" : " " + String.join(" ", entries));
    }

    /** Uma linha da tabela: o bloco, a classe, a reserva, o estoque e as contas do dia. */
    public record Row(String key, StockClass stockClass, int reserve, int stock, int used, int waited, int surged) {

        int activity() {
            return used + waited + surged;
        }
    }

    /** A tabela de agora, para a linha e para o {@code /vc blocos}: os mais movimentados e os abaixo da reserva antes. */
    public static List<Row> rows(ServerWorld world, Colony colony) {
        com.villagecolony.fabric.integration.Supplies.takeItems(colony.id()).forEach((what, count) ->
                surged(colony.id(), what, count));
        Map<String, Integer> stock = stockOf(world, colony);
        Map<String, int[]> day = today(colony.id());
        List<Row> rows = new ArrayList<>();

        for (String key : new TreeSet<>(union(stock, day))) {
            Item item = StockReserve.itemOf(key);
            StockClass stockClass = StockReserve.classOf(world, item);

            int[] counts = day.getOrDefault(key, new int[3]);

            rows.add(new Row(key, stockClass, BlockReserve.reserveOf(stockClass), stock.getOrDefault(key, 0),
                    counts[USED], counts[WAITED], counts[SURGED]));
        }

        rows.sort(Comparator.comparingInt(Row::activity).reversed()
                .thenComparing(row -> row.stock() >= row.reserve())
                .thenComparing(Row::key));

        return rows;
    }

    private static java.util.Set<String> union(Map<String, Integer> stock, Map<String, int[]> day) {
        java.util.Set<String> keys = new java.util.HashSet<>(stock.keySet());
        keys.addAll(day.keySet());
        return keys;
    }

    /** Os blocos nos baús da colônia, pelo id curto. */
    private static Map<String, Integer> stockOf(ServerWorld world, Colony colony) {
        Map<String, Integer> stock = new TreeMap<>();

        for (ColonyPos chest : ColonyChests.nearestFirst(world, colony.id(), colony.center())) {
            ChestWithdrawer.contents(world, chest).forEach((item, count) -> {
                if (item instanceof BlockItem) {
                    stock.merge(keyOf(StockReserve.idOf(item)), count, Integer::sum);
                }
            });
        }

        return stock;
    }

    private static void count(UUID colonyId, String what, int column, int amount) {
        String key = keyOf(what);

        if (ITEM_ID.matcher(key).matches()) {
            DAY.computeIfAbsent(colonyId, ignored -> new HashMap<>())
                    .computeIfAbsent(key, ignored -> new int[3])[column] += amount;
        }
    }

    /** O id curto: sem o {@code minecraft:}. */
    static String keyOf(String what) {
        return what.startsWith("minecraft:") ? what.substring("minecraft:".length()) : what;
    }
}
