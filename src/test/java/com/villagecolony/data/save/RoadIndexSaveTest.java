package com.villagecolony.data.save;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.ColonyLifecycle;
import com.villagecolony.core.colony.model.ColonyState;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.integration.BuildSiteScanner;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O índice de ruas atravessa o fechar do mundo — 2026-08-27.
 *
 * <p>Sem isto, toda entrada no mundo remedia as 16.641 colunas do raio
 * de 64: mil por passagem, uma passagem por ciclo, dezessete ciclos —
 * oito minutos e meio antes de a colônia saber onde procurar lote. As
 * sessões curtas acabavam antes disso, e a vila passava a sessão inteira
 * dizendo "não há lote" com lote existindo.
 *
 * <p>Round-trip puro, como {@link MineSaveTest}: toca NBT e não precisa
 * de servidor.
 *
 * <p>O último teste sai da camada de dados de propósito. O empacotamento
 * da coluna é feito dos dois lados do disco, e é justamente aí que uma
 * discordância de bits não apareceria: o índice voltaria com o tamanho
 * certo e as ruas em outro lugar.
 */
class RoadIndexSaveTest {

    private static final ColonyPos CENTER = new ColonyPos(120, 68, -340);

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

    private static ColonyRoads roadsOf(UUID colonyId, long... columns) {
        List<Long> packed = new ArrayList<>();

        for (long column : columns) {
            packed.add(column);
        }

        return new ColonyRoads(colonyId, CENTER, packed);
    }

    private static ColonySavedData savedWith(Colony colony, ColonyRoads roads) {
        ColonySavedData data = empty();

        data.sync(
                List.of(colony),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(roads));

        return data;
    }

    /** As colunas, a ordem delas e o centro de onde foram medidas. */
    @Test
    void theIndexSurvivesTheRoundTrip() {
        UUID colonyId = UUID.randomUUID();

        ColonyRoads roads = roadsOf(
                colonyId,
                ColonyRoads.column(120, -340),
                ColonyRoads.column(121, -340),
                ColonyRoads.column(-7, 12));

        List<ColonyRoads> read = roundTrip(savedWith(colonyAt(colonyId), roads)).roads();

        assertEquals(1, read.size());

        ColonyRoads back = read.get(0);

        assertEquals(colonyId, back.colonyId());
        assertEquals(CENTER, back.from());
        assertEquals(roads.columns(), back.columns());
    }

    /**
     * Coordenada negativa volta negativa.
     *
     * <p>O empacotamento junta dois inteiros num long, e é onde o erro
     * de sinal se esconde: um z negativo mal lido vira uma rua a quatro
     * bilhões de blocos, e o índice inteiro passa a apontar para o vazio.
     */
    @Test
    void negativeColumnsComeBackNegative() {
        UUID colonyId = UUID.randomUUID();

        long column = ColonyRoads.column(-1204, -3388);

        ColonyRoads back = roundTrip(savedWith(colonyAt(colonyId), roadsOf(colonyId, column)))
                .roads()
                .get(0);

        assertEquals(-1204, ColonyRoads.xOf(back.columns().get(0)));
        assertEquals(-3388, ColonyRoads.zOf(back.columns().get(0)));
    }

    /** Índice de colônia que não voltou é mapa de vila nenhuma. */
    @Test
    void anIndexOfAnUnknownColonyIsDropped() {
        ColonyRoads orphan = roadsOf(UUID.randomUUID(), ColonyRoads.column(0, 0));

        ColonySavedData data = empty();

        data.sync(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(orphan));

        assertTrue(roundTrip(data).roads().isEmpty());
    }

    /**
     * Índice vazio é recusado.
     *
     * <p>Ele não quer dizer "esta vila não tem rua" — quer dizer "eu
     * varri tudo e não achei nenhuma", e uma colônia que acreditasse
     * nisso pararia de procurar lote para sempre. Varrer de novo é o
     * certo.
     */
    @Test
    void anEmptyIndexIsRefused() {
        UUID colonyId = UUID.randomUUID();

        ColonySavedData data = savedWith(colonyAt(colonyId), roadsOf(colonyId));

        assertTrue(roundTrip(data).roads().isEmpty());
    }

    /**
     * Save de antes desta versão abre sem índice, e ninguém quebra.
     *
     * <p>A colônia volta sem mapa e varre o quadrado na sessão seguinte —
     * que é exatamente o que esta versão fazia toda vez.
     */
    @Test
    void anOlderSaveSimplyHasNoIndex() {
        UUID colonyId = UUID.randomUUID();

        ColonySavedData data = empty();

        data.sync(List.of(colonyAt(colonyId)), List.of());

        NbtCompound nbt = data.writeNbt(new NbtCompound(), null);

        nbt.remove("roads");

        assertTrue(ColonySavedData.TYPE.deserializer().apply(nbt, null).roads().isEmpty());
    }

    /** Entrada sem dono é descartada, e não meio lida. */
    @Test
    void anEntryWithoutAnOwnerIsRefused() {
        UUID colonyId = UUID.randomUUID();

        NbtCompound nbt = savedWith(colonyAt(colonyId), roadsOf(colonyId, ColonyRoads.column(1, 1)))
                .writeNbt(new NbtCompound(), null);

        NbtList roads = nbt.getList("roads", NbtElement.COMPOUND_TYPE);

        roads.getCompound(0).remove("colonyId");

        assertTrue(ColonySavedData.TYPE.deserializer().apply(nbt, null).roads().isEmpty());
    }

    /**
     * A varredura de amanhã começa com o mapa de hoje.
     *
     * <p>A propriedade que motivou tudo, afirmada de ponta a ponta: o
     * que a sessão gravou é o que a seguinte encontra em mãos, com as
     * mesmas ruas nos mesmos lugares.
     */
    @Test
    void theNextSessionOpensWithTheIndexInHand() {
        UUID colonyId = UUID.randomUUID();

        BuildSiteScanner.clearAll();

        BuildSiteScanner.restore(roadsOf(
                colonyId,
                ColonyRoads.column(120, -340),
                ColonyRoads.column(-7, 12)));

        ColonySavedData data = empty();

        data.sync(
                List.of(colonyAt(colonyId)),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                BuildSiteScanner.saved());

        List<ColonyRoads> read = roundTrip(data).roads();

        BuildSiteScanner.clearAll();

        assertTrue(BuildSiteScanner.roadIndexSize(colonyId).isEmpty());

        read.forEach(BuildSiteScanner::restore);

        assertFalse(BuildSiteScanner.roadIndexSize(colonyId).isEmpty());
        assertEquals(2, BuildSiteScanner.roadIndexSize(colonyId).getAsInt());
        assertEquals(
                List.of(ColonyRoads.column(120, -340), ColonyRoads.column(-7, 12)),
                BuildSiteScanner.saved().get(0).columns());

        BuildSiteScanner.clearAll();
    }
}
