package com.villagecolony.data.save;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import java.util.List;

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

    /** As regiões das versões em que o MAE escrevia a obra saem do save — ADR-047. */
    public void clearRegions() {
        NbtCompound root = payload == null ? new NbtCompound() : ((NbtCompound) payload).copy();

        root.putInt("version", root.contains("version", NbtElement.INT_TYPE) ? root.getInt("version") : 1);
        root.put("regions", new NbtList());
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
