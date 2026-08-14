package com.villagecolony.data.save;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.ColonyLifecycle;
import com.villagecolony.core.colony.model.ColonyState;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.construction.service.ConstructionService;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A obra e a casa atravessam o fechar do mundo.
 *
 * <p>Era a dívida mais cara do projeto em 2026-08-14: a casa continuava
 * de pé — quem guarda blocos é o mundo —, mas a colônia reabria sem saber
 * que ela era dela, e a proteção do PROJECT_CONSTITUTION.md §10 sumia.
 *
 * <p>Round-trip puro, como {@link ColonySavedDataTest}: toca NBT e não
 * precisa de servidor. É onde os erros de persistência moram.
 */
class ConstructionSaveTest {

    private static final ResourceId HOUSE =
            ResourceId.vanilla("village/plains/houses/plains_small_house_1");

    private static ColonySavedData empty() {
        return ColonySavedData.TYPE.constructor().get();
    }

    private static ColonySavedData roundTrip(ColonySavedData data) {
        NbtCompound nbt = data.writeNbt(new NbtCompound(), null);

        return ColonySavedData.TYPE.deserializer().apply(nbt, null);
    }

    private static Colony colonyAt(UUID id, ColonyPos center) {
        return Colony.restore(id, center, ColonyState.STABLE, ColonyLifecycle.ACTIVE);
    }

    @Test
    void aBuildingSurvivesTheRoundTrip() {
        UUID colonyId = UUID.randomUUID();

        Building building = new Building(
                UUID.randomUUID(),
                colonyId,
                HOUSE,
                new ColonyPos(10, 64, -20),
                new ColonyPos(16, 70, -14));

        ColonySavedData data = empty();

        data.sync(
                List.of(colonyAt(colonyId, new ColonyPos(0, 64, 0))),
                List.of(),
                List.of(),
                List.of(building));

        List<Building> restored = roundTrip(data).buildings();

        assertEquals(1, restored.size());
        assertEquals(building, restored.get(0));
    }

    @Test
    void anOpenProjectSurvivesTheRoundTrip() {
        UUID colonyId = UUID.randomUUID();

        ConstructionService.Pending project = new ConstructionService.Pending(
                UUID.randomUUID(),
                colonyId,
                HOUSE,
                new ColonyPos(-5, 63, 200),
                ConstructionState.BUILDING);

        ColonySavedData data = empty();

        data.sync(
                List.of(colonyAt(colonyId, new ColonyPos(0, 64, 0))),
                List.of(),
                List.of(project),
                List.of());

        List<ConstructionService.Pending> restored = roundTrip(data).projects();

        assertEquals(1, restored.size());
        assertEquals(project, restored.get(0));
    }

    /**
     * Construção de colônia que não veio no mesmo arquivo é descartada.
     *
     * <p>Mesma regra dos trabalhadores: uma casa de colônia inexistente
     * seria protegida para sempre por um dono que ninguém acha.
     */
    @Test
    void anOrphanBuildingIsDropped() {
        Building orphan = new Building(
                UUID.randomUUID(),
                UUID.randomUUID(),
                HOUSE,
                new ColonyPos(0, 64, 0),
                new ColonyPos(1, 65, 1));

        ColonySavedData data = empty();

        data.sync(List.of(), List.of(), List.of(), List.of(orphan));

        assertTrue(roundTrip(data).buildings().isEmpty());
    }

    @Test
    void anOrphanProjectIsDropped() {
        ConstructionService.Pending orphan = new ConstructionService.Pending(
                UUID.randomUUID(),
                UUID.randomUUID(),
                HOUSE,
                new ColonyPos(0, 64, 0),
                ConstructionState.BUILDING);

        ColonySavedData data = empty();

        data.sync(List.of(), List.of(), List.of(orphan), List.of());

        assertTrue(roundTrip(data).projects().isEmpty());
    }

    /** Save anterior a esta versão não tem as chaves, e isso não é erro. */
    @Test
    void anOldSaveLoadsWithoutConstruction() {
        ColonySavedData data = empty();

        data.sync(List.of(colonyAt(UUID.randomUUID(), new ColonyPos(0, 64, 0))), List.of());

        ColonySavedData restored = roundTrip(data);

        assertTrue(restored.buildings().isEmpty());
        assertTrue(restored.projects().isEmpty());
        assertEquals(1, restored.colonies().size());
    }

    /**
     * Coordenada negativa sobrevive.
     *
     * <p>O mesmo caso que {@link ColonySavedDataTest} guarda para as
     * colônias: metade do mundo tem coordenada negativa, e uma conversão
     * que a perdesse só apareceria lá.
     */
    @Test
    void negativeCoordinatesSurvive() {
        UUID colonyId = UUID.randomUUID();

        Building building = new Building(
                UUID.randomUUID(),
                colonyId,
                HOUSE,
                new ColonyPos(-1200, -59, -3400),
                new ColonyPos(-1194, -53, -3394));

        ColonySavedData data = empty();

        data.sync(
                List.of(colonyAt(colonyId, new ColonyPos(-1200, -59, -3400))),
                List.of(),
                List.of(),
                List.of(building));

        assertEquals(building, roundTrip(data).buildings().get(0));
    }
}
