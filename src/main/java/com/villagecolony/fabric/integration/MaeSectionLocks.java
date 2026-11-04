package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.WorkArea;
import com.villagecolony.data.save.MaeWorkStore;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.world.ServerWorld;
import java.util.UUID;

/** Exclusão derivada do diário persistente, inclusive pausas e falhas incertas — MAE Fase 2. */
final class MaeSectionLocks {
    private MaeSectionLocks() { }

    static void check(ServerWorld world, MaeWorkStore store, UUID regionId, WorkArea inspection) {
        for (NbtCompound other : store.regions()) {
            if (!other.containsUuid("id") || other.getString("dimension").isEmpty()
                    || net.minecraft.util.Identifier.tryParse(other.getString("dimension")) == null) {
                throw new IllegalStateException("invalid_region");
            }
            if (regionId.equals(other.getUuid("id"))
                    || !world.getRegistryKey().getValue().toString().equals(other.getString("dimension"))) continue;
            if (!other.contains("job")) continue;
            if (!other.contains("job", NbtElement.COMPOUND_TYPE)) throw new IllegalStateException("invalid_job");
            NbtCompound job = other.getCompound("job");
            boolean known = !job.contains("type") ? !other.contains("policy")
                    : (job.getInt("contract") == 1 || (job.getInt("contract") == 2
                            && MaeStructureExecution.TYPE.equals(job.getString("type"))))
                    && (MaeTerrainExecution.TYPE.equals(job.getString("type"))
                    || MaeRoadExecution.TYPE.equals(job.getString("type"))
                    || MaeStructureExecution.TYPE.equals(job.getString("type")));
            var plan = job.getList("plan", NbtElement.COMPOUND_TYPE);
            if (known && !plan.isEmpty() && plan.size() <= 1024
                    && plan.size() == job.getInt("cursor") && "complete".equals(job.getString("state"))
                    && !job.getBoolean("pendingWrite") && !job.getBoolean("pendingPublication")) continue;
            WorkArea occupied = MaeProtection.margin(MaeWorldWriter.area(job));
            if ((inspection.min().x() >> 4) <= (occupied.max().x() >> 4)
                    && (inspection.max().x() >> 4) >= (occupied.min().x() >> 4)
                    && (inspection.min().y() >> 4) <= (occupied.max().y() >> 4)
                    && (inspection.max().y() >> 4) >= (occupied.min().y() >> 4)
                    && (inspection.min().z() >> 4) <= (occupied.max().z() >> 4)
                    && (inspection.max().z() >> 4) >= (occupied.min().z() >> 4)) {
                throw new IllegalStateException("shared_section_busy");
            }
        }
    }
}
