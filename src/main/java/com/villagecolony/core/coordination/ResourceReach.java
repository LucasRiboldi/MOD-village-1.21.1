package com.villagecolony.core.coordination;

import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;

import java.util.Objects;

/**
 * Até onde a busca de recurso vai, e de onde ela parte — ADR-036 item 18.
 *
 * <p>O limite é a caixa da vila mais {@value #EDGE_MARGIN} blocos. As buscas
 * alternam: a de vez par parte do centro, com raio que cobre o limite; a de vez
 * ímpar parte do meio de uma borda do limite, em rodízio horário a partir do
 * norte, com raio de meio limite. Achado fora do limite não vale.
 *
 * <p>Sem caixa medida, vale o {@link GatheringReach} de antes, a partir do centro.
 */
public final class ResourceReach {

    /** Quanto a busca passa da borda da vila. */
    public static final int EDGE_MARGIN = 10;

    private static final Side[] EDGES = {Side.NORTH, Side.EAST, Side.SOUTH, Side.WEST};

    /** Uma busca: de onde parte e até que distância dali. */
    public record Search(ColonyPos origin, int radius) {
    }

    private ResourceReach() {
    }

    /** A caixa da vila com a margem de busca. */
    public static VillageBounds limit(VillageBounds box) {
        Objects.requireNonNull(box, "box");

        return new VillageBounds(
                box.minX() - EDGE_MARGIN, box.minY(), box.minZ() - EDGE_MARGIN,
                box.maxX() + EDGE_MARGIN, box.maxY(), box.maxZ() + EDGE_MARGIN);
    }

    /** Se a coluna está dentro do limite de busca desta vila. */
    public static boolean within(VillageBounds box, int x, int z) {
        return box.containsColumn(x, z, EDGE_MARGIN);
    }

    /**
     * A busca da vez {@code turn}.
     *
     * @param centre o centro da vila; a altura dele vai para a origem
     */
    public static Search search(VillageBounds box, ColonyPos centre, int turn) {
        VillageBounds limit = limit(box);

        if (Math.floorMod(turn, 2) == 0) {
            int radius = Math.max(
                    Math.max(centre.x() - limit.minX(), limit.maxX() - centre.x()),
                    Math.max(centre.z() - limit.minZ(), limit.maxZ() - centre.z()));

            return new Search(centre, Math.max(1, radius));
        }

        Side edge = EDGES[Math.floorMod(turn / 2, EDGES.length)];
        ColonyPos origin = switch (edge) {
            case NORTH -> new ColonyPos(centre.x(), centre.y(), limit.minZ());
            case EAST -> new ColonyPos(limit.maxX(), centre.y(), centre.z());
            case SOUTH -> new ColonyPos(centre.x(), centre.y(), limit.maxZ());
            case WEST -> new ColonyPos(limit.minX(), centre.y(), centre.z());
        };

        return new Search(origin, Math.max(1, Math.max(limit.sizeX(), limit.sizeZ()) / 2));
    }
}
