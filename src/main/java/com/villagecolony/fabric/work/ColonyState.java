package com.villagecolony.fabric.work;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BenchPresence;
import com.villagecolony.fabric.integration.ChestInventoryReader;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.LotRefusals;
import com.villagecolony.fabric.integration.ProfessionChestOverflow;
import com.villagecolony.fabric.integration.RoadIndex;
import com.villagecolony.fabric.integration.SupplyGapLedger;
import com.villagecolony.fabric.integration.VillageStyle;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.EntityType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * O retrato completo de uma colônia — {@code VC_STATE}, pedido do autor (2026-10-28): os dados do jogo e do
 * mod que as análises precisam, num registro só por vila a cada cinco minutos, para comparar sessões e
 * versões. Vai para o log ({@code VC_STATE}) e para o arquivo da pasta do mundo ({@link AnalyticsLog}),
 * que sobrevive entre sessões; {@code scripts/state_report.py} lê os dois.
 *
 * <p>O esquema tem número ({@link #SCHEMA}): campo novo entra sem mudar o número; campo que muda de
 * sentido sobe o número, e o script lê os dois.
 */
public final class ColonyState {

    /** A versão do formato do registro. */
    public static final int SCHEMA = 1;

    private ColonyState() {
    }

    /** O retrato desta colônia agora. */
    public static JsonObject of(ServerWorld world, Colony colony) {
        JsonObject state = new JsonObject();
        long tick = world.getTime();
        BlockPos center = MinecraftTypeAdapter.toBlockPos(colony.center());

        state.addProperty("schema", SCHEMA);
        state.addProperty("mod", version());
        state.addProperty("tick", tick);
        state.addProperty("day", world.getTimeOfDay() / 24_000);
        state.addProperty("daytime", world.getTimeOfDay() % 24_000);
        state.addProperty("colony", colony.id().toString().substring(0, 8));
        state.addProperty("biome", world.getBiome(center).getKey().map(key -> key.getValue().getPath()).orElse("?"));
        state.addProperty("style", VillageStyle.biomeAt(world, colony.center()).getValue().getPath());
        state.addProperty("x", center.getX());
        state.addProperty("y", center.getY());
        state.addProperty("z", center.getZ());
        colony.bounds().ifPresent(box -> {
            state.addProperty("width", box.maxX() - box.minX() + 1);
            state.addProperty("depth", box.maxZ() - box.minZ() + 1);

            // Os cantos: com o centro fixo (Regra 141), centro e tamanho não dizem onde a caixa está.
            JsonObject corners = new JsonObject();
            corners.addProperty("minX", box.minX());
            corners.addProperty("minZ", box.minZ());
            corners.addProperty("maxX", box.maxX());
            corners.addProperty("maxZ", box.maxZ());
            state.add("box", corners);
        });
        state.addProperty("mspt", Math.round(world.getServer().getAverageNanosPerTick() / 100_000.0) / 10.0);

        state.add("people", people(colony));
        state.add("time", timeShares(colony.id()));
        state.add("stock", stock(world, colony));
        state.add("build", build(colony));
        state.add("tasks", tasks(colony.id()));
        state.add("mine", mine(world, colony.id()));
        state.add("village", village(world, colony));

        return state;
    }

    private static JsonObject people(Colony colony) {
        JsonObject people = new JsonObject();
        List<Worker> workers = VillageColonyMod.WORKERS.ofColony(colony.id());
        Map<String, Integer> trades = new TreeMap<>();
        Map<WorkTime.Badge, Integer> badges = new EnumMap<>(WorkTime.Badge.class);
        JsonArray stuck = new JsonArray();

        for (Worker worker : workers) {
            trades.merge(worker.profession().map(Enum::name).orElse("NONE"), 1, Integer::sum);
            WorkTime.Badge badge = WorkTime.badgeOf(worker.villagerId());
            badges.merge(badge, 1, Integer::sum);

            if (badge == WorkTime.Badge.STUCK && stuck.size() < 8) {
                stuck.add(worker.villagerId().toString().substring(0, 8) + ":"
                        + worker.profession().map(Enum::name).orElse("NONE"));
            }
        }

        people.addProperty("villagers", workers.size());
        people.addProperty("employed", workers.stream().filter(worker -> worker.profession().isPresent()).count());
        people.addProperty("beds", colony.observedBeds());
        people.addProperty("stranded", workers.stream().filter(worker -> StrandedWorkers.isStranded(worker.villagerId())).count());
        people.add("trades", toJson(trades));
        JsonObject badgeJson = new JsonObject();
        badges.forEach((badge, count) -> badgeJson.addProperty(badge.name(), count));
        people.add("badges", badgeJson);
        people.add("stuck", stuck);

        return people;
    }

    /** A parte de cada estado na última janela de cinco minutos, por ofício, em porcentagem. */
    private static JsonObject timeShares(UUID colonyId) {
        JsonObject time = new JsonObject();

        for (Map.Entry<String, Map<WorkTime.State, Integer>> trade : WorkTime.lastWindowOf(colonyId).entrySet()) {
            int total = trade.getValue().values().stream().mapToInt(Integer::intValue).sum();

            if (total == 0) {
                continue;
            }

            JsonObject shares = new JsonObject();
            trade.getValue().forEach((state, seconds) -> shares.addProperty(state.name(), Math.round(100.0 * seconds / total)));
            shares.addProperty("rule51", WorkTime.idleRuleShare(trade.getValue()));
            time.add(trade.getKey(), shares);
        }

        return time;
    }

    private static JsonObject stock(ServerWorld world, Colony colony) {
        JsonObject stock = new JsonObject();
        List<ColonyPos> chests = ColonyChests.nearestFirst(world, colony.id(), colony.center());
        var total = ChestInventoryReader.survey(world, chests).resources().total();

        stock.addProperty("chests", chests.size());
        ProfessionChestOverflow.fullnessOf(colony.id()).ifPresent(share -> stock.addProperty("fill", Math.round(share * 100)));
        stock.addProperty("needsStorage", ProfessionChestOverflow.needsStorage(colony.id()));

        for (ResourceGroup group : ResourceGroup.values()) {
            stock.addProperty(group.name(), total.amountOfGroup(group));
        }

        return stock;
    }

    private static JsonObject build(Colony colony) {
        JsonObject build = new JsonObject();

        build.addProperty("buildings", VillageColonyMod.BUILDINGS.ofColony(colony.id()).size());
        build.addProperty("lotsOk", LotRefusals.acceptedIn(colony.id()));
        build.addProperty("supplyGaps", SupplyGapLedger.sessionCountOf(colony.id()));
        // A nota do último lote no mapa de interesse e a distância média das construções ao centro: medem se a
        // vila adensa antes de esticar (autor, 2026-10-28).
        com.villagecolony.fabric.integration.SweepState.lastLotScore(colony.id())
                .ifPresent(score -> build.addProperty("lotScore", Math.round(score * 10) / 10.0));
        build.addProperty("meanDistance", Math.round(VillageColonyMod.BUILDINGS.ofColony(colony.id()).stream()
                .mapToDouble(building -> Math.hypot((building.min().x() + building.max().x()) / 2.0 - colony.center().x(),
                        (building.min().z() + building.max().z()) / 2.0 - colony.center().z()))
                .average().orElse(0)));
        VillageColonyMod.CONSTRUCTIONS.openOf(colony.id()).ifPresent(open -> {
            build.addProperty("open", open.blueprint().id().path().replaceAll(".*/", ""));
            build.addProperty("state", open.state().name());
            build.addProperty("left", open.remaining().size());
            build.addProperty("deferred", open.deferredPieces().size());
        });

        JsonObject refusals = new JsonObject();
        for (LotRefusals.Reason reason : LotRefusals.Reason.values()) {
            int count = LotRefusals.countOf(colony.id(), reason);

            if (count > 0) {
                refusals.addProperty(reason.name(), count);
            }
        }
        build.add("refusals", refusals);

        return build;
    }

    private static JsonObject tasks(UUID colonyId) {
        JsonObject tasks = new JsonObject();
        Map<String, Integer> open = new TreeMap<>();
        int held = 0;

        for (Task task : VillageColonyMod.TASKS.ofColony(colonyId)) {
            if (task.isOpen()) {
                open.merge(task.type().name(), 1, Integer::sum);
                held += task.isHeld() ? 1 : 0;
            }
        }

        tasks.add("open", toJson(open));
        tasks.addProperty("held", held);

        return tasks;
    }

    private static JsonObject mine(ServerWorld world, UUID colonyId) {
        JsonObject mine = new JsonObject();

        VillageColonyMod.MINES.of(colonyId).ifPresent(found -> {
            mine.addProperty("entryY", found.shaft().entry().y());
            mine.addProperty("levels", MineEntrance.levelsDown(world, colonyId, found));
            mine.addProperty("cut", java.util.Arrays.stream(found.cuts()).sum());
        });

        return mine;
    }

    private static JsonObject village(ServerWorld world, Colony colony) {
        JsonObject village = new JsonObject();
        BlockPos center = MinecraftTypeAdapter.toBlockPos(colony.center());
        Box area = colony.bounds()
                .map(box -> new Box(box.minX(), box.minY() - 8, box.minZ(), box.maxX() + 1, box.maxY() + 8, box.maxZ() + 1))
                .orElseGet(() -> new Box(center).expand(48));

        village.addProperty("roads", RoadIndex.roadsOf(colony.id()).map(roads -> roads.columns().size()).orElse(0));

        JsonArray benches = new JsonArray();
        for (ProfessionType trade : ProfessionType.values()) {
            if (BenchPresence.has(world, colony, trade)) {
                benches.add(trade.name());
            }
        }
        village.add("benches", benches);

        JsonObject animals = new JsonObject();
        animals.addProperty("sheep", world.getEntitiesByType(EntityType.SHEEP, area, any -> true).size());
        animals.addProperty("cow", world.getEntitiesByType(EntityType.COW, area, any -> true).size());
        animals.addProperty("pig", world.getEntitiesByType(EntityType.PIG, area, any -> true).size());
        animals.addProperty("chicken", world.getEntitiesByType(EntityType.CHICKEN, area, any -> true).size());
        village.add("animals", animals);

        return village;
    }

    private static JsonObject toJson(Map<String, Integer> counts) {
        JsonObject json = new JsonObject();
        counts.forEach(json::addProperty);
        return json;
    }

    static String version() {
        return FabricLoader.getInstance().getModContainer("villagecolony")
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                .orElse("?");
    }
}
