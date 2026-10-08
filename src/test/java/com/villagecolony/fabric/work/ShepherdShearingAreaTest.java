package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.type.ColonyPos;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O pastor procura ovelha com lã onde o rebanho é contado: na colônia
 * 33a6b9c4 o curral ficava 43 blocos ao norte do centro, fora do raio de 32.
 */
class ShepherdShearingAreaTest {

    @BeforeAll
    static void bootMinecraft() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
    }

    /** O curral do playtest, em volta do centro (0, 64, 0). */
    private static final Vec3d PEN = new Vec3d(-11.5, 64, 43.5);

    @Test
    void theMeasuredVillageReachesThePenOutsideTheRadius() {
        Colony colony = Colony.create(UUID.randomUUID(), new ColonyPos(0, 64, 0));
        colony.measure(new VillageBounds(-57, 50, -67, 57, 80, 67));

        Box area = ShepherdFlock.shearingArea(colony, new BlockPos(colony.center().x(), 64,
                colony.center().z()), 32);

        assertTrue(area.contains(PEN), "o curral dentro da vila ficou fora da busca: " + area);
    }

    @Test
    void withoutAMeasuredVillageTheRadiusIsTheWholeSearch() {
        Colony colony = Colony.create(UUID.randomUUID(), new ColonyPos(0, 64, 0));

        Box area = ShepherdFlock.shearingArea(colony, new BlockPos(0, 64, 0), 32);

        assertFalse(area.contains(PEN), "sem vila medida a busca devia ser só o raio");
        assertTrue(area.contains(new Vec3d(20.5, 64, -20.5)));
    }

    @Test
    void aShortTestRadiusStaysShortWithoutAVillage() {
        // As arenas de GameTest não têm vila medida: o raio encurtado continua valendo.
        Colony colony = Colony.create(UUID.randomUUID(), new ColonyPos(0, 64, 0));

        Box area = ShepherdFlock.shearingArea(colony, new BlockPos(0, 64, 0), 4);

        assertFalse(area.contains(new Vec3d(8.5, 64, 0.5)));
    }
}
