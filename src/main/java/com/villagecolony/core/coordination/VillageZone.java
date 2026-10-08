package com.villagecolony.core.coordination;

import com.villagecolony.core.colony.model.VillageBounds;

/**
 * Em que faixa da vila uma coluna fica — pedido do autor, 2026-10-08, para o
 * lenhador: <i>"varrer primeiro as árvores centrais da vila, depois as
 * intermediárias da área da vila, depois as das bordas; se não houver nenhuma
 * nessas, ele pode ir um pouco fora da vila"</i>.
 *
 * <p>A distância é medida do centro da caixa da vila (ADR-003 Emenda 6) e dividida
 * pela meia-largura de cada eixo, então a caixa comprida tem faixas compridas: o
 * centro é o terço de dentro, a intermediária o terço do meio, a borda o terço de
 * fora. "Um pouco fora" é a margem de {@link ResourceReach}; além dela, nada.
 */
public enum VillageZone {

    CENTER(0),
    MIDDLE(1),
    EDGE(2),
    OUTSIDE(3),
    BEYOND(4);

    private final int rank;

    VillageZone(int rank) {
        this.rank = rank;
    }

    /** A ordem de busca: menor vem antes. */
    public int rank() {
        return rank;
    }

    /** A faixa da coluna {@code (x, z)}, com {@code margin} blocos de "um pouco fora". */
    public static VillageZone of(VillageBounds box, int x, int z, int margin) {
        if (!box.containsColumn(x, z)) {
            return box.containsColumn(x, z, margin) ? OUTSIDE : BEYOND;
        }

        double halfX = Math.max(1, box.sizeX() / 2.0);
        double halfZ = Math.max(1, box.sizeZ() / 2.0);
        double spread = Math.max(
                Math.abs(x - box.centerX()) / halfX,
                Math.abs(z - box.centerZ()) / halfZ);

        if (spread <= 1.0 / 3) {
            return CENTER;
        }

        return spread <= 2.0 / 3 ? MIDDLE : EDGE;
    }

    /** Se o lenhador pode ir até aqui. */
    public boolean withinReach() {
        return this != BEYOND;
    }
}
