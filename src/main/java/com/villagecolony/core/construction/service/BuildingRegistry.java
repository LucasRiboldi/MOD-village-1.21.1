package com.villagecolony.core.construction.service;

import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ColonyPos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * O que a colônia construiu — TASK-036 e TASK-037.
 *
 * <p>Fase 11. É o registro que sobrevive à obra: o canteiro sai de
 * {@code ConstructionService} quando termina, e o que fica é isto.
 *
 * <p><b>Por que ele não é opcional.</b> Três coisas do projeto dependem
 * de saber que um bloco é da colônia:
 *
 * <ul>
 *   <li>a proteção — PROJECT_CONSTITUTION.md §10 diz que infraestrutura
 *       da colônia é permanente, e sem este registro "permanente" não
 *       tem como ser verificado;
 *   <li>a fusão de vilas decidida em 2026-08-12 — duas viram uma quando
 *       um bloco de uma encostar no bloco da outra;
 *   <li>a escolha do próximo lote, que não pode cair em cima do
 *       anterior.
 * </ul>
 *
 * <p>É por isso que a Fase 11 caminha junto da TASK-035 e não depois
 * dela: colocar o bloco e dizer de quem ele é são o mesmo momento.
 */
public final class BuildingRegistry {

    /** Ordem de inserção, para log e iteração reproduzíveis. */
    private final Map<UUID, Building> buildings = new LinkedHashMap<>();

    public void register(Building building) {
        Objects.requireNonNull(building, "building");

        buildings.put(building.id(), building);
    }

    /** A construção que ocupa esta posição, se alguma. */
    public Optional<Building> at(ColonyPos pos) {
        if (pos == null) {
            return Optional.empty();
        }

        for (Building building : buildings.values()) {
            if (building.contains(pos)) {
                return Optional.of(building);
            }
        }

        return Optional.empty();
    }

    /**
     * Se esta posição é infraestrutura da colônia — TASK-037.
     *
     * <p>A pergunta que a proteção faz. Todo bloco dentro da caixa de uma
     * construção tem origem "Colony Infrastructure", e é assim que o
     * trabalhador sabe não desmanchar o que a vila levantou.
     */
    public boolean isColonyInfrastructure(ColonyPos pos) {
        return at(pos).isPresent();
    }

    public List<Building> ofColony(UUID colonyId) {
        List<Building> found = new ArrayList<>();

        if (colonyId == null) {
            return found;
        }

        for (Building building : buildings.values()) {
            if (building.colonyId().equals(colonyId)) {
                found.add(building);
            }
        }

        return found;
    }

    /**
     * As construções de <b>outra</b> colônia que encostam nesta.
     *
     * <p>A pergunta da fusão, pronta para o dia em que ela for
     * implementada. Hoje ninguém a faz — e é melhor que ela exista aqui,
     * ao lado do que a responde, do que seja reinventada mais tarde
     * dentro do código que funde.
     */
    public List<Building> foreignNeighboursOf(Building building) {
        Objects.requireNonNull(building, "building");

        List<Building> found = new ArrayList<>();

        for (Building other : buildings.values()) {
            if (other.colonyId().equals(building.colonyId())) {
                continue;
            }

            if (other.touches(building)) {
                found.add(other);
            }
        }

        return found;
    }

    /** Todas, em ordem de registro. Somente leitura. */
    public Collection<Building> all() {
        return Collections.unmodifiableCollection(buildings.values());
    }

    public int count() {
        return buildings.size();
    }

    /**
     * Esvazia o registro. Usado ao descarregar o mundo.
     *
     * <p>Não apaga casa alguma do mundo: apaga a memória de que ela é da
     * colônia. Enquanto a persistência da Fase 11 não existir, essa
     * memória morre ao fechar o jogo — ver Project-State §9.
     */
    public void clear() {
        buildings.clear();
    }
}
