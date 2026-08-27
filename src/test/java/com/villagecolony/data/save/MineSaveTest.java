package com.villagecolony.data.save;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.ColonyLifecycle;
import com.villagecolony.core.colony.model.ColonyState;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A mina atravessa o fechar do mundo — 2026-08-20.
 *
 * <p>O mundo guarda os túneis; ninguém guardava a mina. Reabrindo o save,
 * o mineiro reprocurava a boca — e achava outra, alguns blocos abaixo,
 * porque a de ontem tinha sido cavada — e recomeçava a fronteira do
 * primeiro degrau, revarrendo índice por índice tudo o que já estava
 * aberto. Custava pouco no primeiro dia e crescia com a profundidade.
 *
 * <p>Round-trip puro, como {@link ConstructionSaveTest}: toca NBT e não
 * precisa de servidor.
 */
class MineSaveTest {

    private static final ColonyPos CENTER = new ColonyPos(0, 64, 0);

    private static final ColonyPos MOUTH = new ColonyPos(40, 63, -8);

    private static ColonySavedData empty() {
        return ColonySavedData.TYPE.constructor().get();
    }

    private static ColonySavedData roundTrip(ColonySavedData data) {
        NbtCompound nbt = data.writeNbt(new NbtCompound(), null);

        return ColonySavedData.TYPE.deserializer().apply(nbt, null);
    }

    private static Colony colonyAt(UUID id) {
        return Colony.restore(id, CENTER, ColonyState.STABLE, ColonyLifecycle.ACTIVE);
    }

    private static ColonySavedData savedWith(Colony colony, Mine mine) {
        ColonySavedData data = empty();

        data.sync(
                List.of(colony),
                List.of(),
                List.of(),
                List.of(),
                List.of(mine));

        return data;
    }

    /** A boca, os dois lados e a fronteira — os quatro que o mundo não diz. */
    @Test
    void theMineSurvivesTheRoundTrip() {
        UUID colonyId = UUID.randomUUID();

        Mine mine = Mine.restore(
                colonyId,
                MineShaft.from(MOUTH, Side.EAST),
                437);

        List<Mine> read = roundTrip(savedWith(colonyAt(colonyId), mine)).mines();

        assertEquals(1, read.size());

        Mine back = read.get(0);

        assertEquals(colonyId, back.colonyId());
        assertEquals(MOUTH, back.entry());
        assertEquals(mine.shaft().descent(), back.shaft().descent());
        assertEquals(mine.shaft().gallery(), back.shaft().gallery());
        assertEquals(437, back.cut());
    }

    /**
     * A galeria virada continua virada.
     *
     * <p>É a metade que mais escondia o erro: sem gravar o lado, a mina
     * reabria apontada para a lava que já a tinha feito virar, e o
     * mineiro batia oito vezes na mesma barreira para virar de novo.
     */
    @Test
    void theTurnedGalleryComesBackTurned() {
        UUID colonyId = UUID.randomUUID();

        Mine mine = Mine.open(colonyId, MineShaft.from(MOUTH, Side.EAST));

        Side straight = mine.shaft().gallery();

        mine.turn();

        Mine back = roundTrip(savedWith(colonyAt(colonyId), mine)).mines().get(0);

        assertEquals(straight.clockwise(), back.shaft().gallery());
    }

    /**
     * A sessão seguinte cava adiante, e não de novo.
     *
     * <p>A propriedade que motivou tudo, afirmada onde ela se prova: a
     * posição que sai depois de reabrir é a que vem <b>depois</b> da
     * última olhada ontem.
     */
    @Test
    void theMinerPicksUpWhereTheSessionEnded() {
        UUID colonyId = UUID.randomUUID();

        Mine mine = Mine.open(colonyId, MineShaft.from(MOUTH, Side.EAST));

        ColonyPos last = null;

        for (int i = 0; i < 60; i++) {
            last = mine.nextPosition();
        }

        Mine back = roundTrip(savedWith(colonyAt(colonyId), mine)).mines().get(0);

        assertEquals(60, back.cut());
        assertEquals(mine.shaft().positionAt(59), last);
        assertEquals(mine.shaft().positionAt(60), back.nextPosition());
    }

    /** Mina de colônia que não voltou é escada de dono nenhum. */
    @Test
    void aMineOfAnUnknownColonyIsDropped() {
        Mine orphan = Mine.open(UUID.randomUUID(), MineShaft.from(MOUTH, Side.EAST));

        ColonySavedData data = empty();

        data.sync(List.of(), List.of(), List.of(), List.of(), List.of(orphan));

        assertTrue(roundTrip(data).mines().isEmpty());
    }

    /**
     * Save de antes desta versão abre sem mina, e ninguém quebra.
     *
     * <p>A colônia volta sem escada e o mineiro abre uma na sessão
     * seguinte — que é exatamente o que esta versão fazia toda vez.
     */
    @Test
    void anOlderSaveSimplyHasNoMine() {
        UUID colonyId = UUID.randomUUID();

        ColonySavedData data = empty();

        data.sync(List.of(colonyAt(colonyId)), List.of());

        NbtCompound nbt = data.writeNbt(new NbtCompound(), null);

        nbt.remove("mines");

        assertTrue(ColonySavedData.TYPE.deserializer().apply(nbt, null).mines().isEmpty());
    }

    /**
     * Mina de antes da escada de três volta do começo — 2026-08-27.
     *
     * <p>A fronteira é um índice na ordem de cavar, e a ordem mudou: o
     * degrau passou de dois blocos para três, e o primeiro lance de vinte
     * posições para trinta. Um {@code cut} escrito na ordem antiga aponta
     * para outro lugar na nova, e a mina retomaria no meio — deixando
     * atrás dela exatamente os blocos de cabeça que este ciclo existe
     * para abrir.
     *
     * <p>Voltar do começo é barato e conserta: já aberto é pulado de
     * graça, 64 por passagem, e o que passa a ser cavado é só o que
     * faltava. A boca e os dois lados ficam — eles não dependem da ordem.
     */
    @Test
    void aMineFromBeforeTheTallerStairStartsOver() {
        UUID colonyId = UUID.randomUUID();

        Mine mine = Mine.restore(colonyId, MineShaft.from(MOUTH, Side.EAST), 437);

        NbtCompound nbt = savedWith(colonyAt(colonyId), mine)
                .writeNbt(new NbtCompound(), null);

        nbt.getList("mines", NbtElement.COMPOUND_TYPE).getCompound(0).remove("shape");

        Mine back = ColonySavedData.TYPE.deserializer().apply(nbt, null).mines().get(0);

        assertEquals(0, back.cut());
        assertEquals(MOUTH, back.entry());
        assertEquals(mine.shaft().gallery(), back.shaft().gallery());
    }

    /** A mina desta versão retoma onde parou, e não do começo. */
    @Test
    void aMineOfThisShapeKeepsItsFrontier() {
        UUID colonyId = UUID.randomUUID();

        Mine mine = Mine.restore(colonyId, MineShaft.from(MOUTH, Side.EAST), 437);

        assertEquals(437, roundTrip(savedWith(colonyAt(colonyId), mine)).mines().get(0).cut());
    }

    /**
     * Entrada estragada é descartada inteira, e não meio lida.
     *
     * <p>Um lado que não existe viraria mina apontada para lugar nenhum,
     * e uma fronteira negativa mandaria o mineiro cavar antes do primeiro
     * degrau. A fronteira do sistema recusa as duas.
     */
    @Test
    void aCorruptEntryIsRefused() {
        UUID colonyId = UUID.randomUUID();

        Mine mine = Mine.restore(colonyId, MineShaft.from(MOUTH, Side.EAST), 12);

        NbtCompound nbt = savedWith(colonyAt(colonyId), mine)
                .writeNbt(new NbtCompound(), null);

        NbtList mines = nbt.getList("mines", NbtElement.COMPOUND_TYPE);

        mines.getCompound(0).putString("gallery", "UPWARDS");

        assertTrue(ColonySavedData.TYPE.deserializer().apply(nbt, null).mines().isEmpty());

        mines.getCompound(0).putString("gallery", Side.WEST.name());
        mines.getCompound(0).putInt("cut", -3);

        assertTrue(ColonySavedData.TYPE.deserializer().apply(nbt, null).mines().isEmpty());
    }
}
