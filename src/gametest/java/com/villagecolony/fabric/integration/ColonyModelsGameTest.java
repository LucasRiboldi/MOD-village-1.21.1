package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.work.HousePlans;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

import java.util.Optional;

/**
 * Os modelos da colônia vêm antes dos do jogo — pedido do autor, 2026-10-02.
 *
 * <p>A bateria divide um mundo só, e um modelo de verdade no jar mudaria as
 * obras de todos os outros testes. Por isso o modelo é "fingido" (o caminho do
 * modelo aponta para a casa pequena de teste) e desfeito na mesma chamada.
 */
public final class ColonyModelsGameTest implements FabricGameTest {

    private static final ResourceId SMALL = new ResourceId("villagecolony", "houses/small_house");

    private static final ResourceId TEMPLE = ResourceId.vanilla("village/snowy/houses/snowy_temple_1");

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "colony_models", tickLimit = 20)
    public void aColonyModelReplacesTheGameStructureButKeepsItsName(TestContext context) {
        int small = StructureBlueprintReader.read(context.getWorld(), SMALL).orElseThrow().blocks().size();
        int temple = StructureBlueprintReader.read(context.getWorld(), TEMPLE).orElseThrow().blocks().size();

        context.assertTrue(small != temple, "o teste precisa de duas plantas de tamanhos diferentes");

        try {
            ColonyModels.pretend("colony/override/" + TEMPLE.path(), SMALL);

            Blueprint read = StructureBlueprintReader.read(context.getWorld(), TEMPLE).orElseThrow();

            context.assertTrue(read.id().equals(TEMPLE), "o nome da planta tinha de continuar o do jogo: " + read.id());
            context.assertTrue(read.blocks().size() == small,
                    "o modelo da colônia não foi o lido: " + read.blocks().size() + " blocos, o modelo tem " + small);
        } finally {
            ColonyModels.stopPretending();
        }

        int after = StructureBlueprintReader.read(context.getWorld(), TEMPLE).orElseThrow().blocks().size();

        context.assertTrue(after == temple, "sem o modelo, a planta do jogo tinha de voltar");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "colony_models", tickLimit = 20)
    public void aColonyTradeHouseComesFirstAndBelongsToItsTrade(TestContext context) {
        ResourceId trade = new ResourceId("villagecolony", "colony/desert/trade_builder");

        try {
            ColonyModels.pretend(trade.path(), SMALL);

            context.assertTrue(VillageStructures.buildableFor("desert").get(0).equals(trade),
                    "a casa de ofício da colônia devia vir à frente: " + VillageStructures.buildableFor("desert").get(0));
            context.assertTrue(ColonyModels.professionOf(trade).equals(Optional.of(ProfessionType.BUILDER)),
                    "o nome trade_builder devia dar o construtor");
            context.assertFalse(HousePlans.isHouse(trade), "casa de ofício não é moradia do rodízio");
            context.assertTrue(StructureBlueprintReader.read(context.getWorld(), trade).isPresent(),
                    "a casa de ofício da colônia não foi lida");
        } finally {
            ColonyModels.stopPretending();
        }

        context.assertFalse(VillageStructures.buildableFor("desert").contains(trade),
                "sem o modelo, a lista do deserto tinha de voltar a ser a do jogo");
        context.complete();
    }
}
