package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.WorkArea;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.data.save.ColonySavedData;
import com.villagecolony.data.save.MaeWorkStore;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Única porta de escrita MAE, sem ativar ou substituir os construtores legados — ADR-044. */
public final class MaeWorldWriter {
    public static final int MAX_WRITES_PER_TICK = 4;
    private static final int MAX_HISTORY = 1024;
    private static final long SLICE_NANOS = 2_000_000;
    private static final Map<ServerWorld, Budget> BUDGETS = new WeakHashMap<>();
    private final MaeWorkStore store;
    private final MaeStructureExecution.Sources sources;

    static {
        com.villagecolony.core.type.ServerMemory.register(MaeWorldWriter.class, MaeWorldWriter::clearAll);
    }

    /** O orçamento pertence à sessão do servidor, nunca ao próximo mundo — ADR-044. */
    public static void clearAll() {
        BUDGETS.clear();
    }

    private static final class Budget {
        int tick = -1;
        int attempts;
        long nanos;
    }

    public record Change(BlockPos position, BlockState before, BlockState after) {
        public Change {
            position = position.toImmutable();
            java.util.Objects.requireNonNull(before);
            java.util.Objects.requireNonNull(after);
        }
    }

    /** Adaptador normal usa o mesmo PersistentState das obras e colônias — ADR-044. */
    public static MaeWorldWriter of(ServerWorld world) {
        return new MaeWorldWriter(ColonySavedData.get(world.getServer()).maeWork());
    }

    public MaeWorldWriter(MaeWorkStore store) {
        this(store, MaeStructureExecution.Sources.GAME);
    }

    MaeWorldWriter(MaeWorkStore store, MaeStructureExecution.Sources sources) {
        this.store = store;
        this.sources = sources;
    }

    /** Chamada explícita representa a aprovação supervisionada da região; ainda não aprova obra — ADR-044. */
    public UUID reserve(ServerWorld world, UUID colonyId, WorkArea area) {
        return reserve(world, colonyId, area, false);
    }

    /** Reserva supervisionada v2; vegetação permitida continua intacta até aprovação tipada — MAE Fase 2. */
    public UUID reserveTerrain(ServerWorld world, UUID colonyId, WorkArea area) {
        return reserve(world, colonyId, area, true);
    }

    private UUID reserve(ServerWorld world, UUID colonyId, WorkArea area, boolean terrain) {
        checkThread(world);
        validateRegionSize(area);
        if (terrain) {
            MaeProtection.inspectTerrain(world, colonyId, area);
        } else {
            MaeProtection.inspect(world, colonyId, area);
        }
        MaeGenerationEvidence.inspect(world, area, true, 128);
        for (NbtCompound other : store.regions()) {
            validateRegion(other);
            if (dimension(world).equals(other.getString("dimension")) && area.intersects(area(other))) {
                throw new IllegalStateException("overlapping_region");
            }
        }
        NbtCompound region = new NbtCompound();
        UUID id = UUID.randomUUID();
        region.putUuid("id", id);
        region.putUuid("colony", colonyId);
        region.putString("dimension", dimension(world));
        region.put("area", encode(area));
        region.putLong("created", world.getTime());
        region.putString("proof", "generation_sections_sha256_v1");
        region.put("history", new NbtList());
        region.put("ownedSections", new NbtCompound());
        if (terrain) {
            region.putString("policy", "terrain_v2");
            store.reserveTerrain(region);
        } else {
            store.update(region);
        }
        return id;
    }

    /** O lote reutiliza a geometria de Building; aprovação fixa posições e estados antes/depois — ADR-044. */
    public void approve(ServerWorld world, UUID regionId, Building lot, List<Change> changes) {
        checkThread(world);
        NbtCompound region = region(world, regionId);
        if (region.contains("policy")) {
            throw new IllegalStateException("terrain_region_requires_typed_approval");
        }
        WorkArea lotArea = new WorkArea(lot.min(), lot.max());
        WorkArea inspection = MaeProtection.margin(lotArea);
        MaeSectionLocks.check(world, store, regionId, inspection);
        if (!region.getUuid("colony").equals(lot.colonyId()) || !area(region).contains(inspection)
                || lotArea.volume() > 4096 || changes.isEmpty()
                || changes.size() + history(region).size() > MAX_HISTORY) {
            throw new IllegalArgumentException("invalid_lot_or_history_budget");
        }
        if (region.contains("job") && !"complete".equals(region.getCompound("job").getString("state"))) {
            throw new IllegalStateException("unfinished_job");
        }
        MaeProtection.inspect(world, lot.colonyId(), inspection);
        NbtCompound expected = MaeGenerationEvidence.inspect(world, inspection, false, 8);
        validateEvidence(world, inspection, expected, region.getCompound("ownedSections"));
        var positions = new HashSet<BlockPos>();
        NbtList plan = new NbtList();
        for (Change change : changes) {
            BlockPos pos = change.position();
            if (!lotArea.contains(MinecraftTypeAdapter.toColonyPos(pos)) || !positions.add(pos)
                    || !MaeProtection.terrain(change.after()) || change.before().equals(change.after())
                    || !world.getBlockState(pos).equals(change.before())) {
                throw new IllegalArgumentException("invalid_change_or_expected_state");
            }
            NbtCompound step = new NbtCompound();
            step.putLong("position", pos.asLong());
            step.put("before", NbtHelper.fromBlockState(change.before()));
            step.put("after", NbtHelper.fromBlockState(change.after()));
            plan.add(step);
        }
        NbtCompound job = new NbtCompound();
        job.putUuid("id", lot.id());
        job.putString("blueprint", lot.blueprint().toString());
        job.put("area", encode(lotArea));
        job.put("expectedSections", expected);
        job.put("plan", plan);
        job.putInt("cursor", 0);
        job.putString("state", "approved");
        region.put("job", job);
        store.update(region);
    }

    /** Executa somente trabalho aprovado. Orçamento global do mundo, conferido entre operações indivisíveis — ADR-044. */
    public boolean approveTerrain(ServerWorld world, MaeTerrainPreparation.Prepared prepared) {
        NbtCompound region = terrainContext(world, prepared.regionId(), prepared.work(), prepared.readOnly());
        if (!region.getUuid("colony").equals(prepared.lot().colonyId())
                || !prepared.evidence().equals(MaeGenerationEvidence.inspect(world,
                        MaeProtection.margin(prepared.work()), false, 8))) {
            throw new IllegalStateException("changed_prepared_terrain");
        }
        if (prepared.changes().isEmpty()) return false;
        if (prepared.changes().size() + history(region).size() > MAX_HISTORY) {
            throw new IllegalStateException("history_budget_exceeded");
        }
        region.put("job", MaeTerrainExecution.job(world, prepared));
        store.update(region);
        return true;
    }

    /** Rua e terreno compartilham diário e exclusão mútua; reserva não publica a rota — MAE Fase 2. */
    public void approveRoad(ServerWorld world, MaeRoadPreparation.Prepared prepared) {
        NbtCompound job = prepared.job();
        NbtCompound region = terrainContext(world, prepared.region(), prepared.work(), job.getCompound("readOnly"));
        if (!job.getCompound("expectedSections").equals(MaeGenerationEvidence.inspect(world,
                MaeProtection.margin(prepared.work()), false, 8))) throw new IllegalStateException("changed_prepared_road");
        if (job.getList("plan", NbtElement.COMPOUND_TYPE).size() + history(region).size() + 1 > MAX_HISTORY) {
            throw new IllegalStateException("history_budget_exceeded");
        }
        MaeRoadExecution.validate(world, region, job);
        region.put("job", job);
        store.update(region);
    }

    /** Aprovação tipada de edifício sobre terreno concluído da mesma obra; promove o envelope — ADR-046. */
    void approveStructure(ServerWorld world, UUID regionId, ConstructionProject project, int quarterTurns,
                          MaeStructureAccess.Selected access) {
        NbtCompound region = region(world, regionId);
        NbtCompound job = MaeStructureExecution.job(world, region, project, quarterTurns, access, sources);
        MaeSectionLocks.check(world, store, regionId, MaeProtection.margin(area(job)));
        region.put("job", job);
        store.approveStructure(region);
    }

    /** Executa somente trabalho aprovado. Orçamento global do mundo, conferido entre operações indivisíveis — ADR-044. */
    public int runSlice(ServerWorld world, UUID regionId) {
        checkThread(world);
        Budget budget = BUDGETS.computeIfAbsent(world, ignored -> new Budget());
        if (budget.tick != world.getServer().getTicks()) {
            budget.tick = world.getServer().getTicks();
            budget.attempts = 0;
            budget.nanos = 0;
        }
        if (budget.attempts >= MAX_WRITES_PER_TICK || budget.nanos >= SLICE_NANOS) {
            return 0;
        }
        NbtCompound region;
        long lookupStarted = System.nanoTime();
        try {
            region = region(world, regionId);
        } finally {
            budget.nanos += System.nanoTime() - lookupStarted;
        }
        NbtCompound job = region.getCompound("job");
        boolean structure = MaeStructureExecution.is(job);
        if (structure ? !MaeStructureExecution.runnable(world, job) : !"approved".equals(job.getString("state"))) {
            return 0;
        }
        int written = 0;
        while (budget.attempts < MAX_WRITES_PER_TICK && budget.nanos < SLICE_NANOS) {
            budget.attempts++;
            long started = System.nanoTime();
            try {
                if (structure) validateJob(world, region, job);
                if (!(structure ? MaeStructureExecution.writeNext(world, region, job, store, sources)
                        : writeNext(world, region, job))) {
                    break;
                }
                written++;
                if ("complete".equals(job.getString("state")) || "built".equals(job.getString("state"))) {
                    break;
                }
            } catch (IllegalStateException | IllegalArgumentException failure) {
                job.putString("state", "blocked");
                job.putString("reason", failure.getMessage() == null ? "invalid_payload" : failure.getMessage());
                region.put("job", job);
                store.update(region);
                break;
            } finally {
                budget.nanos += System.nanoTime() - started;
            }
        }
        return written;
    }

    public void pause(ServerWorld world, UUID regionId) {
        NbtCompound region = region(world, regionId);
        NbtCompound job = region.getCompound("job");
        if ("approved".equals(job.getString("state"))) {
            job.putString("state", "paused");
            region.put("job", job);
            store.update(region);
        }
    }

    /** Retomada explícita revalida o mundo; escrita interrompida sem confirmação exige análise, nunca repetição — ADR-044. */
    public void resume(ServerWorld world, UUID regionId) {
        NbtCompound region = region(world, regionId);
        NbtCompound job = region.getCompound("job");
        if (MaeStructureExecution.is(job) && List.of("paused", "blocked").contains(job.getString("state"))) {
            MaeStructureExecution.revalidate(world, region, job, store, sources);
        }
        if (!List.of("paused", "blocked").contains(job.getString("state")) || job.getBoolean("pendingWrite") || job.getBoolean("pendingPublication")) {
            throw new IllegalStateException("unconfirmed_or_nonresumable_job");
        }
        validateJob(world, region, job);
        job.putString("state", "approved");
        job.remove("reason");
        region.put("job", job);
        store.update(region);
    }

    private boolean writeNext(ServerWorld world, NbtCompound region, NbtCompound job) {
        validateJob(world, region, job);
        NbtList plan = job.getList("plan", NbtElement.COMPOUND_TYPE);
        int cursor = job.getInt("cursor");
        NbtCompound step = plan.getCompound(cursor);
        if (!step.contains("position", NbtElement.LONG_TYPE) || !step.contains("before", NbtElement.COMPOUND_TYPE)
                || !step.contains("after", NbtElement.COMPOUND_TYPE)) {
            throw new IllegalStateException("invalid_change_payload");
        }
        BlockPos pos = BlockPos.fromLong(step.getLong("position"));
        BlockState before = decodeState(step.getCompound("before"));
        BlockState after = decodeState(step.getCompound("after"));
        if (!area(job).contains(MinecraftTypeAdapter.toColonyPos(pos)) || !world.getBlockState(pos).equals(before)
                || !(MaeRoadExecution.TYPE.equals(job.getString("type")) ? MaeRoadExecution.accepts(after) : MaeProtection.terrain(after))) {
            throw new IllegalStateException("invalid_or_changed_target");
        }
        NbtCompound expectedAfter = MaeGenerationEvidence.after(world, MaeProtection.margin(area(job)), pos, after);
        NbtCompound record = step.copy();
        if (job.contains("type")) MaeTerrainExecution.intent(step, record);
        record.putUuid("operation", job.getUuid("id"));
        record.putLong("tick", world.getTime());
        record.putString("result", "pending");
        NbtList history = history(region);
        if (history.size() >= MAX_HISTORY) {
            throw new IllegalStateException("history_budget_exceeded");
        }
        history.add(record);
        region.put("history", history);
        job.putBoolean("pendingWrite", true);
        region.put("job", job);
        store.update(region);
        if (!world.setBlockState(pos, after, Block.NOTIFY_LISTENERS | Block.FORCE_STATE)
                || !expectedAfter.equals(MaeGenerationEvidence.inspect(world,
                        MaeProtection.margin(area(job)), false, 8))) {
            throw new IllegalStateException("write_not_confirmed");
        }
        record.putString("result", "applied");
        history.set(history.size() - 1, record);
        if (job.contains("type")) {
            region.put("history", history);
            store.update(region);
            if (MaeRoadExecution.TYPE.equals(job.getString("type"))) {
                MaeRoadExecution.deliver(world, region.getUuid("colony"), record, pos, after);
            } else MaeTerrainExecution.deliver(world, region.getUuid("colony"), record, pos, after);
            history.set(history.size() - 1, record);
        }
        job.putBoolean("pendingWrite", false);
        job.putInt("cursor", cursor + 1);
        job.put("expectedSections", expectedAfter);
        NbtCompound owned = region.getCompound("ownedSections");
        for (String key : expectedAfter.getKeys()) {
            owned.put(key, expectedAfter.get(key).copy());
        }
        region.put("ownedSections", owned);
        if (cursor + 1 == plan.size()) {
            if (MaeRoadExecution.TYPE.equals(job.getString("type"))) MaeRoadExecution.finish(world, region, job, store);
            else if (job.contains("type")) MaeTerrainExecution.finish(world, job);
            job.putString("state", "complete");
        }
        region.put("job", job);
        store.update(region);
        return true;
    }

    private void validateJob(ServerWorld world, NbtCompound region, NbtCompound job) {
        MaeSectionLocks.check(world, store, region.getUuid("id"), MaeProtection.margin(area(job)));
        if (!job.containsUuid("id") || !job.contains("cursor", NbtElement.INT_TYPE)
                || !job.contains("plan", NbtElement.LIST_TYPE) || job.getBoolean("pendingWrite") || job.getBoolean("pendingPublication")) {
            throw new IllegalStateException("invalid_or_unconfirmed_job");
        }
        var plan = job.getList("plan", NbtElement.COMPOUND_TYPE);
        if (plan.isEmpty() || plan.size() > MAX_HISTORY || job.getInt("cursor") < 0
                || job.getInt("cursor") >= plan.size() || area(job).volume() > 4096
                || !area(region).contains(MaeProtection.margin(area(job)))) {
            throw new IllegalStateException("invalid_job_bounds_or_cursor");
        }
        if (job.contains("type")) {
            if (MaeRoadExecution.TYPE.equals(job.getString("type"))) MaeRoadExecution.validate(world, region, job);
            else if (MaeStructureExecution.is(job)) MaeStructureExecution.validate(world, region, job);
            else MaeTerrainExecution.validate(world, region, job);
        } else {
            if (region.contains("policy")) throw new IllegalStateException("typed_terrain_job_required");
            MaeProtection.inspect(world, region.getUuid("colony"), MaeProtection.margin(area(job)));
        }
        NbtCompound actual = MaeGenerationEvidence.inspect(world, MaeProtection.margin(area(job)), false, 8);
        if (!actual.equals(job.getCompound("expectedSections"))) {
            throw new IllegalStateException("world_changed_since_approval");
        }
    }

    static void validateEvidence(ServerWorld world, WorkArea inspection, NbtCompound actual, NbtCompound owned) {
        NbtCompound original = MaeGenerationEvidence.original(world, inspection);
        for (String key : actual.getKeys()) {
            NbtElement expected = owned.contains(key) ? owned.get(key) : original.get(key);
            if (!actual.get(key).equals(expected)) {
                throw new IllegalStateException("changed_generation_evidence");
            }
        }
    }

    /** Contexto de leitura não aprova trabalho; prova e proteções continuam obrigatórias — MAE Fase 2. */
    NbtCompound terrainContext(ServerWorld world, UUID id, WorkArea work) {
        return terrainContext(world, id, work, new NbtCompound());
    }

    NbtCompound roadContext(ServerWorld world, UUID id, WorkArea work, ColonyPos anchor) {
        return MaeRoadEvidence.capture(world, region(world, id), MaeProtection.margin(work), anchor);
    }

    NbtCompound terrainContext(ServerWorld world, UUID id, WorkArea work, NbtCompound readOnly) {
        NbtCompound region = region(world, id);
        MaeSectionLocks.check(world, store, id, MaeProtection.margin(work));
        if (!"terrain_v2".equals(region.getString("policy"))) {
            throw new IllegalStateException("terrain_policy_required");
        }
        if (work.volume() > 4096 || !area(region).contains(MaeProtection.margin(work))) {
            throw new IllegalStateException("outside_work_area");
        }
        if (region.contains("job") && !"complete".equals(region.getCompound("job").getString("state"))) {
            throw new IllegalStateException("unfinished_job");
        }
        MaeRoadEvidence.revalidate(world, region, readOnly);
        MaeProtection.inspectTerrain(world, region.getUuid("colony"), MaeProtection.margin(work), MaeRoadEvidence.floors(readOnly));
        NbtCompound actual = MaeGenerationEvidence.inspect(world, MaeProtection.margin(work), false, 8);
        validateEvidence(world, MaeProtection.margin(work), actual, region.getCompound("ownedSections"));
        return region;
    }

    private NbtCompound region(ServerWorld world, UUID id) {
        checkThread(world);
        NbtCompound region = store.region(id).orElseThrow(() -> new IllegalStateException("unknown_region"));
        validateRegion(region);
        if (!dimension(world).equals(region.getString("dimension"))) {
            throw new IllegalStateException("wrong_dimension");
        }
        return region;
    }

    private static void validateRegion(NbtCompound region) {
        if (region.contains("policy") && !"terrain_v2".equals(region.getString("policy"))) {
            throw new IllegalStateException("unknown_region_policy");
        }
        if (!region.containsUuid("id") || !region.containsUuid("colony")
                || !"generation_sections_sha256_v1".equals(region.getString("proof"))
                || region.getString("dimension").isEmpty() || !region.contains("history", NbtElement.LIST_TYPE)
                || !region.contains("ownedSections", NbtElement.COMPOUND_TYPE)
                || !region.contains("created", NbtElement.LONG_TYPE)
                || history(region).size() != ((NbtList) region.get("history")).size()
                || history(region).size() > MAX_HISTORY) {
            throw new IllegalStateException("invalid_region");
        }
        validateRegionSize(area(region));
    }

    private static void validateRegionSize(WorkArea area) {
        if (area.max().x() - area.min().x() > 127 || area.max().z() - area.min().z() > 127
                || area.volume() > 262_144) {
            throw new IllegalArgumentException("region_budget_exceeded");
        }
    }

    private static NbtList history(NbtCompound region) {
        return region.getList("history", NbtElement.COMPOUND_TYPE);
    }

    static WorkArea area(NbtCompound parent) {
        NbtCompound value = parent.getCompound("area");
        for (String key : List.of("minX", "minY", "minZ", "maxX", "maxY", "maxZ")) {
            if (!value.contains(key, NbtElement.INT_TYPE)) {
                throw new IllegalStateException("missing_area_coordinate");
            }
        }
        return new WorkArea(new ColonyPos(value.getInt("minX"), value.getInt("minY"), value.getInt("minZ")),
                new ColonyPos(value.getInt("maxX"), value.getInt("maxY"), value.getInt("maxZ")));
    }

    static NbtCompound encode(WorkArea area) {
        NbtCompound value = new NbtCompound();
        value.putInt("minX", area.min().x()); value.putInt("minY", area.min().y()); value.putInt("minZ", area.min().z());
        value.putInt("maxX", area.max().x()); value.putInt("maxY", area.max().y()); value.putInt("maxZ", area.max().z());
        return value;
    }

    static BlockState decodeState(NbtCompound value) {
        var id = net.minecraft.util.Identifier.tryParse(value.getString("Name"));
        if (!value.contains("Name", NbtElement.STRING_TYPE)
                || id == null || !Registries.BLOCK.containsId(id)) {
            throw new IllegalStateException("unknown_block_state");
        }
        BlockState state = NbtHelper.toBlockState(Registries.BLOCK.getReadOnlyWrapper(), value);
        if (!NbtHelper.fromBlockState(state).equals(value)) {
            throw new IllegalStateException("invalid_block_state_properties");
        }
        return state;
    }

    private static String dimension(ServerWorld world) {
        return world.getRegistryKey().getValue().toString();
    }

    private static void checkThread(ServerWorld world) {
        if (!world.getServer().isOnThread()) {
            throw new IllegalStateException("not_server_thread");
        }
    }
}
