package com.villagecolony.data.save;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A migração central de {@code ColonySavedData}, isolada do que
 * {@code MineSave} já resolve sozinho.
 *
 * <p><b>Por que não absorve o {@code SHAPE_VERSION} da mina.</b> Aquele
 * campo já é testado em produção e sua regra é "versão diferente descarta
 * a fronteira e recomeça": não é uma transformação de forma antiga para
 * nova, é um descarte deliberado. Misturar as duas aqui obrigaria reescrever
 * {@code MineSave.read} sem necessidade — decisão do autor, 2026-09-24.
 *
 * <p>Esta classe existe para o que hoje não tem dono: transformar o NBT
 * <b>antes</b> de qualquer leitor decidir sozinho o que fazer com um valor
 * legado. A primeira transformação real é a que já vivia espalhada em
 * {@code ColonySavedData.readProfession} — {@code BREEDER} virando
 * {@code SHEPHERD} — agora centralizada aqui.
 */
class SaveMigrationTest {

    @Test
    void freshNbtGetsCurrentVersionWithoutChangingShape() {
        NbtCompound source = new NbtCompound();

        NbtCompound migrated = SaveMigration.migrate(source);

        assertEquals(SaveMigration.CURRENT, migrated.getInt("saveVersion"));
    }

    @Test
    void migrationIsIdempotent() {
        NbtCompound source = legacyFixtureWithBreeder();

        NbtCompound once = SaveMigration.migrate(source);
        NbtCompound twice = SaveMigration.migrate(once);

        assertEquals(once, twice);
        assertEquals(SaveMigration.CURRENT, twice.getInt("saveVersion"));
    }

    @Test
    void legacyBreederProfessionBecomesShepherdBeforeAnyReaderRuns() {
        NbtCompound source = legacyFixtureWithBreeder();

        NbtCompound migrated = SaveMigration.migrate(source);

        String profession = migrated.getList("workers", 10)
                .getCompound(0)
                .getString("profession");

        assertEquals("SHEPHERD", profession);
    }

    /** O migrador nunca cria uma colônia, uma cama ou qualquer bloco. */
    @Test
    void migrationNeverAddsColoniesOrBuildings() {
        NbtCompound source = legacyFixtureWithBreeder();

        NbtCompound migrated = SaveMigration.migrate(source);

        assertTrue(migrated.getList("colonies", 10).isEmpty());
        assertTrue(migrated.getList("buildings", 10).isEmpty());
    }

    /** Save vindo de uma versão futura não pode regredir a leitura. */
    @Test
    void futureVersionIsLeftUntouched() {
        NbtCompound source = new NbtCompound();
        source.putInt("saveVersion", SaveMigration.CURRENT + 5);

        NbtCompound migrated = SaveMigration.migrate(source);

        assertEquals(SaveMigration.CURRENT + 5, migrated.getInt("saveVersion"));
    }

    private static NbtCompound legacyFixtureWithBreeder() {
        NbtCompound source = new NbtCompound();
        // Save anterior a esta versão nunca escreveu "saveVersion".

        NbtList workers = new NbtList();
        NbtCompound worker = new NbtCompound();
        worker.putUuid("villagerId", java.util.UUID.randomUUID());
        worker.putUuid("colonyId", java.util.UUID.randomUUID());
        worker.putString("profession", "BREEDER");
        workers.add(worker);

        source.put("workers", workers);
        source.put("colonies", new NbtList());
        source.put("buildings", new NbtList());

        return source;
    }
}
