package com.villagecolony.core.construction.service;

import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineShaft;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Uma mina por colônia — 2026-08-20.
 *
 * <p><b>Uma, e não uma por mineiro.</b> A mina é da colônia: dois
 * mineiros da mesma vila descem a mesma escada e avançam a mesma
 * fronteira. Antes disto cada trabalhador levava a sua cópia da geometria
 * e o seu cursor, e o segundo mineiro repetia bloco a bloco o caminho que
 * o primeiro já tinha aberto.
 *
 * <p>É o registro que o save grava, pelo mesmo motivo do
 * {@link BuildingRegistry}: sem ele a colônia reabre o mundo sem saber
 * onde está a própria mina.
 */
public final class MineRegistry {

    /** Ordem de inserção, para log e gravação reproduzíveis. */
    private final Map<UUID, Mine> mines = new LinkedHashMap<>();

    /**
     * Abre a mina desta colônia, ou devolve a que já existe.
     *
     * <p>Nunca substitui: uma segunda boca apagaria a fronteira da
     * primeira, e o mineiro recomeçaria a escada dentro da mina que ele
     * mesmo cavou.
     */
    public Mine open(UUID colonyId, MineShaft shaft) {
        Objects.requireNonNull(colonyId, "colonyId");
        Objects.requireNonNull(shaft, "shaft");

        return mines.computeIfAbsent(colonyId, id -> Mine.open(id, shaft));
    }

    /** Recoloca no registro a mina que o save trouxe. */
    public void restore(Mine mine) {
        Objects.requireNonNull(mine, "mine");

        mines.put(mine.colonyId(), mine);
    }

    /** A mina desta colônia, se ela já abriu uma. */
    public Optional<Mine> of(UUID colonyId) {
        if (colonyId == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(mines.get(colonyId));
    }

    /** Todas as minas, para gravar. */
    public Collection<Mine> all() {
        return List.copyOf(mines.values());
    }

    /**
     * Esquece a mina desta colônia.
     *
     * <p>Colônia abandonada, e o teste que se limpa pelo identificador
     * sem tocar no vizinho — ver {@code ColonyFixture}.
     */
    public void removeOfColony(UUID colonyId) {
        if (colonyId == null) {
            return;
        }

        mines.remove(colonyId);
    }

    /** Esvazia o registro. Chamado ao abrir e ao fechar o mundo. */
    public void clear() {
        mines.clear();
    }

    public int count() {
        return mines.size();
    }
}
