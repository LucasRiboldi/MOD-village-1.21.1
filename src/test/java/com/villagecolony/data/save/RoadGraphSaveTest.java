package com.villagecolony.data.save;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.ColonyLifecycle;
import com.villagecolony.core.colony.model.ColonyState;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.RoadGraph;
import com.villagecolony.core.type.ColonyPos;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtLong;
import net.minecraft.nbt.NbtString;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Compatibilidade paralela do grafo, sem servidor nem escrita de blocos — ADR-043. */
class RoadGraphSaveTest {
    private static final ColonyPos HUB = new ColonyPos(10, 64, 10);

    private static ColonySavedData data(UUID id) {
        var data = ColonySavedData.TYPE.constructor().get();
        data.sync(List.of(Colony.restore(id, HUB, ColonyState.EXPANSION, ColonyLifecycle.ACTIVE)),
                List.of(), List.of(), List.of(), List.of(),
                List.of(new ColonyRoads(id, HUB, List.of(ColonyRoads.column(10, 10)))));
        return data;
    }

    private static RoadGraph graph() {
        var door = new ColonyPos(11, 64, 10);
        return RoadGraph.from(Set.of(HUB, door), HUB, Set.of(door));
    }

    private static ColonySavedData load(NbtCompound nbt) {
        return ColonySavedData.TYPE.deserializer().apply(nbt, null);
    }

    @Test
    void currentGraphRoundTripsBesideUnchangedLegacyColumns() {
        UUID id = UUID.randomUUID();
        var data = data(id);
        data.roadGraphs().update(id, graph());
        var nbt = data.writeNbt(new NbtCompound(), null);
        var loaded = load(nbt);
        assertEquals(graph(), loaded.roadGraphs().graph(id).orElseThrow());
        assertEquals(data.roads(), loaded.roads());
        assertEquals(nbt, loaded.writeNbt(new NbtCompound(), null));
    }

    @Test
    void legacySaveDoesNotInventConfirmedGraphOrChangeItsIndex() {
        UUID id = UUID.randomUUID();
        var data = data(id);
        var loaded = load(data.writeNbt(new NbtCompound(), null));
        assertTrue(loaded.roadGraphs().graph(id).isEmpty());
        assertTrue(loaded.roadGraphs().versionSupported(id));
        assertEquals(data.roads(), loaded.roads());
    }

    @Test
    void futureVersionSurvivesSavingAndRefusesReplacement() {
        UUID id = UUID.randomUUID();
        var nbt = data(id).writeNbt(new NbtCompound(), null);
        var payload = new NbtCompound();
        payload.putInt("version", 999);
        payload.putString("unknown", "preserve this exact payload");
        var graphs = new NbtCompound();
        graphs.put(id.toString(), payload);
        nbt.put("roadGraphs", graphs);
        var loaded = load(nbt);
        assertFalse(loaded.roadGraphs().versionSupported(id));
        assertTrue(loaded.roadGraphs().graph(id).isEmpty());
        assertThrows(IllegalStateException.class, () -> loaded.roadGraphs().update(id, graph()));
        assertEquals(payload, loaded.writeNbt(new NbtCompound(), null)
                .getCompound("roadGraphs").getCompound(id.toString()));
        assertEquals(1, loaded.roads().size());
    }

    @Test
    void nonIntVersionsRemainOpaqueAndRefuseReplacement() {
        for (NbtElement version : List.of(NbtLong.of(4294967297L), NbtLong.of(1L),
                NbtDouble.of(1.5), NbtString.of("1"))) {
            NbtCompound payload = RoadGraphSave.encode(graph());
            payload.put("version", version);
            assertOpaqueAndNotReplaceable(payload);
        }
    }

    @Test
    void unknownOrAbsentVersionsRemainOpaqueAndRefuseReplacement() {
        for (int version : List.of(-1, 0, 2, 999)) {
            NbtCompound payload = RoadGraphSave.encode(graph());
            payload.put("version", NbtInt.of(version));
            assertOpaqueAndNotReplaceable(payload);
        }
        NbtCompound absent = RoadGraphSave.encode(graph());
        absent.remove("version");
        assertOpaqueAndNotReplaceable(absent);
    }

    private static void assertOpaqueAndNotReplaceable(NbtCompound payload) {
        UUID id = UUID.randomUUID();
        NbtCompound nbt = data(id).writeNbt(new NbtCompound(), null);
        nbt.getCompound("roadGraphs").put(id.toString(), payload.copy());
        var loaded = load(nbt);
        assertAll(payload.toString(),
                () -> assertTrue(loaded.roadGraphs().graph(id).isEmpty()),
                () -> assertFalse(loaded.roadGraphs().versionSupported(id)),
                () -> assertThrows(IllegalStateException.class,
                        () -> loaded.roadGraphs().update(id, graph())),
                () -> assertEquals(payload, load(loaded.writeNbt(new NbtCompound(), null))
                        .writeNbt(new NbtCompound(), null).getCompound("roadGraphs").getCompound(id.toString())),
                () -> assertEquals(1, loaded.roads().size()));
    }

    @Test
    void multipleHubsAreRejectedOnDecode() {
        NbtCompound payload = RoadGraphSave.encode(graph());
        NbtList roles = new NbtList();
        roles.add(NbtString.of("HUB"));
        payload.getList("nodes", NbtElement.COMPOUND_TYPE).getCompound(1).put("roles", roles);
        assertTrue(RoadGraphSave.decode(payload).isEmpty());
    }

    @Test
    void multipleHubsAreRejectedBeforePublishingOrDirtyingStore() {
        RoadGraph valid = graph();
        RoadGraph invalid = new RoadGraph(valid.nodes().stream()
                .map(node -> new RoadGraph.Node(node.position(), Set.of(RoadGraph.Role.HUB)))
                .toList(), valid.edges());
        int[] changes = {0};
        RoadGraphStore store = new RoadGraphStore(() -> changes[0]++);
        UUID id = UUID.randomUUID();
        store.update(id, valid);
        assertThrows(IllegalArgumentException.class, () -> store.update(id, invalid));
        assertEquals(valid, store.graph(id).orElseThrow());
        assertEquals(1, changes[0]);
    }

    @Test
    void malformedCurrentGraphFallsBackToLegacyAndCanBeRebuilt() {
        UUID id = UUID.randomUUID();
        var nbt = data(id).writeNbt(new NbtCompound(), null);
        var payload = new NbtCompound();
        payload.putInt("version", 1);
        payload.putString("nodes", "not a node list");
        var graphs = new NbtCompound();
        graphs.put(id.toString(), payload);
        nbt.put("roadGraphs", graphs);
        var loaded = load(nbt);
        assertTrue(loaded.roadGraphs().graph(id).isEmpty());
        assertEquals(1, loaded.roads().size());
        loaded.roadGraphs().update(id, graph());
        assertEquals(graph(), load(loaded.writeNbt(new NbtCompound(), null)).roadGraphs().graph(id).orElseThrow());
    }

    @Test
    void aNodeInsideAnUnsplitEdgeIsRejectedOnDecode() {
        ColonyPos middle = new ColonyPos(11, 64, 10);
        ColonyPos end = new ColonyPos(12, 64, 10);
        RoadGraph valid = RoadGraph.from(Set.of(HUB, middle, end), HUB, Set.of(end));
        NbtCompound payload = RoadGraphSave.encode(valid);
        NbtCompound node = new NbtCompound();
        node.putIntArray("position", new int[]{middle.x(), middle.y(), middle.z()});
        NbtList roles = new NbtList();
        roles.add(NbtString.of("JUNCTION"));
        node.put("roles", roles);
        payload.getList("nodes", NbtElement.COMPOUND_TYPE).add(node);

        assertTrue(RoadGraphSave.decode(payload).isEmpty(), "nó interno exige divisão do trecho");
    }

    @Test
    void anUnsplitInteriorNodeCannotReplaceOrDirtyTheStoredGraph() {
        ColonyPos middle = new ColonyPos(11, 64, 10);
        ColonyPos end = new ColonyPos(12, 64, 10);
        RoadGraph valid = RoadGraph.from(Set.of(HUB, middle, end), HUB, Set.of(end));
        var nodes = new java.util.ArrayList<>(valid.nodes());
        nodes.add(new RoadGraph.Node(middle, Set.of(RoadGraph.Role.JUNCTION)));
        RoadGraph invalid = new RoadGraph(nodes, valid.edges());
        int[] changes = {0};
        RoadGraphStore store = new RoadGraphStore(() -> changes[0]++);
        UUID id = UUID.randomUUID();
        store.update(id, valid);

        assertThrows(IllegalArgumentException.class, () -> store.update(id, invalid));
        assertEquals(valid, store.graph(id).orElseThrow());
        assertEquals(1, changes[0]);
    }

    @Test
    void orphanPayloadIsIgnoredWithoutDroppingKnownGraph() {
        UUID id = UUID.randomUUID();
        var data = data(id);
        data.roadGraphs().update(id, graph());
        var nbt = data.writeNbt(new NbtCompound(), null);
        var graphs = nbt.getCompound("roadGraphs");
        graphs.put(UUID.randomUUID().toString(), graphs.getCompound(id.toString()).copy());
        var loaded = load(nbt);
        var saved = loaded.writeNbt(new NbtCompound(), null);
        assertEquals(1, saved.getCompound("roadGraphs").getKeys().size());
        assertTrue(saved.contains("roads", NbtElement.LIST_TYPE));
        assertEquals(graph(), loaded.roadGraphs().graph(id).orElseThrow());
    }

    @Test
    void wrongListElementTypesAreCorruptionRatherThanEmptyGraph() {
        for (String key : List.of("nodes", "edges")) {
            NbtCompound payload = RoadGraphSave.encode(graph());
            NbtList wrong = new NbtList();
            wrong.add(NbtString.of("not a compound"));
            payload.put(key, wrong);
            assertTrue(RoadGraphSave.decode(payload).isEmpty(), key);
        }
    }

    @Test
    void cycleAtPhysicalColumnLimitRetainsRepeatedClosingAnchor() {
        Set<ColonyPos> columns = new HashSet<>();
        for (int i = 0; i < 1500; i++) {
            columns.add(new ColonyPos(i, 64, 0));
            columns.add(new ColonyPos(1500, 64, i));
            columns.add(new ColonyPos(1500 - i, 64, 1500));
            columns.add(new ColonyPos(0, 64, 1500 - i));
        }
        var cycle = RoadGraph.from(columns, new ColonyPos(-20, 64, -20), Set.of());
        assertEquals(6000, columns.size());
        assertEquals(6001, cycle.edges().getFirst().columns().size());
        assertEquals(cycle, RoadGraphSave.decode(RoadGraphSave.encode(cycle)).orElseThrow());
    }
}
