package com.villagecolony.client;

import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.worker.model.ProfessionType;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cada símbolo que o overlay pede existe nos recursos — o autor não viu a arte
 * em 2026-10-03 porque nenhum código a usava.
 */
class OverlaySpritesTest {

    private static final Path ASSETS = Path.of("src/main/resources/assets", OverlaySprites.NAMESPACE);

    @Test
    void everyProfessionHasItsSimpleSymbol() {
        for (ProfessionType profession : ProfessionType.values()) {
            Path icon = ASSETS.resolve(OverlaySprites.profession(profession.name()));

            assertTrue(Files.isRegularFile(icon), profession + " sem ícone: " + icon);
        }
    }

    @Test
    void everyConstructionStateHasItsSymbol() {
        for (ConstructionState state : ConstructionState.values()) {
            for (String missing : new String[] {"", "minecraft:glass_pane 4"}) {
                Path icon = ASSETS.resolve(OverlaySprites.construction(state.name(), missing));

                assertTrue(Files.isRegularFile(icon), state + " sem ícone: " + icon);
            }
        }
    }

    @Test
    void aMissingPieceShowsTheWaitingIcon() {
        assertEquals("textures/gui/overlays/construction/waiting_material.png",
                OverlaySprites.construction("BUILDING", "minecraft:glass_pane 4"));
        assertEquals("textures/gui/overlays/construction/building.png",
                OverlaySprites.construction("BUILDING", ""));
    }

    @Test
    void theSignShowsTheShortBlueprintName() {
        assertEquals("plains shepherds house 1",
                OverlaySprites.shortName("minecraft:village/plains/houses/plains_shepherds_house_1"));
        assertEquals("big house mod", OverlaySprites.shortName("villagecolony:houses/big_house_mod"));
    }
}
