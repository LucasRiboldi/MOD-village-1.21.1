package com.villagecolony.core.construction.model;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Como o grafo de ruas é montado — ADR-057 (autor, 2026-11-18): três modos, um ligado por vez, cada um com a cor da aresta
 * no overlay das ruas.
 */
public enum RoadGraphMode {

    /** O grafo de sempre: afina os blocos de caminho da caixa inteira a cada releitura (ADR-043, ADR-050). */
    ATUAL("atual", 0xFFFF00),

    /** O grafo de sempre, refeito só nos quadrados de 16 × 16 que mudaram; o resto fica como estava. */
    REGIOES("regioes", 0xFF8C00),

    /** Plano primeiro: vizinhança relativa entre as entradas, cada ligação por A* com custo de terreno. */
    VIZINHANCA("vizinhanca", 0xA040FF);

    private final String id;

    private final int edgeColor;

    RoadGraphMode(String id, int edgeColor) {
        this.id = id;
        this.edgeColor = edgeColor;
    }

    /** O nome do modo no comando e no log. */
    public String id() {
        return id;
    }

    /** A cor da aresta deste modo no overlay das ruas, em RGB. */
    public int edgeColor() {
        return edgeColor;
    }

    /** O modo deste nome, sem diferença de maiúscula; vazio se não existe. */
    public static Optional<RoadGraphMode> parse(String name) {
        String wanted = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);

        return Arrays.stream(values()).filter(mode -> mode.id.equals(wanted)).findFirst();
    }
}
