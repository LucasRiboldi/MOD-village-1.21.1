package com.villagecolony.fabric.integration;

import com.villagecolony.core.resource.model.ColonyResources;
import com.villagecolony.core.resource.model.ResourceTally;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A frase da varredura de baús, e o que ela não pode deixar concluir.
 *
 * <p>Este teste existe por um defeito de <b>leitura</b>, e não de
 * contagem. Em 2026-09-11 a linha
 * {@code "Colony 3 stores {...} in 1 of 8 chests read"} foi lida como
 * cobertura — <i>"a colônia lê 1 de 8 baús"</i> — e virou o P0.2 do plano
 * de correção, com relatório de scan, cache por evento e varredura em
 * fila pendurados nela. Os dois números eram <b>baús com conteúdo</b> e
 * <b>baús lidos</b>: a varredura tinha alcançado os oito, e o que havia
 * era uma colônia com estoque num baú só.
 *
 * <p>O custo de descobrir isso é a razão do teste. A contagem sempre
 * esteve certa; o que faltava era uma frase que só admitisse uma leitura.
 *
 * <p>A regra que estes casos guardam: <b>a forma {@code "X of Y chests
 * read"} é reservada para cobertura de verdade</b>. Ela só aparece quando
 * algum baú ficou sem ser lido. Varredura completa não tem "de".
 */
class ChestSurveyCoverageTest {

    private static final ColonyPos CHEST = new ColonyPos(1436, 64, 730);

    /** Uma varredura completa de {@code read} baús, {@code withItems} deles cheios. */
    private static ChestInventoryReader.ChestSurvey complete(int read, int withItems) {
        return survey(read, withItems, 0);
    }

    private static ChestInventoryReader.ChestSurvey survey(
            int read, int withItems, int unreachable) {

        Map<ColonyPos, ResourceTally> byChest = new LinkedHashMap<>();

        for (int i = 0; i < withItems; i++) {
            Map<ResourceType, Integer> counts = new EnumMap<>(ResourceType.class);
            counts.put(ResourceType.COBBLESTONE, 12);

            byChest.put(new ColonyPos(CHEST.x() + i, CHEST.y(), CHEST.z()),
                    ResourceTally.of(counts));
        }

        return new ChestInventoryReader.ChestSurvey(
                ColonyResources.of(byChest), read, unreachable);
    }

    /**
     * O caso exato da sessão: oito lidos, um com estoque.
     *
     * <p>É a linha que foi lida ao contrário. A frase precisa dizer que
     * <b>oito</b> foram lidos — não que um de oito foi alcançado.
     */
    @Test
    void aFullSweepOfEightChestsSaysEightWereRead() {
        String coverage = complete(8, 1).coverage();

        assertEquals("8 chests read, 1 with items", coverage);
    }

    /**
     * E a forma que enganou não aparece quando nada ficou por ler.
     *
     * <p>A asserção é sobre o {@code " of "}: é ele que transforma dois
     * números independentes numa fração de cobertura aos olhos de quem
     * lê. Sem baú fora de alcance não há fração para dizer.
     */
    @Test
    void aFullSweepNeverSpellsAFraction() {
        assertFalse(complete(8, 1).coverage().contains(" of "),
                "varredura completa não pode sair como fração de cobertura");
        assertFalse(complete(16, 5).coverage().contains(" of "));
        assertFalse(complete(8, 0).coverage().contains(" of "));
    }

    /**
     * Cobertura de verdade usa a fração, e diz o total conhecido.
     *
     * <p>Aqui o "de" é o certo: três baús registrados ficaram em chunk
     * descarregado, e o número que importa é quantos dos <b>onze</b> a
     * varredura alcançou.
     */
    @Test
    void anIncompleteSweepSpellsTheFractionItActuallyMissed() {
        ChestInventoryReader.ChestSurvey partial = survey(8, 1, 3);

        assertEquals(11, partial.chestsKnown());
        assertTrue(partial.isPartial());
        assertEquals(
                "8 of 11 chests read (3 in unloaded chunks), 1 with items",
                partial.coverage());
    }

    /**
     * Os dois números são perguntas diferentes, e a frase os separa.
     *
     * <p>Duas colônias que o log antigo descrevia com o mesmo par de
     * números — "1" e "8" — em ordens diferentes. Sem a separação, quem
     * lê o relatório não sabe qual das duas está vendo: a colônia que
     * leu tudo e tem pouco, ou a que não conseguiu ler.
     */
    @Test
    void readingAChestAndFindingSomethingInItAreNotTheSameCount() {
        String readEverythingFoundLittle = complete(8, 1).coverage();
        String readOneOfEight = survey(1, 1, 7).coverage();

        assertEquals("8 chests read, 1 with items", readEverythingFoundLittle);
        assertEquals(
                "1 of 8 chests read (7 in unloaded chunks), 1 with items",
                readOneOfEight);

        assertFalse(readEverythingFoundLittle.equals(readOneOfEight),
                "a colônia que leu tudo e a que não leu nada não podem sair iguais");
    }

    /** Baú vazio conta como lido, e é essa a diferença que o V5 apontava. */
    @Test
    void anEmptyChestIsStillAChestThatWasRead() {
        ChestInventoryReader.ChestSurvey survey = complete(8, 0);

        assertEquals(8, survey.chestsRead());
        assertEquals(0, survey.chestsWithItems());
        assertFalse(survey.isPartial());
        assertEquals("8 chests read, 0 with items", survey.coverage());
    }
}
