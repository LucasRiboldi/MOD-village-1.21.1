package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.WorkArea;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.data.save.ColonySavedData;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/** Legado não altera seções supervisionadas nem adota trabalho incerto — ADR-044, MAE Fase 2. */
public final class MaeRoadAccess {
    private MaeRoadAccess() { }

    public static boolean legacyAllowed(ServerWorld world, WorkArea work) {
        var store = ColonySavedData.get(world.getServer()).maeWork();
        if (!store.supported()) return false;
        try {
            for (NbtCompound region : store.regions()) {
                WorkArea reserved = validatedArea(region);
                if (MaeHandover.isHandedOver(region)) continue;
                if (world.getRegistryKey().getValue().toString().equals(region.getString("dimension"))
                        && (work.min().x() >> 4) <= (reserved.max().x() >> 4)
                        && (work.max().x() >> 4) >= (reserved.min().x() >> 4)
                        && (work.min().y() >> 4) <= (reserved.max().y() >> 4)
                        && (work.max().y() >> 4) >= (reserved.min().y() >> 4)
                        && (work.min().z() >> 4) <= (reserved.max().z() >> 4)
                        && (work.max().z() >> 4) >= (reserved.min().z() >> 4)) {
                    return false;
                }
            }
            return true;
        } catch (IllegalArgumentException | IllegalStateException malformed) {
            return false;
        }
    }

    public static boolean legacyAllowed(ServerWorld world, BlockPos min, BlockPos max) {
        try {
            return legacyAllowed(world, new WorkArea(MinecraftTypeAdapter.toColonyPos(min),
                    MinecraftTypeAdapter.toColonyPos(max)));
        } catch (IllegalArgumentException malformed) {
            return false;
        }
    }

    /** Toda a operação é verificada antes da primeira escrita, inclusive margem e clearance. */
    public static boolean legacyAllowed(ServerWorld world, List<BlockPos> cells, int margin, int clearance) {
        if (cells.isEmpty()) return true;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos at : cells) {
            minX = Math.min(minX, at.getX()); minY = Math.min(minY, at.getY()); minZ = Math.min(minZ, at.getZ());
            maxX = Math.max(maxX, at.getX()); maxY = Math.max(maxY, at.getY()); maxZ = Math.max(maxZ, at.getZ());
        }
        return legacyAllowed(world, new BlockPos(minX - margin, minY, minZ - margin),
                new BlockPos(maxX + margin, Math.min(world.getTopY() - 1, maxY + clearance), maxZ + margin));
    }

    /** Sem filtro espacial nos produtores legados, qualquer reserva não entregue da dimensão adia a redescoberta — ADR-045 item 10, Regra 187. */
    public static boolean metadataAllowed(ServerWorld world, UUID colonyId) {
        var store = ColonySavedData.get(world.getServer()).maeWork();
        if (!store.supported()) return false;
        try {
            for (NbtCompound region : store.regions()) {
                validatedArea(region);
                if (!MaeHandover.isHandedOver(region)
                        && world.getRegistryKey().getValue().toString().equals(region.getString("dimension"))) return false;
            }
            return true;
        } catch (IllegalArgumentException | IllegalStateException malformed) {
            return false;
        }
    }

    private static WorkArea validatedArea(NbtCompound region) {
        if (!region.containsUuid("id") || !region.containsUuid("colony")
                || !"generation_sections_sha256_v1".equals(region.getString("proof"))
                || region.getString("dimension").isEmpty() || Identifier.tryParse(region.getString("dimension")) == null
                || !region.contains("created", NbtElement.LONG_TYPE)
                || !region.contains("ownedSections", NbtElement.COMPOUND_TYPE)
                || !region.contains("history", NbtElement.LIST_TYPE)
                || region.getList("history", NbtElement.COMPOUND_TYPE).size() != ((NbtList) region.get("history")).size()
                || region.getList("history", NbtElement.COMPOUND_TYPE).size() > 1024
                || (region.contains("policy") && !"terrain_v2".equals(region.getString("policy")))) {
            throw new IllegalStateException("invalid_region");
        }
        WorkArea area = MaeWorldWriter.area(region);
        if (area.max().x() - area.min().x() > 127 || area.max().z() - area.min().z() > 127
                || area.volume() > 262_144) throw new IllegalStateException("region_budget_exceeded");
        return area;
    }
}
