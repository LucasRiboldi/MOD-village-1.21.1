package com.villagecolony.core.coordination;

import com.villagecolony.core.type.ResourceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * ADR-062, Regra 340: o cru não tem mais reserva de metade (Regra 5) nem de um terço (Regra 248), e a exceção da
 * areia e da argila (Regra 257) deixa de ser exceção. A tora vira tábua até a
 * reserva da tábua, e o cru inteiro pode ir à fornalha; o que segura a conversão é a reserva pequena do fabricado.
 */
class RawWithoutReserveTest {

    @Test
    void theWholeRawMayBeSmelted() {
        assertEquals(100, StockRules.rawThatMayBeSmelted(ResourceType.COBBLESTONE, 100, 0));
        assertEquals(40, StockRules.rawThatMayBeSmelted(ResourceType.COBBLESTONE, 40, 40),
                "Regra 340: o empate não segura mais a fornalha");
    }

    @Test
    void logsBecomePlanksUpToThePlankReserve() {
        assertEquals(3, StockRules.logsToConvert(100, 0), "Regra 340: 12 tábuas são 3 toras");
        assertEquals(0, StockRules.logsToConvert(100, 12), "com a reserva cheia, nada mais vira tábua");
        assertEquals(2, StockRules.logsToConvert(2, 0), "toda tora vira tábua, sem guardar um terço");
    }
}
