package com.villagecolony.data.save;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.ColonyLifecycle;
import com.villagecolony.core.colony.model.ColonyState;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ColonyHut;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.construction.service.ConstructionService;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
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

    /**
     * A conta do que a casa já recebeu atravessa o save.
     *
     * <p>Regra do autor, 2026-08-20: peça destruída não volta. Se esta
     * lista não sobrevivesse ao servidor parar, a cama que o jogador
     * desfez reapareceria na sessão seguinte — o mesmo defeito com prazo
     * mais longo, e mais difícil de ver.
     */
    @Test
    void whatTheHouseAlreadyGotSurvivesTheRoundTrip() {
        UUID colonyId = UUID.randomUUID();

        Building building = new Building(
                UUID.randomUUID(),
                colonyId,
                HOUSE,
                new ColonyPos(10, 64, -20),
                new ColonyPos(16, 70, -14))
                .withFurnished(ResourceId.vanilla("white_bed"))
                .withFurnished(ResourceId.vanilla("lantern"));

        ColonySavedData data = empty();

        data.sync(
                List.of(colonyAt(colonyId, new ColonyPos(0, 64, 0))),
                List.of(),
                List.of(),
                List.of(building));

        Building restored = roundTrip(data).buildings().get(0);

        assertEquals(
                Set.of(ResourceId.vanilla("white_bed"), ResourceId.vanilla("lantern")),
                restored.furnished());
    }

    /**
     * Casa do jogo gravada antes da lista: continua sem conta nenhuma.
     *
     * <p>A migração é só da cabana. Casa lida do arquivo do jogo tem a
     * mobília que o arquivo manda e nunca passou por esta regra.
     */
    @Test
    void aBuildingFromBeforeTheFurnitureRuleLoadsEmpty() {
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

        NbtCompound nbt = data.writeNbt(new NbtCompound(), null);

        for (int i = 0; i < nbt.getList("buildings", NbtElement.COMPOUND_TYPE).size(); i++) {
            nbt.getList("buildings", NbtElement.COMPOUND_TYPE).getCompound(i).remove("furnished");
        }

        assertTrue(ColonySavedData.TYPE.deserializer().apply(nbt, null).buildings().get(0).furnished().isEmpty());
    }

    /**
     * Cabana gravada antes da lista conta como já mobiliada.
     *
     * <p>Pedido do autor em 2026-08-20. Sem isto, cada casa que já está
     * de pé no mundo dele receberia cama, baú e lampião mais uma vez —
     * inclusive as de onde ele tinha acabado de tirar as peças, que é o
     * defeito que a regra veio proibir.
     */
    @Test
    void aHutFromBeforeTheFurnitureRuleCountsAsAlreadyFurnished() {
        NbtCompound nbt = savedHut();

        stripFurnishedList(nbt);

        Building restored =
                ColonySavedData.TYPE.deserializer().apply(nbt, null).buildings().get(0);

        for (BlueprintBlock piece : ColonyHut.furnishings()) {
            assertTrue(
                    restored.wasFurnishedWith(piece.block()),
                    "a casa antiga vai receber " + piece.block() + " de novo");
        }
    }

    /**
     * Cabana nova, com a lista vazia gravada, continua podendo receber.
     *
     * <p>É a outra metade da migração, e a que a torna segura: lista
     * ausente e lista vazia dizem coisas diferentes. Tratá-las igual
     * congelaria toda casa recém-construída sem mobília para sempre.
     */
    @Test
    void aNewHutWithAnEmptyListStillGetsItsFurniture() {
        Building restored = ColonySavedData.TYPE.deserializer()
                .apply(savedHut(), null).buildings().get(0);

        assertTrue(restored.furnished().isEmpty());
    }

    private static NbtCompound savedHut() {
        UUID colonyId = UUID.randomUUID();

        ColonySavedData data = empty();

        data.sync(
                List.of(colonyAt(colonyId, new ColonyPos(0, 64, 0))),
                List.of(),
                List.of(),
                List.of(new Building(
                        UUID.randomUUID(),
                        colonyId,
                        ColonyHut.ID,
                        new ColonyPos(10, 64, -20),
                        new ColonyPos(14, 68, -16))));

        return data.writeNbt(new NbtCompound(), null);
    }

    private static void stripFurnishedList(NbtCompound nbt) {
        NbtList saved = nbt.getList("buildings", NbtElement.COMPOUND_TYPE);

        for (int i = 0; i < saved.size(); i++) {
            saved.getCompound(i).remove("furnished");
        }
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
