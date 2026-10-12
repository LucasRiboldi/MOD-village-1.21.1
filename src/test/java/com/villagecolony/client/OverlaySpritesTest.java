package com.villagecolony.client;

import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.worker.model.ProfessionType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
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

    @Test
    void theSignNameSplitsKindAndBiome() {
        OverlaySprites.BlueprintLabel house =
                OverlaySprites.label("minecraft:village/plains/houses/plains_medium_house_1");

        assertEquals("medium_house", house.kind());
        assertEquals("plains", house.biome());
        assertEquals("shepherds_house", OverlaySprites.label("minecraft:village/desert/houses/desert_shepherd_house_1").kind());
        assertEquals("lamp", OverlaySprites.label("minecraft:village/snowy/snowy_lamp_post_01").kind());
        assertEquals("", OverlaySprites.label("villagecolony:houses/big_house_mod").biome());
        assertEquals("big house mod", OverlaySprites.label("villagecolony:houses/big_house_mod").fallback());
    }

    /** Pedido do autor (2026-10-11): toda peça das vilas Vanilla com nome em português. */
    @Test
    void everyVanillaVillagePieceHasAPortugueseName() throws IOException {
        String ptPt = Files.readString(ASSETS.resolve("lang/pt_pt.json"));
        String[] pieces = {
            "plains_small_house_1",
            "plains_medium_house_2",
            "plains_big_house_1",
            "plains_butcher_shop_1",
            "plains_tool_smith_1",
            "plains_fletcher_house_1",
            "plains_shepherds_house_1",
            "plains_armorer_house_1",
            "plains_fisher_cottage_1",
            "plains_tannery_1",
            "plains_cartographer_1",
            "plains_library_1",
            "plains_masons_house_1",
            "plains_weaponsmith_1",
            "plains_temple_3",
            "plains_stable_1",
            "plains_large_farm_1",
            "plains_small_farm_1",
            "plains_animal_pen_1",
            "plains_accessory_1",
            "plains_meeting_point_1",
            "plains_lamp_1",
            "desert_shepherd_house_1",
            "desert_armorer_1",
            "desert_fisher_1",
            "desert_cartographer_house_1",
            "desert_mason_1",
            "desert_farm_1",
            "desert_camel_pen_1",
            "savanna_butchers_shop_1",
            "savanna_shepherd_1",
            "snowy_fisherman_cottage_1",
            "snowy_weapon_smith_1",
            "snowy_lamp_post_01",
            "snowy_igloo",
            "taiga_large_house_1"
        };

        for (String piece : pieces) {
            OverlaySprites.BlueprintLabel label = OverlaySprites.label("minecraft:village/x/houses/" + piece);

            assertTrue(ptPt.contains("\"overlay.building." + label.kind() + "\""), piece + " sem nome: " + label.kind());
            assertTrue(label.biome().isEmpty() || ptPt.contains("\"overlay.biome." + label.biome() + "\""),
                    piece + " sem bioma: " + label.biome());
        }
    }

    @Test
    void theMissingMaterialSplitsItemAndCount() {
        assertEquals("minecraft:glass_pane", OverlaySprites.missingParts("minecraft:glass_pane 4")[0]);
        assertEquals("4", OverlaySprites.missingParts("minecraft:glass_pane 4")[1]);
        assertEquals("", OverlaySprites.missingParts("minecraft:white_bed")[1]);
    }
}
