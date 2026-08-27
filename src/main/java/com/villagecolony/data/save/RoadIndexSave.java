package com.villagecolony.data.save;

import com.villagecolony.core.construction.model.ColonyRoads;
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
 * O índice de ruas de cada colônia, indo e voltando do disco —
 * 2026-08-27.
 *
 * <p><b>A conta que pediu isto.</b> Achar as ruas custa varrer o
 * quadrado de raio 64: 16.641 colunas, mil por passagem, uma passagem
 * por ciclo — dezessete ciclos, oito minutos e meio. Enquanto o
 * resultado morria ao fechar o mundo, toda entrada pagava a conta
 * inteira de novo, e as sessões curtas acabavam antes do fim dela. Três
 * de cada quatro não chegavam a construir.
 *
 * <p><b>Por que é seguro gravar uma leitura do mundo.</b> Porque ela não
 * é acreditada: cada coluna do índice é reconferida quando visitada — o
 * {@code siteBesideRoadAt} pergunta ao mundo antes de oferecer o lote —,
 * e o centro de onde a medida saiu vem junto para que um índice velho
 * demais seja jogado fora em vez de mentir. O que se grava é o
 * <b>caminho até a pergunta</b>, e não a resposta.
 *
 * <p>Num arquivo separado pelo mesmo motivo do {@link MineSave}: o
 * {@code ColonySavedData} já passa das quinhentas linhas que o projeto
 * se impôs, e um agregado por arquivo é a forma de o próximo não repetir
 * a conta.
 */
final class RoadIndexSave {

    private static final String ROADS = "roads";
    private static final String COLONY_ID = "colonyId";
    private static final String FROM_X = "fromX";
    private static final String FROM_Y = "fromY";
    private static final String FROM_Z = "fromZ";
    private static final String COLUMNS = "columns";

    private RoadIndexSave() {
    }

    static void write(NbtCompound nbt, Collection<ColonyRoads> roads) {
        NbtList list = new NbtList();

        for (ColonyRoads index : roads) {
            NbtCompound entry = new NbtCompound();

            entry.putUuid(COLONY_ID, index.colonyId());
            entry.putInt(FROM_X, index.from().x());
            entry.putInt(FROM_Y, index.from().y());
            entry.putInt(FROM_Z, index.from().z());

            // Vetor de longs, e não uma lista de compostos: são umas
            // setecentas colunas por colônia, e cada composto custaria
            // um cabeçalho e um nome de chave por rua.
            long[] columns = new long[index.columns().size()];

            for (int i = 0; i < columns.length; i++) {
                columns[i] = index.columns().get(i);
            }

            entry.putLongArray(COLUMNS, columns);

            list.add(entry);
        }

        nbt.put(ROADS, list);
    }

    /**
     * Os índices do disco, sem os de colônia desconhecida.
     *
     * <p>É a fronteira do sistema, e desconfia de tudo o que lê. Save
     * anterior a 2026-08-27 não tem a chave e {@code getList} devolve
     * lista vazia — a colônia reabre sem mapa e varre o quadrado, que é o
     * comportamento de antes desta versão.
     *
     * <p><b>Índice vazio é recusado, e não é o mesmo que "vila sem
     * rua".</b> Ele afirma "varri o raio inteiro e não achei nenhuma
     * rua", e uma colônia que acreditasse nisso nunca mais procuraria
     * lote. Entrada sem dono cai pela mesma razão das casas e das minas:
     * seria mapa de vila nenhuma, guardado para sempre.
     */
    static List<ColonyRoads> read(NbtCompound nbt, Set<UUID> knownColonies) {
        List<ColonyRoads> found = new ArrayList<>();

        NbtList list = nbt.getList(ROADS, NbtElement.COMPOUND_TYPE);

        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);

            if (!entry.containsUuid(COLONY_ID)) {
                continue;
            }

            UUID colonyId = entry.getUuid(COLONY_ID);

            if (!knownColonies.contains(colonyId)) {
                continue;
            }

            long[] columns = entry.getLongArray(COLUMNS);

            if (columns.length == 0) {
                continue;
            }

            List<Long> packed = new ArrayList<>(columns.length);

            for (long column : columns) {
                packed.add(column);
            }

            found.add(new ColonyRoads(
                    colonyId,
                    new ColonyPos(
                            entry.getInt(FROM_X),
                            entry.getInt(FROM_Y),
                            entry.getInt(FROM_Z)),
                    packed));
        }

        return found;
    }
}
