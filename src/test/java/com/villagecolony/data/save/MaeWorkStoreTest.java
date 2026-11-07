package com.villagecolony.data.save;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A fila de obras pedidas no save, e as regiões antigas que saem dele — ADR-047. */
class MaeWorkStoreTest {

    private static NbtCompound savedWith(int version, NbtList regions) {
        NbtCompound work = new NbtCompound();
        NbtCompound root = new NbtCompound();

        work.putInt("version", version);
        work.put("regions", regions);
        root.put(MaeWorkStore.KEY, work);

        return root;
    }

    private static NbtCompound request(String path) {
        NbtCompound nbt = new NbtCompound();

        nbt.putString("path", path);

        return nbt;
    }

    @Test
    void anEmptyStoreHasNothingAndWritesNothing() {
        MaeWorkStore store = new MaeWorkStore(() -> { });
        NbtCompound root = new NbtCompound();

        store.write(root);

        assertTrue(store.supported());
        assertTrue(store.regions().isEmpty());
        assertTrue(store.requests().isEmpty());
        assertTrue(root.isEmpty());
    }

    @Test
    void theQueueKeepsItsOrderThroughTheSave() {
        AtomicInteger changes = new AtomicInteger();
        MaeWorkStore store = new MaeWorkStore(changes::incrementAndGet);

        store.setRequests(List.of(request("a"), request("b")));

        NbtCompound root = new NbtCompound();

        store.write(root);

        MaeWorkStore loaded = new MaeWorkStore(() -> { });

        loaded.read(root);

        assertEquals(1, changes.get());
        assertTrue(loaded.supported());
        assertEquals("a", loaded.requests().get(0).getString("path"));
        assertEquals("b", loaded.requests().get(1).getString("path"));
    }

    @Test
    void anOldRegionIsReadAndThenLeavesTheSave() {
        NbtList regions = new NbtList();
        NbtCompound region = new NbtCompound();

        region.putString("dimension", "minecraft:overworld");
        regions.add(region);

        AtomicInteger changes = new AtomicInteger();
        MaeWorkStore store = new MaeWorkStore(changes::incrementAndGet);

        store.read(savedWith(3, regions));

        assertEquals(1, store.regions().size());

        store.setRequests(List.of(request("a")));
        store.clearRegions();

        assertTrue(store.regions().isEmpty());
        assertEquals(1, store.requests().size(), "limpar as regiões apagou a fila");
        assertEquals(2, changes.get());
    }

    @Test
    void anUnknownEnvelopeIsNotTouched() {
        NbtCompound root = savedWith(99, new NbtList());
        MaeWorkStore store = new MaeWorkStore(() -> { });

        store.read(root);

        assertFalse(store.supported());
        assertThrows(IllegalStateException.class, store::regions);
        assertThrows(IllegalStateException.class, () -> store.setRequests(List.of()));
    }

    @Test
    void aMalformedRegionListIsNotSupported() {
        NbtList strange = new NbtList();

        strange.add(NbtString.of("x"));

        MaeWorkStore store = new MaeWorkStore(() -> { });

        store.read(savedWith(1, strange));

        assertFalse(store.supported());
    }
}
