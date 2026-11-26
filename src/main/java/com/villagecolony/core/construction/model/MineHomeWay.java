package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * O caminho entre a porta da casa do mineiro e a entrada da mina — Regra 386 (nota do jogo da 0.4.148, K5 🅱️: "o caminho
 * do arco da mina até a casa do mineiro tem que ter um caminho que una os dois e esse caminho deve ser unido até o grafo
 * mais perto existente"). Em L, com uma dobra só e degrau de no máximo um bloco, como a ligação em reta da Regra 224; o
 * trecho entra no grafo como caminho planejado, e a parte dele busca a linha a até {@link RoadLines#MAX_FAR_LINK} blocos.
 * Puro.
 */
public final class MineHomeWay {

    private MineHomeWay() {
    }

    /**
     * As colunas de {@code from} até {@code to}, sem a primeira e com a última; vazio quando nenhuma das duas dobras cabe.
     */
    public static List<ColonyPos> between(ColonyPos from, ColonyPos to, PathTerrain terrain) {
        for (boolean alongX : new boolean[] {true, false}) {
            List<ColonyPos> way = bend(from, to, alongX, terrain);

            if (!way.isEmpty()) {
                return way;
            }
        }

        return List.of();
    }

    private static List<ColonyPos> bend(ColonyPos from, ColonyPos to, boolean alongX, PathTerrain terrain) {
        List<ColonyPos> way = new ArrayList<>();
        int x = from.x();
        int z = from.z();
        int y = from.y();

        while (x != to.x() || z != to.z()) {
            boolean stepX = alongX ? x != to.x() : z == to.z();

            if (stepX) {
                x += Integer.signum(to.x() - x);
            } else {
                z += Integer.signum(to.z() - z);
            }

            OptionalInt ground = terrain.groundY(x, z, y);

            if (ground.isPresent() && Math.abs(ground.getAsInt() - y) <= 1) {
                y = ground.getAsInt();
            } else if (terrain.blocked(x, z) || !terrain.canCut(x, z, y)) {
                return List.of();
            }

            way.add(new ColonyPos(x, y, z));
        }

        return Math.abs(y - to.y()) <= 1 ? way : List.of();
    }
}
