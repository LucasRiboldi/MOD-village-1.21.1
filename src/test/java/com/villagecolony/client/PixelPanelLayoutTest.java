package com.villagecolony.client;

import com.villagecolony.core.worker.model.ProfessionType;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PixelPanelLayoutTest {
    @Test
    void mapsStableProfessionAndConstructionIdentifiersToTheirOwnTextures() {
        for (ProfessionType profession : ProfessionType.values()) {
            String path = "textures/gui/overlays/professions/"
                    + profession.name().toLowerCase(java.util.Locale.ROOT) + ".png";
            assertEquals(Identifier.of("villagecolony", path),
                    PixelPanelLayout.professionTexture(profession.name()),
                    () -> "textura trocada para " + profession);
        }
        assertEquals(PixelPanelLayout.UNKNOWN, PixelPanelLayout.professionTexture("NOT_A_ROLE"));
        assertEquals(Identifier.of("villagecolony", "textures/gui/overlays/construction/building.png"),
                PixelPanelLayout.constructionTexture("BUILDING"));
        assertEquals(PixelPanelLayout.UNKNOWN, PixelPanelLayout.constructionTexture("UNKNOWN"));
    }

    @Test
    void centersTheProfessionIconAboveItsResponsiveFrame() {
        PixelPanelLayout panel = PixelPanelLayout.singleLine(
                PixelPanelLayout.professionTexture("BUILDER"), 83, true);

        assertTrue(panel.width() >= 83 + 2 * PixelPanelLayout.PADDING);
        assertEquals((panel.width() - PixelPanelLayout.ICON_SIZE) / 2, panel.iconX());
        assertTrue(panel.iconY() + PixelPanelLayout.ICON_SIZE <= 0);
        assertEquals((panel.width() - 83) / 2, panel.firstTextX());
        assertTrue(panel.hasFrame());
    }

    @Test
    void givesConstructionLinesOneResponsiveFrameAndOneIcon() {
        PixelPanelLayout panel = PixelPanelLayout.twoLines(
                PixelPanelLayout.constructionTexture("WAITING_RESOURCES"), 42, 117);

        assertTrue(panel.width() >= 117 + 2 * PixelPanelLayout.PADDING);
        assertTrue(panel.height() > PixelPanelLayout.SINGLE_LINE_HEIGHT);
        assertEquals((panel.width() - PixelPanelLayout.ICON_SIZE) / 2, panel.iconX());
        assertEquals((panel.width() - 42) / 2, panel.firstTextX());
        assertEquals((panel.width() - 117) / 2, panel.secondTextX());
    }

    @Test
    void hidesTheProfessionFrameTogetherWithItsNameButKeepsTheIcon() {
        PixelPanelLayout panel = PixelPanelLayout.singleLine(
                PixelPanelLayout.professionTexture("MASON"), 64, false);

        assertEquals(PixelPanelLayout.ICON_SIZE, panel.width());
        assertTrue(!panel.hasFrame());
    }
}
