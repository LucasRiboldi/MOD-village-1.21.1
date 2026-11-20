package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.coordination.WorkClock;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * O balanço diário dos itens nos baús das vilas — Regra 344, telemetria T3. As portas do mod contam cada item que
 * entrou ({@link ChestDepositor}), saiu ({@link ChestWithdrawer}) ou não coube; no fim do expediente sai a linha
 * {@code VC_FLOW} com, por item, {@code entrou,saiu,recusado,estoque de ontem,estoque de hoje,por fora}. "Por fora" é
 * {@code hoje − ontem − (entrou − saiu)}: o que mudou sem passar pelas portas (o jogador, ou um caminho do mod que mexe
 * no baú direto). O {@code flow_report.py} soma os dias.
 */
public final class ItemFlow {

    private static final int IN = 0;

    private static final int OUT = 1;

    private static final int REFUSED = 2;

    private static final Map<String, int[]> DAY = new HashMap<>();

    /** O estoque do último balanço; vazio antes do primeiro. */
    private static Map<String, Integer> lastStock = new HashMap<>();

    private static boolean known;

    private static long reportedDay = Long.MIN_VALUE;

    static {
        ServerMemory.register(ItemFlow.class, ItemFlow::clear);
    }

    private ItemFlow() {
    }

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (!world.getRegistryKey().equals(World.OVERWORLD) || world.getTime() % 100 != 0
                    || Math.floorMod(world.getTimeOfDay(), WorkClock.DAY) < WorkClock.WORK_END) {
                return;
            }

            long day = world.getTimeOfDay() / WorkClock.DAY;

            if (day != reportedDay && VillageColonyMod.COLONIES.all().stream()
                    .anyMatch(colony -> VillageFocus.isWorking(world, colony.id()))) {
                reportedDay = day;
                VillageColonyMod.LOGGER.info(report(world, day));
            }
        });
    }

    static void in(Item item, int amount) {
        count(item, IN, amount);
    }

    static void out(Item item, int amount) {
        count(item, OUT, amount);
    }

    static void refused(Item item, int amount) {
        count(item, REFUSED, amount);
    }

    private static void count(Item item, int column, int amount) {
        if (amount > 0) {
            DAY.computeIfAbsent(keyOf(item), ignored -> new int[3])[column] += amount;
        }
    }

    /** A linha do dia e o recomeço da conta: o estoque de hoje vira o de ontem do próximo balanço. */
    public static String report(ServerWorld world, long day) {
        Map<String, Integer> stock = stockOf(world);
        List<String> entries = new ArrayList<>();
        Set<String> keys = new TreeSet<>(DAY.keySet());

        for (Map.Entry<String, Integer> held : stock.entrySet()) {
            if (known && !held.getValue().equals(lastStock.getOrDefault(held.getKey(), 0))) {
                keys.add(held.getKey());
            }
        }

        if (known) {
            lastStock.forEach((key, count) -> {
                if (!stock.containsKey(key)) {
                    keys.add(key);
                }
            });
        }

        for (String key : keys) {
            int[] counts = DAY.getOrDefault(key, new int[3]);
            int before = lastStock.getOrDefault(key, 0);
            int now = stock.getOrDefault(key, 0);
            String outside = known ? Integer.toString(now - before - (counts[IN] - counts[OUT])) : "?";

            entries.add(key + "=" + counts[IN] + "," + counts[OUT] + "," + counts[REFUSED] + "," + (known ? before : "?")
                    + "," + now + "," + outside);
        }

        DAY.clear();
        lastStock = stock;
        known = true;

        return "VC_FLOW version=1 day=" + day + " items=" + entries.size()
                + (entries.isEmpty() ? "" : " " + String.join(" ", entries));
    }

    /** Todos os itens nos baús de todas as vilas, cada baú uma vez. */
    private static Map<String, Integer> stockOf(ServerWorld world) {
        Set<ColonyPos> chests = new LinkedHashSet<>();

        for (Colony colony : VillageColonyMod.COLONIES.all()) {
            chests.addAll(ColonyChests.nearestFirst(world, colony.id(), colony.center()));
        }

        Map<String, Integer> stock = new TreeMap<>();

        for (ColonyPos chest : chests) {
            ChestWithdrawer.contents(world, chest).forEach((item, count) -> stock.merge(keyOf(item), count, Integer::sum));
        }

        return stock;
    }

    private static String keyOf(Item item) {
        var id = Registries.ITEM.getId(item);

        return id.getNamespace().equals("minecraft") ? id.getPath() : id.toString();
    }

    /** Esquece a conta e o estoque guardado. */
    static void clear() {
        DAY.clear();
        lastStock = new HashMap<>();
        known = false;
        reportedDay = Long.MIN_VALUE;
    }
}
