package com.villagecolony.data.save;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class MaeWorkStoreTest {
    @Test
    void missingSectionDoesNotCreatePermissionsOrChangeLegacyData() {
        NbtCompound legacy = new NbtCompound();
        legacy.putString("unrelated", "preserved");
        var store = new MaeWorkStore(() -> { });
        store.read(legacy);
        store.write(legacy);
        assertTrue(store.regions().isEmpty());
        assertEquals("preserved", legacy.getString("unrelated"));
        assertFalse(legacy.contains(MaeWorkStore.KEY));
    }

    @Test
    void futureAndMalformedPayloadsRemainOpaqueAndBlockUpdates() {
        for (int version : new int[]{1, 2}) {
            NbtCompound payload = new NbtCompound();
            payload.putInt("version", version);
            if (version == 1) {
                payload.putString("regions", "unknown shape");
            } else {
                payload.put("regions", new NbtList());
            }
            NbtCompound root = new NbtCompound();
            root.put(MaeWorkStore.KEY, payload);
            var store = new MaeWorkStore(() -> { });
            store.read(root);
            assertFalse(store.supported());
            assertThrows(IllegalStateException.class, store::regions);
            NbtCompound region = new NbtCompound();
            region.putUuid("id", UUID.randomUUID());
            assertThrows(IllegalStateException.class, () -> store.update(region));
            NbtCompound rewritten = new NbtCompound();
            store.write(rewritten);
            assertEquals(root, rewritten);
        }
    }

    @Test
    void updatingKnownVersionPreservesUninterpretedEnvelopeFields() {
        NbtCompound payload = new NbtCompound();
        payload.putInt("version", 1);
        payload.put("regions", new NbtList());
        payload.putString("extension", "preserved");
        NbtCompound root = new NbtCompound();
        root.put(MaeWorkStore.KEY, payload);
        var store = new MaeWorkStore(() -> { });
        store.read(root);
        NbtCompound region = new NbtCompound();
        region.putUuid("id", UUID.randomUUID());
        store.update(region);
        NbtCompound rewritten = new NbtCompound();
        store.write(rewritten);
        assertEquals("preserved", rewritten.getCompound(MaeWorkStore.KEY).getString("extension"));
    }

    @Test
    void colonySaveRoundTripPreservesIntentAndNeverExposesMutablePayload() {
        var data = ColonySavedData.TYPE.constructor().get();
        UUID id = UUID.randomUUID();
        NbtCompound region = new NbtCompound();
        region.putUuid("id", id);
        region.putString("reason", "blocked");
        region.put("history", new NbtList());
        data.maeWork().update(region);
        region.putString("reason", "changed outside store");
        assertEquals("blocked", data.maeWork().region(id).orElseThrow().getString("reason"));
        NbtCompound root = data.writeNbt(new NbtCompound(), null);
        var restored = ColonySavedData.TYPE.deserializer().apply(root, null);
        assertEquals(data.maeWork().region(id), restored.maeWork().region(id));
        restored.maeWork().region(id).orElseThrow().remove("history");
        assertTrue(restored.maeWork().region(id).orElseThrow().contains("history"));
        assertTrue(data.isDirty());
    }
}
