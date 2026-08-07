package com.villagecolony.data.save;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.ColonyLifecycle;
import com.villagecolony.core.colony.model.ColonyState;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip de serialização.
 *
 * <p>Toca tipos do Minecraft (NBT), então não é unit test de Core pela
 * definição de Testing-Strategy.md §3. Não precisa de servidor: exercita
 * apenas a conversão, que é onde os erros de persistência moram.
 */
class ColonySavedDataTest {

    private static ColonySavedData empty() {
        return ColonySavedData.TYPE.constructor().get();
    }

    private static ColonySavedData roundTrip(ColonySavedData data) {
        NbtCompound nbt = data.writeNbt(new NbtCompound(), null);

        return ColonySavedData.TYPE.deserializer().apply(nbt, null);
    }

    @Test
    void survivesRoundTrip() {
        Colony colony = Colony.restore(
                UUID.randomUUID(),
                new ColonyPos(120, 68, -340),
                ColonyState.EXPANSION,
                ColonyLifecycle.ACTIVE);

        ColonySavedData data = empty();
        data.sync(List.of(colony), List.of());

        List<Colony> loaded = roundTrip(data).colonies();

        assertEquals(1, loaded.size());

        Colony restored = loaded.get(0);

        assertEquals(colony.id(), restored.id());
        assertEquals(colony.center(), restored.center());
        assertEquals(ColonyState.EXPANSION, restored.state());
    }

    /** ADR-002: nada está carregado ao abrir o mundo. */
    @Test
    void everyColonyComesBackDormant() {
        Colony active = Colony.restore(
                UUID.randomUUID(), new ColonyPos(0, 64, 0),
                ColonyState.PRODUCTION, ColonyLifecycle.ACTIVE);

        ColonySavedData data = empty();
        data.sync(List.of(active), List.of());

        Colony restored = roundTrip(data).colonies().get(0);

        assertEquals(ColonyLifecycle.DORMANT, restored.lifecycle());
        assertEquals(ColonyState.PRODUCTION, restored.state());
    }

    @Test
    void keepsSeveralColonies() {
        List<Colony> original = List.of(
                Colony.create(UUID.randomUUID(), new ColonyPos(1, 64, 1)),
                Colony.create(UUID.randomUUID(), new ColonyPos(2, 64, 2)),
                Colony.create(UUID.randomUUID(), new ColonyPos(3, 64, 3)));

        ColonySavedData data = empty();
        data.sync(original, List.of());

        List<Colony> loaded = roundTrip(data).colonies();

        assertEquals(original.size(), loaded.size());
        assertEquals(
                original.stream().map(Colony::id).toList(),
                loaded.stream().map(Colony::id).toList());
    }

    @Test
    void emptyRegistryRoundTripsToEmpty() {
        ColonySavedData data = empty();
        data.sync(List.of(), List.of());

        assertTrue(roundTrip(data).colonies().isEmpty());
    }

    @Test
    void negativeCoordinatesSurvive() {
        Colony colony = Colony.create(
                UUID.randomUUID(), new ColonyPos(-1500, -60, -2000));

        ColonySavedData data = empty();
        data.sync(List.of(colony), List.of());

        assertEquals(
                new ColonyPos(-1500, -60, -2000),
                roundTrip(data).colonies().get(0).center());
    }

    /** Enum removido do código não pode impedir de abrir o mundo. */
    @Test
    void unknownStateFallsBackToStable() {
        Colony colony = Colony.create(UUID.randomUUID(), new ColonyPos(0, 64, 0));

        ColonySavedData data = empty();
        data.sync(List.of(colony), List.of());

        NbtCompound nbt = data.writeNbt(new NbtCompound(), null);
        nbt.getList("colonies", 10).getCompound(0).putString("state", "HARVESTING_MOONBEAMS");

        Colony restored = ColonySavedData.TYPE.deserializer().apply(nbt, null).colonies().get(0);

        assertEquals(ColonyState.STABLE, restored.state());
    }

    /** Entrada sem id é lixo: ignorar, não derrubar o carregamento. */
    @Test
    void entryWithoutIdIsSkipped() {
        Colony colony = Colony.create(UUID.randomUUID(), new ColonyPos(0, 64, 0));

        ColonySavedData data = empty();
        data.sync(List.of(colony), List.of());

        NbtCompound nbt = data.writeNbt(new NbtCompound(), null);
        nbt.getList("colonies", 10).getCompound(0).remove("id");

        assertTrue(ColonySavedData.TYPE.deserializer().apply(nbt, null).colonies().isEmpty());
    }

    // --- Trabalhadores (TASK-012b) ---

    /** A profissão é decisão da colônia: se não voltar, some. */
    @Test
    void workerKeepsProfessionAcrossRoundTrip() {
        Colony colony = Colony.create(UUID.randomUUID(), new ColonyPos(0, 64, 0));

        Worker worker = Worker.restore(
                UUID.randomUUID(), colony.id(), ProfessionType.LUMBERJACK);

        ColonySavedData data = empty();
        data.sync(List.of(colony), List.of(worker));

        List<Worker> loaded = roundTrip(data).workers();

        assertEquals(1, loaded.size());
        assertEquals(worker.villagerId(), loaded.get(0).villagerId());
        assertEquals(colony.id(), loaded.get(0).colonyId());
        assertEquals(ProfessionType.LUMBERJACK, loaded.get(0).profession().orElseThrow());
    }

    /** Registrado e ainda sem função é um estado válido, não um defeito. */
    @Test
    void workerWithoutProfessionSurvives() {
        Colony colony = Colony.create(UUID.randomUUID(), new ColonyPos(0, 64, 0));

        Worker worker = Worker.register(UUID.randomUUID(), colony.id());

        ColonySavedData data = empty();
        data.sync(List.of(colony), List.of(worker));

        Worker restored = roundTrip(data).workers().get(0);

        assertFalse(restored.hasProfession());
    }

    /**
     * O motivo de colônias e trabalhadores morarem no mesmo arquivo:
     * um trabalhador sem colônia seria invisível para sempre.
     */
    @Test
    void workerOfUnknownColonyIsDropped() {
        Colony colony = Colony.create(UUID.randomUUID(), new ColonyPos(0, 64, 0));

        Worker orphan = Worker.restore(
                UUID.randomUUID(), UUID.randomUUID(), ProfessionType.FARMER);

        ColonySavedData data = empty();
        data.sync(List.of(colony), List.of(orphan));

        assertTrue(roundTrip(data).workers().isEmpty());
    }

    /** Enum removido do código não pode impedir de abrir o mundo. */
    @Test
    void unknownProfessionFallsBackToNone() {
        Colony colony = Colony.create(UUID.randomUUID(), new ColonyPos(0, 64, 0));

        Worker worker = Worker.restore(
                UUID.randomUUID(), colony.id(), ProfessionType.BUILDER);

        ColonySavedData data = empty();
        data.sync(List.of(colony), List.of(worker));

        NbtCompound nbt = data.writeNbt(new NbtCompound(), null);
        nbt.getList("workers", 10).getCompound(0).putString("profession", "MOONBEAM_HARVESTER");

        Worker restored = ColonySavedData.TYPE.deserializer().apply(nbt, null).workers().get(0);

        assertFalse(restored.hasProfession());
    }

    /** Entrada sem villagerId é lixo: ignorar, não derrubar o carregamento. */
    @Test
    void workerWithoutVillagerIdIsSkipped() {
        Colony colony = Colony.create(UUID.randomUUID(), new ColonyPos(0, 64, 0));

        Worker worker = Worker.register(UUID.randomUUID(), colony.id());

        ColonySavedData data = empty();
        data.sync(List.of(colony), List.of(worker));

        NbtCompound nbt = data.writeNbt(new NbtCompound(), null);
        nbt.getList("workers", 10).getCompound(0).remove("villagerId");

        assertTrue(ColonySavedData.TYPE.deserializer().apply(nbt, null).workers().isEmpty());
    }

    /**
     * Save anterior à TASK-012b não tem a lista. A varredura reencontra
     * os aldeões; o mundo precisa abrir.
     */
    @Test
    void saveWithoutWorkerListLoadsEmpty() {
        Colony colony = Colony.create(UUID.randomUUID(), new ColonyPos(0, 64, 0));

        ColonySavedData data = empty();
        data.sync(List.of(colony), List.of());

        NbtCompound nbt = data.writeNbt(new NbtCompound(), null);
        nbt.remove("workers");

        ColonySavedData loaded = ColonySavedData.TYPE.deserializer().apply(nbt, null);

        assertEquals(1, loaded.colonies().size());
        assertTrue(loaded.workers().isEmpty());
    }

    @Test
    void coloniesListIsUnmodifiable() {
        ColonySavedData data = empty();
        data.sync(List.of(Colony.create(UUID.randomUUID(), new ColonyPos(0, 64, 0))), List.of());

        List<Colony> loaded = data.colonies();

        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class, () -> loaded.clear());
    }
}
