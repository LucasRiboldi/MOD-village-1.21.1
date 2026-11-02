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
                && root.contains("version", NbtElement.INT_TYPE) && root.getInt("version") == 1
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
        root.putInt("version", 1);
        root.put("regions", list);
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
