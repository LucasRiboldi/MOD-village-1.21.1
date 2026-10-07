package com.villagecolony.client;

/** Preferências locais de leitura dos painéis de mundo. */
public final class OverlayPreferences {
    private static boolean professionTextVisible = true;

    private OverlayPreferences() {
    }

    public static boolean professionTextVisible() {
        return professionTextVisible;
    }

    public static void toggleProfessionTextVisible() {
        professionTextVisible = !professionTextVisible;
    }
}
