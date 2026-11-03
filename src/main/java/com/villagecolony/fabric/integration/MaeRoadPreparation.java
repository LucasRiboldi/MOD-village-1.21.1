package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.WorkArea;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/** Aprovação explícita da rota existente, sem Building fictício nem agendamento — MAE Fase 2. */
public final class MaeRoadPreparation {
    private MaeRoadPreparation() { }

    public static final class Prepared {
        private final UUID region;
        private final WorkArea work;
        private final NbtCompound job;
        private Prepared(UUID region, WorkArea work, NbtCompound job) {
            this.region = region;
            this.work = work;
            this.job = job.copy();
        }
        UUID region() { return region; }
        WorkArea work() { return work; }
        NbtCompound job() { return job.copy(); }
    }

    /** A rota precisa estar nivelada; cortes/aterros exigem sua própria aprovação de terreno — MAE Fase 2. */
    public static Prepared prepare(ServerWorld world, MaeWorldWriter writer, UUID regionId,
                                   List<BlockPos> route, BlockPos hub) {
        return prepare(world, writer, regionId, route, hub, null);
    }

    /** Conexão certificada é explícita e somente lida; nunca substitui o destino aprovado — MAE Fase 2. */
    public static Prepared prepare(ServerWorld world, MaeWorldWriter writer, UUID regionId,
                                   List<BlockPos> route, BlockPos hub,
                                   @org.jspecify.annotations.Nullable BlockPos connection) {
        if (route.isEmpty() || route.size() > 4096 || new HashSet<>(route).size() != route.size()) {
            throw new IllegalArgumentException("invalid_road_route");
        }
        List<BlockPos> positions = route.stream().map(BlockPos::toImmutable).toList();
        WorkArea work = bounds(positions);
        NbtCompound readOnly = connection == null ? new NbtCompound() : writer.roadContext(world, regionId, work,
                MinecraftTypeAdapter.toColonyPos(connection));
        NbtCompound region = writer.terrainContext(world, regionId, work, readOnly);
        UUID colony = region.getUuid("colony");
        NbtCompound job = new NbtCompound();
        job.putString("type", MaeRoadExecution.TYPE);
        job.putInt("contract", 1);
        job.putUuid("id", UUID.randomUUID());
        job.put("area", MaeWorldWriter.encode(work));
        job.put("readOnly", readOnly);
        job.put("expectedSections", MaeGenerationEvidence.inspect(world, MaeProtection.margin(work), false, 8));
        job.putLong("hub", hub.asLong());
        job.putString("networkBefore", MaeRoadExecution.networkRevision(world, colony));
        NbtList surfaces = new NbtList();
        NbtList plan = new NbtList();
        for (int i = 0; i < positions.size(); i++) {
            BlockPos at = positions.get(i);
            MaeRoadExecution.requireWritable(at, readOnly);
            if (i > 0) {
                BlockPos previous = positions.get(i - 1);
                if (Math.abs((long) at.getX() - previous.getX()) + Math.abs((long) at.getZ() - previous.getZ()) != 1
                        || Math.abs((long) at.getY() - previous.getY()) > 1) {
                    throw new IllegalStateException("disconnected_road_route");
                }
            }
            MaeRoadExecution.checkSurface(world, at, false);
            if (!world.getBlockState(at.up()).isAir() || !world.getBlockState(at.up(2)).isAir()) {
                // Remoção de plantas exige mudança distinta, aprovada antes da pavimentação — desenho Fase 2.
                throw new IllegalStateException("road_clearance_requires_terrain");
            }
            BlockState before = world.getBlockState(at);
            BlockState after = StreetSurface.center(world, at, before);
            if (before.equals(after)) throw new IllegalStateException("uncertified_existing_road");
            NbtCompound step = MaeTerrainExecution.step(world, at, before, after, "pave");
            plan.add(step);
            NbtCompound surface = new NbtCompound();
            surface.putLong("position", at.asLong());
            surfaces.add(surface);
        }
        job.put("surfaces", surfaces);
        job.put("plan", plan);
        job.putInt("cursor", 0);
        job.putString("state", "approved");
        MaeRoadExecution.publicationGraph(world, colony, job);
        return new Prepared(regionId, work, job);
    }

    private static WorkArea bounds(List<BlockPos> route) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos at : route) {
            minX = Math.min(minX, at.getX()); maxX = Math.max(maxX, at.getX());
            minY = Math.min(minY, at.getY() - 2); maxY = Math.max(maxY, at.getY() + 3);
            minZ = Math.min(minZ, at.getZ()); maxZ = Math.max(maxZ, at.getZ());
        }
        return new WorkArea(MinecraftTypeAdapter.toColonyPos(new BlockPos(minX, minY, minZ)),
                MinecraftTypeAdapter.toColonyPos(new BlockPos(maxX, maxY, maxZ)));
    }
}
