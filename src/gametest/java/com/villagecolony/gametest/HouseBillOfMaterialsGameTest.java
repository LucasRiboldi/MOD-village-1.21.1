package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Blueprint;
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
