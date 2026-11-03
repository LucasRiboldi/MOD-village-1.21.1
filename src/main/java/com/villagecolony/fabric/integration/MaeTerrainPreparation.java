package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.TerrainPlan;
import com.villagecolony.core.construction.model.TerrainPlanner;
import com.villagecolony.core.construction.model.WorkArea;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EmptyBlockView;
import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.WeakHashMap;

/** Levantamento explícito, sem eventos de execução ou alteração do mundo — MAE Fase 2. */
public final class MaeTerrainPreparation {
    public static final int MAX_READS_PER_TICK = 256;
    private static final long SLICE_NANOS = 2_000_000;
    private static final Map<ServerWorld, Budget> BUDGETS = new WeakHashMap<>();

    static {
        ServerMemory.register(MaeTerrainPreparation.class, MaeTerrainPreparation::clearAll);
    }

    private MaeTerrainPreparation() { }

    public static void clearAll() {
        BUDGETS.clear();
    }

    private static final class Budget {
        int tick = -1;
        int reads;
        long nanos;
    }

    /** Superfícies e corredor vêm do chamador; o levantamento não cria outro grafo — MAE Fase 2. */
    public record Proposal(Building lot, List<ColonyPos> surfaces, ColonyPos entry,
                           @Nullable ColonyPos anchor, List<ColonyPos> corridor, int pathLength) {
        public Proposal(Building lot, List<ColonyPos> surfaces, ColonyPos entry,
                        @Nullable ColonyPos anchor, int pathLength) {
            this(lot, surfaces, entry, anchor, List.of(), pathLength);
        }

        public Proposal {
            Objects.requireNonNull(lot);
            surfaces = List.copyOf(surfaces);
            corridor = List.copyOf(corridor);
            Objects.requireNonNull(entry);
            if (surfaces.isEmpty() || surfaces.size() > 4096 || pathLength < 0) {
                throw new IllegalArgumentException("invalid_surface_proposal");
            }
        }
    }

    /** Resultado só nasce de amostra verificada; o Building original não é ampliado para autorizar escrita. */
    public static final class Prepared {
        private final UUID regionId;
        private final Building lot;
        private final WorkArea work;
        private final TerrainPlan terrain;
        private final List<MaeWorldWriter.Change> changes;
        private final NbtCompound evidence;
        private final NbtCompound readOnly;

        private Prepared(UUID regionId, Proposal proposal, WorkArea work, TerrainPlan terrain,
                         List<MaeWorldWriter.Change> changes, NbtCompound evidence, NbtCompound readOnly) {
            this.regionId = regionId;
            this.lot = proposal.lot();
            this.work = work;
            this.terrain = terrain;
            this.changes = List.copyOf(changes);
            this.evidence = evidence.copy();
            this.readOnly = readOnly.copy();
        }

        public UUID regionId() { return regionId; }
        public Building lot() { return lot; }
        public WorkArea work() { return work; }
        public TerrainPlan terrain() { return terrain; }
        public List<MaeWorldWriter.Change> changes() { return changes; }
        NbtCompound evidence() { return evidence.copy(); }
        NbtCompound readOnly() { return readOnly.copy(); }
    }

    public static Survey begin(ServerWorld world, MaeWorldWriter writer, UUID regionId,
                               List<Proposal> proposals, ColonyPos center) {
        return new Survey(world, writer, regionId, proposals, center);
    }

    /** Cada fatia revalida hashes antes de continuar; não carrega chunks nem reserva/aprova implicitamente. */
    public static final class Survey {
        private final ServerWorld world;
        private final UUID regionId;
        private final List<Proposal> proposals;
        private final ColonyPos center;
        private final WorkArea authorized;
        private final List<Sample> samples = new ArrayList<>();
        private final Map<TerrainPlanner.Candidate, Prepared> evaluated = new HashMap<>();
        private int current;
        private @Nullable Prepared result;
        private @Nullable String failure;
        private String candidateRefusal = "no_feasible_candidate";

        private Survey(ServerWorld world, MaeWorldWriter writer, UUID regionId,
                       List<Proposal> proposals, ColonyPos center) {
            checkThread(world);
            this.world = world;
            this.regionId = regionId;
            this.proposals = List.copyOf(proposals);
            this.center = Objects.requireNonNull(center);
            if (proposals.isEmpty() || proposals.size() > TerrainPlanner.MAX_CANDIDATES) {
                throw new IllegalArgumentException("candidate_budget");
            }
            WorkArea regionArea = null;
            for (Proposal proposal : this.proposals) {
                WorkArea work = bounds(proposal.surfaces());
                NbtCompound readOnly = proposal.anchor() == null ? new NbtCompound()
                        : writer.roadContext(world, regionId, work, proposal.anchor());
                NbtCompound region = writer.terrainContext(world, regionId, work, readOnly);
                if (!region.getUuid("colony").equals(proposal.lot().colonyId())) {
                    throw new IllegalStateException("wrong_colony");
                }
                regionArea = MaeWorldWriter.area(region);
                samples.add(new Sample(proposal, work,
                        MaeGenerationEvidence.inspect(world, MaeProtection.margin(work), false, 8), readOnly));
            }
            authorized = Objects.requireNonNull(regionArea);
        }

        public List<Proposal> proposals() { return proposals; }
        public boolean complete() { return result != null || failure != null; }

        public Prepared result() {
            if (failure != null) throw new IllegalStateException(failure);
            if (result == null) throw new IllegalStateException("survey_incomplete");
            return result;
        }

        public int runSlice() {
            checkThread(world);
            if (complete()) return 0;
            Budget budget = BUDGETS.computeIfAbsent(world, ignored -> new Budget());
            if (budget.tick != world.getServer().getTicks()) {
                budget.tick = world.getServer().getTicks();
                budget.reads = 0;
                budget.nanos = 0;
            }
            if (budget.reads >= MAX_READS_PER_TICK || budget.nanos >= SLICE_NANOS) return 0;
            int reads = 0;
            try {
                Sample sample = samples.get(current);
                // Hashes/proteções têm custo síncrono limitado próprio; não são leituras da amostra incremental.
                sample.revalidate(world);
                long started = System.nanoTime();
                try {
                    while (sample.positions.hasNext() && budget.reads < MAX_READS_PER_TICK
                            && budget.nanos + System.nanoTime() - started < SLICE_NANOS) {
                        BlockPos position = sample.positions.next().toImmutable();
                        sample.states.put(position, world.getBlockState(position));
                        budget.reads++;
                        reads++;
                    }
                    if (!sample.positions.hasNext()) {
                        sample.revalidate(world);
                        try {
                            evaluate(sample);
                        } catch (IllegalArgumentException | IllegalStateException rejected) {
                            candidateRefusal = rejected.getMessage() == null ? "candidate_refused" : rejected.getMessage();
                        }
                        if (++current == samples.size()) finish();
                    }
                } finally {
                    budget.nanos += System.nanoTime() - started;
                }
            } catch (IllegalArgumentException | IllegalStateException refused) {
                failure = refused.getMessage() == null ? "survey_refused" : refused.getMessage();
                throw refused;
            }
            return reads;
        }

        private void evaluate(Sample sample) {
            List<TerrainPlanner.Column> columns = new ArrayList<>();
            List<MaeWorldWriter.Change> changes = new ArrayList<>();
            for (ColonyPos surface : sample.proposal.surfaces()) {
                BlockPos at = MinecraftTypeAdapter.toBlockPos(surface);
                if (MaeRoadEvidence.floors(sample.readOnly).stream().anyMatch(road -> road.getX() == at.getX() && road.getZ() == at.getZ())) {
                    throw new IllegalStateException("anchor_write");
                }
                int natural = surface.y() + 4;
                while (natural >= surface.y() - 2 && !solid(sample.state(at.withY(natural)), at.withY(natural))) natural--;
                if (natural < surface.y() - 2 || Math.abs(natural - surface.y()) > 2) {
                    throw new IllegalStateException("cut_fill_limit");
                }
                BlockPos original = at.withY(natural);
                boolean openDepression = world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE,
                        at.getX(), at.getZ()) - 1 <= natural + (MaeProtection.vegetation(sample.state(original.up())) ? 1 : 0);
                int support = 0;
                while (support < 2 && solid(sample.state(original.down(support + 1)), original.down(support + 1))) support++;
                int plants = 0;
                int mergedPlants = 0;
                var plant = original.up();
                if (natural != surface.y() && MaeProtection.vegetation(sample.state(plant))) {
                    changes.add(new MaeWorldWriter.Change(plant, sample.state(plant), Blocks.AIR.getDefaultState()));
                    plants++;
                }
                if (natural > surface.y()) {
                    for (int y = natural; y > surface.y(); y--) {
                        BlockPos cut = at.withY(y);
                        changes.add(new MaeWorldWriter.Change(cut, sample.state(cut), Blocks.AIR.getDefaultState()));
                    }
                } else if (natural < surface.y()) {
                    BlockState fill = VillageBiomes.foundationGroundAt(world, at).getDefaultState();
                    if (!solid(fill, at)) throw new IllegalStateException("unsupported_biome_material");
                    for (int y = natural + 1; y <= surface.y(); y++) {
                        BlockPos place = at.withY(y);
                        if (!sample.state(place).isAir() && !MaeProtection.vegetation(sample.state(place))) {
                            throw new IllegalStateException("unsafe_fill");
                        }
                        // Planta já foi incluída antes da terraplanagem, sem duplicar a posição do aterro.
                        BlockState before = sample.state(place);
                        if (MaeProtection.vegetation(before)) {
                            changes.removeIf(change -> change.position().equals(place));
                            mergedPlants++;
                        }
                        changes.add(new MaeWorldWriter.Change(place, before, fill));
                    }
                }
                columns.add(new TerrainPlanner.Column(MinecraftTypeAdapter.toColonyPos(original), surface.y(), support,
                        2, openDepression, false, 0, plants, mergedPlants));
            }
            Map<BlockPos, BlockState> after = new HashMap<>();
            for (var change : changes) {
                if (after.put(change.position(), change.after()) != null) throw new IllegalStateException("duplicate_change");
            }
            for (ColonyPos surface : sample.proposal.surfaces()) {
                BlockPos at = MinecraftTypeAdapter.toBlockPos(surface);
                for (int layer = 0; layer <= 2; layer++) {
                    BlockPos support = at.down(layer);
                    if (!solid(after.getOrDefault(support, sample.state(support)), support)) {
                        throw new IllegalStateException("insufficient_support");
                    }
                }
                for (int height = 1; height <= 2; height++) {
                    BlockPos head = at.up(height);
                    if (!passable(after.getOrDefault(head, sample.state(head)), head)) {
                        throw new IllegalStateException("clearance");
                    }
                }
            }
            List<TerrainPlanner.Barrier> barriers = new ArrayList<>();
            Map<BlockPos, ColonyPos> byColumn = new HashMap<>();
            for (ColonyPos surface : sample.proposal.surfaces()) {
                byColumn.put(new BlockPos(surface.x(), 0, surface.z()), surface);
            }
            for (ColonyPos from : sample.proposal.surfaces()) {
                BlockPos column = new BlockPos(from.x(), 0, from.z());
                for (var side : net.minecraft.util.math.Direction.Type.HORIZONTAL) {
                    ColonyPos to = byColumn.get(column.offset(side));
                    if (to != null && to.y() == from.y() + 1) {
                        BlockPos head = MinecraftTypeAdapter.toBlockPos(from).up(3);
                        if (!passable(after.getOrDefault(head, sample.state(head)), head)) {
                            barriers.add(new TerrainPlanner.Barrier(from, to));
                        }
                    }
                }
            }
            Proposal proposal = sample.proposal;
            if (proposal.anchor() != null) {
                BlockPos anchor = MinecraftTypeAdapter.toBlockPos(proposal.anchor());
                for (ColonyPos from : proposal.surfaces()) {
                    if (Math.abs((long) from.x() - proposal.anchor().x()) + Math.abs((long) from.z() - proposal.anchor().z()) == 1
                            && Math.abs(from.y() - proposal.anchor().y()) == 1) {
                        BlockPos lower = from.y() < anchor.getY() ? MinecraftTypeAdapter.toBlockPos(from) : anchor;
                        BlockPos head = lower.up(3);
                        if (!passable(after.getOrDefault(head, sample.state(head)), head)) {
                            barriers.add(new TerrainPlanner.Barrier(from, proposal.anchor()));
                        }
                    }
                }
            }
            var candidate = new TerrainPlanner.Candidate(proposal.lot().min(),
                    new WorkArea(proposal.lot().min(), proposal.lot().max()), proposal.entry(), proposal.anchor(),
                    columns, barriers, proposal.corridor(), proposal.pathLength());
            TerrainPlan terrain = TerrainPlanner.plan(new TerrainPlanner.Surface(List.of(candidate)),
                    new TerrainPlanner.Request(authorized, center));
            if (terrain.feasible()) {
                changes.sort(java.util.Comparator.<MaeWorldWriter.Change>comparingInt(change ->
                        MaeProtection.vegetation(change.before()) ? 0 : change.after().isAir() ? 1 : 2)
                        .thenComparingInt(change -> change.after().isAir() ? -change.position().getY() : change.position().getY())
                        .thenComparingLong(change -> change.position().asLong()));
                evaluated.put(candidate, new Prepared(regionId, proposal, sample.work, terrain, changes, sample.evidence, sample.readOnly));
            } else candidateRefusal = terrain.reason();
        }

        private void finish() {
            for (Sample sample : samples) sample.revalidate(world);
            var best = TerrainPlanner.plan(new TerrainPlanner.Surface(List.copyOf(evaluated.keySet())),
                    new TerrainPlanner.Request(authorized, center));
            if (best.feasible()) {
                result = evaluated.get(best.candidate());
                failure = null;
            } else failure = candidateRefusal;
        }
    }

    private static final class Sample {
        final Proposal proposal;
        final WorkArea work;
        final NbtCompound evidence;
        final NbtCompound readOnly;
        final Map<BlockPos, BlockState> states = new LinkedHashMap<>();
        final Iterator<BlockPos> positions;

        Sample(Proposal proposal, WorkArea work, NbtCompound evidence, NbtCompound readOnly) {
            this.proposal = proposal;
            this.work = work;
            this.evidence = evidence;
            this.readOnly = readOnly;
            WorkArea inspection = MaeProtection.margin(work);
            positions = BlockPos.iterate(inspection.min().x(), inspection.min().y(), inspection.min().z(),
                    inspection.max().x(), inspection.max().y(), inspection.max().z()).iterator();
        }

        BlockState state(BlockPos pos) {
            BlockState state = states.get(pos);
            if (state == null) throw new IllegalStateException("outside_sample");
            return state;
        }

        void revalidate(ServerWorld world) {
            if (!evidence.equals(MaeGenerationEvidence.inspect(world, MaeProtection.margin(work), false, 8))) {
                throw new IllegalStateException("world_changed_during_survey");
            }
        }
    }

    private static WorkArea bounds(List<ColonyPos> surfaces) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (ColonyPos pos : surfaces) {
            minX = Math.min(minX, pos.x()); maxX = Math.max(maxX, pos.x());
            minY = Math.min(minY, pos.y() - 4); maxY = Math.max(maxY, pos.y() + 4);
            minZ = Math.min(minZ, pos.z()); maxZ = Math.max(maxZ, pos.z());
        }
        return new WorkArea(new ColonyPos(minX, minY, minZ), new ColonyPos(maxX, maxY, maxZ));
    }

    private static boolean solid(BlockState state, BlockPos at) {
        return !state.isAir() && MaeProtection.terrain(state) && state.isFullCube(EmptyBlockView.INSTANCE, at);
    }

    private static boolean passable(BlockState state, BlockPos at) {
        return state.getFluidState().isEmpty() && state.getCollisionShape(EmptyBlockView.INSTANCE, at).isEmpty();
    }

    private static void checkThread(ServerWorld world) {
        if (!world.getServer().isOnThread()) throw new IllegalStateException("not_server_thread");
    }
}
