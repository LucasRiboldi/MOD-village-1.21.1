package com.villagecolony.fabric.work;

import com.google.gson.JsonObject;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.block.Block;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O diário da vila — escolha do autor (2026-10-28): o registro automático tem de bater com o jogo. Três
 * tipos de linha no arquivo do {@link AnalyticsLog} (e no log do jogo):
 *
 * <ul>
 *   <li>{@code VC_EVENT} ({@code kind: "event"}) — cada fato, quando acontece: obra aberta e fechada (com as
 *       peças contadas no mundo), a caixa que cresce, as camas que mudam, o teleporte para a cama, o nível da
 *       mina, a morte de um aldeão. Com dia e hora do jogo e a versão do mod.</li>
 *   <li>{@code VC_DAY} ({@code kind: "day"}) — o retrato da vila ao virar o dia de jogo, com o tempo das
 *       profissões somado no dia inteiro.</li>
 *   <li>{@code VC_AUDIT} ({@code kind: "audit"}) — ao virar o dia, o que o mod afirma conferido contra o
 *       mundo: camas, trabalhadores vivos e as peças da obra aberta que já estão no lugar.</li>
 * </ul>
 */
public final class VillageDiary {

    static {
        ServerMemory.register(VillageDiary.class, VillageDiary::clearAll);
    }

    private static @Nullable MinecraftServer server;

    /** O tempo de expediente somado no dia, por colônia e ofício. */
    private static final Map<UUID, Map<String, EnumMap<WorkTime.State, Integer>>> DAY = new HashMap<>();

    /** O último dia de jogo visto por colônia. */
    private static final Map<UUID, Long> LAST_DAY = new HashMap<>();

    private VillageDiary() {
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(started -> server = started);
        ServerLifecycleEvents.SERVER_STOPPED.register(stopped -> server = null);
        ServerTickEvents.END_SERVER_TICK.register(tick -> {
            if (tick.getTicks() % 200 == 77) {
                turnTheDay(tick.getOverworld());
            }
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof VillagerEntity villager) {
                VillageColonyMod.WORKERS.find(villager.getUuid()).ifPresent(worker -> {
                    JsonObject details = new JsonObject();
                    details.addProperty("villager", villager.getUuid().toString().substring(0, 8));
                    details.addProperty("profession", worker.profession().map(Enum::name).orElse("NONE"));
                    details.addProperty("cause", source.getName());
                    details.addProperty("at", villager.getBlockPos().toShortString());
                    event(worker.colonyId(), "death", details);
                });
            }
        });
    }

    static void clearAll() {
        DAY.clear();
        LAST_DAY.clear();
    }

    /** Um fato da vila: vai ao log ({@code VC_EVENT}) e ao arquivo do mundo. */
    public static void event(UUID colonyId, String type, JsonObject details) {
        MinecraftServer current = server;

        if (current == null) {
            return;
        }

        ServerWorld world = current.getOverworld();
        JsonObject record = header(world, colonyId);
        record.addProperty("kind", "event");
        record.addProperty("type", type);
        details.entrySet().forEach(entry -> record.add(entry.getKey(), entry.getValue()));
        VillageColonyMod.LOGGER.info("VC_EVENT {}", record);
        AnalyticsLog.append(world, record);
    }

    /** A obra terminou: quantas peças do projeto estão de fato no mundo (o log dizia "0 blocks placed"). */
    public static void buildFinished(ConstructionProject project, int placedByLast, @Nullable UUID builder) {
        MinecraftServer current = server;
        int pieces = project.blueprint().blocks().size();
        int inPlace = current == null ? -1 : inPlace(current.getOverworld(), project, project.blueprint().blocks());

        VillageColonyMod.LOGGER.info("Colony {} finished {} at {} — {} of {} pieces in place in the world"
                        + " ({} by the last builder, {}), now colony infrastructure",
                project.colonyId(), project.blueprint().id(), project.origin(), inPlace, pieces, placedByLast,
                builder == null ? "closed by the colony" : builder);

        JsonObject details = new JsonObject();
        details.addProperty("building", project.blueprint().id().toString());
        details.addProperty("origin", project.origin().toString());
        details.addProperty("pieces", pieces);
        details.addProperty("inPlace", inPlace);
        event(project.colonyId(), "build_finished", details);
    }

    /** Quantas destas peças do projeto já têm o bloco da planta no mundo. */
    static int inPlace(ServerWorld world, ConstructionProject project, List<BlueprintBlock> blocks) {
        int found = 0;

        for (BlueprintBlock block : blocks) {
            Optional<Block> wanted = MinecraftTypeAdapter.toBlock(block.block());
            BlockPos at = MinecraftTypeAdapter.toBlockPos(project.worldPositionOf(block));

            if (wanted.isPresent() && world.isChunkLoaded(at) && world.getBlockState(at).isOf(wanted.get())) {
                found++;
            }
        }

        return found;
    }

    /** A janela de cinco minutos do {@link WorkTime} entra na soma do dia. */
    static void addWindows(Map<UUID, Map<String, EnumMap<WorkTime.State, Integer>>> windows) {
        windows.forEach((colony, trades) -> trades.forEach((trade, seconds) -> {
            EnumMap<WorkTime.State, Integer> day = DAY.computeIfAbsent(colony, id -> new HashMap<>())
                    .computeIfAbsent(trade, id -> new EnumMap<>(WorkTime.State.class));

            seconds.forEach((state, value) -> day.merge(state, value, Integer::sum));
        }));
    }

    /** Ao virar o dia de jogo: o resumo do dia que passou e a auditoria, por vila atendida. */
    static void turnTheDay(ServerWorld world) {
        long today = world.getTimeOfDay() / 24_000;
        long now = world.getTime();

        for (Colony colony : List.copyOf(VillageColonyMod.COLONIES.all())) {
            if (!colony.canWork() || !colony.isAttended(now)) {
                continue;
            }

            Long last = LAST_DAY.put(colony.id(), today);

            if (last != null && last != today) {
                summary(world, colony, last);
                audit(world, colony);
            }
        }
    }

    private static void summary(ServerWorld world, Colony colony, long day) {
        JsonObject record = ColonyState.of(world, colony);
        record.addProperty("kind", "day");
        record.addProperty("ofDay", day);

        JsonObject time = new JsonObject();
        Map<String, EnumMap<WorkTime.State, Integer>> trades = DAY.remove(colony.id());

        if (trades != null) {
            trades.forEach((trade, seconds) -> {
                JsonObject shares = new JsonObject();
                int total = seconds.values().stream().mapToInt(Integer::intValue).sum();

                shares.addProperty("seconds", total);
                seconds.forEach((state, value) -> shares.addProperty(state.name().toLowerCase(java.util.Locale.ROOT),
                        total == 0 ? 0 : Math.round(100f * value / total)));
                shares.addProperty("rule51", WorkTime.idleRuleShare(new EnumMap<>(seconds)));
                time.add(trade, shares);
            });
        }

        record.add("dayTime", time);
        VillageColonyMod.LOGGER.info("VC_DAY {}", record);
        AnalyticsLog.append(world, record);
    }

    /** O que o mod afirma, conferido contra o mundo; a diferença sai como {@code VC_AUDIT}. */
    static JsonObject audit(ServerWorld world, Colony colony) {
        JsonObject record = header(world, colony.id());
        record.addProperty("kind", "audit");
        int problems = 0;

        record.addProperty("beds", colony.observedBeds());
        var counted = colony.bounds().map(box -> BoxBeds.count(world, box)).orElse(java.util.OptionalInt.empty());

        if (counted.isPresent()) {
            record.addProperty("bedsInTheWorld", counted.getAsInt());
            problems += counted.getAsInt() == colony.observedBeds() ? 0 : 1;
        }

        int workers = 0;
        int missing = 0;

        for (Worker worker : VillageColonyMod.WORKERS.ofColony(colony.id())) {
            workers++;

            if (!(world.getEntity(worker.villagerId()) instanceof VillagerEntity villager) || !villager.isAlive()) {
                missing++;
            }
        }

        record.addProperty("workers", workers);
        record.addProperty("workersNotInTheWorld", missing);
        problems += missing > 0 ? 1 : 0;

        Optional<ConstructionProject> open = VillageColonyMod.CONSTRUCTIONS.openOf(colony.id());

        if (open.isPresent()) {
            int left = open.get().remainingCount();
            int alreadyThere = inPlace(world, open.get(), open.get().remaining());

            record.addProperty("openBuilding", open.get().blueprint().id().toString());
            record.addProperty("piecesLeft", left);
            record.addProperty("piecesLeftAlreadyInPlace", alreadyThere);
            problems += alreadyThere > 0 ? 1 : 0;
        }

        record.addProperty("problems", problems);

        if (problems > 0) {
            VillageColonyMod.LOGGER.warn("VC_AUDIT {}", record);
        } else {
            VillageColonyMod.LOGGER.info("VC_AUDIT {}", record);
        }

        AnalyticsLog.append(world, record);

        return record;
    }

    private static JsonObject header(ServerWorld world, UUID colonyId) {
        JsonObject record = new JsonObject();
        record.addProperty("schema", ColonyState.SCHEMA);
        record.addProperty("mod", ColonyState.version());
        record.addProperty("tick", world.getTime());
        record.addProperty("day", world.getTimeOfDay() / 24_000);
        record.addProperty("daytime", world.getTimeOfDay() % 24_000);
        record.addProperty("colony", colonyId.toString().substring(0, 8));

        return record;
    }
}
