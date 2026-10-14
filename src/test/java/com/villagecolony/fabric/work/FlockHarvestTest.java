package com.villagecolony.fabric.work;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A regra do abate do excedente do rebanho — decisão do autor, 2026-10-14: o pastor colhe couro e
 * carne do que passa de {@link FlockHarvest#HARVEST_KEEP} adultos de uma espécie, um por ciclo,
 * deixando sempre a reserva viva. Lógica pura, sem mundo.
 */
class FlockHarvestTest {

    @Test
    void nothingToHarvestAtOrBelowTheLiveReserve() {
        assertFalse(FlockHarvest.hasSurplus(FlockHarvest.HARVEST_KEEP), "no teto da reserva não abate");
        assertFalse(FlockHarvest.hasSurplus(FlockHarvest.HARVEST_KEEP - 1), "abaixo da reserva não abate");
        assertFalse(FlockHarvest.hasSurplus(0), "rebanho vazio não abate");
    }

    @Test
    void oneAboveTheReserveIsSurplus() {
        assertTrue(FlockHarvest.hasSurplus(FlockHarvest.HARVEST_KEEP + 1), "um acima da reserva é excedente");
        assertTrue(FlockHarvest.hasSurplus(FlockHarvest.FLOCK_FULL), "o rebanho cheio tem excedente");
    }

    @Test
    void harvestingNeverDropsBelowTheReserve() {
        // Abate 1 por ciclo; o excedente é adults - HARVEST_KEEP, nunca negativo.
        assertEquals(0, FlockHarvest.surplus(FlockHarvest.HARVEST_KEEP), "sem excedente no teto");
        assertEquals(1, FlockHarvest.surplus(FlockHarvest.HARVEST_KEEP + 1), "um acima = 1 de excedente");
        assertEquals(FlockHarvest.FLOCK_FULL - FlockHarvest.HARVEST_KEEP,
                FlockHarvest.surplus(FlockHarvest.FLOCK_FULL), "cheio = faixa de colheita");
        assertEquals(0, FlockHarvest.surplus(FlockHarvest.HARVEST_KEEP - 3), "abaixo da reserva: zero, nunca negativo");
    }
}
