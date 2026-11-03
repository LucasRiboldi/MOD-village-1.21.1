package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkAreaTest {
    @Test
    void inclusiveBoundsDetectTouchingAndRejectOutsidePoints() {
        var area = new WorkArea(new ColonyPos(-2, 0, -2), new ColonyPos(2, 2, 2));
        assertEquals(75, area.volume());
        assertTrue(area.contains(area));
        assertTrue(area.contains(new ColonyPos(2, 2, 2)));
        assertFalse(area.contains(new ColonyPos(2, 3, 2)));
        assertTrue(area.intersects(new WorkArea(new ColonyPos(2, 2, 2), new ColonyPos(4, 4, 4))));
        assertFalse(area.intersects(new WorkArea(new ColonyPos(3, 0, 0), new ColonyPos(4, 1, 1))));
    }

    @Test
    void invertedCoordinatesAndOverflowNeverCreateValidVolumes() {
        assertThrows(IllegalArgumentException.class,
                () -> new WorkArea(new ColonyPos(1, 0, 0), new ColonyPos(0, 0, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> new WorkArea(new ColonyPos(Integer.MIN_VALUE, 0, 0), new ColonyPos(0, 0, 0)));
        var enormous = new WorkArea(new ColonyPos(-29_999_000, -2048, -29_999_000),
                new ColonyPos(29_999_000, 2047, 29_999_000));
        assertThrows(ArithmeticException.class, enormous::volume);
    }
}
