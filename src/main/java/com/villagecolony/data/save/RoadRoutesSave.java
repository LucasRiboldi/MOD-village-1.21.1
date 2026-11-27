package com.villagecolony.data.save;

import com.villagecolony.core.construction.model.NeighborhoodRoutes;
import com.villagecolony.core.construction.model.RoadEdgeState;
import com.villagecolony.core.construction.model.RoadLink;
import com.villagecolony.core.type.ColonyPos;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * As ligações do grafo planejado no save, ao lado do grafo da colônia — Regra 388, ADR-065 §8: o traçado, o estado e as
 * montagens iguais de cada par, e a última ponta de cada lado. Reabrir o mundo não muda o grafo. Entrada estragada é
 * ignorada, sem derrubar o resto.
 */
final class RoadRoutesSave {

    private static final String KEY = "routes";

    /** Teto de ligações lidas, contra save estragado. */
    private static final int MAX_LINKS = 4096;

    private RoadRoutesSave() {
    }

    /** As ligações guardadas neste grafo; nenhuma, se não há. */
    static NeighborhoodRoutes read(NbtCompound payload) {
        Map<List<Long>, RoadLink> links = new HashMap<>();
        Map<Integer, ColonyPos> tipEdges = new HashMap<>();
        Map<Integer, Long> tipDays = new HashMap<>();

        if (!payload.contains(KEY, NbtElement.COMPOUND_TYPE)) {
            return NeighborhoodRoutes.restore(links, tipEdges, tipDays);
        }

        NbtCompound routes = payload.getCompound(KEY);
        NbtList list = routes.getList("links", NbtElement.COMPOUND_TYPE);

        for (int i = 0; i < Math.min(list.size(), MAX_LINKS); i++) {
            NbtCompound entry = list.getCompound(i);
            int[] columns = entry.getIntArray("c");

            if (columns.length % 3 != 0) {
                continue;
            }

            try {
                RoadEdgeState state = RoadEdgeState.valueOf(entry.getString("s"));

                links.put(List.of(entry.getLong("a"), entry.getLong("b")),
                        new RoadLink(positions(columns), state, entry.getInt("n"), entry.getString("r")));
            } catch (IllegalArgumentException unknown) {
                // Estado de outra versão: a ligação é traçada de novo.
            }
        }

        NbtList tips = routes.getList("tips", NbtElement.COMPOUND_TYPE);

        for (int i = 0; i < tips.size(); i++) {
            NbtCompound tip = tips.getCompound(i);
            int[] at = tip.getIntArray("at");

            if (at.length == 3) {
                tipEdges.put(tip.getInt("side"), new ColonyPos(at[0], at[1], at[2]));
                tipDays.put(tip.getInt("side"), tip.getLong("day"));
            }
        }

        return NeighborhoodRoutes.restore(links, tipEdges, tipDays);
    }

    /** Grava as ligações neste grafo, no lugar das que havia. */
    static void put(NbtCompound payload, NeighborhoodRoutes routes) {
        NbtCompound out = new NbtCompound();
        NbtList list = new NbtList();

        routes.links().entrySet().stream().sorted(Map.Entry.comparingByKey((a, b) -> {
            int first = Long.compare(a.get(0), b.get(0));

            return first != 0 ? first : Long.compare(a.get(1), b.get(1));
        })).limit(MAX_LINKS).forEach(entry -> {
            NbtCompound link = new NbtCompound();

            link.putLong("a", entry.getKey().get(0));
            link.putLong("b", entry.getKey().get(1));
            link.putIntArray("c", coordinates(entry.getValue().columns()));
            link.putString("s", entry.getValue().state().name());
            link.putInt("n", entry.getValue().sameReads());
            link.putString("r", entry.getValue().reason());
            list.add(link);
        });
        out.put("links", list);

        NbtList tips = new NbtList();

        routes.tipEdges().forEach((side, at) -> {
            NbtCompound tip = new NbtCompound();

            tip.putInt("side", side);
            tip.putIntArray("at", new int[] {at.x(), at.y(), at.z()});
            tip.putLong("day", routes.tipDays().getOrDefault(side, 0L));
            tips.add(tip);
        });
        out.put("tips", tips);
        payload.put(KEY, out);
    }

    /** Passa as ligações do grafo antigo para o novo, na troca do grafo. */
    static void carry(NbtCompound from, NbtCompound to) {
        if (from.contains(KEY, NbtElement.COMPOUND_TYPE)) {
            to.put(KEY, from.getCompound(KEY).copy());
        }
    }

    private static List<ColonyPos> positions(int[] values) {
        List<ColonyPos> positions = new ArrayList<>();

        for (int i = 0; i < values.length; i += 3) {
            positions.add(new ColonyPos(values[i], values[i + 1], values[i + 2]));
        }

        return positions;
    }

    private static int[] coordinates(List<ColonyPos> positions) {
        int[] values = new int[positions.size() * 3];

        for (int i = 0; i < positions.size(); i++) {
            values[i * 3] = positions.get(i).x();
            values[i * 3 + 1] = positions.get(i).y();
            values[i * 3 + 2] = positions.get(i).z();
        }

        return values;
    }
}
