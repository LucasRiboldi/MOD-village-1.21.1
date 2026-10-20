package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O recurso que a vila provou não ter no alcance — regra do autor, 2026-10-20.
 *
 * <p>Depois das buscas vazias que fazem o material aparecer ({@link LocateFallback}), a ausência fica
 * registrada: a próxima vez que o fundidor precisar, o material aparece no baú dele direto, sem novas
 * viagens. Antes, a entrega zerava a contagem e cada novo lote custava mais quatro buscas inúteis
 * (playtest 0.3.64: 82 buscas vazias para 20 entregas de argila).
 */
class KnownAbsentTest {

    private static final UUID COLONY = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @BeforeEach
    void forget() {
        KnownAbsent.clearAll();
    }

    @Test
    void nothingIsAbsentBeforeItIsRegistered() {
        assertFalse(KnownAbsent.isAbsent(COLONY, ResourceType.CLAY, 1_000),
                "sem registro, a argila ainda é procurada");
    }

    @Test
    void aRegisteredAbsenceHoldsForItsWholeWindow() {
        KnownAbsent.register(COLONY, ResourceType.CLAY, 1_000);

        assertTrue(KnownAbsent.isAbsent(COLONY, ResourceType.CLAY, 1_000), "logo depois do registro");
        assertTrue(KnownAbsent.isAbsent(COLONY, ResourceType.CLAY, 1_000 + KnownAbsent.FOR_TICKS - 1),
                "no último tique da janela");
    }

    @Test
    void theAbsenceExpiresSoTheReachIsCheckedAgain() {
        KnownAbsent.register(COLONY, ResourceType.CLAY, 1_000);

        assertFalse(KnownAbsent.isAbsent(COLONY, ResourceType.CLAY, 1_000 + KnownAbsent.FOR_TICKS),
                "vencida a janela, a vila volta a procurar (o mundo pode ter mudado)");
    }

    @Test
    void itIsPerResourceAndPerColony() {
        KnownAbsent.register(COLONY, ResourceType.CLAY, 1_000);

        assertFalse(KnownAbsent.isAbsent(COLONY, ResourceType.SAND, 1_000), "outro recurso não herda");
        assertFalse(KnownAbsent.isAbsent(UUID.randomUUID(), ResourceType.CLAY, 1_000), "outra colônia não herda");
    }

    @Test
    void findingTheResourceClearsTheAbsence() {
        KnownAbsent.register(COLONY, ResourceType.CLAY, 1_000);
        KnownAbsent.found(COLONY, ResourceType.CLAY);

        assertFalse(KnownAbsent.isAbsent(COLONY, ResourceType.CLAY, 1_001), "achou: deixa de ser ausente");
    }
}
