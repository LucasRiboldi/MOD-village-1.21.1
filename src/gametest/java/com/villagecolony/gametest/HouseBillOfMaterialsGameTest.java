package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.ColonyHut;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A lista de compras da casa — 2026-08-15.
 *
 * <p>Não prova regra nenhuma: responde uma pergunta que custava uma
 * sessão de jogo por vez. A obra parou em {@code WAITING_RESOURCES} com
 * 149 blocos e o log não dizia <b>de quê</b> ela estava à espera, e sem
 * isso não dá para saber se a casa está lenta ou impossível.
 *
 * <p>A diferença é a que decide o próximo trabalho. O que a colônia
 * fabrica a partir de tronco — tábua, porta, escada — ela resolve
 * sozinha quando a Regra 10 existir. O que não se fabrica a partir de
 * madeira — pedregulho, vidro — não tem produtor nenhum nesta vila, e
 * nenhuma quantidade de lenhador vai mudar isso.
 *
 * <p>Sempre passa. O que ele entrega é a lista no log.
 */
public class HouseBillOfMaterialsGameTest implements FabricGameTest {

    /**
     * O que sai de tronco, e portanto do lenhador mais o fabricante.
     *
     * <p>Lista curta de propósito: cada nome aqui é uma cadeia que o mod
     * tem, ou vai ter pela Regra 10. Acrescentar um nome sem a cadeia
     * existir é o mesmo que voltar à casa de planície.
     */
    private static final java.util.Set<String> FROM_LOGS =
            java.util.Set.of("oak_planks", "oak_door", "oak_stairs", "oak_slab", "oak_fence");


    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "house_bill")
    public void theHouseSaysWhatItIsMadeOf(TestContext context) {
        StructureBlueprintReader.read(
                        context.getWorld(), StructureBlueprintReader.PLAINS_SMALL_HOUSE)
                .ifPresentOrElse(
                        HouseBillOfMaterialsGameTest::announce,
                        () -> VillageColonyMod.LOGGER.info(
                                "BILL — this game has no {}",
                                StructureBlueprintReader.PLAINS_SMALL_HOUSE));

        context.complete();
    }

    /**
     * E a cabana da colônia, que é o alvo do MVP desde 08-15.
     *
     * <p>Este afirma, e não só relata: a cabana tem de ser feita
     * <b>só</b> do que a colônia produz a partir de tronco. No dia em
     * que alguém puser pedregulho nela, a obra volta a ser impossível e
     * este teste cai antes de a sessão de jogo descobrir.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "hut_bill")
    public void theColonyCanMakeEverythingTheHutIsMadeOf(TestContext context) {
        Blueprint hut = ColonyHut.blueprint();

        announce(hut);

        for (ResourceId material : hut.materials().keySet()) {
            context.assertTrue(
                    FROM_LOGS.contains(material.path()),
                    "a cabana pede " + material + ", que a colônia não faz a partir de tronco");
        }

        context.complete();
    }

    /** Uma linha por material, do mais pedido para o menos. */
    private static void announce(Blueprint house) {
        Map<ResourceId, Integer> materials = house.materials();

        List<Map.Entry<ResourceId, Integer>> ordered = new ArrayList<>(materials.entrySet());

        ordered.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        VillageColonyMod.LOGGER.info(
                "BILL — {} needs {} blocks of {} kinds",
                house.id(),
                house.blockCount(),
                materials.size());

        for (Map.Entry<ResourceId, Integer> material : ordered) {
            VillageColonyMod.LOGGER.info(
                    "BILL   {} x {}", material.getValue(), material.getKey());
        }
    }
}
