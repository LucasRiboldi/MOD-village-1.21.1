package com.villagecolony.data.save;

import com.villagecolony.core.construction.model.ColonySweepCursor;
import com.villagecolony.core.type.ColonyPos;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A varredura pela metade, indo e voltando do disco — 2026-08-27.
 *
 * <p>O irmão do {@link RoadIndexSave}, e os dois se completam: aquele
 * guarda a volta <b>terminada</b>, este guarda a que ficou no meio. A
 * sessão das 20:22 mostrou por que faltava o segundo — catorze
 * passagens de dezessete, e nada gravado, porque o índice só nasce
 * completo.
 *
 * <p>Nunca os dois para a mesma colônia, e não por acaso: o índice é
 * construído no mesmo instante em que o cursor é apagado, e o cursor só
 * existe enquanto não há índice.
 *
 * <p><b>O que já foi achado vai junto</b>, e é a metade que faz o
 * conserto ser conserto. Ver {@link ColonySweepCursor}: cursor sem
 * memória terminaria a volta com meia lista de ruas e a chamaria de
 * índice completo.
 */
final class SweepCursorSave {

    private static final String SWEEPS = "sweeps";
    private static final String COLONY_ID = "colonyId";
    private static final String FROM_X = "fromX";
    private static final String FROM_Y = "fromY";
    private static final String FROM_Z = "fromZ";
    private static final String RING = "ring";
    private static final String COLUMN = "column";
    private static final String FOUND = "found";

    private SweepCursorSave() {
    }

    static void write(NbtCompound nbt, Collection<ColonySweepCursor> cursors) {
        NbtList list = new NbtList();

        for (ColonySweepCursor cursor : cursors) {
            NbtCompound entry = new NbtCompound();

            entry.putUuid(COLONY_ID, cursor.colonyId());
            entry.putInt(FROM_X, cursor.from().x());
            entry.putInt(FROM_Y, cursor.from().y());
            entry.putInt(FROM_Z, cursor.from().z());
            entry.putInt(RING, cursor.ring());
            entry.putInt(COLUMN, cursor.column());

            long[] found = new long[cursor.found().size()];

            for (int i = 0; i < found.length; i++) {
                found[i] = cursor.found().get(i);
            }

            entry.putLongArray(FOUND, found);

            list.add(entry);
        }

        nbt.put(SWEEPS, list);
    }

    /**
     * Os cursores do disco, sem os de colônia desconhecida.
     *
     * <p>Fronteira do sistema, e desconfia de tudo. Save anterior a
     * 2026-08-27 não tem a chave: a colônia varre do centro, que é o que
     * esta versão fazia toda vez.
     *
     * <p><b>Anel ou coluna negativos são recusados.</b> Não existem na
     * varredura, e retomar num deles pediria ao laço uma casca sem
     * colunas — a colônia atravessaria o raio inteiro sem olhar nada e
     * diria "não há lote", que é a mentira do E14 por outro caminho.
     *
     * <p>Lista de achados vazia é aceita, ao contrário do índice pronto:
     * os anéis de dentro podem não ter calçamento, e recusar por isso
     * devolveria a colônia ao centro.
     */
    static List<ColonySweepCursor> read(NbtCompound nbt, Set<UUID> knownColonies) {
        List<ColonySweepCursor> found = new ArrayList<>();

        NbtList list = nbt.getList(SWEEPS, NbtElement.COMPOUND_TYPE);

        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);

            if (!entry.containsUuid(COLONY_ID)) {
                continue;
            }

            UUID colonyId = entry.getUuid(COLONY_ID);

            if (!knownColonies.contains(colonyId)) {
                continue;
            }

            int ring = entry.getInt(RING);
            int column = entry.getInt(COLUMN);

            if (ring < 0 || column < 0) {
                continue;
            }

            long[] columns = entry.getLongArray(FOUND);

            List<Long> roads = new ArrayList<>(columns.length);

            for (long road : columns) {
                roads.add(road);
            }

            found.add(new ColonySweepCursor(
                    colonyId,
                    new ColonyPos(
                            entry.getInt(FROM_X),
                            entry.getInt(FROM_Y),
                            entry.getInt(FROM_Z)),
                    ring,
                    column,
                    roads));
        }

        return found;
    }
}
