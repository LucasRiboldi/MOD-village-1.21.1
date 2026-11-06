package com.villagecolony.data.save;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Reservas, aprovações e intenções MAE no arquivo da colônia; futuro permanece opaco — ADR-044. */
public final class MaeWorkStore {
    public static final String KEY = "maeWork";
    private NbtElement payload;
    private final Runnable changed;

    public MaeWorkStore(Runnable changed) {
        this.changed = changed;
    }

    public boolean supported() {
        return payload == null || (payload instanceof NbtCompound root
                && root.contains("version", NbtElement.INT_TYPE)
                && root.getInt("version") >= 1 && root.getInt("version") <= 3
                && root.contains("regions", NbtElement.LIST_TYPE)
                && root.getList("regions", NbtElement.COMPOUND_TYPE).size() == ((NbtList) root.get("regions")).size());
    }

    public List<NbtCompound> regions() {
        return entries().stream().map(entry -> ((NbtCompound) entry).copy()).toList();
    }

    private NbtList entries() {
        if (!supported()) {
            throw new IllegalStateException("unsupported MAE work payload");
        }
        return payload == null ? new NbtList() : ((NbtCompound) payload).getList("regions", NbtElement.COMPOUND_TYPE);
    }

    public Optional<NbtCompound> region(UUID id) {
        return entries().stream().map(entry -> (NbtCompound) entry)
                .filter(entry -> entry.containsUuid("id") && id.equals(entry.getUuid("id")))
                .findFirst().map(NbtCompound::copy);
    }

    public void update(NbtCompound region) {
        if (!supported()) {
            throw new IllegalStateException("unsupported MAE work payload");
        }
        if (region.contains("policy") && (payload == null || ((NbtCompound) payload).getInt("version") < 2)) {
            throw new IllegalStateException("terrain_reservation_required");
        }
        if (region.getCompound("job").getString("type").equals("structure_v3")
                && (payload == null || ((NbtCompound) payload).getInt("version") < 3)) {
            throw new IllegalStateException("structure_approval_required");
        }
        update(region, 1);
    }

    /** Somente uma reserva nova explicitamente aceita promove o envelope — desenho MAE Fase 2. */
    public void reserveTerrain(NbtCompound region) {
        if (!"terrain_v2".equals(region.getString("policy"))) {
            throw new IllegalArgumentException("unknown_terrain_policy");
        }
        if (!region.containsUuid("id") || region(region.getUuid("id")).isPresent()) {
            throw new IllegalStateException("new_terrain_reservation_required");
        }
        update(region, 2);
    }

    /** A primeira obra structure_v3 promove o envelope; leitores até v2 passam a vê-lo opaco — ADR-046 P1. */
    public void approveStructure(NbtCompound region) {
        if (!"terrain_v2".equals(region.getString("policy"))
                || !"structure_v3".equals(region.getCompound("job").getString("type"))) {
            throw new IllegalArgumentException("unknown_structure_contract");
        }
        if (!region.containsUuid("id") || region(region.getUuid("id")).isEmpty()) {
            throw new IllegalStateException("terrain_reservation_required");
        }
        update(region, 3);
    }

    private void update(NbtCompound region, int version) {
        if (!region.containsUuid("id")) {
            throw new IllegalArgumentException("MAE region missing identity");
        }
        var entries = regions();
        UUID id = region.getUuid("id");
        if (entries.size() >= 128 && region(id).isEmpty()) {
            throw new IllegalStateException("MAE reservation limit reached");
        }
        NbtList list = new NbtList();
        for (NbtCompound entry : entries) {
            if (!entry.containsUuid("id") || !id.equals(entry.getUuid("id"))) {
                list.add(entry);
            }
        }
        list.add(region.copy());
        NbtCompound root = payload == null ? new NbtCompound() : ((NbtCompound) payload).copy();
        root.putInt("version", Math.max(version, root.contains("version") ? root.getInt("version") : 1));
        root.put("regions", list);
        payload = root;
        changed.run();
    }

    /** As obras pedidas, na ordem da fila — Regra 213. */
    public List<NbtCompound> requests() {
        if (!(payload instanceof NbtCompound root)) {
            return List.of();
        }

        return root.getList("requests", NbtElement.COMPOUND_TYPE).stream().map(entry -> ((NbtCompound) entry).copy()).toList();
    }

    public void setRequests(List<NbtCompound> requests) {
        if (!supported()) {
            throw new IllegalStateException("unsupported MAE work payload");
        }

        NbtCompound root = payload == null ? new NbtCompound() : ((NbtCompound) payload).copy();
        NbtList list = new NbtList();

        list.addAll(requests);

        if (!root.contains("version", NbtElement.INT_TYPE)) {
            root.putInt("version", 1);
            root.put("regions", new NbtList());
        }

        root.put("requests", list);
        payload = root;
        changed.run();
    }

    public void read(NbtCompound root) {
        payload = root.contains(KEY) ? root.get(KEY).copy() : null;
    }

    public void write(NbtCompound root) {
        if (payload != null) {
            root.put(KEY, payload.copy());
        }
    }
}
