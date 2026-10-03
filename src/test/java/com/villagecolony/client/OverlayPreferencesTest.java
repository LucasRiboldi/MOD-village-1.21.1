package com.villagecolony.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

class OverlayPreferencesTest {
    @Test
    void togglesOnlyTheLocalProfessionTextPreference() {
        boolean original = OverlayPreferences.professionTextVisible();

        try {
            OverlayPreferences.toggleProfessionTextVisible();

            assertNotEquals(original, OverlayPreferences.professionTextVisible());
        } finally {
            if (OverlayPreferences.professionTextVisible() != original) {
                OverlayPreferences.toggleProfessionTextVisible();
            }
        }
    }
}
