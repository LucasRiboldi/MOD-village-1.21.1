package com.villagecolony.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PixelPanelLayoutTest {

    @Test
    void professionNameAndIconStayInsideTheSameBackground() {
        PixelPanelLayout panel = PixelPanelLayout.withText(new float[] {83.0F}, 10.0F);

        assertTrue(panel.hasFrame());
        assertTrue(panel.frameLeft() <= panel.firstTextLeft());
        assertTrue(panel.firstTextLeft() + 83.0F <= panel.frameRight());
        assertTrue(panel.frameLeft() <= panel.iconLeft());
        assertTrue(panel.iconRight() <= panel.frameRight());
        assertTrue(panel.frameTop() <= panel.iconTop());
        assertTrue(panel.iconBottom() <= panel.frameBottom());
        assertTrue(panel.firstTextLeft() + 83.0F + PixelPanelLayout.GAP <= panel.iconLeft());
        assertTrue(panel.frameWidth() >= 83.0F
                + PixelPanelLayout.GAP
                + PixelPanelLayout.ICON_SIZE
                + 2.0F * PixelPanelLayout.PADDING);
    }

    @Test
    void hiddenProfessionNameKeepsOnlyTheIcon() {
        PixelPanelLayout panel = PixelPanelLayout.iconOnly();

        assertTrue(!panel.hasFrame());
        assertEquals(PixelPanelLayout.ICON_SIZE, panel.width());
        assertEquals(-PixelPanelLayout.ICON_SIZE, panel.iconTop());
    }

    @Test
    void twoLineConstructionPanelKeepsBothLinesAndIconInsideTheSameBackground() {
        PixelPanelLayout panel = PixelPanelLayout.withText(new float[] {42.0F, 117.0F}, 10.0F);

        assertTrue(panel.firstTextLeft() + 42.0F <= panel.frameRight());
        assertTrue(panel.secondTextLeft() + 117.0F <= panel.frameRight());
        assertTrue(panel.frameLeft() <= panel.iconLeft());
        assertTrue(panel.iconRight() <= panel.frameRight());
        assertTrue(panel.frameTop() <= panel.iconTop());
        assertTrue(panel.iconBottom() <= panel.frameBottom());
        assertTrue(panel.frameWidth() >= 117.0F
                + PixelPanelLayout.GAP
                + PixelPanelLayout.ICON_SIZE
                + 2.0F * PixelPanelLayout.PADDING);
    }
}
