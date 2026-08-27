package com.villagecolony.data.save;

import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A mina de cada colônia, indo e voltando do disco — Regra 29.
 *
 * <p>Sete campos: a colônia dona, a boca em três coordenadas, o lado da
 * descida, o lado da galeria e a fronteira já cavada. É pouco, e é
 * exatamente o que o mundo <b>não</b> guarda — os túneis abertos ficam
 * onde estão, mas nada neles diz onde a escada começa nem até onde a
 * picareta chegou.
 *
 * <p><b>Por que num arquivo separado.</b> {@code ColonySavedData} já
 * passa das quinhentas linhas que o projeto se impôs, e crescer nele
 * empurrava a dívida para a frente. Serialização de um agregado por
 * arquivo é a forma de o próximo agregado não repetir a conta.
 */
final class MineSave {

    private static final String MINES = "mines";
    private static final String COLONY_ID = "colonyId";
    private static final String ENTRY_X = "entryX";
    private static final String ENTRY_Y = "entryY";
    private static final String ENTRY_Z = "entryZ";
    private static final String DESCENT = "descent";
    private static final String GALLERY = "gallery";
    private static final String CUT = "cut";
    private static final String SHAPE = "shape";

    /**
     * Qual geometria de mina escreveu esta fronteira — 2026-08-27.
     *
     * <p>{@code cut} é um indice na ordem de cavar do {@code MineShaft},
     * e essa ordem ja mudou uma vez: o degrau passou de dois blocos para
     * tres, e o primeiro lance de vinte posicoes para trinta. Um numero
     * escrito numa ordem e lido noutra aponta para outro lugar, e a mina
     * retomaria no meio — deixando atras dela justamente os blocos que a
     * mudanca existe para abrir.
     *
     * <p><b>Sobe sempre que {@code positionAt} mudar de ordem.</b> Save
     * de forma diferente volta com a fronteira no zero, e nao com ela
     * traduzida: traduzir exigiria saber todas as formas antigas, e
     * recomecar custa pouco — o ja aberto e pulado de graca, 64 por
     * passagem.
     *
     * <p>Um, e nao zero, para o save anterior a esta chave — onde
     * {@code getInt} devolve zero — cair no ramo do recomeco.
     */
    private static final int SHAPE_VERSION = 2;

    private MineSave() {
    }

    static void write(NbtCompound nbt, Collection<Mine> mines) {
        NbtList list = new NbtList();

        for (Mine mine : mines) {
            NbtCompound entry = new NbtCompound();

            entry.putUuid(COLONY_ID, mine.colonyId());
            entry.putInt(ENTRY_X, mine.entry().x());
            entry.putInt(ENTRY_Y, mine.entry().y());
            entry.putInt(ENTRY_Z, mine.entry().z());
            entry.putString(DESCENT, mine.shaft().descent().name());
            entry.putString(GALLERY, mine.shaft().gallery().name());
            entry.putInt(CUT, mine.cut());
            entry.putInt(SHAPE, SHAPE_VERSION);

            list.add(entry);
        }

        nbt.put(MINES, list);
    }

    /**
     * As minas do disco, sem as de colônia desconhecida.
     *
     * <p>Mesma regra das casas e dos trabalhadores: mina órfã seria uma
     * escada de dono nenhum, guardada para sempre.
     *
     * <p>É a fronteira do sistema, e por isso desconfia de tudo o que lê.
     * Save anterior a 2026-08-20 não tem a chave e {@code getList}
     * devolve lista vazia — a colônia reabre sem mina e o mineiro abre
     * uma, que é o comportamento de antes desta versão. Entrada sem dono,
     * com lado que não existe ou com fronteira negativa é descartada: uma
     * mina meio lida mandaria o mineiro cavar em lugar nenhum.
     */
    static List<Mine> read(NbtCompound nbt, Set<UUID> knownColonies) {
        List<Mine> found = new ArrayList<>();

        NbtList list = nbt.getList(MINES, NbtElement.COMPOUND_TYPE);

        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);

            if (!entry.containsUuid(COLONY_ID)) {
                continue;
            }

            UUID colonyId = entry.getUuid(COLONY_ID);

            if (!knownColonies.contains(colonyId)) {
                continue;
            }

            Optional<Side> descent = sideNamed(entry.getString(DESCENT));
            Optional<Side> gallery = sideNamed(entry.getString(GALLERY));

            if (descent.isEmpty() || gallery.isEmpty()) {
                continue;
            }

            int cut = entry.getInt(CUT);

            if (cut < 0) {
                continue;
            }

            if (entry.getInt(SHAPE) != SHAPE_VERSION) {
                // Fronteira escrita noutra geometria. Ver SHAPE: a mina
                // volta inteira, so que do primeiro degrau.
                cut = 0;
            }

            ColonyPos entrance = new ColonyPos(
                    entry.getInt(ENTRY_X),
                    entry.getInt(ENTRY_Y),
                    entry.getInt(ENTRY_Z));

            found.add(Mine.restore(
                    colonyId,
                    new MineShaft(entrance, descent.get(), gallery.get()),
                    cut));
        }

        return found;
    }

    private static Optional<Side> sideNamed(String name) {
        for (Side side : Side.values()) {
            if (side.name().equals(name)) {
                return Optional.of(side);
            }
        }

        return Optional.empty();
    }
}
