package com.villagecolony.data.save;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Um save com uma entrada malformada abre com o resto intacto — AUD-02.
 *
 * <p>Por que importa: uma exceção que escapa de {@code readNbt} faz o
 * {@code PersistentStateManager} do Minecraft criar um estado vazio e gravá-lo
 * por cima do arquivo no próximo salvamento. Cada caso parte de um save válido
 * com uma colônia e um trabalhador, estraga um campo, e exige que a colônia
 * válida continue lá.
 *
 * <p>Nasceu como sonda da auditoria de 10-10 ({@code scripts/audit/probes}),
 * onde 6 dos 12 casos falhavam.
 */
class ColonySaveFuzzTest {

    private static NbtCompound validSave(UUID colonyId) {
        Colony colony = Colony.create(colonyId, new ColonyPos(0, 64, 0));
        Worker worker = Worker.restore(UUID.randomUUID(), colonyId, ProfessionType.SHEPHERD);
        ColonySavedData data = ColonySavedData.TYPE.constructor().get();
        data.sync(List.of(colony), List.of(worker));
        return data.writeNbt(new NbtCompound(), null);
    }

    private static NbtCompound entryOf(UUID colonyId, String blueprint) {
        NbtCompound entry = new NbtCompound();
        entry.putUuid(ColonySavedData.ID, UUID.randomUUID());
        entry.putUuid(ColonySavedData.COLONY_ID, colonyId);
        if (blueprint != null) {
            entry.putString(ColonySavedData.BLUEPRINT, blueprint);
        }
        return entry;
    }

    private static void append(NbtCompound save, String list, NbtCompound entry) {
        NbtList entries = save.contains(list) ? save.getList(list, 10) : new NbtList();
        entries.add(entry);
        save.put(list, entries);
    }

    private static DynamicTest opens(String name, BiConsumer<NbtCompound, UUID> damage) {
        return DynamicTest.dynamicTest(name, () -> {
            UUID colonyId = UUID.randomUUID();
            NbtCompound save = validSave(colonyId);
            damage.accept(save, colonyId);

            ColonySavedData loaded = ColonySavedData.TYPE.deserializer().apply(save, null);

            assertEquals(1, loaded.colonies().size(), "a colônia válida se perdeu ao carregar");
            assertEquals(colonyId, loaded.colonies().get(0).id());
        });
    }

    @TestFactory
    List<DynamicTest> aMalformedEntryNeverEmptiesTheSave() {
        List<DynamicTest> cases = new ArrayList<>();
        cases.add(opens("save válido", (save, id) -> { }));
        cases.add(opens("versão futura", (save, id) -> save.putInt("saveVersion", 99)));
        cases.add(opens("versão negativa", (save, id) -> save.putInt("saveVersion", -1)));
        cases.add(opens("construção com blueprint de dois ':'", (save, id) ->
                append(save, ColonySavedData.BUILDINGS, entryOf(id, "minecraft:village:house"))));
        cases.add(opens("construção sem blueprint", (save, id) ->
                append(save, ColonySavedData.BUILDINGS, entryOf(id, null))));
        cases.add(opens("construção com blueprint em branco", (save, id) ->
                append(save, ColonySavedData.BUILDINGS, entryOf(id, "   "))));
        cases.add(opens("obra com blueprint de dois ':'", (save, id) ->
                append(save, ColonySavedData.PROJECTS, entryOf(id, "a:b:c"))));
        cases.add(opens("obra sem blueprint", (save, id) ->
                append(save, ColonySavedData.PROJECTS, entryOf(id, null))));
        cases.add(opens("profissão com tipo trocado", (save, id) ->
                save.getList(ColonySavedData.WORKERS, 10).getCompound(0).putInt(ColonySavedData.PROFESSION, 7)));
        cases.add(opens("profissão desconhecida", (save, id) ->
                save.getList(ColonySavedData.WORKERS, 10).getCompound(0)
                        .putString(ColonySavedData.PROFESSION, "MANUFACTURER")));
        cases.add(opens("lista de trabalhadores com string", (save, id) -> {
            NbtList junk = new NbtList();
            junk.add(NbtString.of("lixo"));
            save.put(ColonySavedData.WORKERS, junk);
        }));
        cases.add(opens("centro com tipo trocado", (save, id) ->
                save.getList(ColonySavedData.COLONIES, 10).getCompound(0)
                        .putString(ColonySavedData.CENTER_X, "dez")));
        return cases;
    }

    /** A entrada boa ao lado da ruim continua: descarta-se só a malformada. */
    @Test
    void theGoodBuildingNextToTheBadOneSurvives() {
        UUID colonyId = UUID.randomUUID();
        NbtCompound save = validSave(colonyId);

        NbtCompound good = entryOf(colonyId, "minecraft:village/plains/houses/plains_small_house_1");
        good.putBoolean(ColonySavedData.FINISHED, true);
        append(save, ColonySavedData.BUILDINGS, entryOf(colonyId, "a:b:c"));
        append(save, ColonySavedData.BUILDINGS, good);

        ColonySavedData loaded = ColonySavedData.TYPE.deserializer().apply(save, null);

        assertEquals(1, loaded.buildings().size(), "a construção válida devia sobreviver à vizinha ruim");
        assertTrue(loaded.buildings().get(0).blueprint().toString().endsWith("plains_small_house_1"));
    }
}
