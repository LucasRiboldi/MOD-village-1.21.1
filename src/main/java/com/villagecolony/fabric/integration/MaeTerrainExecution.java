package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.WorkArea;
import com.villagecolony.core.coordination.SupplyException;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Contrato de terreno e efeitos materiais do writer; nenhuma execução independente — MAE Fase 2. */
final class MaeTerrainExecution {
    static final String TYPE = "terrain_v2";

    private MaeTerrainExecution() { }

    static NbtCompound job(ServerWorld world, MaeTerrainPreparation.Prepared prepared) {
        NbtCompound job = new NbtCompound();
        job.putString("type", TYPE);
        job.putInt("contract", 1);
        job.putUuid("id", prepared.lot().id());
        job.putString("blueprint", prepared.lot().blueprint().toString());
        job.put("area", MaeWorldWriter.encode(prepared.work()));
        job.put("footprint", MaeWorldWriter.encode(new WorkArea(prepared.lot().min(), prepared.lot().max())));
        job.put("expectedSections", prepared.evidence());
        job.put("readOnly", prepared.readOnly());
        var candidate = java.util.Objects.requireNonNull(prepared.terrain().candidate());
        job.putLong("entry", MinecraftTypeAdapter.toBlockPos(candidate.entry()).asLong());
        if (candidate.anchor() != null) job.putLong("anchor", MinecraftTypeAdapter.toBlockPos(candidate.anchor()).asLong());
        NbtList surfaces = new NbtList();
        for (var column : candidate.columns()) {
            NbtCompound surface = new NbtCompound();
            surface.putLong("position", MinecraftTypeAdapter.toBlockPos(column.surface()).asLong());
            surfaces.add(surface);
        }
        job.put("surfaces", surfaces);
        NbtList plan = new NbtList();
        for (var change : prepared.changes()) {
            if (!prepared.work().contains(MinecraftTypeAdapter.toColonyPos(change.position()))
                    || !world.getBlockState(change.position()).equals(change.before())
                    || !MaeProtection.terrain(change.after()) || change.before().equals(change.after())) {
                throw new IllegalStateException("invalid_terrain_change");
            }
            NbtCompound step = step(world, change.position(), change.before(), change.after(),
                    MaeProtection.vegetation(change.before()) ? "plant" : change.after().isAir() ? "cut" : "fill");
            plan.add(step);
        }
        job.put("plan", plan);
        job.putInt("cursor", 0);
        job.putString("state", "approved");
        return job;
    }

    static NbtCompound step(ServerWorld world, BlockPos at, BlockState before, BlockState after, String purpose) {
        NbtCompound step = new NbtCompound();
        step.putLong("position", at.asLong());
        step.put("before", net.minecraft.nbt.NbtHelper.fromBlockState(before));
        step.put("after", net.minecraft.nbt.NbtHelper.fromBlockState(after));
        step.putString("purpose", purpose);
        // O sorteio é fixado na aprovação; retomar nunca consulta o loot novamente — Regras 110/111.
        NbtList loot = new NbtList();
        if (!before.isAir()) {
            Map<Item, Integer> taken = new HashMap<>();
            for (ItemStack stack : Block.getDroppedStacks(before, world, at, null,
                    null, new ItemStack(Items.IRON_PICKAXE))) {
                taken.merge(stack.getItem(), stack.getCount(), Math::addExact);
            }
            taken.forEach((item, count) -> {
                NbtCompound entry = new NbtCompound();
                entry.putString("item", Registries.ITEM.getId(item).toString());
                entry.putInt("count", count);
                loot.add(entry);
            });
        }
        step.put("loot", loot);
        return step;
    }

    static void validate(ServerWorld world, NbtCompound region, NbtCompound job) {
        if (!TYPE.equals(job.getString("type")) || !TYPE.equals(region.getString("policy"))
                || job.getInt("contract") != 1 || !job.contains("surfaces", NbtElement.LIST_TYPE)) {
            throw new IllegalStateException("unknown_terrain_contract");
        }
        var surfaces = surfaces(job);
        MaeRoadEvidence.revalidate(world, region, job.getCompound("readOnly"));
        MaeProtection.inspectTerrain(world, region.getUuid("colony"), MaeProtection.margin(MaeWorldWriter.area(job)),
                MaeRoadEvidence.floors(job.getCompound("readOnly")));
        for (var step : job.getList("plan", NbtElement.COMPOUND_TYPE)) {
            BlockPos at = BlockPos.fromLong(((NbtCompound) step).getLong("position"));
            if (surfaces.stream().noneMatch(surface -> surface.getX() == at.getX() && surface.getZ() == at.getZ())) {
                throw new IllegalStateException("outside_prepared_surface");
            }
            if (MaeRoadEvidence.floors(job.getCompound("readOnly")).stream()
                    .anyMatch(road -> road.getX() == at.getX() && road.getZ() == at.getZ())) {
                throw new IllegalStateException("anchor_write");
            }
        }
    }

    static void intent(NbtCompound step, NbtCompound record) {
        if (!List.of("plant", "cut", "fill", "pave").contains(step.getString("purpose"))
                || !step.contains("loot", NbtElement.LIST_TYPE)) {
            throw new IllegalStateException("invalid_material_contract");
        }
        loot(step);
        record.putString("materials", "pending");
    }

    static void deliver(ServerWorld world, UUID colony, NbtCompound record, BlockPos position, BlockState after) {
        deliverLoot(world, colony, record, position);
        if (!after.isAir()) {
            Supplies.record(SupplyException.FOUNDATION_GROUND, colony, Registries.BLOCK.getId(after.getBlock()).toString(), 1);
        }
        record.putString("materials", "confirmed");
    }

    static void deliverLoot(ServerWorld world, UUID colony, NbtCompound record, BlockPos position) {
        Cuttings.keep(world, colony, position, loot(record));
    }

    private static Map<Item, Integer> loot(NbtCompound step) {
        NbtList list = step.getList("loot", NbtElement.COMPOUND_TYPE);
        if (list.size() != ((NbtList) step.get("loot")).size() || list.size() > 16) {
            throw new IllegalStateException("invalid_material_loot");
        }
        Map<Item, Integer> result = new HashMap<>();
        for (var element : list) {
            NbtCompound entry = (NbtCompound) element;
            var id = net.minecraft.util.Identifier.tryParse(entry.getString("item"));
            if (id == null || !Registries.ITEM.containsId(id) || entry.getInt("count") <= 0 || entry.getInt("count") > 64) {
                throw new IllegalStateException("invalid_material_loot");
            }
            result.merge(Registries.ITEM.get(id), entry.getInt("count"), Math::addExact);
        }
        return result;
    }

    static void finish(ServerWorld world, NbtCompound job) {
        WorkArea work = MaeWorldWriter.area(job);
        var surfaces = surfaces(job);
        for (BlockPos at : surfaces) {
            for (int layer = 0; layer <= 2; layer++) {
                BlockPos support = at.down(layer);
                BlockState state = world.getBlockState(support);
                if (!MaeProtection.terrain(state) || !state.isFullCube(world, support)) {
                    throw new IllegalStateException("final_support");
                }
            }
            for (int height = 1; height <= 2; height++) {
                BlockPos head = at.up(height);
                BlockState state = world.getBlockState(head);
                if (!state.getFluidState().isEmpty() || !state.getCollisionShape(world, head).isEmpty()) {
                    throw new IllegalStateException("final_clearance");
                }
            }
        }
        // Toda célula, inclusive bordas e degraus, precisa compartilhar acesso físico — desenho MAE Fase 2.
        var circulation = new java.util.HashSet<>(surfaces);
        if (job.contains("anchor", NbtElement.LONG_TYPE)) {
            BlockPos anchor = BlockPos.fromLong(job.getLong("anchor"));
            if (!job.getCompound("readOnly").contains("anchor", NbtElement.LONG_TYPE)
                    || job.getCompound("readOnly").getLong("anchor") != anchor.asLong()) {
                throw new IllegalStateException("invalid_final_anchor");
            }
            MaeRoadExecution.checkSurface(world, anchor, true);
            circulation.add(anchor);
        }
        var reached = new java.util.HashSet<BlockPos>();
        var pending = new java.util.ArrayDeque<BlockPos>();
        BlockPos entry = BlockPos.fromLong(job.getLong("entry"));
        reached.add(entry); pending.add(entry);
        while (!pending.isEmpty()) {
            BlockPos from = pending.removeFirst();
            for (var side : net.minecraft.util.math.Direction.Type.HORIZONTAL) for (int dy = -1; dy <= 1; dy++) {
                BlockPos to = from.offset(side).up(dy);
                if (!circulation.contains(to) || reached.contains(to)) continue;
                BlockPos extraHead = (dy < 0 ? to : from).up(3);
                BlockState head = world.getBlockState(extraHead);
                if (dy != 0 && (!head.getFluidState().isEmpty() || !head.getCollisionShape(world, extraHead).isEmpty())) continue;
                reached.add(to); pending.add(to);
            }
        }
        if (reached.size() != circulation.size()) throw new IllegalStateException("final_disconnected_surface");
        WorkArea inspection = MaeProtection.margin(work);
        for (BlockPos at : BlockPos.iterate(inspection.min().x(), inspection.min().y(), inspection.min().z(),
                inspection.max().x(), inspection.max().y(), inspection.max().z())) {
            BlockState state = world.getBlockState(at);
            if (MaeProtection.vegetation(state) && !state.canPlaceAt(world, at)) {
                throw new IllegalStateException("unsupported_remaining_plant");
            }
        }
    }

    private static java.util.Set<BlockPos> surfaces(NbtCompound job) {
        if (!job.contains("surfaces", NbtElement.LIST_TYPE)) throw new IllegalStateException("invalid_terrain_surface");
        var list = job.getList("surfaces", NbtElement.COMPOUND_TYPE);
        if (list.isEmpty() || list.size() > 4096 || list.size() != ((NbtList) job.get("surfaces")).size()) {
            throw new IllegalStateException("invalid_terrain_surface");
        }
        var positions = new java.util.HashSet<BlockPos>();
        var columns = new java.util.HashSet<Long>();
        WorkArea work = MaeWorldWriter.area(job);
        for (var element : list) {
            var value = (NbtCompound) element;
            BlockPos at = BlockPos.fromLong(value.getLong("position"));
            if (!value.contains("position", NbtElement.LONG_TYPE) || !positions.add(at)
                    || !columns.add(com.villagecolony.core.construction.model.ColonyRoads.column(at.getX(), at.getZ()))
                    || !work.contains(MinecraftTypeAdapter.toColonyPos(at))) throw new IllegalStateException("invalid_terrain_surface");
        }
        if (!job.contains("entry", NbtElement.LONG_TYPE) || !positions.contains(BlockPos.fromLong(job.getLong("entry")))) {
            throw new IllegalStateException("invalid_terrain_entry");
        }
        return positions;
    }
}
