package com.villagecolony.fabric.integration;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A placa de obra órfã sai do mundo quando volta a carregar — 2026-09-30. */
class SiteSignJanitorTest {

    private static final UUID PROJECT = UUID.fromString("5f0c7c52-6b0e-4f6f-9d7a-1b2c3d4e5f60");
    private static final UUID SELF = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void aSignWithoutProjectTagIsDiscarded() {
        // Toda placa das versões anteriores: não há obra a quem perguntar.
        assertEquals(SiteSignJanitor.Fate.DISCARD,
                SiteSignJanitor.judge(Optional.empty(), false, SELF, null, false));
    }

    @Test
    void aSignOfAClosedOrForgottenProjectIsDiscarded() {
        // A placa que o servidor esqueceu ao reiniciar, ou que estava
        // descarregada quando a obra fechou.
        assertEquals(SiteSignJanitor.Fate.DISCARD,
                SiteSignJanitor.judge(Optional.of(PROJECT), false, SELF, null, false));
        assertEquals(SiteSignJanitor.Fate.DISCARD,
                SiteSignJanitor.judge(Optional.of(PROJECT), false, SELF, SELF, true));
    }

    @Test
    void theSignOfAnOpenProjectStays() {
        assertEquals(SiteSignJanitor.Fate.KEEP,
                SiteSignJanitor.judge(Optional.of(PROJECT), true, SELF, null, false));
        assertEquals(SiteSignJanitor.Fate.KEEP,
                SiteSignJanitor.judge(Optional.of(PROJECT), true, SELF, SELF, true));
    }

    @Test
    void aSecondSignForTheSameProjectIsDiscardedWhileTheFirstStands() {
        assertEquals(SiteSignJanitor.Fate.DISCARD,
                SiteSignJanitor.judge(Optional.of(PROJECT), true, SELF, OTHER, true));
    }

    @Test
    void aSecondSignTakesOverWhenTheKnownOneIsNotInTheWorld() {
        assertEquals(SiteSignJanitor.Fate.KEEP,
                SiteSignJanitor.judge(Optional.of(PROJECT), true, SELF, OTHER, false));
    }

    @Test
    void theProjectIsReadBackFromTheTag() {
        assertEquals(Optional.of(PROJECT),
                SiteSignJanitor.parseProject(SiteSignJanitor.PROJECT_TAG_PREFIX + PROJECT));
    }

    @Test
    void aMalformedOrForeignTagHasNoProject() {
        assertEquals(Optional.empty(),
                SiteSignJanitor.parseProject(SiteSignJanitor.PROJECT_TAG_PREFIX + "not-a-uuid"));
        assertEquals(Optional.empty(), SiteSignJanitor.parseProject("villagecolony_site_sign"));
    }
}
