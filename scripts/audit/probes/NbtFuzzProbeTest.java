package com.villagecolony.audit;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.data.save.ColonySavedData;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Sonda da auditoria de 2026-10-10 (D5/D9): fuzzing leve do leitor do save.
 * Não faz parte da bateria; {@code scripts/audit/check-nbt-migration.sh}
 * copia este arquivo para {@code src/test}, roda e o remove.
 *
 * <p>Cada caso parte de um save válido com uma colônia e um trabalhador e
 * estraga um campo. A expectativa é a do skill
 * {@code fabric-persistence-migration}: o leitor não lança; descarta a
 * entrada ruim e segue. Um caso que falha aqui é um caso em que o
 * {@code PersistentStateManager} do Minecraft registra "Error loading saved
 * data", devolve um estado vazio e grava esse vazio por cima do arquivo no
 * próximo salvamento.
 */
class NbtFuzzProbeTest {

    private static NbtCompound baseSave(UUID colonyId) {
        Colony colony = Colony.create(colonyId, new ColonyPos(0, 64, 0));
        Worker worker = Worker.restore(UUID.randomUUID(), colonyId, ProfessionType.SHEPHERD);
        ColonySavedData data = ColonySavedData.TYPE.constructor().get();
        data.sync(List.of(colony), List.of(worker));
        return data.writeNbt(new NbtCompound(), null);
    }

    private static NbtCompound entry(UUID colonyId, String blueprint) {
        NbtCompound e = new NbtCompound();
        e.putUuid("id", UUID.randomUUID());
        e.putUuid("colonyId", colonyId);
        if (blueprint != null) {
            e.putString("blueprint", blueprint);
        }
        return e;
    }

    private static void addTo(NbtCompound save, String list, NbtCompound e) {
        NbtList l = save.contains(list) ? save.getList(list, 10) : new NbtList();
        l.add(e);
        save.put(list, l);
    }

    private static DynamicTest loads(String name, Consumer<NbtCompound> damage) {
        return DynamicTest.dynamicTest(name, () -> {
            UUID colonyId = UUID.randomUUID();
            NbtCompound save = baseSave(colonyId);
            save.putUuid("__colony", colonyId);
            damage.accept(save);
            save.remove("__colony");
            ColonySavedData loaded = ColonySavedData.TYPE.deserializer().apply(save, null);
            if (loaded.colonies().isEmpty()) {
                throw new AssertionError("a colônia válida se perdeu ao carregar");
            }
        });
    }

    @TestFactory
    List<DynamicTest> malformedSavesLoadWithoutThrowing() {
        List<DynamicTest> cases = new ArrayList<>();
        cases.add(loads("controle: save válido", s -> { }));
        cases.add(loads("controle: versão futura (saveVersion=99)", s -> s.putInt("saveVersion", 99)));
        cases.add(loads("saveVersion negativo", s -> s.putInt("saveVersion", -1)));
        cases.add(loads("building com blueprint de dois ':'", s ->
                addTo(s, "buildings", entry(s.getUuid("__colony"), "minecraft:village:house"))));
        cases.add(loads("building sem blueprint", s ->
                addTo(s, "buildings", entry(s.getUuid("__colony"), null))));
        cases.add(loads("building com blueprint em branco", s ->
                addTo(s, "buildings", entry(s.getUuid("__colony"), "   "))));
        cases.add(loads("projeto com blueprint de dois ':'", s ->
                addTo(s, "projects", entry(s.getUuid("__colony"), "a:b:c"))));
        cases.add(loads("projeto sem blueprint", s ->
                addTo(s, "projects", entry(s.getUuid("__colony"), null))));
        cases.add(loads("profession com tipo trocado (int)", s ->
                s.getList("workers", 10).getCompound(0).putInt("profession", 7)));
        cases.add(loads("profession desconhecida", s ->
                s.getList("workers", 10).getCompound(0).putString("profession", "MANUFACTURER")));
        cases.add(loads("lista workers com elemento string", s -> {
            NbtList l = new NbtList();
            l.add(NbtString.of("lixo"));
            s.put("workers", l);
        }));
        cases.add(loads("centerX com tipo trocado (string)", s ->
                s.getList("colonies", 10).getCompound(0).putString("centerX", "dez")));
        return cases;
    }
}
