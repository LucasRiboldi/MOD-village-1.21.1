package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;

import java.util.List;

/**
 * Uma ligação do grafo planejado entre dois nós — ADR-065 §6.
 *
 * @param columns o traçado, de um nó ao outro; vazio quando {@link RoadEdgeState#FAILED}
 * @param state o estado
 * @param sameReads em quantas montagens seguidas o traçado ficou igual, até {@link RoadStability#READS}
 * @param reason por que falhou ({@code closed}: sem passagem; {@code exhausted}: a busca passou do teto); vazio fora disso
 */
public record RoadLink(List<ColonyPos> columns, RoadEdgeState state, int sameReads, String reason) {

    public RoadLink {
        columns = List.copyOf(columns);
    }

    static RoadLink planned(List<ColonyPos> columns) {
        return new RoadLink(columns, RoadEdgeState.PLANNED, 1, "");
    }

    static RoadLink failed(String reason) {
        return new RoadLink(List.of(), RoadEdgeState.FAILED, 0, reason);
    }

    /** A mesma ligação, usada mais uma vez: trava na {@link RoadStability#READS}ª montagem igual. */
    RoadLink reused() {
        if (state == RoadEdgeState.FAILED) {
            return this;
        }

        int reads = Math.min(RoadStability.READS, sameReads + 1);

        return new RoadLink(columns, reads >= RoadStability.READS && state == RoadEdgeState.PLANNED ? RoadEdgeState.LOCKED
                : state, reads, "");
    }

    /** O estado pelo mundo: calçada quando toda coluna é caminho; a calçada que perdeu caminho volta a travada. */
    RoadLink paved(boolean everyColumnIsPath) {
        if (state == RoadEdgeState.LOCKED && everyColumnIsPath) {
            return new RoadLink(columns, RoadEdgeState.PAVED, sameReads, "");
        }

        if (state == RoadEdgeState.PAVED && !everyColumnIsPath) {
            return new RoadLink(columns, RoadEdgeState.LOCKED, sameReads, "");
        }

        return this;
    }
}
