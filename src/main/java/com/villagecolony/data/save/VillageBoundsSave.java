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
        });
    }

    static void read(NbtCompound entry, Colony colony) {
        if (!entry.contains(MIN_X) || !entry.contains(MAX_Z)) {
            return;
        }

        colony.measure(new VillageBounds(
                entry.getInt(MIN_X), entry.getInt(MIN_Y), entry.getInt(MIN_Z),
                entry.getInt(MAX_X), entry.getInt(MAX_Y), entry.getInt(MAX_Z)));
    }
}
