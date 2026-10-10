package com.villagecolony.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PixelPanelLayoutTest {

    @Test
    void professionNameStaysInsideItsBackground() {
        PixelPanelLayout panel = PixelPanelLayout.withText(new float[] {83.0F}, 10.0F);

        assertTrue(panel.hasFrame());
        assertTrue(panel.frameLeft() <= panel.firstTextLeft());
        assertTrue(panel.firstTextLeft() + 83.0F <= panel.frameRight());
        assertTrue(panel.iconBottom() <= panel.frameTop() - PixelPanelLayout.GAP);
        assertEquals((panel.width() - PixelPanelLayout.ICON_SIZE) / 2.0F, panel.iconLeft());
    }

    @Test
    void hiddenProfessionNameKeepsOnlyTheIcon() {
        PixelPanelLayout panel = PixelPanelLayout.iconOnly();

        assertTrue(!panel.hasFrame());
        assertEquals(PixelPanelLayout.ICON_SIZE, panel.width());
        assertEquals(-PixelPanelLayout.ICON_SIZE, panel.iconTop());
    }

    @Test
    void twoLineConstructionPanelKeepsBothLinesInsideTheSameBackground() {
        PixelPanelLayout panel = PixelPanelLayout.withText(new float[] {42.0F, 117.0F}, 10.0F);

        assertTrue(panel.firstTextLeft() + 42.0F <= panel.frameRight());
        assertTrue(panel.secondTextLeft() + 117.0F <= panel.frameRight());
        assertTrue(panel.frameWidth() >= 117.0F + 2.0F * PixelPanelLayout.PADDING);
    }
}
