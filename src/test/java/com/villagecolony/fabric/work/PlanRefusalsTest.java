package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ResourceId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O registro das plantas que a colônia não conseguiu levantar — 2026-09-12.
 *
 * <p>Afirma a parte que não precisa de mundo: o que entra no registro e o
 * que não entra. A outra metade — a planta <b>voltar</b> quando o material
 * ficar ao alcance — pergunta ao {@code ColonySupply}, que varre baú, e por
 * isso vive no jogo e não aqui.
 *
 * <p><b>O que este arquivo existe para impedir.</b> Na sessão de 09-12 a
 * colônia escolheu {@code plains_butcher_shop_2} duas vezes seguidas,
 * esperou vinte ciclos por {@code smooth_stone_slab} em cada, e morreu nas
 * duas. A escolha da Regra 25 é determinística, então a terceira seria a
 * mesma: um laço de obras mortas, e o autor vendo <i>"nenhuma construção
 * nascendo"</i>.
 */
class PlanRefusalsTest {

    private static final ResourceId BIG =
            ResourceId.parse("minecraft:village/plains/houses/plains_butcher_shop_2");

    private static final ResourceId SMALL =
            ResourceId.parse("minecraft:village/plains/houses/plains_small_house_1");

    private static final ResourceId SLAB = ResourceId.vanilla("smooth_stone_slab");

    private UUID colony;

    @BeforeEach
    void freshRegistry() {
        PlanRefusals.clearAll();

        colony = UUID.randomUUID();
    }

    private static Map<ResourceId, Integer> missing(ResourceId what, int howMany) {
        Map<ResourceId, Integer> bill = new LinkedHashMap<>();

        bill.put(what, howMany);

        return bill;
    }

    /** A planta que morreu esperando material entra no registro. */
    @Test
    void thePlanThatDiedWaitingIsRemembered() {
        PlanRefusals.refused(colony, BIG, missing(SLAB, 12));

        assertTrue(
                PlanRefusals.skipped(colony).contains(BIG),
                "a planta que morreu esperando material não foi lembrada — a escolha"
                        + " determinística vai reoferecê-la no ciclo seguinte");
    }

    /**
     * Obra largada sem material faltando não condena a planta.
     *
     * <p>A desistência tem outras causas além de escassez, e a planta não
     * tem culpa delas. Marcar aqui tiraria da lista uma casa perfeitamente
     * viável — e como a marca só se desfaz quando o material volta, e não
     * há material, ela ficaria fora para sempre.
     */
    @Test
    void anEmptyBillCondemnsNothing() {
        PlanRefusals.refused(colony, BIG, Map.of());

        assertTrue(
                PlanRefusals.skipped(colony).isEmpty(),
                "obra largada sem material faltando riscou a planta da lista");
    }

    /**
     * Duas plantas podem estar marcadas ao mesmo tempo.
     *
     * <p>E é o caso comum, não o raro: a casa grande e a média de um mesmo
     * bioma costumam pedir a mesma peça. Com uma marca só, a segunda
     * desistência apagaria a primeira e a lista voltaria a oferecer a casa
     * grande — o laço de novo, com um ciclo de atraso.
     */
    @Test
    void twoPlansCanBeSkippedAtOnce() {
        PlanRefusals.refused(colony, BIG, missing(SLAB, 12));
        PlanRefusals.refused(colony, SMALL, missing(SLAB, 4));

        assertEquals(
                2,
                PlanRefusals.skipped(colony).size(),
                "a segunda desistência apagou a primeira: " + PlanRefusals.skipped(colony));
    }

    /** O registro é por colônia: a vizinha não herda a recusa. */
    @Test
    void theRefusalBelongsToOneColony() {
        PlanRefusals.refused(colony, BIG, missing(SLAB, 12));

        assertFalse(
                PlanRefusals.skipped(UUID.randomUUID()).contains(BIG),
                "a colônia vizinha herdou uma recusa que não é dela — ela pode ter"
                        + " fundidor, baú cheio, e a mesma casa de pé");
    }

    /** Parar o servidor esquece tudo: a marca é de sessão, como o TreeMarks. */
    @Test
    void stoppingTheServerForgets() {
        PlanRefusals.refused(colony, BIG, missing(SLAB, 12));

        PlanRefusals.clearAll();

        assertTrue(
                PlanRefusals.skipped(colony).isEmpty(),
                "a marca atravessou o clearAll, e ela é de memória de sessão");
    }
}
