package com.villagecolony.gametest;

import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.integration.VillageStructures;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

import java.util.List;

/**
 * A Regra 27 — o construtor só levanta o que está no catálogo do jogo.
 *
 * <p>Regra imutável do autor, 2026-08-20: as estruturas que o construtor
 * de cada bioma pode construir são as da pasta de estruturas, e o mod
 * não cria nenhuma outra.
 *
 * <p>Isso desfez a Regra 13, que tinha inventado a cabana do mod porque
 * a casa do jogo era impossível de levantar. A resposta passou a ser
 * outra: a casa do jogo pede pedra, então a colônia aprendeu a minerar.
 */
public class VillageStructuresGameTest implements FabricGameTest {

    /** As cinco vilas que o jogo gera. */
    private static final List<String> STYLES =
            List.of("plains", "taiga", "savanna", "snowy", "desert");

    /** Cada estilo de vila tem casas, e o deserto tem as dele. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_catalog")
    public void everyVillageStyleHasHousesOfItsOwn(TestContext context) {
        for (String style : STYLES) {
            List<ResourceId> houses = VillageStructures.housesFor(style);

            context.assertTrue(
                    !houses.isEmpty(),
                    "a vila de " + style + " ficou sem casa nenhuma no catálogo");

            for (ResourceId house : houses) {
                context.assertTrue(
                        house.path().startsWith("village/" + style + "/houses/"),
                        house + " não é casa de " + style);
            }
        }

        context.complete();
    }

    /**
     * Nenhuma casa inventada pelo mod entra na lista.
     *
     * <p>É a regra dita pelo lado de fora: a cabana escrita em código —
     * {@code villagecolony:hut} — existia e era o que a colônia
     * levantava. Ela foi apagada em 2026-08-21, e o que resta afirmar é
     * que <b>nada fora do jogo entra na lista</b>: um namespace que não
     * seja {@code minecraft} é casa que o mod inventou.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_catalog")
    public void nothingTheModInventedIsOffered(TestContext context) {
        for (String style : STYLES) {
            for (ResourceId house : VillageStructures.housesFor(style)) {
                context.assertTrue(
                        ResourceId.VANILLA.equals(house.namespace()),
                        house + " não é do jogo, e a Regra 27 só aceita o que é");
            }
        }

        context.complete();
    }

    /**
     * A vila em ruína não é modelo de construção.
     *
     * <p>As variantes zumbi são as mesmas casas com teia e tocha
     * apagada. Uma colônia que as levantasse estaria construindo a
     * própria decadência.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_catalog")
    public void theRuinedVariantsAreNotBuildable(TestContext context) {
        for (String style : STYLES) {
            for (ResourceId house : VillageStructures.housesFor(style)) {
                context.assertTrue(
                        !house.path().contains("zombie"),
                        "a colônia ia levantar uma ruína: " + house);
            }
        }

        context.complete();
    }
}
