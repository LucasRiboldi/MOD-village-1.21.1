package com.villagecolony.data.save;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import net.minecraft.nbt.NbtCompound;

/**
 * A caixa da vila no save — ADR-003 Emenda 6, 2026-09-30.
 *
 * <p>Separada do {@link ColonySavedData}, que já passou de 500 linhas. Save
 * antigo não tem as chaves: a colônia volta sem caixa e é medida de novo na
 * primeira sonda com o chunk do centro carregado.
 */
final class VillageBoundsSave {

    static final String MIN_X = "boundsMinX";
    static final String MIN_Y = "boundsMinY";
    static final String MIN_Z = "boundsMinZ";
    static final String MAX_X = "boundsMaxX";
    static final String MAX_Y = "boundsMaxY";
    static final String MAX_Z = "boundsMaxZ";

    /**
     * A regra com que a caixa foi medida. A 2, de 2026-10-03, parte das peças
     * da vila gerada (sem a folga de 12 da estrutura), 15 em volta das obras e
     * lados ímpares. Caixa sem esta marca é da regra antiga e é medida de novo.
     */
    static final String MODEL = "boundsModel";

    static final int CURRENT_MODEL = 2;

    private VillageBoundsSave() {
    }

    static void write(NbtCompound entry, Colony colony) {
        colony.bounds().ifPresent(bounds -> {
            entry.putInt(MIN_X, bounds.minX());
            entry.putInt(MIN_Y, bounds.minY());
            entry.putInt(MIN_Z, bounds.minZ());
            entry.putInt(MAX_X, bounds.maxX());
            entry.putInt(MAX_Y, bounds.maxY());
            entry.putInt(MAX_Z, bounds.maxZ());
            entry.putInt(MODEL, CURRENT_MODEL);
        });
    }

    static void read(NbtCompound entry, Colony colony) {
        if (!entry.contains(MIN_X) || !entry.contains(MAX_Z) || entry.getInt(MODEL) != CURRENT_MODEL) {
            // Regra antiga ou save sem caixa: a colônia é medida de novo na
            // primeira sonda com o chunk do centro carregado (VillageMeasure).
            return;
        }

        colony.measure(new VillageBounds(
                entry.getInt(MIN_X), entry.getInt(MIN_Y), entry.getInt(MIN_Z),
                entry.getInt(MAX_X), entry.getInt(MAX_Y), entry.getInt(MAX_Z)));
    }
}
