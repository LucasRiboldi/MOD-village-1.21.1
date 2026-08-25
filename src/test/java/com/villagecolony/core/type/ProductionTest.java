package com.villagecolony.core.type;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * De onde cada recurso vem — declarado, e não deduzido por nome.
 *
 * <p>Havia em {@code ColonyCycle.typeFor} uma <b>exceção nominal</b>:
 * {@code if (resource == GLASS || resource == IRON_INGOT) return
 * SMELT_MATERIAL}. Ela funcionava e pedia mais um nome a cada material
 * novo — o arenito liso seria o terceiro. A regra de ouro da ADR-009
 * manda questionar soluções dessa forma.
 *
 * <p>O que estes testes travam é a propriedade, e não os casos: recurso
 * novo <b>tem</b> de declarar como é feito, e o compilador cobra isso
 * sozinho. Aqui se afirma que ninguém declarou errado.
 */
class ProductionTest {

    /** Nenhum recurso fica sem dizer de onde vem. */
    @Test
    void everyResourceDeclaresHowItIsMade() {
        for (ResourceType type : ResourceType.values()) {
            assertNotNull(type.production(), type + " não diz de onde vem");
        }
    }

    /** O que sai da fornalha, e é o que a exceção nominal listava. */
    @Test
    void theFurnaceMakesGlassIngotsAndSmoothSandstone() {
        Set<ResourceType> smelted = EnumSet.noneOf(ResourceType.class);

        for (ResourceType type : ResourceType.values()) {
            if (type.production() == Production.SMELTED) {
                smelted.add(type);
            }
        }

        assertEquals(
                EnumSet.of(
                        ResourceType.GLASS,
                        ResourceType.IRON_INGOT,
                        ResourceType.SMOOTH_SANDSTONE),
                smelted,
                "a lista do que sai de fornalha mudou sem que ninguém dissesse");
    }

    /**
     * Cada profissão fica com o que é dela.
     *
     * <p>Areia e pedregulho são os dois naturais e os dois minerados;
     * tábua e vidro são os dois processados, e um sai da bancada e o
     * outro da fornalha. É por isso que a produção não é a categoria.
     */
    @Test
    void eachTradeKeepsWhatIsItsOwn() {
        assertEquals(Production.HARVESTED, ResourceType.OAK_LOG.production());
        assertEquals(Production.CRAFTED, ResourceType.OAK_PLANKS.production());
        assertEquals(Production.SHEARED, ResourceType.WHITE_WOOL.production());

        for (ResourceType mined : EnumSet.of(
                ResourceType.COBBLESTONE,
                ResourceType.SANDSTONE,
                ResourceType.SAND,
                ResourceType.COAL,
                ResourceType.RAW_IRON)) {

            assertEquals(Production.MINED, mined.production(), mined + " saiu da mina");
        }
    }

    /**
     * O arenito liso é processado, e não natural.
     *
     * <p>A distinção decide quem recebe a tarefa: o mineiro cava o
     * arenito cru, e é a fornalha que o assa. Confundi-los poria o
     * mineiro a procurar no mundo um bloco que não nasce nele.
     */
    @Test
    void smoothSandstoneIsProcessedAndNotDug() {
        assertEquals(
                ResourceCategory.PROCESSED, ResourceType.SMOOTH_SANDSTONE.category());

        assertEquals(
                ResourceCategory.NATURAL, ResourceType.SANDSTONE.category());
    }
}
