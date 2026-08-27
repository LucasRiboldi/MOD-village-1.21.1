package com.villagecolony.data.save;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.ColonyLifecycle;
import com.villagecolony.core.colony.model.ColonyState;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.ColonySweepCursor;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.integration.BuildSiteScanner;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A varredura pela metade atravessa o fechar do mundo — 2026-08-27.
 *
 * <p><b>A medição que pediu isto.</b> Sessão das 20:22, colônia
 * {@code 56c5b68d}: <i>14 passes over 14336 columns, 1 restarts (0 by
 * drift), 0 complete rounds</i>. A varredura andou perfeitamente — uma
 * passagem por ciclo, zero deriva — e precisava de 17. Faltaram três
 * ciclos, noventa segundos, e as catorze passagens foram para o lixo
 * porque o índice só nasce de uma volta <b>completa</b>.
 *
 * <p>Gravar o índice não bastou. O ciclo anterior escreveu que <i>"é ele
 * que vale gravar, não o cursor"</i>, e a medição desmentiu.
 *
 * <p><b>E o cursor sozinho também não basta</b> — é o que o último teste
 * afirma. Retomar no anel 40 sem o que os anéis 0 a 39 acharam faria a
 * volta terminar com meia lista de ruas e chamá-la de índice completo:
 * um índice que <b>mente</b> sobre ter visto tudo, que é pior do que não
 * ter índice nenhum.
 */
class SweepCursorSaveTest {

    private static final ColonyPos CENTER = new ColonyPos(772, 68, 898);

    private static ColonySavedData empty() {
        return ColonySavedData.TYPE.constructor().get();
    }

    private static ColonySavedData roundTrip(ColonySavedData data) {
        NbtCompound nbt = data.writeNbt(new NbtCompound(), null);

        return ColonySavedData.TYPE.deserializer().apply(nbt, null);
    }

    private static Colony colonyAt(UUID id) {
        return Colony.restore(id, CENTER, ColonyState.EXPANSION, ColonyLifecycle.ACTIVE);
    }

    private static ColonySavedData savedWith(Colony colony, ColonySweepCursor cursor) {
        ColonySavedData data = empty();

        data.sync(
                List.of(colony),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(cursor));

        return data;
    }

    /** O anel, a coluna, o centro, e o que a meia volta já achou. */
    @Test
    void theHalfSweepSurvivesTheRoundTrip() {
        UUID colonyId = UUID.randomUUID();

        ColonySweepCursor cursor = new ColonySweepCursor(
                colonyId, CENTER, 40, 137,
                List.of(ColonyRoads.column(772, 898), ColonyRoads.column(-7, 12)));

        List<ColonySweepCursor> read =
                roundTrip(savedWith(colonyAt(colonyId), cursor)).sweeps();

        assertEquals(1, read.size());

        ColonySweepCursor back = read.get(0);

        assertEquals(colonyId, back.colonyId());
        assertEquals(CENTER, back.from());
        assertEquals(40, back.ring());
        assertEquals(137, back.column());
        assertEquals(cursor.found(), back.found());
    }

    /**
     * Meia volta que ainda não achou rua nenhuma continua valendo.
     *
     * <p>Ao contrário do índice pronto, aqui a lista vazia é honesta: os
     * anéis de dentro podem não ter calçamento, e jogar o cursor fora por
     * isso devolveria a colônia ao centro — que é o custo inteiro que
     * este ciclo existe para evitar.
     */
    @Test
    void aSweepThatFoundNoRoadYetIsStillWorthKeeping() {
        UUID colonyId = UUID.randomUUID();

        ColonySweepCursor cursor =
                new ColonySweepCursor(colonyId, CENTER, 12, 3, List.of());

        assertEquals(12, roundTrip(savedWith(colonyAt(colonyId), cursor))
                .sweeps().get(0).ring());
    }

    /** Cursor de colônia que não voltou é lugar em vila nenhuma. */
    @Test
    void aCursorOfAnUnknownColonyIsDropped() {
        ColonySweepCursor orphan =
                new ColonySweepCursor(UUID.randomUUID(), CENTER, 3, 1, List.of());

        ColonySavedData data = empty();

        data.sync(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(orphan));

        assertTrue(roundTrip(data).sweeps().isEmpty());
    }

    /** Save de antes desta versão abre sem cursor, e varre do centro. */
    @Test
    void anOlderSaveSimplyHasNoCursor() {
        UUID colonyId = UUID.randomUUID();

        ColonySavedData data = empty();

        data.sync(List.of(colonyAt(colonyId)), List.of());

        NbtCompound nbt = data.writeNbt(new NbtCompound(), null);

        nbt.remove("sweeps");

        assertTrue(ColonySavedData.TYPE.deserializer().apply(nbt, null).sweeps().isEmpty());
    }

    /**
     * Anel negativo é recusado.
     *
     * <p>Um anel abaixo de zero não existe na varredura, e retomar nele
     * seria pedir ao laço uma casca que não tem colunas — a colônia
     * atravessaria o raio inteiro sem olhar nada e diria "não há lote".
     */
    @Test
    void aCorruptCursorIsRefused() {
        UUID colonyId = UUID.randomUUID();

        NbtCompound nbt = savedWith(
                colonyAt(colonyId),
                new ColonySweepCursor(colonyId, CENTER, 40, 137, List.of()))
                .writeNbt(new NbtCompound(), null);

        NbtList sweeps = nbt.getList("sweeps", NbtElement.COMPOUND_TYPE);

        sweeps.getCompound(0).putInt("ring", -1);

        assertTrue(ColonySavedData.TYPE.deserializer().apply(nbt, null).sweeps().isEmpty());

        sweeps.getCompound(0).putInt("ring", 40);
        sweeps.getCompound(0).putInt("column", -8);

        assertTrue(ColonySavedData.TYPE.deserializer().apply(nbt, null).sweeps().isEmpty());
    }

    /**
     * A sessão seguinte retoma no anel em que a anterior parou.
     *
     * <p>A propriedade que motivou tudo: catorze passagens deixam de ser
     * jogadas fora quando o mundo fecha.
     */
    @Test
    void theNextSessionResumesWhereTheLastOneStopped() {
        UUID colonyId = UUID.randomUUID();

        BuildSiteScanner.clearAll();

        BuildSiteScanner.restore(new ColonySweepCursor(
                colonyId, CENTER, 40, 137, List.of(ColonyRoads.column(772, 898))));

        ColonySavedData data = empty();

        data.sync(
                List.of(colonyAt(colonyId)),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                BuildSiteScanner.pausedSweeps());

        List<ColonySweepCursor> read = roundTrip(data).sweeps();

        BuildSiteScanner.clearAll();

        assertTrue(BuildSiteScanner.sweepPausedAt(colonyId).isEmpty());

        read.forEach(BuildSiteScanner::restore);

        assertEquals(40, BuildSiteScanner.sweepPausedAt(colonyId).getAsInt());

        BuildSiteScanner.clearAll();
    }

    /**
     * O que a meia volta achou volta com ela — e não como índice.
     *
     * <p>É a armadilha deste ciclo. O índice de ruas só nasce quando a
     * volta <b>completa</b>, juntando o que todas as passagens acharam.
     * Um cursor restaurado sem essa memória faria a volta terminar com
     * só as ruas dos anéis de fora e chamá-la de índice completo — e a
     * colônia passaria a perguntar a uma lista que não viu metade do
     * raio, sem nunca mais varrer para descobrir.
     *
     * <p>Por isso o cursor carrega o que já foi achado, e é por isso que
     * ele <b>não</b> aparece em {@code roadIndexSize}: meia volta não é
     * índice, e a diferença é a única coisa que separa um atalho de uma
     * mentira.
     */
    @Test
    void whatTheHalfSweepFoundIsNotAnIndexYet() {
        UUID colonyId = UUID.randomUUID();

        BuildSiteScanner.clearAll();

        BuildSiteScanner.restore(new ColonySweepCursor(
                colonyId, CENTER, 40, 137,
                List.of(ColonyRoads.column(772, 898), ColonyRoads.column(773, 898))));

        assertTrue(
                BuildSiteScanner.roadIndexSize(colonyId).isEmpty(),
                "meia volta virou índice, e ele mente sobre ter visto o raio inteiro");

        // Mas a memória está lá: ela sai de novo com o cursor.
        assertEquals(2, BuildSiteScanner.pausedSweeps().get(0).found().size());

        BuildSiteScanner.clearAll();
    }
}
