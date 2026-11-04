package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.BlueprintAccess;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.WorkArea;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.fluid.FluidState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.EmptyBlockView;
import org.jspecify.annotations.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Prova local de circulação prevista ou real; não reserva nem autoriza escrita — ADR-046. */
public final class MaeStructureAccess {
    private MaeStructureAccess() { }

    /** Resultado transitório: o writer ainda precisa revalidar evidência, proteções e materiais. */
    public static final class Selected {
        private final UUID projectId;
        private final BlockPos opening;
        private final BlockPos support;
        private final Set<BlockPos> gates;
        private final ColonyPos goal;
        private final List<ColonyPos> circulation;

        Selected(UUID projectId, BlockPos opening, BlockPos support, Set<BlockPos> gates,
                 ColonyPos goal, List<ColonyPos> circulation) {
            this.projectId = projectId;
            this.opening = opening.toImmutable();
            this.support = support.toImmutable();
            this.gates = Set.copyOf(gates);
            this.goal = goal;
            this.circulation = List.copyOf(circulation);
        }

        public UUID projectId() { return projectId; }
        public BlockPos opening() { return opening; }
        public BlockPos support() { return support; }
        public Set<BlockPos> gates() { return gates; }
        /** Destino útil e circulação da prova; a conferência após a obra repete a mesma pergunta — ADR-046. */
        public ColonyPos goal() { return goal; }
        public List<ColonyPos> circulation() { return circulation; }
    }

    /** O chamador informa o destino útil; circulação não inclui água funcional, cultivos ou paredes. */
    public static Survey begin(ServerWorld world, ConstructionProject project,
                               List<MaeWorldWriter.Change> terrain, List<ColonyPos> circulation,
                               ColonyPos interiorGoal, boolean built) {
        return new Survey(world, project, terrain, circulation, interiorGoal, built);
    }

    /**
     * Caminho do jogo: circulação e destino derivados da planta e das superfícies preparadas.
     * O levantamento continua sendo a prova; a derivação só escolhe a pergunta — ADR-046 item 6.
     */
    public static Survey beginFor(ServerWorld world, ConstructionProject project,
                                  List<MaeWorldWriter.Change> terrain, List<ColonyPos> surfaces, boolean built) {
        List<ColonyPos> circulation = circulation(project, surfaces);
        return begin(world, project, terrain, circulation, interiorGoal(project, circulation), built);
    }

    /** Só a superfície fora da pegada é circulação; a de dentro vira piso da própria obra. */
    static List<ColonyPos> circulation(ConstructionProject project, List<ColonyPos> surfaces) {
        Building lot = Building.of(project);
        return surfaces.stream().filter(at -> at.x() < lot.min().x() || at.x() > lot.max().x()
                || at.z() < lot.min().z() || at.z() > lot.max().z()).distinct().toList();
    }

    /**
     * A primeira célula transitável para dentro de um vão declarado, pelas peças planejadas: porta e portão são
     * atravessados, não são destino. Primeiro os vãos cuja primeira célula fora da pegada está na circulação — a
     * mesma condição com que o levantamento aceita um acesso —, depois os demais, na ordem do levantamento. Sem nenhuma, a obra
     * não tem destino útil.
     */
    static ColonyPos interiorGoal(ConstructionProject project, List<ColonyPos> circulation) {
        Building lot = Building.of(project);
        Map<BlockPos, BlockState> planned = MaeStructurePieces.planned(project);
        BlockPos origin = MinecraftTypeAdapter.toBlockPos(project.origin());
        var outside = new java.util.HashSet<BlockPos>();
        circulation.forEach(at -> outside.add(MinecraftTypeAdapter.toBlockPos(at).up()));
        var accesses = sorted(project.blueprint().accesses()).stream().sorted(Comparator.comparing((BlueprintAccess access) -> {
            var first = access.opening().getFirst();
            BlockPos exit = origin.add(first.x(), first.y(), first.z());
            Direction direction = MinecraftTypeAdapter.toDirection(access.outward());
            for (int step = 0; step < 7 && (step == 0 || lot.contains(MinecraftTypeAdapter.toColonyPos(exit))); step++) {
                exit = exit.offset(direction);
            }
            return !outside.contains(exit);
        })).toList();
        for (BlueprintAccess access : accesses) {
            Direction inward = MinecraftTypeAdapter.toDirection(access.outward()).getOpposite();
            var first = access.opening().getFirst();
            BlockPos at = origin.add(first.x(), first.y(), first.z());
            for (int step = 0; step < 6; step++) {
                at = at.offset(inward);
                if (!lot.contains(MinecraftTypeAdapter.toColonyPos(at))) break;
                BlockState feet = planned.getOrDefault(at, Blocks.AIR.getDefaultState());
                if (interactive(feet)) continue;
                if (!passable(planned, at) || !passable(planned, at.up())) break;
                BlockState floor = planned.getOrDefault(at.down(), Blocks.AIR.getDefaultState());
                if (floor.getFluidState().isEmpty()
                        && !floor.getCollisionShape(EmptyBlockView.INSTANCE, at.down()).isEmpty()) {
                    return MinecraftTypeAdapter.toColonyPos(at);
                }
            }
        }
        throw new IllegalArgumentException("no_interior_goal");
    }

    private static boolean passable(Map<BlockPos, BlockState> planned, BlockPos at) {
        BlockState state = planned.getOrDefault(at, Blocks.AIR.getDefaultState());
        return state.getFluidState().isEmpty() && !(state.getBlock() instanceof CropBlock)
                && state.getCollisionShape(EmptyBlockView.INSTANCE, at).isEmpty();
    }

    static List<BlueprintAccess> sorted(List<BlueprintAccess> accesses) {
        return accesses.stream().sorted(Comparator
                .comparingInt((BlueprintAccess access) -> access.opening().getFirst().x())
                .thenComparingInt(access -> access.opening().getFirst().y())
                .thenComparingInt(access -> access.opening().getFirst().z())
                .thenComparing(access -> access.outward().name())).toList();
    }

    public static final class Survey {
        private final ServerWorld world;
        private final ConstructionProject project;
        private final Building lot;
        private final WorkArea area;
        private final NbtCompound evidence;
        private final Map<BlockPos, BlockState> sampled = new HashMap<>();
        private final Map<BlockPos, BlockState> overlay = new HashMap<>();
        private final Set<BlockPos> circulation;
        private final List<ColonyPos> circulationInput;
        private final BlockPos goal;
        private final List<BlueprintAccess> candidates;
        private final Iterator<BlockPos> positions;
        private final View view = new View();
        private final ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        private final Set<BlockPos> visited = new HashSet<>();
        private final Set<BlockPos> gates = new HashSet<>();
        private final Map<BlockPos, BlockPos> parents = new HashMap<>();
        private final ArrayDeque<BlockPos> routes = new ArrayDeque<>();
        private boolean tracing;
        private int candidate;
        private @Nullable BlockPos opening;
        private @Nullable BlockPos exterior;
        private @Nullable Selected result;
        private @Nullable String failure;

        private Survey(ServerWorld world, ConstructionProject project, List<MaeWorldWriter.Change> terrain,
                       List<ColonyPos> circulation, ColonyPos goal, boolean built) {
            MaeTerrainPreparation.budget(world);
            this.world = world;
            this.project = project;
            this.lot = Building.of(project);
            this.goal = MinecraftTypeAdapter.toBlockPos(goal);
            if (!inside(this.goal) || circulation.isEmpty()) throw new IllegalArgumentException("invalid_access_goal");
            this.circulationInput = List.copyOf(circulation);
            this.circulation = new HashSet<>();
            circulation.forEach(at -> this.circulation.add(MinecraftTypeAdapter.toBlockPos(at).up()));
            long minX = (long) lot.min().x() - 2, minY = (long) lot.min().y() - 1, minZ = (long) lot.min().z() - 2;
            long maxX = (long) lot.max().x() + 2, maxY = (long) lot.max().y() + 2, maxZ = (long) lot.max().z() + 2;
            for (BlockPos feet : this.circulation) {
                minX = Math.min(minX, (long) feet.getX() - 1); maxX = Math.max(maxX, (long) feet.getX() + 1);
                minY = Math.min(minY, (long) feet.getY() - 2); maxY = Math.max(maxY, (long) feet.getY() + 2);
                minZ = Math.min(minZ, (long) feet.getZ() - 1); maxZ = Math.max(maxZ, (long) feet.getZ() + 1);
            }
            long width = maxX - minX + 1, height = maxY - minY + 1, depth = maxZ - minZ + 1;
            if (width > 4096 || height > 4096 || depth > 4096 || width * height * depth > 4096
                    || minX < Integer.MIN_VALUE || minY < Integer.MIN_VALUE || minZ < Integer.MIN_VALUE
                    || maxX > Integer.MAX_VALUE || maxY > Integer.MAX_VALUE || maxZ > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("access_budget");
            }
            area = new WorkArea(new ColonyPos((int) minX, (int) minY, (int) minZ),
                    new ColonyPos((int) maxX, (int) maxY, (int) maxZ));
            evidence = MaeGenerationEvidence.inspect(world, area, false, 8);
            positions = BlockPos.iterate((int) minX, (int) minY, (int) minZ, (int) maxX, (int) maxY, (int) maxZ).iterator();
            candidates = sorted(project.blueprint().accesses());
            if (!built) {
                for (var change : terrain) {
                    if (!area.contains(MinecraftTypeAdapter.toColonyPos(change.position()))
                            || overlay.put(change.position(), change.after()) != null) {
                        throw new IllegalArgumentException("invalid_access_overlay");
                    }
                }
                overlay.putAll(MaeStructurePieces.planned(project));
            } else if (!terrain.isEmpty()) throw new IllegalArgumentException("actual_access_has_overlay");
        }

        public boolean complete() { return result != null || failure != null; }

        public Selected result() {
            if (failure != null) throw new IllegalStateException(failure);
            if (result == null) throw new IllegalStateException("access_survey_incomplete");
            return result;
        }

        /** Leituras e expansão local compartilham 256 unidades e 2 ms com o levantamento — ADR-045/046. */
        public int runSlice() {
            var budget = MaeTerrainPreparation.budget(world);
            if (complete() || budget.reads >= MaeTerrainPreparation.MAX_READS_PER_TICK
                    || budget.nanos >= MaeTerrainPreparation.SLICE_NANOS) return 0;
            revalidate();
            int reads = 0;
            long started = System.nanoTime();
            try {
                while (!complete() && budget.reads < MaeTerrainPreparation.MAX_READS_PER_TICK
                        && budget.nanos + System.nanoTime() - started < MaeTerrainPreparation.SLICE_NANOS) {
                    budget.reads++;
                    reads++;
                    if (positions.hasNext()) {
                        var at = positions.next().toImmutable();
                        sampled.put(at, world.getBlockState(at));
                    } else if (pending.isEmpty()) {
                        if (opening != null && visited.contains(goal) && visited.containsAll(circulation)) {
                            if (!tracing) {
                                tracing = true;
                                routes.add(goal);
                                routes.addAll(circulation);
                            } else if (!routes.isEmpty()) trace();
                            else {
                                revalidate();
                                result = new Selected(project.id(), opening, exterior.down(), gates,
                                        MinecraftTypeAdapter.toColonyPos(goal), circulationInput);
                            }
                        } else if (candidate >= candidates.size()) failure = "no_physical_structure_access";
                        else start(candidates.get(candidate++));
                    } else expand(pending.removeFirst());
                }
            } catch (IllegalStateException refused) {
                failure = refused.getMessage();
                throw refused;
            } finally {
                budget.nanos += System.nanoTime() - started;
            }
            return reads;
        }

        private void revalidate() {
            if (!evidence.equals(MaeGenerationEvidence.inspect(world, area, false, 8))) {
                throw new IllegalStateException("world_changed_during_access_survey");
            }
        }

        private void start(BlueprintAccess access) {
            opening = null;
            exterior = null;
            visited.clear();
            gates.clear();
            parents.clear();
            routes.clear();
            tracing = false;
            Direction outward = MinecraftTypeAdapter.toDirection(access.outward());
            for (ColonyPos relative : access.opening()) {
                BlockPos at = absolute(relative);
                if (!standable(at)) return;
            }
            for (ColonyPos relative : access.opening()) {
                BlockPos at = absolute(relative);
                BlockPos outside = at.offset(outward);
                if (!step(at, outside)) continue;
                // Uma porta interna não substitui a saída: a projeção exterior não atravessa outra porta.
                BlockPos trace = outside;
                int distance = 0;
                while (inside(trace) && distance++ < 6) {
                    if (interactive(view.getBlockState(trace)) || !step(trace, trace.offset(outward))) break;
                    trace = trace.offset(outward);
                }
                // A circulação começa na primeira célula fora da pegada; alpendre dentro dela é atravessado.
                if (inside(trace) || !circulation.contains(trace)) continue;
                opening = at;
                exterior = trace;
                visited.add(at);
                visited.add(outside);
                parents.put(at, at);
                parents.put(outside, at);
                pending.add(at);
                pending.add(outside);
                return;
            }
        }

        private void expand(BlockPos from) {
            for (Direction direction : Direction.Type.HORIZONTAL) for (int dy = -1; dy <= 1; dy++) {
                BlockPos to = from.offset(direction).up(dy);
                if (!visited.contains(to) && area.contains(MinecraftTypeAdapter.toColonyPos(to))
                        && (inside(to) || circulation.contains(to)) && step(from, to)) {
                    visited.add(to);
                    parents.put(to, from);
                    pending.addLast(to);
                }
            }
        }

        private void trace() {
            var at = routes.removeFirst();
            var parent = parents.remove(at);
            if (parent == null) return;
            if (view.getBlockState(at).getBlock() instanceof FenceGateBlock) gates.add(at);
            if (!parent.equals(at)) routes.addLast(parent);
        }

        private boolean step(BlockPos from, BlockPos to) {
            if (!standable(from) || !standable(to)) return false;
            Box first = body(from), last = body(to);
            double rise = last.minY - first.minY;
            if (Math.abs(rise) > 1.00001) return false;
            double top = Math.max(first.minY, last.minY);
            return clear(first.stretch(0, top - first.minY, 0))
                    && clear(last.stretch(0, top - last.minY, 0))
                    && clear(first.offset(0, top - first.minY, 0).union(last.offset(0, top - last.minY, 0)));
        }

        private boolean standable(BlockPos at) {
            if (!sampled.containsKey(at.down()) || !sampled.containsKey(at.up(2))) return false;
            BlockState floor = view.getBlockState(at.down());
            if (!floor.getFluidState().isEmpty() || floor.getCollisionShape(view, at.down()).isEmpty()) return false;
            return clear(body(at));
        }

        private Box body(BlockPos at) {
            double floorY = at.getY() - 1 + view.getBlockState(at.down()).getCollisionShape(view, at.down()).getMax(Direction.Axis.Y);
            var size = EntityType.VILLAGER.getDimensions();
            double half = size.width() / 2.0;
            return new Box(at.getX() + .5 - half, floorY + .00001, at.getZ() + .5 - half,
                    at.getX() + .5 + half, floorY + size.height(), at.getZ() + .5 + half);
        }

        private boolean clear(Box box) {
            for (BlockPos at : BlockPos.iterate(BlockPos.ofFloored(box.minX, box.minY, box.minZ),
                    BlockPos.ofFloored(box.maxX, box.maxY, box.maxZ))) {
                if (!sampled.containsKey(at)) return false;
                BlockState state = view.getBlockState(at);
                if (!state.getFluidState().isEmpty() || state.getBlock() instanceof CropBlock) return false;
                if (state.getBlock() instanceof DoorBlock && DoorBlock.canOpenByHand(state)) state = state.with(Properties.OPEN, true);
                else if (state.getBlock() instanceof FenceGateBlock) state = state.with(Properties.OPEN, true);
                if (VoxelShapes.matchesAnywhere(state.getCollisionShape(view, at).offset(at.getX(), at.getY(), at.getZ()),
                        VoxelShapes.cuboid(box), BooleanBiFunction.AND)) return false;
            }
            return true;
        }

        private boolean inside(BlockPos at) {
            return at.getX() >= lot.min().x() && at.getX() <= lot.max().x()
                    && at.getZ() >= lot.min().z() && at.getZ() <= lot.max().z();
        }

        private BlockPos absolute(ColonyPos relative) {
            return MinecraftTypeAdapter.toBlockPos(project.origin()).add(relative.x(), relative.y(), relative.z());
        }

        private final class View implements BlockView {
            @Override public @Nullable BlockEntity getBlockEntity(BlockPos pos) { return null; }
            @Override public BlockState getBlockState(BlockPos pos) {
                return overlay.getOrDefault(pos, sampled.getOrDefault(pos, Blocks.BARRIER.getDefaultState()));
            }
            @Override public FluidState getFluidState(BlockPos pos) { return getBlockState(pos).getFluidState(); }
            @Override public int getHeight() { return world.getHeight(); }
            @Override public int getBottomY() { return world.getBottomY(); }
        }
    }

    private static boolean interactive(BlockState state) {
        return state.getBlock() instanceof DoorBlock || state.getBlock() instanceof FenceGateBlock;
    }
}
